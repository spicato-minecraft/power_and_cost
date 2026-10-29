package power_and_cost.power;

import net.minecraft.world.item.ItemStack;

/**
 * Sigmoid formulas for anvil cost and repair effectiveness.
 */
public final class AnvilCostFormulas {

    /** Full-repair material count {@code y} from recipe material count {@code x}: {@code y = sqrt(x * 3) - 0.70}. */
    private static final double FULL_REPAIR_SQRT_FACTOR = 3.0;
    private static final double FULL_REPAIR_SQRT_OFFSET = 0.70;

    private final PowerLevelConfig config;

    public AnvilCostFormulas(PowerLevelConfig config) {
        this.config = config;
    }

    /**
     * XP level cost for enchant/combine operations.
     * Uses total power value (material + enchantment) of the resulting item.
     * Formula: floor(minCost + (maxCost - minCost) / (1 + e^(-k × (totalPL - x0)))),
     * clamped to [{@code minCost}, {@code maxCost}]. Floor (not ceil) so the left
     * asymptote of 1 displays as 1 instead of 2.
     */
    public int enchantCostFromPL(int totalPL) {
        int minCost = config.getEnchantCostMin();
        int maxCost = config.getEnchantCostMax();
        int x0 = config.getEnchantCostX0();
        double k = config.getEnchantCostK();
        double exp = Math.exp(-k * (totalPL - x0));
        double cost = minCost + (maxCost - minCost) / (1.0 + exp);
        int levels = (int) Math.floor(cost + 1e-9);
        return Math.max(minCost, Math.min(maxCost, levels));
    }

    /**
     * Durability repair factor (multiplier of vanilla repair quantity).
     * Uses enchantment power value only.
     * Formula: minRepair + (maxRepair - minRepair) / (1 + e^(k × (enchantmentPowerValue - x0)))
     */
    public double durabilityRepairFactor(int enchantmentPowerValue) {
        double minRepair = config.getRepairMin();
        double maxRepair = config.getRepairMax();
        int x0 = config.getRepairMidpoint();
        double k = config.getRepairSteepness();
        double exp = Math.exp(k * (enchantmentPowerValue - x0));
        return minRepair + (maxRepair - minRepair) / (1.0 + exp);
    }

    /**
     * Materials to restore 100% durability (unenchanted), from recipe material count {@code x}.
     */
    public double fullRepairMaterialCost(int itemCraftCost) {
        int x = Math.max(1, itemCraftCost);
        return Math.sqrt(x * FULL_REPAIR_SQRT_FACTOR) - FULL_REPAIR_SQRT_OFFSET;
    }

    /**
     * Materials needed to repair the current damage (before enchant efficacy).
     */
    public double materialsForCurrentDamage(int itemCraftCost, int damage, int maxDamage) {
        if (damage <= 0 || maxDamage <= 0) {
            return 0;
        }
        return fullRepairMaterialCost(itemCraftCost) * (damage / (double) maxDamage);
    }

    /**
     * Whole materials consumed this repair: everything offered, up to the ceiling needed for a full fix.
     * Efficacy is fractional ({@code materials / yFull} of max durability); 4 mats on a 4.20-y chestplate
     * does not fully repair.
     */
    public int materialRepairCount(int itemCraftCost, int damage, int maxDamage, int availableMaterials) {
        if (damage <= 0 || availableMaterials <= 0) {
            return 0;
        }
        double materialsForFullFix = materialsForCurrentDamage(itemCraftCost, damage, maxDamage);
        int toFullyRepair = (int) Math.ceil(materialsForFullFix - 1e-9);
        int count = Math.min(availableMaterials, toFullyRepair);
        return Math.max(1, count);
    }

    /**
     * Durability restored after material efficacy and enchant penalty (single float step, no double-floor).
     */
    public double durabilityRestoredAfterEnchant(
            int itemCraftCost,
            int materialCount,
            ItemStack item,
            double enchantEfficacyFactor
    ) {
        if (materialCount <= 0 || !item.isDamageableItem() || enchantEfficacyFactor <= 0) {
            return 0;
        }
        int damage = item.getDamageValue();
        int maxDamage = item.getMaxDamage();
        if (damage <= 0 || maxDamage <= 0) {
            return 0;
        }
        double yFull = fullRepairMaterialCost(itemCraftCost);
        double restored = (materialCount / yFull) * maxDamage * enchantEfficacyFactor;
        return Math.min(damage, restored);
    }

    /**
     * Damage value after material repair; snaps tiny leftovers from rounding so the bar can read 100%.
     */
    public int damageAfterMaterialRepair(
            int itemCraftCost,
            int materialCount,
            ItemStack item,
            double enchantEfficacyFactor
    ) {
        int damage = item.getDamageValue();
        int maxDamage = item.getMaxDamage();
        if (damage <= 0) {
            return 0;
        }
        double restored = durabilityRestoredAfterEnchant(
                itemCraftCost,
                materialCount,
                item,
                enchantEfficacyFactor
        );
        int newDamage = Math.max(0, damage - (int) Math.round(restored));
        int snapThreshold = Math.max(1, (int) Math.floor(maxDamage * 0.005 + 0.5));
        if (newDamage > 0 && newDamage <= snapThreshold) {
            return 0;
        }
        return newDamage;
    }
}
