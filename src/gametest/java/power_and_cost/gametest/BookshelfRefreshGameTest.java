package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import power_and_cost.TableXpCostAccess;

import java.util.Arrays;

public class BookshelfRefreshGameTest {

    @GameTest(maxTicks = 200)
    public void bookshelvesChangedRefreshOffers(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = EnchantmentGameTestHelper.openMaxedTable(context);
        EnchantmentGameTestHelper.populateTable(session.menu(), new ItemStack(Items.DIAMOND_SWORD), 64);

        EnchantmentMenu menu = session.menu();
        int[] costsBefore = menu.costs.clone();
        int[] xpBefore = ((TableXpCostAccess) menu).power_and_cost$xpCosts().clone();

        EnchantmentGameTestHelper.clearBookshelves(context);
        menu.stillValid(session.player());

        int[] costsAfter = menu.costs.clone();
        int[] xpAfter = ((TableXpCostAccess) menu).power_and_cost$xpCosts().clone();
        if (Arrays.equals(costsBefore, costsAfter) && Arrays.equals(xpBefore, xpAfter)) {
            throw AnvilGameTestHelper.fail(
                    "Removing bookshelves left the offers unchanged. costs="
                            + costsAfter[0] + "," + costsAfter[1] + "," + costsAfter[2]
                            + " xp=" + xpAfter[0] + "," + xpAfter[1] + "," + xpAfter[2]
            );
        }
        context.succeed();
    }
}
