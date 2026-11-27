# Features

Each business capability is a vertical slice under `src/features/<feature>/`:

```
src/features/<feature>/
  pages/        route-level screens (composed by src/app/router.tsx)
  components/   UI used only by this feature
  hooks/        feature hooks (TanStack Query wrappers over the generated client)
  schemas/      Zod schemas for forms and input validation
  realtime/     STOMP subscriptions that invalidate this feature's queries
  index.ts      the ONLY public entry of the slice
```

Rules (enforced by `pnpm lint`):

- Dependencies are one-way: `app -> features -> shared`. A feature never imports `src/app`.
- `src/app` and other features import a feature only through `@/features/<feature>`
  (its `index.ts`). Deep imports such as `@/features/leads/components/x` are lint errors.
- Anything reused by two or more features moves down to `src/shared`.
- Use the `@/` alias for cross-layer imports; relative imports are for files inside the same slice.
- Permissions in the UI (`<Can>`, `useHasPermission`) are presentation only; the backend enforces.

Realtime (`@stomp/stompjs`) is deliberately not installed yet. It is added together with the
leads slice, which is the first consumer: its `realtime/` folder subscribes to
`/user/queue/...` and invalidates queries; REST stays the source of truth.

No business features exist yet; login, users and roles arrive in T8.
