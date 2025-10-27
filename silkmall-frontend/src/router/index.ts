import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthState } from '@/services/authState'

const AuthView = () => import('../views/auth/AuthView.vue')
const ConsumerDashboard = () => import('../views/dashboard/ConsumerDashboard.vue')
const SupplierWorkbench = () => import('../views/dashboard/SupplierWorkbench.vue')
const AdminOverview = () => import('../views/dashboard/AdminOverview.vue')
const AdminConsumerManagement = () => import('../views/dashboard/AdminConsumerManagement.vue')
const AdminProductManagement = () => import('../views/dashboard/AdminProductManagement.vue')
const AdminOrderManagement = () => import('../views/dashboard/AdminOrderManagement.vue')
const ProductDetailView = () => import('../views/ProductDetailView.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: ProductDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/auth',
      name: 'auth',
      component: AuthView,
      meta: { guestOnly: true, requiresAuth: false },
    },
    {
      path: '/login',
      redirect: (to) => ({ name: 'auth', query: { ...to.query, mode: 'login' } }),
    },
    {
      path: '/register',
      redirect: (to) => ({ name: 'auth', query: { ...to.query, mode: 'register' } }),
    },
    {
      path: '/consumer/dashboard',
      name: 'consumer-dashboard',
      component: ConsumerDashboard,
      meta: { requiresAuth: true, roles: ['consumer'] },
    },
    {
      path: '/supplier/workbench',
      name: 'supplier-workbench',
      component: SupplierWorkbench,
      meta: { requiresAuth: true, roles: ['supplier'] },
    },
    {
      path: '/admin/overview',
      name: 'admin-overview',
      component: AdminOverview,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
    {
      path: '/admin/products',
      name: 'admin-products',
      component: AdminProductManagement,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
    {
      path: '/admin/orders',
      name: 'admin-orders',
      component: AdminOrderManagement,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
    {
      path: '/admin/consumers',
      name: 'admin-consumers',
      component: AdminConsumerManagement,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
  ],
})

function resolveHome(role?: string | null) {
  switch (role) {
    case 'admin':
      return '/admin/overview'
    case 'supplier':
      return '/supplier/workbench'
    case 'consumer':
      return '/consumer/dashboard'
    default:
      return '/auth'
  }
}

router.beforeEach((to) => {
  const { isAuthenticated, state } = useAuthState()

  const requiresAuth = to.meta?.requiresAuth !== false

  if (requiresAuth && !isAuthenticated.value && to.name !== 'auth') {
    return { name: 'auth', query: { ...to.query, redirect: to.fullPath, mode: 'login' } }
  }

  if (to.meta?.roles && isAuthenticated.value) {
    const allowed = Array.isArray(to.meta.roles) ? to.meta.roles : [to.meta.roles]
    const currentRole = state.user?.userType ?? null
    if (!allowed.includes(currentRole as never)) {
      return { path: resolveHome(currentRole) }
    }
  }

  if (to.meta?.guestOnly && isAuthenticated.value) {
    return { path: resolveHome(state.user?.userType ?? null) }
  }

  return true
})

export default router
