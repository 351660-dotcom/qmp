<template>
  <div class="panel">
    <div class="head">
      <span>票种列表</span>
      <el-button size="small" type="primary" :icon="Plus" @click="dialog = true">新建票种</el-button>
    </div>
    <el-table :data="skus" size="small" border v-loading="loading">
      <el-table-column prop="sku_id" label="票种ID" width="180" />
      <el-table-column prop="ticket_type" label="类型" width="140" />
      <el-table-column label="需要场次" width="120">
        <template #default="{ row }">{{ row.requires_time_slot ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="场次定义" min-width="200">
        <template #default="{ row }">{{ (row.time_slot_definitions || []).join('、') || '—' }}</template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="新建票种" width="520px" append-to-body>
      <el-form :model="form" label-width="110px">
        <el-form-item label="票种类型">
          <el-select v-model="form.ticket_type" style="width:220px">
            <el-option label="成人票" value="ADULT" />
            <el-option label="儿童票" value="CHILD" />
            <el-option label="老人票" value="SENIOR" />
            <el-option label="学生票" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="需要场次"><el-switch v-model="form.requires_time_slot" /></el-form-item>
        <el-form-item v-if="form.requires_time_slot" label="场次(逗号分隔)">
          <el-input v-model="form.slots" placeholder="09:00-11:00,11:00-13:00" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { productApi } from '../api/admin'

const props = defineProps({ product: { type: Object, required: true } })
const skus = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const form = reactive({ ticket_type: 'ADULT', requires_time_slot: false, slots: '' })

async function load() {
  loading.value = true
  try { skus.value = await productApi.listSkus(props.product.product_id) } finally { loading.value = false }
}
load()

async function save() {
  const body = {
    product_id: props.product.product_id,
    ticket_type: form.ticket_type,
    requires_time_slot: form.requires_time_slot,
    time_slot_definitions: form.requires_time_slot && form.slots
      ? form.slots.split(',').map((s) => s.trim()).filter(Boolean) : [],
  }
  saving.value = true
  try {
    await productApi.createSku(body)
    ElMessage.success('票种已创建')
    dialog.value = false
    load()
  } finally { saving.value = false }
}
</script>

<style scoped>
.panel { padding: 8px 16px; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
</style>
