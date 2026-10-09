package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import power_and_cost.power.PowerLevelConfig;

public class SameItemCombineCostGameTest {

    private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
    private static final PowerLevelCalculator CALCULATOR = new PowerLevelCalculator(CONFIG);
    private static final AnvilCostFormulas FORMULAS = new AnvilCostFormulas(CONFIG);

    @GameTest(maxTicks = 200)
    public void sameItemCombineCostsResultPl(GameTestHelper context) {
        AnvilMenu menu = AnvilGameTestHelper.openAnvilMenu(context);

        ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
        left.setDamageValue(200);
        AnvilGameTestHelper.enchant(context, left, Enchantments.SHARPNESS, 5);
        ItemStack right = new ItemStack(Items.DIAMOND_SWORD);
        right.setDamageValue(200);
        AnvilGameTestHelper.populateAnvil(menu, left, right, null);

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected a same-item combine result");
            }
            int resultPl = CALCULATOR.computeTotalPowerValue(result);
            int expectedCost = FORMULAS.enchantCostFromPL(resultPl);
            if (menu.getCost() != expectedCost) {
                throw AnvilGameTestHelper.fail(
                        "Expected same-item cost " + expectedCost + " from result PL " + resultPl
                                + ", got " + menu.getCost()
                );
            }
        });
    }
}
