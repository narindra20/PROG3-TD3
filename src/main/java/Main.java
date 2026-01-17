import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever dr = new DataRetriever();

        System.out.println("=== TEST COMPLET DE LA NORMALISATION ===");
        System.out.println("Base de données configurée avec succès ✓");

        // 1. Test de getDishCost() - doit afficher les coûts
        System.out.println("\n1. TEST getDishCost() :");
        dr.getDishCost();

        // 2. Test de getGrossMargin() pour chaque plat
        System.out.println("\n2. TEST getGrossMargin() :");

        // Tableau des plats avec leurs IDs
        int[] dishIds = {1, 2, 3, 4, 5};
        String[] dishNames = {
                "Salade fraîche (ID 1)",
                "Poulet grillé (ID 2)",
                "Riz aux légumes (ID 3)",
                "Gâteau au chocolat (ID 4)",
                "Salade de fruits (ID 5)"
        };

        for (int i = 0; i < dishIds.length; i++) {
            System.out.println("\n  • " + dishNames[i] + " :");
            try {
                Double margin = dr.getGrossMargin(dishIds[i]);
                System.out.println("    Marge calculée : " + String.format("%.2f", margin));

                // Vérification avec les valeurs attendues
                checkExpectedMargin(dishIds[i], margin);

            } catch (IllegalStateException e) {
                System.out.println("    " + e.getMessage());
                checkExpectedException(dishIds[i]);
            } catch (Exception e) {
                System.out.println("    ERREUR : " + e.getMessage());
            }
        }

        // 3. Test de findIngredientsByDishId()
        System.out.println("\n3. TEST findIngredientsByDishId() :");
        testFindIngredients(dr);


        System.out.println("\n=== TESTS TERMINÉS ===");
    }

    private static void checkExpectedMargin(int dishId, Double actualMargin) {
        switch(dishId) {
            case 1: // Salade fraîche
                if (Math.abs(actualMargin - 3250.00) < 0.01) {
                    System.out.println("    ✓ Valeur attendue : 3250.00");
                } else {
                    System.out.println("    ✗ ATTENDU : 3250.00, OBTENU : " + actualMargin);
                }
                break;
            case 2: // Poulet grillé
                if (Math.abs(actualMargin - 7500.00) < 0.01) {
                    System.out.println("    ✓ Valeur attendue : 7500.00");
                } else {
                    System.out.println("    ✗ ATTENDU : 7500.00, OBTENU : " + actualMargin);
                }
                break;
            case 4: // Gâteau au chocolat
                if (Math.abs(actualMargin - 6600.00) < 0.01) {
                    System.out.println("    ✓ Valeur attendue : 6600.00");
                } else {
                    System.out.println("    ✗ ATTENDU : 6600.00, OBTENU : " + actualMargin);
                }
                break;
        }
    }

    private static void checkExpectedException(int dishId) {
        if (dishId == 3 || dishId == 5) {
            System.out.println("    ✓ Exception attendue (prix NULL)");
        } else {
            System.out.println("    ✗ Exception non attendue pour ce plat");
        }
    }

    private static void testFindIngredients(DataRetriever dr) {
        System.out.println("  • Ingrédients du plat ID 1 (Salade fraîche) :");
        List<Ingredient> ingredients = dr.findIngredientsByDishId(1);
        System.out.println("    Nombre d'ingrédients : " + ingredients.size());
        for (Ingredient ing : ingredients) {
            System.out.println("    - " + ing.getName() + " (prix: " + ing.getPrice() + ")");
        }
    }
}
