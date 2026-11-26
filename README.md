# LeadCenter ERP

Internal ERP for LeadCenter: leads, clients, sales, users and permissions.

| Part | Path | Stack |
|---|---|---|
| API | `backend/` | Java 25, Spring Boot 4.1, Spring Modulith, PostgreSQL, Flyway |
| Web | `frontend/` | React, Vite, TypeScript |
| Local services | `compose.yaml` | PostgreSQL 17 |

## Requirements

- JDK 25
- Node 24 and pnpm 10
- Docker (local database and integration tests)

## Run locally

```bash
docker compose up -d            # PostgreSQL on localhost:5432 (erp/erp)
cd backend && ./gradlew bootRun # API on http://localhost:8080/api
cd frontend && pnpm install && pnpm dev   # Web on http://localhost:5173, proxies /api to the API
```

- Health: `GET http://localhost:8080/api/actuator/health`
- API docs (requires an authenticated session): `http://localhost:8080/api/docs`

## Configuration

The API reads its database settings from the environment:

| Variable | Default (development only) |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/erp` |
| `DB_USERNAME` | `erp` |
| `DB_PASSWORD` | `erp` |
| `SERVER_PORT` | `8080` |

## Tests

```bash
cd backend && ./gradlew test    # unit, module-boundary and Testcontainers tests (Docker required)
cd frontend && pnpm build       # type check and production build
```

## Architecture

The API is a modular monolith: each business domain is a top-level package under
`pe.leadcenter.erp` treated as a Spring Modulith module. `ModularityTests` fails the build
when a module uses another module's internals. Database changes go only through versioned
Flyway migrations in `backend/src/main/resources/db/migration`.
