import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestDataRetriever {

    private DataRetriever dr;

    @BeforeEach
    void setUp() {
        dr = new DataRetriever();
    }

    // ===============================
    // getDishCost()
    // ===============================

    @Test
    void testGetDishCost_DoesNotCrash() {
        assertDoesNotThrow(() -> {
            dr.getDishCost();
        });
    }

    // ===============================
    // getGrossMargin()
    // ===============================

    @Test
    void testGetGrossMargin_SaladeFraiche() {
        Double margin = dr.getGrossMargin(1);
        assertEquals(3250.00, margin, 0.01);
    }

    @Test
    void testGetGrossMargin_PouletGrille() {
        Double margin = dr.getGrossMargin(2);
        assertEquals(7500.00, margin, 0.01);
    }

    @Test
    void testGetGrossMargin_GateauChocolat() {
        Double margin = dr.getGrossMargin(4);
        assertEquals(6600.00, margin, 0.01);
    }
    @Test
    void testGetGrossMargin_RizAuxLegumes() {
        Double margin = dr.getGrossMargin(3);
        assertEquals(-400.00, margin, 0.01);
    }


    @Test
    void testGetGrossMargin_SaladeFruits_PrixNull() {
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> dr.getGrossMargin(5));

        assertTrue(exception.getMessage().contains("Prix de vente non défini"));
    }

    // ===============================
    // findIngredientsByDishId()
    // ===============================

    @Test
    void testFindIngredientsByDishId_SaladeFraiche() {
        List<Ingredient> ingredients = dr.findIngredientsByDishId(1);

        assertNotNull(ingredients);
        assertFalse(ingredients.isEmpty());

        Ingredient first = ingredients.get(0);
        assertNotNull(first.getName());
        assertNotNull(first.getCategory());
    }

    @Test
    void testFindIngredientsByDishId_PlatSansIngredients() {
        List<Ingredient> ingredients = dr.findIngredientsByDishId(999);

        assertNotNull(ingredients);
        assertTrue(ingredients.isEmpty());
    }
}
