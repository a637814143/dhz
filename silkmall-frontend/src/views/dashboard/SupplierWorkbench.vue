<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type {
  CategoryOption,
  HomepageContent,
  PageResponse,
  ProductSummary,
  ReturnRequest,
  SupplierOrderSummary,
} from '@/types'

interface SupplierProfile {
  id: number
  username: string
  companyName?: string | null
  email?: string | null
  phone?: string | null
  address?: string | null
  contactPerson?: string | null
  businessLicense?: string | null
  supplierLevel?: string | null
  status?: string | null
}

const PRODUCT_EVENT_NAME = 'silkmall:products:changed'

type ProductChangeAction = 'created' | 'updated' | 'deleted' | 'status-changed'

interface ProductChangeDetail {
  action: ProductChangeAction
  productId?: number | null
  source?: string
}

const { state } = useAuthState()

const profile = ref<SupplierProfile | null>(null)
const products = ref<ProductSummary[]>([])
const soldOrders = ref<SupplierOrderSummary[]>([])
const homeContent = ref<HomepageContent | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const categories = ref<CategoryOption[]>([])
const walletBalance = ref<number | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)

const profileDialogOpen = ref(false)
const profileSubmitting = ref(false)
const profileFormMessage = ref<string | null>(null)
const profileFormError = ref<string | null>(null)

const profileForm = reactive({
  username: '',
  companyName: '',
  contactPerson: '',
  businessLicense: '',
  email: '',
  phone: '',
  address: '',
})

const productDialogOpen = ref(false)
const savingProduct = ref(false)
const productFormError = ref<string | null>(null)
const productFormMessage = ref<string | null>(null)
const deletingProductId = ref<number | null>(null)
const soldOrdersLoading = ref(false)
const soldOrdersError = ref<string | null>(null)
const shippingOrderId = ref<number | null>(null)

const productForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  price: '',
  unit: '',
  stock: 0,
  categoryId: null as number | null,
  status: 'OFF_SALE',
  mainImage: '',
})
const productImagePreview = ref<string | null>(null)
const productImageInput = ref<HTMLInputElement | null>(null)

const categoryNameInput = ref('')
const categorySaving = ref(false)
const categoryFeedback = ref<string | null>(null)
const categoryError = ref<string | null>(null)
const categoryDeletingId = ref<number | null>(null)
const categoryListExpanded = ref(false)

const unitOptions = ['件', '条', '个', '箱']

const returnRequests = ref<ReturnRequest[]>([])
const returnRequestsLoading = ref(false)
const returnRequestsError = ref<string | null>(null)
const returnActionMessage = ref<string | null>(null)
const returnActionError = ref<string | null>(null)
const updatingReturnId = ref<number | null>(null)
const resolutionDrafts = reactive<Record<number, string>>({})

watch(
  () => categories.value.length,
  (next, previous) => {
    const prevCount = typeof previous === 'number' ? previous : 0
    if (next > prevCount) {
      categoryListExpanded.value = true
    } else if (next === 0) {
      categoryListExpanded.value = false
    }
  }
)

function extractNumericId(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (!value || typeof value !== 'object') {
    return null
  }
  const source = value as Record<string, unknown>
  const rawId = source.id
  const parsed = typeof rawId === 'number' ? rawId : Number(rawId)
  return Number.isFinite(parsed) ? parsed : null
}

function announceProductChange(detail: ProductChangeDetail) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent<ProductChangeDetail>(PRODUCT_EVENT_NAME, { detail }))
}

async function loadProfile() {
  if (!state.user) return
  const { data } = await api.get<SupplierProfile>(`/suppliers/${state.user.id}`)
  profile.value = data
}

async function loadProducts() {
  if (!state.user) return
  const { data } = await api.get<PageResponse<ProductSummary>>(`/products/supplier/${state.user.id}`, {
    params: { page: 0, size: 1000 },
  })
  const content = Array.isArray(data?.content) ? data.content : []
  products.value = content
}

async function loadSoldOrders() {
  if (!state.user) {
    soldOrders.value = []
    soldOrdersError.value = null
    soldOrdersLoading.value = false
    return
  }

  soldOrdersLoading.value = true
  soldOrdersError.value = null
  try {
    const { data } = await api.get<PageResponse<SupplierOrderSummary>>(
      `/orders/supplier/${state.user.id}`,
      {
        params: { page: 0, size: 1000 },
      }
    )
    soldOrders.value = Array.isArray(data?.content) ? data.content : []
  } catch (err) {
    soldOrders.value = []
    soldOrdersError.value = err instanceof Error ? err.message : '加载已销售订单失败'
  } finally {
    soldOrdersLoading.value = false
  }
}

async function loadReturnRequests() {
  if (!state.user) {
    returnRequests.value = []
    returnRequestsError.value = null
    return
  }

  returnRequestsLoading.value = true
  returnRequestsError.value = null
  try {
    const { data } = await api.get<ReturnRequest[]>(`/returns/suppliers/${state.user.id}`)
    const list = Array.isArray(data) ? data : []
    list.sort((a, b) => {
      const timeA = a?.requestedAt ? new Date(a.requestedAt).getTime() : 0
      const timeB = b?.requestedAt ? new Date(b.requestedAt).getTime() : 0
      return timeB - timeA
    })
    returnRequests.value = list
    list.forEach((item) => {
      if (!item?.id) return
      const existing = resolutionDrafts[item.id]
      const value = typeof existing === 'string' ? existing : item.resolution ?? ''
      resolutionDrafts[item.id] = value
    })
  } catch (err) {
    returnRequests.value = []
    returnRequestsError.value = err instanceof Error ? err.message : '加载退货申请失败'
  } finally {
    returnRequestsLoading.value = false
  }
}

async function loadHomeContent() {
  const { data } = await api.get<HomepageContent>('/content/home')
  homeContent.value = data
}

function toCategoryOption(raw: unknown, fallbackName?: string): CategoryOption | null {
  if (!raw || typeof raw !== 'object') {
    return null
  }

  const source = raw as Record<string, unknown>
  const idRaw = source.id ?? source.categoryId ?? source.value
  const id = typeof idRaw === 'number' ? idRaw : Number(idRaw)

  if (!Number.isFinite(id)) {
    return null
  }

  const rawName = source.name ?? source.categoryName ?? fallbackName ?? ''
  const name = typeof rawName === 'string' ? rawName.trim() : String(rawName ?? '').trim()

  return {
    id,
    name: name.length > 0 ? name : `分类 ${id}`,
  }
}

function sortCategoryOptions(options: CategoryOption[]): CategoryOption[] {
  return [...options].sort((a, b) => {
    const nameA = (a.name ?? '').trim()
    const nameB = (b.name ?? '').trim()
    return nameA.localeCompare(nameB, 'zh-CN')
  })
}

function extractCategoryArray(payload: unknown): unknown[] {
  if (Array.isArray(payload)) {
    return payload
  }

  if (!payload || typeof payload !== 'object') {
    return []
  }

  const container = payload as Record<string, unknown>
  const nestedSources: unknown[] = []

  const directData = container['data']
  if (Array.isArray(directData)) {
    return directData
  }

  if (directData && typeof directData === 'object' && directData !== payload) {
    nestedSources.push(directData)
  }

  const candidateKeys = ['records', 'content', 'list', 'items', 'rows', 'result']
  for (const key of candidateKeys) {
    const value = container[key]
    if (Array.isArray(value)) {
      return value
    }
    if (value && typeof value === 'object' && value !== payload) {
      nestedSources.push(value)
    }
  }

  for (const source of nestedSources) {
    const extracted = extractCategoryArray(source)
    if (extracted.length > 0) {
      return extracted
    }
  }

  return []
}

function extractSingleCategory(payload: unknown): unknown {
  const list = extractCategoryArray(payload)
  if (list.length > 0) {
    return list[0]
  }

  if (payload && typeof payload === 'object') {
    const container = payload as Record<string, unknown>
    const directData = container['data']
    if (directData && directData !== payload) {
      return extractSingleCategory(directData)
    }
  }

  return payload
}

function normaliseCategoryOptions(payload: unknown): CategoryOption[] {
  const rawList = extractCategoryArray(payload)

  const deduped = new Map<number, CategoryOption>()
  for (const item of rawList) {
    const option = toCategoryOption(item)
    if (option) {
      deduped.set(option.id, option)
    }
  }

  return sortCategoryOptions([...deduped.values()])
}

function mergeCategoryOption(option: CategoryOption) {
  categories.value = sortCategoryOptions([
    ...categories.value.filter((existing) => existing.id !== option.id),
    option,
  ])
}

async function loadCategories() {
  try {
    const { data } = await api.get<CategoryOption[]>('/categories/options')
    categories.value = normaliseCategoryOptions(data)
  } catch (err) {
    console.warn('加载分类失败', err)
    categories.value = []
  }
}

async function loadWallet() {
  if (!state.user) return
  try {
    const { data } = await api.get<{ balance: number }>('/wallet')
    walletBalance.value = data.balance
  } catch (err) {
    console.warn('加载钱包失败', err)
    walletBalance.value = null
  }
}

function openProfileDialog() {
  const current = profile.value
  profileForm.username = current?.username ?? state.user?.username ?? ''
  profileForm.companyName = current?.companyName ?? ''
  profileForm.contactPerson = current?.contactPerson ?? ''
  profileForm.businessLicense = current?.businessLicense ?? ''
  profileForm.email = current?.email ?? ''
  profileForm.phone = current?.phone ?? ''
  profileForm.address = current?.address ?? ''
  profileFormMessage.value = null
  profileFormError.value = null
  profileDialogOpen.value = true
}

function closeProfileDialog() {
  profileDialogOpen.value = false
  profileSubmitting.value = false
  profileFormMessage.value = null
  profileFormError.value = null
}

async function submitProfileUpdate() {
  if (!state.user) {
    profileFormError.value = '请先登录供应商账号'
    return
  }

  const companyName = profileForm.companyName.trim()
  if (!companyName) {
    profileFormError.value = '请填写企业名称'
    return
  }

  const contactPerson = profileForm.contactPerson.trim()
  if (!contactPerson) {
    profileFormError.value = '请填写联系人信息'
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
    companyName,
    contactPerson,
    businessLicense: profileForm.businessLicense.trim() || null,
    email: profileForm.email.trim() || null,
    phone: profileForm.phone.trim() || null,
    address: profileForm.address.trim() || null,
  }

  profileSubmitting.value = true
  profileFormError.value = null
  profileFormMessage.value = null
  try {
    const { data } = await api.put<SupplierProfile>(`/suppliers/${state.user.id}`, payload)
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

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([
      loadProfile(),
      loadProducts(),
      loadSoldOrders(),
      loadReturnRequests(),
      loadHomeContent(),
      loadCategories(),
      loadWallet(),
    ])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载供应商数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

const totalStock = computed(() => products.value.reduce((acc, item) => acc + (item.stock ?? 0), 0))
const totalSales = computed(() => products.value.reduce((acc, item) => acc + (item.sales ?? 0), 0))
const onSaleProducts = computed(() => products.value.filter((item) => item.status === 'ON_SALE').length)

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

function orderStatusLabel(status?: string | null) {
  if (!status) return '未知状态'
  const normalized = status.trim()
  return normalized.length > 0 ? normalized : '未知状态'
}

function normalizeOrderStatus(value?: string | null) {
  if (!value) return ''
  return value.trim().replace(/[\s_-]+/g, '').toUpperCase()
}

const awaitingConsumerConfirmationStatuses = new Set(
  [
    '已发货',
    '运送中',
    '待收货',
    'SHIPPED',
    'IN TRANSIT',
    'AWAITING RECEIPT',
  ].map((value) => normalizeOrderStatus(value))
)

const pendingShipmentStatuses = new Set(
  ['待发货', 'PENDING SHIPMENT'].map((value) => normalizeOrderStatus(value))
)

function soldOrderHint(order: SupplierOrderSummary): string {
  const status = normalizeOrderStatus(order.status)
  if (order.mixedSuppliers && pendingShipmentStatuses.has(status)) {
    return '订单包含其他供应商商品，需平台协调发货'
  }
  if (awaitingConsumerConfirmationStatuses.has(status)) {
    return '等待消费者确认收货'
  }
  return '订单当前状态无需操作'
}

function returnStatusLabel(status?: string | null) {
  if (!status) return '未知状态'
  const normalized = status.trim().toUpperCase()
  switch (normalized) {
    case 'PENDING':
      return '待处理'
    case 'AWAITING_ADMIN':
      return '待管理员审批'
    case 'APPROVED':
      return '已确认'
    case 'REJECTED':
      return '已拒绝'
    case 'COMPLETED':
      return '已完成'
    default:
      return '未知状态'
  }
}

function canProcessReturn(request: ReturnRequest) {
  return (request.status ?? '').toUpperCase() === 'PENDING'
}

function resolveDraft(requestId: number) {
  const value = resolutionDrafts[requestId]
  return typeof value === 'string' ? value : ''
}

function updateResolutionDraft(requestId: number, value: string) {
  resolutionDrafts[requestId] = value
}

async function handleReturnDecision(request: ReturnRequest, status: 'APPROVED' | 'REJECTED') {
  if (!request?.id) return

  const note = resolveDraft(request.id).trim()
  if (status === 'REJECTED' && !note) {
    returnActionError.value = '请填写拒绝原因'
    return
  }

  updatingReturnId.value = request.id
  returnActionMessage.value = null
  returnActionError.value = null
  try {
    await api.put(`/returns/${request.id}/status`, {
      status,
      resolution: note,
    })
    let baseMessage = '已拒绝退货申请'
    if (status === 'APPROVED') {
      baseMessage = request.afterReceipt
        ? '已确认退货申请，等待管理员审批'
        : '已确认退货申请'
    }
    const orderNoPart = request.orderId ? `（订单 ${request.orderId}）` : ''
    returnActionMessage.value = `${baseMessage}${orderNoPart}`
    await loadReturnRequests()
    await loadSoldOrders()
  } catch (err) {
    returnActionError.value = err instanceof Error ? err.message : '处理退货申请失败'
  } finally {
    updatingReturnId.value = null
  }
}

function productStatus(status?: string | null) {
  if (!status) return '未知'
  return status === 'ON_SALE' ? '在售' : '未上架'
}

function resetProductForm() {
  productForm.id = null
  productForm.name = ''
  productForm.description = ''
  productForm.price = ''
  productForm.unit = ''
  productForm.stock = 0
  productForm.categoryId = null
  productForm.status = 'OFF_SALE'
  productForm.mainImage = ''
  productFormError.value = null
  productFormMessage.value = null
  productImagePreview.value = null
  if (productImageInput.value) {
    productImageInput.value.value = ''
  }
}

async function openProductForm(product?: ProductSummary) {
  productFormError.value = null
  productFormMessage.value = null
  await loadCategories()
  if (product) {
    productForm.id = product.id
    productForm.name = product.name
    productForm.description = product.description ?? ''
    productForm.price = product.price?.toString() ?? ''
    productForm.unit = (product as any).unit ?? ''
    productForm.stock = product.stock ?? 0
    const categoryRef = (product as any).categoryId ?? (product as any).category?.id ?? null
    if (typeof categoryRef === 'number') {
      productForm.categoryId = categoryRef
    } else if (categoryRef !== null && categoryRef !== undefined) {
      const parsed = Number(categoryRef)
      productForm.categoryId = Number.isFinite(parsed) ? parsed : null
    } else {
      productForm.categoryId = null
    }
    productForm.status = product.status ?? 'OFF_SALE'
    productForm.mainImage = product.mainImage ?? ''
    productImagePreview.value = productForm.mainImage || null
    if (productImageInput.value) {
      productImageInput.value.value = ''
    }
  } else {
    resetProductForm()
  }
  productDialogOpen.value = true
}

function cancelProductForm() {
  productDialogOpen.value = false
  resetProductForm()
}

function removeProductImage() {
  productForm.mainImage = ''
  productImagePreview.value = null
  if (productImageInput.value) {
    productImageInput.value.value = ''
  }
}

function triggerProductImageSelection() {
  productFormError.value = null
  if (productImageInput.value) {
    productImageInput.value.click()
  }
}

function handleProductImageChange(event: Event) {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0]
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    productFormError.value = '请选择图片文件'
    if (target) target.value = ''
    return
  }
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    productFormError.value = '图片大小不能超过 5MB'
    if (target) target.value = ''
    return
  }
  productFormError.value = null
  const reader = new FileReader()
  reader.onload = () => {
    if (typeof reader.result === 'string') {
      productForm.mainImage = reader.result
      productImagePreview.value = reader.result
    }
  }
  reader.onerror = () => {
    productFormError.value = '图片读取失败，请重试'
    if (target) target.value = ''
  }
  reader.readAsDataURL(file)
}

async function saveProduct() {
  if (!state.user) {
    productFormError.value = '请先登录供应商账号'
    return
  }
  const name = productForm.name.trim()
  if (!name) {
    productFormError.value = '请填写商品名称'
    return
  }
  const price = Number(productForm.price)
  if (!Number.isFinite(price) || price <= 0) {
    productFormError.value = '请输入有效的商品价格'
    return
  }
  const stock = Number(productForm.stock)
  if (!Number.isInteger(stock) || stock < 0) {
    productFormError.value = '库存必须为非负整数'
    return
  }
  const unit = productForm.unit.trim()
  if (!unit) {
    productFormError.value = '请填写商品单位'
    return
  }

  const payload: Record<string, unknown> = {
    name,
    description: productForm.description.trim() || null,
    price,
    unit,
    stock,
    status: productForm.status,
    mainImage: productForm.mainImage.trim() || null,
    supplier: { id: state.user.id },
  }
  if (productForm.categoryId) {
    payload.category = { id: productForm.categoryId }
  }

  savingProduct.value = true
  productFormError.value = null
  try {
    if (productForm.id) {
      await api.put(`/products/${productForm.id}`, payload)
      productFormMessage.value = '商品信息已更新'
      announceProductChange({
        action: 'updated',
        productId: productForm.id,
        source: 'supplier-workbench',
      })
    } else {
      const { data } = await api.post('/products', payload)
      productFormMessage.value = '商品已创建并保存'
      announceProductChange({
        action: 'created',
        productId: extractNumericId(data),
        source: 'supplier-workbench',
      })
    }
    await loadProducts()
    productDialogOpen.value = false
    resetProductForm()
  } catch (err) {
    const message = err instanceof Error ? err.message : '保存商品失败'
    productFormError.value = message
  } finally {
    savingProduct.value = false
  }
}

async function deleteProduct(productId: number) {
  if (!window.confirm('确定删除该商品吗？')) return
  deletingProductId.value = productId
  try {
    await api.delete(`/products/${productId}`)
    await loadProducts()
    announceProductChange({
      action: 'deleted',
      productId,
      source: 'supplier-workbench',
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除商品失败'
    window.alert(message)
  } finally {
    deletingProductId.value = null
  }
}

async function confirmShipment(orderId: number) {
  if (!window.confirm('确认已将该订单中的商品交付物流？')) return
  shippingOrderId.value = orderId
  try {
    await api.put(`/orders/${orderId}/supplier-ship`)
    await loadSoldOrders()
    window.alert('发货状态已更新')
  } catch (err) {
    const message = err instanceof Error ? err.message : '确认发货失败'
    window.alert(message)
  } finally {
    shippingOrderId.value = null
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

async function createCategory() {
  categoryFeedback.value = null
  categoryError.value = null
  const name = categoryNameInput.value.trim()
  if (!name) {
    categoryError.value = '请输入分类名称'
    return
  }
  categorySaving.value = true
  try {
    const { data } = await api.post('/categories', {
      name,
      description: null,
      sortOrder: 0,
      enabled: true,
    })
    const option = toCategoryOption(extractSingleCategory(data), name)

    if (!option) {
      categoryError.value = '分类创建成功，但解析新分类失败，请刷新后重试'
      await loadCategories()
      return
    }

    mergeCategoryOption(option)
    categoryNameInput.value = ''
    categoryFeedback.value = `分类「${name}」创建成功`
  } catch (err) {
    const message = err instanceof Error ? err.message : '创建分类失败'
    categoryError.value = message
  } finally {
    categorySaving.value = false
  }
}

async function removeCategory(option: CategoryOption) {
  const id = option?.id
  if (typeof id !== 'number') {
    return
  }

  const name = (option.name ?? '').trim()
  const label = name.length > 0 ? `分类「${name}」` : '该分类'
  if (!window.confirm(`确定删除${label}吗？此操作不可撤销。`)) {
    return
  }

  categoryFeedback.value = null
  categoryError.value = null
  categoryDeletingId.value = id

  try {
    await api.delete(`/categories/${id}`)
    categories.value = categories.value.filter((item) => item.id !== id)
    if (productForm.categoryId === id) {
      productForm.categoryId = null
    }
    categoryFeedback.value = name.length > 0 ? `分类「${name}」已删除` : '分类已删除'
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除分类失败'
    categoryError.value = message
  } finally {
    if (categoryDeletingId.value === id) {
      categoryDeletingId.value = null
    }
  }
}

</script>

<template>
  <section class="workbench-shell">
    <header class="workbench-header">
      <div>
        <h1>{{ profile?.companyName ?? state.user?.username ?? '供应商工作台' }}</h1>
        <p>掌握商品库存、销售表现与平台促销资源，助力高效运营。</p>
      </div>
      <div class="metrics">
        <div>
          <span>总库存</span>
          <strong>{{ totalStock }}</strong>
        </div>
        <div>
          <span>累计销量</span>
          <strong>{{ totalSales }}</strong>
        </div>
        <div>
          <span>上架商品</span>
          <strong>{{ onSaleProducts }}</strong>
        </div>
        <div>
          <span>钱包余额</span>
          <strong>{{ formatCurrency(walletBalance ?? 0) }}</strong>
        </div>
      </div>
    </header>

    <div v-if="loading" class="placeholder">正在加载工作台数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
      <section class="panel profile" aria-labelledby="supplier-info">
        <div class="panel-title-row">
          <div class="panel-title" id="supplier-info">基础信息</div>
          <button type="button" class="panel-action-button" @click="openProfileDialog">编辑基础信息</button>
        </div>
        <ul>
          <li><span>企业名称</span><strong>{{ profile?.companyName ?? '—' }}</strong></li>
          <li><span>联系人</span><strong>{{ profile?.contactPerson ?? '—' }}</strong></li>
          <li><span>联系人邮箱</span><strong>{{ profile?.email ?? '—' }}</strong></li>
          <li><span>联系电话</span><strong>{{ profile?.phone ?? '—' }}</strong></li>
          <li><span>联系地址</span><strong>{{ profile?.address ?? '—' }}</strong></li>
          <li><span>营业执照</span><strong>{{ profile?.businessLicense ?? '—' }}</strong></li>
          <li><span>等级</span><strong>{{ profile?.supplierLevel ?? '未评级' }}</strong></li>
          <li><span>审核状态</span><strong>{{ profile?.status ?? '待审核' }}</strong></li>
        </ul>
        <div class="redeem-box">
          <label>
            <span>兑换码</span>
            <input
              v-model="redeemCodeInput"
              type="text"
              placeholder="输入兑换码获取余额"
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

      <section class="panel products" aria-labelledby="product-list">
        <div class="panel-title-row">
          <div class="panel-title" id="product-list">商品概览</div>
          <button type="button" class="primary" @click="openProductForm()">新增商品</button>
        </div>
        <div v-if="products.length" class="product-table scrollable-table">
          <table>
            <thead>
              <tr>
                <th scope="col">商品名称</th>
                <th scope="col">售价</th>
                <th scope="col">计量单位</th>
                <th scope="col">库存</th>
                <th scope="col">销量</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in products" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ formatCurrency(item.price) }}</td>
                <td>{{ item.unit ?? '—' }}</td>
                <td>{{ item.stock }}</td>
                <td>{{ item.sales }}</td>
                <td><span class="status-pill">{{ productStatus(item.status) }}</span></td>
                <td class="actions">
                  <button type="button" class="link-button" @click="openProductForm(item)">编辑</button>
                  <button
                    type="button"
                    class="link-button danger"
                    @click="deleteProduct(item.id)"
                    :disabled="deletingProductId === item.id"
                  >
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty">暂无商品，请尽快完成商品录入与上架。</p>

        <div class="category-create">
          <h4>快速创建分类</h4>
          <div class="category-row">
            <input v-model="categoryNameInput" type="text" placeholder="分类名称" :disabled="categorySaving" />
            <button type="button" @click="createCategory" :disabled="categorySaving">
              {{ categorySaving ? '创建中…' : '添加分类' }}
            </button>
          </div>
          <p v-if="categoryFeedback" class="success">{{ categoryFeedback }}</p>
          <p v-if="categoryError" class="error">{{ categoryError }}</p>
          <div class="category-existing">
            <button
              type="button"
              class="category-toggle"
              @click="categoryListExpanded = !categoryListExpanded"
              :aria-expanded="categoryListExpanded ? 'true' : 'false'"
            >
              <span class="label">
                已有分类
                <span v-if="categories.length" class="count">({{ categories.length }})</span>
              </span>
              <span class="chevron" :class="{ open: categoryListExpanded }">⌄</span>
            </button>
            <p v-if="categoryListExpanded && !categories.length" class="empty">暂无分类，创建后可在此管理。</p>
            <ul v-if="categoryListExpanded && categories.length" class="category-list">
              <li v-for="category in categories" :key="category.id">
                <span>{{ category.name }}</span>
                <button
                  type="button"
                  class="link-button danger"
                  @click="removeCategory(category)"
                  :disabled="categoryDeletingId === category.id"
                >
                  {{ categoryDeletingId === category.id ? '删除中…' : '删除' }}
                </button>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <section class="panel sold-orders" aria-labelledby="sold-orders">
        <div class="panel-title-row">
          <div class="panel-title" id="sold-orders">已销售商品</div>
        </div>
        <p v-if="soldOrdersLoading" class="empty">正在加载已销售的订单…</p>
        <p v-else-if="soldOrdersError" class="sold-order-error">{{ soldOrdersError }}</p>
        <div v-else-if="soldOrders.length" class="sold-order-container scrollable-list">
          <ul class="sold-order-list">
            <li v-for="order in soldOrders" :key="order.id" class="sold-order-card">
              <header class="sold-order-header">
                <div>
                  <h3>订单号：{{ order.orderNo }}</h3>
                  <p>下单时间：{{ formatDateTime(order.orderTime) }}</p>
                  <p v-if="order.paymentTime">支付时间：{{ formatDateTime(order.paymentTime) }}</p>
                </div>
                <div class="sold-order-status">
                  <span class="status-pill">{{ orderStatusLabel(order.status) }}</span>
                  <span v-if="order.mixedSuppliers" class="sold-order-note">含其他供应商商品</span>
                </div>
              </header>
              <div class="sold-order-body">
                <dl class="sold-order-meta">
                  <div>
                    <dt>收货人</dt>
                    <dd>{{ order.recipientName ?? '—' }}</dd>
                  </div>
                  <div>
                    <dt>联系电话</dt>
                    <dd>{{ order.recipientPhone ?? '—' }}</dd>
                  </div>
                  <div>
                    <dt>配送地址</dt>
                    <dd>{{ order.shippingAddress ?? '—' }}</dd>
                  </div>
                  <div>
                    <dt>商品金额</dt>
                    <dd>{{ formatCurrency(order.supplierTotalAmount) }}</dd>
                  </div>
                  <div>
                    <dt>商品数量</dt>
                    <dd>{{ order.supplierTotalQuantity }}</dd>
                  </div>
                </dl>
                <p v-if="!order.items.length" class="sold-order-empty-items">订单中暂无属于您的商品</p>
                <ul v-else class="sold-order-items">
                  <li v-for="item in order.items" :key="item.id">
                    <div>
                      <strong>{{ item.productName ?? '商品' }}</strong>
                      <span>数量 × {{ item.quantity }}</span>
                    </div>
                    <div class="sold-order-item-amount">{{ formatCurrency(item.totalPrice) }}</div>
                  </li>
                </ul>
              </div>
              <footer class="sold-order-footer">
                <button
                  v-if="order.canShip"
                  type="button"
                  class="primary-button"
                  @click="confirmShipment(order.id)"
                  :disabled="shippingOrderId === order.id"
                >
                  {{ shippingOrderId === order.id ? '更新中…' : '确认发货' }}
                </button>
                <span v-else class="sold-order-hint">{{ soldOrderHint(order) }}</span>
              </footer>
            </li>
          </ul>
        </div>
        <p v-else class="empty">暂时没有已销售的订单。</p>
      </section>

      <section class="panel returns" aria-labelledby="return-management">
        <div class="panel-title-row">
          <div class="panel-title" id="return-management">退货管理</div>
        </div>
        <transition name="fade">
          <p v-if="returnActionMessage" class="return-feedback return-feedback--success">
            {{ returnActionMessage }}
          </p>
        </transition>
        <transition name="fade">
          <p v-if="returnActionError" class="return-feedback return-feedback--error">
            {{ returnActionError }}
          </p>
        </transition>
        <p v-if="returnRequestsLoading" class="empty">正在加载退货申请…</p>
        <p v-else-if="returnRequestsError" class="return-error">{{ returnRequestsError }}</p>
        <div v-else-if="returnRequests.length" class="return-list scrollable-list">
          <article
            v-for="request in returnRequests"
            :key="request.id"
            :class="['return-card', { 'return-card--resolved': !canProcessReturn(request) }]"
          >
            <header class="return-card__head">
              <div>
                <h3>{{ request.productName ?? '商品' }}</h3>
                <p class="return-meta">申请时间：{{ formatDateTime(request.requestedAt) }}</p>
                <p class="return-meta" v-if="request.consumerName">消费者：{{ request.consumerName }}</p>
              </div>
              <span class="status-pill">{{ returnStatusLabel(request.status) }}</span>
            </header>
            <section class="return-card__body">
              <p>
                <strong>退货原因：</strong>
                <span>{{ request.reason?.trim() ? request.reason : '未提供' }}</span>
              </p>
              <p v-if="request.afterReceipt && request.refundAmount">
                <strong>退款金额：</strong>
                <span>
                  {{ formatCurrency(request.refundAmount) }}
                  <template v-if="request.supplierShareAmount || request.commissionAmount">
                    （供应商承担 {{ formatCurrency(request.supplierShareAmount) }}，平台承担
                    {{ formatCurrency(request.commissionAmount) }}）
                  </template>
                </span>
              </p>
              <p v-if="request.afterReceipt && request.status?.toUpperCase() === 'AWAITING_ADMIN'" class="return-hint">
                该退货已由您确认，正等待管理员最终审批退款。
              </p>
              <p v-if="request.resolution && !canProcessReturn(request)">
                <strong>处理说明：</strong>
                <span>{{ request.resolution }}</span>
              </p>
              <p v-if="request.adminResolution" class="return-admin-note">
                <strong>管理员说明：</strong>
                <span>
                  {{ request.adminResolution }}
                  <template v-if="request.adminProcessedAt">
                    （{{ formatDateTime(request.adminProcessedAt) }}）
                  </template>
                </span>
              </p>
              <label v-if="canProcessReturn(request)">
                <span>处理说明</span>
                <textarea
                  :value="resolveDraft(request.id)"
                  @input="updateResolutionDraft(request.id, ($event.target as HTMLTextAreaElement).value)"
                  rows="3"
                  placeholder="可填写退货处理备注"
                  :disabled="updatingReturnId === request.id"
                ></textarea>
              </label>
            </section>
            <footer class="return-card__footer">
              <button
                type="button"
                class="secondary"
                :disabled="!canProcessReturn(request) || updatingReturnId === request.id"
                @click="handleReturnDecision(request, 'REJECTED')"
              >
                {{ updatingReturnId === request.id ? '提交中…' : '拒绝退货' }}
              </button>
              <button
                type="button"
                class="primary"
                :disabled="!canProcessReturn(request) || updatingReturnId === request.id"
                @click="handleReturnDecision(request, 'APPROVED')"
              >
                {{ updatingReturnId === request.id ? '提交中…' : '确认退货' }}
              </button>
            </footer>
          </article>
        </div>
        <p v-else class="empty">暂无退货申请。</p>
      </section>

      <section class="panel promotions" aria-labelledby="promotion-list">
        <div class="panel-title" id="promotion-list">平台促销建议</div>
        <ul class="promotion-list">
          <li v-for="promo in homeContent?.promotions ?? []" :key="promo.productId">
            <div>
              <strong>{{ promo.title }}</strong>
              <p>{{ promo.description }}</p>
            </div>
            <span class="badge">{{ Math.round(promo.discountRate * 100) }}% OFF</span>
          </li>
        </ul>
      </section>
    </template>

    <div v-if="productDialogOpen" class="modal-backdrop" @click.self="cancelProductForm">
      <section
        class="modal"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="productForm.id ? 'supplier-product-edit-title' : 'supplier-product-create-title'"
      >
        <header class="modal-header">
          <h3 :id="productForm.id ? 'supplier-product-edit-title' : 'supplier-product-create-title'">
            {{ productForm.id ? '编辑商品' : '新增商品' }}
          </h3>
          <button type="button" class="icon-button" @click="cancelProductForm" aria-label="关闭">
            ×
          </button>
        </header>
        <form class="modal-body product-modal" @submit.prevent="saveProduct">
          <div class="modal-grid">
            <label>
              <span>商品名称</span>
              <input v-model="productForm.name" type="text" placeholder="请输入商品名称" />
            </label>
            <label class="product-status-field">
              <span>商品状态</span>
              <div class="product-status-display" role="status" aria-live="polite">
                {{ productStatus(productForm.status) }}
              </div>
              <p class="field-hint status-hint">
                新增商品将保持未上架状态，需管理员审核后才能上架并在产品中心展示。
              </p>
            </label>
          </div>

          <div class="modal-grid">
            <label>
              <span>售价（CNY）</span>
              <input v-model="productForm.price" type="number" min="0" step="0.01" placeholder="如：199" />
            </label>
            <label>
              <span>计量单位</span>
              <input
                v-model="productForm.unit"
                type="text"
                placeholder="如：件 / 箱 / kg"
                maxlength="20"
                list="supplier-product-unit-options"
              />
              <datalist id="supplier-product-unit-options">
                <option v-for="option in unitOptions" :key="option" :value="option"></option>
              </datalist>
            </label>
            <label>
              <span>库存数量</span>
              <input v-model.number="productForm.stock" type="number" min="0" placeholder="库存" />
            </label>
          </div>

          <label>
            <span>所属分类</span>
            <select v-model.number="productForm.categoryId">
              <option :value="null">未选择分类</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>

          <label class="product-image-field">
            <span>商品图片</span>
            <input
              ref="productImageInput"
              type="file"
              accept="image/*"
              class="product-image-input"
              @change="handleProductImageChange"
            />
            <button type="button" class="ghost-button image-picker" @click="triggerProductImageSelection">
              {{ productImagePreview ? '重新选择图片' : '选择图片' }}
            </button>
            <p class="field-hint">请选择商品主图，支持 JPG、PNG、WEBP 等格式，最大 5MB，可直接从电脑上传。</p>
            <div v-if="productImagePreview" class="product-image-preview" role="group" aria-label="商品图片预览">
              <img :src="productImagePreview" alt="商品图片预览" />
              <button type="button" class="ghost-button" @click="removeProductImage">移除图片</button>
            </div>
          </label>

          <label>
            <span>商品描述</span>
            <textarea v-model="productForm.description" rows="3" placeholder="介绍商品亮点"></textarea>
          </label>

          <p v-if="productFormError" class="modal-error">{{ productFormError }}</p>
          <p v-if="productFormMessage" class="modal-success">{{ productFormMessage }}</p>

          <footer class="modal-actions">
            <button type="button" class="ghost-button" @click="cancelProductForm" :disabled="savingProduct">
              取消
            </button>
            <button type="submit" class="primary-button" :disabled="savingProduct">
              {{ savingProduct ? '保存中…' : productForm.id ? '保存修改' : '创建商品' }}
            </button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="profileDialogOpen" class="modal-backdrop" @click.self="closeProfileDialog">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="supplier-profile-title">
        <header class="modal-header">
          <h3 id="supplier-profile-title">编辑基础信息</h3>
          <button type="button" class="icon-button" @click="closeProfileDialog" aria-label="关闭">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitProfileUpdate">
          <label>
            <span>企业名称</span>
            <input v-model="profileForm.companyName" type="text" placeholder="请输入企业名称" />
          </label>
          <label>
            <span>联系人</span>
            <input v-model="profileForm.contactPerson" type="text" placeholder="请输入联系人" />
          </label>
          <label>
            <span>联系人邮箱</span>
            <input v-model="profileForm.email" type="email" placeholder="请输入联系人邮箱" />
          </label>
          <label>
            <span>联系电话</span>
            <input v-model="profileForm.phone" type="tel" placeholder="请输入联系电话" />
          </label>
          <label>
            <span>联系地址</span>
            <input v-model="profileForm.address" type="text" placeholder="请输入联系地址" />
          </label>
          <label>
            <span>营业执照</span>
            <input v-model="profileForm.businessLicense" type="text" placeholder="请输入营业执照编号" />
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
  </section>
</template>

<style scoped>
.workbench-shell {
  display: grid;
  gap: 2.5rem;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  gap: 2rem;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(56, 189, 248, 0.12));
  padding: 2.25rem;
  border-radius: 24px;
}

.workbench-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.workbench-header p {
  color: rgba(15, 23, 42, 0.66);
}

.metrics {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.metrics div {
  background: rgba(255, 255, 255, 0.9);
  padding: 1rem 1.4rem;
  border-radius: 16px;
  box-shadow: 0 12px 26px rgba(14, 165, 233, 0.18);
  min-width: 140px;
}

.metrics span {
  display: block;
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.metrics strong {
  font-size: 1.4rem;
  font-weight: 700;
}

.placeholder {
  padding: 2rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  text-align: center;
  color: rgba(15, 23, 42, 0.6);
}

.placeholder.is-error {
  background: rgba(248, 113, 113, 0.12);
  color: #7f1d1d;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 1.8rem;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 1.5rem;
}

.panel-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: rgba(15, 23, 42, 0.78);
}

.panel-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.panel-action-button {
  border: 1px solid rgba(14, 165, 233, 0.24);
  background: rgba(56, 189, 248, 0.12);
  color: #0f172a;
  border-radius: 999px;
  padding: 0.5rem 1.2rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease, color 0.2s ease;
}

.panel-action-button:hover {
  background: rgba(14, 165, 233, 0.18);
  color: #0e7490;
  transform: translateY(-1px);
}

.panel-action-button:focus-visible {
  outline: 2px solid rgba(14, 165, 233, 0.45);
  outline-offset: 2px;
}

.panel-action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.primary {
  border: none;
  border-radius: 0.75rem;
  padding: 0.55rem 1.5rem;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.panel.profile ul {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 0.9rem;
}

.panel.profile li {
  display: flex;
  justify-content: space-between;
  color: rgba(15, 23, 42, 0.65);
}

.panel.profile strong {
  color: rgba(15, 23, 42, 0.85);
}

.redeem-box {
  display: grid;
  gap: 0.75rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding-top: 1rem;
}

.redeem-box label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
}

.redeem-box input {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
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
  background: linear-gradient(135deg, #0ea5e9, #38bdf8);
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

.product-table {
  border: 1px solid rgba(14, 165, 233, 0.18);
  border-radius: 18px;
  background: rgba(240, 249, 255, 0.75);
  overflow: hidden;
  overflow-x: auto;
}

.scrollable-table {
  --supplier-visible-rows: 3;
  --supplier-row-height: 4.75rem;
  --supplier-header-height: 3.4rem;
  max-height: calc(
    var(--supplier-visible-rows) * var(--supplier-row-height) +
      var(--supplier-header-height)
  );
  overflow-y: auto;
  overscroll-behavior: contain;
}

.scrollable-table::-webkit-scrollbar {
  width: 6px;
}

.scrollable-table::-webkit-scrollbar-thumb {
  background: rgba(37, 99, 235, 0.35);
  border-radius: 999px;
}

.scrollable-table::-webkit-scrollbar-track {
  background: transparent;
}

.product-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.product-table th,
.product-table td {
  padding: 0.75rem 0.5rem;
  text-align: left;
}

.product-table thead {
  color: rgba(15, 23, 42, 0.55);
}

.product-table tbody tr:nth-child(odd) {
  background: rgba(14, 165, 233, 0.08);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.18);
  color: #0ea5e9;
  font-weight: 600;
}

.product-table td.actions {
  display: flex;
  gap: 0.5rem;
}

.link-button {
  border: none;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.link-button.danger {
  color: #b91c1c;
}

.link-button[disabled] {
  opacity: 0.45;
  cursor: not-allowed;
}

.product-modal {
  display: grid;
  gap: 1.1rem;
}

.product-modal .modal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.product-modal label {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-weight: 600;
}

.product-status-field .product-status-display {
  display: inline-flex;
  align-items: center;
  min-height: 2.75rem;
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px dashed rgba(15, 23, 42, 0.2);
  background: rgba(15, 23, 42, 0.04);
  color: rgba(15, 23, 42, 0.85);
}

.product-status-field .status-hint {
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
  margin: 0;
}

.product-image-field {
  position: relative;
}

.product-modal input,
.product-modal select,
.product-modal textarea {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.15);
  font-size: 0.95rem;
}

.product-modal textarea {
  resize: vertical;
}

.product-image-field .product-image-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.product-image-field .image-picker {
  align-self: flex-start;
}

.product-image-field .field-hint {
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.product-image-preview {
  margin-top: 0.35rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.product-image-preview img {
  max-width: 220px;
  border-radius: 0.75rem;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
  object-fit: cover;
}

.product-image-preview .ghost-button {
  align-self: flex-start;
}

.product-modal .modal-actions {
  margin-top: 0.25rem;
}

.category-create {
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 0.75rem;
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.category-row input {
  flex: 1;
  min-width: 200px;
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.category-row button {
  padding: 0.55rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.category-row button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.category-create .success {
  color: #15803d;
}

.category-create .error {
  color: #b91c1c;
}

.category-existing {
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding-top: 1rem;
  display: grid;
  gap: 0.75rem;
}

.category-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border: none;
  background: transparent;
  padding: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
  cursor: pointer;
}

.category-toggle .label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.category-toggle .count {
  font-weight: 500;
  color: rgba(15, 23, 42, 0.6);
}

.category-toggle .chevron {
  display: inline-block;
  transition: transform 0.2s ease;
}

.category-toggle .chevron.open {
  transform: rotate(180deg);
}

.category-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

.category-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.55rem 0.75rem;
  border-radius: 0.75rem;
  background: rgba(15, 23, 42, 0.04);
}

.category-list li span {
  font-weight: 600;
  color: rgba(15, 23, 42, 0.8);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-list li button {
  white-space: nowrap;
}

.empty {
  color: rgba(15, 23, 42, 0.6);
}

.sold-orders {
  gap: 1.75rem;
}

.sold-order-error {
  color: #b91c1c;
}

.sold-order-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1.25rem;
}

.sold-order-container {
  border: 1px solid rgba(14, 165, 233, 0.18);
  border-radius: 20px;
  background: rgba(240, 249, 255, 0.7);
  padding: 1.25rem;
}

.scrollable-list {
  --supplier-visible-cards: 3;
  --supplier-card-height: 12.5rem;
  max-height: calc(var(--supplier-visible-cards) * var(--supplier-card-height));
  overflow-y: auto;
  overscroll-behavior: contain;
}

.scrollable-list::-webkit-scrollbar {
  width: 6px;
}

.scrollable-list::-webkit-scrollbar-thumb {
  background: rgba(14, 165, 233, 0.35);
  border-radius: 999px;
}

.scrollable-list::-webkit-scrollbar-track {
  background: transparent;
}

.sold-order-card {
  border: 1px solid rgba(14, 165, 233, 0.15);
  border-radius: 18px;
  padding: 1.5rem;
  display: grid;
  gap: 1rem;
  background: rgba(240, 249, 255, 0.55);
}

.sold-order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.sold-order-header h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
}

.sold-order-header p {
  margin: 0.15rem 0 0;
  color: rgba(15, 23, 42, 0.6);
  font-size: 0.9rem;
}

.sold-order-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
}

.sold-order-note {
  font-size: 0.75rem;
  color: #92400e;
  background: rgba(251, 191, 36, 0.2);
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-weight: 600;
}

.sold-order-body {
  display: grid;
  gap: 1rem;
}

.sold-order-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
  margin: 0;
}

.sold-order-meta div {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  padding: 0.75rem;
  display: grid;
  gap: 0.35rem;
}

.sold-order-meta dt {
  margin: 0;
  font-size: 0.8rem;
  color: rgba(15, 23, 42, 0.55);
  font-weight: 600;
}

.sold-order-meta dd {
  margin: 0;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.sold-order-empty-items {
  margin: 0;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: rgba(226, 232, 240, 0.45);
  color: rgba(15, 23, 42, 0.6);
  font-size: 0.9rem;
}

.sold-order-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.sold-order-items li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  background: rgba(14, 165, 233, 0.12);
  padding: 0.75rem 1rem;
  border-radius: 14px;
}

.sold-order-items strong {
  font-size: 0.95rem;
}

.sold-order-items span {
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.sold-order-item-amount {
  font-weight: 700;
  color: #0f172a;
}

.sold-order-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 1rem;
}

.sold-order-hint {
  color: rgba(15, 23, 42, 0.6);
  font-size: 0.9rem;
  text-align: right;
}

.return-feedback {
  padding: 0.75rem 1rem;
  border-radius: 0.9rem;
  font-weight: 600;
}

.return-feedback--success {
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
}

.return-feedback--error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.return-error {
  color: #b91c1c;
  font-weight: 600;
}

.return-list {
  display: grid;
  gap: 1.2rem;
  list-style: none;
  padding: 0;
}

.return-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 1rem;
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
  background: rgba(248, 250, 252, 0.8);
}

.return-card--resolved {
  background: rgba(241, 245, 249, 0.7);
  border-color: rgba(15, 23, 42, 0.06);
}

.return-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.return-card__head h3 {
  margin: 0;
  font-size: 1.1rem;
}

.return-meta {
  color: rgba(15, 23, 42, 0.55);
  font-size: 0.85rem;
}

.return-card__body {
  display: grid;
  gap: 0.8rem;
  color: rgba(15, 23, 42, 0.8);
}

.return-card__body strong {
  font-weight: 700;
  margin-right: 0.35rem;
}

.return-hint {
  color: #2563eb;
  font-size: 0.9rem;
}

.return-admin-note {
  color: rgba(15, 23, 42, 0.7);
}

.return-card__body label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
}

.return-card__body textarea {
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.15);
  padding: 0.6rem 0.75rem;
  resize: vertical;
  min-height: 5.5rem;
  font-family: inherit;
}

.return-card__body textarea:disabled {
  background: rgba(241, 245, 249, 0.6);
  cursor: not-allowed;
}

.return-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.return-card__footer .secondary {
  border: 1px solid rgba(248, 113, 113, 0.3);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
  border-radius: 0.75rem;
  padding: 0.55rem 1.3rem;
  font-weight: 600;
  cursor: pointer;
}

.return-card__footer .secondary:disabled,
.return-card__footer .primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.return-card__footer .primary {
  border: none;
  border-radius: 0.75rem;
  padding: 0.55rem 1.5rem;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.promotion-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}

.promotion-list li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.25rem;
  border-radius: 16px;
  border: 1px solid rgba(14, 165, 233, 0.15);
  background: rgba(240, 249, 255, 0.85);
}

.promotion-list strong {
  display: block;
  margin-bottom: 0.25rem;
}

.promotion-list p {
  color: rgba(15, 23, 42, 0.6);
  font-size: 0.9rem;
}

.badge {
  font-weight: 700;
  color: #0284c7;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 30;
  overflow-y: auto;
}

.modal {
  background: #fff;
  border-radius: 20px;
  max-width: 520px;
  width: 100%;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
  overflow: hidden;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.2rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  flex: 1 1 auto;
  overflow-y: auto;
}

.modal-body label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-size: 0.95rem;
}

.modal-body input {
  padding: 0.65rem 0.8rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.18);
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.primary-button {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  border: none;
  border-radius: 0.75rem;
  padding: 0.65rem 1.4rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.primary-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.22);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-button {
  border: 1px solid rgba(15, 23, 42, 0.18);
  background: transparent;
  color: #0f172a;
  border-radius: 0.75rem;
  padding: 0.65rem 1.3rem;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.ghost-button:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.15);
}

.ghost-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.icon-button {
  border: none;
  background: transparent;
  font-size: 1.4rem;
  cursor: pointer;
  color: rgba(15, 23, 42, 0.6);
}

.icon-button:hover {
  color: rgba(15, 23, 42, 0.85);
}

.modal-success {
  color: #15803d;
  font-size: 0.9rem;
}

.modal-error {
  color: #b91c1c;
  font-size: 0.9rem;
}

@media (max-width: 900px) {
  .workbench-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .metrics {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
