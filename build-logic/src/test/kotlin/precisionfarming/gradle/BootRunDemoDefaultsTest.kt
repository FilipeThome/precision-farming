package precisionfarming.gradle

import kotlin.test.Test
import kotlin.test.assertEquals

class BootRunDemoDefaultsTest {
    @Test
    fun blankEnvSeedsAndUsesLocalProfile() {
        assertEquals(
            mapOf("APP_SEED" to "true", "SPRING_PROFILES_ACTIVE" to "local"),
            BootRunDemoDefaults.environmentOverrides(null, null, null),
        )
        assertEquals(
            mapOf("APP_SEED" to "true", "SPRING_PROFILES_ACTIVE" to "local"),
            BootRunDemoDefaults.environmentOverrides("  ", "", " "),
        )
    }

    @Test
    fun allowDemoSecretsFalseDoesNotInjectLocalProfile() {
        assertEquals(
            mapOf("APP_SEED" to "true"),
            BootRunDemoDefaults.environmentOverrides(null, null, "false"),
        )
    }

    @Test
    fun explicitProfileAndSeedAreLeftAlone() {
        assertEquals(
            emptyMap(),
            BootRunDemoDefaults.environmentOverrides("false", "prod", "false"),
        )
    }

    @Test
    fun explicitProfileStillDefaultsSeed() {
        assertEquals(
            mapOf("APP_SEED" to "true"),
            BootRunDemoDefaults.environmentOverrides(null, "prod", null),
        )
    }

    @Test
    fun explicitSeedStillUsesLocalProfileWhenOptOutIsBlank() {
        assertEquals(
            mapOf("SPRING_PROFILES_ACTIVE" to "local"),
            BootRunDemoDefaults.environmentOverrides("true", null, null),
        )
    }
}
