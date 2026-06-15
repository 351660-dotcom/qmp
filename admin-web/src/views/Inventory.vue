<template>
  <div>
    <el-card header="维护库存桶（按 票种 + 日期 + 场次 幂等：存在则调整配额）">
      <el-form :inline="true" :model="form" @submit.prevent>
        <el-form-item label="票种ID"><el-input v-model="form.sku_id" style="width:150px" /></el-form-item>
        <el-form-item label="景区ID"><el-input v-model="form.scenic_id" style="width:130px" /></el-form-item>
        <el-form-item label="游玩日"><el-date-picker v-model="form.sale_date" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="场次ID"><el-input-number v-model="form.time_slot_id" :min="0" /></el-form-item>
        <el-form-item label="总配额"><el-input-number v-model="form.total_quota" :min="0" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button @click="query" :disabled="!form.sku_id || !form.sale_date">查询</el-button>
      </el-form>
    </el-card>

    <el-descriptions v-if="bucket" :column="4" border class="mt" title="当前库存桶">
      <el-descriptions-item label="桶ID">{{ bucket.bucket_id }}</el-descriptions-item>
      <el-descriptions-item label="总配额">{{ bucket.total_quota }}</el-descriptions-item>
      <el-descriptions-item label="已售">{{ bucket.sold_count }}</el-descriptions-item>
      <el-descriptions-item label="锁定">{{ bucket.locked_count }}</el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { inventoryApi } from '../api/admin'

const form = reactive({ sku_id: '', scenic_id: '', sale_date: '', time_slot_id: 0, total_quota: 100 })
const bucket = ref(null)
const saving = ref(false)

async function save() {
  if (!form.sku_id || !form.scenic_id || !form.sale_date) { ElMessage.warning('请填写票种/景区/游玩日'); return }
  saving.value = true
  try {
    await inventoryApi.upsertBucket({
      sku_id: Number(form.sku_id), scenic_id: Number(form.scenic_id),
      sale_date: form.sale_date, time_slot_id: form.time_slot_id, total_quota: form.total_quota,
    })
    ElMessage.success('已保存')
    query()
  } finally { saving.value = false }
}
async function query() {
  bucket.value = await inventoryApi.getBucket(Number(form.sku_id), form.sale_date, form.time_slot_id)
}
</script>

<style scoped>.mt { margin-top: 16px; }</style>
