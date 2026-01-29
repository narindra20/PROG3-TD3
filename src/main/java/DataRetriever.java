import java.sql.*;
import java.time.Instant;
import java.util.*;

public class DataRetriever {

    // =================== ORDERS ===================

    public Order findOrderByReference(String reference) {
        String sql = "SELECT id, reference, creation_datetime, type, status FROM \"order\" WHERE reference = ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = new Order();
                    int orderId = rs.getInt("id");
                    order.setId(orderId);
                    order.setReference(rs.getString("reference"));
                    order.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    order.setType(OrderTypeEnum.valueOf(rs.getString("type")));
                    order.setStatus(OrderStatusEnum.valueOf(rs.getString("status")));
                    order.setDishOrderList(findDishOrderByIdOrder(orderId));
                    return order;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Order not found: " + reference);
    }

    private List<DishOrder> findDishOrderByIdOrder(int orderId) {
        String sql = "SELECT id, id_dish, quantity FROM dish_order WHERE id_order = ?";
        List<DishOrder> dishOrders = new ArrayList<>();
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Dish dish = findDishById(rs.getInt("id_dish"));
                    DishOrder dishOrder = new DishOrder();
                    dishOrder.setId(rs.getInt("id"));
                    dishOrder.setDish(dish);
                    dishOrder.setQuantity(rs.getInt("quantity"));
                    dishOrders.add(dishOrder);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dishOrders;
    }

    public Order saveOrder(Order orderToSave) {
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);

            // Valeurs par défaut
            if (orderToSave.getType() == null) orderToSave.setType(OrderTypeEnum.EAT_IN);
            if (orderToSave.getStatus() == null) orderToSave.setStatus(OrderStatusEnum.CREATED);

            // Vérifie si la commande existe déjà et est livrée
            if (orderToSave.getId() != 0) {
                try {
                    Order existing = findOrderByReference(orderToSave.getReference());
                    if (existing.getStatus() == OrderStatusEnum.DELIVERED) {
                        throw new RuntimeException("Impossible de modifier une commande déjà livrée");
                    }
                } catch (RuntimeException e) {
                    if (!e.getMessage().contains("Order not found")) throw e;
                }
            }

            // UPDATE
            if (orderToSave.getId() != 0) {
                String updateSql = "UPDATE \"order\" SET type=?, status=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, orderToSave.getType().name());
                    ps.setString(2, orderToSave.getStatus().name());
                    ps.setInt(3, orderToSave.getId());
                    ps.executeUpdate();
                }
                conn.commit();
                return findOrderByReference(orderToSave.getReference());
            }

            // INSERT
            Instant now = Instant.now();

            // Vérification du stock
            for (DishOrder dishOrder : orderToSave.getDishOrderList()) {
                Dish dish = dishOrder.getDish();
                int qtyOrdered = dishOrder.getQuantity();
                for (DishIngredient di : dish.getDishIngredients()) {
                    double requiredQty = di.getQuantity() * qtyOrdered;
                    double currentStock = getCurrentStock(conn, di.getIngredient().getId(), now);
                    if (currentStock < requiredQty) {
                        throw new RuntimeException("Stock insuffisant pour : " + di.getIngredient().getName());
                    }
                }
            }

            int orderId = getNextSerialValue(conn, "order", "id");
            String insertOrderSql =
                    "INSERT INTO \"order\" (id, reference, creation_datetime, type, status) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, orderToSave.getReference());
                ps.setTimestamp(3, Timestamp.from(now));
                ps.setString(4, orderToSave.getType().name());
                ps.setString(5, orderToSave.getStatus().name());
                ps.executeUpdate();
            }

            // Insertion dish_order
            String insertDishOrderSql =
                    "INSERT INTO dish_order (id, id_order, id_dish, quantity) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertDishOrderSql)) {
                for (DishOrder dishOrder : orderToSave.getDishOrderList()) {
                    int dishOrderId = getNextSerialValue(conn, "dish_order", "id");
                    ps.setInt(1, dishOrderId);
                    ps.setInt(2, orderId);
                    ps.setInt(3, dishOrder.getDish().getId());
                    ps.setInt(4, dishOrder.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Stock movement
            String insertStockMovementSql =
                    "INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime) " +
                            "VALUES (?, ?, ?, ?::movement_type, ?::unit, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertStockMovementSql)) {
                for (DishOrder dishOrder : orderToSave.getDishOrderList()) {
                    Dish dish = dishOrder.getDish();
                    int qtyOrdered = dishOrder.getQuantity();
                    for (DishIngredient di : dish.getDishIngredients()) {
                        int movementId = getNextSerialValue(conn, "stock_movement", "id");
                        ps.setInt(1, movementId);
                        ps.setInt(2, di.getIngredient().getId());
                        ps.setDouble(3, di.getQuantity() * qtyOrdered);
                        ps.setString(4, MovementTypeEnum.OUT.name());
                        ps.setString(5, di.getUnit().name());
                        ps.setTimestamp(6, Timestamp.from(now));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            conn.commit();
            return findOrderByReference(orderToSave.getReference());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private double getCurrentStock(Connection conn, int ingredientId, Instant now) throws SQLException {
        String sql =
                "SELECT COALESCE(SUM(CASE WHEN type='IN' THEN quantity ELSE -quantity END), 0) AS stock " +
                        "FROM stock_movement WHERE id_ingredient=? AND creation_datetime <= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setTimestamp(2, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("stock");
            }
        }
        return 0.0;
    }

    // =================== DISH ===================

    public Dish findDishById(Integer idDish) {
        String sql = "SELECT id, name, dish_type, selling_price FROM dish WHERE id = ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDish);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setPrice(rs.getObject("selling_price") == null ? null : rs.getDouble("selling_price"));
                    dish.setDishIngredients(findIngredientByDishId(idDish));
                    return dish;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Dish not found: " + idDish);
    }

    public Dish saveDish(Dish dishToSave) {
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);

            if (dishToSave.getId() != 0) {
                // UPDATE
                String updateSql = "UPDATE dish SET name=?, dish_type=?::dish_type, selling_price=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, dishToSave.getName());
                    ps.setString(2, dishToSave.getDishType().name());
                    ps.setDouble(3, dishToSave.getPrice() != null ? dishToSave.getPrice() : 0.0);
                    ps.setInt(4, dishToSave.getId());
                    ps.executeUpdate();
                }
                conn.commit();
                return findDishById(dishToSave.getId());
            }

            // INSERT
            int dishId = getNextSerialValue(conn, "dish", "id");
            String insertSql = "INSERT INTO dish (id, name, dish_type, selling_price) VALUES (?, ?, ?::dish_type, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, dishId);
                ps.setString(2, dishToSave.getName());
                ps.setString(3, dishToSave.getDishType().name());
                ps.setDouble(4, dishToSave.getPrice() != null ? dishToSave.getPrice() : 0.0);
                ps.executeUpdate();
                dishToSave.setId(dishId);
            }

            conn.commit();
            return dishToSave;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // =================== INGREDIENT ===================

    public Ingredient findIngredientById(Integer idIngredient) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE id = ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idIngredient);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int ingId = rs.getInt("id");
                    String name = rs.getString("name");
                    Double price = rs.getDouble("price");
                    CategoryEnum category = CategoryEnum.valueOf(rs.getString("category"));
                    return new Ingredient(ingId, name, category, price, findStockMovementsByIngredientId(ingId));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Ingredient not found: " + idIngredient);
    }

    public Ingredient saveIngredient(Ingredient ingredientToSave) {
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);

            if (ingredientToSave.getId() != 0) {
                // UPDATE
                String updateSql = "UPDATE ingredient SET name=?, price=?, category=?::ingredient_category WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, ingredientToSave.getName());
                    ps.setDouble(2, ingredientToSave.getPrice() != null ? ingredientToSave.getPrice() : 0.0);
                    ps.setString(3, ingredientToSave.getCategory().name());
                    ps.setInt(4, ingredientToSave.getId());
                    ps.executeUpdate();
                }
                conn.commit();
                return findIngredientById(ingredientToSave.getId());
            }

            // INSERT
            int ingredientId = getNextSerialValue(conn, "ingredient", "id");
            String insertSql = "INSERT INTO ingredient (id, name, price, category) VALUES (?, ?, ?, ?::ingredient_category)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, ingredientId);
                ps.setString(2, ingredientToSave.getName());
                ps.setDouble(3, ingredientToSave.getPrice() != null ? ingredientToSave.getPrice() : 0.0);
                ps.setString(4, ingredientToSave.getCategory().name());
                ps.executeUpdate();
                ingredientToSave.setId(ingredientId);
            }

            conn.commit();
            return ingredientToSave;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<StockMovement> findStockMovementsByIngredientId(int id) {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT id, quantity, unit, type, creation_datetime FROM stock_movement WHERE id_ingredient = ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setType(MovementTypeEnum.valueOf(rs.getString("type")));
                    sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());

                    StockValue sv = new StockValue();
                    sv.setQuantity(rs.getDouble("quantity"));
                    sv.setUnit(Unit.valueOf(rs.getString("unit")));
                    sm.setValue(sv);

                    movements.add(sm);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return movements;
    }

    private List<DishIngredient> findIngredientByDishId(int dishId) {
        List<DishIngredient> list = new ArrayList<>();
        String sql = "SELECT i.id, i.name, i.price, i.category, di.required_quantity, di.unit " +
                "FROM ingredient i JOIN dish_ingredient di ON di.id_ingredient = i.id " +
                "WHERE di.id_dish = ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ingredient ing = new Ingredient();
                    ing.setId(rs.getInt("id"));
                    ing.setName(rs.getString("name"));
                    ing.setPrice(rs.getDouble("price"));
                    ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                    DishIngredient di = new DishIngredient();
                    di.setIngredient(ing);
                    di.setQuantity(rs.getDouble("required_quantity"));
                    di.setUnit(Unit.valueOf(rs.getString("unit")));

                    list.add(di);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    // =================== SERIAL / SEQUENCE ===================

    private String getSerialSequenceName(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT pg_get_serial_sequence(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String table, String column) throws SQLException {
        String seq = getSerialSequenceName(conn, table, column);
        if (seq == null) throw new IllegalArgumentException("No sequence found for " + table + "." + column);

        String sql = "SELECT nextval(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seq);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
