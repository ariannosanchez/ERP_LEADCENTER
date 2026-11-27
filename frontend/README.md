# LeadCenter ERP — web

React 19 + Vite + TypeScript SPA. See [../docs/frontend-architecture.md](../docs/frontend-architecture.md)
for the structure, dependency rules and the OpenAPI sync flow.

```bash
pnpm install
pnpm dev            # http://localhost:5173, proxies /api to the backend
pnpm lint && pnpm format:check && pnpm test --run && pnpm build
```
