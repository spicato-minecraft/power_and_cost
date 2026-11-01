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
        int count = formulas.materialRepairCount(8, 216, 240, 64, 1.0);
        assertEquals(4, count);
    }

    @Test
    void enchantPenaltyLetsExtraMaterialsFinishANearlyFullRepair() {
        double factor = 0.9;
        int craftCost = 3;
        int max = 1561;
        double yFull = formulas.fullRepairMaterialCost(craftCost);
        int damage = (int) Math.floor(max / yFull) - 1;

        assertTrue(
                formulas.materialsForCurrentDamage(craftCost, damage, max, 1.0) <= 1.0,
                "Setup should sit inside the old one-material ceiling"
        );
        assertTrue(
                (1.0 / yFull) * max * factor < damage,
                "One material at this factor should leave the item short of full"
        );

        int count = formulas.materialRepairCount(craftCost, damage, max, 64, factor);
        assertTrue(count > 1, "Expected a second material to be consumed, got " + count);
        double restored = Math.min(damage, (count / yFull) * max * factor);
        assertEquals(damage, restored, 0.5, "Offered stack should cover the remaining damage");
    }

    @Test
    void halfMaterialEfficacyDoublesUnitsNeeded() {
        double baseline = formulas.materialsForCurrentDamage(8, 200, 240, 1.0);
        double half = formulas.materialsForCurrentDamage(8, 200, 240, 1.0, 0.5);
        assertEquals(baseline * 2, half, 1e-9);
    }

    @Test
    void doubleMaterialEfficacyHalvesUnitsNeeded() {
        double baseline = formulas.materialsForCurrentDamage(8, 200, 240, 1.0);
        double doubled = formulas.materialsForCurrentDamage(8, 200, 240, 1.0, 2.0);
        assertEquals(baseline / 2, doubled, 1e-9);
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
