<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

type UserRole = 'customer' | 'vendor' | 'admin'

type RoleOption = {
  value: UserRole
  label: string
  subtitle: string
  highlight: string
}

type UserAccount = {
  id: string
  name: string
  username: string
  organization: string
  description: string
}

const roleOptions: RoleOption[] = [
  {
    value: 'customer',
    label: '消费者',
    subtitle: '快速选购、查看订单',
    highlight: '适用于终端用户',
  },
  {
    value: 'vendor',
    label: '供货商',
    subtitle: '管理商品、同步库存',
    highlight: '适用于桑蚕企业与合作社',
  },
  {
    value: 'admin',
    label: '平台管理员',
    subtitle: '配置系统、监控经营',
    highlight: '适用于平台运维人员',
  },
]

const multiAccounts: Record<UserRole, UserAccount[]> = {
  customer: [
    {
      id: 'customer-01',
      name: '李华',
      username: 'lihua88',
      organization: '丝路良品体验店',
      description: '偏好高端丝绸被品类',
    },
    {
      id: 'customer-02',
      name: '王敏',
      username: 'wangmin',
      organization: '线下门店会员账号',
      description: '已绑定线下会员卡，可同步积分',
    },
  ],
  vendor: [
    {
      id: 'vendor-01',
      name: '陈强',
      username: 'cq_factory',
      organization: '苏州瑞蚕织造厂',
      description: '负责丝绸成品的采购计划',
    },
    {
      id: 'vendor-02',
      name: '赵莉',
      username: 'zhaoli_ops',
      organization: '江南蚕茧合作社',
      description: '常用账号：可查看原料库存和批次溯源',
    },
  ],
  admin: [
    {
      id: 'admin-01',
      name: '平台值班',
      username: 'admin-duty',
      organization: 'SilkMall 数字运营中心',
      description: '具备核心运营、订单审批权限',
    },
    {
      id: 'admin-02',
      name: '风控专员',
      username: 'risk-control',
      organization: 'SilkMall 运营安全组',
      description: '具备数据看板、风控策略管理权限',
    },
  ],
}

const formState = reactive({
  role: roleOptions[0].value as UserRole,
  username: multiAccounts[roleOptions[0].value][0].username,
  password: '',
  remember: true,
})

const selectedAccountId = ref(multiAccounts[formState.role][0].id)
const isSubmitting = ref(false)
const feedback = ref<{ type: 'success' | 'error' | 'info'; text: string } | null>(null)

const activeRoleOption = computed(() =>
  roleOptions.find((role) => role.value === formState.role) ?? roleOptions[0],
)

const activeAccounts = computed(() => multiAccounts[formState.role])

const activeAccount = computed(() =>
  activeAccounts.value.find((account) => account.id === selectedAccountId.value) ??
  activeAccounts.value[0],
)

watch(
  () => formState.role,
  (newRole) => {
    const [firstAccount] = multiAccounts[newRole]
    selectedAccountId.value = firstAccount?.id ?? ''
    formState.username = firstAccount?.username ?? ''
    formState.password = ''
    feedback.value = null
  },
)

watch(selectedAccountId, (id) => {
  const targetAccount = activeAccounts.value.find((account) => account.id === id)
  if (targetAccount) {
    formState.username = targetAccount.username
    feedback.value = null
  }
})

const handleSubmit = async () => {
  if (!formState.username || !formState.password) {
    feedback.value = {
      type: 'error',
      text: '请输入完整的账号和密码信息。',
    }
    return
  }

  isSubmitting.value = true
  feedback.value = {
    type: 'info',
    text: '正在验证身份，请稍候……',
  }

  await new Promise((resolve) => setTimeout(resolve, 800))

  feedback.value = {
    type: 'success',
    text: `${activeRoleOption.value.label}【${activeAccount.value.name}】登录成功，正在同步账号数据……`,
  }

  isSubmitting.value = false
}
</script>

<template>
  <section class="login-section">
    <header class="login-header">
      <h1>多角色统一登录中心</h1>
      <p>
        为蚕桑产业链的不同角色提供协同支持。请选择您的身份并完成登录，即刻开启丝绸商品的智慧化运营体验。
      </p>
    </header>

    <div class="login-layout">
      <form class="login-card" @submit.prevent="handleSubmit">
        <div class="role-switcher" role="tablist" aria-label="选择登录身份">
          <button
            v-for="role in roleOptions"
            :key="role.value"
            type="button"
            class="role-tab"
            :class="{ active: role.value === formState.role }"
            role="tab"
            :aria-selected="role.value === formState.role"
            @click="formState.role = role.value"
          >
            <span class="role-title">{{ role.label }}</span>
            <small>{{ role.subtitle }}</small>
          </button>
        </div>

        <div class="form-group">
          <label class="form-label" for="account-selector">快捷选择账号</label>
          <div class="account-list" id="account-selector">
            <button
              v-for="account in activeAccounts"
              :key="account.id"
              type="button"
              class="account-item"
              :class="{ active: account.id === selectedAccountId }"
              @click="selectedAccountId = account.id"
            >
              <div class="account-meta">
                <strong>{{ account.name }}</strong>
                <span>{{ account.organization }}</span>
              </div>
              <p>{{ account.description }}</p>
            </button>
          </div>
        </div>

        <div class="form-fields">
          <div class="field">
            <label for="username">登录账号</label>
            <input
              id="username"
              v-model="formState.username"
              type="text"
              autocomplete="username"
              placeholder="请输入用户名或手机号"
              required
            />
          </div>

          <div class="field">
            <label for="password">密码</label>
            <input
              id="password"
              v-model="formState.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              required
            />
          </div>
        </div>

        <div class="form-footer">
          <label class="remember">
            <input v-model="formState.remember" type="checkbox" />
            14 天内免登录
          </label>
          <a class="link" href="#">忘记密码？</a>
        </div>

        <button class="submit" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '正在登录...' : '立即登录' }}
        </button>

        <p v-if="feedback" class="feedback" :class="feedback.type">{{ feedback.text }}</p>
      </form>

      <aside class="login-aside" aria-live="polite">
        <h2>{{ activeRoleOption.label }}专属提示</h2>
        <ul>
          <li>{{ activeRoleOption.highlight }}</li>
          <li>当前选择账号：{{ activeAccount.name }}（{{ activeAccount.organization }}）</li>
          <li>支持扫码、短信验证码等企业级二次验证方式</li>
          <li>如需新增账号，请联系平台管理员或拨打 400-123-4588</li>
        </ul>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.login-section {
  display: grid;
  gap: 2rem;
  padding: 2rem 0 3rem;
}

.login-header h1 {
  font-size: clamp(1.75rem, 2vw + 1rem, 2.5rem);
  margin-bottom: 0.75rem;
}

.login-header p {
  max-width: 48rem;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.7;
}

.login-layout {
  display: grid;
  gap: 2rem;
  grid-template-columns: minmax(0, 2.25fr) minmax(0, 1fr);
  align-items: start;
}

.login-card {
  display: grid;
  gap: 1.5rem;
  padding: 2rem;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 25px 65px rgba(26, 26, 26, 0.08);
  backdrop-filter: blur(10px);
}

.role-switcher {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
  border-radius: 1.25rem;
  padding: 0.5rem;
  background: rgba(242, 142, 28, 0.08);
}

.role-tab {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.85rem 1rem;
  border-radius: 1rem;
  border: none;
  background: transparent;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
  cursor: pointer;
}

.role-tab .role-title {
  font-size: 1rem;
  font-weight: 600;
}

.role-tab small {
  font-size: 0.8rem;
  color: rgba(0, 0, 0, 0.55);
}

.role-tab.active {
  background: linear-gradient(120deg, rgba(242, 142, 28, 0.85), rgba(245, 195, 66, 0.75));
  color: #3a220d;
  transform: translateY(-2px);
  box-shadow: 0 15px 35px rgba(242, 142, 28, 0.35);
}

.account-list {
  display: grid;
  gap: 0.75rem;
}

.account-item {
  display: grid;
  gap: 0.35rem;
  padding: 0.85rem 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(242, 142, 28, 0.18);
  background: rgba(255, 255, 255, 0.75);
  text-align: left;
  cursor: pointer;
  transition: border 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.account-item:hover {
  border-color: rgba(242, 142, 28, 0.5);
  background: rgba(255, 248, 240, 0.9);
}

.account-item.active {
  border-color: rgba(242, 142, 28, 0.9);
  box-shadow: 0 12px 35px rgba(242, 142, 28, 0.25);
}

.account-meta {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.account-meta strong {
  font-size: 1rem;
}

.account-meta span {
  font-size: 0.85rem;
  color: rgba(0, 0, 0, 0.55);
}

.account-item p {
  margin: 0;
  font-size: 0.8rem;
  color: rgba(0, 0, 0, 0.5);
}

.form-fields {
  display: grid;
  gap: 1rem;
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field label {
  font-size: 0.9rem;
  font-weight: 600;
}

.field input {
  padding: 0.75rem 1rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.9);
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus {
  outline: none;
  border-color: rgba(242, 142, 28, 0.6);
  box-shadow: 0 0 0 3px rgba(242, 142, 28, 0.2);
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.85rem;
  color: rgba(0, 0, 0, 0.65);
}

.remember {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.link {
  color: #f28e1c;
}

.submit {
  padding: 0.85rem 1.2rem;
  border-radius: 1rem;
  border: none;
  background: linear-gradient(120deg, #f28e1c, #f5c342);
  color: #4a270e;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.submit:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 35px rgba(242, 142, 28, 0.35);
}

.feedback {
  padding: 0.75rem 1rem;
  border-radius: 0.85rem;
  font-size: 0.9rem;
}

.feedback.info {
  background: rgba(80, 120, 180, 0.12);
  color: #2d4f9d;
}

.feedback.success {
  background: rgba(73, 160, 120, 0.12);
  color: #2e6b4f;
}

.feedback.error {
  background: rgba(220, 80, 80, 0.12);
  color: #a22626;
}

.login-aside {
  display: grid;
  gap: 1rem;
  padding: 1.75rem;
  border-radius: 1.5rem;
  background: linear-gradient(150deg, rgba(243, 207, 155, 0.95), rgba(201, 234, 232, 0.85));
  box-shadow: 0 25px 60px rgba(42, 82, 103, 0.15);
  color: #3b2e1f;
}

.login-aside h2 {
  font-size: 1.25rem;
}

.login-aside ul {
  display: grid;
  gap: 0.75rem;
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .login-layout {
    grid-template-columns: 1fr;
  }

  .login-aside {
    order: -1;
  }
}

@media (prefers-color-scheme: dark) {
  .login-card {
    background: rgba(30, 30, 32, 0.85);
    box-shadow: 0 25px 65px rgba(10, 10, 10, 0.8);
  }

  .role-switcher {
    background: rgba(242, 177, 66, 0.16);
  }

  .role-tab small {
    color: rgba(255, 255, 255, 0.65);
  }

  .account-item {
    background: rgba(24, 24, 28, 0.8);
    border-color: rgba(242, 177, 66, 0.15);
  }

  .account-item p {
    color: rgba(255, 255, 255, 0.55);
  }

  .field input {
    background: rgba(28, 28, 32, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
    color: #f7f7f7;
  }

  .form-footer,
  .login-header p {
    color: rgba(255, 255, 255, 0.75);
  }

  .login-aside {
    background: linear-gradient(150deg, rgba(70, 54, 38, 0.9), rgba(25, 57, 66, 0.85));
    color: rgba(255, 255, 255, 0.9);
  }
}
</style>
