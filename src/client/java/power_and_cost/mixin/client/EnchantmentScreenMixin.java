package power_and_cost.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import power_and_cost.TableXpCostAccess;

/**
 * Right-side numerals stay vanilla bookshelf power (level requirement).
 * Left XP orbs are numberless; PL XP is drawn beside them like vanilla's 1/2/3.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {

    private static final int ENABLED_COST_COLOR = 0xFFC8FF8F;
    private static final int DISABLED_COST_COLOR = 0xFF807F60;
    private static final Identifier LEVEL_ORB =
            Identifier.fromNamespaceAndPath("power_and_cost", "container/enchanting_table/level_orb");
    private static final Identifier LEVEL_ORB_DISABLED =
            Identifier.fromNamespaceAndPath("power_and_cost", "container/enchanting_table/level_orb_disabled");

    @Shadow
    @Final
    private static Identifier[] ENABLED_LEVEL_SPRITES;

    @Shadow
    @Final
    private static Identifier[] DISABLED_LEVEL_SPRITES;

    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Redirect(
            method = "extractBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void power_and_cost_drawOrbAndXp(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height
    ) {
        boolean disabled = power_and_cost$isSprite(DISABLED_LEVEL_SPRITES, sprite);
        boolean enabled = power_and_cost$isSprite(ENABLED_LEVEL_SPRITES, sprite);
        if (!disabled && !enabled) {
            graphics.blitSprite(pipeline, sprite, x, y, width, height);
            return;
        }
        int slot = power_and_cost$slotFromLevelOrbY(y);
        int xp = power_and_cost$xpForSlot(slot);
        graphics.blitSprite(pipeline, disabled ? LEVEL_ORB_DISABLED : LEVEL_ORB, x - 2, y, width, height);
        if (xp <= 0) {
            return;
        }
        String text = Integer.toString(xp);
        graphics.text(
                this.font,
                text,
                x + 11,
                y + 4,
                disabled ? DISABLED_COST_COLOR : ENABLED_COST_COLOR,
                true
        );
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private MutableComponent power_and_cost_tooltipLevelOne(String key) {
        if (!"container.enchant.level.one".equals(key)) {
            return Component.translatable(key);
        }
        int xp = power_and_cost$xpForSlot(0);
        if (xp <= 1) {
            return Component.translatable(key);
        }
        return Component.translatable("container.enchant.level.many", xp);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    private MutableComponent power_and_cost_tooltipLevelMany(String key, Object[] args) {
        if (!"container.enchant.level.many".equals(key) || args.length == 0 || !(args[0] instanceof Integer slotPlusOne)) {
            return Component.translatable(key, args);
        }
        int xp = power_and_cost$xpForSlot(slotPlusOne - 1);
        if (xp <= 1) {
            return Component.translatable("container.enchant.level.one");
        }
        return Component.translatable(key, xp);
    }

    /**
     * Gray out a row if the player cannot pay PL XP, without changing the right-side requirement numeral.
     */
    @ModifyExpressionValue(
            method = "extractBackground",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I"
            )
    )
    private int power_and_cost_lockWhenXpUnpaid(int experienceLevel, @Local(index = 8) int slot) {
        int xp = power_and_cost$xpForSlot(slot);
        if (xp > 0 && experienceLevel < xp) {
            return 0;
        }
        return experienceLevel;
    }

    @Unique
    private int power_and_cost$xpForSlot(int slot) {
        int[] xpCosts = ((TableXpCostAccess) this.menu).power_and_cost$xpCosts();
        if (slot < 0 || slot >= xpCosts.length) {
            return 0;
        }
        return xpCosts[slot];
    }

    @Unique
    private int power_and_cost$slotFromLevelOrbY(int orbY) {
        int rel = orbY - this.topPos - 15;
        if (rel < 0) {
            return 0;
        }
        return Math.max(0, Math.min(2, rel / 19));
    }

    @Unique
    private static boolean power_and_cost$isSprite(Identifier[] sprites, Identifier sprite) {
        for (Identifier candidate : sprites) {
            if (candidate.equals(sprite)) {
                return true;
            }
        }
        return false;
    }
}
