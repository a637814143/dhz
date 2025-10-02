<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import ProductCard from '@/components/ProductCard.vue'
import api from '@/services/api'
import type {
  CategoryOption,
  PageResponse,
  ProductOverview,
  ProductSummary,
  SupplierOption,
} from '@/types'

const products = ref<ProductSummary[]>([])
const overview = ref<ProductOverview | null>(null)
const categories = ref<CategoryOption[]>([])
const suppliers = ref<SupplierOption[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const pagination = reactive({
  page: 0,
  size: 12,
  totalPages: 0,
  totalElements: 0,
})

const filters = reactive({
  keyword: '',
  categoryId: 'all',
  supplierId: 'all',
  minPrice: '',
  maxPrice: '',
  status: 'all',
  sortBy: 'createdAt',
  sortDirection: 'DESC' as 'ASC' | 'DESC',
})

const sortSelection = ref('createdAt:DESC')
const pageSize = ref(12)

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '上架商品', value: 'ON_SALE' },
  { label: '下架商品', value: 'OFF_SALE' },
]

const sortOptions = [
  { label: '最新发布', value: 'createdAt:DESC' },
  { label: '价格从低到高', value: 'price:ASC' },
  { label: '价格从高到低', value: 'price:DESC' },
  { label: '销量优先', value: 'sales:DESC' },
  { label: '库存从高到低', value: 'stock:DESC' },
]

const pageSizeOptions = [12, 24, 48]

const emptyState = computed(() => !loading.value && products.value.length === 0)
const canPrev = computed(() => pagination.page > 0)
const canNext = computed(() => pagination.page + 1 < pagination.totalPages)

function normaliseAmount(value: string) {
  if (!value.trim()) return null
  const amount = Number(value)
  return Number.isFinite(amount) && amount >= 0 ? amount : null
}

function buildQueryParams(resetPage = false) {
  const params: Record<string, unknown> = {
    page: resetPage ? 0 : pagination.page,
    size: pagination.size,
    sortBy: filters.sortBy,
    sortDirection: filters.sortDirection,
  }

  const keyword = filters.keyword.trim()
  if (keyword) {
    params.keyword = keyword
  }

  if (filters.categoryId !== 'all') {
    params.categoryId = Number(filters.categoryId)
  }

  if (filters.supplierId !== 'all') {
    params.supplierId = Number(filters.supplierId)
  }

  const minPrice = normaliseAmount(filters.minPrice)
  const maxPrice = normaliseAmount(filters.maxPrice)

  if (minPrice !== null) {
    params.minPrice = minPrice
  }
  if (maxPrice !== null) {
    params.maxPrice = maxPrice
  }

  if (filters.status !== 'all') {
    params.status = filters.status
  }

  return params
}

async function fetchOverview() {
  try {
    const { data } = await api.get<ProductOverview>('/products/overview')
    overview.value = data
  } catch (err) {
    console.warn('无法加载统计信息', err)
  }
}

async function fetchCategories() {
  try {
    const { data } = await api.get<CategoryOption[]>('/categories/enabled')
    categories.value = data
  } catch (err) {
    console.warn('无法加载分类信息', err)
  }
}

async function fetchSuppliers() {
  try {
    const { data } = await api.get<SupplierOption[]>('/suppliers/status/APPROVED')
    suppliers.value = data
  } catch (err) {
    console.warn('无法加载供应商信息', err)
  }
}

async function fetchProducts(resetPage = false) {
  loading.value = true
  error.value = null
  if (resetPage) {
    pagination.page = 0
  }

  const params = buildQueryParams(resetPage)

  try {
    const { data } = await api.get<PageResponse<ProductSummary>>('/products/advanced-search', {
      params,
    })
    products.value = data.content
    pagination.page = data.number
    pagination.size = data.size
    pagination.totalPages = data.totalPages
    pagination.totalElements = data.totalElements
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载商品失败'
    products.value = []
    pagination.totalPages = 0
    pagination.totalElements = 0
  } finally {
    loading.value = false
  }
}

function submitFilters() {
  const minPrice = normaliseAmount(filters.minPrice)
  const maxPrice = normaliseAmount(filters.maxPrice)

  if (minPrice !== null && maxPrice !== null && minPrice > maxPrice) {
    error.value = '最低价格不能高于最高价格'
    return
  }

  fetchProducts(true)
}

function changePage(page: number) {
  if (page < 0 || page >= pagination.totalPages || page === pagination.page) {
    return
  }
  pagination.page = page
  fetchProducts()
}

function onSortChange() {
  const [field, direction] = sortSelection.value.split(':')
  filters.sortBy = field
  filters.sortDirection = (direction === 'ASC' ? 'ASC' : 'DESC') as 'ASC' | 'DESC'
  fetchProducts(true)
}

function onPageSizeChange() {
  pagination.size = pageSize.value
  fetchProducts(true)
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = 'all'
  filters.supplierId = 'all'
  filters.minPrice = ''
  filters.maxPrice = ''
  filters.status = 'all'
  sortSelection.value = 'createdAt:DESC'
  pageSize.value = 12
  filters.sortBy = 'createdAt'
  filters.sortDirection = 'DESC'
  pagination.size = pageSize.value
  error.value = null
  fetchProducts(true)
}

onMounted(async () => {
  pagination.size = pageSize.value
  await Promise.all([fetchOverview(), fetchCategories(), fetchSuppliers()])
  await fetchProducts()
})
</script>

<template>
  <section class="home-view">
    <section class="hero">
      <div class="hero-text">
        <p class="tag">SilkMall · 蚕桑数智化</p>
        <h1>用数据驱动蚕制品销售，打造全渠道智慧商城</h1>
        <p>
          集产品管理、供应商协同与销售洞察于一体，帮助蚕桑企业实时掌握库存、销量与渠道动态，实现丝绸好物的高效上架与精准营销。
        </p>
      </div>
      <div class="hero-illustration" aria-hidden="true">
        <div class="bubble bubble-lg"></div>
        <div class="bubble bubble-md"></div>
        <div class="bubble bubble-sm"></div>
      </div>
    </section>

    <section class="overview" v-if="overview">
      <div class="overview-card">
        <span class="label">商品总量</span>
        <strong>{{ overview.totalProducts }}</strong>
        <small>涵盖蚕丝面料、日用家纺及文创礼盒</small>
      </div>
      <div class="overview-card">
        <span class="label">在售商品</span>
        <strong>{{ overview.onSaleProducts }}</strong>
        <small>可立即下单的上架商品数量</small>
      </div>
      <div class="overview-card">
        <span class="label">累计销量</span>
        <strong>{{ overview.totalSalesVolume }}</strong>
        <small>全平台成交总量（件）</small>
      </div>
      <div class="overview-card">
        <span class="label">库存总量</span>
        <strong>{{ overview.totalStock }}</strong>
        <small>仓储可用库存（件）</small>
      </div>
    </section>

    <section class="filters">
      <form class="filter-form" @submit.prevent="submitFilters">
        <div class="field text-field">
          <label for="keyword">关键字</label>
          <input
            id="keyword"
            v-model="filters.keyword"
            type="text"
            placeholder="搜索商品名称或描述"
            autocomplete="off"
          />
        </div>
        <div class="field">
          <label for="category">商品分类</label>
          <select id="category" v-model="filters.categoryId">
            <option value="all">全部分类</option>
            <option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </div>
        <div class="field">
          <label for="supplier">供应商</label>
          <select id="supplier" v-model="filters.supplierId">
            <option value="all">全部供应商</option>
            <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
              {{ supplier.companyName }}
            </option>
          </select>
        </div>
        <div class="field price-field">
          <label>价格区间（元）</label>
          <div class="range">
            <input v-model="filters.minPrice" type="number" min="0" placeholder="最低价" />
            <span>—</span>
            <input v-model="filters.maxPrice" type="number" min="0" placeholder="最高价" />
          </div>
        </div>
        <div class="field">
          <label for="status">上架状态</label>
          <select id="status" v-model="filters.status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
        <div class="field">
          <label for="sort">排序规则</label>
          <select id="sort" v-model="sortSelection" @change="onSortChange">
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </div>
        <div class="field">
          <label for="page-size">每页数量</label>
          <select id="page-size" v-model.number="pageSize" @change="onPageSizeChange">
            <option v-for="option in pageSizeOptions" :key="option" :value="option">{{ option }}</option>
          </select>
        </div>
        <div class="actions">
          <button type="submit" class="primary">应用筛选</button>
          <button type="button" class="secondary" @click="resetFilters">
            重置
          </button>
        </div>
      </form>
    </section>

    <section class="product-list">
      <header class="list-header">
        <h2>精选蚕制品</h2>
        <p>共 {{ pagination.totalElements }} 件商品</p>
      </header>

      <p v-if="error" class="error-message">{{ error }}</p>
      <div v-if="loading" class="loading">正在加载商品数据...</div>
      <div v-else-if="emptyState" class="empty">暂无符合条件的商品，尝试调整筛选条件。</div>

      <div v-if="!loading && !emptyState" class="grid">
        <ProductCard v-for="item in products" :key="item.id" :product="item" />
      </div>

      <div v-if="pagination.totalPages > 1" class="pagination" role="navigation" aria-label="分页导航">
        <button type="button" :disabled="!canPrev" @click="changePage(pagination.page - 1)">上一页</button>
        <span>第 {{ pagination.page + 1 }} / {{ pagination.totalPages }} 页</span>
        <button type="button" :disabled="!canNext" @click="changePage(pagination.page + 1)">下一页</button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.hero {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 2.5rem;
  padding: 2.5rem;
  border-radius: 2rem;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.28), rgba(111, 169, 173, 0.35));
  position: relative;
  overflow: hidden;
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.hero-text h1 {
  font-size: clamp(1.8rem, 2.8vw, 2.6rem);
  font-weight: 800;
  line-height: 1.25;
}

.hero-text p {
  font-size: 0.95rem;
  color: rgba(0, 0, 0, 0.7);
}

.tag {
  display: inline-block;
  padding: 0.4rem 0.75rem;
  border-radius: 999px;
  background: rgba(92, 44, 12, 0.12);
  color: #5c2c0c;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.1em;
}

.hero-illustration {
  position: relative;
  min-height: 220px;
}

.bubble {
  position: absolute;
  border-radius: 50%;
  filter: blur(0.5px);
  opacity: 0.75;
}

.bubble-lg {
  width: 220px;
  height: 220px;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.95), rgba(242, 177, 66, 0.85));
  top: 10%;
  right: 10%;
}

.bubble-md {
  width: 160px;
  height: 160px;
  background: radial-gradient(circle at 40% 40%, rgba(255, 255, 255, 0.95), rgba(111, 169, 173, 0.8));
  bottom: 15%;
  left: 10%;
}

.bubble-sm {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.9), rgba(242, 177, 66, 0.7));
  bottom: 30%;
  right: -5%;
}

.overview {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.overview-card {
  padding: 1.5rem;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.overview-card strong {
  font-size: 2rem;
  font-weight: 700;
}

.overview-card .label {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.55);
  letter-spacing: 0.08em;
}

.overview-card small {
  color: rgba(0, 0, 0, 0.45);
  font-size: 0.8rem;
}

.filters {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 1.5rem;
  padding: 1.75rem;
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08);
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem 1.5rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.6);
}

.field input,
.field select {
  height: 44px;
  border-radius: 0.85rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
  padding: 0 0.85rem;
  font-size: 0.9rem;
  background: rgba(255, 255, 255, 0.9);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus,
.field select:focus {
  outline: none;
  border-color: rgba(242, 177, 66, 0.6);
  box-shadow: 0 0 0 4px rgba(242, 177, 66, 0.16);
}

.text-field input {
  width: 100%;
}

.price-field .range {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.price-field input {
  flex: 1;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
}

.actions button {
  min-width: 120px;
  height: 44px;
  border-radius: 999px;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.actions button.primary {
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  box-shadow: 0 12px 24px rgba(242, 142, 28, 0.25);
}

.actions button.secondary {
  background: rgba(111, 169, 173, 0.1);
  color: #1c1c1e;
}

.actions button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.list-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
}

.list-header h2 {
  font-size: 1.4rem;
  font-weight: 700;
}

.list-header p {
  color: rgba(0, 0, 0, 0.55);
}

.loading,
.empty,
.error-message {
  padding: 1.5rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.06);
}

.error-message {
  color: #b91c1c;
  box-shadow: inset 0 0 0 1px rgba(185, 28, 28, 0.2);
}

.grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.25rem;
  margin-top: 1rem;
}

.pagination button {
  min-width: 110px;
  height: 40px;
  border-radius: 999px;
  border: none;
  background: rgba(242, 177, 66, 0.15);
  color: #1c1c1e;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.pagination button:not(:disabled):hover {
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .hero {
    padding: 1.75rem;
  }

  .filter-form {
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: stretch;
  }

  .actions button {
    flex: 1;
  }
}

@media (prefers-color-scheme: dark) {
  .hero-text p,
  .overview-card small,
  .overview-card .label,
  .list-header p,
  .actions button.secondary {
    color: rgba(226, 232, 240, 0.75);
  }

  .overview-card,
  .filters,
  .loading,
  .empty,
  .error-message {
    background: rgba(31, 41, 55, 0.8);
    box-shadow: 0 16px 38px rgba(0, 0, 0, 0.35);
  }

  .field input,
  .field select {
    background: rgba(15, 23, 42, 0.85);
    color: #f9fafb;
    border-color: rgba(148, 163, 184, 0.25);
  }

  .pagination button {
    background: rgba(242, 177, 66, 0.28);
    color: #f9fafb;
  }
}
</style>