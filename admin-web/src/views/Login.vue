<template>
  <div class="login">
    <el-card class="box">
      <h2>景区文旅 SaaS · 管理后台</h2>
      <p class="hint">v1 使用「管理员令牌 + 租户 ID」登录（对接后端 AdminAuthFilter）。后续接入账号体系 + RBAC。</p>
      <el-form :model="form" label-width="90px" @submit.prevent>
        <el-form-item label="租户 ID">
          <el-input v-model="form.tenantId" placeholder="如 1001" />
        </el-form-item>
        <el-form-item label="管理员令牌">
          <el-input v-model="form.adminToken" type="password" show-password placeholder="ADMIN_API_TOKEN" />
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="submit">登录</el-button>
      </el-form>
      <p class="dev">本地默认令牌：<code>scenic-admin-dev-token</code></p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const form = reactive({ tenantId: '1001', adminToken: 'scenic-admin-dev-token' })
const router = useRouter()
const auth = useAuthStore()
function submit() {
  if (!form.tenantId || !form.adminToken) {
    ElMessage.warning('请填写租户 ID 与管理员令牌')
    return
  }
  auth.login(form.adminToken.trim(), form.tenantId.trim())
  router.push('/')
}
</script>

<style scoped>
.login { height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.box { width: 420px; padding: 12px; }
h2 { margin: 0 0 8px; }
.hint { color: #909399; font-size: 13px; margin-bottom: 16px; }
.dev { color: #c0c4cc; font-size: 12px; margin-top: 12px; }
</style>
