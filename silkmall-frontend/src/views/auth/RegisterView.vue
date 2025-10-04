<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/services/auth'
import type { RegisterPayload, UserRole } from '@/types'

const router = useRouter()

const form = reactive<RegisterPayload>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  companyName: '',
  userType: 'consumer',
})

const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const roles: { label: string; value: UserRole; description: string }[] = [
  { label: '采购/消费者', value: 'consumer', description: '浏览商城、下单、评价与售后' },
  { label: '供应商', value: 'supplier', description: '管理商品、库存、订单发货' },
  { label: '平台管理员', value: 'admin', description: '运维管理端、配置公告与权限' },
]

const canSubmit = computed(() => {
  return (
    form.username.trim().length >= 3 &&
    form.password.trim().length >= 6 &&
    form.password === form.confirmPassword &&
    form.email.trim().length > 0 &&
    form.phone.trim().length > 0 &&
    (form.userType !== 'supplier' || form.companyName?.trim().length) &&
    !loading.value
  )
})

async function submit() {
  loading.value = true
  error.value = null
  successMessage.value = null

  try {
    const payload: RegisterPayload = {
      ...form,
      companyName: form.userType === 'supplier' ? form.companyName?.trim() || '' : null,
    }

    await register(payload)
    successMessage.value = '注册成功，请使用账号登录'
    window.setTimeout(() => {
      router.replace({ name: 'login', query: { redirect: router.currentRoute.value.query.redirect } })
    }, 800)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="register-shell">
    <div class="register-header">
      <h1>创建 SilkMall 账户</h1>
      <p>完善资料后即可体验多角色协同的蚕丝产业销售平台。</p>
    </div>

    <form class="register-form" @submit.prevent="submit">
      <div class="form-grid">
        <label class="form-control">
          <span>用户名</span>
          <input v-model.trim="form.username" type="text" placeholder="3-20 个字符" autocomplete="username" />
        </label>

        <label class="form-control">
          <span>邮箱</span>
          <input v-model.trim="form.email" type="email" placeholder="example@sikmall.com" autocomplete="email" />
        </label>

        <label class="form-control">
          <span>手机号</span>
          <input v-model.trim="form.phone" type="tel" placeholder="用于接收通知" autocomplete="tel" />
        </label>

        <label class="form-control">
          <span>登录密码</span>
          <input v-model="form.password" type="password" placeholder="至少 6 位" autocomplete="new-password" />
        </label>

        <label class="form-control">
          <span>确认密码</span>
          <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" autocomplete="new-password" />
        </label>

        <label v-if="form.userType === 'supplier'" class="form-control">
          <span>企业名称</span>
          <input v-model.trim="form.companyName" type="text" placeholder="请填写贵司名称" />
        </label>
      </div>

      <fieldset class="role-selector">
        <legend>选择用户类型</legend>
        <div class="role-options">
          <label v-for="role in roles" :key="role.value" :class="['role-card', { active: form.userType === role.value }]">
            <input v-model="form.userType" type="radio" :value="role.value" />
            <div class="role-meta">
              <strong>{{ role.label }}</strong>
              <p>{{ role.description }}</p>
            </div>
          </label>
        </div>
      </fieldset>

      <div v-if="error" class="form-alert is-error" role="alert">{{ error }}</div>
      <div v-if="successMessage" class="form-alert is-success" role="status">{{ successMessage }}</div>

      <button class="submit-button" type="submit" :disabled="!canSubmit">
        <span v-if="loading" class="loader" aria-hidden="true"></span>
        {{ loading ? '提交中…' : '提交注册' }}
      </button>
    </form>

    <footer class="register-footer">
      <span>已有账号？</span>
      <router-link to="/login">直接登录</router-link>
      <router-link to="/">返回首页</router-link>
    </footer>
  </section>
</template>

<style scoped>
.register-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 3.5rem 2.5rem 4rem;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 28px;
  box-shadow: 0 25px 60px rgba(20, 20, 20, 0.08);
  display: grid;
  gap: 2.5rem;
}

.register-header h1 {
  font-size: 2.25rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.register-header p {
  color: rgba(17, 24, 39, 0.65);
}

.register-form {
  display: grid;
  gap: 2rem;
}

.form-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.form-control {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-control span {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.7);
}

.form-control input {
  border-radius: 14px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  padding: 0.9rem 1rem;
  background: rgba(255, 255, 255, 0.95);
  font-size: 1rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-control input:focus {
  outline: none;
  border-color: rgba(79, 70, 229, 0.65);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.18);
}

.role-selector {
  border: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}

.role-selector legend {
  font-weight: 700;
  color: rgba(17, 24, 39, 0.75);
}

.role-options {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.role-card {
  position: relative;
  border-radius: 16px;
  border: 1px solid rgba(79, 70, 229, 0.15);
  padding: 1.2rem;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(79, 70, 229, 0.15);
}

.role-card.active {
  border-color: rgba(79, 70, 229, 0.55);
  box-shadow: 0 20px 40px rgba(79, 70, 229, 0.18);
}

.role-card input {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.role-meta {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.role-meta strong {
  font-size: 1.05rem;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.82);
}

.role-meta p {
  color: rgba(17, 24, 39, 0.6);
  font-size: 0.9rem;
}

.form-alert {
  border-radius: 12px;
  padding: 0.85rem 1rem;
  font-weight: 600;
}

.form-alert.is-error {
  background: rgba(240, 68, 56, 0.12);
  color: #8b1d12;
}

.form-alert.is-success {
  background: rgba(34, 197, 94, 0.12);
  color: #166534;
}

.submit-button {
  justify-self: start;
  border: none;
  border-radius: 14px;
  padding: 0.9rem 2.5rem;
  font-size: 1.05rem;
  font-weight: 700;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.submit-button[disabled] {
  opacity: 0.65;
  cursor: not-allowed;
}

.submit-button:not([disabled]):hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 32px rgba(79, 70, 229, 0.22);
}

.loader {
  width: 1.1rem;
  height: 1.1rem;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.4);
  border-top-color: rgba(255, 255, 255, 0.95);
  animation: spin 0.85s linear infinite;
}

.register-footer {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.6);
}

.register-footer a {
  color: #4f46e5;
  text-decoration: none;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .register-shell {
    padding: 2.5rem 1.75rem 3rem;
    border-radius: 20px;
  }
}

@media (max-width: 520px) {
  .register-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
