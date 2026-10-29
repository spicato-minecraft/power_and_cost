package power_and_cost.mixin;

import power_and_cost.PowerAndCost;
import power_and_cost.anvil.LikeItemTransfer;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    private int repairItemCountCost;

    @Shadow
    private String itemName;

    @Unique
    private boolean power_and_cost$applyingTransfer;

    /**
     * Vanilla rolls this at 12%, so a new anvil lasts about 25 uses. God-tier repairs
     * restore about a third of that durability, so the same tool needs about three
     * times as many anvil actions. 4% keeps the expected lifetime near 75 uses.
     */
    @Unique
    private static final float ANVIL_DAMAGE_CHANCE = 0.04f;
    @Unique
    private static final int RENAME_COST = 1;
    @Unique
    private static final int INPUT_SLOT_LEFT = 0;
    @Unique
    private static final int INPUT_SLOT_RIGHT = 1;
    @Unique
    private static final int RESULT_SLOT_INDEX = 2;

    /**
     * Bypasses the vanilla 40-level cost cap. This mod replaces the prior work penalty
     * with a power-level-based economy, so costs can legitimately exceed 40 levels.
     */
    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40))
    private int gear_maintenance_raiseCostCap40(int constant) {
        return Integer.MAX_VALUE;
    }

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 39))
    private int gear_maintenance_raiseCostCap39(int constant) {
        return Integer.MAX_VALUE - 1;
    }

    @ModifyConstant(method = "lambda$onTake$0", constant = @Constant(floatValue = 0.12f))
    private static float power_and_cost$lowerAnvilDamageChance(float vanillaChance) {
        return ANVIL_DAMAGE_CHANCE;
    }

    /**
     * Material-only repairs use {@code repairItemCountCost} but no XP levels ({@code cost == 0}).
     */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void power_and_cost$materialRepairWithoutXpCost(
            Player player,
            boolean hasItem,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!hasItem || this.cost.get() > 0 || this.repairItemCountCost <= 0) {
            return;
        }
        cir.setReturnValue(true);
    }

    /**
     * Intercepts vanilla's cost set so we never write 4997 (prior work penalty).
     * Prevents the cost flash when rapidly changing the name.
     */
    @Unique
    @Redirect(
        method = "createResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V")
    )
    private void power_and_cost_redirectCostSet(DataSlot slot, int vanillaValue) {
        if (slot != cost) {
            slot.set(vanillaValue);
            return;
        }
        int ourCost = computeOurCost();
        slot.set(ourCost);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void power_and_cost$skipReentrantCreateResult(CallbackInfo ci) {
        if (power_and_cost$applyingTransfer) {
            ci.cancel();
        }
    }

    /**
     * Vanilla returns early when the two inputs are different items, before the final
     * return. This has to run on every return so a like-item transfer can still fill
     * the result that early return just cleared.
     */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void gear_maintenance_overrideCostAndDurability(CallbackInfo ci) {
        if (power_and_cost$applyingTransfer) {
            return;
        }

        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(INPUT_SLOT_LEFT).getItem();
        ItemStack right = self.getSlot(INPUT_SLOT_RIGHT).getItem();
        ItemStack result = self.getSlot(RESULT_SLOT_INDEX).getItem();

        if (left.isEmpty()) {
            return;
        }

        if (result.isEmpty()) {
            applyLikeItemTransfer(self, left, right);
            return;
        }

        PowerLevelCalculator calc = PowerAndCost.POWER_LEVEL_CALCULATOR;
        AnvilCostFormulas formulas = new AnvilCostFormulas(PowerAndCost.POWER_LEVEL_CONFIG);
        int leftEnchant = calc.computeEnchantmentPowerValue(left);

        if (right.isEmpty()) {
            repairItemCountCost = 0;
            cost.set(computeOurCost());
            return;
        }

        if (right.is(Items.ENCHANTED_BOOK)) {
            repairItemCountCost = 0;
            cost.set(computeOurCost());
            return;
        }

        if (left.getItem() == right.getItem()) {
            repairItemCountCost = 0;
            double factor = formulas.durabilityRepairFactor(leftEnchant);
            scaleResultDurability(left, result, factor);
            return;
        }

        if (isRepairMaterial(left, right)) {
            int craftCost = calc.computeItemCraftCost(left);
            int materialCount = formulas.materialRepairCount(
                    craftCost,
                    left.getDamageValue(),
                    left.getMaxDamage(),
                    right.getCount()
            );
            repairItemCountCost = materialCount;
            double factor = formulas.durabilityRepairFactor(leftEnchant);
            applyMaterialRepairDurability(left, result, craftCost, materialCount, factor);
            cost.set(computeOurCost());
            return;
        }

        // Fallback: repairItemCountCost left at default
    }

    @Unique
    private int computeOurCost() {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(INPUT_SLOT_LEFT).getItem();
        ItemStack right = self.getSlot(INPUT_SLOT_RIGHT).getItem();
        ItemStack result = self.getSlot(RESULT_SLOT_INDEX).getItem();

        if (left.isEmpty() || result.isEmpty()) {
            return 0;
        }

        PowerLevelCalculator calc = PowerAndCost.POWER_LEVEL_CALCULATOR;
        AnvilCostFormulas formulas = new AnvilCostFormulas(PowerAndCost.POWER_LEVEL_CONFIG);
        int renameCost = getRenameCost(left, right, result);

        if (right.isEmpty()) {
            return renameCost;
        }

        if (right.is(Items.ENCHANTED_BOOK)) {
            int resultPL = calc.computeMaterialPowerValue(result) + calc.computeEnchantmentPowerValue(result);
            return formulas.enchantCostFromPL(resultPL) + renameCost;
        }

        if (left.getItem() == right.getItem() || LikeItemTransfer.areLike(left, right)) {
            int resultPL = calc.computeMaterialPowerValue(result) + calc.computeEnchantmentPowerValue(result);
            return formulas.enchantCostFromPL(resultPL) + renameCost;
        }

        if (isRepairMaterial(left, right)) {
            return renameCost;
        }

        return 1 + renameCost;
    }

    @Unique
    private static boolean isRepairMaterial(ItemStack left, ItemStack right) {
        return left.isDamageableItem() && left.isValidRepairItem(right);
    }

    @Unique
    private int getRenameCost(ItemStack left, ItemStack right, ItemStack result) {
        if (LikeItemTransfer.areLike(left, result)) {
            return anvilRenameCost(left, right);
        }
        return !Objects.equals(left.getHoverName().getString(), result.getHoverName().getString())
                ? RENAME_COST : 0;
    }

    /**
     * The anvil name field is filled with an input's name. That is not a rename.
     * Charge only when the field is a name the player actually typed.
     */
    @Unique
    private int anvilRenameCost(ItemStack left, ItemStack right) {
        if (!isPlayerRename(left, right)) {
            return 0;
        }
        return RENAME_COST;
    }

    @Unique
    private boolean isPlayerRename(ItemStack left, ItemStack right) {
        if (itemName == null || StringUtil.isBlank(itemName)) {
            return false;
        }
        if (itemName.equals(left.getHoverName().getString())) {
            return false;
        }
        return !itemName.equals(right.getHoverName().getString());
    }

    @Unique
    private void applyLikeItemTransfer(AbstractContainerMenu self, ItemStack left, ItemStack right) {
        if (right.isEmpty()) {
            return;
        }

        ItemStack transferred = LikeItemTransfer.transfer(left, right, PowerAndCost.POWER_LEVEL_CALCULATOR);
        if (transferred.isEmpty()) {
            return;
        }
        applyAnvilName(transferred, left, right);

        power_and_cost$applyingTransfer = true;
        try {
            self.getSlot(RESULT_SLOT_INDEX).set(transferred);
            repairItemCountCost = 0;
            cost.set(computeOurCost());
        } finally {
            power_and_cost$applyingTransfer = false;
        }
    }

    @Unique
    private void applyAnvilName(ItemStack stack, ItemStack left, ItemStack right) {
        if (!isPlayerRename(left, right)) {
            return;
        }
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(itemName));
    }

    @Unique
    private void applyMaterialRepairDurability(
            ItemStack left,
            ItemStack result,
            int craftCost,
            int materialCount,
            double enchantEfficacyFactor
    ) {
        if (!left.isDamageableItem() || !result.isDamageableItem() || materialCount <= 0) {
            return;
        }
        AnvilCostFormulas formulas = new AnvilCostFormulas(PowerAndCost.POWER_LEVEL_CONFIG);
        int newDamage = formulas.damageAfterMaterialRepair(
                craftCost,
                materialCount,
                left,
                enchantEfficacyFactor
        );
        if (newDamage == left.getDamageValue()) {
            return;
        }
        result.setDamageValue(newDamage);
    }

    @Unique
    private void scaleResultDurability(ItemStack left, ItemStack result, double factor) {
        if (!left.isDamageableItem() || !result.isDamageableItem()) {
            return;
        }
        int leftDamage = left.getDamageValue();
        int resultDamage = result.getDamageValue();
        int vanillaRestored = leftDamage - resultDamage;
        if (vanillaRestored <= 0) {
            return;
        }
        int ourRestored = (int) Math.round(vanillaRestored * factor);
        roundUpNearFullDurability(result, leftDamage - ourRestored);
    }

    @Unique
    private static void roundUpNearFullDurability(ItemStack result, int newDamage) {
        int maxDamage = result.getMaxDamage();
        if (newDamage > 0 && newDamage <= maxDamage * 0.05) {
            newDamage = 0;
        }
        result.setDamageValue(Math.max(0, newDamage));
    }
}
