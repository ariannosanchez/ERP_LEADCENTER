import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Same-origin in development: the SPA calls /api and Vite forwards it to Spring Boot,
    // mirroring the production reverse proxy so session cookies behave identically.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
