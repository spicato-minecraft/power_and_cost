package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
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

    @GameTest(maxTicks = 200)
    public void sameItemCombineTransfersEnchantments(GameTestHelper context) {
        AnvilMenu menu = AnvilGameTestHelper.openAnvilMenu(context);

        ItemStack left = new ItemStack(Items.DIAMOND_SWORD);
        AnvilGameTestHelper.enchant(context, left, Enchantments.SHARPNESS, 5);
        ItemStack right = new ItemStack(Items.DIAMOND_SWORD);
        AnvilGameTestHelper.enchant(context, right, Enchantments.UNBREAKING, 3);
        AnvilGameTestHelper.populateAnvil(menu, left, right, null);

        context.succeedWhen(() -> {
            ItemStack result = AnvilGameTestHelper.resultStack(menu);
            if (result.isEmpty()) {
                throw AnvilGameTestHelper.fail("Expected a same-item combine result");
            }
            int sharpness = enchantLevel(context, result, Enchantments.SHARPNESS);
            int unbreaking = enchantLevel(context, result, Enchantments.UNBREAKING);
            if (sharpness != 5 || unbreaking != 3) {
                throw AnvilGameTestHelper.fail(
                        "Expected Sharpness 5 and Unbreaking 3 on the result, got Sharpness "
                                + sharpness + " and Unbreaking " + unbreaking
                );
            }
        });
    }

    private static int enchantLevel(
            GameTestHelper context,
            ItemStack stack,
            ResourceKey<Enchantment> key
    ) {
        Holder<Enchantment> holder = context.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return stack.getEnchantments().getLevel(holder);
    }
}
