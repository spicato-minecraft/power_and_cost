package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import power_and_cost.power.PowerLevelConfig;

import java.lang.reflect.Method;

public class MaterialRepairGameTest {

    private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
    private static final PowerLevelCalculator CALCULATOR = new PowerLevelCalculator(CONFIG);
    private static final AnvilCostFormulas FORMULAS = new AnvilCostFormulas(CONFIG);

    @GameTest(maxTicks = 200)
    public void materialRepairCostsOneLevelOneMaterial(GameTestHelper context) {
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
            if (menu.getCost() != 1) {
                throw AnvilGameTestHelper.fail("Expected material repair cost 1, got " + menu.getCost());
            }

            int vanillaRestored = Math.min(left.getDamageValue(), left.getMaxDamage() / 4);
            double factor = FORMULAS.durabilityRepairFactor(CALCULATOR.computeEnchantmentPowerValue(left));
            int restored = (int) Math.round(vanillaRestored * factor);
            int expectedDamage = Math.max(0, left.getDamageValue() - restored);
            int maxDamage = result.getMaxDamage();
            if (expectedDamage > 0 && expectedDamage <= maxDamage * 0.05) {
                expectedDamage = 0;
            }
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
