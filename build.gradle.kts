plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dep.mgmt) apply false
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
}

tasks.register("seedDemoData") {
    group = "demo"
    doLast {
        println("With APP_SEED=true services seed on startup. Or POST /api/v1/dev/seed/reset through the gateway.")
    }
}
