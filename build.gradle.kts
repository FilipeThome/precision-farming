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
}

tasks.register("seedDemoData") {
    group = "demo"
    doLast {
        println("With APP_SEED=true services seed on startup. Or POST /api/v1/dev/seed/reset through the gateway.")
    }
}
