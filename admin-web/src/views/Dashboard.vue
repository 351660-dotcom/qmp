<template>
  <div>
    <el-alert title="景区文旅 SaaS 管理后台 v1" type="success" :closable="false"
              description="当前覆盖：商品/票种、价格、库存、订单、商户分账、核销密钥。对接后端各服务 /admin/v1 接口。" />

    <el-card class="card" header="跨业态日对账查询">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="日期">
          <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="商户 ID">
          <el-input v-model="merchantId" placeholder="可选" style="width:160px" />
        </el-form-item>
        <el-button type="primary" :disabled="!date" @click="query">查询</el-button>
      </el-form>
      <el-descriptions v-if="summary" :column="3" border class="card">
        <el-descriptions-item label="入账合计">{{ summary.in_total }}</el-descriptions-item>
        <el-descriptions-item label="出账合计">{{ summary.out_total }}</el-descriptions-item>
        <el-descriptions-item label="净额">{{ summary.net }}</el-descriptions-item>
        <el-descriptions-item label="笔数">{{ summary.count }}</el-descriptions-item>
        <el-descriptions-item label="按来源" :span="2">
          <el-tag v-for="(v, k) in summary.by_source" :key="k" class="tag">{{ k }}: {{ v }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { reconApi } from '../api/admin'

const date = ref('')
const merchantId = ref('')
const summary = ref(null)
async function query() {
  summary.value = await reconApi.daily(date.value, merchantId.value)
}
</script>

<style scoped>
.card { margin-top: 16px; }
.tag { margin-right: 8px; }
</style>
