plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.20")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

gradlePlugin {
    plugins {
        create("bootRunDemo") {
            id = "precision-farming.boot-run-demo"
            implementationClass = "precisionfarming.gradle.BootRunDemoPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
