import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    process.env.NODE_ENV === 'development' && vueDevTools(),
  ].filter(Boolean),
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }
          if (id.includes('element-plus')) {
            const componentMatch = id.match(/element-plus[\\/](es|lib)[\\/]components[\\/]+([^\\/]+)/)
            if (componentMatch?.[2]) {
              return `ep-${componentMatch[2]}`
            }
            return 'element-plus-base'
          }
          if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
            return 'vue-vendor'
          }
          return 'vendor'
        },
      },
    },
  },
  server: {
    allowedHosts: ['reference-request-stumble.ngrok-free.dev'],
    proxy: {
      '/api': {
        target: process.env.API_PROXY_TARGET || 'http://localhost:8880',
        changeOrigin: true,
      },
    },
  },
})
