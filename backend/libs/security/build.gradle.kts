plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dep.mgmt)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    api(project(":backend:libs:common"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.6"))
    api(libs.spring.boot.web)
    api(libs.spring.boot.security)
    api(libs.spring.boot.oauth2.rs)
    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    api(libs.jackson.kotlin)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.kotlin.test)
    testImplementation(project(":backend:libs:security-issuer"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
