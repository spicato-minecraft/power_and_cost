package power_and_cost.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ClientInformation;

import java.util.UUID;

/**
 * Shared helpers for anvil GameTests.
 */
public final class AnvilGameTestHelper {

    private AnvilGameTestHelper() {
    }

    public static void buildFloor(GameTestHelper context, int size) {
        BlockState floor = Blocks.STONE.defaultBlockState();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                context.setBlock(x, 0, z, floor);
            }
        }
    }

    public record AnvilSession(AnvilMenu menu, ServerPlayer player) {
    }

    public static AnvilMenu openAnvilMenu(GameTestHelper context) {
        return openAnvil(context).menu();
    }

    public static AnvilSession openAnvil(GameTestHelper context) {
        buildFloor(context, 3);
        BlockPos anvilPos = context.absolutePos(new BlockPos(1, 1, 1));
        context.setBlock(anvilPos, Blocks.ANVIL.defaultBlockState());

        ServerLevel level = context.getLevel();
        ServerPlayer player = createOffWorldPlayer(level);
        ContainerLevelAccess access = ContainerLevelAccess.create(level, anvilPos);
        return new AnvilSession(new AnvilMenu(0, player.getInventory(), access), player);
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

    public static void populateAnvil(AnvilMenu menu, ItemStack left, ItemStack right, String itemName) {
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left.copy());
        if (itemName != null) {
            menu.setItemName(itemName);
            return;
        }
        if (!right.isEmpty()) {
            menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right.copy());
        }
    }

    public static int vanillaSameItemResultDamage(ItemStack left, ItemStack right) {
        int maxDamage = left.getMaxDamage();
        int leftRemaining = maxDamage - left.getDamageValue();
        int rightRemaining = maxDamage - right.getDamageValue();
        int combinedRemaining = leftRemaining + rightRemaining + maxDamage * 12 / 100;
        return Math.max(0, maxDamage - combinedRemaining);
    }

    public static ItemStack enchant(
            GameTestHelper context,
            ItemStack stack,
            ResourceKey<Enchantment> enchantmentKey,
            int level
    ) {
        return enchant(context.getLevel().registryAccess(), stack, enchantmentKey, level);
    }

    public static ItemStack enchant(
            RegistryAccess registries,
            ItemStack stack,
            ResourceKey<Enchantment> enchantmentKey,
            int level
    ) {
        Holder<Enchantment> holder = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantmentKey);
        ItemEnchantments existing = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
        mutable.set(holder, level);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }

    public static ItemStack enchantedBook(
            GameTestHelper context,
            ResourceKey<Enchantment> enchantmentKey,
            int level
    ) {
        return enchantedBook(context.getLevel().registryAccess(), enchantmentKey, level);
    }

    public static ItemStack enchantedBook(
            RegistryAccess registries,
            ResourceKey<Enchantment> enchantmentKey,
            int level
    ) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        Holder<Enchantment> holder = registries
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantmentKey);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(holder, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return book;
    }

    public static ItemStack resultStack(AnvilMenu menu) {
        return menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
    }

    public static GameTestAssertException fail(String message) {
        return new GameTestAssertException(Component.literal(message), 0);
    }
}
