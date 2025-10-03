import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useAuthState } from '@/services/authState'

const OrderCenterView = () => import('../views/OrderCenterView.vue')
const LoginView = () => import('../views/auth/LoginView.vue')
const RegisterView = () => import('../views/auth/RegisterView.vue')
const ConsumerDashboard = () => import('../views/dashboard/ConsumerDashboard.vue')
const SupplierWorkbench = () => import('../views/dashboard/SupplierWorkbench.vue')
const AdminOverview = () => import('../views/dashboard/AdminOverview.vue')
const AdminConsumerManagement = () => import('../views/dashboard/AdminConsumerManagement.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/orders',
      name: 'orders',
      component: OrderCenterView,
      meta: { requiresAuth: true, roles: ['consumer', 'admin'] },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guestOnly: true },
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
      return '/login'
  }
}

router.beforeEach((to) => {
  const { isAuthenticated, state } = useAuthState()

  if (to.meta?.requiresAuth && !isAuthenticated.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
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
