<template>
  <div>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width:200px" @change="load">
        <el-option label="待支付" value="PENDING_PAYMENT" />
        <el-option label="已支付" value="PAID" />
        <el-option label="已关闭" value="CLOSED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-table :data="orders" border v-loading="loading">
      <el-table-column prop="order_id" label="订单号" width="190" />
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }"><el-tag :type="tagType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="total_amount" label="总额" width="110" />
      <el-table-column prop="paid_amount" label="已付" width="110" />
      <el-table-column prop="refund_amount" label="已退" width="110" />
      <el-table-column prop="user_id" label="用户" width="120" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="detail(row.order_id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" title="订单详情" width="560px">
      <el-descriptions v-if="cur" :column="2" border>
        <el-descriptions-item label="订单号">{{ cur.order_id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(cur.status) }}</el-descriptions-item>
        <el-descriptions-item label="总额">{{ cur.total_amount }}</el-descriptions-item>
        <el-descriptions-item label="已付">{{ cur.paid_amount }}</el-descriptions-item>
        <el-descriptions-item label="已退">{{ cur.refund_amount }}</el-descriptions-item>
        <el-descriptions-item label="支付截止">{{ cur.pay_expire_at }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="cur && cur.items" :data="cur.items" size="small" border class="mt">
        <el-table-column prop="order_item_id" label="明细" min-width="160" />
        <el-table-column prop="sku_id" label="票种" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="verified_count" label="已核销" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { orderApi } from '../api/admin'

const orders = ref([])
const status = ref('')
const loading = ref(false)
const dlg = ref(false)
const cur = ref(null)

const statusLabel = (s) => ({ PENDING_PAYMENT: '待支付', PAID: '已支付', CLOSED: '已完成', CANCELLED: '已取消' }[s] || s)
const tagType = (s) => ({ PAID: 'success', PENDING_PAYMENT: 'warning', CANCELLED: 'info', CLOSED: '' }[s] || '')

async function load() {
  loading.value = true
  try { orders.value = await orderApi.list(status.value) || [] } finally { loading.value = false }
}
load()
async function detail(id) { cur.value = await orderApi.detail(id); dlg.value = true }
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.mt { margin-top: 12px; }
</style>
