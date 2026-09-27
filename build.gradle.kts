plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dep.mgmt) apply false
    id("precision-farming.boot-run-demo") apply false
}

subprojects {
    group = "com.precisionfarming"
    version = "0.1.0"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(26))
            }
        }
        // Load Byte Buddy at JVM start so MockK does not need runtime attach (JDK 26 / Docker).
        val byteBuddyAgent = configurations.maybeCreate("byteBuddyAgent")
        dependencies.add(byteBuddyAgent.name, libs.byte.buddy.agent)
        tasks.withType<Test>().configureEach {
            val agentJar = byteBuddyAgent.elements.map { files -> files.single().asFile.absolutePath }
            jvmArgumentProviders.add(
                CommandLineArgumentProvider {
                    listOf("-javaagent:${agentJar.get()}")
                },
            )
        }
    }

    // bootRun defaults live in build-logic (BootRunDemoPlugin). bootJar is not touched.
    plugins.withId("org.springframework.boot") {
        apply(plugin = "precision-farming.boot-run-demo")
    }
}

tasks.register("testBootRunDefaults") {
    group = "verification"
    description = "Kotlin tests for bootRun demo env defaults (ALLOW_DEMO_SECRETS opt-out, no bootJar injection)."
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}

tasks.register("seedDemoData") {
    group = "demo"
    doLast {
        println("With APP_SEED=true services seed on startup. Or POST /api/v1/dev/seed/reset through the gateway.")
    }
}
