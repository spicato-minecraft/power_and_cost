package power_and_cost.anvil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Extra anvil repair materials for netherite gear.
 * Diamonds restore half the usual durability per unit. Netherite scraps restore twice as much.
 */
public final class NetheriteRepairMaterials {

    /** Each diamond restores half of the recipe-size repair rate. */
    public static final double DIAMOND_EFFICACY = 0.5;
    /** Each netherite scrap restores twice the recipe-size repair rate. */
    public static final double SCRAP_EFFICACY = 2.0;

    private NetheriteRepairMaterials() {
    }

    public static boolean isAlternateMaterial(ItemStack gear, ItemStack material) {
        return efficacy(gear, material) > 0;
    }

    /**
     * @return the efficacy multiplier for a diamond or netherite scrap on netherite gear, otherwise {@code 0}
     */
    public static double efficacy(ItemStack gear, ItemStack material) {
        if (!isNetheriteGear(gear) || material.isEmpty()) {
            return 0;
        }
        if (material.is(Items.DIAMOND)) {
            return DIAMOND_EFFICACY;
        }
        if (material.is(Items.NETHERITE_SCRAP)) {
            return SCRAP_EFFICACY;
        }
        return 0;
    }

    /**
     * Efficacy for this pair, or {@code 1} when the pair uses the normal repair rate.
     */
    public static double efficacyOrDefault(ItemStack gear, ItemStack material) {
        double efficacy = efficacy(gear, material);
        return efficacy > 0 ? efficacy : 1.0;
    }

    public static boolean isNetheriteGear(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("netherite_");
    }
}
