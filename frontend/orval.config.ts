import { defineConfig } from 'orval'

// Generates the TanStack Query client from the backend OpenAPI snapshot.
// Output in src/shared/api/generated is never hand-edited: re-run `pnpm api:generate`.
export default defineConfig({
  erp: {
    input: './openapi/erp-api.json',
    output: {
      mode: 'tags-split',
      packageJson: './package.json',
      target: './src/shared/api/generated/endpoints',
      schemas: './src/shared/api/generated/model',
      client: 'react-query',
      httpClient: 'fetch',
      clean: true,
      override: {
        query: { version: 5 },
        mutator: {
          path: './src/shared/api/http.ts',
          name: 'customFetch',
        },
      },
    },
  },
})
