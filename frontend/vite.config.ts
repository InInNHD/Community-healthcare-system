import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'

const backendProxy = { '/api': 'http://localhost:8080' }

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: backendProxy,
  },
  preview: {
    port: 5173,
    strictPort: true,
    proxy: backendProxy,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rolldownOptions: {
      input: {
        index: fileURLToPath(new URL('./index.html', import.meta.url)),
        admin: fileURLToPath(new URL('./admin.html', import.meta.url)),
        staff: fileURLToPath(new URL('./staff.html', import.meta.url)),
        resident: fileURLToPath(new URL('./resident.html', import.meta.url)),
      },
    },
  },
})
