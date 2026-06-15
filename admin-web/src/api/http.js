import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import router from '../router'

// 各服务开发期通过 Vite proxy 前缀访问（见 vite.config.js）。生产改为网关统一前缀即可。
export const SVC = {
  product: '/svc/product',
  pricing: '/svc/pricing',
  member: '/svc/member',
  inventory: '/svc/inventory',
  payment: '/svc/payment',
  ticket: '/svc/ticket',
  order: '/svc/order',
  reconciliation: '/svc/reconciliation',
}

const http = axios.create({ timeout: 15000 })

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  config.headers['X-Tenant-Id'] = auth.tenantId
  if (auth.adminToken) config.headers['X-Admin-Token'] = auth.adminToken
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 后端统一响应 {code,message,data,trace_id}
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 'OK') return body.data
      ElMessage.error(body.message || body.code)
      return Promise.reject(new Error(body.message || body.code))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      ElMessage.error('管理员令牌无效，请重新登录')
      useAuthStore().logout()
      router.push('/login')
    } else {
      const msg = err.response?.data?.message || err.message || '请求失败'
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  },
)

export default http
