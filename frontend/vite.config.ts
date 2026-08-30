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
      output: {
        codeSplitting: {
          minSize: 20_000,
          maxSize: 420_000,
          groups: [
            {
              name: 'element-plus',
              test: /node_modules[\\/]element-plus/,
              priority: 20,
              includeDependenciesRecursively: false,
            },
            {
              name: 'vue-vendor',
              test: /node_modules[\\/](?:vue|vue-router|@vue)[\\/]/,
              priority: 10,
              includeDependenciesRecursively: false,
            },
            {
              name: 'vendor',
              test: /node_modules/,
              priority: 1,
              includeDependenciesRecursively: false,
            },
          ],
        },
      },
    },
  },
})
