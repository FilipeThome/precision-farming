---
name: cybersecurity-team
description: >-
  Authorized defensive security assessment for Precision Farming (web, backend,
  mobile). Runs blue, purple, red, and pentester perspectives to find
  vulnerabilities and recommend hardening. Use when the user asks for security
  scan, pentest review, cyber team audit, or vulnerability assessment.
---

# Cybersecurity Team (Authorized Assessment)

Use this skill only on code/systems the user owns or is explicitly authorized to assess (this monorepo and local Compose stack).

## Hard limits (never violate)

- **No exploit PoCs**, payloads, attack scripts, or step-by-step exploitation.
- **No** unauthorized access guidance against third-party systems.
- Red team / pentester output = **attack surface + impact + remediation**, not how to break in.
- Prefer fixing/hardening recommendations over offensive detail.

## Team roles

| Role | Focus | Typical outputs |
| --- | --- | --- |
| **Blue Team** | Controls, monitoring, secure defaults, secrets, config | Misconfigs, missing authZ, logging/audit gaps, hardening checklist |
| **Purple Team** | Map threats → controls; validate whether defenses would catch them | Control gaps, detection blind spots, prioritized fix order |
| **Red Team** | Adversary thinking (threat model, abuse cases) | High-value targets, trust-boundary failures, privilege escalation paths (descriptive) |
| **Pentester** | Structured app assessment (OWASP ASVS / API Top 10 lens) | Finding table: Severity, Location, Finding, Remediation |

## When to run

- User asks for security scan, pentest, cyber team, vulnerability review
- Before merging large auth/API/gateway changes
- After adding new microservices or public routes

## Procedure

1. **Inventory attack surface**
   - Gateway routes (`backend/gateway/**/application.yml`)
   - `SecurityConfig` / `GatewaySecurityConfig` permitAll paths
   - JWT claims and role usage
   - Client token storage (web Zustand, mobile Session)
   - Seed/dev endpoints, demo credentials, `.env.example`
   - Cross-service trust (minted service JWT, RestClient)

2. **Static greps (Blue + Pentester)**
   - Secrets: `password|secret|api[_-]?key|Bearer |private_key` in sources (exclude build/node_modules)
   - Auth gaps: `permitAll`, `csrf`, `CORS`, `allowedOriginPatterns`
   - Injection sinks: raw SQL, `Runtime.exec`, SpEL, unsafe deserialization
   - IDOR patterns: `findById` / `findAll` without farm/tenant scope
   - Path traversal / SSRF: user-controlled URLs/paths

3. **Diff-focused review**
   - Launch Cursor `security-review` subagent with:
     ```
     Full Repository Path: <repo root>
     Diff: branch changes
     Base Branch: main
     Custom Instructions: Focus on authZ, IDOR, JWT, secrets, gateway exposure; no exploit PoCs.
     ```
   - Or `Diff: uncommitted changes` if user asks for dirty tree only.

4. **Purple synthesis**
   - For each High/Critical finding: which blue control should exist (JWT claim scope, `@PreAuthorize`, network bind, profile-gated seed)?
   - Note if logging/audit would detect the abuse.

5. **Report format (always)**
   - Short executive summary (1–3 sentences)
   - Markdown table sorted by severity: **Severity | Location | Finding | Remediation**
   - Deferred / accepted risks for demo MVP (label clearly)
   - Do **not** auto-fix unless the user asks

## Stack-specific checklist (Precision Farming)

- [ ] Clients only hit gateway `:8080`; services not public in Compose/prod
- [ ] JWT secret not default outside `local` profile
- [ ] Farm/tenant scope on every list/get/mutate
- [ ] Role gates on approve/complete/dispatch/admin
- [ ] `/api/v1/dev/seed/**` auth or profile-gated
- [ ] CORS not `*` + credentials in non-local
- [ ] No secrets in git; Maps keys via env
- [ ] Saga/service tokens short-lived and least-privilege
- [ ] Web token memory-only; mobile secure storage for production path

## Related

- Built-in Cursor skill: `review-security` → `security-review` subagent
- Inspired by public skills for white-box review (e.g. EastSword code security review) and red/blue synthesis (e.g. ECC AgentShield opus pipeline for *agent configs* — different scope). This skill targets **application** code.
