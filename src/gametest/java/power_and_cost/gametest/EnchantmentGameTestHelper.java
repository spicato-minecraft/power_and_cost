package power_and_cost.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

final class EnchantmentGameTestHelper {

    private EnchantmentGameTestHelper() {
    }

    record TableSession(EnchantmentMenu menu, ServerPlayer player) {
    }

    static TableSession openMaxedTable(GameTestHelper context) {
        // Stay inside the default 8x8 GameTest structure so parallel tests do not overlap.
        buildFloor(context, 8);
        BlockPos tableRel = new BlockPos(3, 1, 3);
        context.setBlock(3, 1, 3, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos shelfRel = tableRel.offset(offset);
            context.setBlock(shelfRel.getX(), shelfRel.getY(), shelfRel.getZ(), Blocks.BOOKSHELF.defaultBlockState());
        }

        ServerLevel level = context.getLevel();
        ServerPlayer player = createOffWorldPlayer(level);
        player.experienceLevel = 50;
        BlockPos tableAbs = context.absolutePos(tableRel);
        ContainerLevelAccess access = ContainerLevelAccess.create(level, tableAbs);
        EnchantmentMenu menu = new EnchantmentMenu(0, player.getInventory(), access);
        return new TableSession(menu, player);
    }

    static void populateTable(EnchantmentMenu menu, ItemStack item, int lapisCount) {
        menu.getSlot(0).set(item.copy());
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, lapisCount));
    }

    static java.util.List<BlockPos> bookshelfPositions() {
        BlockPos tableRel = new BlockPos(3, 1, 3);
        java.util.List<BlockPos> positions = new java.util.ArrayList<>();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            positions.add(tableRel.offset(offset));
        }
        return positions;
    }

    static void clearBookshelves(GameTestHelper context) {
        BlockPos tableRel = new BlockPos(3, 1, 3);
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos shelfRel = tableRel.offset(offset);
            context.setBlock(shelfRel.getX(), shelfRel.getY(), shelfRel.getZ(), Blocks.AIR.defaultBlockState());
        }
    }

    static int firstPricedSlot(EnchantmentMenu menu) {
        for (int slot = 0; slot < 3; slot++) {
            if (menu.costs[slot] > 0) {
                return slot;
            }
        }
        return -1;
    }

    private static void buildFloor(GameTestHelper context, int size) {
        BlockState floor = Blocks.STONE.defaultBlockState();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                context.setBlock(x, 0, z, floor);
            }
        }
    }

    private static ServerPlayer createOffWorldPlayer(ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "GameTestPlayer");
        return new ServerPlayer(
                level.getServer(),
                level,
                profile,
                ClientInformation.createDefault()
        );
    }
}
