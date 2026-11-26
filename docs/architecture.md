# Backend architecture

The API (`backend/`) is a **modular monolith**: Java 25, Spring Boot 4.1.3, Spring Modulith 2.1.1,
PostgreSQL, Flyway. Each business domain is a Spring Modulith module; inside a module the code
follows **hexagonal architecture with vertical slicing**.

Base package: `pe.leadcenter.erp`. Every direct sub-package is a module
(`pe.leadcenter.erp.<module>`), except `shared`, the shared kernel.

## Module layout

```
pe.leadcenter.erp.<module>
├── (root)                    public API of the module + public events (the only thing others may use)
├── domain/                   pure Java: aggregates, value objects, domain services, out ports
├── application/<usecase>/    vertical slice: command/query (records), use case port, handler
└── infrastructure/
    ├── in/{web,webhook}/     inbound adapters (REST controllers, webhook endpoints)
    └── out/{persistence,realtime}/   outbound adapters (JPA entities + repositories, WebSocket/STOMP)
```

Rules, enforced by ArchUnit (`src/test/java/pe/leadcenter/erp/architecture`):

- `domain` must not depend on Spring, Jakarta Persistence, `application` or `infrastructure`.
- `application` must not depend on `infrastructure`.
- `infrastructure.in` must not depend on `infrastructure.out` (adapters talk through application ports).

`ModularityTests` (Spring Modulith `verify()`) additionally fails on cycles and on access to another
module's internal packages.

Domain model and persistence model are **separate**: the domain is plain Java, JPA entities live in
`infrastructure/out/persistence` and are converted with MapStruct.

## Cross-module communication

A module may only use another module's **root package** (its public API) or react to its **public
events**. Prefer events (Spring Modulith `ApplicationEventPublisher` / `@ApplicationModuleListener`);
they are stored in the event publication registry (`event_publication` table) so delivery survives
failures and restarts. Never import `domain`, `application` or `infrastructure` classes of another module.

## Shared kernel (`pe.leadcenter.erp.shared`)

Declared `@ApplicationModule(type = OPEN)`: any module may use it; it must not depend on a module.

| Package | Content |
|---|---|
| `shared.domain` | `UuidV7`, `DomainException`, `NotFoundException`, `BusinessRuleViolationException` (pure Java) |
| `shared.security` | `SecurityConfig`, `AuthenticatedUser` (principal contract), `CurrentUser` |
| `shared.infrastructure.web` | `GlobalExceptionHandler` (Problem Details), `ApiVersionConfig` (`/v1` prefix) |
| `shared.infrastructure.persistence` | `AuditedEntity`, `SoftDeletableAuditedEntity`, JPA auditing config, Envers revision entity |

The kernel keeps its adapters flat (`infrastructure/web`, `infrastructure/persistence`) instead of `in/out`.

## Ids

Ids are **UUID v7** generated in the domain: `UuidV7.generate()` (time-ordered, strictly increasing
within a JVM, no Spring/JPA imports). Create the id in the aggregate's constructor/factory and
persist it as-is; never use database generators for business ids.

## Lombok and MapStruct

- No `@Data` on JPA entities (use `@Getter`, `@NoArgsConstructor(access = PROTECTED)`; `equals/hashCode`
  by id only if needed).
- No `@Setter` in the domain: state changes go through intention-revealing methods.
- Commands, queries and DTOs are Java `record`s.
- MapStruct mappers (`@Mapper`, Spring component model is the default via `-Amapstruct.defaultComponentModel=spring`)
  live next to the adapter that needs them (e.g. `infrastructure/out/persistence`, `infrastructure/in/web`).
- Annotation processor order in `pom.xml` matters and must stay: Lombok, `lombok-mapstruct-binding`,
  MapStruct processor, Error Prone.

## Auditing, soft delete and history (opt-in per entity)

Schema is owned by **Flyway** (`ddl-auto=validate`); every table, `*_aud` history table and column is
created in a versioned migration.

1. **Auditing** (every table): extend `AuditedEntity` for `created_at`, `created_by`, `updated_at`,
   `updated_by` (`timestamptz` in UTC, users as `uuid`). `*_by` comes from `CurrentUser`
   (principal implementing `AuthenticatedUser`, or an authentication whose name is a UUID) and is
   `NULL` for unauthenticated/system writes.
2. **Soft delete** (business entities): extend `SoftDeletableAuditedEntity` instead. `delete` sets
   `deleted_at` (nullable `timestamptz`) and Hibernate hides deleted rows. Add the column in the migration.
3. **History** (sensitive entities: leads, sales, permissions): annotate the entity with
   `@org.hibernate.envers.Audited` and create `<table>_aud` in the migration with `id`, `rev bigint`
   (FK to `revinfo`), `revtype smallint`, and a nullable copy of each audited column. `revinfo`
   (revision header with `modified_by`) already exists (`V3`).

Minimal example (see the test entity `SampleRecord` and `V9000__test_sample_entity.sql`):

```java
@Entity @Table(name = "lead") @Audited
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
class LeadEntity extends SoftDeletableAuditedEntity {
    @Id private UUID id;
    ...
}
```

## Time

Store and process in UTC (JVM default zone set in `ErpApiApplication`, `-Duser.timezone=UTC` in tests
and `spring-boot:run`, `hibernate.jdbc.time_zone=UTC`). Use `Instant` / `timestamptz`. Display in
`America/Lima` is a client concern.

## REST API

- Public base: `/api/v1` (servlet context path `/api` + `/v1`). `ApiVersionConfig` prefixes every
  application `@RestController` with `/v1`; declare mappings **without** it (`@GetMapping("/leads")`).
- Actuator (`/api/actuator/health`, public) and springdoc (`/api/v3/api-docs`, `/api/docs`, authenticated)
  are not prefixed.
- Everything except the health probe requires authentication (`SecurityConfig`).
- Pagination: `page`, `size`, `sort` query parameters.

## Error format (RFC 9457 Problem Details)

All errors are `application/problem+json` produced by `GlobalExceptionHandler`:

| Case | Status | `type` |
|---|---|---|
| Bean validation | 400 | `urn:problem-type:validation-error`, with `errors: [{field, message}]` |
| `NotFoundException` | 404 | `urn:problem-type:not-found`, with `code` |
| `BusinessRuleViolationException` | 422 | `urn:problem-type:business-rule-violation`, with `code` |
| Other `DomainException` | 422 | `urn:problem-type:domain-error`, with `code` |
| Unexpected | 500 | `urn:problem-type:internal-error`, generic detail, nothing internal leaked |

Throw `DomainException` subclasses from the domain/application with a stable `code`
(e.g. `user.email_taken`). Spring Security exceptions are left to the security filter chain (401/403).

## Quality tooling

| Tool | Use |
|---|---|
| Spotless (google-java-format, AOSP style) | `./mvnw spotless:apply` / `./mvnw spotless:check` |
| Error Prone | runs on every compile (static analysis; chosen because it supports JDK 25) |
| JaCoCo | report at `target/site/jacoco/index.html` after `./mvnw test` |
| ArchUnit + Modulith `verify()` | layering and module boundaries |

Build and test: `cd backend && ./mvnw -B spotless:check verify` (Docker required for Testcontainers).
