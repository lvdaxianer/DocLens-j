import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

const dashboardApiTarget = process.env.DASHBOARD_API_TARGET ?? 'http://127.0.0.1:10003'

export default defineConfig({
  base: '/dashboard/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    emptyOutDir: true,
    outDir: '../doclens-server/src/main/resources/static/dashboard',
    rollupOptions: {
      output: {
        manualChunks: {
          charts: ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers'],
          framework: ['pinia', 'vue', 'vue-router'],
          icons: ['@lucide/vue'],
          ui: ['naive-ui']
        }
      }
    }
  },
  server: {
    port: 10002,
    proxy: {
      '/api': {
        target: dashboardApiTarget,
        changeOrigin: true
      }
    }
  }
})
