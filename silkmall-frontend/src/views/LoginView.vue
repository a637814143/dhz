<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../services/auth'
import { useAuth } from '../composables/useAuth'

interface LoginFormState {
  username: string
  password: string
  captcha: string
}

const route = useRoute()
const router = useRouter()
const { setUser } = useAuth()

const form = reactive<LoginFormState>({
  username: '',
  password: '',
  captcha: '',
})

const loading = ref(false)
const errorMessage = ref('')
const captchaCode = ref(generateCaptcha())

function generateCaptcha(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let result = ''
  for (let index = 0; index < 4; index += 1) {
    const randomIndex = Math.floor(Math.random() * chars.length)
    result += chars[randomIndex]
  }
  return result
}

function refreshCaptcha() {
  captchaCode.value = generateCaptcha()
}

function normalizeRedirect(target?: string) {
  if (!target || typeof target !== 'string') {
    return '/'
  }

  if (!target.startsWith('/')) {
    return '/'
  }

  return target
}

async function handleSubmit() {
  if (loading.value) {
    return
  }

  errorMessage.value = ''

  if (!form.username.trim() || !form.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  if (form.captcha.trim().toUpperCase() !== captchaCode.value) {
    errorMessage.value = '验证码错误，请重新输入'
    form.captcha = ''
    refreshCaptcha()
    return
  }

  loading.value = true
  try {
    const user = await login({
      username: form.username.trim(),
      password: form.password,
    })

    setUser(user)
    const redirectTarget = normalizeRedirect(route.query.redirect as string | undefined)
    router.replace(redirectTarget)
  } catch (error) {
    if (error instanceof Error) {
      errorMessage.value = error.message
    } else {
      errorMessage.value = '登录失败，请稍后重试'
    }
    refreshCaptcha()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-container">
    <div class="login-card" role="form" aria-labelledby="login-title">
      <header class="login-header">
        <h1 id="login-title">统一登录</h1>
        <p class="login-subtitle">请使用分配的角色账号登录以访问 SilkMall 平台</p>
      </header>

      <form class="login-form" @submit.prevent="handleSubmit">
        <label class="form-field">
          <span class="field-label">用户名</span>
          <input
            v-model.trim="form.username"
            type="text"
            name="username"
            autocomplete="username"
            placeholder="请输入账号"
            required
          />
        </label>

        <label class="form-field">
          <span class="field-label">密码</span>
          <input
            v-model="form.password"
            type="password"
            name="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            required
          />
        </label>

        <div class="form-field captcha-field">
          <label class="captcha-input">
            <span class="field-label">验证码</span>
            <input
              v-model="form.captcha"
              type="text"
              name="captcha"
              placeholder="请输入验证码"
              maxlength="4"
              required
            />
          </label>
          <button type="button" class="captcha-display" @click="refreshCaptcha" title="点击刷新验证码">
            <span aria-hidden="true">{{ captchaCode }}</span>
            <span class="sr-only">点击刷新验证码</span>
          </button>
        </div>

        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

        <button class="submit-button" type="submit" :disabled="loading">
          {{ loading ? '正在登录…' : '登录' }}
        </button>
      </form>

      <aside class="login-hint" aria-live="polite">
        <h2>角色账号说明</h2>
        <ul>
          <li><strong>消费者：</strong>用户名 <code>consumer_01</code>，密码 <code>Consumer@123</code></li>
          <li><strong>供应商：</strong>用户名 <code>supplier_01</code>，密码 <code>Supplier@123</code></li>
          <li><strong>管理员：</strong>用户名 <code>admin_01</code>，密码 <code>Admin@123</code></li>
        </ul>
        <p>如需修改默认密码，请登录后前往账号管理模块。</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.login-container {
  display: grid;
  place-items: center;
  min-height: calc(100vh - 6rem);
  padding: 2rem 1.5rem 3rem;
  background: linear-gradient(135deg, rgba(245, 208, 197, 0.4), rgba(242, 142, 28, 0.18));
}

.login-card {
  width: min(960px, 100%);
  background: #fff;
  border-radius: 1.5rem;
  box-shadow: 0 25px 55px rgba(15, 23, 42, 0.1);
  padding: 2.5rem;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2.5rem;
}

.login-header {
  grid-column: 1 / -1;
}

.login-header h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #1c1c1e;
}

.login-subtitle {
  margin-top: 0.75rem;
  color: rgba(0, 0, 0, 0.6);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-weight: 600;
  color: #374151;
}

input[type='text'],
input[type='password'] {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.5);
  background: rgba(248, 250, 252, 0.8);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  font-size: 1rem;
}

input[type='text']:focus,
input[type='password']:focus {
  outline: none;
  border-color: #f28e1c;
  box-shadow: 0 0 0 3px rgba(242, 142, 28, 0.15);
}

.captcha-field {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
}

.captcha-input {
  flex: 1;
}

.captcha-display {
  display: grid;
  place-items: center;
  min-width: 120px;
  height: 54px;
  border-radius: 0.9rem;
  border: none;
  background: repeating-linear-gradient(
      45deg,
      rgba(242, 142, 28, 0.2),
      rgba(242, 142, 28, 0.2) 10px,
      rgba(245, 195, 66, 0.25) 10px,
      rgba(245, 195, 66, 0.25) 20px
    );
  color: #9a3412;
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.4rem;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(242, 142, 28, 0.35);
  transition: transform 0.2s ease;
}

.captcha-display:hover {
  transform: translateY(-1px);
}

.form-error {
  margin: 0;
  padding: 0.75rem 1rem;
  border-radius: 0.85rem;
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
  font-weight: 600;
}

.submit-button {
  appearance: none;
  border: none;
  border-radius: 0.9rem;
  padding: 0.85rem 1rem;
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  cursor: pointer;
  box-shadow: 0 20px 35px rgba(242, 142, 28, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 24px 40px rgba(242, 142, 28, 0.28);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.login-hint {
  background: rgba(242, 142, 28, 0.06);
  border-radius: 1rem;
  padding: 1.5rem;
  color: #4b5563;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.login-hint h2 {
  margin: 0;
  font-size: 1.15rem;
  color: #9a3412;
}

.login-hint ul {
  margin: 0;
  padding-left: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.login-hint code {
  background: rgba(15, 23, 42, 0.08);
  padding: 0.1rem 0.35rem;
  border-radius: 0.45rem;
  font-family: 'Fira Code', ui-monospace, SFMono-Regular, SFMono, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
    monospace;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

@media (max-width: 768px) {
  .login-card {
    padding: 2rem 1.5rem;
  }

  .captcha-field {
    flex-direction: column;
    align-items: stretch;
  }

  .captcha-display {
    width: 100%;
  }
}

@media (prefers-color-scheme: dark) {
  .login-card {
    background: rgba(17, 24, 39, 0.8);
    box-shadow: 0 25px 55px rgba(0, 0, 0, 0.35);
    backdrop-filter: blur(12px);
  }

  .login-header h1,
  .login-subtitle,
  .field-label,
  .login-hint,
  input[type='text'],
  input[type='password'] {
    color: rgba(255, 255, 255, 0.85);
  }

  input[type='text'],
  input[type='password'] {
    background: rgba(31, 41, 55, 0.65);
    border-color: rgba(148, 163, 184, 0.35);
  }

  .login-hint {
    background: rgba(242, 177, 66, 0.12);
  }
}
</style>