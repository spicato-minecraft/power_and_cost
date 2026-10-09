package power_and_cost.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import power_and_cost.gametest.AnvilGameTestHelper;

import java.lang.reflect.Method;

/**
 * Opens an anvil whose combine costs more than 40 levels and checks the label.
 */
public class AnvilCostLabelClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            world.getServer().runOnServer(server -> openExpensiveAnvil(server.getPlayerList().getPlayers().getFirst()));
            context.waitForScreen(AnvilScreen.class);
            context.waitFor(client ->
                    client.gui.screen() instanceof AnvilScreen screen && screen.getMenu().getCost() > 40
            );
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof AnvilScreen screen)) {
                    throw new AssertionError("Expected the anvil screen");
                }
                if (client.player == null) {
                    throw new AssertionError("Client player is missing");
                }
                client.player.getAbilities().instabuild = false;
                int cost = screen.getMenu().getCost();
                if (cost <= 40) {
                    throw new AssertionError("Expected an anvil cost above 40, got " + cost);
                }

                RecordingGuiGraphics graphics = new RecordingGuiGraphics(client);
                try {
                    Method extractLabels = AnvilScreen.class.getDeclaredMethod(
                            "extractLabels",
                            GuiGraphicsExtractor.class,
                            int.class,
                            int.class
                    );
                    extractLabels.setAccessible(true);
                    extractLabels.invoke(screen, graphics, 0, 0);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError("Anvil label extraction failed", e);
                }

                TranslatableContents costLabel = null;
                for (var drawn : graphics.components) {
                    if (!(drawn.getContents() instanceof TranslatableContents contents)) {
                        continue;
                    }
                    if ("container.repair.expensive".equals(contents.getKey())) {
                        throw new AssertionError("Anvil drew Too Expensive for cost " + cost);
                    }
                    if ("container.repair.cost".equals(contents.getKey())) {
                        costLabel = contents;
                    }
                }
                if (costLabel == null) {
                    throw new AssertionError("Anvil did not draw the level cost. Drawn: " + graphics.components);
                }
                if (costLabel.getArgs().length == 0 || !(costLabel.getArgs()[0] instanceof Integer shown)) {
                    throw new AssertionError("Level cost label has no integer argument: " + costLabel);
                }
                if (shown != cost) {
                    throw new AssertionError("Label showed " + shown + " but the menu cost is " + cost);
                }
            });
        }
    }

    private static void openExpensiveAnvil(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition().offset(1, 0, 0);
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        level.setBlockAndUpdate(pos, Blocks.ANVIL.defaultBlockState());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
        if (!(player.containerMenu instanceof AnvilMenu menu)) {
            throw new AssertionError("Anvil did not open. Menu is " + player.containerMenu.getClass().getName());
        }
        player.getAbilities().instabuild = true;

        ItemStack left = new ItemStack(Items.NETHERITE_SWORD);
        AnvilGameTestHelper.enchant(level.registryAccess(), left, Enchantments.SHARPNESS, 5);
        AnvilGameTestHelper.enchant(level.registryAccess(), left, Enchantments.LOOTING, 3);
        AnvilGameTestHelper.enchant(level.registryAccess(), left, Enchantments.UNBREAKING, 3);
        AnvilGameTestHelper.enchant(level.registryAccess(), left, Enchantments.FIRE_ASPECT, 2);
        AnvilGameTestHelper.enchant(level.registryAccess(), left, Enchantments.SWEEPING_EDGE, 3);
        AnvilGameTestHelper.enchant(level.registryAccess(), left, Enchantments.KNOCKBACK, 2);
        ItemStack book = AnvilGameTestHelper.enchantedBook(level.registryAccess(), Enchantments.EFFICIENCY, 5);
        AnvilGameTestHelper.populateAnvil(menu, left, book, null);
        if (menu.getCost() <= 40) {
            throw new AssertionError("Server anvil cost was " + menu.getCost() + ", expected above 40");
        }
    }
}
