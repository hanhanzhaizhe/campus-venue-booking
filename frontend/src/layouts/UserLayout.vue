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
      <div class="brand">校园场地预约</div>
      <el-menu mode="horizontal" :ellipsis="false" router :default-active="$route.path" class="nav">
        <el-menu-item index="/venues">场地</el-menu-item>
        <el-menu-item index="/me/reservations">我的预约</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/admin/reservations">管理端</el-menu-item>
      </el-menu>
      <div class="user">
        <span class="name">{{ auth.username }}（{{ auth.role }}）</span>
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
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
}
</style>
