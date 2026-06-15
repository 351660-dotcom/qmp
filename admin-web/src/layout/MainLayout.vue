<template>
  <el-container class="app">
    <el-aside width="220px" class="aside">
      <div class="logo">景区文旅 SaaS</div>
      <el-menu :default-active="$route.path" router :collapse="false" background-color="#1f2d3d"
               text-color="#c0ccda" active-text-color="#ffd04b">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon><span>概览</span></el-menu-item>
        <el-menu-item index="/products"><el-icon><Goods /></el-icon><span>商品 / 票种</span></el-menu-item>
        <el-menu-item index="/pricing"><el-icon><PriceTag /></el-icon><span>价格日历</span></el-menu-item>
        <el-menu-item index="/inventory"><el-icon><Box /></el-icon><span>库存桶</span></el-menu-item>
        <el-menu-item index="/orders"><el-icon><List /></el-icon><span>订单</span></el-menu-item>
        <el-menu-item index="/commissions"><el-icon><Money /></el-icon><span>商户分账</span></el-menu-item>
        <el-menu-item index="/verify-keys"><el-icon><Key /></el-icon><span>核销密钥</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ $route.name || '管理后台' }}</span>
        <div class="right">
          <el-tag type="info" effect="plain">租户 {{ auth.tenantId }}</el-tag>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
const auth = useAuthStore()
const router = useRouter()
function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app { height: 100vh; }
.aside { background: #1f2d3d; }
.logo { color: #fff; font-weight: 600; font-size: 16px; height: 56px; line-height: 56px; text-align: center; letter-spacing: 1px; }
.header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee; background: #fff; }
.title { font-size: 16px; font-weight: 600; }
.right { display: flex; align-items: center; gap: 12px; }
.el-menu { border-right: none; }
</style>
