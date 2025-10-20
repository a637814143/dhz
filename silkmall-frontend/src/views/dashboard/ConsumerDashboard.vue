<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type {
  Announcement,
  CartItem,
  HomepageContent,
  OrderDetail,
  OrderItemDetail,
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
  realName?: string | null
  idCard?: string | null
  avatar?: string | null
  points?: number | null
  membershipLevel?: string | null
}

const { state } = useAuthState()

const profile = ref<ConsumerProfile | null>(null)
const orders = ref<OrderSummary[]>([])
const homeContent = ref<HomepageContent | null>(null)
const announcements = ref<Announcement[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const walletBalance = ref<number | null>(null)
const cartItems = ref<CartItem[]>([])
const cartLoading = ref(false)
const cartError = ref<string | null>(null)
const removingCartItemId = ref<number | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)

const activeOrder = ref<OrderSummary | null>(null)
const orderDetail = ref<OrderDetail | null>(null)
const orderDetailLoading = ref(false)
const orderDetailError = ref<string | null>(null)

const showOrderDetailModal = ref(false)
const showReturnModal = ref(false)
const showEditModal = ref(false)
const showProfileModal = ref(false)

const returnSubmitting = ref(false)
const returnMessage = ref<string | null>(null)
const returnError = ref<string | null>(null)
const selectedReturnItemId = ref<number | null>(null)
const returnForm = reactive({
  reason: '',
})

const updateSubmitting = ref(false)
const updateMessage = ref<string | null>(null)
const updateError = ref<string | null>(null)
const orderUpdateForm = reactive({
  shippingAddress: '',
  recipientName: '',
  recipientPhone: '',
})

const profileForm = reactive({
  username: '',
  realName: '',
  idCard: '',
  email: '',
  phone: '',
  address: '',
})

const profileSubmitting = ref(false)
const profileFormMessage = ref<string | null>(null)
const profileFormError = ref<string | null>(null)

const orderItems = computed<OrderItemDetail[]>(() => orderDetail.value?.orderItems ?? [])
const hasRecommendations = computed(() => (homeContent.value?.recommendations?.length ?? 0) > 0)
const hasAnnouncements = computed(() => announcements.value.length > 0)
const maskedIdCard = computed(() => maskIdCard(profile.value?.idCard))
const hasCartItems = computed(() => cartItems.value.length > 0)
const cartTotalQuantity = computed(() =>
  cartItems.value.reduce((total, item) => total + (item.quantity ?? 0), 0)
)
const cartTotalAmount = computed(() =>
  cartItems.value.reduce(
    (total, item) => total + (item.subtotal ?? item.unitPrice * item.quantity),
    0
  )
)

async function loadProfile() {
  if (!state.user) return
  const { data } = await api.get<ConsumerProfile>(`/consumers/${state.user.id}`)
  profile.value = data
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

async function loadCart() {
  if (!state.user) {
    cartItems.value = []
    return
  }
  cartLoading.value = true
  cartError.value = null
  try {
    const { data } = await api.get<CartItem[]>('/cart')
    cartItems.value = data ?? []
  } catch (err) {
    cartItems.value = []
    cartError.value = err instanceof Error ? err.message : '加载购物车失败'
  } finally {
    cartLoading.value = false
  }
}

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([loadProfile(), loadOrders(), loadHomeContent(), loadWallet(), loadCart()])
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

function membershipBadge(level?: string | null) {
  if (!level) return '普通会员'
  return level
}

function maskIdCard(value?: string | null) {
  if (!value) return '—'
  const normalized = value.trim()
  if (!normalized) return '—'
  if (normalized.length <= 4) {
    return '*'.repeat(normalized.length)
  }

  const prefixLength = Math.min(3, normalized.length - 4)
  const prefix = normalized.slice(0, prefixLength)
  const maskedLength = Math.max(0, normalized.length - prefixLength)
  return `${prefix}${'*'.repeat(maskedLength)}`
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

async function removeCartItem(item: CartItem) {
  if (!item?.id) {
    return
  }
  cartError.value = null
  removingCartItemId.value = item.id
  try {
    await api.delete(`/cart/${item.id}`)
    cartItems.value = cartItems.value.filter((entry) => entry.id !== item.id)
  } catch (err) {
    cartError.value = err instanceof Error ? err.message : '移除购物车商品失败'
  } finally {
    removingCartItemId.value = null
  }
}

function syncOrderSummary(detail: OrderDetail) {
  const index = orders.value.findIndex((item) => item.id === detail.id)
  if (index !== -1) {
    orders.value[index] = {
      ...orders.value[index],
      status: detail.status,
      totalAmount: detail.totalAmount,
      totalQuantity: detail.totalQuantity,
      orderTime: detail.orderTime,
    }
  }
}

async function fetchOrderDetail(orderId: number) {
  orderDetailLoading.value = true
  orderDetailError.value = null
  try {
    const { data } = await api.get<OrderDetail>(`/orders/${orderId}/detail`)
    orderDetail.value = data
    syncOrderSummary(data)
    return data
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载订单详情失败'
    orderDetailError.value = message
    orderDetail.value = null
    return null
  } finally {
    orderDetailLoading.value = false
  }
}

async function ensureOrderDetail(order: OrderSummary) {
  if (orderDetail.value && orderDetail.value.id === order.id) {
    return orderDetail.value
  }
  return fetchOrderDetail(order.id)
}

function closeOrderDetailModal() {
  showOrderDetailModal.value = false
}

function closeReturnModal() {
  showReturnModal.value = false
  returnSubmitting.value = false
  returnMessage.value = null
  returnError.value = null
  returnForm.reason = ''
  selectedReturnItemId.value = null
}

function closeEditModal() {
  showEditModal.value = false
  updateSubmitting.value = false
  updateMessage.value = null
  updateError.value = null
}

function prepareReturnItemSelection(detail: OrderDetail | null) {
  if (!detail || !detail.orderItems.length) {
    selectedReturnItemId.value = null
    return
  }
  const existing = detail.orderItems.find((item) => item.id === selectedReturnItemId.value)
  selectedReturnItemId.value = existing?.id ?? detail.orderItems[0]?.id ?? null
}

function fillUpdateForm(detail: OrderDetail | null) {
  orderUpdateForm.shippingAddress = detail?.shippingAddress ?? ''
  orderUpdateForm.recipientName = detail?.recipientName ?? ''
  orderUpdateForm.recipientPhone = detail?.recipientPhone ?? ''
}

function openProfileDialog() {
  const current = profile.value
  profileForm.username = current?.username ?? state.user?.username ?? ''
  profileForm.realName = current?.realName ?? ''
  profileForm.idCard = current?.idCard ?? ''
  profileForm.email = current?.email ?? ''
  profileForm.phone = current?.phone ?? ''
  profileForm.address = current?.address ?? ''
  profileFormMessage.value = null
  profileFormError.value = null
  showProfileModal.value = true
}

function closeProfileDialog() {
  showProfileModal.value = false
  profileSubmitting.value = false
  profileFormMessage.value = null
  profileFormError.value = null
}

async function submitProfileUpdate() {
  if (!state.user) {
    profileFormError.value = '请先登录消费者账号'
    return
  }

  const realName = profileForm.realName.trim()
  if (!realName) {
    profileFormError.value = '请填写真实姓名'
    return
  }

  const idCard = profileForm.idCard.trim()
  if (!idCard) {
    profileFormError.value = '请填写身份证号'
    return
  }

  const username = (profileForm.username || state.user.username).trim()
  if (!username) {
    profileFormError.value = '账号信息缺失，请联系管理员'
    return
  }

  const payload = {
    id: state.user.id,
    username,
    realName,
    idCard,
    email: profileForm.email.trim() || null,
    phone: profileForm.phone.trim() || null,
    address: profileForm.address.trim() || null,
    avatar: profile.value?.avatar ?? null,
  }

  profileSubmitting.value = true
  profileFormError.value = null
  profileFormMessage.value = null
  try {
    const { data } = await api.put<ConsumerProfile>(`/consumers/${state.user.id}`, payload)
    profile.value = data
    profileFormMessage.value = '基础信息已更新'
    const authUser = state.user
    if (authUser) {
      state.user = {
        ...authUser,
        username: data.username ?? authUser.username,
        email: data.email ?? null,
        phone: data.phone ?? null,
      }
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新基础信息失败'
    profileFormError.value = message
  } finally {
    profileSubmitting.value = false
  }
}

async function openOrderDetail(order: OrderSummary) {
  activeOrder.value = order
  const detail = await ensureOrderDetail(order)
  if (!detail) {
    showOrderDetailModal.value = true
    return
  }
  showOrderDetailModal.value = true
}

async function openReturnDialog(order: OrderSummary) {
  activeOrder.value = order
  returnMessage.value = null
  returnError.value = null
  const detail = await ensureOrderDetail(order)
  if (!detail) {
    returnError.value = orderDetailError.value ?? '加载订单详情失败'
    prepareReturnItemSelection(null)
    showReturnModal.value = true
    return
  }
  prepareReturnItemSelection(detail)
  showReturnModal.value = true
}

async function openEditDialog(order: OrderSummary) {
  activeOrder.value = order
  updateMessage.value = null
  updateError.value = null
  const detail = await ensureOrderDetail(order)
  if (!detail) {
    updateError.value = orderDetailError.value ?? '加载订单详情失败'
    fillUpdateForm(null)
    showEditModal.value = true
    return
  }
  fillUpdateForm(detail)
  showEditModal.value = true
}

async function submitReturnRequest() {
  if (!selectedReturnItemId.value) {
    returnError.value = '请选择需要退货的商品'
    return
  }
  const reason = returnForm.reason.trim()
  if (!reason) {
    returnError.value = '请填写退货原因'
    return
  }
  returnSubmitting.value = true
  returnError.value = null
  returnMessage.value = null
  try {
    await api.post(`/returns/order-items/${selectedReturnItemId.value}`, { reason })
    returnMessage.value = '退货申请已提交，请等待客服处理'
    returnForm.reason = ''
  } catch (err) {
    const message = err instanceof Error ? err.message : '提交退货申请失败'
    returnError.value = message
  } finally {
    returnSubmitting.value = false
  }
}

async function submitOrderUpdate() {
  if (!orderDetail.value) return
  updateMessage.value = null
  updateError.value = null
  const payload = {
    shippingAddress: orderUpdateForm.shippingAddress.trim(),
    recipientName: orderUpdateForm.recipientName.trim(),
    recipientPhone: orderUpdateForm.recipientPhone.trim(),
  }
  if (!payload.recipientName) {
    updateError.value = '请填写收货人姓名'
    return
  }
  if (!payload.recipientPhone) {
    updateError.value = '请填写联系电话'
    return
  }
  if (!payload.shippingAddress) {
    updateError.value = '请填写收货地址'
    return
  }
  updateSubmitting.value = true
  try {
    const { data } = await api.put<OrderDetail>(`/orders/${orderDetail.value.id}/contact`, payload)
    orderDetail.value = data
    syncOrderSummary(data)
    fillUpdateForm(data)
    updateMessage.value = '订单联系信息已更新'
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新订单信息失败'
    updateError.value = message
  } finally {
    updateSubmitting.value = false
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
        <section class="panel profile full-row table-panel" aria-labelledby="profile-title">
          <div class="panel-title-row">
            <div class="panel-title" id="profile-title">账户信息</div>
            <button type="button" class="panel-action-button" @click="openProfileDialog">
              编辑基础信息
            </button>
          </div>
          <div class="table-container">
            <table class="dashboard-table profile-table">
              <tbody>
                <tr>
                  <th scope="row">账号</th>
                  <td>{{ profile?.username ?? state.user?.username ?? '—' }}</td>
                </tr>
                <tr>
                  <th scope="row">真实姓名</th>
                  <td>{{ profile?.realName ?? '—' }}</td>
                </tr>
                <tr>
                  <th scope="row">身份证号</th>
                  <td>{{ maskedIdCard }}</td>
                </tr>
                <tr>
                  <th scope="row">邮箱</th>
                  <td>{{ profile?.email ?? '—' }}</td>
                </tr>
                <tr>
                  <th scope="row">联系电话</th>
                  <td>{{ profile?.phone ?? '—' }}</td>
                </tr>
                <tr>
                  <th scope="row">收货地址</th>
                  <td>{{ profile?.address ?? '尚未填写' }}</td>
                </tr>
                <tr>
                  <th scope="row">会员等级</th>
                  <td>{{ membershipBadge(profile?.membershipLevel) }}</td>
                </tr>
                <tr>
                  <th scope="row">积分</th>
                  <td>{{ profile?.points ?? 0 }}</td>
                </tr>
                <tr>
                  <th scope="row">钱包余额</th>
                  <td>{{ formatCurrency(walletBalance ?? 0) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
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

        <section class="panel cart full-row table-panel" aria-labelledby="cart-title">
          <div class="panel-title-row">
            <div class="panel-title" id="cart-title">我的购物车</div>
            <div class="panel-actions">
              <span v-if="hasCartItems" class="cart-summary">
                共 {{ cartTotalQuantity }} 件商品，小计 {{ formatCurrency(cartTotalAmount) }}
              </span>
              <button type="button" class="panel-action-button" @click="loadCart" :disabled="cartLoading">
                {{ cartLoading ? '刷新中…' : '刷新' }}
              </button>
            </div>
          </div>
          <p v-if="cartLoading" class="empty">购物车加载中…</p>
          <p v-else-if="cartError" class="empty is-error">{{ cartError }}</p>
          <div v-else-if="cartItems.length" class="table-container">
            <table class="dashboard-table cart-table">
              <thead>
                <tr>
                  <th scope="col" class="col-product">商品</th>
                  <th scope="col">单价</th>
                  <th scope="col">数量</th>
                  <th scope="col">小计</th>
                  <th scope="col" class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in cartItems" :key="item.id">
                  <td class="col-product">
                    <div class="cart-product">
                      <div class="image">
                        <img
                          v-if="item.product?.mainImage"
                          :src="item.product.mainImage"
                          :alt="item.product?.name ?? '商品图片'"
                          loading="lazy"
                        />
                        <span v-else class="placeholder">{{ item.product?.name?.charAt(0)?.toUpperCase() ?? '货' }}</span>
                      </div>
                      <div class="info">
                        <strong class="name">{{ item.product?.name ?? '未命名商品' }}</strong>
                        <span v-if="item.addedAt" class="added-time">加入时间：{{ formatDateTime(item.addedAt) }}</span>
                        <span
                          v-if="item.product?.status && item.product.status !== 'ON_SALE'"
                          class="status-tag"
                        >
                          状态：{{ item.product.status }}
                        </span>
                      </div>
                    </div>
                  </td>
                  <td>{{ formatCurrency(item.unitPrice) }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ formatCurrency(item.subtotal) }}</td>
                  <td class="actions-cell">
                    <RouterLink
                      v-if="item.product?.id"
                      class="link-button"
                      :to="`/product/${item.product.id}`"
                    >
                      查看商品
                    </RouterLink>
                    <span v-else class="text-muted">商品信息缺失</span>
                    <button
                      type="button"
                      class="link-button danger"
                      :disabled="removingCartItemId === item.id"
                      @click="removeCartItem(item)"
                    >
                      {{ removingCartItemId === item.id ? '移除中…' : '移除' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">购物车空空如也，快去产品中心挑选吧。</p>
        </section>

        <section class="panel orders full-row table-panel" aria-labelledby="orders-title">
          <div class="panel-title" id="orders-title">最近订单</div>
          <div v-if="orders.length" class="table-container">
            <table class="dashboard-table orders-table">
              <thead>
                <tr>
                  <th scope="col" class="col-order-no">订单编号</th>
                  <th scope="col">金额</th>
                  <th scope="col">数量</th>
                  <th scope="col">状态</th>
                  <th scope="col">下单时间</th>
                  <th scope="col" class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orders" :key="order.id">
                  <td class="col-order-no">{{ order.orderNo }}</td>
                  <td>{{ formatCurrency(order.totalAmount) }}</td>
                  <td>{{ order.totalQuantity }}</td>
                  <td><span class="status-pill">{{ order.status }}</span></td>
                  <td>{{ formatDateTime(order.orderTime) }}</td>
                  <td class="actions-cell">
                    <button type="button" class="link-button" @click="openOrderDetail(order)">
                      查看订单
                    </button>
                    <button type="button" class="link-button" @click="openReturnDialog(order)">
                      申请退货
                    </button>
                    <button type="button" class="link-button" @click="openEditDialog(order)">
                      更改信息
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">暂无订单记录，前往首页挑选心仪商品吧。</p>
        </section>

        <section class="panel recommendations full-row table-panel" aria-labelledby="recommend-title">
          <div class="panel-title" id="recommend-title">为您推荐</div>
          <div v-if="hasRecommendations" class="table-container">
            <table class="dashboard-table recommend-table">
              <thead>
                <tr>
                  <th scope="col">商品信息</th>
                  <th scope="col" class="col-price">价格</th>
                  <th scope="col" class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in homeContent?.recommendations ?? []" :key="item.id">
                  <td>
                    <div class="product-info">
                      <strong>{{ item.name }}</strong>
                      <p>{{ item.description ?? '优质蚕丝，严选供应链品质保障。' }}</p>
                    </div>
                  </td>
                  <td class="col-price">{{ formatCurrency(item.price) }}</td>
                  <td class="actions-cell">
                    <router-link class="link-button" :to="`/product/${item.id}`">查看详情</router-link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">暂无推荐商品，稍后再来看看吧。</p>
        </section>

        <section class="panel announcements full-row table-panel" aria-labelledby="announcement-title">
          <div class="panel-title" id="announcement-title">公告与资讯</div>
          <div v-if="hasAnnouncements" class="table-container">
            <table class="dashboard-table announcement-table">
              <thead>
                <tr>
                  <th scope="col">公告内容</th>
                  <th scope="col" class="col-time">发布时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in announcements" :key="item.id">
                  <td>
                    <div class="announcement-info">
                      <strong>{{ item.title }}</strong>
                      <p>{{ item.content }}</p>
                    </div>
                  </td>
                  <td class="col-time">{{ formatDateTime(item.publishedAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">暂无公告，敬请期待更多平台动态。</p>
        </section>
      </div>
    </template>

    <div v-if="showProfileModal" class="modal-backdrop" @click.self="closeProfileDialog">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="profile-edit-title">
        <header class="modal-header">
          <h3 id="profile-edit-title">编辑基础信息</h3>
          <button type="button" class="icon-button" @click="closeProfileDialog" aria-label="关闭">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitProfileUpdate">
          <label>
            <span>真实姓名</span>
            <input v-model="profileForm.realName" type="text" placeholder="请输入真实姓名" />
          </label>
          <label>
            <span>身份证号</span>
            <input v-model="profileForm.idCard" type="text" placeholder="请输入身份证号" />
          </label>
          <label>
            <span>邮箱</span>
            <input v-model="profileForm.email" type="email" placeholder="请输入常用邮箱" />
          </label>
          <label>
            <span>联系电话</span>
            <input v-model="profileForm.phone" type="tel" placeholder="请输入联系电话" />
          </label>
          <label>
            <span>收货地址</span>
            <input v-model="profileForm.address" type="text" placeholder="请输入默认收货地址" />
          </label>
          <p v-if="profileFormMessage" class="modal-success">{{ profileFormMessage }}</p>
          <p v-if="profileFormError" class="modal-error">{{ profileFormError }}</p>
          <footer class="modal-actions">
            <button type="submit" class="primary-button" :disabled="profileSubmitting">
              {{ profileSubmitting ? '保存中…' : '保存信息' }}
            </button>
            <button type="button" class="ghost-button" @click="closeProfileDialog" :disabled="profileSubmitting">
              取消
            </button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="showOrderDetailModal" class="modal-backdrop" @click.self="closeOrderDetailModal">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="order-detail-title"
      >
        <header class="modal-header">
          <h3 id="order-detail-title">订单详情</h3>
          <button type="button" class="icon-button" @click="closeOrderDetailModal" aria-label="关闭">
            ×
          </button>
        </header>
        <div class="modal-body">
          <p v-if="orderDetailLoading" class="modal-status">正在加载订单详情…</p>
          <p v-else-if="orderDetailError" class="modal-error">{{ orderDetailError }}</p>
          <template v-else-if="orderDetail">
            <dl class="order-meta">
              <div>
                <dt>订单编号</dt>
                <dd>{{ orderDetail.orderNo }}</dd>
              </div>
              <div>
                <dt>订单状态</dt>
                <dd><span class="status-pill">{{ orderDetail.status }}</span></dd>
              </div>
              <div>
                <dt>下单时间</dt>
                <dd>{{ formatDateTime(orderDetail.orderTime) }}</dd>
              </div>
              <div>
                <dt>支付时间</dt>
                <dd>{{ formatDateTime(orderDetail.paymentTime) }}</dd>
              </div>
              <div>
                <dt>发货时间</dt>
                <dd>{{ formatDateTime(orderDetail.shippingTime) }}</dd>
              </div>
              <div>
                <dt>收货时间</dt>
                <dd>{{ formatDateTime(orderDetail.deliveryTime) }}</dd>
              </div>
              <div>
                <dt>收货人</dt>
                <dd>{{ orderDetail.recipientName ?? '—' }}</dd>
              </div>
              <div>
                <dt>联系电话</dt>
                <dd>{{ orderDetail.recipientPhone ?? '—' }}</dd>
              </div>
              <div class="address-row">
                <dt>收货地址</dt>
                <dd>{{ orderDetail.shippingAddress ?? '—' }}</dd>
              </div>
            </dl>
            <div class="order-items">
              <h4>商品列表</h4>
              <ul>
                <li v-for="item in orderDetail.orderItems" :key="item.id">
                  <div class="item-info">
                    <strong>{{ item.product.name }}</strong>
                    <span>数量：{{ item.quantity }}</span>
                  </div>
                  <div class="item-prices">
                    <span>单价：{{ formatCurrency(item.unitPrice) }}</span>
                    <span>小计：{{ formatCurrency(item.totalPrice) }}</span>
                  </div>
                </li>
              </ul>
              <footer class="order-summary">
                共 {{ orderDetail.totalQuantity }} 件商品，合计
                <strong>{{ formatCurrency(orderDetail.totalAmount) }}</strong>
              </footer>
            </div>
          </template>
          <p v-else class="modal-status">暂无可显示的订单信息</p>
        </div>
        <footer class="modal-actions">
          <button type="button" class="ghost-button" @click="closeOrderDetailModal">关闭</button>
        </footer>
      </section>
    </div>

    <div v-if="showReturnModal" class="modal-backdrop" @click.self="closeReturnModal">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="return-title">
        <header class="modal-header">
          <h3 id="return-title">申请退货</h3>
          <button type="button" class="icon-button" @click="closeReturnModal" aria-label="关闭">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitReturnRequest">
          <label class="field">
            <span>选择退货商品</span>
            <select
              v-model.number="selectedReturnItemId"
              :disabled="returnSubmitting || !orderItems.length"
            >
              <option v-if="!orderItems.length" disabled value="">
                当前订单暂无可退货的商品
              </option>
              <option v-for="item in orderItems" :key="item.id" :value="item.id">
                {{ item.product.name }}（数量：{{ item.quantity }}）
              </option>
            </select>
          </label>
          <label class="field">
            <span>退货原因</span>
            <textarea
              v-model="returnForm.reason"
              rows="4"
              placeholder="请简要说明退货原因"
              :disabled="returnSubmitting"
            ></textarea>
          </label>
          <p v-if="returnMessage" class="modal-success">{{ returnMessage }}</p>
          <p v-if="returnError" class="modal-error">{{ returnError }}</p>
          <footer class="modal-actions">
            <button type="submit" class="primary-button" :disabled="returnSubmitting">
              {{ returnSubmitting ? '提交中…' : '提交申请' }}
            </button>
            <button type="button" class="ghost-button" @click="closeReturnModal" :disabled="returnSubmitting">
              取消
            </button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="showEditModal" class="modal-backdrop" @click.self="closeEditModal">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="edit-title">
        <header class="modal-header">
          <h3 id="edit-title">更改联系信息</h3>
          <button type="button" class="icon-button" @click="closeEditModal" aria-label="关闭">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitOrderUpdate">
          <label class="field">
            <span>收货人姓名</span>
            <input
              v-model="orderUpdateForm.recipientName"
              type="text"
              placeholder="请输入收货人姓名"
              :disabled="updateSubmitting"
            />
          </label>
          <label class="field">
            <span>联系电话</span>
            <input
              v-model="orderUpdateForm.recipientPhone"
              type="tel"
              placeholder="请输入联系电话"
              :disabled="updateSubmitting"
            />
          </label>
          <label class="field">
            <span>收货地址</span>
            <textarea
              v-model="orderUpdateForm.shippingAddress"
              rows="3"
              placeholder="请输入详细收货地址"
              :disabled="updateSubmitting"
            ></textarea>
          </label>
          <p v-if="updateMessage" class="modal-success">{{ updateMessage }}</p>
          <p v-if="updateError" class="modal-error">{{ updateError }}</p>
          <footer class="modal-actions">
            <button type="submit" class="primary-button" :disabled="updateSubmitting">
              {{ updateSubmitting ? '保存中…' : '保存信息' }}
            </button>
            <button type="button" class="ghost-button" @click="closeEditModal" :disabled="updateSubmitting">
              取消
            </button>
          </footer>
        </form>
      </section>
    </div>
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
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  align-items: stretch;
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

.full-row {
  grid-column: 1 / -1;
}

.table-panel {
  gap: 1.75rem;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.panel-action-button {
  border: 1px solid rgba(79, 70, 229, 0.24);
  background: rgba(79, 70, 229, 0.08);
  color: #4f46e5;
  border-radius: 999px;
  padding: 0.5rem 1.2rem;
  font-weight: 600;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.panel-action-button:hover {
  background: rgba(79, 70, 229, 0.14);
  color: #312e81;
  transform: translateY(-1px);
}

.panel-action-button:focus-visible {
  outline: 2px solid rgba(79, 70, 229, 0.45);
  outline-offset: 2px;
}

.panel-action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.panel-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: rgba(17, 24, 39, 0.78);
}

.table-container {
  border-radius: 18px;
  border: 1px solid rgba(79, 70, 229, 0.12);
  overflow: hidden;
  overflow-x: auto;
  background: rgba(248, 250, 252, 0.68);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.dashboard-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
  min-width: 100%;
}

.dashboard-table th,
.dashboard-table td {
  padding: 0.85rem 1rem;
  text-align: left;
}

.dashboard-table thead {
  background: rgba(79, 70, 229, 0.08);
  color: rgba(17, 24, 39, 0.62);
}

.dashboard-table tbody tr {
  background: rgba(255, 255, 255, 0.92);
}

.dashboard-table tbody tr:nth-child(odd) {
  background: rgba(79, 70, 229, 0.06);
}

.dashboard-table tbody tr + tr {
  border-top: 1px solid rgba(79, 70, 229, 0.08);
}

.profile-table th {
  width: 160px;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.6);
}

.profile-table td {
  color: rgba(17, 24, 39, 0.85);
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

.orders-table .col-order-no {
  max-width: 22rem;
  font-size: 0.96rem;
  line-height: 1.45;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.dashboard-table .col-actions {
  text-align: right;
  width: 160px;
}

.cart-summary {
  color: rgba(17, 24, 39, 0.6);
  font-size: 0.9rem;
}

.cart-table .col-product {
  width: 45%;
}

.cart-product {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.cart-product .image {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.18), rgba(111, 169, 173, 0.2));
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.cart-product .image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-product .image .placeholder {
  font-weight: 700;
  color: rgba(17, 24, 39, 0.55);
}

.cart-product .info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.cart-product .name {
  font-weight: 600;
  color: #111827;
}

.cart-product .added-time {
  font-size: 0.75rem;
  color: rgba(17, 24, 39, 0.55);
}

.cart-product .status-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.2);
  color: #b45309;
  font-size: 0.75rem;
  width: fit-content;
}

.text-muted {
  color: rgba(17, 24, 39, 0.45);
  font-size: 0.85rem;
}

.actions-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  justify-content: flex-end;
}

.link-button {
  display: inline-flex;
  align-items: center;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  border: 1px solid rgba(79, 70, 229, 0.25);
  background: rgba(79, 70, 229, 0.08);
  color: #4338ca;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.link-button.danger {
  border-color: rgba(248, 113, 113, 0.3);
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
}

.link-button.danger:hover {
  background: rgba(248, 113, 113, 0.2);
  color: #991b1b;
  transform: translateY(-1px);
}

.link-button:hover:not(.danger) {
  background: rgba(79, 70, 229, 0.18);
  transform: translateY(-1px);
}

.link-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.recommend-table .product-info {
  display: grid;
  gap: 0.35rem;
}

.recommend-table .product-info strong {
  font-weight: 700;
  color: rgba(17, 24, 39, 0.85);
}

.recommend-table .product-info p {
  color: rgba(17, 24, 39, 0.6);
  font-size: 0.9rem;
  line-height: 1.55;
}

.recommend-table .col-price {
  font-weight: 600;
  color: #16a34a;
  white-space: nowrap;
  text-align: right;
}

.announcement-table .announcement-info {
  display: grid;
  gap: 0.35rem;
}

.announcement-table .announcement-info strong {
  font-weight: 700;
  color: rgba(17, 24, 39, 0.78);
}

.announcement-table .announcement-info p {
  color: rgba(17, 24, 39, 0.6);
  line-height: 1.5;
}

.announcement-table .col-time {
  white-space: nowrap;
  color: rgba(17, 24, 39, 0.55);
  text-align: right;
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

.empty.is-error {
  color: #b91c1c;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  padding: 1.5rem;
  z-index: 40;
}

.modal {
  width: min(640px, 100%);
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
}

.modal-body {
  padding: 1.5rem;
  display: grid;
  gap: 1rem;
  overflow-y: auto;
}

.modal-actions {
  padding: 1rem 1.5rem 1.5rem;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.icon-button {
  border: none;
  background: transparent;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  color: rgba(15, 23, 42, 0.55);
  transition: color 0.2s ease;
}

.icon-button:hover {
  color: rgba(79, 70, 229, 0.9);
}

.primary-button {
  padding: 0.6rem 1.4rem;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #6366f1, #ec4899);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.primary-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 15px 25px rgba(99, 102, 241, 0.35);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.ghost-button {
  padding: 0.55rem 1.3rem;
  border-radius: 999px;
  border: 1px solid rgba(79, 70, 229, 0.35);
  background: transparent;
  color: #4338ca;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.ghost-button:hover {
  background: rgba(79, 70, 229, 0.1);
}

.ghost-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-status {
  color: rgba(17, 24, 39, 0.6);
  text-align: center;
}

.modal-error {
  color: #b91c1c;
}

.modal-success {
  color: #15803d;
}

.order-meta {
  display: grid;
  gap: 0.8rem;
}

.order-meta div {
  display: grid;
  gap: 0.25rem;
  grid-template-columns: 110px 1fr;
}

.order-meta dt {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.6);
}

.order-meta dd {
  margin: 0;
  color: rgba(17, 24, 39, 0.85);
}

.order-meta .address-row {
  grid-template-columns: 110px 1fr;
}

.order-items {
  display: grid;
  gap: 0.75rem;
  border-top: 1px solid rgba(17, 24, 39, 0.08);
  padding-top: 1rem;
}

.order-items h4 {
  margin: 0;
  font-weight: 700;
}

.order-items ul {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 0.75rem;
}

.order-items li {
  padding: 0.75rem 0.9rem;
  border-radius: 14px;
  background: rgba(79, 70, 229, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.item-info {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.item-info strong {
  font-weight: 700;
}

.item-prices {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  color: rgba(17, 24, 39, 0.68);
  font-size: 0.9rem;
}

.order-summary {
  display: flex;
  justify-content: flex-end;
  gap: 0.35rem;
  color: rgba(17, 24, 39, 0.68);
  font-weight: 600;
}

.field {
  display: grid;
  gap: 0.35rem;
}

.field span {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.65);
}

.field input,
.field textarea,
.field select {
  border: 1px solid rgba(17, 24, 39, 0.12);
  border-radius: 0.75rem;
  padding: 0.6rem 0.75rem;
  font-size: 0.95rem;
  resize: vertical;
}

.field select:disabled,
.field input:disabled,
.field textarea:disabled {
  background: rgba(148, 163, 184, 0.15);
  cursor: not-allowed;
}

.field textarea {
  min-height: 120px;
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
