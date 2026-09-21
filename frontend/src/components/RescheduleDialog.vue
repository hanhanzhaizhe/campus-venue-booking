<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { Reservation, RescheduleBody } from '@/types/api'
import { rescheduleReservation } from '@/api/reservation'
import { adminReschedule } from '@/api/admin'
import { splitReservationTime } from '@/utils/time'
import { showError, withPurpose } from '@/utils/result'

const props = defineProps<{
  modelValue: boolean
  mode: 'user' | 'admin'
  reservation: Reservation | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

const form = reactive({
  date: '',
  startTime: '',
  endTime: '',
  purpose: '',
})
const clearPurpose = ref(false)
const submitting = ref(false)

const tip = computed(() =>
  props.mode === 'admin' ? '管理端：开始前均可改约' : '本人改约：开始前 2 小时可改（恰好 2h 允许）',
)

watch(
  () => props.reservation,
  (r) => {
    if (!r) return
    const s = splitReservationTime(r.startTime)
    const e = splitReservationTime(r.endTime)
    form.date = s.date
    form.startTime = s.time
    form.endTime = e.time
    form.purpose = r.purpose ?? ''
    clearPurpose.value = false
  },
  { immediate: true },
)

function onClearPurpose() {
  clearPurpose.value = true
  form.purpose = ''
}

function onPurposeInput() {
  clearPurpose.value = false
}

async function submit() {
  if (!props.reservation) return
  if (!form.date || !form.startTime || !form.endTime) {
    ElMessage.warning('请填写日期与起止时间')
    return
  }
  const base: RescheduleBody = {
    date: form.date,
    startTime: form.startTime,
    endTime: form.endTime,
  }
  const body = withPurpose(base, form.purpose, clearPurpose.value)
  submitting.value = true
  try {
    if (props.mode === 'admin') {
      await adminReschedule(props.reservation.id, body)
    } else {
      await rescheduleReservation(props.reservation.id, body)
    }
    ElMessage.success('改约成功')
    visible.value = false
    emit('success')
  } catch (e) {
    showError(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="改约" width="480px" destroy-on-close>
    <el-alert :title="tip" type="info" show-icon :closable="false" style="margin-bottom: 12px" />
    <el-form label-width="80px">
      <el-form-item label="日期">
        <el-date-picker
          v-model="form.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="开始">
        <el-time-select
          v-model="form.startTime"
          start="00:00"
          step="00:30"
          end="23:30"
          placeholder="开始时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="结束">
        <el-time-select
          v-model="form.endTime"
          start="00:30"
          step="00:30"
          end="24:00"
          placeholder="结束时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="用途">
        <el-input
          v-model="form.purpose"
          maxlength="100"
          show-word-limit
          placeholder="留空则保留原用途"
          @input="onPurposeInput"
        />
        <el-button link type="warning" style="margin-top: 4px" @click="onClearPurpose">
          清空用途
        </el-button>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交改约</el-button>
    </template>
  </el-dialog>
</template>
