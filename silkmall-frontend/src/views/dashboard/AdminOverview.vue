<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type {
  Announcement,
  HomepageContent,
  NewsItem,
  ProductOverview,
  ProductSummary,
  CategoryOption,
  SupplierOption,
} from '@/types'

interface ProductDetail {
  id: number
  name: string
  description?: string | null
  price: number | string
  stock: number
  sales?: number
  mainImage?: string | null
  status?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  category?: { id: number; name?: string | null } | null
  supplier?: { id: number; companyName?: string | null } | null
}

const { state } = useAuthState()

const overview = ref<ProductOverview | null>(null)
const announcements = ref<Announcement[]>([])
const news = ref<NewsItem[]>([])
const hotProducts = ref<ProductSummary[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

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

const productLoading = ref(false)
const productError = ref<string | null>(null)
const productRows = ref<ProductSummary[]>([])
const categoryOptions = ref<CategoryOption[]>([])
const supplierOptions = ref<SupplierOption[]>([])

const productFilters = reactive({
  keyword: '',
  categoryId: 0,
  status: '',
})

const productPagination = reactive({
  page: 0,
  size: 8,
  total: 0,
})

const totalProductPages = computed(() =>
  productPagination.total > 0
    ? Math.ceil(productPagination.total / productPagination.size)
    : 0
)

const productPageIndicator = computed(() => {
  if (totalProductPages.value <= 0) {
    return '0/0'
  }
  return `${productPagination.page + 1}/${totalProductPages.value}`
})

const canGoProductPrevious = computed(
  () => totalProductPages.value > 0 && productPagination.page > 0
)

const canGoProductNext = computed(
  () =>
    totalProductPages.value > 0 && productPagination.page + 1 < totalProductPages.value
)

const PRODUCT_EVENT_NAME = 'silkmall:products:changed'

type ProductChangeAction = 'created' | 'updated' | 'deleted' | 'status-changed'

interface ProductChangeDetail {
  action: ProductChangeAction
  productId?: number | null
  source?: string
}

const productDialogOpen = ref(false)
const productViewOpen = ref(false)
const productFormError = ref<string | null>(null)
const productFormMessage = ref<string | null>(null)
const savingProduct = ref(false)
const deletingProductId = ref<number | null>(null)
const updatingStatusId = ref<number | null>(null)
const viewingProduct = ref<ProductDetail | null>(null)
const pendingExternalRefresh = ref(false)

let externalRefreshTimer: ReturnType<typeof setTimeout> | null = null

const productForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  price: '',
  stock: 0,
  status: 'ON_SALE',
  categoryId: 0,
  supplierId: 0,
  mainImage: '',
})

const statusOptions = [
  { value: 'ON_SALE', label: '在售' },
  { value: 'OFF_SALE', label: '未上架' },
]

function unwrapData<T>(payload: unknown): T | null {
  if (!payload || typeof payload !== 'object') {
    return null
  }

  const source = payload as Record<string, unknown>
  if ('data' in source && source.data && typeof source.data === 'object') {
    return source.data as T
  }

  return payload as T
}

async function loadOverview() {
  const response = await api.get<unknown>('/products/overview')
  const parsed = unwrapData<ProductOverview>(response.data)
  overview.value = parsed ?? null
}

async function loadHomeContent() {
  const response = await api.get<unknown>('/content/home')
  const parsed = unwrapData<HomepageContent>(response.data)
  if (!parsed) {
    announcements.value = []
    news.value = []
    hotProducts.value = []
    return
  }
  announcements.value = parsed.announcements
  news.value = parsed.news
  hotProducts.value = parsed.hotSales.slice(0, 5)
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
    console.warn('加载管理员钱包失败', err)
    walletBalance.value = null
    walletError.value = err instanceof Error ? err.message : '加载钱包信息失败'
  } finally {
    walletLoading.value = false
  }
}

function normaliseCategoryOptions(payload: unknown): CategoryOption[] {
  if (!Array.isArray(payload)) {
    return []
  }
  return payload
    .map((item) => {
      if (!item || typeof item !== 'object') return null
      const source = item as Record<string, unknown>
      const id = Number(source.id)
      if (!Number.isFinite(id)) return null
      const rawName = source.name ?? source['categoryName'] ?? `分类 ${id}`
      const name = typeof rawName === 'string' ? rawName.trim() : String(rawName ?? '').trim()
      return {
        id,
        name: name.length > 0 ? name : `分类 ${id}`,
      }
    })
    .filter((item): item is CategoryOption => item !== null)
    .sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
}

async function loadCategoryOptions() {
  const response = await api.get<unknown>('/categories/options')
  const parsed = unwrapData<CategoryOption[]>(response.data)
  categoryOptions.value = normaliseCategoryOptions(parsed ?? [])
}

function normaliseSupplierOptions(payload: unknown): SupplierOption[] {
  if (!Array.isArray(payload)) {
    return []
  }
  const options: SupplierOption[] = []
  for (const item of payload) {
    if (!item || typeof item !== 'object') continue
    const source = item as Record<string, unknown>
    const id = Number(source.id)
    if (!Number.isFinite(id)) continue
    const rawName = source.companyName ?? source.username ?? `供应商 ${id}`
    const companyName = typeof rawName === 'string' ? rawName.trim() : String(rawName ?? '').trim()
    const option: SupplierOption = {
      id,
      companyName: companyName.length > 0 ? companyName : `供应商 ${id}`,
    }
    if (typeof source.supplierLevel === 'string') {
      option.supplierLevel = source.supplierLevel
    } else if (source.supplierLevel === null) {
      option.supplierLevel = null
    }
    options.push(option)
  }
  return options.sort((a, b) => a.companyName.localeCompare(b.companyName, 'zh-CN'))
}

async function loadSupplierOptions() {
  const response = await api.get<unknown>('/suppliers/status/APPROVED')
  const parsed = unwrapData<unknown[]>(response.data)
  supplierOptions.value = normaliseSupplierOptions(parsed ?? [])
}

interface PagePayload<T> {
  content: T[]
  totalElements?: number
  number?: number
}

function resolvePage<T>(payload: unknown): PagePayload<T> {
  const fallback: PagePayload<T> = { content: [] }
  if (!payload || typeof payload !== 'object') {
    return fallback
  }

  const source = payload as Record<string, unknown>
  let pageLike: unknown = null

  if (Array.isArray(source.content)) {
    pageLike = source
  } else if (source.data && typeof source.data === 'object' && Array.isArray((source.data as Record<string, unknown>).content)) {
    pageLike = source.data
  }

  if (!pageLike || typeof pageLike !== 'object') {
    return fallback
  }

  const page = pageLike as Record<string, unknown>
  if (Array.isArray(page.content)) {
    fallback.content = page.content as T[]
  }
  if (typeof page.totalElements === 'number' && Number.isFinite(page.totalElements)) {
    fallback.totalElements = page.totalElements
  }
  if (typeof page.number === 'number' && Number.isFinite(page.number)) {
    fallback.number = page.number
  }

  return fallback
}

async function loadProducts(withSpinner = true) {
  if (withSpinner) productLoading.value = true
  productError.value = null
  try {
    const params: Record<string, unknown> = {
      page: productPagination.page,
      size: productPagination.size,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    }
    const keyword = productFilters.keyword.trim()
    if (keyword) params.keyword = keyword
    if (productFilters.categoryId > 0) params.categoryId = productFilters.categoryId
    if (productFilters.status) params.status = productFilters.status

    const response = await api.get<unknown>('/products/advanced-search', {
      params,
    })

    const page = resolvePage<ProductSummary>(response.data)
    productRows.value = Array.isArray(page.content) ? page.content : []
    productPagination.total =
      typeof page.totalElements === 'number' ? page.totalElements : productRows.value.length
    if (typeof page.number === 'number' && Number.isFinite(page.number)) {
      productPagination.page = page.number
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载商品列表失败'
    productError.value = message
    throw err instanceof Error ? err : new Error(message)
  } finally {
    if (withSpinner) productLoading.value = false
  }
}

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

function broadcastProductChange(detail: ProductChangeDetail) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent<ProductChangeDetail>(PRODUCT_EVENT_NAME, { detail }))
}

function scheduleExternalRefresh() {
  if (externalRefreshTimer !== null) return
  externalRefreshTimer = setTimeout(async () => {
    externalRefreshTimer = null
    try {
      await refreshProductsAndOverview()
    } catch (err) {
      console.error(err)
    }
  }, 250)
}

function handleExternalProductChange(event: Event) {
  const detail = (event as CustomEvent<ProductChangeDetail | undefined>).detail
  if (detail?.source === 'admin-overview') {
    return
  }
  if (typeof document !== 'undefined' && document.hidden) {
    pendingExternalRefresh.value = true
    return
  }
  scheduleExternalRefresh()
}

function handleVisibilityChange() {
  if (typeof document === 'undefined') return
  if (!document.hidden && pendingExternalRefresh.value) {
    pendingExternalRefresh.value = false
    scheduleExternalRefresh()
  }
}

async function initProductManagement() {
  productLoading.value = true
  productError.value = null
  try {
    await Promise.all([loadCategoryOptions(), loadSupplierOptions()])
    await loadProducts(false)
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载商品概览失败'
    productError.value = message
  } finally {
    productLoading.value = false
  }
}

function resetRedeemFeedback() {
  redeemMessage.value = null
  redeemError.value = null
}

async function redeemWallet() {
  resetRedeemFeedback()
  if (!state.user) {
    redeemError.value = '请先登录管理员账号'
    return
  }
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
    redeemCodeInput.value = ''
    redeemMessage.value = '兑换成功，余额已更新'
    walletError.value = null
  } catch (err) {
    console.warn('管理员钱包兑换失败', err)
    redeemError.value = err instanceof Error ? err.message : '兑换失败，请稍后再试'
  } finally {
    redeeming.value = false
  }
}

function resetProductForm() {
  productForm.id = null
  productForm.name = ''
  productForm.description = ''
  productForm.price = ''
  productForm.stock = 0
  productForm.status = 'ON_SALE'
  productForm.categoryId = 0
  productForm.supplierId = 0
  productForm.mainImage = ''
  productFormError.value = null
  productFormMessage.value = null
}

function resetProductFilters() {
  productFilters.keyword = ''
  productFilters.categoryId = 0
  productFilters.status = ''
  productPagination.page = 0
  loadProducts().catch(() => {})
}

function applyProductFilters() {
  productPagination.page = 0
  loadProducts().catch(() => {})
}

function changeProductPage(target: number) {
  if (target < 0 || target === productPagination.page) return
  if (totalProductPages.value && target >= totalProductPages.value) return
  productPagination.page = target
  loadProducts().catch(() => {})
}

function cancelProductDialog() {
  productDialogOpen.value = false
  resetProductForm()
}

async function openProductDialog(product?: ProductSummary) {
  productFormError.value = null
  productFormMessage.value = null
  try {
    await Promise.all([loadCategoryOptions(), loadSupplierOptions()])
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载商品数据失败'
    window.alert(message)
    return
  }

  if (product) {
    try {
      const response = await api.get<unknown>(`/products/${product.id}`)
      const detail = unwrapData<ProductDetail>(response.data)
      if (!detail) {
        throw new Error('加载商品详情失败')
      }
      productForm.id = detail.id
      productForm.name = detail.name ?? ''
      productForm.description = detail.description ?? ''
      if (typeof detail.price === 'number') {
        productForm.price = detail.price.toString()
      } else {
        const parsed = Number(detail.price)
        productForm.price = Number.isFinite(parsed) ? parsed.toString() : ''
      }
      const detailRecord = detail as unknown as Record<string, unknown>
      const stockValue = Number(detailRecord.stock)
      productForm.stock = Number.isFinite(stockValue) ? stockValue : 0
      productForm.status = detail.status ?? 'OFF_SALE'
      productForm.categoryId = detail.category?.id ?? 0
      productForm.supplierId = detail.supplier?.id ?? 0
      productForm.mainImage = detail.mainImage ?? ''
    } catch (err) {
      const message = err instanceof Error ? err.message : '加载商品详情失败'
      window.alert(message)
      return
    }
  } else {
    resetProductForm()
  }

  productDialogOpen.value = true
}

async function saveProduct() {
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

  if (!productForm.supplierId) {
    productFormError.value = '请选择商品所属供应商'
    return
  }

  const stock = Number(productForm.stock)
  if (!Number.isInteger(stock) || stock < 0) {
    productFormError.value = '库存必须为非负整数'
    return
  }

  const payload: Record<string, unknown> = {
    name,
    description: productForm.description.trim() || null,
    price,
    stock,
    status: productForm.status,
    mainImage: productForm.mainImage.trim() || null,
    supplier: { id: productForm.supplierId },
  }

  if (productForm.categoryId > 0) {
    payload.category = { id: productForm.categoryId }
  }

  savingProduct.value = true
  productFormError.value = null

  try {
    if (productForm.id) {
      await api.put(`/products/${productForm.id}`, payload)
      productFormMessage.value = '商品信息已更新'
      broadcastProductChange({
        action: 'updated',
        productId: productForm.id,
        source: 'admin-overview',
      })
    } else {
      const { data } = await api.post('/products', payload)
      productFormMessage.value = '商品已创建并保存'
      broadcastProductChange({
        action: 'created',
        productId: extractNumericId(data),
        source: 'admin-overview',
      })
    }

    await refreshProductsAndOverview()
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
    broadcastProductChange({
      action: 'deleted',
      productId,
      source: 'admin-overview',
    })
    await refreshProductsAndOverview()
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除商品失败'
    window.alert(message)
  } finally {
    deletingProductId.value = null
  }
}

async function changeProductStatus(productId: number, nextStatus: 'ON_SALE' | 'OFF_SALE') {
  updatingStatusId.value = productId
  try {
    if (nextStatus === 'ON_SALE') {
      await api.put(`/products/${productId}/on-sale`)
    } else {
      await api.put(`/products/${productId}/off-sale`)
    }
    broadcastProductChange({
      action: 'status-changed',
      productId,
      source: 'admin-overview',
    })
    await refreshProductsAndOverview()
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新商品状态失败'
    window.alert(message)
  } finally {
    updatingStatusId.value = null
  }
}

async function refreshProductsAndOverview() {
  try {
    await loadOverview()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载管理数据失败'
  }
  productPagination.page = 0
  await loadProducts().catch(() => {})
  await loadWallet()
}

async function openProductView(product: ProductSummary) {
  try {
    const response = await api.get<unknown>(`/products/${product.id}`)
    const detail = unwrapData<ProductDetail>(response.data)
    if (!detail) {
      throw new Error('加载商品详情失败')
    }
    const detailRecord = detail as unknown as Record<string, unknown>
    const stockValue = Number(detailRecord.stock)
    const salesValue = Number(detailRecord.sales)
    viewingProduct.value = {
      ...detail,
      stock: Number.isFinite(stockValue) ? stockValue : 0,
      sales: Number.isFinite(salesValue) ? salesValue : undefined,
    }
    productViewOpen.value = true
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载商品详情失败'
    window.alert(message)
  }
}

function closeProductView() {
  productViewOpen.value = false
  viewingProduct.value = null
}

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([loadOverview(), loadHomeContent(), loadWallet()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载管理数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
  initProductManagement()
  if (typeof window !== 'undefined') {
    window.addEventListener(PRODUCT_EVENT_NAME, handleExternalProductChange as EventListener)
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener(PRODUCT_EVENT_NAME, handleExternalProductChange as EventListener)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  }
  if (externalRefreshTimer !== null) {
    clearTimeout(externalRefreshTimer)
    externalRefreshTimer = null
  }
  pendingExternalRefresh.value = false
})

function formatCurrency(amount?: number | string | null) {
  const numeric = typeof amount === 'string' ? Number(amount) : amount
  if (typeof numeric !== 'number' || Number.isNaN(numeric)) {
    return '¥0.00'
  }
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(numeric)
}

function formatStatus(status?: string | null) {
  if (!status) return '未知'
  return status === 'ON_SALE' ? '在售' : '未上架'
}

function formatDate(value?: string | Date | null) {
  if (!value) return '—'
  const date = typeof value === 'string' ? new Date(value) : value
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatNumber(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '0'
  return new Intl.NumberFormat('zh-CN').format(value)
}
</script>

<template>
  <section class="admin-shell">
    <header class="admin-header">
      <div>
        <h1>平台运营总览</h1>
        <p>掌握商品、库存、用户互动与资讯发布情况，及时调整销售策略。</p>
      </div>
    </header>

    <div v-if="loading" class="placeholder">正在加载平台数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
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

      <section class="panel metrics" aria-labelledby="metrics-title">
        <div class="panel-title" id="metrics-title">核心指标</div>
        <div class="metric-grid">
          <div>
            <span>商品总数</span>
            <strong>{{ formatNumber(overview?.totalProducts) }}</strong>
          </div>
          <div>
            <span>上架商品</span>
            <strong>{{ formatNumber(overview?.onSaleProducts) }}</strong>
          </div>
          <div>
            <span>下架商品</span>
            <strong>{{ formatNumber(overview?.offSaleProducts) }}</strong>
          </div>
          <div>
            <span>总库存量</span>
            <strong>{{ formatNumber(overview?.totalStock) }}</strong>
          </div>
          <div>
            <span>累计销量</span>
            <strong>{{ formatNumber(overview?.totalSalesVolume) }}</strong>
          </div>
        </div>
      </section>

      <section class="panel product-admin" aria-labelledby="admin-product-title">
        <header class="product-admin-header">
          <div>
            <div class="panel-title" id="admin-product-title">商品概览</div>
            <p class="panel-subtitle">快速查看、搜索并管理全站商品，新增后立即更新统计。</p>
          </div>
          <button type="button" class="primary" @click="openProductDialog()">新增商品</button>
        </header>

        <form class="product-filters" @submit.prevent="applyProductFilters">
          <label>
            <span>关键字</span>
            <input v-model.trim="productFilters.keyword" type="text" placeholder="商品名称或描述" />
          </label>
          <label>
            <span>分类</span>
            <select v-model.number="productFilters.categoryId">
              <option :value="0">全部分类</option>
              <option v-for="option in categoryOptions" :key="option.id" :value="option.id">
                {{ option.name }}
              </option>
            </select>
          </label>
          <label>
            <span>状态</span>
            <select v-model="productFilters.status">
              <option value="">全部状态</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <div class="filter-actions">
            <button type="submit" class="primary">搜索</button>
            <button type="button" class="secondary" @click="resetProductFilters">重置</button>
          </div>
        </form>

        <div v-if="productError" class="product-placeholder is-error">{{ productError }}</div>
        <div v-else-if="productLoading" class="product-placeholder">正在加载商品列表…</div>
        <div v-else>
          <div v-if="productRows.length" class="table-wrapper">
            <table class="product-table">
              <thead>
                <tr>
                  <th scope="col">商品名称</th>
                  <th scope="col">分类</th>
                  <th scope="col">供应商</th>
                  <th scope="col">售价</th>
                  <th scope="col">库存</th>
                  <th scope="col">销量</th>
                  <th scope="col">状态</th>
                  <th scope="col">创建时间</th>
                  <th scope="col">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in productRows" :key="item.id">
                  <td class="name-cell">
                    <strong>{{ item.name }}</strong>
                    <p v-if="item.description" class="description">{{ item.description }}</p>
                  </td>
                  <td>{{ item.categoryName ?? '—' }}</td>
                  <td>{{ item.supplierName ?? '—' }}</td>
                  <td>{{ formatCurrency(item.price) }}</td>
                  <td>{{ formatNumber(item.stock) }}</td>
                  <td>{{ formatNumber(item.sales) }}</td>
                  <td><span class="status-pill">{{ formatStatus(item.status) }}</span></td>
                  <td>{{ formatDate(item.createdAt) }}</td>
                  <td class="product-actions">
                    <button type="button" class="link-button" @click="openProductView(item)">查看</button>
                    <button type="button" class="link-button" @click="openProductDialog(item)">编辑</button>
                    <button
                      type="button"
                      class="link-button"
                      :class="{ danger: item.status === 'ON_SALE' }"
                      @click="changeProductStatus(item.id, item.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE')"
                      :disabled="updatingStatusId === item.id"
                    >
                      {{ item.status === 'ON_SALE' ? '下架' : '上架' }}
                    </button>
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
          <p v-else class="product-placeholder">暂无商品记录，请尝试调整筛选条件或新增商品。</p>

          <nav
            v-if="totalProductPages > 0"
            class="product-pagination-footer"
            aria-label="商品分页导航"
          >
            <span class="pagination-status">共 {{ formatNumber(productPagination.total) }} 件商品</span>
            <div class="pagination-controls">
              <button
                type="button"
                class="pager-button"
                :disabled="!canGoProductPrevious || productLoading"
                @click="changeProductPage(productPagination.page - 1)"
              >
                &lt;
              </button>
              <span class="page-indicator">{{ productPageIndicator }}</span>
              <button
                type="button"
                class="pager-button"
                :disabled="!canGoProductNext || productLoading"
                @click="changeProductPage(productPagination.page + 1)"
              >
                &gt;
              </button>
            </div>
          </nav>
        </div>
      </section>

      <section class="panel hot-products" aria-labelledby="hot-title">
        <div class="panel-title" id="hot-title">热销商品排行</div>
        <table v-if="hotProducts.length">
          <thead>
            <tr>
              <th scope="col">商品</th>
              <th scope="col">销量</th>
              <th scope="col">库存</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in hotProducts" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ formatNumber(item.sales) }}</td>
              <td>{{ formatNumber(item.stock) }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty">暂无热销数据，请提醒供应商及时完善商品。</p>
      </section>

      <section class="panel announcements" aria-labelledby="admin-announcement">
        <div class="panel-title" id="admin-announcement">公告管理</div>
        <ul class="announcement-list">
          <li v-for="item in announcements" :key="item.id">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
            <span class="category">{{ item.category }}</span>
          </li>
        </ul>
      </section>

      <section class="panel news" aria-labelledby="news-title">
        <div class="panel-title" id="news-title">行业资讯</div>
        <ul class="news-list">
          <li v-for="item in news" :key="item.id">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.summary }}</p>
            </div>
            <span class="source">{{ item.source }}</span>
          </li>
        </ul>
      </section>
    </template>

    <form v-if="productDialogOpen" class="product-dialog" @submit.prevent="saveProduct">
      <div class="dialog-surface">
        <header class="dialog-header">
          <h2>{{ productForm.id ? '编辑商品' : '新增商品' }}</h2>
          <button type="button" class="ghost" @click="cancelProductDialog">关闭</button>
        </header>

        <div class="dialog-grid">
          <label>
            <span>商品名称</span>
            <input v-model="productForm.name" type="text" placeholder="请输入商品名称" />
          </label>
          <label>
            <span>所属分类</span>
            <select v-model.number="productForm.categoryId">
              <option :value="0">未分类</option>
              <option v-for="option in categoryOptions" :key="option.id" :value="option.id">
                {{ option.name }}
              </option>
            </select>
          </label>
          <label>
            <span>供应商</span>
            <select v-model.number="productForm.supplierId">
              <option :value="0">请选择供应商</option>
              <option v-for="option in supplierOptions" :key="option.id" :value="option.id">
                {{ option.companyName }}
              </option>
            </select>
          </label>
          <label>
            <span>商品状态</span>
            <select v-model="productForm.status">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            <span>商品价格</span>
            <input v-model="productForm.price" type="number" min="0" step="0.01" placeholder="¥0.00" />
          </label>
          <label>
            <span>库存数量</span>
            <input v-model.number="productForm.stock" type="number" min="0" step="1" />
          </label>
          <label class="full-width">
            <span>主图地址</span>
            <input v-model="productForm.mainImage" type="text" placeholder="请输入图片链接（可选）" />
          </label>
          <label class="full-width">
            <span>商品描述</span>
            <textarea v-model="productForm.description" rows="4" placeholder="请输入商品描述"></textarea>
          </label>
        </div>

        <div v-if="productFormError" class="form-feedback is-error">{{ productFormError }}</div>
        <div v-if="productFormMessage" class="form-feedback is-success">{{ productFormMessage }}</div>

        <footer class="dialog-footer">
          <button type="submit" class="primary" :disabled="savingProduct">
            {{ savingProduct ? '保存中…' : '保存商品' }}
          </button>
        </footer>
      </div>
    </form>

    <div v-if="productViewOpen && viewingProduct" class="product-view-dialog" role="dialog" aria-modal="true">
      <div class="dialog-surface">
        <header class="dialog-header">
          <h2>商品详情</h2>
          <button type="button" class="ghost" @click="closeProductView">关闭</button>
        </header>
        <dl class="product-details">
          <div>
            <dt>商品名称</dt>
            <dd>{{ viewingProduct?.name }}</dd>
          </div>
          <div>
            <dt>分类</dt>
            <dd>{{ viewingProduct?.category?.name ?? '—' }}</dd>
          </div>
          <div>
            <dt>供应商</dt>
            <dd>{{ viewingProduct?.supplier?.companyName ?? '—' }}</dd>
          </div>
          <div>
            <dt>售价</dt>
            <dd>{{ formatCurrency(Number(viewingProduct?.price)) }}</dd>
          </div>
          <div>
            <dt>库存</dt>
            <dd>{{ formatNumber(viewingProduct?.stock) }}</dd>
          </div>
          <div>
            <dt>销量</dt>
            <dd>{{ formatNumber(viewingProduct?.sales ?? 0) }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ formatStatus(viewingProduct?.status) }}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{{ formatDate(viewingProduct?.createdAt) }}</dd>
          </div>
          <div>
            <dt>最近更新</dt>
            <dd>{{ formatDate(viewingProduct?.updatedAt) }}</dd>
          </div>
          <div class="full-width">
            <dt>商品描述</dt>
            <dd>{{ viewingProduct?.description || '—' }}</dd>
          </div>
          <div v-if="viewingProduct?.mainImage" class="full-width">
            <dt>商品主图</dt>
            <dd><img :src="viewingProduct.mainImage" alt="商品主图" /></dd>
          </div>
        </dl>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-shell {
  display: grid;
  gap: 2.5rem;
}

.admin-header {
  padding: 2.5rem;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.12), rgba(234, 179, 8, 0.12));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

.admin-header h1 {
  font-size: 2.1rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.admin-header p {
  color: rgba(30, 41, 59, 0.65);
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
  gap: 1.5rem;
}

.panel-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: rgba(30, 41, 59, 0.78);
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

.panel-subtitle {
  margin-top: 0.35rem;
  color: rgba(30, 41, 59, 0.6);
}

.product-admin-header {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
  align-items: flex-start;
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

.ghost {
  border: none;
  background: none;
  color: rgba(30, 41, 59, 0.65);
  font-weight: 600;
  cursor: pointer;
}

.product-filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  align-items: end;
}

.product-filters label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
}

.product-filters input,
.product-filters select {
  padding: 0.55rem 0.65rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 0.75rem;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.product-placeholder {
  padding: 1.5rem;
  border-radius: 1rem;
  text-align: center;
  color: rgba(15, 23, 42, 0.55);
  background: rgba(37, 99, 235, 0.08);
}

.product-placeholder.is-error {
  background: rgba(248, 113, 113, 0.12);
  color: #7f1d1d;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
}

.metric-grid div {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
  padding: 1.1rem;
  border: 1px solid rgba(249, 115, 22, 0.12);
}

.metric-grid span {
  display: block;
  font-size: 0.85rem;
  color: rgba(30, 41, 59, 0.6);
}

.metric-grid strong {
  font-size: 1.4rem;
  font-weight: 700;
}

.hot-products table {
  width: 100%;
  border-collapse: collapse;
}

.table-wrapper {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
}

.product-table thead {
  background: rgba(15, 23, 42, 0.04);
  color: rgba(15, 23, 42, 0.6);
}

.product-table th,
.product-table td {
  padding: 0.75rem 0.5rem;
  text-align: left;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.product-table tbody tr:nth-child(odd) {
  background: rgba(59, 130, 246, 0.05);
}

.name-cell strong {
  display: block;
}

.name-cell .description {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.18);
  color: #0ea5e9;
  font-weight: 600;
}

.product-actions {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.link-button {
  border: none;
  background: none;
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.link-button.danger {
  color: #dc2626;
}

.link-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.product-pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem 1.5rem;
  margin-top: 1.25rem;
}

.pagination-status {
  color: rgba(15, 23, 42, 0.65);
  font-weight: 600;
}

.pagination-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}

.pager-button {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.2);
  background: rgba(148, 163, 184, 0.12);
  color: rgba(15, 23, 42, 0.75);
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pager-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  min-width: 3.5rem;
  text-align: center;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
}

.product-dialog,
.product-view-dialog {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  z-index: 40;
}

.dialog-surface {
  background: #fff;
  border-radius: 20px;
  padding: 1.8rem;
  width: min(720px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.2);
  display: grid;
  gap: 1.5rem;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.dialog-header h2 {
  font-size: 1.4rem;
  font-weight: 700;
}

.dialog-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.dialog-grid label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
}

.dialog-grid input,
.dialog-grid select,
.dialog-grid textarea {
  padding: 0.55rem 0.65rem;
  border: 1px solid rgba(15, 23, 42, 0.14);
  border-radius: 0.75rem;
  font-size: 0.95rem;
}

.dialog-grid .full-width {
  grid-column: 1 / -1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.form-feedback {
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  font-weight: 600;
}

.form-feedback.is-error {
  background: rgba(248, 113, 113, 0.12);
  color: #7f1d1d;
}

.form-feedback.is-success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.product-details {
  display: grid;
  gap: 0.9rem;
}

.product-details div {
  display: grid;
  gap: 0.25rem;
  grid-template-columns: max-content 1fr;
  align-items: flex-start;
}

.product-details .full-width {
  grid-template-columns: 1fr;
}

.product-details dt {
  font-weight: 600;
  color: rgba(15, 23, 42, 0.6);
}

.product-details dd {
  margin: 0;
  color: rgba(15, 23, 42, 0.9);
  word-break: break-word;
}

.product-details img {
  max-width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.hot-products th,
.hot-products td {
  padding: 0.75rem 0.5rem;
  text-align: left;
}

.hot-products thead {
  color: rgba(30, 41, 59, 0.55);
}

.empty {
  color: rgba(30, 41, 59, 0.6);
}

.announcement-list,
.news-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}

.announcement-list li,
.news-list li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.announcement-list strong,
.news-list strong {
  display: block;
  margin-bottom: 0.25rem;
}

.announcement-list p,
.news-list p {
  color: rgba(30, 41, 59, 0.6);
}

.category,
.source {
  font-weight: 600;
  color: #f97316;
}

@media (max-width: 768px) {
  .announcement-list li,
  .news-list li {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
