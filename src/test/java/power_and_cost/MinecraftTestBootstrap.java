package power_and_cost;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;

/**
 * Initializes Minecraft registries for Fabric Loader JUnit unit tests.
 */
public final class MinecraftTestBootstrap {

    private static boolean initialized;

    private MinecraftTestBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		DataComponentMap defaults = DataComponentMap.builder()
				.set(DataComponents.MAX_STACK_SIZE, 64)
				.build();
		BuiltInRegistries.ITEM.listElements().forEach(ref -> ref.bindComponents(defaults));
		initialized = true;
    }
}
