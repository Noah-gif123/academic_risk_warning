import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true }
    },
    // Windows 上编辑器/构建工具同时写文件时，HMR 偶尔会读到被占用的文件（EBUSY），
    // awaitWriteFinish 等文件写完再读；再关掉阻塞式报错遮罩，避免整页被挡住。
    watch: {
      awaitWriteFinish: { stabilityThreshold: 400, pollInterval: 100 }
    },
    hmr: { overlay: false }
  },
  build: { outDir: 'dist', chunkSizeWarningLimit: 1500 }
})
