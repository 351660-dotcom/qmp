import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: '概览', component: () => import('../views/Dashboard.vue') },
      { path: 'products', name: '商品/票种', component: () => import('../views/ProductList.vue') },
      { path: 'pricing', name: '价格日历', component: () => import('../views/Pricing.vue') },
      { path: 'inventory', name: '库存桶', component: () => import('../views/Inventory.vue') },
      { path: 'orders', name: '订单', component: () => import('../views/Orders.vue') },
      { path: 'commissions', name: '商户分账', component: () => import('../views/Commissions.vue') },
      { path: 'verify-keys', name: '核销密钥', component: () => import('../views/VerifyKeys.vue') },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.public) return true
  const auth = useAuthStore()
  if (!auth.isLoggedIn) return '/login'
  return true
})

export default router
