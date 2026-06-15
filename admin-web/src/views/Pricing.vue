<template>
  <div>
    <el-card header="维护价格（按 票种 + 日期 + 类型 幂等 upsert）">
      <el-form :inline="true" :model="form" @submit.prevent>
        <el-form-item label="票种ID"><el-input v-model="form.sku_id" style="width:160px" /></el-form-item>
        <el-form-item label="游玩日"><el-date-picker v-model="form.sale_date" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.price_type" style="width:140px">
            <el-option label="门市价" value="RETAIL" />
            <el-option label="会员价" value="MEMBER" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" :step="1" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="query" :disabled="!form.sku_id">查询该票种</el-button>
      </el-form>
    </el-card>

    <el-table :data="list" border class="mt" v-if="list.length">
      <el-table-column prop="sale_date" label="游玩日" />
      <el-table-column prop="price_type" label="类型" />
      <el-table-column prop="price" label="价格" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { pricingApi } from '../api/admin'

const form = reactive({ sku_id: '', sale_date: '', price_type: 'RETAIL', price: 0 })
const list = ref([])
const saving = ref(false)

async function save() {
  if (!form.sku_id || !form.sale_date) { ElMessage.warning('请填写票种与游玩日'); return }
  saving.value = true
  try {
    await pricingApi.upsert({ sku_id: Number(form.sku_id), sale_date: form.sale_date, price_type: form.price_type, price: form.price })
    ElMessage.success('已保存')
    query()
  } finally { saving.value = false }
}
async function query() {
  list.value = await pricingApi.list(Number(form.sku_id)) || []
}
</script>

<style scoped>.mt { margin-top: 16px; }</style>
