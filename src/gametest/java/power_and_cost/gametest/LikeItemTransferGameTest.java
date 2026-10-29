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

public class LikeItemTransferGameTest {

	private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
	private static final PowerLevelCalculator CALCULATOR = new PowerLevelCalculator(CONFIG);
	private static final AnvilCostFormulas FORMULAS = new AnvilCostFormulas(CONFIG);

	@GameTest(maxTicks = 200)
	public void enchantedIronPickaxeTransfersOntoDiamond(GameTestHelper context) {
		AnvilMenu menu = AnvilGameTestHelper.openAnvilMenu(context);

		ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
		AnvilGameTestHelper.enchant(context, iron, Enchantments.EFFICIENCY, 5);
		ItemStack diamond = new ItemStack(Items.DIAMOND_PICKAXE);
		diamond.setDamageValue(40);
		AnvilGameTestHelper.populateAnvil(menu, iron, diamond, null);

		context.succeedWhen(() -> assertDiamondReceivesEfficiency(context, menu, 40));
	}

	@GameTest(maxTicks = 200)
	public void diamondOnTheLeftStillReceivesIronEnchantments(GameTestHelper context) {
		AnvilMenu menu = AnvilGameTestHelper.openAnvilMenu(context);

		ItemStack diamond = new ItemStack(Items.DIAMOND_PICKAXE);
		ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
		AnvilGameTestHelper.enchant(context, iron, Enchantments.EFFICIENCY, 5);
		AnvilGameTestHelper.populateAnvil(menu, diamond, iron, null);

		context.succeedWhen(() -> assertDiamondReceivesEfficiency(context, menu, 0));
	}

	@GameTest(maxTicks = 200)
	public void differentKindsDoNotCombine(GameTestHelper context) {
		AnvilMenu menu = AnvilGameTestHelper.openAnvilMenu(context);

		ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
		AnvilGameTestHelper.enchant(context, pickaxe, Enchantments.EFFICIENCY, 5);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		AnvilGameTestHelper.populateAnvil(menu, pickaxe, sword, null);

		context.succeedWhen(() -> {
			if (!AnvilGameTestHelper.resultStack(menu).isEmpty()) {
				throw AnvilGameTestHelper.fail("A pickaxe and a sword should not combine");
			}
		});
	}

	@GameTest(maxTicks = 200)
	public void unenchantedLikeItemsDoNotCombine(GameTestHelper context) {
		AnvilMenu menu = AnvilGameTestHelper.openAnvilMenu(context);
		AnvilGameTestHelper.populateAnvil(
				menu,
				new ItemStack(Items.IRON_PICKAXE),
				new ItemStack(Items.DIAMOND_PICKAXE),
				null
		);

		context.succeedWhen(() -> {
			if (!AnvilGameTestHelper.resultStack(menu).isEmpty()) {
				throw AnvilGameTestHelper.fail("Two unenchanted pickaxes should not combine");
			}
		});
	}

	private static void assertDiamondReceivesEfficiency(GameTestHelper context, AnvilMenu menu, int expectedDamage) {
		ItemStack result = AnvilGameTestHelper.resultStack(menu);
		if (!result.is(Items.DIAMOND_PICKAXE)) {
			throw AnvilGameTestHelper.fail("Expected a diamond pickaxe, got " + result);
		}
		int efficiency = enchantLevel(context, result, Enchantments.EFFICIENCY);
		if (efficiency != 5) {
			throw AnvilGameTestHelper.fail("Expected Efficiency 5 on the diamond pickaxe, got " + efficiency);
		}
		if (result.getDamageValue() != expectedDamage) {
			throw AnvilGameTestHelper.fail(
					"Expected the diamond pickaxe to keep damage " + expectedDamage + ", got " + result.getDamageValue()
			);
		}
		int expectedCost = FORMULAS.enchantCostFromPL(CALCULATOR.computeTotalPowerValue(result));
		if (menu.getCost() != expectedCost) {
			throw AnvilGameTestHelper.fail(
					"Expected transfer cost " + expectedCost + ", got " + menu.getCost()
			);
		}
	}

	private static int enchantLevel(GameTestHelper context, ItemStack stack, ResourceKey<Enchantment> key) {
		Holder<Enchantment> holder = context.getLevel().registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(key);
		return stack.getEnchantments().getLevel(holder);
	}
}
