-- 1. Créer les types ENUM
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'unit_type') THEN
        CREATE TYPE unit_type AS ENUM ('PCS', 'KG', 'L');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'dish_type') THEN
        CREATE TYPE dish_type AS ENUM ('STARTER', 'MAIN', 'DESSERT');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ingredient_category') THEN
        CREATE TYPE ingredient_category AS ENUM ('VEGETABLE', 'MEAT', 'SAUCE', 'SPICE', 'DAIRY', 'FRUIT', 'GRAIN', 'OTHER');
    END IF;
END $$;

-- 2. Ajouter selling_price à Dish
ALTER TABLE Dish
ADD COLUMN IF NOT EXISTS selling_price DECIMAL(10,2) NULL;

-- 3. Supprimer id_dish de Ingredient
ALTER TABLE Ingredient
DROP COLUMN IF EXISTS id_dish;

-- 4. Créer la table DishIngredient
CREATE TABLE IF NOT EXISTS DishIngredient (
    id SERIAL PRIMARY KEY,
    id_dish INT NOT NULL,
    id_ingredient INT NOT NULL,
    quantity_required DECIMAL(10,2) NOT NULL,
    unit unit_type NOT NULL,
    FOREIGN KEY (id_dish) REFERENCES Dish(id) ON DELETE CASCADE,
    FOREIGN KEY (id_ingredient) REFERENCES Ingredient(id) ON DELETE CASCADE
);

-- 5. Insérer les données de jointure (DishIngredient)
INSERT INTO DishIngredient (id_dish, id_ingredient, quantity_required, unit) VALUES
(1, 1, 0.20, 'KG'),
(1, 2, 0.15, 'KG'),
(2, 3, 1.00, 'KG'),
(4, 4, 0.30, 'KG'),
(4, 5, 0.20, 'KG')
ON CONFLICT DO NOTHING;

-- 6. Mettre à jour les selling_price des plats
UPDATE Dish SET selling_price = 3500.00 WHERE id = 1;
UPDATE Dish SET selling_price = 12000.00 WHERE id = 2;
UPDATE Dish SET selling_price = 8000.00 WHERE id = 4;


ALTER TABLE DishIngredient
ADD CONSTRAINT unique_dish_ingredient UNIQUE (id_dish, id_ingredient);