# Precision Farming MVP

Microservices Kotlin (Spring Cloud Gateway), React web, KMP mobile.

- Architect approval before large features (`kotlin-architect`, `react-architect`, `kmp-architect`, `infra-architect`).
- Domain has no Spring/JPA. DTOs at HTTP edges. JWT on gateway and each service.
- Demo credentials are local-only (`Precision@123`). Google Maps keys via env.
- Windows: Android + backend + web. iOS xcodebuild requires macOS.
