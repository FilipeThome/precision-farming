# ADR-003: Spring Boot 4 / Java 26

## Status

Accepted (supersedes the Boot 3.5 / Java 21 first-cut decision)

## Context

The MVP originally shipped on Spring Boot 3.5.x and Java 21 to reduce first-compile ecosystem risk (Springdoc, Cloud Gateway, Hibernate Spatial). Boot 4.1 is GA, Spring Cloud 2025.1.x (Oakwood) tracks Boot 4.0/4.1, Springdoc 3.1 targets Boot 4, and Java 26 is a supported Boot 4.1 runtime. The demo slice is green enough to take the framework generation that was deferred.

## Decision

Run backend services and the gateway on:

- **Java 26** (Gradle toolchain + Foojay resolver; servlet apps keep `spring.threads.virtual.enabled`)
- **Kotlin 2.4.20**
- **Spring Boot 4.1.1** with **Spring Cloud 2025.1.3**
- **Gradle 9.7.1** (Java 26 requires Gradle ≥ 9.4.0)

Clients still call only `http://localhost:8080/api/v1`. Database per service. Domain stays free of Spring/JPA. DTOs at HTTP edges. JWT on the gateway and each service. Mobile uses JDK **26** for Gradle/`jvmToolchain` and Android **jvmTarget 26** (AGP 9.4 D8 accepts class file 70). See [ADR-002](002-kmp-mobile.md). Spring Boot 4 stays backend-only.

Starters follow Boot 4 names (`spring-boot-starter-webmvc`, `spring-boot-starter-security-oauth2-resource-server`, `spring-boot-starter-flyway`, Jackson 3 `tools.jackson.*`). Gateway uses `spring-cloud-starter-gateway-server-webflux` and `spring.cloud.gateway.server.webflux.*` properties.

## Consequences

- Jackson 3 package/group-id migration (`com.fasterxml.jackson.databind` → `tools.jackson.databind`); `jackson-annotations` stay on `com.fasterxml.jackson.annotation`.
- Flyway and RestClient auto-config require explicit starters (no classpath-only activation).
- Docker/CI use Eclipse Temurin 26. Gradle downloads JDK 26 via Foojay when the machine JDK is older.
- Mobile Gradle also runs on JDK 26 with Android `jvmTarget` 26 (verified with D8 on AGP 9.4).
