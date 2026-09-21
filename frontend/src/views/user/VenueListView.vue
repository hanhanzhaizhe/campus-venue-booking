<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listVenues } from '@/api/venue'
import type { Venue } from '@/types/api'
import { showError } from '@/utils/result'

const router = useRouter()
const loading = ref(false)
const venues = ref<Venue[]>([])
const filters = reactive({
  type: '',
  campus: '',
})

async function load() {
  loading.value = true
  try {
    const params: { type?: string; campus?: string } = {}
    if (filters.type.trim()) params.type = filters.type.trim()
    if (filters.campus.trim()) params.campus = filters.campus.trim()
    venues.value = await listVenues(params)
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

function goDetail(row: Venue) {
  void router.push(`/venues/${row.id}`)
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div>
    <h2 class="page-title">场地列表</h2>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="类型">
          <el-input v-model="filters.type" clearable placeholder="如 教室 / 会议室" style="width: 160px" />
        </el-form-item>
        <el-form-item label="校区">
          <el-input v-model="filters.campus" clearable placeholder="校区" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="venues" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="campus" label="校区" width="100" />
        <el-table-column prop="building" label="楼宇" width="100" />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column label="开放时间" width="140">
          <template #default="{ row }">{{ row.openStart }} - {{ row.openEnd }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
          </template>
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
</style>
