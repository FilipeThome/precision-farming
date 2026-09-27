package precisionfarming.gradle

/**
 * Env injected into Gradle `bootRun` only. `bootJar` never calls this, so a packaged
 * jar stays fail-closed unless the process itself sets ALLOW_DEMO_SECRETS or APP_SEED.
 */
object BootRunDemoDefaults {
    fun environmentOverrides(
        appSeed: String?,
        springProfile: String?,
        allowDemoSecrets: String?,
    ): Map<String, String> {
        val overrides = linkedMapOf<String, String>()
        if (appSeed.isNullOrBlank()) overrides["APP_SEED"] = "true"
        if (springProfile.isNullOrBlank() && allowDemoSecrets.isNullOrBlank()) {
            overrides["SPRING_PROFILES_ACTIVE"] = "local"
        }
        return overrides
    }
}
