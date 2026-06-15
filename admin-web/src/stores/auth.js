import { defineStore } from 'pinia'

/**
 * 管理后台会话：v1 用「管理员令牌 + 租户 ID」（对应后端 AdminAuthFilter 的 X-Admin-Token + X-Tenant-Id）。
 * 细粒度 RBAC（ADR-011）后续替换为真实账号登录。令牌/租户存 localStorage，请求拦截器自动附带。
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    adminToken: localStorage.getItem('adminToken') || '',
    tenantId: localStorage.getItem('tenantId') || '1001',
  }),
  getters: {
    isLoggedIn: (s) => !!s.adminToken && !!s.tenantId,
  },
  actions: {
    login(adminToken, tenantId) {
      this.adminToken = adminToken
      this.tenantId = tenantId
      localStorage.setItem('adminToken', adminToken)
      localStorage.setItem('tenantId', tenantId)
    },
    logout() {
      this.adminToken = ''
      localStorage.removeItem('adminToken')
    },
  },
})
