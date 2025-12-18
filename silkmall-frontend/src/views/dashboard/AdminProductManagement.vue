<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import type {
  CategoryOption,
  PageResponse,
  ProductOverview,
  ProductSummary,
  SupplierOption,
} from '@/types'

interface ProductDetail {
  id: number
  name: string
  description?: string | null
  price: number | string
  unit?: string | null
  stock: number
  sales: number
  mainImage?: string | null
  status: string
  createdAt?: string | null
  updatedAt?: string | null
  category?: { id: number; name?: string | null } | null
  supplier?: { id: number; companyName?: string | null } | null
}

const PRODUCT_EVENT_NAME = 'silkmall:products:changed'

type ProductChangeAction = 'created' | 'updated' | 'deleted' | 'status-changed'

interface ProductChangeDetail {
  action: ProductChangeAction
  productId?: number | null
  source?: string
}

const loading = ref(true)
const tableLoading = ref(false)
const error = ref<string | null>(null)

const overview = ref<ProductOverview | null>(null)
const products = ref<ProductSummary[]>([])
const categories = ref<CategoryOption[]>([])
const suppliers = ref<SupplierOption[]>([])

const PAGE_SIZE = 8
const currentPage = ref(1)
const totalPages = ref(0)
const totalElements = ref(0)

const canGoPreviousPage = computed(() => totalPages.value > 0 && currentPage.value > 1)
const canGoNextPage = computed(() => totalPages.value > 0 && currentPage.value < totalPages.value)
const pageIndicator = computed(() => {
  if (totalPages.value <= 0) {
    return '0/0'
  }
  return `${currentPage.value}/${totalPages.value}`
})

const filters = reactive({
  keyword: '',
  categoryId: 0,
  supplierId: 0,
  status: '',
})

const productDialogOpen = ref(false)
const productFormError = ref<string | null>(null)
const productFormMessage = ref<string | null>(null)
const savingProduct = ref(false)
const deletingProductId = ref<number | null>(null)
const statusUpdatingId = ref<number | null>(null)
const productViewOpen = ref(false)
const viewingProduct = ref<ProductDetail | null>(null)

const productForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  price: '',
  unit: '',
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

const unitOptions = ['件', '条', '个', '箱']
const sizeOptions = ['S', 'M', 'L', 'XL', '2XL', '3XL']

const sizeAllocationOpen = ref(false)
const sizeAllocations = reactive<Record<string, number>>(
  sizeOptions.reduce((result, size) => {
    result[size] = 0
    return result
  }, {} as Record<string, number>),
)

const sizeAllocationTotal = computed(() =>
  sizeOptions.reduce((total, size) => total + (Number(sizeAllocations[size]) || 0), 0),
)

const sizeAllocationRemaining = computed(() => Number(productForm.stock || 0) - sizeAllocationTotal.value)

function formatCurrency(amount?: number | null) {
  if (typeof amount !== 'number' || Number.isNaN(amount)) {
    return '¥0.00'
  }
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(amount)
}

function formatNumber(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '0'
  return new Intl.NumberFormat('zh-CN').format(value)
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

function resetProductForm() {
  productForm.id = null
  productForm.name = ''
  productForm.description = ''
  productForm.price = ''
  productForm.unit = ''
  productForm.stock = 0
  productForm.status = 'ON_SALE'
  productForm.categoryId = 0
  productForm.supplierId = 0
  productForm.mainImage = ''
  productFormError.value = null
  productFormMessage.value = null
  sizeAllocationOpen.value = false
  resetSizeAllocations(0)
}

function resetSizeAllocations(stock?: number) {
  const stockValue = Number.isFinite(stock) ? Math.max(0, Number(stock)) : 0
  sizeOptions.forEach((size, index) => {
    sizeAllocations[size] = index === 0 ? stockValue : 0
  })
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

async function loadOverview() {
  const { data } = await api.get<ProductOverview>('/products/overview')
  overview.value = data
}

async function loadCategories() {
  const { data } = await api.get<CategoryOption[]>('/categories/options')
  categories.value = Array.isArray(data)
    ? data
        .map((item) => ({
          id: Number(item.id),
          name: typeof item.name === 'string' ? item.name.trim() : String(item.name ?? '').trim(),
        }))
        .filter((item) => Number.isFinite(item.id) && item.name.length > 0)
        .sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
    : []
}

function normaliseSuppliers(payload: unknown): SupplierOption[] {
  if (!Array.isArray(payload)) {
    return []
  }
  const options: SupplierOption[] = []
  for (const item of payload) {
    if (!item || typeof item !== 'object') continue
    const source = item as Record<string, unknown>
    const id = Number(source.id)
    if (!Number.isFinite(id)) continue
    const companyNameRaw = source.companyName ?? source.username ?? `供应商 ${id}`
    const companyName =
      typeof companyNameRaw === 'string'
        ? companyNameRaw.trim()
        : String(companyNameRaw ?? '').trim()
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

async function loadSuppliers() {
  const { data } = await api.get<unknown[]>('/suppliers/status/APPROVED')
  suppliers.value = normaliseSuppliers(data)
}

async function loadProducts(targetPage?: number) {
  const previousPage = currentPage.value
  tableLoading.value = true
  try {
    const requestedPage =
      typeof targetPage === 'number' && Number.isFinite(targetPage)
        ? Math.floor(targetPage)
        : currentPage.value || 1
    const safeRequestedPage = requestedPage > 0 ? requestedPage : 1

    if (safeRequestedPage !== currentPage.value) {
      currentPage.value = safeRequestedPage
    }

    const params: Record<string, unknown> = {
      page: safeRequestedPage - 1,
      size: PAGE_SIZE,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    }
    const keyword = filters.keyword.trim()
    if (keyword) params.keyword = keyword
    if (filters.categoryId > 0) params.categoryId = filters.categoryId
    if (filters.supplierId > 0) params.supplierId = filters.supplierId
    if (filters.status) params.status = filters.status

    const { data } = await api.get<PageResponse<ProductSummary>>('/products/advanced-search', {
      params,
    })

    const list = Array.isArray(data?.content) ? data.content : []
    products.value = list

    const rawTotalPages = Number((data as PageResponse<ProductSummary> | undefined)?.totalPages)
    const computedTotalPages = Number.isFinite(rawTotalPages) ? Math.max(0, Math.trunc(rawTotalPages)) : 0
    totalPages.value = computedTotalPages > 0 ? computedTotalPages : list.length > 0 ? 1 : 0

    const rawTotalElements = Number((data as PageResponse<ProductSummary> | undefined)?.totalElements)
    totalElements.value = Number.isFinite(rawTotalElements) && rawTotalElements >= 0 ? rawTotalElements : list.length

    if (totalPages.value <= 0) {
      currentPage.value = list.length > 0 ? 1 : 0
      return
    }

    const rawServerPage = Number((data as PageResponse<ProductSummary> | undefined)?.number)
    const serverPage = Number.isFinite(rawServerPage) ? Math.max(1, Math.trunc(rawServerPage) + 1) : safeRequestedPage
    currentPage.value = Math.min(serverPage, totalPages.value)

    if (safeRequestedPage > totalPages.value && list.length === 0) {
      await loadProducts(totalPages.value)
      return
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载商品失败'
    currentPage.value = previousPage
    if (loading.value) {
      throw err instanceof Error ? err : new Error(message)
    }
    window.alert(message)
  } finally {
    tableLoading.value = false
  }
}

async function openProductView(product: ProductSummary) {
  try {
    const { data } = await api.get<ProductDetail>(`/products/${product.id}`)
    const detailRecord = data as unknown as Record<string, unknown>
    const stockValue = Number(detailRecord.stock)
    const salesValue = Number(detailRecord.sales)
    viewingProduct.value = {
      ...data,
      stock: Number.isFinite(stockValue) ? stockValue : 0,
      sales: Number.isFinite(salesValue) ? salesValue : 0,
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
    await Promise.all([loadOverview(), loadCategories(), loadSuppliers()])
    await loadProducts()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载商品管理数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

function applyFilters() {
  currentPage.value = 1
  loadProducts(1)
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = 0
  filters.supplierId = 0
  filters.status = ''
  currentPage.value = 1
  loadProducts(1)
}

async function openProductForm(product?: ProductSummary) {
  productFormError.value = null
  productFormMessage.value = null
  await Promise.all([loadCategories(), loadSuppliers()])

  if (product) {
    const { data } = await api.get<ProductDetail>(`/products/${product.id}`)
    productForm.id = data.id
    productForm.name = data.name ?? ''
    productForm.description = data.description ?? ''
    if (typeof data.price === 'number') {
      productForm.price = data.price.toString()
    } else {
      const parsed = Number(data.price)
      productForm.price = Number.isFinite(parsed) ? parsed.toString() : ''
    }
    const detailRecord = data as unknown as Record<string, unknown>
    const rawUnit = typeof detailRecord.unit === 'string' ? detailRecord.unit : data.unit
    productForm.unit = rawUnit ?? ''
    const stockValue = Number(detailRecord.stock)
    productForm.stock = Number.isFinite(stockValue) ? stockValue : 0
    productForm.status = data.status ?? 'OFF_SALE'
    productForm.categoryId = data.category?.id ?? 0
    productForm.supplierId = data.supplier?.id ?? 0
    productForm.mainImage = data.mainImage ?? ''
  } else {
    resetProductForm()
  }

  sizeAllocationOpen.value = false
  resetSizeAllocations(productForm.stock)
  productDialogOpen.value = true
}

function cancelProductForm() {
  productDialogOpen.value = false
  resetProductForm()
}

function openSizeAllocation() {
  if (!sizeAllocationOpen.value) {
    resetSizeAllocations(productForm.stock)
  }
  sizeAllocationOpen.value = true
}

function closeSizeAllocation() {
  sizeAllocationOpen.value = false
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

  const unit = productForm.unit.trim()
  if (!unit) {
    productFormError.value = '请填写商品单位'
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

  if (sizeAllocationOpen.value && stock > 0 && sizeAllocationTotal.value !== stock) {
    productFormError.value = '尺码数量未分配完成，请确保总计等于库存数量'
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
        source: 'admin-product-management',
      })
    } else {
      const { data } = await api.post('/products', payload)
      productFormMessage.value = '商品已创建并保存'
      broadcastProductChange({
        action: 'created',
        productId: extractNumericId(data),
        source: 'admin-product-management',
      })
    }

    await Promise.all([loadProducts(), loadOverview()])
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
      source: 'admin-product-management',
    })
    await Promise.all([loadProducts(), loadOverview()])
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除商品失败'
    window.alert(message)
  } finally {
    deletingProductId.value = null
  }
}

async function changeProductStatus(productId: number, nextStatus: 'ON_SALE' | 'OFF_SALE') {
  statusUpdatingId.value = productId
  try {
    if (nextStatus === 'ON_SALE') {
      await api.put(`/products/${productId}/on-sale`)
    } else {
      await api.put(`/products/${productId}/off-sale`)
    }
    broadcastProductChange({
      action: 'status-changed',
      productId,
      source: 'admin-product-management',
    })
    await Promise.all([loadProducts(), loadOverview()])
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新商品状态失败'
    window.alert(message)
  } finally {
    statusUpdatingId.value = null
  }
}

function goToPreviousPage() {
  if (!canGoPreviousPage.value) return
  loadProducts(currentPage.value - 1)
}

function goToNextPage() {
  if (!canGoNextPage.value) return
  loadProducts(currentPage.value + 1)
}
</script>

<template>
  <section class="product-admin-shell">
    <header class="page-header">
      <div>
        <h1>商品管理中心</h1>
        <p>管理员可快速查看全站商品表现，并执行新增、编辑、删除与搜索操作。</p>
      </div>
      <button type="button" class="primary" @click="openProductForm()">新增商品</button>
    </header>

    <div v-if="loading" class="placeholder">正在加载商品数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
      <section class="panel metrics" aria-labelledby="metric-title">
        <div class="panel-title" id="metric-title">平台商品概览</div>
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

      <section class="panel filters" aria-labelledby="filter-title">
        <div class="panel-title" id="filter-title">筛选条件</div>
        <div class="filter-grid">
          <label>
            <span>关键字</span>
            <input v-model.trim="filters.keyword" type="text" placeholder="商品名称、描述" />
          </label>
          <label>
            <span>分类</span>
            <select v-model.number="filters.categoryId">
              <option :value="0">全部分类</option>
              <option v-for="option in categories" :key="option.id" :value="option.id">
                {{ option.name }}
              </option>
            </select>
          </label>
          <label>
            <span>供应商</span>
            <select v-model.number="filters.supplierId">
              <option :value="0">全部供应商</option>
              <option v-for="option in suppliers" :key="option.id" :value="option.id">
                {{ option.companyName }}
              </option>
            </select>
          </label>
          <label>
            <span>状态</span>
            <select v-model="filters.status">
              <option value="">全部状态</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
        </div>
        <div class="filter-actions">
          <button type="button" class="primary" @click="applyFilters">应用筛选</button>
          <button type="button" class="secondary" @click="resetFilters">重置条件</button>
        </div>
      </section>

      <section class="panel table" aria-labelledby="product-table">
        <div class="panel-title-row">
          <div class="panel-title" id="product-table">商品列表</div>
          <span v-if="tableLoading" class="table-status">数据刷新中…</span>
        </div>
        <template v-if="products.length">
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th scope="col">商品名称</th>
                  <th scope="col">所属分类</th>
                  <th scope="col">供应商</th>
                  <th scope="col">售价</th>
                  <th scope="col">计量单位</th>
                  <th scope="col">库存</th>
                  <th scope="col">销量</th>
                  <th scope="col">状态</th>
                  <th scope="col">上架时间</th>
                  <th scope="col">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in products" :key="item.id">
                  <td class="name-cell">
                    <strong>{{ item.name }}</strong>
                    <p v-if="item.description" class="description">{{ item.description }}</p>
                  </td>
                  <td>{{ item.categoryName ?? '—' }}</td>
                  <td>{{ item.supplierName ?? '—' }}</td>
                  <td>{{ formatCurrency(item.price) }}</td>
                  <td>{{ item.unit ?? '—' }}</td>
                  <td>{{ formatNumber(item.stock) }}</td>
                  <td>{{ formatNumber(item.sales) }}</td>
                  <td><span class="status-pill">{{ formatStatus(item.status) }}</span></td>
                  <td>{{ formatDate(item.createdAt as string | undefined) }}</td>
                  <td class="actions">
                    <button type="button" class="link-button" @click="openProductView(item)">查看</button>
                    <button type="button" class="link-button" @click="openProductForm(item)">编辑</button>
                    <button
                      type="button"
                      class="link-button"
                      :class="{ danger: item.status === 'ON_SALE' }"
                      @click="changeProductStatus(item.id, item.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE')"
                      :disabled="statusUpdatingId === item.id"
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
          <div v-if="totalPages > 0" class="table-footer">
            <span class="pagination-status">共 {{ formatNumber(totalElements) }} 件商品</span>
            <div class="pagination-controls">
              <button
                type="button"
                class="pager-button"
                :disabled="!canGoPreviousPage || tableLoading"
                @click="goToPreviousPage"
              >
                &lt;
              </button>
              <span class="page-indicator">{{ pageIndicator }}</span>
              <button
                type="button"
                class="pager-button"
                :disabled="!canGoNextPage || tableLoading"
                @click="goToNextPage"
              >
                &gt;
              </button>
            </div>
          </div>
        </template>
        <p v-else class="empty">暂无商品记录，请尝试调整筛选条件或新增商品。</p>
      </section>
    </template>

    <form v-if="productDialogOpen" class="product-form" @submit.prevent="saveProduct">
      <div class="form-shell">
        <header>
          <h2>{{ productForm.id ? '编辑商品' : '新增商品' }}</h2>
          <button type="button" class="ghost" @click="cancelProductForm">关闭</button>
        </header>

        <div class="grid">
          <label>
            <span>商品名称</span>
            <input v-model="productForm.name" type="text" placeholder="请输入商品名称" />
          </label>
          <label>
            <span>所属分类</span>
            <select v-model.number="productForm.categoryId">
              <option :value="0">未分类</option>
              <option v-for="option in categories" :key="option.id" :value="option.id">
                {{ option.name }}
              </option>
            </select>
          </label>
          <label>
            <span>供应商</span>
            <select v-model.number="productForm.supplierId">
              <option :value="0">请选择供应商</option>
              <option v-for="option in suppliers" :key="option.id" :value="option.id">
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
            <span>售价 (CNY)</span>
            <input v-model="productForm.price" type="number" step="0.01" min="0" placeholder="0.00" />
          </label>
          <label>
            <span>计量单位</span>
            <input
              v-model="productForm.unit"
              type="text"
              placeholder="如：件 / 箱 / kg"
              maxlength="20"
              list="admin-product-unit-options"
            />
            <datalist id="admin-product-unit-options">
              <option v-for="option in unitOptions" :key="option" :value="option"></option>
            </datalist>
          </label>
          <label>
            <span>库存数量</span>
            <input v-model.number="productForm.stock" type="number" min="0" />
          </label>
          <div class="size-allocation-row full">
            <div class="size-allocation-row__content">
              <span class="size-allocation-row__label">选择尺码并分配库存</span>
              <p class="size-allocation-row__hint">点击后选择尺码并填写数量（合计需等于库存）</p>
            </div>
            <button type="button" class="primary" @click="openSizeAllocation">选择尺码</button>
          </div>
          <label class="full">
            <span>主图地址</span>
            <input v-model="productForm.mainImage" type="text" placeholder="https://" />
          </label>
          <label class="full">
            <span>商品描述</span>
            <textarea v-model="productForm.description" rows="4" placeholder="简要介绍商品亮点"></textarea>
          </label>
        </div>

        <div v-if="sizeAllocationOpen" class="size-panel">
          <div class="size-panel__header">
            <div>
              <p class="size-panel__title">按尺码分配库存</p>
              <p class="size-panel__hint">请为每个尺码填写数量，合计需与库存一致。</p>
            </div>
            <button type="button" class="ghost" @click="closeSizeAllocation">收起</button>
          </div>
          <div class="size-grid">
            <label v-for="size in sizeOptions" :key="size">
              <span>{{ size }}</span>
              <input v-model.number="sizeAllocations[size]" type="number" min="0" />
            </label>
          </div>
          <p :class="['size-summary', { 'is-error': sizeAllocationRemaining !== 0 }]">
            已分配：{{ formatNumber(sizeAllocationTotal) }} / 库存：{{ formatNumber(productForm.stock) }}
            <span v-if="sizeAllocationRemaining > 0">（剩余待分配 {{ formatNumber(sizeAllocationRemaining) }}）</span>
            <span v-else-if="sizeAllocationRemaining < 0">（已超出库存 {{ formatNumber(Math.abs(sizeAllocationRemaining)) }}）</span>
          </p>
        </div>

        <p v-if="productFormError" class="form-error">{{ productFormError }}</p>
        <p v-if="productFormMessage" class="form-success">{{ productFormMessage }}</p>

        <div class="form-actions">
          <button type="button" class="secondary" @click="cancelProductForm">取消</button>
          <button type="submit" class="primary" :disabled="savingProduct">
            {{ savingProduct ? '保存中…' : productForm.id ? '保存修改' : '创建商品' }}
          </button>
        </div>
      </div>
    </form>

    <div v-if="productViewOpen && viewingProduct" class="product-view" role="dialog" aria-modal="true">
      <div class="view-surface">
        <header>
          <h2>商品详情</h2>
          <button type="button" class="ghost" @click="closeProductView">关闭</button>
        </header>
        <dl class="detail-grid">
          <div>
            <dt>商品名称</dt>
            <dd>{{ viewingProduct?.name }}</dd>
          </div>
          <div>
            <dt>所属分类</dt>
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
            <dt>计量单位</dt>
            <dd>{{ viewingProduct?.unit ?? '—' }}</dd>
          </div>
          <div>
            <dt>库存</dt>
            <dd>{{ formatNumber(viewingProduct?.stock) }}</dd>
          </div>
          <div>
            <dt>销量</dt>
            <dd>{{ formatNumber(viewingProduct?.sales) }}</dd>
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
.product-admin-shell {
  display: grid;
  gap: 2rem;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2.25rem;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(14, 165, 233, 0.12));
  gap: 1.5rem;
}

.page-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.page-header p {
  color: rgba(15, 23, 42, 0.65);
}

.primary {
  border: none;
  border-radius: 999px;
  padding: 0.65rem 1.6rem;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary {
  border: 1px solid rgba(15, 23, 42, 0.18);
  border-radius: 999px;
  padding: 0.6rem 1.5rem;
  background: rgba(148, 163, 184, 0.12);
  color: rgba(15, 23, 42, 0.7);
  font-weight: 600;
  cursor: pointer;
}

.ghost {
  border: none;
  background: transparent;
  color: rgba(15, 23, 42, 0.55);
  cursor: pointer;
  font-weight: 600;
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
  padding: 1.75rem;
  box-shadow: 0 18px 36px rgba(30, 41, 59, 0.08);
  display: grid;
  gap: 1.25rem;
}

.panel-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.85);
}

.panel-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-status {
  color: rgba(14, 165, 233, 0.9);
  font-weight: 600;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1.2rem;
}

.metric-grid div {
  padding: 1rem;
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.08);
  display: grid;
  gap: 0.35rem;
}

.metric-grid span {
  color: rgba(15, 23, 42, 0.55);
}

.metric-grid strong {
  font-size: 1.4rem;
  color: rgba(15, 23, 42, 0.85);
}

.filters .filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.filters label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
}

.filters input,
.filters select {
  padding: 0.55rem 0.65rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 0.75rem;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.table-wrapper {
  overflow-x: auto;
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 18px;
  background: rgba(240, 249, 255, 0.7);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem 1.5rem;
  padding-top: 1rem;
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

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 960px;
}

thead {
  background: rgba(15, 23, 42, 0.04);
  color: rgba(15, 23, 42, 0.6);
}

th,
td {
  text-align: left;
  padding: 0.75rem 0.5rem;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

tbody tr:nth-child(odd) {
  background: rgba(59, 130, 246, 0.05);
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

.description {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.actions {
  display: flex;
  gap: 0.6rem;
}

.link-button {
  border: none;
  background: none;
  color: #2563eb;
  font-weight: 600;
  cursor: pointer;
}

.link-button.danger {
  color: #dc2626;
}

.link-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.product-view {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  z-index: 40;
}

.view-surface {
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

.view-surface header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.detail-grid {
  display: grid;
  gap: 0.9rem;
}

.detail-grid > div {
  display: grid;
  gap: 0.25rem;
  grid-template-columns: max-content 1fr;
  align-items: flex-start;
}

.detail-grid dt {
  font-weight: 600;
  color: rgba(15, 23, 42, 0.65);
}

.detail-grid dd {
  margin: 0;
  color: rgba(15, 23, 42, 0.85);
}

.detail-grid .full-width {
  grid-template-columns: 1fr;
}

.detail-grid img {
  max-width: 100%;
  border-radius: 1rem;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.2);
}

.empty {
  padding: 1.5rem 0;
  text-align: center;
  color: rgba(15, 23, 42, 0.5);
}

.product-form {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  z-index: 20;
  overflow-y: auto;
}

.form-shell {
  width: min(760px, 100%);
  background: #fff;
  border-radius: 20px;
  padding: 2rem;
  display: grid;
  gap: 1.5rem;
  max-height: 90vh;
  overflow-y: auto;
}

.form-shell header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.grid label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
}

.grid label.full {
  grid-column: 1 / -1;
}

.grid input,
.grid select,
.grid textarea {
  padding: 0.6rem 0.7rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.grid textarea {
  resize: vertical;
}

.size-allocation-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 0.75rem;
}

.size-allocation-row.full {
  grid-column: 1 / -1;
  align-items: center;
}

.size-allocation-row__content {
  display: grid;
  gap: 0.2rem;
}

.size-allocation-row__label {
  font-weight: 700;
  color: rgba(15, 23, 42, 0.78);
}

.size-allocation-row__hint {
  margin: 0;
  color: rgba(15, 23, 42, 0.55);
  font-size: 0.9rem;
}

.size-panel {
  margin-top: -0.25rem;
  padding: 1rem 1.25rem;
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.06);
  display: grid;
  gap: 0.75rem;
}

.size-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.size-panel__title {
  font-weight: 700;
  margin: 0;
  color: rgba(15, 23, 42, 0.85);
}

.size-panel__hint {
  margin: 0.1rem 0 0;
  color: rgba(15, 23, 42, 0.6);
}

.size-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.75rem;
}

.size-grid label {
  display: grid;
  gap: 0.35rem;
  padding: 0.75rem;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  font-weight: 700;
}

.size-grid input {
  border-radius: 0.5rem;
}

.size-summary {
  margin: 0;
  font-weight: 700;
  color: #2563eb;
}

.size-summary.is-error {
  color: #dc2626;
}

.form-error {
  color: #dc2626;
  font-weight: 600;
}

.form-success {
  color: #15803d;
  font-weight: 600;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  table {
    min-width: 720px;
  }
}
</style>
