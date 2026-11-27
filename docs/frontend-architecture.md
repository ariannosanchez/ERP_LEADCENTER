# Frontend architecture

The web client in `frontend/` is a Vite + React 19 + TypeScript SPA. The UI is Spanish-only;
code, identifiers and comments are English.

## Stack

| Concern | Choice |
|---|---|
| Build / dev server | Vite (dev server proxies `/api` to Spring Boot, same-origin like production) |
| Styling / components | Tailwind CSS 4 + shadcn/ui (Radix, `neutral` base color) |
| Routing | React Router |
| Server state | TanStack Query |
| API client | orval-generated TanStack Query hooks from the backend OpenAPI document |
| Forms | react-hook-form + Zod (`@hookform/resolvers`) |
| UI state | Zustand (UI-only state, never server data) |
| Lint / format | oxlint, Prettier |
| Tests | Vitest + Testing Library (jsdom) |

Realtime (`@stomp/stompjs`) is deferred to the leads slice.

## Layout

```
src/
  app/        providers.tsx, router.tsx, protected-route.tsx, layout/ (shell), pages/
  features/   one folder per business slice (pages, components, hooks, schemas, realtime, index.ts)
  shared/
    api/      http.ts (fetch wrapper) and generated/ (orval output, never hand-edited)
    auth/     auth context, <Can>, useHasPermission (presentation only)
    lib/      cn, date/currency formatting (America/Lima, PEN), Zustand UI store
    ui/       shadcn primitives actually used
  test/       Vitest setup
```

Path alias: `@/` maps to `src/`. shadcn is configured in `components.json` so primitives land
in `src/shared/ui` and utilities in `src/shared/lib`.

## Dependency rules

- One-way: `app -> features -> shared`.
- No cross-feature internal imports: other layers use a feature only via `@/features/<name>`
  (its `index.ts`).

Enforced by `no-restricted-imports` overrides in `frontend/.oxlintrc.json`, so `pnpm lint`
fails on a violation. Limitation: a relative import that reaches another feature without a
`features/` path segment is not caught; use the `@/` alias across slices.

## HTTP and errors

`src/shared/api/http.ts` calls the same-origin `/api`, always with `credentials: 'include'`.
On `POST/PUT/PATCH/DELETE` it copies the `XSRF-TOKEN` cookie into the `X-XSRF-TOKEN` header.
Non-2xx responses throw `ApiError`, parsed from RFC 9457 Problem Details
(`status`, `title`, `detail`, `type`, and the `errors[{field,message}]` validation list;
`fieldErrors()` maps it onto form fields). Non-JSON error bodies still become an `ApiError`.
`customFetch` is the orval mutator and returns the `{ data, status, headers }` envelope.

## Permissions and auth

`src/shared/auth` exposes an auth context (`status`, `permissions`), `useHasPermission` and
`<Can permission="...">`. They only hide or show UI; the backend enforces every permission.
`src/app/protected-route.tsx` redirects anonymous users to `/login`. Until T8 feeds the context
from the current-user endpoint, `src/app/providers.tsx` supplies a placeholder authenticated
session with no permissions.

## API sync flow

The generated client is derived from a committed OpenAPI snapshot:

1. Export the snapshot from the real backend (Docker required):

   ```bash
   cd backend && ./mvnw -B -Dtest=OpenApiExportIT -Dopenapi.export.path=../frontend/openapi/erp-api.json test
   ```

2. Regenerate the client:

   ```bash
   cd frontend && pnpm api:generate
   ```

3. Commit `frontend/openapi/erp-api.json` and `frontend/src/shared/api/generated/` together with
   the backend change. Never edit generated files by hand.

## Commands

```bash
cd frontend
pnpm dev            # dev server on :5173
pnpm lint           # oxlint, including the dependency rules
pnpm format         # prettier --write
pnpm format:check   # prettier --check
pnpm test           # vitest (watch); add --run for a single pass
pnpm build          # type check + production build
pnpm api:generate   # regenerate the API client
```
