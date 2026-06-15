<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建商品</el-button>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-table :data="products" v-loading="loading" border>
      <el-table-column type="expand">
        <template #default="{ row }">
          <SkuPanel :product="row" />
        </template>
      </el-table-column>
      <el-table-column prop="product_id" label="商品ID" width="180" />
      <el-table-column prop="name" label="标题" min-width="160" />
      <el-table-column prop="scenic_id" label="景区" width="120" />
      <el-table-column prop="merchant_id" label="商户" width="120" />
      <el-table-column label="核销介质" min-width="160">
        <template #default="{ row }">
          <el-tag v-for="m in mediumList(row)" :key="m" size="small" class="tag">{{ mediumLabel(m) }}</el-tag>
          <span v-if="!mediumList(row).length" class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="退改签" min-width="180">
        <template #default="{ row }">{{ refundText(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'ON_SALE'" link type="success" @click="setStatus(row, 'ON_SALE')">上架</el-button>
          <el-button v-else link type="warning" @click="setStatus(row, 'OFF_SALE')">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建商品 -->
    <el-dialog v-model="dialog" title="新建商品" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="标题" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="景区 ID" required><el-input v-model="form.scenic_id" /></el-form-item>
        <el-form-item label="商户 ID" required><el-input v-model="form.merchant_id" /></el-form-item>
        <el-divider content-position="left">核销规则</el-divider>
        <el-form-item label="有效期(天)">
          <el-input-number v-model="form.valid_days" :min="1" />
        </el-form-item>
        <el-form-item label="核销介质">
          <el-checkbox-group v-model="form.verification_medium">
            <el-checkbox value="QR_CODE">二维码</el-checkbox>
            <el-checkbox value="IC_CARD">IC 卡</el-checkbox>
            <el-checkbox value="FACE">人脸</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="实名规则">
          <el-select v-model="form.real_name_rule" style="width:240px">
            <el-option label="不实名" value="NONE" />
            <el-option label="一票一证" value="ONE_TICKET_ONE_ID" />
            <el-option label="一单多人" value="ONE_ORDER_MULTI_PERSON" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">退改签规则</el-divider>
        <el-form-item label="是否支持退改">
          <el-switch v-model="form.refund_supported" />
        </el-form-item>
        <template v-if="form.refund_supported">
          <el-form-item label="可退时间(小时)">
            <el-input-number v-model="form.cutoff_hours" :min="0" />
            <span class="muted">游玩日 0 点前 N 小时可退</span>
          </el-form-item>
          <el-form-item label="手续费比例">
            <el-input-number v-model="form.fee_ratio" :min="0" :max="1" :step="0.05" :precision="2" />
            <span class="muted">退款 = 单价 ×(1−手续费)</span>
          </el-form-item>
        </template>
        <el-form-item label="创建后上架"><el-switch v-model="form.onSale" /></el-form-item>
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { productApi } from '../api/admin'
import SkuPanel from './SkuPanel.vue'

const products = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)

const MEDIUM = { QR_CODE: '二维码', IC_CARD: 'IC卡', FACE: '人脸' }
const mediumLabel = (m) => MEDIUM[m] || m
const statusLabel = (s) => ({ DRAFT: '草稿', PENDING_REVIEW: '待审', ON_SALE: '在售', OFF_SALE: '下架' }[s] || s)

function mediumList(row) {
  try { return row.verification_medium ? JSON.parse(row.verification_medium) : [] } catch { return [] }
}
function refundText(row) {
  if (!row.refund_policy) return '默认'
  try {
    const p = JSON.parse(row.refund_policy)
    if (p.supported === false || p.type === 'NONE') return '不支持退改'
    const fee = p.fee_ratio != null ? `手续费 ${(p.fee_ratio * 100).toFixed(0)}%` : (p.refund_ratio != null ? `退 ${(p.refund_ratio * 100).toFixed(0)}%` : '全额退')
    const cut = p.cutoff_hours != null ? `，提前 ${p.cutoff_hours}h` : ''
    return `支持退改（${fee}${cut}）`
  } catch { return row.refund_policy }
}

async function load() {
  loading.value = true
  try { products.value = await productApi.listProducts() } finally { loading.value = false }
}
load()

const blankForm = () => ({
  name: '', scenic_id: '', merchant_id: '', valid_days: 1,
  verification_medium: ['QR_CODE'], real_name_rule: 'NONE',
  refund_supported: true, cutoff_hours: 24, fee_ratio: 0.2, onSale: true,
})
const form = reactive(blankForm())
function openCreate() { Object.assign(form, blankForm()); dialog.value = true }

async function save() {
  if (!form.name || !form.scenic_id || !form.merchant_id) { ElMessage.warning('请填写标题/景区/商户'); return }
  const body = {
    name: form.name,
    scenic_id: Number(form.scenic_id),
    merchant_id: Number(form.merchant_id),
    valid_period_rule: { type: 'FIXED_DATE', valid_days: form.valid_days },
    real_name_rule: form.real_name_rule,
    verification_medium: form.verification_medium,
    refund_policy: form.refund_supported
      ? { supported: true, cutoff_hours: form.cutoff_hours, fee_ratio: form.fee_ratio }
      : { supported: false },
    status: form.onSale ? 'ON_SALE' : 'DRAFT',
  }
  saving.value = true
  try {
    await productApi.createProduct(body)
    ElMessage.success('创建成功')
    dialog.value = false
    load()
  } finally { saving.value = false }
}

async function setStatus(row, status) {
  await productApi.updateStatus(row.product_id, status)
  ElMessage.success(status === 'ON_SALE' ? '已上架' : '已下架')
  load()
}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.tag { margin-right: 6px; }
.muted { color: #909399; font-size: 12px; margin-left: 8px; }
</style>
