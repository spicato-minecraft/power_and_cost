package power_and_cost.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Loads every client mixin target. A renamed method fails as soon as the client starts.
 */
public class ClientMixinApplySweepGameTest implements FabricClientGameTest {

    private static final String[] TARGETS = {
            "net.minecraft.client.gui.screens.inventory.AnvilScreen",
            "net.minecraft.client.gui.screens.inventory.EnchantmentScreen"
    };

    @Override
    public void runTest(ClientGameTestContext context) {
        context.computeOnClient(client -> {
            for (String name : TARGETS) {
                try {
                    Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("Mixin target failed to load: " + name, e);
                }
            }
            return null;
        });
    }
}
