<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type {
  Announcement,
  HomepageContent,
  PageResponse,
  ProductSummary,
} from '@/types'

interface OrderSummary {
  id: number
  orderNo: string
  totalAmount: number
  totalQuantity: number
  status: string
  orderTime: string
}

interface ConsumerProfile {
  id: number
  username: string
  email?: string | null
  phone?: string | null
  address?: string | null
  avatar?: string | null
  points?: number | null
  membershipLevel?: number | string | null
  enabled?: boolean | null
}

const { state } = useAuthState()

const profile = ref<ConsumerProfile | null>(null)
const orders = ref<OrderSummary[]>([])
const homeContent = ref<HomepageContent | null>(null)
const announcements = ref<Announcement[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const walletBalance = ref<number | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)

const editingProfile = ref(false)
const profileSaving = ref(false)
const profileFormError = ref<string | null>(null)
const profileFeedback = ref<string | null>(null)

const profileForm = reactive({
  email: '',
  phone: '',
  address: '',
})

function resetProfileForm() {
  profileForm.email = ''
  profileForm.phone = ''
  profileForm.address = ''
  profileFormError.value = null
}

function populateProfileForm(source: ConsumerProfile) {
  profileForm.email = source.email ?? ''
  profileForm.phone = source.phone ?? ''
  profileForm.address = source.address ?? ''
  profileFormError.value = null
}

function openProfileEditor() {
  if (!profile.value) {
    return
  }
  populateProfileForm(profile.value)
  profileFeedback.value = null
  editingProfile.value = true
}

function cancelProfileEdit() {
  editingProfile.value = false
  profileFormError.value = null
  if (profile.value) {
    populateProfileForm(profile.value)
  } else {
    resetProfileForm()
  }
}

async function saveProfile() {
  if (!state.user) {
    profileFormError.value = '请先登录账号'
    return
  }
  if (!profile.value) {
    profileFormError.value = '暂无账户信息可更新'
    return
  }

  const email = profileForm.email.trim()
  const phone = profileForm.phone.trim()
  const address = profileForm.address.trim()

  profileSaving.value = true
  profileFormError.value = null

  const payload = {
    id: profile.value.id,
    username: profile.value.username,
    email: email || null,
    phone: phone || null,
    address: address || null,
    avatar: profile.value.avatar ?? null,
    points: profile.value.points ?? undefined,
    membershipLevel: profile.value.membershipLevel ?? undefined,
    enabled: profile.value.enabled ?? undefined,
  }

  try {
    const { data } = await api.put<ConsumerProfile>(`/consumers/${profile.value.id}`, payload)
    profile.value = data
    populateProfileForm(data)
    editingProfile.value = false
    profileFeedback.value = '账户信息已更新'
    if (state.user) {
      Object.assign(state.user, {
        email: data.email ?? null,
        phone: data.phone ?? null,
      })
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新失败，请稍后重试'
    profileFormError.value = message
  } finally {
    profileSaving.value = false
  }
}

async function loadProfile() {
  if (!state.user) return
  const { data } = await api.get<ConsumerProfile>(`/consumers/${state.user.id}`)
  profile.value = data
  if (editingProfile.value) {
    populateProfileForm(data)
  }
}

async function loadOrders() {
  if (!state.user) return
  const { data } = await api.get<PageResponse<OrderSummary>>(`/orders/consumer/${state.user.id}`, {
    params: { page: 0, size: 5 },
  })
  orders.value = data.content ?? []
}

async function loadHomeContent() {
  const { data } = await api.get<HomepageContent>('/content/home')
  homeContent.value = data
  announcements.value = data.announcements
}

async function loadWallet() {
  if (!state.user) return
  try {
    const { data } = await api.get<{ balance: number }>('/wallet')
    walletBalance.value = data.balance
  } catch (err) {
    console.warn('加载钱包信息失败', err)
    walletBalance.value = null
  }
}

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([loadProfile(), loadOrders(), loadHomeContent(), loadWallet()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

function formatCurrency(amount?: number | null) {
  if (typeof amount !== 'number' || Number.isNaN(amount)) return '¥0.00'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(amount)
}

function formatDateTime(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function membershipBadge(level?: number | string | null) {
  if (level === null || level === undefined) return '普通会员'
  if (typeof level === 'number') {
    switch (level) {
      case 1:
        return '普通会员'
      case 2:
        return '银卡会员'
      case 3:
        return '金卡会员'
      case 4:
        return '钻石会员'
      default:
        return `会员等级 ${level}`
    }
  }
  const numeric = Number(level)
  if (!Number.isNaN(numeric)) {
    return membershipBadge(numeric)
  }
  return String(level)
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
    walletBalance.value = data.balance
    redeemCodeInput.value = ''
    redeemMessage.value = '兑换成功，余额已更新'
  } catch (err) {
    const message = err instanceof Error ? err.message : '兑换失败'
    redeemError.value = message
  } finally {
    redeeming.value = false
  }
}

const shortcutLinks = [
  { label: '快速下单', href: '/?quick=true' },
  { label: '我的评价', href: '#reviews' },
  { label: '地址管理', href: '#address' },
]
</script>

<template>
  <section class="dashboard-shell">
    <header class="dashboard-header">
      <div class="headline">
        <h1>您好，{{ state.user?.username ?? '尊敬的用户' }}</h1>
        <p>欢迎回到您的采购工作台，以下是今日为您精选的动态与提醒。</p>
      </div>
      <div class="shortcuts">
        <a v-for="link in shortcutLinks" :key="link.label" :href="link.href">{{ link.label }}</a>
      </div>
    </header>

    <div v-if="loading" class="placeholder">正在加载数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
      <div class="grid">
        <section class="panel profile" aria-labelledby="profile-title">
          <div class="panel-title-row">
            <div class="panel-title" id="profile-title">账户信息</div>
            <button v-if="!editingProfile" type="button" class="link-button" @click="openProfileEditor">
              编辑账户信息
            </button>
          </div>

          <p v-if="profileFeedback" class="form-feedback is-success">{{ profileFeedback }}</p>

          <form v-if="editingProfile" class="profile-form" @submit.prevent="saveProfile">
            <div class="grid">
              <label>
                <span>邮箱</span>
                <input v-model="profileForm.email" type="email" placeholder="用于接收通知" />
              </label>
              <label>
                <span>联系电话</span>
                <input v-model="profileForm.phone" type="tel" placeholder="便于联系您" />
              </label>
            </div>
            <label>
              <span>收货地址</span>
              <textarea v-model="profileForm.address" rows="3" placeholder="请输入常用收货地址"></textarea>
            </label>
            <p v-if="profileFormError" class="form-feedback is-error">{{ profileFormError }}</p>
            <div class="profile-form-actions">
              <button type="button" class="ghost" @click="cancelProfileEdit" :disabled="profileSaving">
                取消
              </button>
              <button type="submit" class="primary" :disabled="profileSaving">
                {{ profileSaving ? '保存中…' : '保存信息' }}
              </button>
            </div>
          </form>

          <template v-else>
            <ul class="profile-list">
              <li>
                <span>邮箱</span>
                <strong>{{ profile?.email ?? '—' }}</strong>
              </li>
              <li>
                <span>联系电话</span>
                <strong>{{ profile?.phone ?? '—' }}</strong>
              </li>
              <li>
                <span>收货地址</span>
                <strong>{{ profile?.address ?? '尚未填写' }}</strong>
              </li>
              <li>
                <span>会员等级</span>
                <strong>{{ membershipBadge(profile?.membershipLevel) }}</strong>
              </li>
              <li>
                <span>积分</span>
                <strong>{{ profile?.points ?? 0 }}</strong>
              </li>
              <li>
                <span>钱包余额</span>
                <strong>{{ formatCurrency(walletBalance ?? 0) }}</strong>
              </li>
            </ul>
          </template>

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

        <section class="panel orders" aria-labelledby="orders-title">
          <div class="panel-title" id="orders-title">最近订单</div>
          <table v-if="orders.length" class="orders-table">
            <thead>
              <tr>
                <th scope="col">订单编号</th>
                <th scope="col">金额</th>
                <th scope="col">数量</th>
                <th scope="col">状态</th>
                <th scope="col">下单时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orders" :key="order.id">
                <td>{{ order.orderNo }}</td>
                <td>{{ formatCurrency(order.totalAmount) }}</td>
                <td>{{ order.totalQuantity }}</td>
                <td><span class="status-pill">{{ order.status }}</span></td>
                <td>{{ formatDateTime(order.orderTime) }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty">暂无订单记录，前往首页挑选心仪商品吧。</p>
        </section>

        <section class="panel recommendations" aria-labelledby="recommend-title">
          <div class="panel-title" id="recommend-title">为您推荐</div>
          <div class="product-grid">
            <article v-for="item in homeContent?.recommendations ?? []" :key="item.id" class="product-card">
              <div class="product-meta">
                <h3>{{ item.name }}</h3>
                <p>{{ item.description ?? '优质蚕丝，严选供应链品质保障。' }}</p>
              </div>
              <footer>
                <span class="price">{{ formatCurrency(item.price) }}</span>
                <router-link :to="`/product/${item.id}`">查看详情</router-link>
              </footer>
            </article>
          </div>
        </section>

        <section class="panel announcements" aria-labelledby="announcement-title">
          <div class="panel-title" id="announcement-title">公告与资讯</div>
          <ul class="announcement-list">
            <li v-for="item in announcements" :key="item.id">
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.content }}</p>
              </div>
              <time>{{ formatDateTime(item.publishedAt) }}</time>
            </li>
          </ul>
        </section>
      </div>
    </template>
  </section>
</template>

<style scoped>
.dashboard-shell {
  display: grid;
  gap: 2.5rem;
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 2rem;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12), rgba(236, 72, 153, 0.08));
  padding: 2.25rem;
  border-radius: 24px;
}

.headline h1 {
  font-size: 2.1rem;
  font-weight: 700;
  margin-bottom: 0.6rem;
}

.headline p {
  color: rgba(17, 24, 39, 0.7);
}

.shortcuts {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.shortcuts a {
  padding: 0.65rem 1.2rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  color: #4f46e5;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.12);
}

.placeholder {
  padding: 2rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  text-align: center;
  color: rgba(17, 24, 39, 0.6);
}

.placeholder.is-error {
  background: rgba(248, 113, 113, 0.12);
  color: #7f1d1d;
}

.grid {
  display: grid;
  gap: 2rem;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.panel {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 20px;
  padding: 1.8rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  box-shadow: 0 20px 40px rgba(31, 41, 55, 0.08);
}

.panel-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: rgba(17, 24, 39, 0.78);
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.link-button {
  border: none;
  background: none;
  color: #4f46e5;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.link-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.profile-list {
  list-style: none;
  display: grid;
  gap: 0.85rem;
  padding: 0;
}

.profile-form {
  display: grid;
  gap: 1.25rem;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
}

.profile-form .grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.profile-form label {
  display: grid;
  gap: 0.45rem;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.75);
}

.profile-form input,
.profile-form textarea {
  padding: 0.65rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(17, 24, 39, 0.12);
  font-size: 0.95rem;
  background: rgba(255, 255, 255, 0.96);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.profile-form input:focus,
.profile-form textarea:focus {
  outline: none;
  border-color: rgba(79, 70, 229, 0.45);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.16);
}

.profile-form textarea {
  resize: vertical;
  min-height: 96px;
}

.profile-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.profile-form-actions .ghost,
.profile-form-actions .primary {
  padding: 0.6rem 1.2rem;
  border-radius: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, opacity 0.2s ease;
}

.profile-form-actions .ghost {
  border: 1px solid rgba(17, 24, 39, 0.18);
  background: rgba(255, 255, 255, 0.85);
  color: rgba(17, 24, 39, 0.78);
}

.profile-form-actions .ghost:hover {
  background: rgba(17, 24, 39, 0.08);
}

.profile-form-actions .primary {
  border: none;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.25);
}

.profile-form-actions .primary:hover {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
}

.profile-form-actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

.form-feedback {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.form-feedback.is-error {
  color: #b91c1c;
}

.form-feedback.is-success {
  color: #15803d;
}

.profile-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: rgba(17, 24, 39, 0.6);
}

.profile-list strong {
  color: rgba(17, 24, 39, 0.85);
}

.redeem-box {
  display: grid;
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
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

.orders-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.orders-table th,
.orders-table td {
  padding: 0.75rem 0.5rem;
  text-align: left;
}

.orders-table thead {
  color: rgba(17, 24, 39, 0.55);
}

.orders-table tbody tr:nth-child(odd) {
  background: rgba(79, 70, 229, 0.06);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.12);
  color: #4338ca;
  font-weight: 600;
  font-size: 0.85rem;
}

.empty {
  color: rgba(17, 24, 39, 0.55);
}

.product-grid {
  display: grid;
  gap: 1.2rem;
}

.product-card {
  border-radius: 16px;
  border: 1px solid rgba(79, 70, 229, 0.15);
  padding: 1.2rem;
  display: grid;
  gap: 1rem;
  background: rgba(255, 255, 255, 0.95);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(79, 70, 229, 0.18);
}

.product-card h3 {
  font-size: 1.05rem;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.85);
}

.product-card p {
  color: rgba(17, 24, 39, 0.58);
  font-size: 0.92rem;
}

.product-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.product-card .price {
  color: #16a34a;
}

.product-card a {
  color: #4f46e5;
  text-decoration: none;
}

.announcement-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1.1rem;
}

.announcement-list li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.announcement-list strong {
  display: block;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.announcement-list p {
  color: rgba(17, 24, 39, 0.6);
}

.announcement-list time {
  color: rgba(17, 24, 39, 0.45);
  font-size: 0.85rem;
}

@media (max-width: 900px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .shortcuts {
    width: 100%;
  }
}
</style>
