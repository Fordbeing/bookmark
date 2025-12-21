import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    base: '/',  // 部署到根路径 (配合独立端口使用)
    server: {
        port: 5174,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
    resolve: {
        alias: {
            '@': '/src'
        }
    }
})
