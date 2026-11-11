// Vite 构建配置：开发代理指向本机 Spring Boot 后端，避免前端直接写死后端地址。
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 统一用 @ 指向 src，页面与接口模块按绝对路径互相引用。
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 前端接口一律以 /api 开头，开发态由 Vite 转发到 8080；生产态交给 Nginx。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    // 整包引入 Element Plus 后单文件体积偏大，这里抬高告警阈值避免构建输出噪音。
    chunkSizeWarningLimit: 1600
  }
})
