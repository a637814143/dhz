<script setup lang="ts">
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router'
import { login, register, requestCaptcha } from '@/services/auth'
import { useAuthState } from '@/services/authState'
import type {
  CaptchaChallenge,
  LoginPayload,
  RegisterPayload,
  UserRole,
} from '@/types'

type AuthMode = 'login' | 'register'

const router = useRouter()
const route = useRoute()
const { setAuth, state } = useAuthState()

const mode = ref<AuthMode>((route.query.mode as AuthMode) === 'register' ? 'register' : 'login')

const loginForm = reactive({
  username: '',
  password: '',
  verificationCode: '',
})

const captcha = ref<CaptchaChallenge | null>(null)
const loginLoading = ref(false)
const loginError = ref<string | null>(null)
const loginSuccessMessage = ref<string | null>(null)
const registrationSuccessMessage = ref<string | null>(null)

const captchaCountdown = ref(0)
let countdownTimer: number | null = null

const registerForm = reactive<RegisterPayload>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  userType: 'consumer',
})

const registerLoading = ref(false)
const registerError = ref<string | null>(null)
const registerSuccessBanner = ref<string | null>(null)

const roles: { label: string; value: UserRole; description: string }[] = [
  { label: '采购/消费者', value: 'consumer', description: '浏览商城、下单、评价与售后' },
  { label: '供应商', value: 'supplier', description: '管理商品、库存、订单发货' },
  { label: '平台管理员', value: 'admin', description: '运维管理端、配置公告与权限' },
]

const canSubmitLogin = computed(() => {
  return (
    loginForm.username.trim().length > 0 &&
    loginForm.password.trim().length >= 6 &&
    loginForm.verificationCode.trim().length > 0 &&
    captcha.value !== null &&
    !loginLoading.value
  )
})

const canSubmitRegister = computed(() => {
  return (
    registerForm.username.trim().length >= 3 &&
    registerForm.password.trim().length >= 6 &&
    registerForm.password === registerForm.confirmPassword &&
    registerForm.email.trim().length > 0 &&
    registerForm.phone.trim().length > 0 &&
    !registerLoading.value
  )
})

watch(
  () => route.query.mode,
  (value) => {
    const next: AuthMode = value === 'register' ? 'register' : 'login'
    if (mode.value !== next) {
      mode.value = next
    }
  }
)

watch(mode, (next) => {
  if (next === 'login') {
    registerError.value = null
    registerSuccessBanner.value = null
    if (!captcha.value) {
      fetchCaptcha()
    }
  } else {
    loginError.value = null
    loginSuccessMessage.value = null
    registrationSuccessMessage.value = null
  }
})

function updateMode(next: AuthMode) {
  if (mode.value === next) return
  mode.value = next
  const query: LocationQueryRaw = { ...route.query, mode: next }
  router.replace({ name: 'auth', query })
}

async function fetchCaptcha() {
  try {
    captcha.value = await requestCaptcha()
    loginForm.verificationCode = ''
    startCountdown(captcha.value.expiresIn)
  } catch (err) {
    loginError.value = err instanceof Error ? err.message : '无法获取验证码'
  }
}

function startCountdown(seconds: number) {
  captchaCountdown.value = seconds
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  countdownTimer = window.setInterval(() => {
    if (captchaCountdown.value <= 1) {
      if (countdownTimer) {
        window.clearInterval(countdownTimer)
      }
      countdownTimer = null
      captchaCountdown.value = 0
    } else {
      captchaCountdown.value -= 1
    }
  }, 1000)
}

async function submitLogin() {
  if (!captcha.value) {
    loginError.value = '请先获取验证码'
    return
  }

  loginLoading.value = true
  loginError.value = null
  loginSuccessMessage.value = null

  const payload: LoginPayload = {
    username: loginForm.username.trim(),
    password: loginForm.password,
    verificationCode: loginForm.verificationCode.trim(),
    challengeId: captcha.value.challengeId,
  }

  try {
    const response = await login(payload)
    setAuth(response)
    loginSuccessMessage.value = '登录成功，即将跳转…'
    const redirect = (route.query.redirect as string | undefined) ?? response.redirectUrl
    window.setTimeout(() => {
      router.replace(redirect ?? '/consumer/dashboard')
    }, 600)
  } catch (err) {
    loginError.value = err instanceof Error ? err.message : '登录失败，请稍后重试'
    await fetchCaptcha()
  } finally {
    loginLoading.value = false
  }
}

function handleLoginKey(event: KeyboardEvent) {
  if (event.key === 'Enter' && canSubmitLogin.value) {
    submitLogin()
  }
}

function refreshCaptcha() {
  fetchCaptcha()
}

async function submitRegister() {
  registerLoading.value = true
  registerError.value = null
  registerSuccessBanner.value = null

  try {
    await register({ ...registerForm })
    registerSuccessBanner.value = '注册成功，即将跳转到登录页'
    window.setTimeout(() => {
      loginForm.username = registerForm.username.trim()
      registrationSuccessMessage.value = '注册成功，请使用账号登录'
      updateMode('login')
      fetchCaptcha()
    }, 800)
  } catch (err) {
    registerError.value = err instanceof Error ? err.message : '注册失败，请稍后重试'
  } finally {
    registerLoading.value = false
  }
}

onMounted(() => {
  if (state.user && state.token) {
    const redirectTarget = state.redirectUrl ?? resolveHome(state.user.userType)
    router.replace(redirectTarget)
    return
  }
  if (mode.value === 'login') {
    fetchCaptcha()
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
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
      return '/'
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-shell" :class="[`mode-${mode}`]">
      <div class="auth-hero" aria-hidden="true">
        <div class="hero-content">
          <h1>蚕制品销售数字蚕桑平台</h1>
          <p>
            一站式完成采购、供应、运营与客服，支持多角色协同与多因素安全登录。
          </p>
          <ul>
            <li>实时掌握热销排行与促销活动</li>
            <li>全链路订单、库存与物流跟踪</li>
            <li>智能客服、评价体系与互动社群</li>
          </ul>
        </div>
      </div>

      <div class="auth-panel" @keyup="handleLoginKey">
        <div class="panel-header">
          <div class="panel-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              :aria-selected="mode === 'login'"
              :class="['tab-button', { active: mode === 'login' }]"
              @click="updateMode('login')"
            >
              安全登录
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="mode === 'register'"
              :class="['tab-button', { active: mode === 'register' }]"
              @click="updateMode('register')"
            >
              立即注册
            </button>
          </div>
          <p v-if="mode === 'login'">请输入账号、密码及验证码完成身份验证。</p>
          <p v-else>完善账户信息，选择角色即可加入蚕制品销售。</p>
        </div>

        <div class="panel-body">
          <Transition name="auth-swap" mode="out-in">
            <form v-if="mode === 'login'" key="login" class="panel-form" @submit.prevent="submitLogin">
              <label class="form-control">
                <span>账号</span>
                <input v-model.trim="loginForm.username" type="text" placeholder="用户名 / 邮箱" autocomplete="username" />
              </label>

              <label class="form-control">
                <span>密码</span>
                <input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                />
              </label>

              <label class="form-control captcha-group">
                <span class="field-label">安全验证码</span>
                <div class="captcha-row">
                  <input
                    id="login-captcha"
                    v-model.trim="loginForm.verificationCode"
                    type="text"
                    placeholder="请输入计算结果"
                    maxlength="6"
                  />
                  <button
                    type="button"
                    class="captcha-button"
                    @click="refreshCaptcha"
                    :disabled="captchaCountdown > 0"
                  >
                    <span class="captcha-question" v-if="captcha">{{ captcha.question }}</span>
                    <span v-else>获取验证码</span>
                    <small v-if="captchaCountdown > 0">{{ captchaCountdown }}s</small>
                  </button>
                </div>
              </label>

              <div v-if="loginError" class="form-alert is-error" role="alert">{{ loginError }}</div>
              <div
                v-if="loginSuccessMessage || registrationSuccessMessage"
                class="form-alert is-success"
                role="status"
              >
                {{ loginSuccessMessage ?? registrationSuccessMessage }}
              </div>

              <button type="submit" class="submit-button" :disabled="!canSubmitLogin">
                <span v-if="loginLoading" class="loader" aria-hidden="true"></span>
                {{ loginLoading ? '正在验证…' : '安全登录' }}
              </button>

              <footer class="panel-footer">
                <span>还没有账号？</span>
                <button type="button" class="link-button" @click="updateMode('register')">立即注册</button>
              </footer>
            </form>

            <form v-else key="register" class="panel-form" @submit.prevent="submitRegister">
              <div class="form-grid">
                <label class="form-control">
                  <span>用户名</span>
                  <input v-model.trim="registerForm.username" type="text" placeholder="3-20 个字符" autocomplete="username" />
                </label>

                <label class="form-control">
                  <span>邮箱</span>
                  <input v-model.trim="registerForm.email" type="email" placeholder="example@canzhipin.com" autocomplete="email" />
                </label>

                <label class="form-control">
                  <span>手机号</span>
                  <input v-model.trim="registerForm.phone" type="tel" placeholder="用于接收通知" autocomplete="tel" />
                </label>

                <label class="form-control">
                  <span>登录密码</span>
                  <input v-model="registerForm.password" type="password" placeholder="至少 6 位" autocomplete="new-password" />
                </label>

                <label class="form-control">
                  <span>确认密码</span>
                  <input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="再次输入密码"
                    autocomplete="new-password"
                  />
                </label>
              </div>

              <fieldset class="role-selector">
                <legend>选择用户类型</legend>
                <div class="role-options">
                  <label
                    v-for="role in roles"
                    :key="role.value"
                    :class="['role-card', { active: registerForm.userType === role.value }]"
                  >
                    <input v-model="registerForm.userType" type="radio" :value="role.value" />
                    <div class="role-meta">
                      <strong>{{ role.label }}</strong>
                      <p>{{ role.description }}</p>
                    </div>
                  </label>
                </div>
              </fieldset>

              <div v-if="registerError" class="form-alert is-error" role="alert">{{ registerError }}</div>
              <div v-if="registerSuccessBanner" class="form-alert is-success" role="status">{{ registerSuccessBanner }}</div>

              <button class="submit-button" type="submit" :disabled="!canSubmitRegister">
                <span v-if="registerLoading" class="loader" aria-hidden="true"></span>
                {{ registerLoading ? '提交中…' : '提交注册' }}
              </button>

              <footer class="panel-footer">
                <span>已有账号？</span>
                <button type="button" class="link-button" @click="updateMode('login')">直接登录</button>
              </footer>
            </form>
          </Transition>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5rem 2.5rem;
  background: radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.95), rgba(245, 222, 179, 0.2)),
    linear-gradient(145deg, rgba(247, 238, 225, 0.8), rgba(242, 142, 28, 0.18));
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr);
  width: min(1500px, 100%);
  background: linear-gradient(135deg, rgba(245, 223, 189, 0.65), rgba(255, 255, 255, 0.95));
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.08);
}

.auth-hero {
  position: relative;
  background: radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.75), transparent 65%),
    radial-gradient(circle at 80% 10%, rgba(255, 255, 255, 0.65), transparent 55%),
    linear-gradient(160deg, rgba(242, 142, 28, 0.35), rgba(93, 44, 12, 0.35));
  color: #1f2937;
  padding: 3.5rem 3rem 3rem;
  display: flex;
  align-items: flex-end;
}

.auth-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 25% 75%, rgba(255, 255, 255, 0.25), transparent 60%),
    radial-gradient(circle at 90% 15%, rgba(242, 142, 28, 0.25), transparent 65%);
  opacity: 0.7;
  pointer-events: none;
}

.hero-content {
  max-width: 480px;
  display: grid;
  gap: 1rem;
  color: rgba(17, 24, 39, 0.9);
  position: relative;
  z-index: 1;
}

.hero-content h1 {
  font-size: 2.4rem;
  font-weight: 700;
  color: #5c2c0c;
}

.hero-content p {
  font-size: 1.05rem;
  line-height: 1.7;
  color: rgba(17, 24, 39, 0.75);
}

.hero-content ul {
  display: grid;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
  list-style: none;
}

.hero-content li {
  padding-left: 1.5rem;
  position: relative;
  font-weight: 500;
  color: rgba(17, 24, 39, 0.75);
}

.hero-content li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.5rem;
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 50%;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  box-shadow: 0 0 0 4px rgba(242, 142, 28, 0.15);
}

.auth-panel {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  padding: 3.6rem 3.9rem 3.9rem;
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
  align-items: center;
  position: relative;
}

.auth-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.panel-header {
  display: grid;
  gap: 1rem;
  justify-items: center;
  text-align: center;
  max-width: 520px;
}

.panel-tabs {
  display: inline-flex;
  padding: 0.35rem;
  border-radius: 999px;
  background: rgba(242, 142, 28, 0.12);
  box-shadow: inset 0 0 0 1px rgba(242, 142, 28, 0.12);
}

.tab-button {
  border: none;
  background: transparent;
  border-radius: 999px;
  padding: 0.65rem 1.5rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: rgba(17, 24, 39, 0.65);
  cursor: pointer;
  transition: all 0.25s ease;
  font-size: 0.95rem;
}

.tab-button.active {
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  box-shadow: 0 12px 20px rgba(242, 142, 28, 0.3);
}

.panel-header p {
  color: rgba(17, 24, 39, 0.6);
}


.panel-form {
  display: grid;
  gap: 1.75rem;
  width: min(720px, 100%);
  padding: 2.3rem 2.6rem 2.5rem;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 20px;
  box-shadow: 0 24px 45px rgba(17, 24, 39, 0.08);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.auth-shell.mode-register .panel-form {
  width: min(1120px, 100%);
}

.panel-body {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.75rem;
}

.auth-swap-enter-active,
.auth-swap-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.auth-swap-enter-from,
.auth-swap-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

.auth-shell.mode-register .panel-form,
.auth-shell.mode-login .panel-form {
  transform: translateY(0);
}

.form-control {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.form-control span {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.75);
}

.form-control input {
  border-radius: 14px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  padding: 0.9rem 1rem;
  background: rgba(255, 255, 255, 0.96);
  font-size: 1rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-control input:focus {
  outline: none;
  border-color: rgba(79, 70, 229, 0.65);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.18);
}

.captcha-group {
  gap: 0.75rem;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.captcha-row input {
  flex: 1;
}

.captcha-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  border: none;
  border-radius: 12px;
  padding: 0.85rem 1.25rem;
  background: rgba(242, 142, 28, 0.1);
  color: #f07a26;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
  min-width: 160px;
  white-space: nowrap;
}

.captcha-button:hover:not(:disabled) {
  background: rgba(242, 142, 28, 0.2);
  transform: translateY(-1px);
}

.captcha-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.captcha-button small {
  font-size: 0.8rem;
  color: rgba(240, 122, 38, 0.75);
}

.form-alert {
  border-radius: 12px;
  padding: 0.9rem 1rem;
  font-weight: 500;
}

.form-alert.is-error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.form-alert.is-success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.submit-button {
  border: none;
  border-radius: 14px;
  padding: 0.95rem 1.4rem;
  font-weight: 700;
  font-size: 1rem;
  letter-spacing: 0.02em;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  box-shadow: 0 18px 25px rgba(242, 142, 28, 0.25);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 22px 30px rgba(242, 142, 28, 0.3);
}

.loader {
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
}

.form-grid {
  display: grid;
  gap: 1.35rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.auth-shell.mode-register .form-grid {
  grid-template-columns: repeat(2, minmax(260px, 1fr));
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

.auth-shell.mode-register .role-options {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 1024px) {
  .auth-shell.mode-register .panel-form {
    width: min(760px, 100%);
  }

  .auth-shell.mode-register .role-options {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.role-card {
  display: grid;
  gap: 0.6rem;
  padding: 1.1rem 1rem;
  border-radius: 16px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  background: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.role-card input {
  display: none;
}

.role-card.active {
  border-color: rgba(242, 142, 28, 0.45);
  box-shadow: 0 12px 24px rgba(242, 142, 28, 0.18);
  transform: translateY(-2px);
}

.role-meta strong {
  font-size: 1rem;
  color: rgba(17, 24, 39, 0.85);
}

.role-meta p {
  font-size: 0.9rem;
  color: rgba(17, 24, 39, 0.6);
}

.panel-footer {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.5rem;
  font-size: 0.95rem;
  color: rgba(17, 24, 39, 0.65);
  justify-content: center;
}

.link-button {
  border: none;
  background: transparent;
  color: #f07a26;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1080px) {
  .auth-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-hero {
    display: none;
  }

  .auth-panel {
    padding: 2.5rem 1.75rem 3rem;
  }
}

@media (max-width: 720px) {
  .auth-page {
    padding: 3rem 1rem;
  }

  .panel-form {
    padding: 1.5rem 1.25rem 1.75rem;
  }

  .form-grid,
  .auth-shell.mode-register .form-grid {
    grid-template-columns: 1fr;
  }

  .auth-shell.mode-register .panel-form {
    width: 100%;
  }

  .auth-shell.mode-register .role-options {
    grid-template-columns: 1fr;
  }

  .captcha-row {
    flex-direction: column;
    align-items: stretch;
  }

  .captcha-button {
    width: 100%;
  }
}
</style>
