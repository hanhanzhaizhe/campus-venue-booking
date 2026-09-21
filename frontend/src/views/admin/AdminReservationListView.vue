<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminCancel, listAdminReservations } from '@/api/admin'
import type { Reservation } from '@/types/api'
import RescheduleDialog from '@/components/RescheduleDialog.vue'
import CancelConfirm from '@/components/CancelConfirm.vue'
import { canAdminReschedule } from '@/utils/time'
import { showError } from '@/utils/result'

const loading = ref(false)
const list = ref<Reservation[]>([])
const filters = reactive({
  venueId: '' as string | number,
  userId: '' as string | number,
  date: '',
})
const dialogVisible = ref(false)
const current = ref<Reservation | null>(null)
const cancelRef = ref<InstanceType<typeof CancelConfirm> | null>(null)

async function load() {
  loading.value = true
  try {
    const params: { venueId?: number; userId?: number; date?: string } = {}
    if (filters.venueId !== '' && filters.venueId != null) {
      params.venueId = Number(filters.venueId)
    }
    if (filters.userId !== '' && filters.userId != null) {
      params.userId = Number(filters.userId)
    }
    if (filters.date) params.date = filters.date
    list.value = await listAdminReservations(params)
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

function openReschedule(row: Reservation) {
  current.value = row
  dialogVisible.value = true
}

async function onCancel(row: Reservation) {
  const ok = await cancelRef.value?.confirm()
  if (!ok) return
  try {
    await adminCancel(row.id)
    ElMessage.success('已取消')
    await load()
  } catch (e) {
    showError(e)
  }
}

function windowOk(row: Reservation) {
  return canAdminReschedule(row.startTime)
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div>
    <h2 class="page-title">预约管理</h2>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="场地ID">
          <el-input v-model="filters.venueId" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="filters.date"
            type="date"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="venueId" label="场地" width="80" />
        <el-table-column prop="userId" label="用户" width="80" />
        <el-table-column prop="startTime" label="开始" min-width="160" />
        <el-table-column prop="endTime" label="结束" min-width="160" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="purpose" label="用途" min-width="120">
          <template #default="{ row }">{{ row.purpose || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :content="windowOk(row) ? '开始前均可改约' : '已开始或不可改'"
              placement="top"
            >
              <span>
                <el-button
                  link
                  type="primary"
                  :disabled="!windowOk(row) || row.status !== 'CONFIRMED'"
                  @click="openReschedule(row)"
                >
                  改约
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
              :content="windowOk(row) ? '开始前可取消' : '已开始或不可取消'"
              placement="top"
            >
              <span>
                <el-button
                  link
                  type="danger"
                  :disabled="!windowOk(row) || row.status !== 'CONFIRMED'"
                  @click="onCancel(row)"
                >
                  取消
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <RescheduleDialog
      v-model="dialogVisible"
      mode="admin"
      :reservation="current"
      @success="load"
    />
    <CancelConfirm ref="cancelRef" tip="确认管理端取消该预约？开始前均可取消。" />
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 20px;
}
</style>
