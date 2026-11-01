package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import power_and_cost.anvil.NetheriteRepairMaterials;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import power_and_cost.power.PowerLevelConfig;

import java.lang.reflect.Method;

public class MaterialRepairGameTest {

    private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
    private static final PowerLevelCalculator CALCULATOR = new PowerLevelCalculator(CONFIG);
    private static final AnvilCostFormulas FORMULAS = new AnvilCostFormulas(CONFIG);

    @GameTest(maxTicks = 200)
    public void materialRepairCostsNoXpLevels(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        AnvilMenu menu = session.menu();

        ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
        left.setDamageValue(800);
        AnvilGameTestHelper.enchant(context, left, Enchantments.SHARPNESS, 5);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        AnvilGameTestHelper.populateAnvil(menu, left, diamond, null);

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected a material repair result");
            }
            if (menu.getCost() != 0) {
                throw AnvilGameTestHelper.fail("Expected no XP cost for material repair, got " + menu.getCost());
            }

            int craftCost = CALCULATOR.computeItemCraftCost(left);
            int materials = reflectRepairItemCountCost(menu);
            double factor = FORMULAS.durabilityRepairFactor(CALCULATOR.computeEnchantmentPowerValue(left));
            int expectedDamage = FORMULAS.damageAfterMaterialRepair(craftCost, materials, left, factor);
            if (result.getDamageValue() != expectedDamage) {
                throw AnvilGameTestHelper.fail(
                        "Expected repaired damage " + expectedDamage + " (factor " + factor + "), got "
                                + result.getDamageValue()
                );
            }

            session.player().getAbilities().instabuild = true;
            takeResult(menu, session.player(), result.copy());
            ItemStack remaining = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
            if (!remaining.isEmpty()) {
                throw AnvilGameTestHelper.fail(
                        "Expected the diamond to be consumed, still holding " + remaining.getCount()
                );
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void materialRepairScalesIngotsByCraftCost(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        AnvilMenu menu = session.menu();

        ItemStack left = new ItemStack(Items.IRON_CHESTPLATE);
        left.setDamageValue((int) (left.getMaxDamage() * 0.9));
        ItemStack ingots = new ItemStack(Items.IRON_INGOT, 20);
        AnvilGameTestHelper.populateAnvil(menu, left, ingots, null);

        int expectedMaterials = FORMULAS.materialRepairCount(
                CALCULATOR.computeItemCraftCost(left),
                left.getDamageValue(),
                left.getMaxDamage(),
                ingots.getCount(),
                FORMULAS.durabilityRepairFactor(0)
        );

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected a material repair result");
            }
            if (menu.getCost() != 0) {
                throw AnvilGameTestHelper.fail("Expected no XP cost, got " + menu.getCost());
            }

            session.player().getAbilities().instabuild = true;
            takeResult(menu, session.player(), result.copy());
            ItemStack remaining = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
            int expectedRemaining = ingots.getCount() - expectedMaterials;
            if (remaining.getCount() != expectedRemaining) {
                throw AnvilGameTestHelper.fail(
                        "Expected " + expectedRemaining + " ingots left after consuming " + expectedMaterials
                                + ", still holding " + remaining.getCount()
                );
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void chestplateMaterialRepairUsesMoreIngotsThanShovel(GameTestHelper context) {
        int damage = 100;

        AnvilGameTestHelper.AnvilSession shovelSession = AnvilGameTestHelper.openAnvil(context);
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        shovel.setDamageValue(damage);
        AnvilGameTestHelper.populateAnvil(
                shovelSession.menu(),
                shovel,
                new ItemStack(Items.IRON_INGOT, 16),
                null
        );

        AnvilGameTestHelper.AnvilSession chestSession = AnvilGameTestHelper.openAnvil(context);
        ItemStack chestplate = new ItemStack(Items.IRON_CHESTPLATE);
        chestplate.setDamageValue(damage);
        AnvilGameTestHelper.populateAnvil(
                chestSession.menu(),
                chestplate,
                new ItemStack(Items.IRON_INGOT, 16),
                null
        );

        context.succeedWhen(() -> {
            if (AnvilGameTestHelper.resultStack(shovelSession.menu()).isEmpty()
                    || AnvilGameTestHelper.resultStack(chestSession.menu()).isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected both material repair results");
            }
            int shovelMaterials = reflectRepairItemCountCost(shovelSession.menu());
            int chestMaterials = reflectRepairItemCountCost(chestSession.menu());
            if (shovelMaterials <= 0 || chestMaterials <= shovelMaterials) {
                throw AnvilGameTestHelper.fail(
                        "Expected chestplate to consume more materials than shovel, got chest="
                                + chestMaterials + " shovel=" + shovelMaterials
                );
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void extraDiamondsFinishRepairOneDiamondLeftShort(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        AnvilMenu menu = session.menu();

        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        AnvilGameTestHelper.enchant(context, pick, Enchantments.FORTUNE, 1);
        int max = pick.getMaxDamage();
        int craftCost = CALCULATOR.computeItemCraftCost(pick);
        double yFull = FORMULAS.fullRepairMaterialCost(craftCost);
        double factor = FORMULAS.durabilityRepairFactor(CALCULATOR.computeEnchantmentPowerValue(pick));
        int damage = (int) Math.floor(max / yFull) - 1;
        pick.setDamageValue(damage);
        if (FORMULAS.damageAfterMaterialRepair(craftCost, 1, pick, factor) <= 0) {
            throw AnvilGameTestHelper.fail("Test setup: one diamond already fully repairs this pick");
        }

        AnvilGameTestHelper.populateAnvil(menu, pick, new ItemStack(Items.DIAMOND, 64), null);

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected a material repair result");
            }
            if (reflectRepairItemCountCost(menu) < 2) {
                throw AnvilGameTestHelper.fail(
                        "Expected a second diamond to be used, consumed " + reflectRepairItemCountCost(menu)
                );
            }
            if (result.getDamageValue() != 0) {
                throw AnvilGameTestHelper.fail(
                        "A stack of diamonds should finish the repair, result damage " + result.getDamageValue()
                );
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void fourDiamondsDoesNotFullyRepairChestplate(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        AnvilMenu menu = session.menu();

        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        int max = chestplate.getMaxDamage();
        chestplate.setDamageValue(max - Math.max(1, max / 100));
        AnvilGameTestHelper.populateAnvil(menu, chestplate, new ItemStack(Items.DIAMOND, 4), null);

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected a material repair result");
            }
            if (reflectRepairItemCountCost(menu) != 4) {
                throw AnvilGameTestHelper.fail("Expected to consume 4 diamonds");
            }
            if (result.getDamageValue() <= 0) {
                throw AnvilGameTestHelper.fail("Four diamonds should not fully repair a nearly broken chestplate");
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void netheriteChestplateDiamondsNeedTwiceTheCurrentRepair(GameTestHelper context) {
        expectNetheriteRepair(context, Items.DIAMOND, NetheriteRepairMaterials.DIAMOND_EFFICACY);
    }

    @GameTest(maxTicks = 200)
    public void netheriteChestplateScrapsNeedHalfTheCurrentRepair(GameTestHelper context) {
        expectNetheriteRepair(context, Items.NETHERITE_SCRAP, NetheriteRepairMaterials.SCRAP_EFFICACY);
    }

    @GameTest(maxTicks = 200)
    public void netheriteIngotRepairStaysOnTheCurrentRate(GameTestHelper context) {
        expectNetheriteRepair(context, Items.NETHERITE_INGOT, 1.0);
    }

    @GameTest(maxTicks = 200)
    public void survivalMaterialRepairDoesNotSpendLevels(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        session.player().getAbilities().instabuild = false;
        session.player().experienceLevel = 0;

        ItemStack left = new ItemStack(Items.IRON_SHOVEL);
        left.setDamageValue(left.getMaxDamage() / 2);
        AnvilGameTestHelper.populateAnvil(session.menu(), left, new ItemStack(Items.IRON_INGOT), null);

        context.succeedWhen(() -> {
            AnvilMenu menu = session.menu();
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Survival player could not preview material repair");
            }
            if (menu.getCost() != 0) {
                throw AnvilGameTestHelper.fail("Expected zero XP cost, got " + menu.getCost());
            }
            takeResult(menu, session.player(), result.copy());
            if (session.player().experienceLevel != 0) {
                throw AnvilGameTestHelper.fail(
                        "Expected no levels spent, now at " + session.player().experienceLevel
                );
            }
        });
    }

    private void expectNetheriteRepair(GameTestHelper context, Item material, double efficacy) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        AnvilMenu menu = session.menu();

        ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
        int damage = (int) (chestplate.getMaxDamage() * 0.9);
        chestplate.setDamageValue(damage);
        ItemStack offered = new ItemStack(material, 64);
        AnvilGameTestHelper.populateAnvil(menu, chestplate, offered, null);

        int craftCost = CALCULATOR.computeItemCraftCost(chestplate);
        double factor = FORMULAS.durabilityRepairFactor(0);
        int expectedMaterials = FORMULAS.materialRepairCount(
                craftCost,
                damage,
                chestplate.getMaxDamage(),
                offered.getCount(),
                factor,
                efficacy
        );
        int currentRateMaterials = FORMULAS.materialRepairCount(
                craftCost,
                damage,
                chestplate.getMaxDamage(),
                offered.getCount(),
                factor
        );
        int expectedDamage = FORMULAS.damageAfterMaterialRepair(
                craftCost,
                expectedMaterials,
                chestplate,
                factor,
                efficacy
        );

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty() || !result.is(Items.NETHERITE_CHESTPLATE)) {
                throw AnvilGameTestHelper.fail("Expected a repaired netherite chestplate, got " + result);
            }
            if (menu.getCost() != 0) {
                throw AnvilGameTestHelper.fail("Expected no XP cost, got " + menu.getCost());
            }
            int consumed = reflectRepairItemCountCost(menu);
            if (consumed != expectedMaterials) {
                throw AnvilGameTestHelper.fail(
                        "Expected to consume " + expectedMaterials + " at efficacy " + efficacy
                                + " (current rate uses " + currentRateMaterials + "), got " + consumed
                );
            }
            if (efficacy < 1.0 && consumed <= currentRateMaterials) {
                throw AnvilGameTestHelper.fail(
                        "Diamond repair should cost more materials than the current rate, got " + consumed
                                + " vs " + currentRateMaterials
                );
            }
            if (efficacy > 1.0 && consumed >= currentRateMaterials) {
                throw AnvilGameTestHelper.fail(
                        "Scrap repair should cost fewer materials than the current rate, got " + consumed
                                + " vs " + currentRateMaterials
                );
            }
            if (result.getDamageValue() != expectedDamage) {
                throw AnvilGameTestHelper.fail(
                        "Expected repaired damage " + expectedDamage + ", got " + result.getDamageValue()
                );
            }
        });
    }

    private static int reflectRepairItemCountCost(AnvilMenu menu) {
        try {
            var field = AnvilMenu.class.getDeclaredField("repairItemCountCost");
            field.setAccessible(true);
            return field.getInt(menu);
        } catch (ReflectiveOperationException e) {
            throw AnvilGameTestHelper.fail("Could not read repairItemCountCost: " + e);
        }
    }

    private static void takeResult(AnvilMenu menu, Player player, ItemStack result) {
        try {
            Method onTake = AnvilMenu.class.getDeclaredMethod("onTake", Player.class, ItemStack.class);
            onTake.setAccessible(true);
            onTake.invoke(menu, player, result);
        } catch (ReflectiveOperationException e) {
            throw AnvilGameTestHelper.fail("Could not take the anvil result: " + e);
        }
    }
}
