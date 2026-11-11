import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          const modulePath = id.replaceAll('\\', '/')
          if (modulePath.includes('/node_modules/three/')) return 'three-core'
          if (modulePath.includes('/node_modules/@react-three/')) return 'three-react'
          if (modulePath.includes('/node_modules/camera-controls/') || modulePath.includes('/node_modules/maath/')) return 'three-utils'
          return undefined
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
