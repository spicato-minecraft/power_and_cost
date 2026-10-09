package power_and_cost.mixin;

import power_and_cost.PowerAndCost;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import net.minecraft.core.component.DataComponents;
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

import java.util.Objects;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    private int repairItemCountCost;

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

    @Inject(method = "createResult", at = @At("TAIL"))
    private void gear_maintenance_overrideCostAndDurability(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack left = self.getSlot(INPUT_SLOT_LEFT).getItem();
        ItemStack right = self.getSlot(INPUT_SLOT_RIGHT).getItem();
        ItemStack result = self.getSlot(RESULT_SLOT_INDEX).getItem();

        if (left.isEmpty() || result.isEmpty()) {
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

        var repairable = left.get(DataComponents.REPAIRABLE);
        boolean isRepairMaterial = repairable != null && repairable.items().stream()
                .anyMatch(holder -> holder.value() == right.getItem());
        if (isRepairMaterial) {
            repairItemCountCost = 1;
            double factor = formulas.durabilityRepairFactor(leftEnchant);
            scaleResultDurability(left, result, factor);
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
        int renameCost = getRenameCost(left, result);

        if (right.isEmpty()) {
            return renameCost;
        }

        if (right.is(Items.ENCHANTED_BOOK)) {
            int resultPL = calc.computeMaterialPowerValue(result) + calc.computeEnchantmentPowerValue(result);
            return formulas.enchantCostFromPL(resultPL) + renameCost;
        }

        if (left.getItem() == right.getItem()) {
            int resultPL = calc.computeMaterialPowerValue(result) + calc.computeEnchantmentPowerValue(result);
            return formulas.enchantCostFromPL(resultPL) + renameCost;
        }

        var repairable = left.get(DataComponents.REPAIRABLE);
        boolean isRepairMaterial = repairable != null && repairable.items().stream()
                .anyMatch(holder -> holder.value() == right.getItem());
        if (isRepairMaterial) {
            return 1 + renameCost;
        }

        return 1 + renameCost;
    }

    @Unique
    private int getRenameCost(ItemStack left, ItemStack result) {
        return !Objects.equals(left.getHoverName().getString(), result.getHoverName().getString())
                ? RENAME_COST : 0;
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
        int newDamage = leftDamage - ourRestored;
        int maxDamage = result.getMaxDamage();
        // Round up to full when repair would leave item at >= 95% durability
        if (newDamage > 0 && newDamage <= maxDamage * 0.05) {
            newDamage = 0;
        }
        result.setDamageValue(Math.max(0, newDamage));
    }
}
