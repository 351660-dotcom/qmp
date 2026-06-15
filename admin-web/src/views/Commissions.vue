<template>
  <div>
    <el-card header="商户分账抽成（每个商户可设不同平台抽成比例）">
      <el-form :inline="true" :model="form" @submit.prevent>
        <el-form-item label="商户ID"><el-input v-model="form.merchant_id" style="width:180px" /></el-form-item>
        <el-form-item label="抽成比例">
          <el-input-number v-model="form.rate" :min="0" :max="1" :step="0.01" :precision="4" />
          <span class="muted">如 0.06 = 平台抽 6%</span>
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form>
      <el-alert class="mt" type="info" :closable="false"
                title="说明：保存后该商户的支付分账将按比例计算 platform_amount / merchant_amount；未配置默认 0（全额归商户）。" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { paymentApi } from '../api/admin'

const form = reactive({ merchant_id: '', rate: 0.06 })
const saving = ref(false)
async function save() {
  if (!form.merchant_id) { ElMessage.warning('请填写商户ID'); return }
  saving.value = true
  try {
    await paymentApi.upsertCommission(Number(form.merchant_id), form.rate)
    ElMessage.success('已保存')
  } finally { saving.value = false }
}
</script>

<style scoped>.muted { color:#909399; font-size:12px; margin-left:8px; } .mt { margin-top:16px; }</style>
