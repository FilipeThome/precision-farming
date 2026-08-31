---
name: kmp-devin
description: Full Kotlin Multiplatform / Compose Multiplatform engineering workflow — Architect, Engineer, Test, and Reviewer with validation loops. Use when working on KMP, Compose Multiplatform, Android, or shared iOS Kotlin UI.
disable-model-invocation: true
---

When present in the workspace, load:
@CLAUDE.md
@AGENTS.md
@memory/memory.md
@memory/context.md
@memory/error_patterns.md

MANDATORY EXECUTION (KMP / Compose Multiplatform):

1. Read memory + patterns (when available)
2. Classify task complexity
3. If large → **kmp-architect** approval REQUIRED before implementation

4. Execute agents in order:
   kmp-architect → kmp-engineer → test-engineer → reviewer

5. Validation:
   - `./gradlew :mobile:composeApp:compileDebugKotlinAndroid` (Windows/Linux)
   - `./gradlew :mobile:composeApp:commonTest` (or equivalent)
   - iOS `xcodebuild` only on macOS
   - Fix failures before continuing

6. Platform bugs:
   reproduce on target → fix via expect/actual if SDK-specific → verify

7. QUALITY LOOP — repeat until:
   - commonMain compiles
   - Android compile + tests pass
   - architecture approved
   - no secrets in source
   - no obvious improvements remain

8. FAIL-FAST: if uncertain → STOP and re-plan

9. MEMORY UPDATE: store mistake + fix + rule (when memory files exist)
