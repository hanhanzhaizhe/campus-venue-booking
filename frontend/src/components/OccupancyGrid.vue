<script setup lang="ts">
import { computed, ref } from 'vue'
import type { OccupiedSlot } from '@/types/api'
import { halfHourSlots, parseHm, formatHm } from '@/utils/time'

const props = defineProps<{
  openStart: string
  openEnd: string
  occupied: OccupiedSlot[]
}>()

const emit = defineEmits<{
  select: [{ startTime: string; endTime: string }]
}>()

const pendingStart = ref<string | null>(null)

const slots = computed(() => halfHourSlots(props.openStart || '08:00', props.openEnd || '22:00'))

function isOccupied(slotStart: string): boolean {
  const s = parseHm(slotStart)
  const slotEnd = s + 30
  return props.occupied.some((o) => {
    const os = parseHm(o.startTime)
    const oe = parseHm(o.endTime)
    return s < oe && slotEnd > os
  })
}

function rangeFree(startHm: string, endHm: string): boolean {
  const a = parseHm(startHm)
  const b = parseHm(endHm)
  for (let t = a; t < b; t += 30) {
    if (isOccupied(formatHm(t))) return false
  }
  return true
}

function onClick(slot: string) {
  if (isOccupied(slot)) {
    pendingStart.value = null
    return
  }
  if (!pendingStart.value) {
    pendingStart.value = slot
    // provisional single half-hour
    emit('select', { startTime: slot, endTime: formatHm(parseHm(slot) + 30) })
    return
  }
  const start = pendingStart.value
  const endCandidate = formatHm(parseHm(slot) + 30)
  if (parseHm(slot) < parseHm(start)) {
    // restart from earlier slot
    pendingStart.value = slot
    emit('select', { startTime: slot, endTime: formatHm(parseHm(slot) + 30) })
    return
  }
  if (rangeFree(start, endCandidate)) {
    emit('select', { startTime: start, endTime: endCandidate })
    pendingStart.value = null
  } else {
    pendingStart.value = slot
    emit('select', { startTime: slot, endTime: formatHm(parseHm(slot) + 30) })
  }
}

function cellClass(slot: string) {
  if (isOccupied(slot)) return 'cell occupied'
  if (pendingStart.value === slot) return 'cell free selected'
  return 'cell free'
}
</script>

<template>
  <div class="grid-wrap">
    <div class="hint">点击空闲格选择时段（再点更晚空闲格可扩展）；能否约上以提交为准</div>
    <div class="grid">
      <button
        v-for="slot in slots"
        :key="slot"
        type="button"
        :class="cellClass(slot)"
        :disabled="isOccupied(slot)"
        @click="onClick(slot)"
      >
        <span class="t">{{ slot }}</span>
        <span class="s">{{ isOccupied(slot) ? '占用' : '空闲' }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.grid-wrap {
  margin: 12px 0;
}
.hint {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.cell {
  width: 72px;
  height: 52px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #fff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  padding: 0;
}
.cell .t {
  font-weight: 600;
}
.cell .s {
  color: #909399;
  font-size: 11px;
}
.cell.free:hover {
  border-color: #409eff;
  color: #409eff;
}
.cell.selected {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}
.cell.occupied {
  background: #f4f4f5;
  color: #c0c4cc;
  cursor: not-allowed;
}
</style>
