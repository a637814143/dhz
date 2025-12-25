import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthState } from '@/services/authState'

const AuthView = () => import('../views/auth/AuthView.vue')
const ConsumerDashboard = () => import('../views/dashboard/ConsumerDashboard.vue')
const SupplierWorkbench = () => import('../views/dashboard/SupplierWorkbench.vue')
const AdminOverview = () => import('../views/dashboard/AdminOverview.vue')
const AdminConsumerManagement = () => import('../views/dashboard/AdminConsumerManagement.vue')
const AdminSupplierManagement = () => import('../views/dashboard/AdminSupplierManagement.vue')
const AdminProductManagement = () => import('../views/dashboard/AdminProductManagement.vue')
const AdminOrderManagement = () => import('../views/dashboard/AdminOrderManagement.vue')
const AdminSalesStatistics = () => import('../views/dashboard/AdminSalesStatistics.vue')
const ProductDetailView = () => import('../views/ProductDetailView.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true, allowGuest: true },
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: true, allowGuest: true },
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: ProductDetailView,
      meta: { requiresAuth: true, allowGuest: true },
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
      meta: { requiresAuth: true, roles: ['consumer'], section: 'overview' },
    },
    {
      path: '/consumer/addresses',
      name: 'consumer-addresses',
      component: ConsumerDashboard,
      meta: { requiresAuth: true, roles: ['consumer'], section: 'address' },
    },
    {
      path: '/consumer/cart',
      name: 'consumer-cart',
      component: ConsumerDashboard,
      meta: { requiresAuth: true, roles: ['consumer'], section: 'cart' },
    },
    {
      path: '/consumer/favorites',
      name: 'consumer-favorites',
      component: ConsumerDashboard,
      meta: { requiresAuth: true, roles: ['consumer'], section: 'favorites' },
    },
    {
      path: '/consumer/orders',
      name: 'consumer-orders',
      component: ConsumerDashboard,
      meta: { requiresAuth: true, roles: ['consumer'], section: 'orders' },
    },
    {
      path: '/consumer/reviews',
      name: 'consumer-reviews',
      component: ConsumerDashboard,
      meta: { requiresAuth: true, roles: ['consumer'], section: 'reviews' },
    },
    {
      path: '/supplier/workbench',
      name: 'supplier-workbench',
      component: SupplierWorkbench,
      meta: { requiresAuth: true, roles: ['supplier'], section: 'overview' },
    },
    {
      path: '/supplier/products',
      name: 'supplier-products',
      component: SupplierWorkbench,
      meta: { requiresAuth: true, roles: ['supplier'], section: 'products' },
    },
    {
      path: '/supplier/orders',
      name: 'supplier-orders',
      component: SupplierWorkbench,
      meta: { requiresAuth: true, roles: ['supplier'], section: 'orders' },
    },
    {
      path: '/supplier/returns',
      name: 'supplier-returns',
      component: SupplierWorkbench,
      meta: { requiresAuth: true, roles: ['supplier'], section: 'returns' },
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
    {
      path: '/admin/suppliers',
      name: 'admin-suppliers',
      component: AdminSupplierManagement,
      meta: { requiresAuth: true, roles: ['admin'] },
    },
    {
      path: '/admin/sales',
      name: 'admin-sales',
      component: AdminSalesStatistics,
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
  const { isAuthenticated, state, isGuestSession } = useAuthState()

  const requiresAuth = to.meta?.requiresAuth !== false

  if (requiresAuth && !isAuthenticated.value && to.name !== 'auth') {
    if (isGuestSession.value && to.meta?.allowGuest) {
      return true
    }
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
