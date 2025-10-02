<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuth } from './composables/useAuth'

const router = useRouter()
const { user, isAuthenticated, logout } = useAuth()

const roleLabels: Record<string, string> = {
  consumer: '消费者',
  supplier: '供应商',
  admin: '管理员',
}

const userRoleLabel = computed(() => {
  const role = user.value?.role
  if (!role) {
    return '访客'
  }

  const normalizedRole = String(role).toLowerCase()
  return roleLabels[normalizedRole] ?? role
})

const handleLogout = () => {
  logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink to="/" class="brand" aria-label="返回首页">
        <span class="brand-mark">丝</span>
        <div class="brand-meta">
          <strong class="brand-title">SilkMall</strong>
          <span class="brand-subtitle">蚕制品智慧销售平台</span>
        </div>
      </RouterLink>

      <nav v-if="isAuthenticated" class="primary-nav" aria-label="主导航">
        <RouterLink to="/" active-class="is-active" class="nav-link">产品中心</RouterLink>
        <RouterLink to="/orders" active-class="is-active" class="nav-link">订单中心</RouterLink>
        <RouterLink to="/about" active-class="is-active" class="nav-link">关于项目</RouterLink>
      </nav>

      <div v-if="isAuthenticated" class="user-meta" aria-live="polite">
        <div class="user-info">
          <span class="user-role" aria-label="当前角色">{{ userRoleLabel }}</span>
          <span class="user-name" aria-label="当前用户">{{ user?.username }}</span>
        </div>
        <button type="button" class="logout-button" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="app-main">
      <RouterView />
    </main>

    <footer class="app-footer">
      <p>© {{ new Date().getFullYear() }} SilkMall. 致力于打造数字化蚕桑产业链。</p>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
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

.user-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.4rem 0.75rem;
  border-radius: 0.75rem;
  background: rgba(242, 142, 28, 0.08);
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-role {
  font-size: 0.85rem;
  font-weight: 600;
  color: #f28e1c;
}

.user-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1c1c1e;
}

.logout-button {
  appearance: none;
  border: none;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(242, 142, 28, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.logout-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(242, 142, 28, 0.28);
}

.logout-button:active {
  transform: translateY(0);
}

.app-main {
  flex: 1;
  padding-bottom: 3rem;
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
    gap: 1rem;
  }

  .primary-nav {
    width: 100%;
    justify-content: space-between;
  }

  .nav-link {
    flex: 1;
    text-align: center;
  }

  .user-meta {
    width: 100%;
    justify-content: space-between;
  }
}

@media (prefers-color-scheme: dark) {
  .brand-subtitle,
  .nav-link,
  .app-footer,
  .user-name {
    color: rgba(255, 255, 255, 0.7);
  }

  .user-meta {
    background: rgba(242, 177, 66, 0.15);
  }

  .logout-button {
    box-shadow: 0 10px 20px rgba(242, 177, 66, 0.25);
  }

  .app-footer {
    border-top-color: rgba(255, 255, 255, 0.12);
  }
}
</style>
