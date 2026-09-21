plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}


dependencies {
    api(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
