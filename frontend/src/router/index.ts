import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    name: 'home',
    redirect: () => {
      const auth = useAuthStore()
      return auth.isAdmin ? '/admin/reservations' : '/venues'
    },
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      {
        path: 'venues',
        name: 'venues',
        component: () => import('@/views/user/VenueListView.vue'),
      },
      {
        path: 'venues/:id',
        name: 'venue-detail',
        component: () => import('@/views/user/VenueDetailView.vue'),
      },
      {
        path: 'me/reservations',
        name: 'my-reservations',
        component: () => import('@/views/user/MyReservationsView.vue'),
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      {
        path: 'reservations',
        name: 'admin-reservations',
        component: () => import('@/views/admin/AdminReservationListView.vue'),
        meta: { admin: true },
      },
      {
        path: 'audit-logs',
        name: 'admin-audit',
        component: () => import('@/views/admin/AdminAuditListView.vue'),
        meta: { admin: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.token) {
    auth.hydrateFromStorage()
  }

  if (to.meta.public) {
    if (auth.isLoggedIn && to.path === '/login') {
      return auth.isAdmin ? '/admin/reservations' : '/venues'
    }
    return true
  }

  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path.startsWith('/admin') && !auth.isAdmin) {
    ElMessage.error('无管理端权限')
    return '/venues'
  }

  return true
})

export default router
