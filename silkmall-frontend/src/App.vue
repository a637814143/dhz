<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute, type RouteLocationRaw } from 'vue-router'
import { useAuthState } from '@/services/authState'

const router = useRouter()
const route = useRoute()
const { state, isAuthenticated, clearAuth, isGuestSession, exitGuestMode } = useAuthState()

interface SidebarLink {
  label: string
  to: RouteLocationRaw
}

const roleHome = computed(() => {
  switch (state.user?.userType) {
    case 'admin':
      return '/admin/overview'
    case 'supplier':
      return '/supplier/workbench'
    case 'consumer':
      return '/consumer/dashboard'
    default:
      return '/auth'
  }
})

const showGuestLinks = computed(() => {
  if (isAuthenticated.value || isGuestSession.value) {
    return true
  }
  return route.name !== 'auth'
})

const showPrimaryNav = computed(() => isAuthenticated.value || isGuestSession.value)
const showSidebar = computed(() => showPrimaryNav.value && route.name !== 'auth')

const sidebarNavItems = computed<SidebarLink[]>(() => {
  const items: SidebarLink[] = []
  const role = state.user?.userType

  if (role === 'consumer') {
    items.push(
      { label: '个人中心', to: { name: 'consumer-dashboard' } },
      { label: '地址管理', to: { name: 'consumer-addresses' } },
      { label: '我的购物车', to: { name: 'consumer-cart' } },
      { label: '我的收藏', to: { name: 'consumer-favorites' } },
      { label: '我的订单', to: { name: 'consumer-orders' } },
      { label: '我的评价', to: { name: 'consumer-reviews' } }
    )
  } else if (role === 'supplier') {
    items.push(
      { label: '个人中心', to: { name: 'supplier-workbench' } },
      { label: '商品管理', to: { name: 'supplier-products' } },
      { label: '已售订单', to: { name: 'supplier-orders' } },
      { label: '退货管理', to: { name: 'supplier-returns' } }
    )
  } else if (role === 'admin') {
    items.push(
      { label: '个人中心', to: { name: 'admin-overview' } },
      { label: '商品管理', to: { name: 'admin-products' } },
      { label: '订单管理', to: { name: 'admin-orders' } },
      { label: '退货管理', to: { name: 'admin-returns' } },
      { label: '消费者管理', to: { name: 'admin-consumers' } },
      { label: '供应商管理', to: { name: 'admin-suppliers' } },
      { label: '销售统计', to: { name: 'admin-sales' } }
    )
  }

  items.push(
    { label: '产品中心', to: { name: 'home' } },
    { label: '关于项目', to: { name: 'about' } }
  )

  return items
})

function signOut() {
  clearAuth()
  router.push({ name: 'auth', query: { mode: 'login' } })
}

function leaveGuestMode() {
  exitGuestMode()
  router.push({ name: 'auth', query: { mode: 'login' } })
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink to="/" class="brand" aria-label="返回首页">
        <span class="brand-mark">丝</span>
        <div class="brand-meta">
          <strong class="brand-title">蚕制品销售</strong>
          <span class="brand-subtitle">蚕制品智慧销售平台</span>
        </div>
      </RouterLink>

      <nav v-if="showPrimaryNav && !showSidebar" class="primary-nav" aria-label="主导航">
        <RouterLink to="/" active-class="is-active" class="nav-link">产品中心</RouterLink>
        <RouterLink to="/about" active-class="is-active" class="nav-link">关于项目</RouterLink>
      </nav>

      <div v-if="showGuestLinks" class="auth-controls">
        <template v-if="isAuthenticated">
          <span class="user-chip">{{ state.user?.username }}</span>
          <RouterLink :to="roleHome" class="dashboard-link">个人中心</RouterLink>
          <button type="button" class="logout-button" @click="signOut">退出</button>
        </template>
        <template v-else-if="isGuestSession">
          <span class="user-chip is-guest">游客模式</span>
          <button type="button" class="logout-button" @click="leaveGuestMode">退出游客</button>
          <RouterLink :to="{ name: 'auth', query: { mode: 'login' } }" class="login-link">登录</RouterLink>
          <RouterLink :to="{ name: 'auth', query: { mode: 'register' } }" class="register-link">注册</RouterLink>
        </template>
        <template v-else>
          <RouterLink :to="{ name: 'auth', query: { mode: 'login' } }" class="login-link">登录</RouterLink>
          <RouterLink :to="{ name: 'auth', query: { mode: 'register' } }" class="register-link">注册</RouterLink>
        </template>
      </div>
    </header>

    <div v-if="showSidebar" class="app-body">
      <aside class="app-sidebar" aria-label="功能导航">
        <div class="sidebar-meta">
          <p class="sidebar-title">导航</p>
          <p v-if="state.user?.username" class="sidebar-user">{{ state.user.username }}</p>
        </div>
        <nav class="sidebar-nav">
          <RouterLink
            v-for="item in sidebarNavItems"
            :key="item.label"
            :to="item.to"
            class="sidebar-link"
            active-class="is-active"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
      </aside>
      <main class="app-main with-sidebar">
        <RouterView />
      </main>
    </div>
    <main v-else class="app-main">
      <RouterView />
    </main>

    <footer class="app-footer">
      <p>© {{ new Date().getFullYear() }} 蚕制品销售。致力于打造数字化蚕桑产业链。</p>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 0;
  flex: 1;
  min-height: calc(100vh - 160px);
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 0 1rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: inherit;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, #f5d0c5, #f28e1c);
  color: #5c2c0c;
  font-size: 1.5rem;
  font-weight: 700;
  box-shadow: 0 10px 25px rgba(242, 142, 28, 0.25);
}

.brand-title {
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.brand-subtitle {
  display: block;
  font-size: 0.85rem;
  color: rgba(0, 0, 0, 0.55);
}

.primary-nav {
  display: flex;
  gap: 1rem;
}

.nav-link {
  position: relative;
  padding: 0.5rem 0.75rem;
  border-radius: 0.75rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.65);
  transition: color 0.3s ease, background 0.3s ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 0.75rem;
  right: 0.75rem;
  bottom: 0.35rem;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #f28e1c, #f5c342);
  opacity: 0;
  transform: translateY(6px);
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.nav-link:hover,
.nav-link.is-active {
  color: #1c1c1e;
  background: rgba(242, 142, 28, 0.08);
}

.nav-link.is-active::after {
  opacity: 1;
  transform: translateY(0);
}

.auth-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}

.login-link,
.register-link,
.dashboard-link {
  font-weight: 600;
  color: #f07a26;
}

.register-link {
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: rgba(240, 122, 38, 0.12);
}

.user-chip {
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.12);
  color: #4338ca;
  font-weight: 600;
}

.user-chip.is-guest {
  background: rgba(59, 130, 246, 0.14);
  color: #1d4ed8;
}

.logout-button {
  border: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.6);
  font-weight: 600;
  cursor: pointer;
}

.app-main {
  flex: 1;
  padding-bottom: 3rem;
}

.app-main.with-sidebar {
  background: #faf7f4;
  padding: 1.5rem 1.75rem;
  min-height: calc(100vh - 200px);
  border-left: 1px solid rgba(0, 0, 0, 0.04);
}

.app-sidebar {
  background: linear-gradient(160deg, #fdf4ed 0%, #f8e4cf 100%);
  padding: 1.5rem 1.1rem;
  border: 1px solid rgba(242, 142, 28, 0.18);
  box-shadow: 0 12px 30px rgba(242, 142, 28, 0.15);
  position: sticky;
  top: 0;
  align-self: stretch;
  min-height: calc(100vh - 120px);
  border-radius: 0 1rem 1rem 0;
}

.sidebar-meta {
  margin-bottom: 1rem;
}

.sidebar-title {
  font-weight: 700;
  color: #9a4a15;
  letter-spacing: 0.02em;
}

.sidebar-user {
  margin-top: 0.2rem;
  color: rgba(0, 0, 0, 0.65);
  font-size: 0.95rem;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.sidebar-link {
  display: block;
  padding: 0.55rem 0.75rem;
  border-radius: 0.85rem;
  color: rgba(0, 0, 0, 0.75);
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.sidebar-link:hover {
  background: rgba(242, 142, 28, 0.12);
  transform: translateX(2px);
}

.sidebar-link.is-active {
  background: rgba(242, 142, 28, 0.18);
  color: #9a4a15;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.app-footer {
  padding: 1.5rem 0 2rem;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 0.85rem;
  color: rgba(0, 0, 0, 0.6);
  text-align: center;
}

@media (max-width: 768px) {
  .app-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .app-body {
    grid-template-columns: 1fr;
  }

  .app-sidebar {
    position: static;
  }

  .primary-nav {
    width: 100%;
    justify-content: space-between;
  }

  .nav-link {
    flex: 1;
    text-align: center;
  }

  .auth-controls {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}

@media (prefers-color-scheme: dark) {
  .brand-subtitle,
  .nav-link,
  .app-footer {
    color: rgba(255, 255, 255, 0.7);
  }

  .nav-link:hover,
  .nav-link.is-active {
    color: #ffffff;
    background: rgba(242, 177, 66, 0.15);
  }

  .app-main.with-sidebar {
    background: #1c1c1e;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  }

  .app-sidebar {
    border-color: rgba(255, 255, 255, 0.15);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.35);
  }

  .app-footer {
    border-top-color: rgba(255, 255, 255, 0.12);
  }
}
</style>
