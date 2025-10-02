import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { isAuthenticated } from '../composables/useAuth'

const LoginView = () => import('../views/LoginView.vue')
const OrderCenterView = () => import('../views/OrderCenterView.vue')
const AboutView = () => import('../views/AboutView.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        public: true,
      },
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/about',
      name: 'about',
      component: AboutView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/orders',
      name: 'orders',
      component: OrderCenterView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to, from, next) => {
  const loggedIn = isAuthenticated()

  if (to.meta?.requiresAuth && !loggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  if (to.name === 'login' && loggedIn) {
    const redirectTarget = typeof to.query.redirect === 'string' ? to.query.redirect : undefined
    if (redirectTarget) {
      next({ path: redirectTarget })
    } else {
      next({ name: 'home' })
    }
    return
  }

  next()
})

export default router
