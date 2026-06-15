<template>
  <div>
    <el-card header="核销码密钥轮换（按景区独立密钥）">
      <el-form :inline="true" :model="form" @submit.prevent>
        <el-form-item label="景区ID"><el-input v-model="form.scenic_id" style="width:180px" /></el-form-item>
        <el-button type="primary" :loading="loading" @click="rotate">轮换密钥</el-button>
      </el-form>
      <el-alert class="mt" type="warning" :closable="false"
                title="轮换后：生成新 ACTIVE 密钥用于新出票；旧密钥置 RETIRED 仍保留，已发出的旧核销码仍可离线验签。" />
      <el-result v-if="result" icon="success" :title="`已轮换：景区 ${form.scenic_id}`"
                 :sub-title="`新密钥 kid=${result.kid}，版本 v${result.key_version}`" class="mt" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ticketApi } from '../api/admin'

const form = reactive({ scenic_id: '' })
const loading = ref(false)
const result = ref(null)
async function rotate() {
  if (!form.scenic_id) { ElMessage.warning('请填写景区ID'); return }
  loading.value = true
  try {
    result.value = await ticketApi.rotateKey(Number(form.scenic_id))
    ElMessage.success('密钥已轮换')
  } finally { loading.value = false }
}
</script>

<style scoped>.mt { margin-top: 16px; }</style>
