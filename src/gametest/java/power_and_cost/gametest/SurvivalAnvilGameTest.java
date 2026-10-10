package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import power_and_cost.power.PowerLevelConfig;

import java.lang.reflect.Method;

public class SurvivalAnvilGameTest {

    private static final String RENAMED = "Renamed Sword";
    private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
    private static final PowerLevelCalculator CALCULATOR = new PowerLevelCalculator(CONFIG);
    private static final AnvilCostFormulas FORMULAS = new AnvilCostFormulas(CONFIG);

    @GameTest(maxTicks = 200)
    public void survivalHighPlCombineKeepsResult(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        session.player().getAbilities().instabuild = false;

        AnvilMenu menu = session.menu();
        ItemStack left = new ItemStack(Items.NETHERITE_SWORD);
        AnvilGameTestHelper.enchant(context, left, Enchantments.SHARPNESS, 5);
        AnvilGameTestHelper.enchant(context, left, Enchantments.LOOTING, 3);
        AnvilGameTestHelper.enchant(context, left, Enchantments.UNBREAKING, 3);
        AnvilGameTestHelper.enchant(context, left, Enchantments.FIRE_ASPECT, 2);
        AnvilGameTestHelper.enchant(context, left, Enchantments.SWEEPING_EDGE, 3);
        AnvilGameTestHelper.enchant(context, left, Enchantments.KNOCKBACK, 2);

        ItemStack book = AnvilGameTestHelper.enchantedBook(context, Enchantments.MENDING, 1);
        AnvilGameTestHelper.populateAnvil(menu, left, book, null);

        context.succeedWhen(() -> {
            if (session.player().hasInfiniteMaterials()) {
                throw AnvilGameTestHelper.fail("Expected a survival player without instabuild");
            }
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail(
                        "Expected high-PL book combine result for a survival player, cost was " + menu.getCost()
                );
            }
            if (enchantLevel(context, result, Enchantments.MENDING) != 1) {
                throw AnvilGameTestHelper.fail("Expected the survival result to keep Mending");
            }
            int resultPl = CALCULATOR.computeTotalPowerValue(result);
            int expectedCost = FORMULAS.enchantCostFromPL(resultPl);
            if (menu.getCost() != expectedCost) {
                throw AnvilGameTestHelper.fail(
                        "Expected survival cost " + expectedCost + " from result PL " + resultPl
                                + ", got " + menu.getCost()
                );
            }
            if (menu.getCost() <= 40) {
                throw AnvilGameTestHelper.fail(
                        "Expected survival cost above vanilla 40-level cap, got " + menu.getCost()
                );
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void survivalRejectsIncompatibleBook(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        session.player().getAbilities().instabuild = false;

        AnvilMenu menu = session.menu();
        ItemStack left = new ItemStack(Items.NETHERITE_SWORD);
        ItemStack book = AnvilGameTestHelper.enchantedBook(context, Enchantments.EFFICIENCY, 5);
        AnvilGameTestHelper.populateAnvil(menu, left, book, null);

        context.succeedWhen(() -> {
            if (session.player().hasInfiniteMaterials()) {
                throw AnvilGameTestHelper.fail("Expected a survival player without instabuild");
            }
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (!result.isEmpty()) {
                throw AnvilGameTestHelper.fail(
                        "Survival applied Efficiency to a sword, cost was " + menu.getCost()
                );
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void survivalTakePaysLevels(GameTestHelper context) {
        AnvilGameTestHelper.AnvilSession session = AnvilGameTestHelper.openAnvil(context);
        session.player().getAbilities().instabuild = false;
        session.player().experienceLevel = 5;

        AnvilMenu menu = session.menu();
        ItemStack left = new ItemStack(Items.IRON_SWORD);
        AnvilGameTestHelper.populateAnvil(menu, left, ItemStack.EMPTY, RENAMED);

        if (session.player().hasInfiniteMaterials()) {
            throw AnvilGameTestHelper.fail("Expected a survival player without instabuild");
        }
        ItemStack result = AnvilGameTestHelper.resultStack(menu);
        if (result.isEmpty()) {
            throw AnvilGameTestHelper.fail("Expected rename result");
        }
        if (menu.getCost() != 1) {
            throw AnvilGameTestHelper.fail("Expected rename cost of 1 level, got " + menu.getCost());
        }
        if (!RENAMED.equals(result.getHoverName().getString())) {
            throw AnvilGameTestHelper.fail("Expected result named " + RENAMED + ", got " + result.getHoverName().getString());
        }

        int levelsBefore = session.player().experienceLevel;
        ItemStack taken = result.copy();
        takeResult(menu, session.player(), taken);
        if (session.player().experienceLevel != levelsBefore - 1) {
            throw AnvilGameTestHelper.fail(
                    "Expected levels to drop by 1 from " + levelsBefore + ", got " + session.player().experienceLevel
            );
        }
        if (!RENAMED.equals(taken.getHoverName().getString())) {
            throw AnvilGameTestHelper.fail("Taken result lost its name: " + taken.getHoverName().getString());
        }
        context.succeed();
    }

    private static int enchantLevel(GameTestHelper context, ItemStack stack, ResourceKey<Enchantment> key) {
        Holder<Enchantment> holder = context.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return stack.getEnchantments().getLevel(holder);
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
