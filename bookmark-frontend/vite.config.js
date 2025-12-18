import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 前端运行端口
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 你的 Java 后端地址
        changeOrigin: true,
        // 如果你后端接口就是 /api/bookmarks/... 就不需要 rewrite
        // 如果后端是 /bookmarks/... 则取消下面注释
        // rewrite: (path) => path.replace(/^\/api/, '') 
      }
    }
  }
})