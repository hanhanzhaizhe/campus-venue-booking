<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listAuditLogs } from '@/api/admin'
import type { AdminAuditLog } from '@/types/api'
import { showError } from '@/utils/result'

const loading = ref(false)
const list = ref<AdminAuditLog[]>([])
const filters = reactive({
  reservationId: '' as string | number,
  operatorId: '' as string | number,
  action: '',
  limit: 50,
})

function prettyJson(raw: string | null): string {
  if (!raw) return '-'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function load() {
  loading.value = true
  try {
    const params: {
      reservationId?: number
      operatorId?: number
      action?: string
      limit?: number
    } = {}
    if (filters.reservationId !== '' && filters.reservationId != null) {
      params.reservationId = Number(filters.reservationId)
    }
    if (filters.operatorId !== '' && filters.operatorId != null) {
      params.operatorId = Number(filters.operatorId)
    }
    if (filters.action.trim()) params.action = filters.action.trim()
    if (filters.limit) params.limit = Number(filters.limit)
    list.value = await listAuditLogs(params)
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div>
    <h2 class="page-title">审计日志</h2>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="预约ID">
          <el-input v-model="filters.reservationId" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="操作人ID">
          <el-input v-model="filters.operatorId" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="filters.action" clearable placeholder="全部" style="width: 200px">
            <el-option label="RESERVATION_CANCEL" value="RESERVATION_CANCEL" />
            <el-option label="RESERVATION_RESCHEDULE" value="RESERVATION_RESCHEDULE" />
          </el-select>
        </el-form-item>
        <el-form-item label="limit">
          <el-input-number v-model="filters.limit" :min="1" :max="200" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="createdAt" label="时间" min-width="160" />
        <el-table-column prop="operatorId" label="操作人" width="90" />
        <el-table-column prop="action" label="动作" min-width="160" />
        <el-table-column prop="reservationId" label="预约" width="80" />
        <el-table-column prop="venueId" label="场地" width="80" />
        <el-table-column label="beforeData" min-width="200">
          <template #default="{ row }">
            <pre class="json">{{ prettyJson(row.beforeData) }}</pre>
          </template>
        </el-table-column>
        <el-table-column label="afterData" min-width="200">
          <template #default="{ row }">
            <pre class="json">{{ prettyJson(row.afterData) }}</pre>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" width="100">
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 20px;
}
.json {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 11px;
  line-height: 1.35;
  max-height: 160px;
  overflow: auto;
}
</style>
