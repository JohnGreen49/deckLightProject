import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// During development the SPA runs on :5173 and proxies API + SSE calls to the
// Spring Boot backend on :8080, so the browser sees a single origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
