<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type {
  Announcement,
  CartItem,
  ConsumerAddress,
  HomepageContent,
  OrderDetail,
  OrderItemDetail,
  PageResponse,
  ConsumerFavorite,
  CartCheckoutResult,
  ProductReview,
  ProductSummary,
} from '@/types'

interface OrderSummary {
  id: number
  orderNo: string
  totalAmount: number
  totalQuantity: number
  status: string
  orderTime: string
  productNames?: string[]
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
const orderActionMessage = ref<string | null>(null)
const orderActionError = ref<string | null>(null)
const confirmingReceiptOrderId = ref<number | null>(null)
const homeContent = ref<HomepageContent | null>(null)
const announcements = ref<Announcement[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const walletBalance = ref<number | null>(null)
const cartItems = ref<CartItem[]>([])
const cartLoading = ref(false)
const cartError = ref<string | null>(null)
const removingCartItemId = ref<number | null>(null)
const cartSelectionEnabled = ref(false)
const selectedCartItemIds = ref<Set<number>>(new Set())
const showCartCheckoutModal = ref(false)
const cartCheckoutSubmitting = ref(false)
const cartCheckoutError = ref<string | null>(null)
const cartCheckoutMessage = ref<string | null>(null)
const favorites = ref<ConsumerFavorite[]>([])
const favoritesLoading = ref(false)
const favoritesError = ref<string | null>(null)
const favoriteActionMessage = ref<string | null>(null)
const favoriteActionError = ref<string | null>(null)
const removingFavoriteId = ref<number | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)

const activeOrder = ref<OrderSummary | null>(null)
const orderDetail = ref<OrderDetail | null>(null)
const orderDetailLoading = ref(false)
const orderDetailError = ref<string | null>(null)

const showOrderDetailModal = ref(false)
const showPaymentModal = ref(false)
const showReturnModal = ref(false)
const showEditModal = ref(false)
const showProfileModal = ref(false)

const myReviews = ref<ProductReview[]>([])
const reviewsLoading = ref(false)
const reviewsError = ref<string | null>(null)
const reviewListMessage = ref<string | null>(null)
const reviewListError = ref<string | null>(null)

const orderReviews = ref<ProductReview[]>([])
const reviewMessage = ref<string | null>(null)
const reviewError = ref<string | null>(null)
const activeReviewItemId = ref<number | null>(null)
const editingReviewId = ref<number | null>(null)
const submittingReview = ref(false)
const reviewForm = reactive({
  rating: 5,
  comment: '',
})

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

const paymentOrder = ref<OrderSummary | null>(null)
const paymentMethod = ref('WECHAT')
const paymentSubmitting = ref(false)
const paymentError = ref<string | null>(null)

const addresses = ref<ConsumerAddress[]>([])
const addressLoading = ref(false)
const addressError = ref<string | null>(null)
const addressActionError = ref<string | null>(null)
const addressMessage = ref<string | null>(null)
const showAddressModal = ref(false)
const addressSubmitting = ref(false)
const addressFormError = ref<string | null>(null)
const editingAddressId = ref<number | null>(null)
const deletingAddressId = ref<number | null>(null)
const addressForm = reactive({
  recipientName: '',
  recipientPhone: '',
  shippingAddress: '',
  isDefault: false,
})

const orderItems = computed<OrderItemDetail[]>(() => orderDetail.value?.orderItems ?? [])
const hasRecommendations = computed(() => (homeContent.value?.recommendations?.length ?? 0) > 0)
const hasAnnouncements = computed(() => announcements.value.length > 0)
const maskedIdCard = computed(() => maskIdCard(profile.value?.idCard))
const defaultAddress = computed(() => addresses.value.find((item) => item.isDefault))
const hasAddresses = computed(() => addresses.value.length > 0)
const hasCartItems = computed(() => cartItems.value.length > 0)
const hasCartSelection = computed(() => selectedCartItemIds.value.size > 0)
const selectedCartItems = computed(() =>
  cartItems.value.filter((item) => selectedCartItemIds.value.has(item.id))
)
const selectedCartTotalAmount = computed(() =>
  selectedCartItems.value.reduce((total, item) => total + (item.subtotal ?? 0), 0)
)
const hasFavorites = computed(() => favorites.value.length > 0)
const cartTotalQuantity = computed(() =>
  cartItems.value.reduce((total, item) => total + (item.quantity ?? 0), 0)
)
const cartTotalAmount = computed(() =>
  cartItems.value.reduce(
    (total, item) => total + (item.subtotal ?? item.unitPrice * item.quantity),
    0
  )
)
const reviewMap = computed(() => {
  const map = new Map<number, ProductReview[]>()
  orderReviews.value.forEach((review) => {
    if (!map.has(review.orderItemId)) {
      map.set(review.orderItemId, [])
    }
    map.get(review.orderItemId)!.push(review)
  })
  map.forEach((entries) =>
    entries.sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  )
  return map
})
const hasReviews = computed(() => myReviews.value.length > 0)
const hasOrders = computed(() => orders.value.length > 0)

const normalizeStatusValue = (value: string) =>
  value.trim().replace(/[\s_-]+/g, '').toUpperCase()

const pendingPaymentStatusValues = [
  '待付款',
  '未支付',
  '待支付',
  'PENDING PAYMENT',
  'AWAITING PAYMENT',
  'UNPAID',
  'NOT PAID',
] as const

const pendingPaymentStatusSet = new Set(
  pendingPaymentStatusValues.map((value) => normalizeStatusValue(value))
)

const awaitingReceiptStatusValues = [
  '已发货',
  '运送中',
  '待收货',
  'SHIPPED',
  'IN TRANSIT',
  'AWAITING RECEIPT',
] as const

const deliveredStatusValues = [
  '已收货',
  'DELIVERED',
] as const

const editLockedStatusValues = [
  '已发货',
  '运送中',
  '待收货',
  '已收货',
  '已取消',
  '已撤销',
  'SHIPPED',
  'IN TRANSIT',
  'AWAITING RECEIPT',
  'DELIVERED',
  'CANCELLED',
  'REVOKED',
] as const

const awaitingReceiptStatusSet = new Set(
  awaitingReceiptStatusValues.map((value) => normalizeStatusValue(value))
)

const deliveredStatusSet = new Set(
  deliveredStatusValues.map((value) => normalizeStatusValue(value))
)

const editLockedStatusSet = new Set(
  editLockedStatusValues.map((value) => normalizeStatusValue(value))
)
const paymentOptions = [
  { value: 'WECHAT', label: '微信支付' },
  { value: 'ALIPAY', label: '支付宝' },
  { value: 'BANK', label: '银行转账' },
  { value: 'COD', label: '货到付款' },
]
const paymentLabelMap = paymentOptions.reduce<Record<string, string>>((map, option) => {
  map[option.value] = option.label
  return map
}, {})

function formatOrderProductNames(names?: readonly string[] | null): string {
  if (!names || names.length === 0) {
    return ''
  }

  const cleaned = names
    .map((name) => (typeof name === 'string' ? name.trim() : ''))
    .filter((name): name is string => name.length > 0)

  if (cleaned.length === 0) {
    return ''
  }

  const maxVisible = 3
  const displayed = cleaned.slice(0, maxVisible)
  let result = displayed.join('、')

  if (cleaned.length > maxVisible) {
    result += ' 等'
  }

  return result
}

function getOrderProductSummary(order: OrderSummary): string {
  return formatOrderProductNames(order.productNames)
}

function isStatusInSet(status: string | null | undefined, set: ReadonlySet<string>): boolean {
  if (!status) {
    return false
  }
  return set.has(normalizeStatusValue(status))
}

function canConfirmReceipt(order: OrderSummary): boolean {
  return isStatusInSet(order.status, awaitingReceiptStatusSet)
}

function canReviewOrder(order: OrderSummary): boolean {
  return isStatusInSet(order.status, deliveredStatusSet)
}

function canEditOrder(order: OrderSummary): boolean {
  return !isStatusInSet(order.status, editLockedStatusSet)
}

async function loadProfile() {
  if (!state.user) return
  const { data } = await api.get<ConsumerProfile>(`/consumers/${state.user.id}`)
  profile.value = data
}

async function loadOrders() {
  if (!state.user) {
    orders.value = []
    return
  }

  const size = 20
  const aggregated = new Map<number, OrderSummary>()
  let page = 0
  let totalPages = 1

  const toTimestamp = (value?: string | null) => {
    if (!value) return 0
    const parsed = new Date(value).getTime()
    return Number.isNaN(parsed) ? 0 : parsed
  }

  do {
    const { data } = await api.get<PageResponse<OrderSummary>>(
      `/orders/consumer/${state.user.id}`,
      {
        params: { page, size },
      }
    )

    const content = data?.content ?? []
    content.forEach((order) => {
      aggregated.set(order.id, order)
    })

    const reportedTotalPages = typeof data?.totalPages === 'number' ? data.totalPages : 0
    const inferredTotalPages =
      reportedTotalPages > 0
        ? reportedTotalPages
        : content.length < size
        ? page + 1
        : Math.max(page + 1, Math.ceil((data?.totalElements ?? content.length) / size))

    totalPages = Math.max(inferredTotalPages, page + 1)
    page += 1
  } while (page < totalPages)

  orders.value = Array.from(aggregated.values()).sort(
    (a, b) => toTimestamp(b.orderTime) - toTimestamp(a.orderTime)
  )
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

async function loadReviews() {
  if (!state.user) {
    myReviews.value = []
    reviewsError.value = null
    reviewListMessage.value = null
    reviewListError.value = null
    return
  }
  reviewsLoading.value = true
  reviewsError.value = null
  reviewListMessage.value = null
  reviewListError.value = null
  try {
    const { data } = await api.get<ProductReview[]>(`/reviews/consumers/${state.user.id}`)
    myReviews.value = data ?? []
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载评价列表失败'
    reviewsError.value = message
    myReviews.value = []
  } finally {
    reviewsLoading.value = false
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
    syncSelectedCartItems()
  } catch (err) {
    cartItems.value = []
    cartError.value = err instanceof Error ? err.message : '加载购物车失败'
  } finally {
    cartLoading.value = false
  }
}

function syncSelectedCartItems() {
  const existingIds = new Set(cartItems.value.map((item) => item.id))
  selectedCartItemIds.value = new Set(
    Array.from(selectedCartItemIds.value).filter((id) => existingIds.has(id))
  )
  if (selectedCartItemIds.value.size === 0) {
    cartSelectionEnabled.value = false
  }
}

async function loadFavorites() {
  if (!state.user) {
    favorites.value = []
    return
  }
  favoritesLoading.value = true
  favoritesError.value = null
  try {
    const { data } = await api.get<ConsumerFavorite[]>('/favorites')
    favorites.value = data ?? []
  } catch (err) {
    favorites.value = []
    favoritesError.value = err instanceof Error ? err.message : '加载收藏失败'
  } finally {
    favoritesLoading.value = false
  }
}

async function removeFavorite(item: ConsumerFavorite) {
  if (!item?.id) return
  removingFavoriteId.value = item.id
  favoriteActionMessage.value = null
  favoriteActionError.value = null
  try {
    await api.delete(`/favorites/${item.id}`)
    favorites.value = favorites.value.filter((entry) => entry.id !== item.id)
    favoriteActionMessage.value = '已移除收藏'
  } catch (err) {
    const message = err instanceof Error ? err.message : '移除收藏失败'
    favoriteActionError.value = message
  } finally {
    removingFavoriteId.value = null
  }
}

async function loadAddresses() {
  if (!state.user) {
    addresses.value = []
    return
  }
  addressLoading.value = true
  addressError.value = null
  try {
    const { data } = await api.get<ConsumerAddress[]>(`/consumers/${state.user.id}/addresses`)
    const items = data ?? []
    addresses.value = items.map((item) => ({
      ...item,
      isDefault: Boolean(item.isDefault),
    }))
    addressActionError.value = null
  } catch (err) {
    addresses.value = []
    const message = err instanceof Error ? err.message : '加载地址列表失败'
    addressError.value = message
    addressActionError.value = message
  } finally {
    addressLoading.value = false
  }
}

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([
      loadProfile(),
      loadOrders(),
      loadHomeContent(),
      loadWallet(),
      loadCart(),
      loadFavorites(),
      loadReviews(),
      loadAddresses(),
    ])
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

function paymentLabel(method?: string | null) {
  if (!method) return ''
  return paymentLabelMap[method] ?? method
}

function isPendingPaymentStatus(status?: string | null) {
  if (!status) return false
  const normalized = normalizeStatusValue(status)
  return normalized.length > 0 && pendingPaymentStatusSet.has(normalized)
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

function fillAddressForm(address: ConsumerAddress | null) {
  addressForm.recipientName = address?.recipientName ?? ''
  addressForm.recipientPhone = address?.recipientPhone ?? ''
  addressForm.shippingAddress = address?.shippingAddress ?? ''
  addressForm.isDefault = address ? Boolean(address.isDefault) : !addresses.value.length
}

function openAddressModal(address?: ConsumerAddress) {
  if (addressSubmitting.value) return
  editingAddressId.value = address?.id ?? null
  addressFormError.value = null
  fillAddressForm(address ?? null)
  showAddressModal.value = true
}

function closeAddressModal() {
  if (addressSubmitting.value) return
  showAddressModal.value = false
  editingAddressId.value = null
  addressFormError.value = null
  fillAddressForm(null)
}

async function submitAddressForm() {
  if (!state.user) return
  const payload = {
    recipientName: addressForm.recipientName.trim(),
    recipientPhone: addressForm.recipientPhone.trim(),
    shippingAddress: addressForm.shippingAddress.trim(),
    isDefault: addressForm.isDefault,
  }
  if (!payload.recipientName) {
    addressFormError.value = '请填写收货人姓名'
    return
  }
  if (!payload.recipientPhone) {
    addressFormError.value = '请填写联系电话'
    return
  }
  if (!payload.shippingAddress) {
    addressFormError.value = '请填写收货地址'
    return
  }
  addressSubmitting.value = true
  addressFormError.value = null
  addressMessage.value = null
  try {
    if (editingAddressId.value) {
      await api.put(`/consumers/${state.user.id}/addresses/${editingAddressId.value}`, payload)
      addressMessage.value = '地址已更新'
    } else {
      await api.post(`/consumers/${state.user.id}/addresses`, payload)
      addressMessage.value = '地址已添加'
    }
    addressActionError.value = null
    closeAddressModal()
    await loadAddresses()
  } catch (err) {
    addressFormError.value = err instanceof Error ? err.message : '保存地址失败'
  } finally {
    addressSubmitting.value = false
  }
}

async function deleteAddress(address: ConsumerAddress) {
  if (!state.user) return
  const confirmed = window.confirm('确定删除该收货地址吗？')
  if (!confirmed) {
    return
  }
  deletingAddressId.value = address.id
  addressActionError.value = null
  addressMessage.value = null
  try {
    await api.delete(`/consumers/${state.user.id}/addresses/${address.id}`)
    addressMessage.value = '地址已删除'
    await loadAddresses()
  } catch (err) {
    addressActionError.value = err instanceof Error ? err.message : '删除地址失败'
  } finally {
    deletingAddressId.value = null
  }
}

async function setDefaultAddress(address: ConsumerAddress) {
  if (!state.user) return
  addressActionError.value = null
  addressMessage.value = null
  try {
    await api.post(`/consumers/${state.user.id}/addresses/${address.id}/default`)
    addressMessage.value = '默认地址已更新'
    await loadAddresses()
  } catch (err) {
    addressActionError.value = err instanceof Error ? err.message : '设置默认地址失败'
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
    selectedCartItemIds.value.delete(item.id)
    syncSelectedCartItems()
  } catch (err) {
    cartError.value = err instanceof Error ? err.message : '移除购物车商品失败'
  } finally {
    removingCartItemId.value = null
  }
}

function toggleCartSelection() {
  cartSelectionEnabled.value = !cartSelectionEnabled.value
  if (!cartSelectionEnabled.value) {
    selectedCartItemIds.value = new Set()
  }
}

function toggleCartItemChecked(item: CartItem) {
  if (!cartSelectionEnabled.value) return
  if (selectedCartItemIds.value.has(item.id)) {
    selectedCartItemIds.value.delete(item.id)
  } else {
    selectedCartItemIds.value.add(item.id)
  }
  selectedCartItemIds.value = new Set(selectedCartItemIds.value)
}

function openCartCheckout() {
  if (!hasCartSelection.value) {
    cartCheckoutError.value = '请先选择要结算的商品'
    return
  }
  cartCheckoutError.value = null
  cartCheckoutMessage.value = null
  showCartCheckoutModal.value = true
}

async function submitCartCheckout() {
  if (!hasCartSelection.value) {
    cartCheckoutError.value = '请先选择要结算的商品'
    return
  }
  cartCheckoutSubmitting.value = true
  cartCheckoutError.value = null
  cartCheckoutMessage.value = null
  try {
    const payload = { itemIds: Array.from(selectedCartItemIds.value) }
    const { data } = await api.post<CartCheckoutResult>('/cart/checkout', payload)
    cartCheckoutMessage.value = '支付成功，已扣除余额'
    if (typeof data?.balance === 'number') {
      walletBalance.value = data.balance
    }
    await loadCart()
    selectedCartItemIds.value = new Set()
    cartSelectionEnabled.value = false
    showCartCheckoutModal.value = false
  } catch (err) {
    cartCheckoutError.value = err instanceof Error ? err.message : '结算失败'
  } finally {
    cartCheckoutSubmitting.value = false
  }
}

function closeCartCheckoutModal() {
  showCartCheckoutModal.value = false
  cartCheckoutSubmitting.value = false
}

function toOrderSummary(detail: OrderDetail): OrderSummary {
  return {
    id: detail.id,
    orderNo: detail.orderNo,
    totalAmount: detail.totalAmount,
    totalQuantity: detail.totalQuantity,
    status: detail.status,
    orderTime: detail.orderTime,
  }
}

function syncOrderSummary(detail: OrderDetail) {
  const summary = toOrderSummary(detail)
  const index = orders.value.findIndex((item) => item.id === summary.id)
  if (index !== -1) {
    orders.value[index] = {
      ...orders.value[index],
      ...summary,
    }
  } else {
    orders.value.unshift(summary)
  }
}

async function fetchAndUpdateOrder(orderId: number) {
  const { data } = await api.get<OrderDetail>(`/orders/${orderId}/detail`)
  syncOrderSummary(data)
  if (orderDetail.value?.id === data.id) {
    orderDetail.value = data
  }
  if (activeOrder.value?.id === data.id) {
    activeOrder.value = toOrderSummary(data)
  }
  return data
}

async function fetchOrderDetail(orderId: number) {
  orderDetailLoading.value = true
  orderDetailError.value = null
  try {
    const { data } = await api.get<OrderDetail>(`/orders/${orderId}/detail`)
    orderDetail.value = data
    syncOrderSummary(data)
    activeOrder.value = toOrderSummary(data)
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

async function openPaymentDialog(order: OrderSummary) {
  paymentOrder.value = order
  paymentMethod.value = 'WECHAT'
  paymentError.value = null
  paymentSubmitting.value = false
  showPaymentModal.value = true
  try {
    const detail = await fetchAndUpdateOrder(order.id)
    paymentOrder.value = toOrderSummary(detail)
    if (detail.paymentMethod) {
      paymentMethod.value = detail.paymentMethod
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载订单支付信息失败'
    paymentError.value = message
  }
}

function closePaymentModal() {
  if (paymentSubmitting.value) {
    return
  }
  showPaymentModal.value = false
  paymentOrder.value = null
  paymentError.value = null
}

async function submitPayment() {
  if (!paymentOrder.value) return
  const method = paymentMethod.value?.trim()
  if (!method) {
    paymentError.value = '请选择支付方式'
    return
  }
  paymentError.value = null
  paymentSubmitting.value = true
  try {
    await api.put(`/orders/${paymentOrder.value.id}/pay`, null, {
      params: { paymentMethod: method },
    })
    await fetchAndUpdateOrder(paymentOrder.value.id)
    showPaymentModal.value = false
    paymentOrder.value = null
  } catch (err) {
    const message = err instanceof Error ? err.message : '支付失败，请稍后再试'
    paymentError.value = message
  } finally {
    paymentSubmitting.value = false
  }
}

function resetReviewContext() {
  reviewMessage.value = null
  reviewError.value = null
  activeReviewItemId.value = null
  editingReviewId.value = null
  submittingReview.value = false
}

async function fetchOrderReviews(orderId: number) {
  try {
    const { data } = await api.get<ProductReview[]>(`/reviews/orders/${orderId}`)
    orderReviews.value = (data ?? []).slice().sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
    return true
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载评价信息失败'
    orderReviews.value = []
    reviewError.value = message
    return false
  }
}

async function confirmReceipt(order: OrderSummary) {
  if (!canConfirmReceipt(order)) {
    return
  }
  if (confirmingReceiptOrderId.value !== null) {
    return
  }
  const confirmed = window.confirm('确认已收到商品吗？')
  if (!confirmed) {
    return
  }
  confirmingReceiptOrderId.value = order.id
  orderActionMessage.value = null
  orderActionError.value = null
  try {
    await api.put(`/orders/${order.id}/confirm`)
    await fetchAndUpdateOrder(order.id)
    orderActionMessage.value = '确认收货成功，感谢您的使用'
  } catch (err) {
    const message = err instanceof Error ? err.message : '确认收货失败'
    orderActionError.value = message
  } finally {
    confirmingReceiptOrderId.value = null
  }
}

function reviewRoleLabel(role?: string | null) {
  if (!role) return '评价人'
  switch (role.toUpperCase()) {
    case 'CONSUMER':
      return '消费者'
    case 'SUPPLIER':
      return '商家'
    case 'ADMIN':
      return '管理员'
    default:
      return role
  }
}

function canEditReview(review: ProductReview) {
  if (!state.user) return false
  const role = state.user.userType?.toUpperCase()
  return (
    !!role &&
    review.authorRole?.toUpperCase() === role &&
    review.authorId === state.user.id
  )
}

function canDeleteReview(review: ProductReview) {
  if (!state.user) return false
  if (state.user.userType === 'admin') {
    return true
  }
  return canEditReview(review)
}

function canCreateReviewForItem(item: OrderItemDetail) {
  if (state.user?.userType !== 'consumer') return false
  return !orderReviews.value.some(
    (review) =>
      review.orderItemId === item.id &&
      review.authorRole?.toUpperCase() === 'CONSUMER' &&
      review.authorId === state.user!.id
  )
}

function findEditableReviewForItem(itemId: number) {
  return (
    orderReviews.value.find(
      (review) => review.orderItemId === itemId && canEditReview(review)
    ) ?? null
  )
}

function startEditReviewForItem(itemId: number) {
  const review = findEditableReviewForItem(itemId)
  if (review) {
    startEditExistingReview(review)
  }
}

function openReviewForm(item: OrderItemDetail) {
  reviewMessage.value = null
  reviewError.value = null
  activeReviewItemId.value = item.id
  editingReviewId.value = null
  reviewForm.rating = 5
  reviewForm.comment = ''
}

function cancelReviewForm() {
  activeReviewItemId.value = null
  editingReviewId.value = null
  reviewForm.rating = 5
  reviewForm.comment = ''
  submittingReview.value = false
}

async function submitReview() {
  if (!activeReviewItemId.value) return
  reviewMessage.value = null
  reviewError.value = null

  const rating = Number(reviewForm.rating)
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
    reviewError.value = '请给出 1 - 5 分的评分'
    return
  }

  submittingReview.value = true
  const editing = !!editingReviewId.value
  try {
    const payload = {
      rating,
      comment: reviewForm.comment.trim() || null,
    }
    let saved: ProductReview
    if (editingReviewId.value) {
      const { data } = await api.put<ProductReview>(
        `/reviews/${editingReviewId.value}`,
        payload
      )
      saved = data
    } else {
      const { data } = await api.post<ProductReview>(
        `/reviews/order-items/${activeReviewItemId.value}`,
        payload
      )
      saved = data
    }
    upsertReview(saved)
    const orderId = orderDetail.value?.id
    let reloaded = true
    if (orderId) {
      reloaded = await fetchOrderReviews(orderId)
    }
    await loadReviews()
    if (reloaded) {
      reviewMessage.value = editing ? '评价已更新' : '评价提交成功'
    }
    cancelReviewForm()
  } catch (err) {
    const message = err instanceof Error ? err.message : '提交评价失败'
    reviewError.value = message
  } finally {
    submittingReview.value = false
  }
}

function startEditExistingReview(review: ProductReview) {
  reviewMessage.value = null
  reviewError.value = null
  activeReviewItemId.value = review.orderItemId
  editingReviewId.value = review.id
  reviewForm.rating = review.rating
  reviewForm.comment = review.comment ?? ''
}

async function deleteReview(review: ProductReview, origin: 'modal' | 'list' = 'modal') {
  if (origin === 'modal') {
    reviewMessage.value = null
    reviewError.value = null
  } else {
    reviewListMessage.value = null
    reviewListError.value = null
  }
  try {
    await api.delete(`/reviews/${review.id}`)
    const activeOrderId = orderDetail.value?.id
    let reloaded = true
    if (activeOrderId && activeOrderId === review.orderId) {
      reloaded = await fetchOrderReviews(activeOrderId)
    } else {
      orderReviews.value = orderReviews.value.filter((item) => item.id !== review.id)
    }
    if (editingReviewId.value === review.id) {
      cancelReviewForm()
    }
    await loadReviews()
    if (origin === 'modal') {
      if (reloaded) {
        reviewMessage.value = '评价已删除'
      }
    } else {
      reviewListMessage.value = '评价已删除'
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除评价失败'
    if (origin === 'modal') {
      reviewError.value = message
    } else {
      reviewListError.value = message
    }
  }
}

function upsertReview(review: ProductReview) {
  const index = orderReviews.value.findIndex((item) => item.id === review.id)
  if (index >= 0) {
    orderReviews.value.splice(index, 1, review)
  } else {
    orderReviews.value.push(review)
  }
  orderReviews.value.sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  )
}

async function openOrderDetail(order: OrderSummary) {
  activeOrder.value = order
  resetReviewContext()
  const detail = await ensureOrderDetail(order)
  if (detail) {
    await fetchOrderReviews(order.id)
  } else {
    orderReviews.value = []
  }
  showOrderDetailModal.value = true
  return detail ?? null
}

async function openReviewDialog(order: OrderSummary) {
  if (!canReviewOrder(order)) {
    return
  }
  const detail = await openOrderDetail(order)
  if (!detail) {
    return
  }
  const editable = detail.orderItems
    .map((item) => findEditableReviewForItem(item.id))
    .find((entry) => entry !== null)
  if (editable) {
    startEditExistingReview(editable)
    return
  }
  const candidate = detail.orderItems.find((item) => canCreateReviewForItem(item))
  if (candidate) {
    openReviewForm(candidate)
  }
}

async function openOrderDetailById(orderId: number) {
  const existing = orders.value.find((item) => item.id === orderId)
  if (existing) {
    return openOrderDetail(existing)
  }
  const placeholder: OrderSummary = {
    id: orderId,
    orderNo: `订单 #${orderId}`,
    totalAmount: 0,
    totalQuantity: 0,
    status: '—',
    orderTime: '',
  }
  return openOrderDetail(placeholder)
}

async function openReviewFromList(review: ProductReview) {
  const detail = await openOrderDetailById(review.orderId)
  if (!detail) {
    return
  }
  const latest = orderReviews.value.find((item) => item.id === review.id) ?? review
  startEditExistingReview(latest)
}

function closeOrderDetailModal() {
  showOrderDetailModal.value = false
  orderReviews.value = []
  resetReviewContext()
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
  if (!canEditOrder(order)) {
    return
  }
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
  { label: '我的收藏', href: '#favorites' },
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
                  <td>{{ defaultAddress?.shippingAddress ?? profile?.address ?? '尚未填写' }}</td>
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

        <section
          id="address"
          class="panel address-management full-row table-panel"
          aria-labelledby="address-title"
        >
          <div class="panel-title-row">
            <div class="panel-title" id="address-title">地址管理</div>
            <button type="button" class="panel-action-button" @click="openAddressModal()">
              新增地址
            </button>
          </div>
          <p v-if="addressLoading" class="placeholder">地址加载中…</p>
          <p v-else-if="addressError && !hasAddresses" class="placeholder is-error">
            {{ addressError }}
          </p>
          <div v-else-if="hasAddresses" class="table-container scrollable-table">
            <table class="dashboard-table address-table">
              <thead>
                <tr>
                  <th scope="col">收货人</th>
                  <th scope="col">联系电话</th>
                  <th scope="col">收货地址</th>
                  <th scope="col">默认</th>
                  <th scope="col" class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in addresses" :key="item.id">
                  <td>{{ item.recipientName }}</td>
                  <td>{{ item.recipientPhone }}</td>
                  <td class="address-cell">{{ item.shippingAddress }}</td>
                  <td>
                    <span v-if="item.isDefault" class="default-badge">默认</span>
                    <span v-else class="muted">—</span>
                  </td>
                  <td class="actions-cell">
                    <button type="button" class="link-button" @click="openAddressModal(item)">
                      编辑
                    </button>
                    <button
                      v-if="!item.isDefault"
                      type="button"
                      class="link-button"
                      @click="setDefaultAddress(item)"
                    >
                      设为默认
                    </button>
                    <button
                      type="button"
                      class="link-button danger"
                      :disabled="deletingAddressId === item.id"
                      @click="deleteAddress(item)"
                    >
                      {{ deletingAddressId === item.id ? '删除中…' : '删除' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">您还没有保存收货地址，请添加一个常用地址。</p>
          <p v-if="addressMessage" class="panel-success">{{ addressMessage }}</p>
          <p v-if="addressActionError" class="panel-error">{{ addressActionError }}</p>
        </section>

        <section class="panel cart full-row table-panel" aria-labelledby="cart-title">
      <div class="panel-title-row">
        <div class="panel-title" id="cart-title">我的购物车</div>
        <div class="panel-actions">
          <span v-if="hasCartItems" class="cart-summary">
            共 {{ cartTotalQuantity }} 件商品，小计 {{ formatCurrency(cartTotalAmount) }}
          </span>
          <span v-if="cartSelectionEnabled" class="cart-summary strong">
            已选 {{ selectedCartItemIds.size }} 件，合计 {{ formatCurrency(selectedCartTotalAmount) }}
          </span>
          <button
            type="button"
            class="panel-action-button ghost"
            :disabled="!hasCartItems"
            @click="toggleCartSelection"
          >
            {{ cartSelectionEnabled ? '取消选择' : '选择商品' }}
          </button>
          <button
            type="button"
            class="panel-action-button primary"
            :disabled="!cartSelectionEnabled || !hasCartSelection || cartCheckoutSubmitting"
            @click="openCartCheckout"
          >
            {{ cartCheckoutSubmitting ? '结算中…' : '去结算' }}
          </button>
          <button type="button" class="panel-action-button" @click="loadCart" :disabled="cartLoading">
            {{ cartLoading ? '刷新中…' : '刷新' }}
          </button>
        </div>
      </div>
          <p v-if="cartLoading" class="empty">购物车加载中…</p>
          <p v-else-if="cartError" class="empty is-error">{{ cartError }}</p>
        <div v-else-if="hasCartItems" class="table-container scrollable-table">
          <table class="dashboard-table cart-table">
            <thead>
              <tr>
                <th v-if="cartSelectionEnabled" scope="col" class="col-select">选择</th>
                <th scope="col" class="col-product">商品</th>
                <th scope="col">单价</th>
                <th scope="col">数量</th>
                <th scope="col">小计</th>
                <th scope="col" class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in cartItems" :key="item.id">
                  <td v-if="cartSelectionEnabled" class="col-select">
                    <label class="checkbox-circle">
                      <input
                        type="checkbox"
                        :checked="selectedCartItemIds.has(item.id)"
                        @change="toggleCartItemChecked(item)"
                      />
                      <span class="checkmark" aria-hidden="true"></span>
                    </label>
                  </td>
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
          <p v-if="cartCheckoutMessage" class="panel-success">{{ cartCheckoutMessage }}</p>
          <p v-if="cartCheckoutError" class="panel-error">{{ cartCheckoutError }}</p>
        </section>

        <section
          id="favorites"
          class="panel favorites full-row table-panel"
          aria-labelledby="favorites-title"
        >
          <div class="panel-title-row">
            <div class="panel-title" id="favorites-title">我的收藏</div>
            <div class="panel-actions">
              <button type="button" class="panel-action-button" @click="loadFavorites" :disabled="favoritesLoading">
                {{ favoritesLoading ? '刷新中…' : '刷新' }}
              </button>
            </div>
          </div>
          <p v-if="favoritesLoading" class="empty">收藏加载中…</p>
          <p v-else-if="favoritesError" class="empty is-error">{{ favoritesError }}</p>
          <div v-else-if="hasFavorites" class="table-container scrollable-table">
            <table class="dashboard-table favorites-table">
              <thead>
                <tr>
                  <th scope="col">商品</th>
                  <th scope="col" class="col-price">价格</th>
                  <th scope="col" class="col-time">收藏时间</th>
                  <th scope="col" class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in favorites" :key="item.id">
                  <td class="col-product">
                    <div class="favorite-product">
                      <div class="image">
                        <img
                          v-if="item.product?.mainImage"
                          :src="item.product.mainImage"
                          :alt="item.product?.name ?? '商品图片'"
                          loading="lazy"
                        />
                        <span v-else class="placeholder">
                          {{ item.product?.name?.charAt(0)?.toUpperCase() ?? '藏' }}
                        </span>
                      </div>
                      <div class="info">
                        <strong class="name">{{ item.product?.name ?? '商品已下架' }}</strong>
                        <span class="muted">{{ item.product?.categoryName ?? '未分类' }}</span>
                      </div>
                    </div>
                  </td>
                  <td class="col-price">
                    <span v-if="item.product">¥{{ item.product.price }}</span>
                    <span v-else class="muted">—</span>
                  </td>
                  <td class="col-time">{{ formatDateTime(item.createdAt) }}</td>
                  <td class="actions-cell">
                    <RouterLink
                      v-if="item.product?.id"
                      class="link-button"
                      :to="`/product/${item.product.id}`"
                    >
                      查看
                    </RouterLink>
                    <RouterLink
                      v-if="item.product?.id"
                      class="link-button"
                      :to="`/product/${item.product.id}`"
                    >
                      去购买
                    </RouterLink>
                    <button
                      type="button"
                      class="link-button danger"
                      :disabled="removingFavoriteId === item.id"
                      @click="removeFavorite(item)"
                    >
                      {{ removingFavoriteId === item.id ? '移除中…' : '删除' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">暂无收藏，快去产品中心收藏喜欢的商品吧。</p>
          <p v-if="favoriteActionMessage" class="panel-success">{{ favoriteActionMessage }}</p>
          <p v-if="favoriteActionError" class="panel-error">{{ favoriteActionError }}</p>
        </section>

        <section class="panel orders full-row table-panel" aria-labelledby="orders-title">
          <div class="panel-title" id="orders-title">我的订单</div>
          <div v-if="hasOrders" class="table-container scrollable-table">
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
                  <td class="col-order-no">
                    <span class="order-no">{{ order.orderNo }}</span>
                    <span
                      v-if="getOrderProductSummary(order)"
                      class="order-products"
                    >
                      （{{ getOrderProductSummary(order) }}）
                    </span>
                  </td>
                  <td>{{ formatCurrency(order.totalAmount) }}</td>
                  <td>{{ order.totalQuantity }}</td>
                  <td><span class="status-pill">{{ order.status }}</span></td>
                  <td>{{ formatDateTime(order.orderTime) }}</td>
                  <td class="actions-cell">
                    <button
                      v-if="isPendingPaymentStatus(order.status)"
                      type="button"
                      class="link-button"
                      @click="openPaymentDialog(order)"
                    >
                      去付款
                    </button>
                    <button type="button" class="link-button" @click="openOrderDetail(order)">
                      查看订单
                    </button>
                    <button
                      type="button"
                      class="link-button"
                      :disabled="!canConfirmReceipt(order) || confirmingReceiptOrderId === order.id"
                      @click="confirmReceipt(order)"
                    >
                      {{ confirmingReceiptOrderId === order.id ? '确认中…' : '确认收货' }}
                    </button>
                    <button
                      type="button"
                      class="link-button"
                      :disabled="!canReviewOrder(order)"
                      @click="openReviewDialog(order)"
                    >
                      评价商品
                    </button>
                    <button type="button" class="link-button" @click="openReturnDialog(order)">
                      申请退货
                    </button>
                    <button
                      type="button"
                      class="link-button"
                      :disabled="!canEditOrder(order)"
                      @click="openEditDialog(order)"
                    >
                      更改信息
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty">暂无订单记录，前往首页挑选心仪商品吧。</p>
          <p v-if="orderActionMessage" class="panel-success">{{ orderActionMessage }}</p>
          <p v-if="orderActionError" class="panel-error">{{ orderActionError }}</p>
        </section>

        <section
          id="reviews"
          class="panel reviews full-row table-panel"
          aria-labelledby="reviews-title"
        >
          <div class="panel-title-row">
            <div class="panel-title" id="reviews-title">我的评价</div>
          </div>
          <div v-if="reviewsLoading" class="placeholder">正在加载评价…</div>
          <div v-else-if="reviewsError" class="placeholder is-error">{{ reviewsError }}</div>
          <template v-else>
            <div v-if="hasReviews" class="table-container scrollable-table">
              <table class="dashboard-table review-table">
                <thead>
                  <tr>
                    <th scope="col">商品</th>
                    <th scope="col" class="col-rating">评分</th>
                    <th scope="col">评价内容</th>
                    <th scope="col" class="col-time">时间</th>
                    <th scope="col" class="col-actions">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="review in myReviews" :key="review.id">
                    <td class="col-product">
                      <strong>{{ review.productName }}</strong>
                      <p class="muted">订单ID：{{ review.orderId }}</p>
                    </td>
                    <td class="col-rating">
                      <span class="rating-badge">{{ review.rating }} 分</span>
                    </td>
                    <td>
                      {{ review.comment && review.comment.trim() ? review.comment : '（暂无文字评价）' }}
                    </td>
                    <td class="col-time">{{ formatDateTime(review.createdAt) }}</td>
                    <td class="col-actions">
                      <button type="button" class="link-button" @click="openReviewFromList(review)">
                        编辑
                      </button>
                      <button type="button" class="link-button" @click="openOrderDetailById(review.orderId)">
                        查看订单
                      </button>
                      <button
                        type="button"
                        class="link-button danger"
                        @click="deleteReview(review, 'list')"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="empty">您还没有发布过商品评价，快去评价已完成的订单吧。</p>
          </template>
          <p v-if="reviewListMessage" class="panel-success">{{ reviewListMessage }}</p>
          <p v-if="reviewListError" class="panel-error">{{ reviewListError }}</p>
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

<div v-if="showCartCheckoutModal" class="modal-backdrop" @click.self="closeCartCheckoutModal">
  <section class="modal" role="dialog" aria-modal="true" aria-labelledby="cart-checkout-title">
    <header class="modal-header">
      <h3 id="cart-checkout-title">购物车结算</h3>
      <button type="button" class="icon-button" aria-label="关闭" @click="closeCartCheckoutModal">
        ×
      </button>
    </header>
    <div class="modal-body">
      <p class="modal-hint">已选 {{ selectedCartItemIds.size }} 件商品</p>
      <p class="modal-amount">应付金额：{{ formatCurrency(selectedCartTotalAmount) }}</p>
      <p class="modal-balance" v-if="walletBalance !== null">
        当前余额：{{ formatCurrency(walletBalance) }}
      </p>
      <p v-if="cartCheckoutError" class="modal-error">{{ cartCheckoutError }}</p>
      <p v-if="cartCheckoutMessage" class="modal-success">{{ cartCheckoutMessage }}</p>
      <footer class="modal-actions">
        <button
          type="button"
          class="primary-button"
          :disabled="cartCheckoutSubmitting"
          @click="submitCartCheckout"
        >
          {{ cartCheckoutSubmitting ? '支付中…' : '立即支付' }}
        </button>
        <button type="button" class="ghost-button" :disabled="cartCheckoutSubmitting" @click="closeCartCheckoutModal">
          取消
        </button>
      </footer>
    </div>
  </section>
</div>

<div v-if="showAddressModal" class="modal-backdrop" @click.self="closeAddressModal">
  <section class="modal" role="dialog" aria-modal="true" aria-labelledby="address-modal-title">
        <header class="modal-header">
          <h3 id="address-modal-title">{{ editingAddressId ? '编辑收货地址' : '新增收货地址' }}</h3>
          <button type="button" class="icon-button" aria-label="关闭" @click="closeAddressModal">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitAddressForm">
          <label class="field">
            <span>收货人姓名</span>
            <input
              v-model="addressForm.recipientName"
              type="text"
              placeholder="如：张三"
              :disabled="addressSubmitting"
            />
          </label>
          <label class="field">
            <span>联系电话</span>
            <input
              v-model="addressForm.recipientPhone"
              type="tel"
              placeholder="请输入联系电话"
              :disabled="addressSubmitting"
            />
          </label>
          <label class="field">
            <span>详细收货地址</span>
            <textarea
              v-model="addressForm.shippingAddress"
              rows="3"
              placeholder="请填写详细地址信息"
              :disabled="addressSubmitting"
            ></textarea>
          </label>
          <label class="checkbox-field">
            <input type="checkbox" v-model="addressForm.isDefault" :disabled="addressSubmitting" />
            <span>设为默认地址</span>
          </label>
          <p v-if="addressFormError" class="modal-error">{{ addressFormError }}</p>
          <footer class="modal-actions">
            <button type="submit" class="primary-button" :disabled="addressSubmitting">
              {{ addressSubmitting ? '保存中…' : '保存地址' }}
            </button>
            <button
              type="button"
              class="ghost-button"
              :disabled="addressSubmitting"
              @click="closeAddressModal"
            >
              取消
            </button>
          </footer>
        </form>
      </section>
    </div>

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
            <p v-if="reviewMessage" class="modal-success">{{ reviewMessage }}</p>
            <p v-if="reviewError" class="modal-error">{{ reviewError }}</p>
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
                <dt>支付方式</dt>
                <dd>{{ orderDetail.paymentMethod ? paymentLabel(orderDetail.paymentMethod) : '—' }}</dd>
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
                  <div class="item-head">
                    <div class="item-info">
                      <strong>{{ item.product.name }}</strong>
                      <span>数量：{{ item.quantity }}</span>
                    </div>
                    <div class="item-prices">
                      <span>单价：{{ formatCurrency(item.unitPrice) }}</span>
                      <span>小计：{{ formatCurrency(item.totalPrice) }}</span>
                    </div>
                  </div>
                  <section class="review-section">
                    <header class="review-header">
                      <div>
                        <strong>商品评价</strong>
                        <span class="muted">查看或撰写对本商品的评价</span>
                      </div>
                      <div class="review-header-actions">
                        <button
                          v-if="canCreateReviewForItem(item)"
                          type="button"
                          class="panel-action-button"
                          @click="openReviewForm(item)"
                        >
                          写评价
                        </button>
                        <button
                          v-else-if="findEditableReviewForItem(item.id)"
                          type="button"
                          class="panel-action-button"
                          @click="startEditReviewForItem(item.id)"
                        >
                          修改评价
                        </button>
                      </div>
                    </header>
                    <div v-if="reviewMap.get(item.id)?.length" class="review-list">
                      <article
                        v-for="review in reviewMap.get(item.id) ?? []"
                        :key="review.id"
                        class="review-entry"
                      >
                        <header>
                          <span class="review-author">{{ review.authorName ?? review.consumerName ?? '用户' }}</span>
                          <span class="review-role">{{ reviewRoleLabel(review.authorRole) }}</span>
                          <span class="rating-badge">{{ review.rating }} 分</span>
                          <span class="muted">{{ formatDateTime(review.createdAt) }}</span>
                        </header>
                        <p v-if="review.comment" class="review-comment">{{ review.comment }}</p>
                        <footer class="review-entry-actions">
                          <button
                            v-if="canEditReview(review)"
                            type="button"
                            class="link-button"
                            @click="startEditExistingReview(review)"
                          >
                            编辑
                          </button>
                          <button
                            v-if="canDeleteReview(review)"
                            type="button"
                            class="link-button danger"
                            @click="deleteReview(review, 'modal')"
                          >
                            删除
                          </button>
                        </footer>
                      </article>
                    </div>
                    <p v-else class="muted">暂未有人评价此商品。</p>
                    <form
                      v-if="activeReviewItemId === item.id"
                      class="review-form"
                      @submit.prevent="submitReview"
                    >
                      <label>
                        <span>评分</span>
                        <select v-model.number="reviewForm.rating" :disabled="submittingReview">
                          <option v-for="score in [5, 4, 3, 2, 1]" :key="score" :value="score">
                            {{ score }} 分
                          </option>
                        </select>
                      </label>
                      <label>
                        <span>评价内容</span>
                        <textarea
                          v-model="reviewForm.comment"
                          rows="3"
                          placeholder="分享您的使用体验"
                          :disabled="submittingReview"
                        ></textarea>
                      </label>
                      <div class="review-form-actions">
                        <button type="submit" class="primary-button" :disabled="submittingReview">
                          {{ editingReviewId ? '保存评价' : '提交评价' }}
                        </button>
                        <button
                          type="button"
                          class="ghost-button"
                          @click="cancelReviewForm"
                          :disabled="submittingReview"
                        >
                          取消
                        </button>
                      </div>
                    </form>
                  </section>
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

    <div v-if="showPaymentModal" class="modal-backdrop" @click.self="closePaymentModal">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="payment-title">
        <header class="modal-header">
          <h3 id="payment-title">订单付款</h3>
          <button type="button" class="icon-button" aria-label="关闭" @click="closePaymentModal">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitPayment">
          <div class="modal-summary">
            <p><strong>订单编号：</strong>{{ paymentOrder?.orderNo ?? '—' }}</p>
            <p><strong>订单金额：</strong>{{ formatCurrency(paymentOrder?.totalAmount) }}</p>
            <p><strong>商品数量：</strong>{{ paymentOrder?.totalQuantity ?? 0 }} 件</p>
            <p><strong>当前状态：</strong>{{ paymentOrder?.status ?? '—' }}</p>
          </div>
          <label>
            <span>支付方式</span>
            <select v-model="paymentMethod" :disabled="paymentSubmitting">
              <option v-for="option in paymentOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <p class="modal-status">确认付款后，系统将自动扣款并更新订单状态。</p>
          <p v-if="paymentError" class="modal-error">{{ paymentError }}</p>
          <footer class="modal-actions">
            <button type="submit" class="primary-button" :disabled="paymentSubmitting">
              {{ paymentSubmitting ? '支付中…' : '确认付款' }}
            </button>
            <button
              type="button"
              class="ghost-button"
              :disabled="paymentSubmitting"
              @click="closePaymentModal"
            >
              取消
            </button>
          </footer>
        </form>
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

.panel-action-button.ghost {
  background: rgba(79, 70, 229, 0.08);
  border-color: rgba(79, 70, 229, 0.18);
  color: #4338ca;
}

.panel-action-button.primary {
  background: linear-gradient(135deg, #4f46e5, #a855f7);
  color: #fff;
  border: none;
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.22);
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

.table-container.scrollable-table {
  --dashboard-scroll-visible-rows: 3;
  --dashboard-row-height: 4.8rem;
  --dashboard-header-height: 3.5rem;
  max-height: calc(
    var(--dashboard-scroll-visible-rows) * var(--dashboard-row-height) +
      var(--dashboard-header-height)
  );
  overflow-y: auto;
  overscroll-behavior: contain;
}

.table-container.scrollable-table::-webkit-scrollbar {
  width: 6px;
}

.table-container.scrollable-table::-webkit-scrollbar-thumb {
  background: rgba(79, 70, 229, 0.35);
  border-radius: 999px;
}

.table-container.scrollable-table::-webkit-scrollbar-track {
  background: transparent;
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

.review-table .col-rating {
  width: 90px;
  white-space: nowrap;
}

.review-table .col-time {
  width: 160px;
  white-space: nowrap;
}

.review-table .col-actions {
  width: 220px;
}

.review-table .col-product strong {
  display: block;
  margin-bottom: 0.3rem;
}

.review-table .col-product .muted {
  margin: 0;
}

.profile-table th {
  width: 160px;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.6);
}

.profile-table td {
  color: rgba(17, 24, 39, 0.85);
}

.address-table .address-cell {
  max-width: 32rem;
  word-break: break-word;
  line-height: 1.5;
}

.address-table .col-actions {
  width: 220px;
}

.default-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.65rem;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.16);
  color: #15803d;
  font-size: 0.75rem;
  font-weight: 600;
}

.checkbox-field {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: rgba(17, 24, 39, 0.78);
}

.checkbox-field input {
  width: 1rem;
  height: 1rem;
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

.orders-table .order-no {
  font-weight: 600;
}

.orders-table .order-products {
  display: inline-block;
  margin-left: 0.35rem;
  color: rgba(17, 24, 39, 0.68);
  font-size: 0.85rem;
}

.dashboard-table .col-actions {
  text-align: right;
  width: 160px;
}

.cart-summary {
  color: rgba(17, 24, 39, 0.6);
  font-size: 0.9rem;
}

.cart-summary.strong {
  font-weight: 700;
  color: #0f172a;
}

.cart-table .col-select {
  width: 60px;
}

.checkbox-circle {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.checkbox-circle input {
  opacity: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  position: absolute;
  cursor: pointer;
}

.checkbox-circle .checkmark {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  border: 2px solid rgba(79, 70, 229, 0.35);
  box-sizing: border-box;
  background: #fff;
  transition: all 0.2s ease;
}

.checkbox-circle input:checked + .checkmark {
  background: linear-gradient(135deg, #4f46e5, #a855f7);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.25);
}

.checkbox-circle input:focus-visible + .checkmark {
  outline: 2px solid rgba(79, 70, 229, 0.5);
  outline-offset: 2px;
}

.favorites-table .col-product {
  width: 45%;
}

.favorite-product {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.favorite-product .image {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.18), rgba(111, 169, 173, 0.2));
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.favorite-product .image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-product .image .placeholder {
  font-weight: 700;
  color: rgba(17, 24, 39, 0.55);
}

.favorite-product .info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.favorite-product .name {
  font-weight: 600;
  color: #111827;
}

.favorites-table .col-time {
  width: 160px;
}

.favorites-table .col-price {
  width: 120px;
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

.modal-summary {
  display: grid;
  gap: 0.35rem;
  font-size: 0.95rem;
  color: rgba(17, 24, 39, 0.7);
}

.modal-summary strong {
  font-weight: 600;
  color: rgba(17, 24, 39, 0.85);
}

.modal-hint {
  margin: 0;
  color: rgba(17, 24, 39, 0.65);
}

.modal-amount {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #16a34a;
}

.modal-balance {
  margin: 0;
  color: rgba(17, 24, 39, 0.7);
}

.modal-success {
  color: #15803d;
  font-weight: 600;
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

.panel-success {
  margin-top: 0.75rem;
  color: #15803d;
  font-weight: 600;
}

.panel-error {
  margin-top: 0.75rem;
  color: #b91c1c;
  font-weight: 600;
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

.item-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.item-info strong {
  font-weight: 700;
  font-size: 1rem;
}

.item-prices {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: rgba(17, 24, 39, 0.68);
  font-size: 0.9rem;
}

.review-section {
  display: grid;
  gap: 0.65rem;
  padding: 0.85rem;
  border-radius: 12px;
  border: 1px solid rgba(79, 70, 229, 0.14);
  background: rgba(255, 255, 255, 0.92);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.85rem;
  flex-wrap: wrap;
}

.review-header-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.review-list {
  display: grid;
  gap: 0.75rem;
}

.review-entry {
  display: grid;
  gap: 0.45rem;
  padding: 0.75rem;
  border-radius: 12px;
  border: 1px solid rgba(79, 70, 229, 0.12);
  background: rgba(79, 70, 229, 0.06);
}

.review-entry header {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  align-items: baseline;
}

.review-author {
  font-weight: 600;
}

.review-role {
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 0.75rem;
  font-weight: 600;
}

.review-comment {
  margin: 0;
  color: rgba(17, 24, 39, 0.7);
  line-height: 1.5;
}

.review-entry-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.review-form {
  display: grid;
  gap: 0.65rem;
  padding: 0.85rem;
  border-radius: 12px;
  border: 1px dashed rgba(79, 70, 229, 0.28);
  background: rgba(79, 70, 229, 0.08);
}

.review-form label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.68);
}

.review-form select,
.review-form textarea {
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 0.75rem;
  padding: 0.55rem 0.7rem;
  font-size: 0.95rem;
  background: rgba(255, 255, 255, 0.95);
}

.review-form textarea {
  min-height: 90px;
  resize: vertical;
}

.review-form-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.rating-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.16), rgba(250, 204, 21, 0.24));
  color: #92400e;
  font-weight: 600;
  font-size: 0.85rem;
}

.order-summary {
  display: flex;
  justify-content: flex-end;
  gap: 0.35rem;
  color: rgba(17, 24, 39, 0.68);
  font-weight: 600;
}

.muted {
  color: rgba(17, 24, 39, 0.55);
  font-size: 0.85rem;
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
