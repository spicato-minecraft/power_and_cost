package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import power_and_cost.TableXpCostAccess;

public class CreativeTableSkipsXpGameTest {

    @GameTest(maxTicks = 200)
    public void creativeTableSkipsXp(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = openPricedTable(context);
        EnchantmentMenu menu = session.menu();
        int slot = slotWithXpAboveOne(menu);

        session.player().getAbilities().instabuild = true;
        session.player().experienceLevel = 0;
        boolean clicked = menu.clickMenuButton(session.player(), slot);
        if (!clicked) {
            throw AnvilGameTestHelper.fail(
                    "Creative click failed on slot " + slot + " with XP cost " + xpCosts(menu)[slot]
            );
        }
        if (session.player().experienceLevel != 0) {
            throw AnvilGameTestHelper.fail(
                    "Creative enchant changed level 0 to " + session.player().experienceLevel
            );
        }
        ItemEnchantments enchantments = menu.getSlot(0).getItem().getEnchantments();
        if (enchantments.isEmpty()) {
            throw AnvilGameTestHelper.fail("Creative click did not enchant the sword");
        }
        context.succeed();
    }

    @GameTest(maxTicks = 200)
    public void survivalLevelZeroClickDoesNotEnchant(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = openPricedTable(context);
        EnchantmentMenu menu = session.menu();
        int slot = slotWithXpAboveOne(menu);

        session.player().getAbilities().instabuild = false;
        session.player().experienceLevel = 0;
        boolean clicked = menu.clickMenuButton(session.player(), slot);
        if (clicked) {
            throw AnvilGameTestHelper.fail(
                    "Survival click at level 0 succeeded on slot " + slot
                            + " with XP cost " + xpCosts(menu)[slot]
            );
        }
        if (session.player().experienceLevel != 0) {
            throw AnvilGameTestHelper.fail(
                    "Rejected survival click changed level 0 to " + session.player().experienceLevel
            );
        }
        ItemEnchantments enchantments = menu.getSlot(0).getItem().getEnchantments();
        if (!enchantments.isEmpty()) {
            throw AnvilGameTestHelper.fail("Survival click at level 0 still enchanted the sword");
        }
        context.succeed();
    }

    private static EnchantmentGameTestHelper.TableSession openPricedTable(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = EnchantmentGameTestHelper.openMaxedTable(context);
        EnchantmentGameTestHelper.populateTable(session.menu(), new ItemStack(Items.NETHERITE_SWORD), 64);
        return session;
    }

    private static int slotWithXpAboveOne(EnchantmentMenu menu) {
        int[] xpCosts = xpCosts(menu);
        for (int slot = 0; slot < 3; slot++) {
            if (xpCosts[slot] > 1) {
                return slot;
            }
        }
        throw AnvilGameTestHelper.fail(
                "No slot had an XP cost above 1. costs="
                        + menu.costs[0] + "," + menu.costs[1] + "," + menu.costs[2]
                        + " xp=" + xpCosts[0] + "," + xpCosts[1] + "," + xpCosts[2]
        );
    }

    private static int[] xpCosts(EnchantmentMenu menu) {
        return ((TableXpCostAccess) menu).power_and_cost$xpCosts();
    }
}
