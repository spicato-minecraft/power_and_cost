package power_and_cost.power;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds material power values, enchantment per-level power values, and sigmoid parameters.
 * Defaults match calculator-output.json.
 * Uses item registry IDs for compatibility with Minecraft 1.21.10 (no Tiers/TieredItem/ArmorItem).
 */
public final class PowerLevelConfig {

    // Material power values by item registry ID (tiered tools, armor, and special items)
    private final Map<String, Integer> itemPowerValues = new HashMap<>();

    // Enchantment per-level power values (registry ID string)
    private final Map<String, Integer> enchantmentPowerValues = new HashMap<>();

    // Enchant cost sigmoid: min 1, max 50, x0 195 (shifted -60 from 255), k 0.025
    private int enchantCostMin = 1;
    private int enchantCostMax = 50;
    private int enchantCostX0 = 195;
    private double enchantCostK = 0.025;

    // Repair sigmoid: min 0.33, max 1.1, midpoint 55, steepness 0.035
    private double repairMin = 0.33;
    private double repairMax = 1.1;
    private int repairMidpoint = 55;
    private double repairSteepness = 0.035;

    private static final int DEFAULT_UNKNOWN_ENCHANT = 4;
    private static final int DEFAULT_UNKNOWN_CRAFT_COST = 1;

    public PowerLevelConfig() {
        loadDefaults();
    }

    private void loadDefaults() {
        // Tiered tools: Wood 10, Stone 20, Copper 25, Gold 30, Iron 40, Diamond 70, Netherite 100
        putItemPower("minecraft:wooden_sword", 10);
        putItemPower("minecraft:wooden_pickaxe", 10);
        putItemPower("minecraft:wooden_axe", 10);
        putItemPower("minecraft:wooden_shovel", 10);
        putItemPower("minecraft:wooden_hoe", 10);
        putItemPower("minecraft:stone_sword", 20);
        putItemPower("minecraft:stone_pickaxe", 20);
        putItemPower("minecraft:stone_axe", 20);
        putItemPower("minecraft:stone_shovel", 20);
        putItemPower("minecraft:stone_hoe", 20);
        putItemPower("minecraft:copper_sword", 25);
        putItemPower("minecraft:copper_pickaxe", 25);
        putItemPower("minecraft:copper_axe", 25);
        putItemPower("minecraft:copper_shovel", 25);
        putItemPower("minecraft:copper_hoe", 25);
        putItemPower("minecraft:golden_sword", 30);
        putItemPower("minecraft:golden_pickaxe", 30);
        putItemPower("minecraft:golden_axe", 30);
        putItemPower("minecraft:golden_shovel", 30);
        putItemPower("minecraft:golden_hoe", 30);
        putItemPower("minecraft:iron_sword", 40);
        putItemPower("minecraft:iron_pickaxe", 40);
        putItemPower("minecraft:iron_axe", 40);
        putItemPower("minecraft:iron_shovel", 40);
        putItemPower("minecraft:iron_hoe", 40);
        putItemPower("minecraft:diamond_sword", 70);
        putItemPower("minecraft:diamond_pickaxe", 70);
        putItemPower("minecraft:diamond_axe", 70);
        putItemPower("minecraft:diamond_shovel", 70);
        putItemPower("minecraft:diamond_hoe", 70);
        putItemPower("minecraft:netherite_sword", 100);
        putItemPower("minecraft:netherite_pickaxe", 100);
        putItemPower("minecraft:netherite_axe", 100);
        putItemPower("minecraft:netherite_shovel", 100);
        putItemPower("minecraft:netherite_hoe", 100);

        // Armor: leather 10, chainmail 20, copper 25, iron 40, gold 30, diamond 70, netherite 100
        putItemPower("minecraft:leather_helmet", 10);
        putItemPower("minecraft:leather_chestplate", 10);
        putItemPower("minecraft:leather_leggings", 10);
        putItemPower("minecraft:leather_boots", 10);
        putItemPower("minecraft:chainmail_helmet", 20);
        putItemPower("minecraft:chainmail_chestplate", 20);
        putItemPower("minecraft:chainmail_leggings", 20);
        putItemPower("minecraft:chainmail_boots", 20);
        putItemPower("minecraft:copper_helmet", 25);
        putItemPower("minecraft:copper_chestplate", 25);
        putItemPower("minecraft:copper_leggings", 25);
        putItemPower("minecraft:copper_boots", 25);
        putItemPower("minecraft:iron_helmet", 40);
        putItemPower("minecraft:iron_chestplate", 40);
        putItemPower("minecraft:iron_leggings", 40);
        putItemPower("minecraft:iron_boots", 40);
        putItemPower("minecraft:golden_helmet", 30);
        putItemPower("minecraft:golden_chestplate", 30);
        putItemPower("minecraft:golden_leggings", 30);
        putItemPower("minecraft:golden_boots", 30);
        putItemPower("minecraft:diamond_helmet", 70);
        putItemPower("minecraft:diamond_chestplate", 70);
        putItemPower("minecraft:diamond_leggings", 70);
        putItemPower("minecraft:diamond_boots", 70);
        putItemPower("minecraft:netherite_helmet", 100);
        putItemPower("minecraft:netherite_chestplate", 100);
        putItemPower("minecraft:netherite_leggings", 100);
        putItemPower("minecraft:netherite_boots", 100);
        putItemPower("minecraft:turtle_helmet", 10);

        // Non-tiered items from calculator-output.json
        putItemPower("minecraft:bow", 70);
        putItemPower("minecraft:crossbow", 40);
        putItemPower("minecraft:trident", 45);
        putItemPower("minecraft:fishing_rod", 20);
        putItemPower("minecraft:shield", 40);
        putItemPower("minecraft:elytra", 55);
        putItemPower("minecraft:shears", 35);
        putItemPower("minecraft:carrot_on_a_stick", 15);
        putItemPower("minecraft:warped_fungus_on_a_stick", 18);
        putItemPower("minecraft:book", 0);
        putItemPower("minecraft:enchanted_book", 0);

        // Enchantment per-level values from calculator-output.json
        // Format: registry ID -> perLevel
        putEnchant("minecraft:sharpness", 14);
        putEnchant("minecraft:smite", 12);
        putEnchant("minecraft:bane_of_arthropods", 12);
        putEnchant("minecraft:breach", 12);
        putEnchant("minecraft:density", 12);
        putEnchant("minecraft:power", 14);
        putEnchant("minecraft:impaling", 12);
        putEnchant("minecraft:efficiency", 10);
        putEnchant("minecraft:unbreaking", 8);
        putEnchant("minecraft:knockback", 4);
        putEnchant("minecraft:fire_aspect", 8);
        putEnchant("minecraft:looting", 10);
        putEnchant("minecraft:sweeping_edge", 8);
        putEnchant("minecraft:flame", 8);
        putEnchant("minecraft:punch", 8);
        putEnchant("minecraft:fortune", 14);
        putEnchant("minecraft:silk_touch", 12);
        putEnchant("minecraft:protection", 14);
        putEnchant("minecraft:fire_protection", 8);
        putEnchant("minecraft:blast_protection", 8);
        putEnchant("minecraft:projectile_protection", 8);
        putEnchant("minecraft:feather_falling", 8);
        putEnchant("minecraft:depth_strider", 8);
        putEnchant("minecraft:frost_walker", 8);
        putEnchant("minecraft:respiration", 8);
        putEnchant("minecraft:aqua_affinity", 4);
        putEnchant("minecraft:thorns", 8);
        putEnchant("minecraft:lure", 8);
        putEnchant("minecraft:luck_of_the_sea", 8);
        putEnchant("minecraft:loyalty", 8);
        putEnchant("minecraft:channeling", 4);
        putEnchant("minecraft:riptide", 8);
        putEnchant("minecraft:quick_charge", 8);
        putEnchant("minecraft:piercing", 8);
        putEnchant("minecraft:multishot", 8);
        putEnchant("minecraft:lunge", 8);
        putEnchant("minecraft:wind_burst", 4);
        putEnchant("minecraft:mending", 4);
        putEnchant("minecraft:infinity", 4);
        putEnchant("minecraft:binding_curse", 4);
        putEnchant("minecraft:vanishing_curse", 4);
        putEnchant("minecraft:soul_speed", 4);
        putEnchant("minecraft:swift_sneak", 4);
    }

    private void putItemPower(String itemId, int power) {
        itemPowerValues.put(itemId, power);
    }

    private void putEnchant(String id, int perLevel) {
        enchantmentPowerValues.put(id, perLevel);
    }

    public int getItemPower(String itemId) {
        return itemPowerValues.getOrDefault(itemId, 0);
    }

    /**
     * Primary material count in the item's crafting recipe (ingot/leather/plank equivalent).
     * Tiered tools and armor infer from registry id when not listed explicitly.
     */
    public int getItemCraftCost(String itemId) {
        return inferCraftCostFromId(itemId);
    }

    private static int inferCraftCostFromId(String itemId) {
        String path = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;
        if (path.endsWith("_shovel")) {
            return 1;
        }
        if (path.endsWith("_hoe") || path.endsWith("_sword")) {
            return 2;
        }
        if (path.endsWith("_pickaxe") || path.endsWith("_axe")) {
            return 3;
        }
        if (path.endsWith("_boots")) {
            return 4;
        }
        if (path.endsWith("_helmet") || path.equals("turtle_helmet")) {
            return 5;
        }
        if (path.endsWith("_leggings")) {
            return 7;
        }
        if (path.endsWith("_chestplate")) {
            return 8;
        }
        return switch (path) {
            case "bow", "crossbow" -> 3;
            case "fishing_rod", "shield", "shears" -> 2;
            case "trident" -> 3;
            case "elytra" -> 8;
            case "carrot_on_a_stick", "warped_fungus_on_a_stick" -> 1;
            default -> DEFAULT_UNKNOWN_CRAFT_COST;
        };
    }

    public int getEnchantmentPower(Holder<Enchantment> holder, int level) {
        String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        return getEnchantmentPower(id, level);
    }

    /**
     * Enchantment power from registry ID string ({@code perLevel × level}).
     * Used by table-offer cost math and unit tests that do not have a Holder.
     */
    public int getEnchantmentPower(String enchantmentId, int level) {
        int perLevel = enchantmentPowerValues.getOrDefault(enchantmentId, DEFAULT_UNKNOWN_ENCHANT);
        return perLevel * level;
    }

    public int getEnchantCostMin() { return enchantCostMin; }
    public int getEnchantCostMax() { return enchantCostMax; }
    public int getEnchantCostX0() { return enchantCostX0; }
    public double getEnchantCostK() { return enchantCostK; }

    public double getRepairMin() { return repairMin; }
    public double getRepairMax() { return repairMax; }
    public int getRepairMidpoint() { return repairMidpoint; }
    public double getRepairSteepness() { return repairSteepness; }

}
