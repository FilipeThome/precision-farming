# ADR-002: Kotlin Multiplatform for mobile

## Status

Accepted

## Context

The spec asked for native SwiftUI iOS and Jetpack Compose Android. The team is on Windows and chose a single Kotlin UI codebase.

## Decision

Use Compose Multiplatform + KMP for Android and iOS. Shared `commonMain` holds domain, HTTP, offline queue, and UI. Google Maps uses `expect`/`actual` (Maps Compose on Android, Maps SDK + UIKitView on iOS). A thin Swift host lives in `mobile/iosApp`.

## Consequences

- iOS UI is Compose, not SwiftUI.
- `xcodebuild` still requires macOS; Windows validates `commonTest` and Android.
- Business rules are not duplicated across mobile platforms.
