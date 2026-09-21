<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOccupancy, getVenue } from '@/api/venue'
import { createReservation } from '@/api/reservation'
import type { Occupancy, Venue } from '@/types/api'
import OccupancyGrid from '@/components/OccupancyGrid.vue'
import { todayStr } from '@/utils/time'
import { showError, withPurpose } from '@/utils/result'

const route = useRoute()
const router = useRouter()
const venueId = computed(() => Number(route.params.id))

const venue = ref<Venue | null>(null)
const occupancy = ref<Occupancy | null>(null)
const loading = ref(false)
const submitting = ref(false)
const clearPurpose = ref(false)

const form = reactive({
  date: todayStr(),
  startTime: '',
  endTime: '',
  purpose: '',
})

async function loadVenue() {
  venue.value = await getVenue(venueId.value)
}

async function loadOccupancy() {
  if (!form.date) return
  occupancy.value = await getOccupancy(venueId.value, form.date)
}

async function loadAll() {
  loading.value = true
  try {
    await loadVenue()
    await loadOccupancy()
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

watch(
  () => form.date,
  () => {
    void loadOccupancy().catch(showError)
  },
)

function onSelectSlot(payload: { startTime: string; endTime: string }) {
  form.startTime = payload.startTime
  form.endTime = payload.endTime
}

function onClearPurpose() {
  clearPurpose.value = true
  form.purpose = ''
}

function onPurposeInput() {
  clearPurpose.value = false
}

async function submit() {
  if (!form.date || !form.startTime || !form.endTime) {
    ElMessage.warning('请选择日期与时段')
    return
  }
  const base = {
    venueId: venueId.value,
    date: form.date,
    startTime: form.startTime,
    endTime: form.endTime,
  }
  const body = withPurpose(base, form.purpose, clearPurpose.value)
  submitting.value = true
  try {
    await createReservation(body)
    ElMessage.success('预约成功')
    clearPurpose.value = false
    form.purpose = ''
    await loadOccupancy()
    void router.push('/me/reservations')
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <div v-loading="loading">
    <el-page-header @back="router.push('/venues')">
      <template #content>
        <span class="page-title">{{ venue?.name || '场地详情' }}</span>
      </template>
    </el-page-header>

    <el-card v-if="venue" shadow="never" style="margin-top: 16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="类型">{{ venue.type }}</el-descriptions-item>
        <el-descriptions-item label="校区">{{ venue.campus }}</el-descriptions-item>
        <el-descriptions-item label="楼宇">{{ venue.building }}</el-descriptions-item>
        <el-descriptions-item label="容量">{{ venue.capacity }}</el-descriptions-item>
        <el-descriptions-item label="开放">
          {{ venue.openStart }} - {{ venue.openEnd }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ venue.status }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-head">
          <span>当日占用</span>
          <el-date-picker
            v-model="form.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </div>
      </template>
      <el-alert
        title="能否约上以提交为准"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />
      <OccupancyGrid
        v-if="occupancy"
        :open-start="occupancy.openStart"
        :open-end="occupancy.openEnd"
        :occupied="occupancy.occupied"
        @select="onSelectSlot"
      />
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>预约表单</template>
      <el-form label-width="80px" style="max-width: 480px">
        <el-form-item label="日期">
          <el-date-picker
            v-model="form.date"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始">
          <el-time-select
            v-model="form.startTime"
            start="00:00"
            step="00:30"
            end="23:30"
            placeholder="开始"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束">
          <el-time-select
            v-model="form.endTime"
            start="00:30"
            step="00:30"
            end="24:00"
            placeholder="结束"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="用途">
          <el-input
            v-model="form.purpose"
            maxlength="100"
            show-word-limit
            placeholder="可选；留空则不传该字段"
            @input="onPurposeInput"
          />
          <el-button link type="warning" style="margin-top: 4px" @click="onClearPurpose">
            清空用途
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交预约</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
