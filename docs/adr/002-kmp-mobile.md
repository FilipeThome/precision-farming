# ADR-002: Kotlin Multiplatform for mobile

## Status

Accepted

## Context

The spec asked for native SwiftUI iOS and Jetpack Compose Android. The team is on Windows and chose a single Kotlin UI codebase.

## Decision

Use Compose Multiplatform + KMP for Android and iOS. Shared `commonMain` holds domain, HTTP, offline queue, and UI. Google Maps uses `expect`/`actual` (Maps Compose on Android, Maps SDK + UIKitView on iOS). A thin Swift host lives in `mobile/iosApp`.

The current Windows slice stays a single Android `composeApp` module (KMP `shared` / `androidApp` / `iosApp` extract is deferred). Clients still call only `http://localhost:8080/api/v1`.

## Toolchain (2026-09)

Mobile is a **separate** Gradle project (`./gradlew -p mobile`) on the repo-root wrapper **Gradle 9.7.1**.

| Piece | Version | Notes |
| --- | --- | --- |
| JDK (Gradle daemon + `jvmToolchain`) | **26** | Foojay resolver in `mobile/settings.gradle.kts` (same plugin as backend). |
| Android `jvmTarget` / `compileOptions` | **26** | AGP 9.4 D8 dexed class file major 70 (`:composeApp:mergeProjectDexDebug` green). `minSdk 26` is Android 8.0, not Java 26 — keep it unless a library requires a bump. |
| Kotlin + Compose compiler plugin + serialization plugin | **2.4.20** | Matches backend. AGP 9 built-in Kotlin; do **not** apply `org.jetbrains.kotlin.android`. Compose compiler version is the Kotlin version. |
| AGP | **9.4.0** | Requires Gradle ≥ 9.6.0; max API 37. |
| compileSdk / targetSdk | **37** | Compose 1.12 / BOM 2026.09 requires compileSdk 37. |
| Compose BOM | **2026.09.00** | |
| Ktor | **3.5.2** | |

No Spring/JPA types in mobile modules. Spring Boot 4 is backend-only.

## Consequences

- iOS UI is Compose, not SwiftUI.
- `xcodebuild` still requires macOS; Windows validates unit tests and Android compile.
- Business rules are not duplicated across mobile platforms.
- Install Android SDK platform `platforms;android-37` (and Build-Tools 36+) before `:composeApp:compileDebugKotlin`.
