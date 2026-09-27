package precisionfarming.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

/**
 * Local demo defaults for `bootRun`. `providers.environmentVariable` follows the build
 * invocation, not the Gradle daemon process environment.
 */
class BootRunDemoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.withType(JavaExec::class.java).configureEach { task ->
            if (task.name != "bootRun") return@configureEach
            val appSeed = target.providers.environmentVariable("APP_SEED")
            val springProfile = target.providers.environmentVariable("SPRING_PROFILES_ACTIVE")
            val allowDemo = target.providers.environmentVariable("ALLOW_DEMO_SECRETS")
            task.doFirst {
                BootRunDemoDefaults.environmentOverrides(
                    appSeed.orNull,
                    springProfile.orNull,
                    allowDemo.orNull,
                ).forEach { (key, value) ->
                    task.environment(key, value)
                }
            }
        }
    }
}
