---
name: kotlin-devin
description: Full Kotlin/Spring Boot engineering workflow — Architect, Backend, Test, and Reviewer with validation loops.
disable-model-invocation: true
---

When present in the workspace, load:
@CLAUDE.md
@memory/memory.md
@memory/context.md
@memory/error_patterns.md

MANDATORY EXECUTION (Kotlin / Spring Boot):

1. Read memory + patterns (when available)
2. Classify task complexity
3. If large → **kotlin-architect** approval REQUIRED before implementation

4. Execute agents in order:
   kotlin-architect → kotlin-backend-engineer → test-engineer → reviewer

5. Validation:
   - `./gradlew build` or `./gradlew test` (or Maven equivalent)
   - Fix failures before continuing

6. If logs detected:
   diagnostics → self-healing → reviewer

7. QUALITY LOOP — repeat until:
   - build and tests pass
   - architecture approved
   - no violations
   - no obvious improvements remain

8. FAIL-FAST: if uncertain → STOP and re-plan

9. MEMORY UPDATE: store mistake + fix + rule (when memory files exist)
