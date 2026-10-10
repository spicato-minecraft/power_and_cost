package power_and_cost.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Loads every server mixin target. A renamed class fails as soon as the test runs.
 */
public class ServerMixinApplySweepGameTest {

    private static final String[] TARGETS = {
            "net.minecraft.world.inventory.AnvilMenu",
            "net.minecraft.world.inventory.EnchantmentMenu"
    };

    @GameTest(maxTicks = 20)
    public void serverMixinApplySweep(GameTestHelper context) {
        for (String name : TARGETS) {
            try {
                Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw AnvilGameTestHelper.fail("Mixin target failed to load: " + name);
            }
        }
        context.succeed();
    }
}
