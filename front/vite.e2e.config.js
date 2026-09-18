import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  envDir: false,
  build: { outDir: 'dist-e2e' },
  preview: {
    host: '127.0.0.1', port: 15173, strictPort: true,
    // No API proxy: tests must use the gateway public origin, never this asset server.
  },
});
