package power_and_cost.power;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Computes material power value and enchantment power value for item stacks.
 * Uses item registry ID lookup (compatible with Minecraft 1.21.10).
 */
public final class PowerLevelCalculator {

    private final PowerLevelConfig config;

    public PowerLevelCalculator(PowerLevelConfig config) {
        this.config = config;
    }

    /**
     * Material power value from item registry ID (tiered tools, armor, special items).
     */
    public int computeMaterialPowerValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return config.getItemPower(itemId);
    }

    /** Craft recipe material count used to scale anvil material repair cost. */
    public int computeItemCraftCost(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return config.getItemCraftCost(itemId);
    }

    /**
     * Enchantment power value only (excludes material).
     */
    public int computeEnchantmentPowerValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        int total = 0;
        for (var holder : enchantments.keySet()) {
            int level = enchantments.getLevel(holder);
            total += config.getEnchantmentPower(holder, level);
        }
        return total;
    }

    /**
     * Total power value (material + enchantment) for enchant/combine cost.
     */
    public int computeTotalPowerValue(ItemStack stack) {
        return computeMaterialPowerValue(stack) + computeEnchantmentPowerValue(stack);
    }

    /**
     * Power contributed by one offered enchantment (table button or book).
     */
    public int computeEnchantmentPower(Holder<Enchantment> holder, int level) {
        return config.getEnchantmentPower(holder, level);
    }
}
