# ADR-001: Microservices in the MVP

## Status

Accepted

## Context

The implementation spec (§2 rule 8) required a modular monolith. The product prototype and stakeholder decision require independently deployable services, database-per-service, and distributed sagas.

## Decision

Ship the MVP as Spring Boot microservices behind Spring Cloud Gateway. Communication is HTTP (request/response) plus transactional outbox events on RabbitMQ. Operation lifecycle uses orchestrated sagas with compensating actions.

## Consequences

- Higher local operational cost (`docker compose` runs many processes).
- No cross-service SQL or 2PC.
- OpenAPI per service; clients use the gateway prefix `/api/v1`.
