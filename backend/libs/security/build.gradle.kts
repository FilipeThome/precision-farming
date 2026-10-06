plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.dep.mgmt)
}


dependencies {
    api(project(":backend:libs:common"))
    implementation(platform(libs.spring.boot.bom))
    api(libs.spring.boot.web)
    implementation("org.springframework:spring-tx")
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
