import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// /api ki saari requests backend (Spring Boot, port 8080) ko proxy hoti hain,
// isliye frontend me koi URL / CORS change nahi karna padta.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
