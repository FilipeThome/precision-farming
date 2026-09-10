plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dep.mgmt)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
    }
}

dependencies {
    implementation(project(":backend:libs:common"))
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.oauth2.rs)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
    testImplementation("org.springframework:spring-web")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("gateway")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
