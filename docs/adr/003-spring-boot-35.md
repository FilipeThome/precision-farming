# ADR-003: Spring Boot 3.5 for the first cut

## Status

Accepted

## Context

Spring Boot 4.1.1 is current, but Springdoc, Cloud Gateway, and Hibernate Spatial are smoother on the 3.5 line, which remains in OSS support.

## Decision

Implement the MVP on Spring Boot 3.5.x, Spring Cloud 2025.0.x, Kotlin 2.1, **Java 21** (LTS with virtual threads GA). Servlet apps enable `spring.threads.virtual.enabled`. Do not bump the toolchain past the JDK installed on the machine. Revisit Boot 4 after the demo slice is green.

## Consequences

- Slightly older framework generation than 4.1.
- Lower risk of first-compile ecosystem breakage.
