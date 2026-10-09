package power_and_cost.mixin.client;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Bypasses the vanilla "Too Expensive" display when cost >= 40.
 * This mod replaces the prior work penalty with a power-level-based economy,
 * so costs can legitimately exceed 40 levels.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {

    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
    private int gear_maintenance_raiseTooExpensiveThreshold(int constant) {
        return Integer.MAX_VALUE;
    }
}
