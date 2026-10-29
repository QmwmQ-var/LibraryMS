import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '首页', roles: ['ADMIN', 'READER'], eyebrow: 'Overview' },
      },
      {
        path: 'books',
        name: 'books',
        component: () => import('@/views/book/BookListView.vue'),
        meta: { title: '馆藏检索', roles: ['ADMIN', 'READER'], eyebrow: 'Collection' },
      },
      {
        path: 'books-manage',
        name: 'books-manage',
        component: () => import('@/views/book/BookListView.vue'),
        meta: {
          title: '图书管理',
          roles: ['ADMIN'],
          eyebrow: 'Catalog',
          management: true,
        },
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/category/CategoryView.vue'),
        meta: { title: '分类管理', roles: ['ADMIN'], eyebrow: 'Taxonomy' },
      },
      {
        path: 'readers',
        name: 'readers',
        component: () => import('@/views/reader/ReaderListView.vue'),
        meta: { title: '读者管理', roles: ['ADMIN'], eyebrow: 'Readers' },
      },
      {
        path: 'circulation',
        name: 'circulation',
        component: () => import('@/views/borrow/CirculationView.vue'),
        meta: { title: '流通办理', roles: ['ADMIN'], eyebrow: 'Circulation' },
      },
      {
        path: 'my-borrows',
        name: 'my-borrows',
        component: () => import('@/views/borrow/MyBorrowsView.vue'),
        meta: { title: '我的借阅', roles: ['READER'], eyebrow: 'My Reading' },
      },
      {
        path: 'my-reservations',
        name: 'my-reservations',
        component: () => import('@/views/reservation/MyReservationsView.vue'),
        meta: { title: '我的预约', roles: ['READER'], eyebrow: 'Reservations' },
      },
      {
        path: 'reservations-manage',
        name: 'reservations-manage',
        component: () => import('@/views/reservation/ReservationManageView.vue'),
        meta: { title: '预约管理', roles: ['ADMIN'], eyebrow: 'Reservations' },
      },
      {
        path: 'penalties',
        name: 'penalties',
        component: () => import('@/views/penalty/PenaltyView.vue'),
        meta: { title: '我的罚款', roles: ['READER'], eyebrow: 'Fines' },
      },
      {
        path: 'penalties-manage',
        name: 'penalties-manage',
        component: () => import('@/views/penalty/PenaltyManageView.vue'),
        meta: { title: '罚款管理', roles: ['ADMIN'], eyebrow: 'Fines' },
      },
      {
        path: 'stats',
        name: 'stats',
        component: () => import('@/views/stats/StatsView.vue'),
        meta: { title: '统计报表', roles: ['ADMIN'], eyebrow: 'Insights' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', roles: ['ADMIN', 'READER'], eyebrow: 'Account' },
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/error/ForbiddenView.vue'),
        meta: { title: '无权访问' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to, from, savedPosition) =>
    savedPosition || { top: 0, behavior: 'smooth' },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  document.title = to.meta.title
    ? `${to.meta.title} · 知墨图书馆`
    : '知墨图书馆 · 高校图书管理系统'

  if (to.meta.public) {
    if (to.name === 'login' && auth.isAuthenticated) {
      return { name: 'dashboard' }
    }
    return true
  }

  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!auth.user) {
    try {
      await auth.fetchProfile()
    } catch {
      await auth.logout()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return { name: 'forbidden' }
  }

  return true
})

export default router
