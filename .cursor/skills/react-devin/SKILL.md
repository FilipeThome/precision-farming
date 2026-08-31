---
name: react-devin
description: Full React.js engineering workflow — Architect, Engineer, Test, and Reviewer with validation loops.
disable-model-invocation: true
---

When present in the workspace, load:
@CLAUDE.md
@AGENTS.md
@memory/memory.md
@memory/context.md
@memory/error_patterns.md

MANDATORY EXECUTION (React.js):

1. Read memory + patterns (when available)
2. Classify task complexity
3. If large → **react-architect** approval REQUIRED before implementation

4. Execute agents in order:
   react-architect → react-engineer → test-engineer → reviewer

5. Validation:
   - TypeScript compile (`npm run build` or `tsc --noEmit`)
   - Tests (`npm test` or project equivalent)
   - Fix failures before continuing

6. UI bugs:
   reproduce → fix → verify in browser or test

7. QUALITY LOOP — repeat until:
   - build and tests pass
   - architecture approved
   - no console errors
   - accessibility basics covered
   - no obvious improvements remain

8. FAIL-FAST: if uncertain → STOP and re-plan

9. MEMORY UPDATE: store mistake + fix + rule (when memory files exist)
