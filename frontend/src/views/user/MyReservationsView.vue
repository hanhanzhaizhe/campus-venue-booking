<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelReservation, listMyReservations } from '@/api/reservation'
import type { Reservation, ReservationFilter } from '@/types/api'
import RescheduleDialog from '@/components/RescheduleDialog.vue'
import CancelConfirm from '@/components/CancelConfirm.vue'
import { canUserReschedule } from '@/utils/time'
import { showError } from '@/utils/result'

const filter = ref<ReservationFilter>('UPCOMING')
const loading = ref(false)
const list = ref<Reservation[]>([])
const dialogVisible = ref(false)
const current = ref<Reservation | null>(null)
const cancelRef = ref<InstanceType<typeof CancelConfirm> | null>(null)

const tabs: { label: string; name: ReservationFilter }[] = [
  { label: '即将开始', name: 'UPCOMING' },
  { label: '进行中', name: 'ONGOING' },
  { label: '已结束', name: 'ENDED' },
  { label: '已取消', name: 'CANCELLED' },
]

async function load() {
  loading.value = true
  try {
    list.value = await listMyReservations(filter.value)
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
    await cancelReservation(row.id)
    ElMessage.success('已取消')
    await load()
  } catch (e) {
    showError(e)
  }
}

function windowOk(row: Reservation) {
  return canUserReschedule(row.startTime)
}

watch(filter, () => {
  void load()
})

onMounted(() => {
  void load()
})
</script>

<template>
  <div>
    <h2 class="page-title">我的预约</h2>
    <el-card shadow="never">
      <el-tabs v-model="filter">
        <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" />
      </el-tabs>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="venueId" label="场地ID" width="90" />
        <el-table-column prop="startTime" label="开始" min-width="160" />
        <el-table-column prop="endTime" label="结束" min-width="160" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="purpose" label="用途" min-width="120">
          <template #default="{ row }">{{ row.purpose || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="filter === 'UPCOMING'" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              :content="windowOk(row) ? '开始前 2 小时可改' : '已不足 2 小时，不可改约'"
              placement="top"
            >
              <span>
                <el-button
                  link
                  type="primary"
                  :disabled="!windowOk(row)"
                  @click="openReschedule(row)"
                >
                  改约
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
              :content="windowOk(row) ? '开始前 2 小时可取消' : '已不足 2 小时，不可取消'"
              placement="top"
            >
              <span>
                <el-button link type="danger" :disabled="!windowOk(row)" @click="onCancel(row)">
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
      mode="user"
      :reservation="current"
      @success="load"
    />
    <CancelConfirm ref="cancelRef" tip="确认取消该预约？开始前 2 小时可取消。" />
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 20px;
}
</style>
