package power_and_cost.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerLevelConfigTest {

    @Test
    void unknownEnchantmentPowerIsFourPerLevel() {
        PowerLevelConfig config = new PowerLevelConfig();
        assertEquals(12, config.getEnchantmentPower("minecraft:not_a_real_enchantment", 3));
    }
}
