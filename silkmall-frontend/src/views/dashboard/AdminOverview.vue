<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type { AdminProfile } from '@/types'

const { state } = useAuthState()

const walletBalance = ref<number | null>(null)
const walletLoading = ref(false)
const walletError = ref<string | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)
const walletDisplay = computed(() =>
  typeof walletBalance.value === 'number' ? formatCurrency(walletBalance.value) : '—'
)

const profile = reactive<AdminProfile>({
  id: 0,
  username: '',
  email: '',
  phone: '',
  address: '',
  department: '',
  position: '',
})
const profileSnapshot = ref<AdminProfile | null>(null)
const profileLoading = ref(false)
const profileSaving = ref(false)
const profileError = ref<string | null>(null)
const profileMessage = ref<string | null>(null)

const pageLoading = ref(true)
const pageError = ref<string | null>(null)

function applyProfile(data: Partial<AdminProfile>) {
  profile.id = Number(data.id) || profile.id
  profile.username = data.username ?? ''
  profile.email = data.email ?? ''
  profile.phone = data.phone ?? ''
  profile.address = data.address ?? ''
  profile.department = data.department ?? ''
  profile.position = data.position ?? ''
  profileSnapshot.value = { ...profile }
}

async function loadWallet() {
  if (!state.user) {
    walletBalance.value = null
    walletError.value = '请先登录管理员账号'
    return
  }
  walletLoading.value = true
  walletError.value = null
  try {
    const { data } = await api.get<{ balance: number }>('/wallet')
    walletBalance.value = typeof data?.balance === 'number' ? data.balance : null
  } catch (err) {
    walletBalance.value = null
    walletError.value = err instanceof Error ? err.message : '加载钱包信息失败'
  } finally {
    walletLoading.value = false
  }
}

async function redeemWallet() {
  redeemMessage.value = null
  redeemError.value = null
  const code = redeemCodeInput.value.trim()
  if (!code) {
    redeemError.value = '请输入兑换码'
    return
  }
  redeeming.value = true
  try {
    const { data } = await api.post<{ balance: number }>('/wallet/redeem', { code })
    const balance = typeof data?.balance === 'number' ? data.balance : null
    walletBalance.value = balance
    redeemMessage.value = '兑换成功，余额已更新'
    redeemCodeInput.value = ''
    walletError.value = null
  } catch (err) {
    redeemError.value = err instanceof Error ? err.message : '兑换失败，请稍后再试'
  } finally {
    redeeming.value = false
  }
}

async function loadAdminProfile() {
  if (!state.user?.id) {
    profileError.value = '缺少管理员信息，请重新登录'
    return
  }
  profileLoading.value = true
  profileError.value = null
  try {
    const { data } = await api.get<AdminProfile>(`/admins/${state.user.id}`)
    applyProfile(data ?? {})
  } catch (err) {
    profileError.value = err instanceof Error ? err.message : '加载管理员信息失败'
  } finally {
    profileLoading.value = false
  }
}

async function saveAdminProfile() {
  if (!state.user?.id) {
    profileError.value = '请先登录管理员账号'
    return
  }
  if (!profile.username.trim()) {
    profileError.value = '请输入账号名称'
    return
  }
  profileSaving.value = true
  profileMessage.value = null
  profileError.value = null
  try {
    const payload: AdminProfile = {
      id: profile.id,
      username: profile.username.trim(),
      email: profile.email?.trim() || '',
      phone: profile.phone?.trim() || '',
      address: profile.address?.trim() || '',
      department: profile.department?.trim() || '',
      position: profile.position?.trim() || '',
    }
    const { data } = await api.put<AdminProfile>(`/admins/${state.user.id}`, payload)
    applyProfile(data ?? payload)
    profileMessage.value = '基础信息已更新'
    if (state.user) {
      state.user = {
        ...state.user,
        username: profile.username,
        email: profile.email ?? null,
        phone: profile.phone ?? null,
        address: profile.address ?? null,
      }
    }
  } catch (err) {
    profileError.value = err instanceof Error ? err.message : '更新管理员信息失败'
  } finally {
    profileSaving.value = false
  }
}

function resetAdminProfile() {
  if (!profileSnapshot.value) return
  applyProfile(profileSnapshot.value)
  profileMessage.value = null
  profileError.value = null
}

async function bootstrap() {
  pageLoading.value = true
  pageError.value = null
  try {
    await Promise.all([loadWallet(), loadAdminProfile()])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '加载管理员中心失败'
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

function formatCurrency(amount?: number | string | null) {
  const numeric = typeof amount === 'string' ? Number(amount) : amount
  if (typeof numeric !== 'number' || Number.isNaN(numeric)) {
    return '¥0.00'
  }
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(numeric)
}
</script>

<template>
  <section class="admin-shell">
    <header class="admin-header">
      <div>
        <h1>平台运营总览</h1>
        <p>掌握资金与账号信息，及时调整平台运营策略。</p>
      </div>
    </header>

    <div v-if="pageLoading" class="placeholder">正在加载管理员信息…</div>
    <div v-else-if="pageError" class="placeholder is-error">{{ pageError }}</div>
    <template v-else>
      <section class="panel admin-wallet" aria-labelledby="wallet-title">
        <header class="wallet-header">
          <div>
            <div class="panel-title" id="wallet-title">平台钱包</div>
            <p class="panel-subtitle">查看结算储备与平台收益，支持使用兑换码快速充值。</p>
          </div>
          <button type="button" class="secondary" @click="loadWallet" :disabled="walletLoading">
            {{ walletLoading ? '刷新中…' : '刷新余额' }}
          </button>
        </header>
        <div class="wallet-summary">
          <div class="wallet-amount">{{ walletDisplay }}</div>
          <p class="wallet-caption">当前余额</p>
        </div>
        <p v-if="walletError" class="wallet-error">{{ walletError }}</p>
        <p v-else-if="walletLoading" class="wallet-status">余额刷新中…</p>
        <div class="redeem-box">
          <label>
            <span>兑换码</span>
            <input
              v-model="redeemCodeInput"
              type="text"
              placeholder="输入兑换码兑换余额"
              :disabled="redeeming"
            />
          </label>
          <div class="redeem-actions">
            <button type="button" @click="redeemWallet" :disabled="redeeming">
              {{ redeeming ? '兑换中…' : '兑换余额' }}
            </button>
            <p v-if="redeemMessage" class="redeem-success">{{ redeemMessage }}</p>
            <p v-if="redeemError" class="redeem-error">{{ redeemError }}</p>
          </div>
        </div>
      </section>

      <section class="panel admin-profile" aria-labelledby="admin-profile-title">
        <header class="profile-header">
          <div>
            <div class="panel-title" id="admin-profile-title">管理员基础信息</div>
            <p class="panel-subtitle">查看并维护您的账号资料，确保联系方式最新。</p>
          </div>
          <div class="profile-actions">
            <button type="button" class="secondary" :disabled="profileLoading" @click="resetAdminProfile">
              重置
            </button>
            <button type="button" class="primary" :disabled="profileSaving || profileLoading" @click="saveAdminProfile">
              {{ profileSaving ? '保存中…' : '保存信息' }}
            </button>
          </div>
        </header>

        <p v-if="profileLoading" class="feedback">基础信息加载中…</p>
        <p v-else-if="profileError" class="feedback feedback--error">{{ profileError }}</p>
        <template v-else>
          <transition name="fade">
            <p v-if="profileMessage" class="feedback feedback--success">{{ profileMessage }}</p>
          </transition>
          <div class="profile-grid">
            <label>
              <span>账号名称</span>
              <input v-model="profile.username" type="text" placeholder="请输入账号名称" />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="profile.email" type="email" placeholder="请输入邮箱" />
            </label>
            <label>
              <span>联系电话</span>
              <input v-model="profile.phone" type="tel" placeholder="请输入联系电话" />
            </label>
            <label>
              <span>联系地址</span>
              <input v-model="profile.address" type="text" placeholder="请输入联系地址" />
            </label>
            <label>
              <span>所属部门</span>
              <input v-model="profile.department" type="text" placeholder="请输入所属部门" />
            </label>
            <label>
              <span>职位</span>
              <input v-model="profile.position" type="text" placeholder="请输入职位" />
            </label>
          </div>
        </template>
      </section>
    </template>
  </section>
</template>

<style scoped>
.admin-shell {
  display: grid;
  gap: 2rem;
}

.admin-header {
  padding: 2.2rem;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.12), rgba(234, 179, 8, 0.12));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

.admin-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.35rem;
}

.admin-header p {
  color: rgba(30, 41, 59, 0.65);
  margin: 0;
}

.placeholder {
  padding: 2rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  text-align: center;
  color: rgba(30, 41, 59, 0.6);
}

.placeholder.is-error {
  background: rgba(248, 113, 113, 0.12);
  color: #7f1d1d;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 1.8rem;
  box-shadow: 0 20px 40px rgba(30, 41, 59, 0.08);
  display: grid;
  gap: 1.25rem;
}

.panel-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: rgba(30, 41, 59, 0.78);
}

.panel-subtitle {
  margin-top: 0.35rem;
  color: rgba(30, 41, 59, 0.6);
}

.admin-wallet {
  gap: 1.25rem;
}

.wallet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.wallet-header .panel-subtitle {
  margin: 0.35rem 0 0;
  color: rgba(30, 41, 59, 0.58);
}

.wallet-summary {
  display: grid;
  gap: 0.35rem;
}

.wallet-amount {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
}

.wallet-caption {
  margin: 0;
  color: rgba(30, 41, 59, 0.6);
}

.wallet-status {
  margin: 0;
  color: rgba(37, 99, 235, 0.75);
  font-weight: 500;
}

.wallet-error {
  margin: 0;
  color: #b91c1c;
  font-weight: 600;
}

.redeem-box {
  display: grid;
  gap: 0.85rem;
  padding: 1.2rem 1.3rem;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(99, 102, 241, 0.08));
}

.redeem-box label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
}

.redeem-box input {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(17, 24, 39, 0.12);
  font-size: 0.95rem;
}

.redeem-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.redeem-actions button {
  padding: 0.55rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.redeem-actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.redeem-success {
  color: #15803d;
}

.redeem-error {
  color: #b91c1c;
}

.admin-profile {
  gap: 1.2rem;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
}

.profile-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-items: center;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1rem;
}

.profile-grid label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
  color: rgba(30, 41, 59, 0.78);
}

.profile-grid input {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.feedback {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  font-weight: 600;
  margin: 0;
}

.feedback--success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.feedback--error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.primary {
  padding: 0.55rem 1.4rem;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.primary:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.28);
}

.secondary {
  padding: 0.55rem 1.4rem;
  border-radius: 999px;
  border: 1px solid rgba(30, 41, 59, 0.18);
  background: rgba(226, 232, 240, 0.4);
  color: rgba(30, 41, 59, 0.75);
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 720px) {
  .admin-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .wallet-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
