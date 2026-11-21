import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isAdmin, isLoggedIn } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/products' },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录 / 注册' }
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('../views/ProductListView.vue'),
    meta: { title: '商品列表' }
  },
  {
    path: '/products/:itemId',
    name: 'product-detail',
    component: () => import('../views/ProductDetailView.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/MyOrdersView.vue'),
    // 需要登录：未登录会跳到登录页，登录后回跳
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/admin/games',
    name: 'admin-games',
    component: () => import('../views/AdminGamesView.vue'),
    // 只有 ADMIN 能进；前端判断只是体验优化，后端 Security 才是真正的权限控制
    meta: { title: '商品管理', requiresAuth: true, requiresAdmin: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/products' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !isAdmin()) {
    ElMessage.warning('该页面仅管理员可访问')
    return { path: '/products' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 游戏交易平台` : '游戏交易平台'
})

export default router
