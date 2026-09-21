<script setup lang="ts">
import { ElMessageBox } from 'element-plus'

const props = defineProps<{
  tip?: string
}>()

async function confirm(): Promise<boolean> {
  try {
    await ElMessageBox.confirm(props.tip || '确认取消该预约？', '取消预约', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
    })
    return true
  } catch {
    return false
  }
}

defineExpose({ confirm })
</script>

<template>
  <span style="display: none" />
</template>
