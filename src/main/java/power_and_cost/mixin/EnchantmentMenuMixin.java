package power_and_cost.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import power_and_cost.PowerAndCost;
import power_and_cost.TableXpCostAccess;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.TableEnchantCost;

import java.util.List;

/**
 * Leaves vanilla {@link EnchantmentMenu#costs} as bookshelf enchantment power (offers + unlock).
 * Syncs a second array for XP charged, so the sigmoid cannot collapse offer generation to ~4.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin implements TableXpCostAccess {

    @Shadow
    @Final
    public int[] costs;

    @Shadow
    @Final
    private Container enchantSlots;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    private List<EnchantmentInstance> getEnchantmentList(
            RegistryAccess registryAccess,
            ItemStack stack,
            int slot,
            int enchantmentPower
    ) {
        throw new AssertionError();
    }

    @Unique
    private final int[] power_and_cost$xpCosts = new int[3];

    @Unique
    private int power_and_cost$lastBookshelfCount = -1;

    @Override
    public int[] power_and_cost$xpCosts() {
        return power_and_cost$xpCosts;
    }

    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL")
    )
    private void power_and_cost_registerXpCostSlots(CallbackInfo ci) {
        AbstractContainerMenuAccess access = (AbstractContainerMenuAccess) this;
        access.invokeAddDataSlot(DataSlot.shared(power_and_cost$xpCosts, 0));
        access.invokeAddDataSlot(DataSlot.shared(power_and_cost$xpCosts, 1));
        access.invokeAddDataSlot(DataSlot.shared(power_and_cost$xpCosts, 2));
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void power_and_cost_fillXpCosts(Container container, CallbackInfo ci) {
        if (container != this.enchantSlots) {
            return;
        }
        ItemStack stack = this.enchantSlots.getItem(0);
        if (stack.isEmpty() || !stack.isEnchantable()) {
            power_and_cost$lastBookshelfCount = -1;
            for (int slot = 0; slot < 3; slot++) {
                power_and_cost$xpCosts[slot] = 0;
            }
            return;
        }
        this.access.execute((level, pos) -> {
            power_and_cost$lastBookshelfCount = power_and_cost$countBookshelves(level, pos);
            TableEnchantCost table = new TableEnchantCost(
                    PowerAndCost.POWER_LEVEL_CALCULATOR,
                    new AnvilCostFormulas(PowerAndCost.POWER_LEVEL_CONFIG)
            );
            for (int slot = 0; slot < 3; slot++) {
                if (costs[slot] <= 0) {
                    power_and_cost$xpCosts[slot] = 0;
                    continue;
                }
                List<EnchantmentInstance> offered = getEnchantmentList(
                        level.registryAccess(),
                        stack,
                        slot,
                        costs[slot]
                );
                power_and_cost$xpCosts[slot] = offered.isEmpty() ? 0 : table.slotCost(stack, offered);
            }
        });
    }

    @Inject(method = "stillValid", at = @At("HEAD"))
    private void power_and_cost_refreshWhenBookshelvesChange(CallbackInfoReturnable<Boolean> cir) {
        access.execute((level, pos) -> {
            ItemStack stack = enchantSlots.getItem(0);
            if (stack.isEmpty() || !stack.isEnchantable()) {
                return;
            }
            int shelves = power_and_cost$countBookshelves(level, pos);
            if (shelves != power_and_cost$lastBookshelfCount) {
                power_and_cost$lastBookshelfCount = shelves;
                ((EnchantmentMenu) (Object) this).slotsChanged(enchantSlots);
            }
        });
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void power_and_cost_requireXpCost(
            net.minecraft.world.entity.player.Player player,
            int slot,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (slot < 0 || slot >= power_and_cost$xpCosts.length) {
            return;
        }
        int xp = power_and_cost$xpCosts[slot];
        if (xp > 0 && player.experienceLevel < xp && !player.hasInfiniteMaterials()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(
            method = "lambda$clickMenuButton$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V"
            ),
            index = 1
    )
    private int power_and_cost_deductXpCost(int lapisCount) {
        int slot = lapisCount - 1;
        if (slot >= 0 && slot < power_and_cost$xpCosts.length && power_and_cost$xpCosts[slot] > 0) {
            return power_and_cost$xpCosts[slot];
        }
        return lapisCount;
    }

    @Unique
    private static int power_and_cost$countBookshelves(Level level, BlockPos tablePos) {
        int count = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, tablePos, offset)) {
                count++;
            }
        }
        return count;
    }
}
