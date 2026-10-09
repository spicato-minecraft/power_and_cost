package power_and_cost.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import power_and_cost.TableXpCostAccess;
import power_and_cost.mixin.EnchantmentMenuAccess;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import power_and_cost.power.PowerLevelConfig;
import power_and_cost.power.TableEnchantCost;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Opens the enchanting screen and checks the XP numerals, tooltip, unpaid row, and synced costs.
 */
public class EnchantScreenClientGameTest implements FabricClientGameTest {

    private static final Identifier LEVEL_ORB =
            Identifier.fromNamespaceAndPath("power_and_cost", "container/enchanting_table/level_orb");
    private static final Identifier LEVEL_ORB_DISABLED =
            Identifier.fromNamespaceAndPath("power_and_cost", "container/enchanting_table/level_orb_disabled");

    private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
    private static final TableEnchantCost TABLE_COST = new TableEnchantCost(
            new PowerLevelCalculator(CONFIG),
            new AnvilCostFormulas(CONFIG)
    );

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runOnServer(server -> openMaxedTable(server.getPlayerList().getPlayers().getFirst()));
            context.waitForScreen(EnchantmentScreen.class);
            context.waitFor(client -> {
                if (!(client.screen instanceof EnchantmentScreen screen)) {
                    return false;
                }
                EnchantmentMenu menu = screen.getMenu();
                int[] xp = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
                for (int slot = 0; slot < 3; slot++) {
                    if (xp[slot] > 1 && xp[slot] > menu.costs[slot] && menu.costs[slot] > 0) {
                        return true;
                    }
                }
                return false;
            });

            context.runOnClient(client -> {
                if (!(client.screen instanceof EnchantmentScreen screen)) {
                    throw new AssertionError("Expected the enchanting screen");
                }
                var player = client.player;
                if (player == null || client.level == null) {
                    throw new AssertionError("Client player is missing");
                }
                player.getAbilities().instabuild = false;
                EnchantmentMenu menu = screen.getMenu();
                int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
                ItemStack input = menu.getSlot(0).getItem();
                EnchantmentMenuAccess access = (EnchantmentMenuAccess) menu;

                int comparableSlot = -1;
                for (int slot = 0; slot < 3; slot++) {
                    if (menu.costs[slot] <= 0) {
                        continue;
                    }
                    List<EnchantmentInstance> offered = access.invokeGetEnchantmentList(
                            client.level.registryAccess(),
                            input,
                            slot,
                            menu.costs[slot]
                    );
                    int expected = offered.isEmpty() ? 0 : TABLE_COST.slotCost(input, offered);
                    if (xpCosts[slot] != expected) {
                        throw new AssertionError(
                                "Slot " + slot + " client XP " + xpCosts[slot] + " != formula " + expected
                        );
                    }
                    if (expected > 1 && expected != menu.costs[slot] && expected > menu.costs[slot]) {
                        comparableSlot = slot;
                    }
                }
                if (comparableSlot < 0) {
                    throw new AssertionError(
                            "No slot has an XP cost above both 1 and the vanilla cost. costs="
                                    + menu.costs[0] + "," + menu.costs[1] + "," + menu.costs[2]
                                    + " xp=" + xpCosts[0] + "," + xpCosts[1] + "," + xpCosts[2]
                    );
                }

                player.experienceLevel = 100;
                RecordingGuiGraphics paid = renderBackground(client, screen);
                if (!paid.sprites.contains(LEVEL_ORB)) {
                    throw new AssertionError("Paid row did not draw the mod level orb. Sprites: " + paid.sprites);
                }
                if (paid.sprites.contains(LEVEL_ORB_DISABLED)) {
                    throw new AssertionError("A payable row drew the disabled orb");
                }
                assertShowsBothNumbers(paid, xpCosts[comparableSlot], menu.costs[comparableSlot]);

                player.experienceLevel = menu.costs[comparableSlot];
                RecordingGuiGraphics unpaid = renderBackground(client, screen);
                if (!unpaid.sprites.contains(LEVEL_ORB_DISABLED)) {
                    throw new AssertionError(
                            "Unpaid row did not draw the disabled orb at level " + player.experienceLevel
                                    + " xp " + xpCosts[comparableSlot] + ". Sprites: " + unpaid.sprites
                    );
                }

                int left = screenX(screen, "leftPos");
                int top = screenX(screen, "topPos");
                int mouseX = left + 60 + 20;
                int mouseY = top + 14 + comparableSlot * 19 + 8;
                RecordingGuiGraphics tooltipGraphics = new RecordingGuiGraphics(client);
                screen.extractRenderState(tooltipGraphics, mouseX, mouseY, 0.0f);
                int xp = xpCosts[comparableSlot];
                boolean found = false;
                for (List<Component> tooltip : tooltipGraphics.tooltips) {
                    for (Component line : tooltip) {
                        if (!(line.getContents() instanceof TranslatableContents contents)) {
                            continue;
                        }
                        if (!"container.enchant.level.many".equals(contents.getKey())) {
                            continue;
                        }
                        if (contents.getArgs().length > 0 && contents.getArgs()[0] instanceof Integer shown && shown == xp) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    throw new AssertionError(
                            "Tooltip did not show XP cost " + xp + ". Tooltips: " + tooltipGraphics.tooltips
                    );
                }
            });
        }
    }

    private static void openMaxedTable(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos table = player.blockPosition().offset(1, 0, 1);
        player.teleportTo(table.getX() + 0.5, table.getY(), table.getZ() + 1.5);
        level.setBlockAndUpdate(table, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            level.setBlockAndUpdate(table.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }
        player.experienceLevel = 100;
        player.getAbilities().instabuild = false;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false);
        player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
        if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
            throw new AssertionError("Enchanting table did not open. Menu is " + player.containerMenu.getClass().getName());
        }
        menu.getSlot(0).set(new ItemStack(Items.NETHERITE_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));
        if (slotWhereXpExceedsVanilla(menu) >= 0) {
            return;
        }
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            level.setBlockAndUpdate(table.offset(offset), Blocks.AIR.defaultBlockState());
            menu.stillValid(player);
            if (slotWhereXpExceedsVanilla(menu) >= 0) {
                return;
            }
        }
        int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
        throw new AssertionError(
                "No bookshelf count put an XP cost above the vanilla cost. costs="
                        + menu.costs[0] + "," + menu.costs[1] + "," + menu.costs[2]
                        + " xp=" + xpCosts[0] + "," + xpCosts[1] + "," + xpCosts[2]
        );
    }

    private static int slotWhereXpExceedsVanilla(EnchantmentMenu menu) {
        int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
        for (int slot = 0; slot < 3; slot++) {
            if (menu.costs[slot] > 0 && xpCosts[slot] > 1 && xpCosts[slot] > menu.costs[slot]) {
                return slot;
            }
        }
        return -1;
    }

    private static RecordingGuiGraphics renderBackground(net.minecraft.client.Minecraft client, EnchantmentScreen screen) {
        RecordingGuiGraphics graphics = new RecordingGuiGraphics(client);
        screen.extractBackground(graphics, 0, 0, 0.0f);
        return graphics;
    }

    private static void assertShowsBothNumbers(RecordingGuiGraphics graphics, int xp, int vanilla) {
        String xpText = Integer.toString(xp);
        String vanillaText = Integer.toString(vanilla);
        if (!graphics.strings.contains(xpText) || !graphics.strings.contains(vanillaText)) {
            throw new AssertionError(
                    "Expected both XP " + xpText + " and vanilla power " + vanillaText + " on the screen. Drawn: "
                            + graphics.strings
            );
        }
    }

    private static int screenX(EnchantmentScreen screen, String fieldName) {
        Class<?> type = screen.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getInt(screen);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("Missing screen field " + fieldName);
    }
}
