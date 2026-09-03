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

    // Load Byte Buddy at JVM start so MockK does not need runtime attach (JDK 21 / Docker).
    plugins.withId("org.jetbrains.kotlin.jvm") {
        val byteBuddyAgent = configurations.maybeCreate("byteBuddyAgent")
        dependencies.add(byteBuddyAgent.name, "net.bytebuddy:byte-buddy-agent:1.17.7")
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
