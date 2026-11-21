import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发环境用 Vite 代理转发到后端，避免额外的跨域配置：
//   /api/**       -> app 模块（用户端接口，8080）
//   /admin-api/** -> console 模块（管理端接口，8081）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/admin-api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/admin-api/, '')
      }
    }
  },
  build: {
    // Element Plus 全量引入后包体较大，这里放宽提示阈值，避免构建时刷警告
    chunkSizeWarningLimit: 1200
  }
})
