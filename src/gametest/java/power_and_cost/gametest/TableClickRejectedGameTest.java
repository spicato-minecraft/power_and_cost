package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import power_and_cost.TableXpCostAccess;

public class TableClickRejectedGameTest {

    @GameTest(maxTicks = 200)
    public void tableClickRejectedBelowXpCost(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = EnchantmentGameTestHelper.openMaxedTable(context);
        ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
        EnchantmentGameTestHelper.populateTable(session.menu(), sword, 64);

        EnchantmentMenu menu = session.menu();
        StringBuilder tried = new StringBuilder();
        int slot = slotWhereXpExceedsVanilla(menu, tried);
        if (slot < 0) {
            for (BlockPos shelf : EnchantmentGameTestHelper.bookshelfPositions()) {
                context.setBlock(shelf.getX(), shelf.getY(), shelf.getZ(), Blocks.AIR.defaultBlockState());
                menu.stillValid(session.player());
                slot = slotWhereXpExceedsVanilla(menu, tried);
                if (slot >= 0) {
                    break;
                }
            }
        }
        if (slot < 0) {
            throw AnvilGameTestHelper.fail(
                    "No bookshelf count produced a slot whose XP cost is above the vanilla cost. " + tried
            );
        }

        int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
        int vanillaCost = menu.costs[slot];
        session.player().experienceLevel = vanillaCost;
        session.player().getAbilities().instabuild = false;
        boolean clicked = menu.clickMenuButton(session.player(), slot);
        if (clicked) {
            throw AnvilGameTestHelper.fail(
                    "Click succeeded at vanilla cost " + vanillaCost + " while XP cost is " + xpCosts[slot]
            );
        }
        if (session.player().experienceLevel != vanillaCost) {
            throw AnvilGameTestHelper.fail(
                    "Rejected click changed levels from " + vanillaCost + " to " + session.player().experienceLevel
            );
        }
        ItemEnchantments enchantments = menu.getSlot(0).getItem().getEnchantments();
        if (!enchantments.isEmpty()) {
            throw AnvilGameTestHelper.fail("Rejected click still enchanted the sword");
        }
        context.succeed();
    }

    private static int slotWhereXpExceedsVanilla(EnchantmentMenu menu, StringBuilder tried) {
        int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
        tried.append("[costs=")
                .append(menu.costs[0]).append(',').append(menu.costs[1]).append(',').append(menu.costs[2])
                .append(" xp=")
                .append(xpCosts[0]).append(',').append(xpCosts[1]).append(',').append(xpCosts[2])
                .append("] ");
        for (int candidate = 0; candidate < 3; candidate++) {
            if (menu.costs[candidate] > 0 && xpCosts[candidate] > menu.costs[candidate]) {
                return candidate;
            }
        }
        return -1;
    }
}
