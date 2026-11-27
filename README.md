# LeadCenter ERP

Internal ERP for LeadCenter: leads, clients, sales, users and permissions.

| Part | Path | Stack |
|---|---|---|
| API | `backend/` | Java 25, Spring Boot 4.1, Spring Modulith, PostgreSQL, Flyway (Maven wrapper) |
| Web | `frontend/` | React 19, Vite, TypeScript, Tailwind, shadcn/ui, TanStack Query |
| Local services | `compose.yaml` | PostgreSQL 17 |

## Requirements

- JDK 25
- Node 24 and pnpm 10
- Docker (local database and integration tests)

## Run locally

```bash
docker compose up -d            # PostgreSQL on localhost:5432 (erp/erp)
cd backend && ./mvnw spring-boot:run # API on http://localhost:8080/api
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
| `ERP_BOOTSTRAP_ADMIN_EMAIL` | none |
| `ERP_BOOTSTRAP_ADMIN_PASSWORD` | none (at least 12 characters) |
| `ERP_BOOTSTRAP_ADMIN_FULL_NAME` | `Administrador` |

The `ERP_BOOTSTRAP_ADMIN_*` variables create the initial Administrador account at start. They are read
only when no active administrator exists: set them for the first start, then remove them. Without
them (and with no administrator) the API starts and logs a warning. An invalid value, or an email
that already belongs to a non-administrator, aborts startup with an explanatory message.

## Tests

```bash
cd backend && ./mvnw -B spotless:check verify  # format check + unit, architecture and Testcontainers tests (Docker required)
cd backend && ./mvnw spotless:apply            # auto-format Java sources
cd frontend && pnpm lint && pnpm format:check && pnpm test --run && pnpm build  # lint, format, unit tests, type check + build
cd frontend && pnpm format      # auto-format with Prettier
cd frontend && pnpm api:generate # regenerate the API client from frontend/openapi/erp-api.json
cd backend && ./mvnw -B -Dtest=OpenApiExportIT -Dopenapi.export.path=../frontend/openapi/erp-api.json test  # refresh the OpenAPI snapshot, then api:generate
```

## Architecture

The API is a modular monolith: each business domain is a top-level package under
`pe.leadcenter.erp` treated as a Spring Modulith module. `ModularityTests` fails the build
when a module uses another module's internals. Database changes go only through versioned
Flyway migrations in `backend/src/main/resources/db/migration`. See [docs/architecture.md](docs/architecture.md)
for the module layout, conventions and tooling. The web client structure, dependency rules and
API sync flow are in [docs/frontend-architecture.md](docs/frontend-architecture.md).

## Continuous integration

`.github/workflows/ci.yml` runs on every pull request and on pushes to `main`:

- **Backend:** `./mvnw -B spotless:check verify` on JDK 25 (Testcontainers uses the runner's Docker).
- **Frontend:** `pnpm install --frozen-lockfile`, then `lint`, `format:check`, `test --run` and `build` on Node 24.
