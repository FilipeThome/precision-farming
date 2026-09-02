plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dep.mgmt)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    implementation(project(":backend:libs:common"))
    implementation(project(":backend:libs:security"))
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.jpa)
    implementation(libs.spring.boot.validation)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.security)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.postgresql)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc)
    implementation(libs.micrometer.prometheus)
    implementation(libs.spring.boot.amqp)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("agronomy-service")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
