package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import power_and_cost.mixin.EnchantmentMenuAccess;
import power_and_cost.TableXpCostAccess;
import power_and_cost.power.AnvilCostFormulas;
import power_and_cost.power.PowerLevelCalculator;
import power_and_cost.power.PowerLevelConfig;
import power_and_cost.power.TableEnchantCost;

import java.util.List;

public class EnchantingTableCostGameTest {

    private static final PowerLevelConfig CONFIG = new PowerLevelConfig();
    private static final TableEnchantCost TABLE_COST = new TableEnchantCost(
            new PowerLevelCalculator(CONFIG),
            new AnvilCostFormulas(CONFIG)
    );

    @GameTest(maxTicks = 200)
    public void tableCostsMatchInputPlusOfferPower(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = EnchantmentGameTestHelper.openMaxedTable(context);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentGameTestHelper.populateTable(session.menu(), sword, 64);

        assertCostsMatchFormula(context, session.menu(), sword);
        assertClickPaysRemappedXpAndVanillaLapis(session);
        context.succeed();
    }

    @GameTest(maxTicks = 200)
    public void bookTableCostsUseMaterialPlZero(GameTestHelper context) {
        EnchantmentGameTestHelper.TableSession session = EnchantmentGameTestHelper.openMaxedTable(context);
        ItemStack book = new ItemStack(Items.BOOK);
        EnchantmentGameTestHelper.populateTable(session.menu(), book, 64);

        assertCostsMatchFormula(context, session.menu(), book);
        context.succeed();
    }

    private static void assertClickPaysRemappedXpAndVanillaLapis(
            EnchantmentGameTestHelper.TableSession session
    ) {
        EnchantmentMenu menu = session.menu();
        int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
        int slot = -1;
        int cost = 0;
        for (int candidate = 0; candidate < 3; candidate++) {
            if (menu.costs[candidate] <= 0) {
                continue;
            }
            int xp = xpCosts[candidate];
            int lapis = candidate + 1;
            if (xp != lapis) {
                slot = candidate;
                cost = xp;
                break;
            }
        }
        if (slot < 0) {
            throw AnvilGameTestHelper.fail(
                    "Every priced slot's XP cost equals its lapis count, so a lapis deduction would look like an XP payment. costs="
                            + menu.costs[0] + "," + menu.costs[1] + "," + menu.costs[2]
            );
        }
        if (session.player().experienceLevel < cost) {
            session.player().experienceLevel = cost;
        }
        int levelsBefore = session.player().experienceLevel;
        boolean clicked = menu.clickMenuButton(session.player(), slot);
        if (!clicked) {
            throw AnvilGameTestHelper.fail("Expected click to succeed on slot " + slot + " cost " + cost);
        }
        int expectedLevels = levelsBefore - cost;
        if (session.player().experienceLevel != expectedLevels) {
            throw AnvilGameTestHelper.fail(
                    "Expected levels " + expectedLevels + " after paying " + cost
                            + ", got " + session.player().experienceLevel
            );
        }
        if (!session.player().hasInfiniteMaterials()) {
            int remaining = menu.getSlot(1).getItem().getCount();
            int expectedLapis = 64 - (slot + 1);
            if (remaining != expectedLapis) {
                throw AnvilGameTestHelper.fail(
                        "Expected lapis " + expectedLapis + " after slot " + slot + ", got " + remaining
                );
            }
        }
    }

    private static void assertCostsMatchFormula(GameTestHelper context, EnchantmentMenu menu, ItemStack input) {
        int priced = 0;
        int[] expectedVanillaPower = vanillaEnchantPowers(menu.getEnchantmentSeed(), 15, input);
        int[] xpCosts = ((TableXpCostAccess) menu).power_and_cost$xpCosts();
        EnchantmentMenuAccess access = (EnchantmentMenuAccess) menu;
        int maxVanilla = 0;
        for (int slot = 0; slot < 3; slot++) {
            if (menu.costs[slot] <= 0) {
                continue;
            }
            priced++;
            maxVanilla = Math.max(maxVanilla, menu.costs[slot]);
            if (menu.costs[slot] != expectedVanillaPower[slot]) {
                throw AnvilGameTestHelper.fail(
                        "Slot " + slot + " vanilla power should stay " + expectedVanillaPower[slot]
                                + ", got " + menu.costs[slot]
                );
            }
            if (xpCosts[slot] < CONFIG.getEnchantCostMin() || xpCosts[slot] > CONFIG.getEnchantCostMax()) {
                throw AnvilGameTestHelper.fail(
                        "Slot " + slot + " XP " + xpCosts[slot] + " outside min/max"
                );
            }
            List<EnchantmentInstance> offered = access.invokeGetEnchantmentList(
                    context.getLevel().registryAccess(),
                    input,
                    slot,
                    menu.costs[slot]
            );
            if (offered.isEmpty()) {
                continue;
            }
            int expectedXp = TABLE_COST.slotCost(input, offered);
            if (xpCosts[slot] != expectedXp) {
                throw AnvilGameTestHelper.fail(
                        "Slot " + slot + " expected XP " + expectedXp + " from offer PL, got " + xpCosts[slot]
                                + " (vanilla power " + menu.costs[slot] + ")"
                );
            }
        }
        if (maxVanilla <= 4) {
            throw AnvilGameTestHelper.fail(
                    "Expected maxed bookshelves to produce vanilla power above 4, got max " + maxVanilla
            );
        }
        if (priced == 0) {
            throw AnvilGameTestHelper.fail("Expected at least one table offer on a maxed bookshelf setup");
        }
    }

    private static int[] vanillaEnchantPowers(int seed, int bookshelves, ItemStack stack) {
        RandomSource random = RandomSource.create();
        random.setSeed(seed);
        int[] powers = new int[3];
        for (int slot = 0; slot < 3; slot++) {
            int power = EnchantmentHelper.getEnchantmentCost(random, slot, bookshelves, stack);
            if (power < slot + 1) {
                power = 0;
            }
            powers[slot] = power;
        }
        return powers;
    }
}
