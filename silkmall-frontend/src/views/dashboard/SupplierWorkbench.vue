<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type { CategoryOption, HomepageContent, ProductDetail, ProductImage, ProductSummary } from '@/types'

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

const { state } = useAuthState()

const profile = ref<SupplierProfile | null>(null)
const products = ref<ProductSummary[]>([])
const homeContent = ref<HomepageContent | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const categories = ref<CategoryOption[]>([])
const walletBalance = ref<number | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)

const productDialogOpen = ref(false)
const savingProduct = ref(false)
const productFormError = ref<string | null>(null)
const productFormMessage = ref<string | null>(null)
const deletingProductId = ref<number | null>(null)
const togglingProductId = ref<number | null>(null)

const viewDialogOpen = ref(false)
const viewDialogRef = ref<HTMLDivElement | null>(null)
const viewingProduct = ref<ProductDetail | null>(null)
const viewLoading = ref(false)
const viewError = ref<string | null>(null)

const profileDialogRef = ref<HTMLDivElement | null>(null)
const productDialogRef = ref<HTMLDivElement | null>(null)

const profileDialogOpen = ref(false)
const profileSaving = ref(false)
const profileFormError = ref<string | null>(null)
const profileFormMessage = ref<string | null>(null)
const profileForm = reactive({
  companyName: '',
  email: '',
  phone: '',
  address: '',
  contactPerson: '',
  businessLicense: '',
})

const productForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  price: '',
  stock: 0,
  categoryId: null as number | null,
  status: 'ON_SALE',
  mainImage: '',
})

const categoryNameInput = ref('')
const categorySaving = ref(false)
const categoryFeedback = ref<string | null>(null)
const categoryError = ref<string | null>(null)

async function loadProfile() {
  if (!state.user) return
  const { data } = await api.get<SupplierProfile>(`/suppliers/${state.user.id}`)
  profile.value = data
  fillProfileForm(data)
}

function fillProfileForm(source: SupplierProfile | null) {
  profileForm.companyName = source?.companyName ?? ''
  profileForm.email = source?.email ?? ''
  profileForm.phone = source?.phone ?? ''
  profileForm.address = source?.address ?? ''
  profileForm.contactPerson = source?.contactPerson ?? ''
  profileForm.businessLicense = source?.businessLicense ?? ''
}

function toIsoString(value: unknown): string {
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) {
      return ''
    }
    const parsed = Date.parse(trimmed)
    if (!Number.isNaN(parsed)) {
      return new Date(parsed).toISOString()
    }
    const numeric = Number(trimmed)
    if (Number.isFinite(numeric)) {
      const fromNumeric = new Date(numeric)
      if (!Number.isNaN(fromNumeric.getTime())) {
        return fromNumeric.toISOString()
      }
    }
    return trimmed
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    const fromNumber = new Date(value)
    if (!Number.isNaN(fromNumber.getTime())) {
      return fromNumber.toISOString()
    }
  }

  if (value instanceof Date && !Number.isNaN(value.getTime())) {
    return value.toISOString()
  }

  return ''
}

function unwrapProductArray(payload: unknown): unknown[] {
  if (Array.isArray(payload)) {
    return payload
  }

  if (!payload || typeof payload !== 'object') {
    return []
  }

  const source = payload as Record<string, unknown>
  const candidateKeys = ['content', 'records', 'items', 'list', 'rows', 'result', 'data']

  for (const key of candidateKeys) {
    const value = source[key]
    if (Array.isArray(value)) {
      return value
    }
  }

  for (const key of candidateKeys) {
    const value = source[key]
    if (value && typeof value === 'object' && value !== payload) {
      const nested = unwrapProductArray(value)
      if (nested.length > 0) {
        return nested
      }
    }
  }

  return []
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  if (typeof value === 'bigint') {
    return Number(value)
  }
  return fallback
}

function toInteger(value: unknown, fallback = 0): number {
  const parsed = toNumber(value, fallback)
  if (!Number.isFinite(parsed)) {
    return fallback
  }
  const integer = Math.trunc(parsed)
  return Number.isFinite(integer) ? integer : fallback
}

function normaliseProduct(payload: unknown): ProductSummary | null {
  if (!payload || typeof payload !== 'object') {
    return null
  }

  const source = payload as Record<string, any>
  const id = toInteger(source.id ?? source.productId, NaN)
  if (!Number.isFinite(id)) {
    return null
  }

  const nameRaw = source.name ?? ''
  const name = typeof nameRaw === 'string' ? nameRaw.trim() : String(nameRaw ?? '').trim()
  const description = typeof source.description === 'string' ? source.description : ''
  const price = toNumber(source.price, 0)
  const stock = Math.max(0, toInteger(source.stock, 0))
  const sales = Math.max(0, toInteger(source.sales, 0))
  const mainImage = typeof source.mainImage === 'string' ? source.mainImage : null
  const statusRaw = typeof source.status === 'string' ? source.status.toUpperCase() : 'OFF_SALE'
  const createdAtIso =
    toIsoString(source.createdAt ?? source.createTime ?? source.created_at) || new Date().toISOString()
  const categoryName =
    typeof source.categoryName === 'string'
      ? source.categoryName
      : typeof source.category?.name === 'string'
        ? source.category.name
        : null
  const supplierName =
    typeof source.supplierName === 'string'
      ? source.supplierName
      : typeof source.supplier?.companyName === 'string'
        ? source.supplier.companyName
        : null
  const supplierLevel =
    typeof source.supplierLevel === 'string'
      ? source.supplierLevel
      : typeof source.supplier?.supplierLevel === 'string'
        ? source.supplier.supplierLevel
        : null

  return {
    id,
    name: name || `商品 ${id}`,
    description,
    price,
    stock,
    sales,
    mainImage,
    status: statusRaw,
    createdAt: createdAtIso,
    categoryName,
    supplierName,
    supplierLevel,
  }
}

function toTimestamp(value?: string | null): number {
  if (!value) {
    return 0
  }
  const parsed = Date.parse(value)
  if (!Number.isNaN(parsed)) {
    return parsed
  }
  const numeric = Number(value)
  if (Number.isFinite(numeric)) {
    return numeric
  }
  return 0
}

function sortProducts(list: ProductSummary[]): ProductSummary[] {
  return [...list].sort((a, b) => {
    const timeDiff = toTimestamp(b.createdAt) - toTimestamp(a.createdAt)
    if (timeDiff !== 0) {
      return timeDiff
    }
    return b.id - a.id
  })
}

function normaliseProductList(payload: unknown): ProductSummary[] {
  const rawList = unwrapProductArray(payload)
  const mapped = rawList
    .map((item) => normaliseProduct(item))
    .filter((item): item is ProductSummary => item !== null)
  return sortProducts(mapped)
}

async function loadProducts() {
  if (!state.user) return
  const { data } = await api.get(`/products/supplier/${state.user.id}`, {
    params: { page: 0, size: 20, sort: 'createdAt,desc' },
  })
  products.value = normaliseProductList(data)
}

function updateProductList(product: ProductSummary, options: { prepend?: boolean } = {}) {
  const base = products.value.filter((item) => item.id !== product.id)
  if (options.prepend) {
    base.unshift(product)
  } else {
    base.push(product)
  }
  products.value = sortProducts(base)
}

function removeProductFromList(productId: number) {
  products.value = products.value.filter((item) => item.id !== productId)
}

function setProductStatusInList(productId: number, status: string) {
  products.value = sortProducts(
    products.value.map((item) => (item.id === productId ? { ...item, status } : item))
  )
}

function normaliseProductDetail(payload: unknown, fallback?: ProductSummary): ProductDetail | null {
  const summary = normaliseProduct(payload) ?? fallback ?? null
  if (!summary) {
    return null
  }

  const source = payload && typeof payload === 'object' ? (payload as Record<string, any>) : {}
  const detail: ProductDetail = {
    ...summary,
    description:
      typeof source.description === 'string'
        ? source.description
        : summary.description ?? fallback?.description ?? '',
  }

  const updatedIso = toIsoString(source.updatedAt ?? source.updateTime ?? source.updated_at)
  if (updatedIso) {
    detail.updatedAt = updatedIso
  }

  if (Array.isArray(source.images)) {
    const parsedImages = source.images
      .map((item: unknown) => {
        if (!item || typeof item !== 'object') {
          return null
        }
        const imageSource = item as Record<string, any>
        const imageId = toInteger(imageSource.id, NaN)
        const imageUrl = typeof imageSource.imageUrl === 'string' ? imageSource.imageUrl : null
        if (!Number.isFinite(imageId) || !imageUrl) {
          return null
        }
        const image: ProductImage = {
          id: imageId,
          imageUrl,
        }
        if (imageSource.sortOrder !== undefined) {
          const sortOrder = toInteger(imageSource.sortOrder, NaN)
          if (Number.isFinite(sortOrder)) {
            image.sortOrder = sortOrder
          }
        }
        const created = toIsoString(imageSource.createdAt ?? imageSource.createTime ?? imageSource.created_at)
        if (created) {
          image.createdAt = created
        }
        return image
      })
      .filter((item): item is ProductImage => item !== null)
    if (parsedImages.length > 0) {
      detail.images = parsedImages
    }
  }

  const categorySource = source.category
  if (categorySource && typeof categorySource === 'object') {
    const record = categorySource as Record<string, any>
    const categoryId = toInteger(record.id, 0)
    const categoryName =
      typeof record.name === 'string' ? record.name : detail.categoryName ?? fallback?.categoryName ?? ''
    detail.category = {
      id: Number.isFinite(categoryId) ? categoryId : 0,
      name: categoryName || `分类 ${categoryId || ''}`.trim(),
      description: typeof record.description === 'string' ? record.description : null,
    }
  } else if (detail.categoryName) {
    detail.category = {
      id: 0,
      name: detail.categoryName,
      description: null,
    }
  }

  const supplierSource = source.supplier
  if (supplierSource && typeof supplierSource === 'object') {
    const record = supplierSource as Record<string, any>
    const supplierId = toInteger(record.id, 0)
    detail.supplier = {
      id: Number.isFinite(supplierId) ? supplierId : 0,
      companyName:
        typeof record.companyName === 'string'
          ? record.companyName
          : detail.supplierName ?? fallback?.supplierName ?? '—',
      supplierLevel:
        typeof record.supplierLevel === 'string'
          ? record.supplierLevel
          : detail.supplierLevel ?? null,
      contactName:
        typeof record.contactName === 'string'
          ? record.contactName
          : typeof record.contactPerson === 'string'
            ? record.contactPerson
            : undefined,
      contactPhone: typeof record.contactPhone === 'string' ? record.contactPhone : undefined,
    }
  } else if (detail.supplierName) {
    detail.supplier = {
      id: 0,
      companyName: detail.supplierName,
      supplierLevel: detail.supplierLevel ?? null,
    }
  }

  return detail
}

async function refreshProductsSilently() {
  try {
    await loadProducts()
  } catch (err) {
    console.warn('刷新商品列表失败', err)
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

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([loadProfile(), loadProducts(), loadHomeContent(), loadCategories(), loadWallet()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载供应商数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

watch(profileDialogOpen, (open) => {
  if (open) {
    nextTick(() => {
      profileDialogRef.value?.focus()
    })
  }
})

watch(productDialogOpen, (open) => {
  if (open) {
    nextTick(() => {
      productDialogRef.value?.focus()
    })
  }
})

watch(viewDialogOpen, (open) => {
  if (open) {
    nextTick(() => {
      viewDialogRef.value?.focus()
    })
  } else {
    viewLoading.value = false
    viewError.value = null
    viewingProduct.value = null
  }
})

const totalStock = computed(() => products.value.reduce((acc, item) => acc + (item.stock ?? 0), 0))
const totalSales = computed(() => products.value.reduce((acc, item) => acc + (item.sales ?? 0), 0))
const onSaleProducts = computed(() => products.value.filter((item) => item.status === 'ON_SALE').length)

function formatCurrency(amount?: number | null) {
  if (typeof amount !== 'number' || Number.isNaN(amount)) return '¥0.00'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(amount)
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '—'
  }
  const parsed = new Date(value)
  if (!Number.isNaN(parsed.getTime())) {
    return parsed.toLocaleString('zh-CN', { hour12: false })
  }
  const numeric = Number(value)
  if (Number.isFinite(numeric)) {
    const fromNumeric = new Date(numeric)
    if (!Number.isNaN(fromNumeric.getTime())) {
      return fromNumeric.toLocaleString('zh-CN', { hour12: false })
    }
  }
  return value
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
  productForm.stock = 0
  productForm.categoryId = null
  productForm.status = 'ON_SALE'
  productForm.mainImage = ''
  productFormError.value = null
  productFormMessage.value = null
}

function openProfileDialog() {
  profileFormError.value = null
  profileFormMessage.value = null
  fillProfileForm(profile.value)
  profileDialogOpen.value = true
}

function cancelProfileDialog() {
  profileDialogOpen.value = false
  profileFormError.value = null
  profileFormMessage.value = null
}

async function saveProfile() {
  if (!state.user) {
    profileFormError.value = '请先登录供应商账号'
    return
  }

  const companyName = profileForm.companyName.trim()
  if (!companyName) {
    profileFormError.value = '请填写企业名称'
    return
  }

  const payload: Record<string, unknown> = {
    companyName,
    email: profileForm.email.trim() || null,
    phone: profileForm.phone.trim() || null,
    address: profileForm.address.trim() || null,
    contactPerson: profileForm.contactPerson.trim() || null,
    businessLicense: profileForm.businessLicense.trim() || null,
  }

  profileSaving.value = true
  profileFormError.value = null
  try {
    const { data } = await api.put<SupplierProfile>(`/suppliers/${state.user.id}/profile`, payload)
    profile.value = data
    fillProfileForm(data)
    profileFormMessage.value = '基础信息已更新'
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新基础信息失败'
    profileFormError.value = message
  } finally {
    profileSaving.value = false
  }
}

async function openProductForm(product?: ProductSummary) {
  productFormError.value = null
  productFormMessage.value = null
  await loadCategories()
  if (product) {
    let source: ProductDetail | ProductSummary = product
    try {
      const { data } = await api.get<ProductDetail>(`/products/${product.id}`)
      const detail = normaliseProductDetail(data, product)
      if (detail) {
        source = detail
      }
    } catch (err) {
      console.warn('加载商品详情失败', err)
    }
    productForm.id = source.id
    productForm.name = source.name
    productForm.description = source.description ?? ''
    productForm.price = (source.price ?? '').toString()
    productForm.stock = source.stock ?? 0
    const categoryRef = (source as any).categoryId ?? (source as any).category?.id ?? null
    if (typeof categoryRef === 'number') {
      productForm.categoryId = categoryRef
    } else if (categoryRef !== null && categoryRef !== undefined) {
      const parsed = Number(categoryRef)
      productForm.categoryId = Number.isFinite(parsed) ? parsed : null
    } else {
      productForm.categoryId = null
    }
    productForm.status = source.status ?? 'ON_SALE'
    productForm.mainImage = source.mainImage ?? ''
  } else {
    resetProductForm()
  }
  productDialogOpen.value = true
}

function cancelProductForm() {
  productDialogOpen.value = false
  resetProductForm()
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

  const payload: Record<string, unknown> = {
    name,
    description: productForm.description.trim() || null,
    price,
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
      const { data } = await api.put(`/products/${productForm.id}`, payload)
      const updated = normaliseProduct(data)
      if (updated) {
        updateProductList(updated)
      } else {
        await refreshProductsSilently()
      }
      productFormMessage.value = '商品信息已更新'
    } else {
      const { data } = await api.post('/products', payload)
      const created = normaliseProduct(data)
      if (created) {
        updateProductList(created, { prepend: true })
      } else {
        await refreshProductsSilently()
      }
      productFormMessage.value = '商品已创建并保存'
    }
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
    removeProductFromList(productId)
    await refreshProductsSilently()
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除商品失败'
    window.alert(message)
  } finally {
    deletingProductId.value = null
  }
}

async function toggleProductStatus(product: ProductSummary) {
  const targetStatus = product.status === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
  togglingProductId.value = product.id
  try {
    const endpoint = targetStatus === 'ON_SALE' ? 'on-sale' : 'off-sale'
    await api.put(`/products/${product.id}/${endpoint}`)
    setProductStatusInList(product.id, targetStatus)
    window.alert(targetStatus === 'ON_SALE' ? '商品已上架' : '商品已下架')
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新商品状态失败'
    window.alert(message)
  } finally {
    togglingProductId.value = null
    await refreshProductsSilently()
  }
}

async function openProductDetails(product: ProductSummary) {
  viewError.value = null
  viewLoading.value = true
  const fallback = normaliseProductDetail(product, product)
  if (fallback) {
    viewingProduct.value = fallback
  }
  viewDialogOpen.value = true
  try {
    const { data } = await api.get<ProductDetail>(`/products/${product.id}`)
    const detail = normaliseProductDetail(data, product)
    if (detail) {
      viewingProduct.value = detail
    }
  } catch (err) {
    viewError.value = err instanceof Error ? err.message : '加载商品详情失败'
  } finally {
    viewLoading.value = false
  }
}

function closeProductDetails() {
  viewDialogOpen.value = false
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
    categoryFeedback.value = '分类创建成功'
  } catch (err) {
    const message = err instanceof Error ? err.message : '创建分类失败'
    categoryError.value = message
  } finally {
    categorySaving.value = false
  }
}

const statusOptions = [
  { value: 'ON_SALE', label: '在售' },
  { value: 'OFF_SALE', label: '已下架' },
]
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
          <button type="button" class="link-button" @click="openProfileDialog">编辑资料</button>
        </div>
        <ul>
          <li><span>企业名称</span><strong>{{ profile?.companyName ?? '—' }}</strong></li>
          <li><span>联系人</span><strong>{{ profile?.contactPerson ?? '—' }}</strong></li>
          <li><span>联系人邮箱</span><strong>{{ profile?.email ?? '—' }}</strong></li>
          <li><span>联系电话</span><strong>{{ profile?.phone ?? '—' }}</strong></li>
          <li><span>联系地址</span><strong>{{ profile?.address ?? '—' }}</strong></li>
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
        <div v-if="products.length" class="product-table">
          <table>
            <thead>
              <tr>
                <th scope="col">商品名称</th>
                <th scope="col">售价</th>
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
                <td>{{ item.stock }}</td>
                <td>{{ item.sales }}</td>
                <td><span class="status-pill">{{ productStatus(item.status) }}</span></td>
                <td class="actions">
                  <button type="button" class="link-button" @click="openProductDetails(item)">查看</button>
                  <button type="button" class="link-button" @click="openProductForm(item)">编辑</button>
                  <button
                    type="button"
                    class="link-button warning"
                    @click="toggleProductStatus(item)"
                    :disabled="togglingProductId === item.id"
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
        </div>
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
  </section>

  <teleport to="body">
    <div
      v-if="profileDialogOpen"
      class="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="profile-dialog-title"
      @click.self="cancelProfileDialog"
    >
      <div
        ref="profileDialogRef"
        class="modal-card"
        role="document"
        tabindex="-1"
        @keydown.esc="cancelProfileDialog"
      >
        <header class="modal-header">
          <h2 id="profile-dialog-title">编辑基础信息</h2>
          <button type="button" class="icon-button" @click="cancelProfileDialog" aria-label="关闭窗口">×</button>
        </header>
        <form class="profile-form" @submit.prevent="saveProfile">
          <div class="grid">
            <label>
              <span>企业名称</span>
              <input v-model="profileForm.companyName" type="text" placeholder="请输入企业名称" />
            </label>
            <label>
              <span>联系人</span>
              <input v-model="profileForm.contactPerson" type="text" placeholder="请输入联系人姓名" />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="profileForm.email" type="email" placeholder="请输入联系邮箱" />
            </label>
            <label>
              <span>联系电话</span>
              <input v-model="profileForm.phone" type="text" placeholder="请输入联系电话" />
            </label>
            <label class="full">
              <span>联系地址</span>
              <input v-model="profileForm.address" type="text" placeholder="请输入联系地址" />
            </label>
            <label class="full">
              <span>营业执照编号</span>
              <input
                v-model="profileForm.businessLicense"
                type="text"
                placeholder="请输入营业执照编号"
              />
            </label>
          </div>
          <p v-if="profileFormError" class="form-error">{{ profileFormError }}</p>
          <p v-if="profileFormMessage" class="form-success">{{ profileFormMessage }}</p>
          <div class="form-actions">
            <button type="submit" class="primary" :disabled="profileSaving">
              {{ profileSaving ? '保存中…' : '保存资料' }}
            </button>
            <button type="button" class="ghost" @click="cancelProfileDialog" :disabled="profileSaving">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </teleport>

  <teleport to="body">
    <div
      v-if="viewDialogOpen"
      class="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="product-view-dialog-title"
      @click.self="closeProductDetails"
    >
      <div
        ref="viewDialogRef"
        class="modal-card is-wide"
        role="document"
        tabindex="-1"
        @keydown.esc="closeProductDetails"
      >
        <header class="modal-header">
          <h2 id="product-view-dialog-title">商品详情</h2>
          <button type="button" class="icon-button" @click="closeProductDetails" aria-label="关闭窗口">×</button>
        </header>
        <div class="modal-body">
          <div v-if="viewLoading" class="placeholder">正在加载商品信息…</div>
          <div v-else-if="viewError" class="placeholder is-error">{{ viewError }}</div>
          <template v-else-if="viewingProduct">
            <div class="product-detail-grid">
              <figure v-if="viewingProduct.mainImage" class="product-cover">
                <img :src="viewingProduct.mainImage" :alt="viewingProduct.name" />
              </figure>
              <div class="product-info-block">
                <h3>{{ viewingProduct.name }}</h3>
                <p class="price">{{ formatCurrency(viewingProduct.price) }}</p>
                <p class="status">
                  状态：
                  <span class="status-pill" :class="{ 'is-off': viewingProduct.status !== 'ON_SALE' }">
                    {{ productStatus(viewingProduct.status) }}
                  </span>
                </p>
                <ul class="meta-list">
                  <li>库存：<strong>{{ viewingProduct.stock }}</strong></li>
                  <li>销量：<strong>{{ viewingProduct.sales }}</strong></li>
                  <li>
                    分类：
                    <strong>{{ viewingProduct.category?.name ?? viewingProduct.categoryName ?? '未分类' }}</strong>
                  </li>
                  <li>
                    供应商：
                    <strong>{{ viewingProduct.supplier?.companyName ?? viewingProduct.supplierName ?? '—' }}</strong>
                  </li>
                  <li>创建时间：<strong>{{ formatDateTime(viewingProduct.createdAt) }}</strong></li>
                  <li>
                    更新时间：
                    <strong>{{ formatDateTime(viewingProduct.updatedAt ?? viewingProduct.createdAt) }}</strong>
                  </li>
                </ul>
              </div>
            </div>
            <section class="detail-section">
              <h3>商品描述</h3>
              <p>{{ viewingProduct.description?.trim().length ? viewingProduct.description : '暂无描述' }}</p>
            </section>
            <section v-if="viewingProduct.images?.length" class="detail-section">
              <h3>图片</h3>
              <div class="image-strip">
                <img
                  v-for="image in viewingProduct.images"
                  :key="image.id"
                  :src="image.imageUrl"
                  :alt="`${viewingProduct.name} 图片`"
                />
              </div>
            </section>
          </template>
        </div>
      </div>
    </div>
  </teleport>

  <teleport to="body">
    <div
      v-if="productDialogOpen"
      class="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="product-dialog-title"
      @click.self="cancelProductForm"
    >
      <div
        ref="productDialogRef"
        class="modal-card"
        role="document"
        tabindex="-1"
        @keydown.esc="cancelProductForm"
      >
        <header class="modal-header">
          <h2 id="product-dialog-title">{{ productForm.id ? '编辑商品' : '新增商品' }}</h2>
          <button type="button" class="icon-button" @click="cancelProductForm" aria-label="关闭窗口">×</button>
        </header>
        <form class="product-form" @submit.prevent="saveProduct">
          <div class="grid">
            <label>
              <span>商品名称</span>
              <input v-model="productForm.name" type="text" placeholder="请输入商品名称" />
            </label>
            <label>
              <span>商品状态</span>
              <select v-model="productForm.status">
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>

          <div class="grid">
            <label>
              <span>售价（CNY）</span>
              <input v-model="productForm.price" type="number" min="0" step="0.01" placeholder="如：199" />
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

          <label>
            <span>主图地址</span>
            <input v-model="productForm.mainImage" type="text" placeholder="可选：图片链接" />
          </label>

          <label>
            <span>商品描述</span>
            <textarea v-model="productForm.description" rows="3" placeholder="介绍商品亮点"></textarea>
          </label>

          <p v-if="productFormError" class="error">{{ productFormError }}</p>
          <p v-if="productFormMessage" class="success">{{ productFormMessage }}</p>

          <div class="form-actions">
            <button type="submit" :disabled="savingProduct">
              {{ savingProduct ? '保存中…' : productForm.id ? '保存修改' : '创建商品' }}
            </button>
            <button type="button" class="ghost" @click="cancelProductForm" :disabled="savingProduct">
              取消
            </button>
          </div>
        </form>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.workbench-shell {
  display: grid;
  gap: 2.5rem;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 2000;
}

.modal-card {
  background: #fff;
  border-radius: 20px;
  width: min(720px, 100%);
  max-height: min(90vh, 880px);
  overflow: auto;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.18);
  padding: 1.8rem;
  display: grid;
  gap: 1.5rem;
}

.modal-card.is-wide {
  width: min(860px, 100%);
}

.modal-body {
  display: grid;
  gap: 1.5rem;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.9);
}

.product-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 260px) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.product-cover {
  margin: 0;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}

.product-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info-block {
  display: grid;
  gap: 0.75rem;
}

.product-info-block h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.9);
}

.product-info-block .price {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 600;
  color: #0ea5e9;
}

.product-info-block .status {
  margin: 0;
  color: rgba(15, 23, 42, 0.65);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.meta-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.35rem;
  color: rgba(15, 23, 42, 0.7);
}

.meta-list li strong {
  margin-left: 0.3rem;
  color: rgba(15, 23, 42, 0.95);
}

.detail-section {
  display: grid;
  gap: 0.6rem;
}

.detail-section h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.85);
}

.detail-section p {
  margin: 0;
  line-height: 1.65;
  color: rgba(15, 23, 42, 0.7);
}

.image-strip {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.75rem;
}

.image-strip img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.18);
}

.icon-button {
  border: none;
  background: none;
  font-size: 1.6rem;
  line-height: 1;
  cursor: pointer;
  color: rgba(15, 23, 42, 0.6);
}

.icon-button:hover {
  color: rgba(15, 23, 42, 0.85);
}

@media (max-width: 640px) {
  .modal-card {
    padding: 1.25rem;
    border-radius: 16px;
  }

  .modal-header h2 {
    font-size: 1.15rem;
  }

  .product-detail-grid {
    grid-template-columns: 1fr;
  }
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

.profile-form {
  display: grid;
  gap: 1rem;
}

.profile-form .grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.profile-form label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.75);
}

.profile-form label.full {
  grid-column: 1 / -1;
}

.profile-form input {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.ghost {
  border: 1px solid rgba(37, 99, 235, 0.35);
  border-radius: 0.75rem;
  padding: 0.55rem 1.5rem;
  background: transparent;
  color: rgba(37, 99, 235, 0.95);
  font-weight: 600;
  cursor: pointer;
}

.ghost:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.form-error {
  color: #b91c1c;
  font-size: 0.95rem;
}

.form-success {
  color: #15803d;
  font-size: 0.95rem;
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

.status-pill.is-off {
  background: rgba(107, 114, 128, 0.18);
  color: #6b7280;
}

.product-table td.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.link-button {
  border: none;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.link-button.warning {
  color: #d97706;
}

.link-button.danger {
  color: #b91c1c;
}

.product-form {
  display: grid;
  gap: 1rem;
}

.product-form .grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.product-form label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
}

.product-form input,
.product-form select,
.product-form textarea {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  font-size: 0.95rem;
}

.product-form textarea {
  resize: vertical;
}

.product-form .form-actions {
  display: flex;
  gap: 0.75rem;
}

.product-form .form-actions button {
  padding: 0.55rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.product-form .form-actions button.ghost {
  background: transparent;
  color: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(15, 23, 42, 0.15);
}

.product-form .form-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.product-form .error {
  color: #b91c1c;
}

.product-form .success {
  color: #15803d;
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

.empty {
  color: rgba(15, 23, 42, 0.6);
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
