package power_and_cost.anvil;

import net.minecraft.core.Holder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import power_and_cost.power.PowerLevelCalculator;

/**
 * Moves enchantments between two different items of the same kind, such as an iron pickaxe
 * and a diamond pickaxe. The higher-tier item is kept.
 */
public final class LikeItemTransfer {

	private static final TagKey<Item>[] LIKE_GROUPS = likeGroups();

	private LikeItemTransfer() {
	}

	public static boolean areLike(ItemStack left, ItemStack right) {
		if (left.isEmpty() || right.isEmpty() || left.getItem() == right.getItem()) {
			return false;
		}
		if (left.is(Items.ENCHANTED_BOOK) || right.is(Items.ENCHANTED_BOOK)) {
			return false;
		}
		for (TagKey<Item> group : LIKE_GROUPS) {
			if (left.is(group) && right.is(group)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the higher-tier item with the other item's compatible enchantments applied,
	 * or empty when nothing can move
	 */
	public static ItemStack transfer(ItemStack left, ItemStack right, PowerLevelCalculator calculator) {
		if (!areLike(left, right)) {
			return ItemStack.EMPTY;
		}

		ItemStack kept = keptItem(left, right, calculator);
		ItemStack donor = kept == left ? right : left;
		ItemStack result = kept.copy();
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
				EnchantmentHelper.getEnchantmentsForCrafting(result)
		);
		boolean changed = false;
		ItemEnchantments donated = EnchantmentHelper.getEnchantmentsForCrafting(donor);
		for (Holder<Enchantment> enchantment : donated.keySet()) {
			if (applyEnchantment(result, enchantments, enchantment, donated.getLevel(enchantment))) {
				changed = true;
			}
		}
		if (!changed) {
			return ItemStack.EMPTY;
		}
		EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
		return result;
	}

	private static boolean applyEnchantment(
			ItemStack result,
			ItemEnchantments.Mutable enchantments,
			Holder<Enchantment> enchantment,
			int donorLevel
	) {
		if (donorLevel <= 0 || !enchantment.value().canEnchant(result)) {
			return false;
		}
		if (!compatibleWith(enchantments, enchantment)) {
			return false;
		}

		int current = enchantments.getLevel(enchantment);
		int merged = current == donorLevel ? donorLevel + 1 : Math.max(current, donorLevel);
		merged = Math.min(merged, enchantment.value().getMaxLevel());
		if (merged <= current) {
			return false;
		}
		enchantments.set(enchantment, merged);
		return true;
	}

	private static boolean compatibleWith(ItemEnchantments.Mutable enchantments, Holder<Enchantment> candidate) {
		for (Holder<Enchantment> existing : enchantments.keySet()) {
			if (!existing.equals(candidate) && !Enchantment.areCompatible(candidate, existing)) {
				return false;
			}
		}
		return true;
	}

	private static ItemStack keptItem(ItemStack left, ItemStack right, PowerLevelCalculator calculator) {
		int leftPower = calculator.computeMaterialPowerValue(left);
		int rightPower = calculator.computeMaterialPowerValue(right);
		return leftPower >= rightPower ? left : right;
	}

	@SuppressWarnings("unchecked")
	private static TagKey<Item>[] likeGroups() {
		return new TagKey[] {
				ItemTags.PICKAXES,
				ItemTags.AXES,
				ItemTags.SHOVELS,
				ItemTags.HOES,
				ItemTags.SWORDS,
				ItemTags.HEAD_ARMOR,
				ItemTags.CHEST_ARMOR,
				ItemTags.LEG_ARMOR,
				ItemTags.FOOT_ARMOR
		};
	}
}
