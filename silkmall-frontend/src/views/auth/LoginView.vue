<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, requestCaptcha } from '@/services/auth'
import { useAuthState } from '@/services/authState'
import type { CaptchaChallenge, LoginPayload } from '@/types'

const router = useRouter()
const route = useRoute()
const { setAuth, state } = useAuthState()

const form = reactive({
  username: '',
  password: '',
  verificationCode: '',
})

const captcha = ref<CaptchaChallenge | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const captchaCountdown = ref(0)
let countdownTimer: number | null = null

const canSubmit = computed(() => {
  return (
    form.username.trim().length > 0 &&
    form.password.trim().length >= 6 &&
    form.verificationCode.trim().length > 0 &&
    captcha.value !== null &&
    !loading.value
  )
})

async function fetchCaptcha() {
  try {
    captcha.value = await requestCaptcha()
    form.verificationCode = ''
    startCountdown(captcha.value.expiresIn)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '无法获取验证码'
  }
}

function startCountdown(seconds: number) {
  captchaCountdown.value = seconds
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  countdownTimer = window.setInterval(() => {
    if (captchaCountdown.value <= 1) {
      window.clearInterval(countdownTimer!)
      countdownTimer = null
      captchaCountdown.value = 0
    } else {
      captchaCountdown.value -= 1
    }
  }, 1000)
}

async function submit() {
  if (!captcha.value) {
    error.value = '请先获取验证码'
    return
  }

  loading.value = true
  error.value = null
  successMessage.value = null

  const payload: LoginPayload = {
    username: form.username.trim(),
    password: form.password,
    verificationCode: form.verificationCode.trim(),
    challengeId: captcha.value.challengeId,
  }

  try {
    const response = await login(payload)
    setAuth(response)
    successMessage.value = '登录成功，即将跳转…'
    const redirect = (route.query.redirect as string | undefined) ?? response.redirectUrl
    window.setTimeout(() => {
      router.replace(redirect)
    }, 600)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败，请稍后重试'
    await fetchCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (state.user && state.token) {
    router.replace(state.redirectUrl ?? '/consumer/dashboard')
    return
  }
  fetchCaptcha()
})

function handleKeySubmit(event: KeyboardEvent) {
  if (event.key === 'Enter' && canSubmit.value) {
    submit()
  }
}

function refreshCaptcha() {
  fetchCaptcha()
}

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
})
</script>

<template>
  <section class="login-shell">
    <div class="login-hero" aria-hidden="true">
      <div class="hero-content">
        <h1>欢迎回到 SilkMall</h1>
        <p>
          使用多因素认证保障安全，畅享蚕丝产业链数字化销售、采购、客服一体化服务。
        </p>
        <ul>
          <li>实时掌握热销排行与促销活动</li>
          <li>多角色协同的订单与库存管理</li>
          <li>一站式客服、评价与互动体验</li>
        </ul>
      </div>
    </div>

    <div class="login-panel" @keyup="handleKeySubmit">
      <div class="panel-header">
        <h2>账户登录</h2>
        <p>请使用注册时的账号、密码及安全验证码完成验证。</p>
      </div>

      <form class="panel-form" @submit.prevent="submit">
        <label class="form-control">
          <span>账号</span>
          <input v-model.trim="form.username" type="text" placeholder="用户名 / 邮箱" autocomplete="username" />
        </label>

        <label class="form-control">
          <span>密码</span>
          <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </label>

        <div class="form-control captcha-group">
          <div class="captcha-input">
            <span>安全验证码</span>
            <input
              v-model.trim="form.verificationCode"
              type="text"
              placeholder="请输入计算结果"
              maxlength="6"
            />
          </div>
          <button type="button" class="captcha-button" @click="refreshCaptcha" :disabled="captchaCountdown > 0">
            <span v-if="captcha">{{ captcha.question }}</span>
            <span v-else>获取验证码</span>
            <small v-if="captchaCountdown > 0">{{ captchaCountdown }}s</small>
          </button>
        </div>

        <div v-if="error" class="form-alert is-error" role="alert">{{ error }}</div>
        <div v-if="successMessage" class="form-alert is-success" role="status">{{ successMessage }}</div>

        <button type="submit" class="submit-button" :disabled="!canSubmit">
          <span v-if="loading" class="loader" aria-hidden="true"></span>
          {{ loading ? '正在验证…' : '安全登录' }}
        </button>
      </form>

      <div class="panel-footer">
        <router-link to="/register">还没有账号？立即注册</router-link>
        <button class="link-button" type="button" @click="router.push({ name: 'home' })">返回首页浏览</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  min-height: calc(100vh - 120px);
  background: linear-gradient(135deg, rgba(245, 223, 189, 0.6), rgba(255, 255, 255, 0.9));
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.08);
}

.login-hero {
  position: relative;
  background: radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.75), transparent 65%),
    linear-gradient(135deg, #f7d08a, #f2a65a);
  padding: 4rem 3.5rem;
  color: #40210f;
}

.login-hero::after {
  content: '';
  position: absolute;
  inset: 20px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 420px;
}

.hero-content h1 {
  font-size: 2.75rem;
  line-height: 1.15;
  margin-bottom: 1.5rem;
  font-weight: 700;
}

.hero-content p {
  font-size: 1rem;
  margin-bottom: 1.75rem;
  color: rgba(64, 33, 15, 0.88);
}

.hero-content ul {
  display: grid;
  gap: 0.75rem;
  list-style: none;
  padding: 0;
}

.hero-content li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 600;
}

.hero-content li::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.9);
}

.login-panel {
  padding: 3.5rem 3rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.panel-header h2 {
  font-size: 1.75rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.panel-header p {
  color: rgba(17, 24, 39, 0.65);
}

.panel-form {
  display: grid;
  gap: 1.25rem;
}

.form-control {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-control span {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.75);
}

.form-control input {
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 12px;
  padding: 0.85rem 1rem;
  font-size: 1rem;
  background: rgba(255, 255, 255, 0.9);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-control input:focus {
  outline: none;
  border-color: rgba(242, 166, 90, 0.85);
  box-shadow: 0 0 0 4px rgba(242, 166, 90, 0.15);
}

.captcha-group {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1rem;
  align-items: end;
}

.captcha-input {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.captcha-button {
  border: none;
  border-radius: 12px;
  padding: 0.85rem 1.1rem;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, #f2a65a, #f07a26);
  color: #fff;
  min-width: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.captcha-button[disabled] {
  cursor: not-allowed;
  opacity: 0.7;
}

.captcha-button:hover:not([disabled]) {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(240, 122, 38, 0.28);
}

.captcha-button small {
  font-size: 0.75rem;
  opacity: 0.85;
}

.form-alert {
  border-radius: 12px;
  padding: 0.85rem 1rem;
  font-size: 0.95rem;
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
  border: none;
  border-radius: 14px;
  padding: 0.95rem 1rem;
  font-size: 1.05rem;
  font-weight: 700;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #2d1b05;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.submit-button[disabled] {
  opacity: 0.65;
  cursor: not-allowed;
}

.submit-button:not([disabled]):hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 30px rgba(242, 142, 28, 0.32);
}

.loader {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.4);
  border-top-color: rgba(255, 255, 255, 0.95);
  animation: spin 0.9s linear infinite;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  color: rgba(17, 24, 39, 0.65);
}

.panel-footer a,
.link-button {
  font-weight: 600;
  color: #f07a26;
  text-decoration: none;
}

.link-button {
  border: none;
  background: transparent;
  cursor: pointer;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-hero {
    padding: 3rem 2.5rem;
  }

  .login-panel {
    padding: 2.75rem 2rem 3rem;
  }
}

@media (max-width: 640px) {
  .login-panel {
    padding: 2.25rem 1.75rem;
  }

  .captcha-group {
    grid-template-columns: 1fr;
  }

  .captcha-button {
    width: 100%;
  }
}
</style>
