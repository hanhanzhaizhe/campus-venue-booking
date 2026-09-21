<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  void router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header" height="56px">
      <div class="brand">管理端 · 场地预约</div>
      <el-menu mode="horizontal" :ellipsis="false" router :default-active="$route.path" class="nav">
        <el-menu-item index="/admin/reservations">预约管理</el-menu-item>
        <el-menu-item index="/admin/audit-logs">审计</el-menu-item>
        <el-menu-item index="/venues">回用户端场地</el-menu-item>
      </el-menu>
      <div class="user">
        <span class="name">{{ auth.username }}（ADMIN）</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
}
.brand {
  font-weight: 600;
  font-size: 16px;
  white-space: nowrap;
}
.nav {
  flex: 1;
  border-bottom: none;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.name {
  color: #606266;
  font-size: 13px;
}
.main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
</style>
