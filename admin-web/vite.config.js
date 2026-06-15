import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发期反向代理到各微服务（避免浏览器跨域）。前缀 → 服务端口，详见 src/api/http.js。
// 生产环境应由网关统一路由 + 鉴权，这里仅本地开发用。
const svc = (port) => ({ target: `http://localhost:${port}`, changeOrigin: true, rewrite: (p) => p.replace(/^\/svc\/[^/]+/, '') })

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/svc/product': svc(8081),
      '/svc/pricing': svc(8082),
      '/svc/member': svc(8083),
      '/svc/inventory': svc(8084),
      '/svc/payment': svc(8085),
      '/svc/ticket': svc(8086),
      '/svc/order': svc(8087),
      '/svc/reconciliation': svc(8093),
    },
  },
})
