rootProject.name = "precision-farming"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("backend/gradle/libs.versions.toml"))
        }
    }
}

include("backend:libs:common")
include("backend:libs:security")
include("backend:libs:security-issuer")
include("backend:gateway")
include("backend:services:auth")
include("backend:services:farm")
include("backend:services:asset")
include("backend:services:telemetry")
include("backend:services:weather")
include("backend:services:operation")
include("backend:services:inventory")
include("backend:services:alert")
include("backend:services:ai")
include("backend:services:notification")
include("backend:services:file")
include("backend:services:reporting")
include("backend:services:sync")
include("backend:services:integration")
include("backend:services:agronomy")
include("backend:services:irrigation")
include("backend:services:harvest")
include("backend:services:finance")
include("backend:services:compliance")
