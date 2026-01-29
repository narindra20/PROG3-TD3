import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        DataRetriever dr = new DataRetriever();

        System.out.println("=== TEST COMPLET DU PROJET (AVEC NOUVELLES FONCTIONNALITÉS) ===");

        /* ================================
           TEST DES PLATS (EXISTANT)
           ================================ */
        int[] dishIds = {1, 2, 3, 4, 5};
        String[] dishNames = {
                "Salade fraîche",
                "Poulet grillé",
                "Riz aux légumes",
                "Gâteau au chocolat",
                "Salade de fruits"
        };

        System.out.println("\n1) TEST getDishCost() et getGrossMargin()");
        for (int i = 0; i < dishIds.length; i++) {
            Dish dish = dr.findDishById(dishIds[i]);

            // Calcul du coût du plat à partir des ingrédients
            double cost = dish.getDishIngredients().stream()
                    .mapToDouble(di -> {
                        Double price = di.getIngredient().getPrice();
                        return price != null && di.getQuantity() != null ? price * di.getQuantity() : 0;
                    }).sum();

            Double margin = dish.getPrice() != null ? dish.getPrice() - cost : null;

            System.out.println(dishNames[i] + " -> coût : " + cost + ", marge : " + margin);
        }

        System.out.println("\n2) TEST findIngredientsByDishId()");
        for (int i = 0; i < dishIds.length; i++) {
            Dish dish = dr.findDishById(dishIds[i]);
            List<DishIngredient> ingredients = dish.getDishIngredients();
            System.out.println(dishNames[i] + " -> ingrédients : ");
            for (DishIngredient di : ingredients) {
                System.out.println("   - " + di.getIngredient().getName());
            }
        }

        /* ================================
           TEST DU STOCK (EXISTANT)
           ================================ */
        System.out.println("\n3) TEST DU STOCK");

        Instant t = Timestamp.valueOf("2024-01-06 12:00:00").toInstant();

        int[] ingredientIds = {1, 2, 3, 4, 5};
        String[] ingredientNames = {
                "Laitue",
                "Tomate",
                "Poulet",
                "Chocolat",
                "Beurre"
        };

        for (int i = 0; i < ingredientIds.length; i++) {
            Ingredient ing = dr.findIngredientById(ingredientIds[i]);

            double stock = ing.getStockMovementList().stream()
                    .filter(sm -> !sm.getCreationDatetime().isAfter(t))
                    .mapToDouble(sm -> sm.getValue().getQuantity())
                    .sum();

            System.out.println(ingredientNames[i] + " -> stock = " + stock);
        }

        /* ================================
           TEST DES NOUVELLES MÉTHODES REQUISES
           ================================ */
        System.out.println("\n4) TEST DES NOUVELLES MÉTHODES REQUISES");

        // Test saveDish
        try {
            Dish newDish = new Dish();
            newDish.setName("Nouveau Plat Test");
            newDish.setDishType(DishTypeEnum.MAIN); // Assurez-vous que cette enum existe
            newDish.setPrice(15.99);

            Dish savedDish = dr.saveDish(newDish);
            System.out.println("✅ Dish.saveDish() : Plat créé avec ID: " + savedDish.getId());
        } catch (Exception e) {
            System.out.println("❌ Erreur saveDish: " + e.getMessage());
        }

        // Test saveIngredient
        try {
            Ingredient newIngredient = new Ingredient();
            newIngredient.setName("Nouvel Ingrédient Test");
            newIngredient.setCategory(CategoryEnum.VEGETABLE); // Assurez-vous que cette enum existe
            newIngredient.setPrice(3.50);

            Ingredient savedIngredient = dr.saveIngredient(newIngredient);
            System.out.println("✅ Ingredient.saveIngredient() : Ingrédient créé avec ID: " + savedIngredient.getId());
        } catch (Exception e) {
            System.out.println("❌ Erreur saveIngredient: " + e.getMessage());
        }

        /* ================================
           TEST DES COMMANDES (AMÉLIORÉ POUR TESTER LES NOUVELLES FONCTIONNALITÉS)
           ================================ */
        System.out.println("\n5) TEST DES COMMANDES AVEC TYPE ET STATUT");

        try {
            // Création d'une commande (sans spécifier type/statut -> valeurs par défaut)
            Order order = new Order();
            order.setReference("CMD001");

            List<DishOrder> dishOrders = new ArrayList<>();

            DishOrder do1 = new DishOrder();
            do1.setDish(dr.findDishById(1)); // Salade fraîche
            do1.setQuantity(2);
            dishOrders.add(do1);

            DishOrder do2 = new DishOrder();
            do2.setDish(dr.findDishById(2)); // Poulet grillé
            do2.setQuantity(1);
            dishOrders.add(do2);

            order.setDishOrderList(dishOrders);

            // Sauvegarde de la commande
            Order savedOrder = dr.saveOrder(order);
            System.out.println("✅ Commande sauvegardée : " + savedOrder.getReference());
            System.out.println("   Type par défaut: " + savedOrder.getType());
            System.out.println("   Statut par défaut: " + savedOrder.getStatus());

            // Récupération et affichage de la commande
            Order retrievedOrder = dr.findOrderByReference("CMD001");
            System.out.println("✅ Commande récupérée : " + retrievedOrder.getReference());
            System.out.println("   Type: " + retrievedOrder.getType());
            System.out.println("   Statut: " + retrievedOrder.getStatus());

            for (DishOrder dishOrder : retrievedOrder.getDishOrderList()) {
                Dish dish = dishOrder.getDish();
                int qty = dishOrder.getQuantity();

                double cost = dish.getDishIngredients().stream()
                        .mapToDouble(di -> {
                            Double price = di.getIngredient().getPrice();
                            return price != null && di.getQuantity() != null ? price * di.getQuantity() : 0;
                        }).sum();
                double margin = dish.getPrice() != null ? dish.getPrice() - cost : 0;

                System.out.println("- " + dish.getName() + ", quantité : " + qty
                        + " -> coût total : " + (cost * qty)
                        + ", marge totale : " + (margin * qty));
            }

            /* ================================
               TEST DE LA MODIFICATION D'UNE COMMANDE
               ================================ */
            System.out.println("\n6) TEST MODIFICATION COMMANDE");

            // Modification du type et statut
            retrievedOrder.setType(OrderTypeEnum.TAKE_AWAY);
            retrievedOrder.setStatus(OrderStatusEnum.READY);
            Order modifiedOrder = dr.saveOrder(retrievedOrder);
            System.out.println("✅ Commande modifiée :");
            System.out.println("   Nouveau type: " + modifiedOrder.getType());
            System.out.println("   Nouveau statut: " + modifiedOrder.getStatus());

            /* ================================
               TEST COMMANDE LIVRÉE (NE DOIT PLUS ÊTRE MODIFIABLE)
               ================================ */
            System.out.println("\n7) TEST COMMANDE LIVRÉE (NON MODIFIABLE)");

            // Passage en statut DELIVERED
            modifiedOrder.setStatus(OrderStatusEnum.DELIVERED);
            Order deliveredOrder = dr.saveOrder(modifiedOrder);
            System.out.println("✅ Commande livrée : statut = " + deliveredOrder.getStatus());

            // Tentative de modification d'une commande livrée
            System.out.println("⚠️  Tentative de modification d'une commande livrée...");
            try {
                deliveredOrder.setType(OrderTypeEnum.EAT_IN);
                dr.saveOrder(deliveredOrder);
                System.out.println("❌ ERREUR: La commande a pu être modifiée !");
            } catch (RuntimeException e) {
                System.out.println("✅ SUCCÈS: Exception levée comme attendu: " + e.getMessage());
            }

        } catch (RuntimeException e) {
            System.err.println("❌ Erreur lors de la commande : " + e.getMessage());
            e.printStackTrace();
        }

        /* ================================
           TEST COMMANDE AVEC TYPE ET STATUT EXPLICITES
           ================================ */
        System.out.println("\n8) TEST COMMANDE AVEC VALEURS EXPLICITES");
        try {
            Order order2 = new Order();
            order2.setReference("CMD002");
            order2.setType(OrderTypeEnum.TAKE_AWAY);  // Type explicite
            order2.setStatus(OrderStatusEnum.READY);  // Statut explicite

            List<DishOrder> dishOrders2 = new ArrayList<>();
            DishOrder do3 = new DishOrder();
            do3.setDish(dr.findDishById(3)); // Riz aux légumes
            do3.setQuantity(1);
            dishOrders2.add(do3);
            order2.setDishOrderList(dishOrders2);

            Order savedOrder2 = dr.saveOrder(order2);
            System.out.println("✅ Commande avec valeurs explicites créée:");
            System.out.println("   Référence: " + savedOrder2.getReference());
            System.out.println("   Type: " + savedOrder2.getType());
            System.out.println("   Statut: " + savedOrder2.getStatus());

        } catch (RuntimeException e) {
            System.err.println("❌ Erreur commande 2: " + e.getMessage());
        }

        System.out.println("\n=== TOUS LES TESTS TERMINÉS ===");
    }
}