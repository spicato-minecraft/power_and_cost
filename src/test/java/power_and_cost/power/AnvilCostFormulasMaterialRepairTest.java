package power_and_cost.power;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilCostFormulasMaterialRepairTest {

    private AnvilCostFormulas formulas;

    @BeforeEach
    void setUp() {
        formulas = new AnvilCostFormulas(new PowerLevelConfig());
    }

    @Test
    void shovelFullRepairCostIsAboutOne() {
        double y = formulas.fullRepairMaterialCost(1);
        assertTrue(y > 0.95 && y < 1.1, "Expected ~1 material for shovel full repair, got " + y);
    }

    @Test
    void chestplateFullRepairCostIsBluntedVersusLinear() {
        double y = formulas.fullRepairMaterialCost(8);
        assertTrue(y > 4.0 && y < 4.5, "Expected ~4.2 materials for chestplate full repair, got " + y);
    }

    @Test
    void materialCountScalesWithMissingDurability() {
        int count = formulas.materialRepairCount(8, 216, 240, 64);
        assertEquals(4, count);
    }

    @Test
    void fourMaterialsRestoreLessThanNearFullDamage() {
        double yFull = formulas.fullRepairMaterialCost(8);
        int max = 528;
        int damage = max - max / 100;
        double restored = Math.min(damage, (4 / yFull) * max);
        assertTrue(restored < damage - max * 0.01, "Four mats should not cover all damage when yFull is ~4.2");
    }
}
