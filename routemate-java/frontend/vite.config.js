import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies /api to the Spring Boot backend so the browser
// makes same-origin calls (no CORS needed in development).
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: process.env.API_TARGET || 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
});
