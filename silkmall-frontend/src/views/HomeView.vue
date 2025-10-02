<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import ProductCard from '@/components/ProductCard.vue'
import api from '@/services/api'
import type {
  CategoryOption,
  HomepageContent,
  PageResponse,
  ProductOverview,
  ProductSummary,
  SupplierOption,
} from '@/types'

const products = ref<ProductSummary[]>([])
const overview = ref<ProductOverview | null>(null)
const categories = ref<CategoryOption[]>([])
const suppliers = ref<SupplierOption[]>([])
const homepageContent = ref<HomepageContent | null>(null)
const homepageLoading = ref(false)
const homepageError = ref<string | null>(null)
const activeBannerIndex = ref(0)
let bannerTimer: ReturnType<typeof setInterval> | null = null
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
const topHotProducts = computed(() => (homepageContent.value?.hotProducts ?? []).slice(0, 5))
const recommendationShowcase = computed(() => (homepageContent.value?.recommendedProducts ?? []).slice(0, 4))
const announcementList = computed(() => homepageContent.value?.announcements ?? [])

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

function nextBanner() {
  const banners = homepageContent.value?.banners ?? []
  if (!banners.length) return
  activeBannerIndex.value = (activeBannerIndex.value + 1) % banners.length
}

function previousBanner() {
  const banners = homepageContent.value?.banners ?? []
  if (!banners.length) return
  activeBannerIndex.value = (activeBannerIndex.value - 1 + banners.length) % banners.length
}

function goToBanner(index: number) {
  const banners = homepageContent.value?.banners ?? []
  if (!banners.length || index < 0 || index >= banners.length) return
  activeBannerIndex.value = index
}

function startBannerRotation() {
  const banners = homepageContent.value?.banners ?? []
  if (bannerTimer || banners.length <= 1) return
  bannerTimer = setInterval(() => {
    nextBanner()
  }, 8000)
}

function stopBannerRotation() {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}

function formatDiscount(discountRate?: number | null) {
  if (typeof discountRate !== 'number' || !Number.isFinite(discountRate) || discountRate <= 0) {
    return null
  }
  const scaled = Math.round(discountRate * 100) / 10
  return Number.isInteger(scaled) ? `${scaled.toFixed(0)}折` : `${scaled.toFixed(1)}折`
}

function formatDateRange(start?: string | null, end?: string | null) {
  const formatterOptions: Intl.DateTimeFormatOptions = { month: '2-digit', day: '2-digit' }
  const format = (value: string) => {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return null
    return date.toLocaleDateString('zh-CN', formatterOptions)
  }

  const startDate = start ? format(start) : null
  const endDate = end ? format(end) : null

  if (startDate && endDate) {
    return `${startDate} - ${endDate}`
  }
  if (startDate) {
    return `${startDate} 起`
  }
  if (endDate) {
    return `截至 ${endDate}`
  }
  return ''
}

function formatAnnouncementTime(value?: string | null) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

async function fetchOverview() {
  try {
    const { data } = await api.get<ProductOverview>('/products/overview')
    overview.value = data
  } catch (err) {
    console.warn('无法加载统计信息', err)
  }
}

async function fetchHomepageContent() {
  homepageLoading.value = true
  homepageError.value = null
  try {
    const { data } = await api.get<HomepageContent>('/homepage')
    homepageContent.value = data
    activeBannerIndex.value = 0
    stopBannerRotation()
    if (data.banners?.length && data.banners.length > 1) {
      startBannerRotation()
    }
  } catch (err) {
    homepageError.value = err instanceof Error ? err.message : '加载首页信息失败'
    homepageContent.value = null
    stopBannerRotation()
  } finally {
    homepageLoading.value = false
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
  await Promise.all([
    fetchHomepageContent(),
    fetchOverview(),
    fetchCategories(),
    fetchSuppliers(),
  ])
  await fetchProducts()
})

onBeforeUnmount(() => {
  stopBannerRotation()
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

    <section class="spotlight" aria-label="轮播推荐与热销资讯">
      <div class="spotlight-grid">
        <div class="banner-column">
          <div v-if="homepageLoading" class="card loading-card">轮播内容加载中...</div>
          <div v-else-if="homepageError" class="card error-card">{{ homepageError }}</div>
          <div
            v-else-if="homepageContent?.banners?.length"
            class="banner-viewport"
            @mouseenter="stopBannerRotation"
            @mouseleave="startBannerRotation"
          >
            <article
              v-for="(banner, index) in homepageContent.banners"
              v-show="index === activeBannerIndex"
              :key="banner.id"
              class="banner-card"
            >
              <div
                class="banner-media"
                :style="banner.imageUrl ? { backgroundImage: `url(${banner.imageUrl})` } : undefined"
                role="img"
                :aria-label="banner.title"
              ></div>
              <div class="banner-body">
                <p class="eyebrow">精选活动</p>
                <h2>{{ banner.title }}</h2>
                <p class="description">{{ banner.description }}</p>
                <a v-if="banner.linkUrl" class="banner-link" :href="banner.linkUrl">
                  {{ banner.ctaText ?? '了解详情' }}
                </a>
              </div>
            </article>

            <div class="banner-controls" v-if="homepageContent.banners.length > 1">
              <button type="button" class="nav prev" @click="previousBanner" aria-label="上一条轮播">‹</button>
              <button type="button" class="nav next" @click="nextBanner" aria-label="下一条轮播">›</button>
              <div class="dots" role="tablist" aria-label="轮播切换">
                <button
                  v-for="(banner, index) in homepageContent.banners"
                  :key="banner.id + index"
                  type="button"
                  :class="{ active: index === activeBannerIndex }"
                  :aria-label="`切换到${banner.title}`"
                  @click="goToBanner(index)"
                ></button>
              </div>
            </div>
          </div>
          <div v-else class="card empty-card">暂无轮播内容</div>
        </div>

        <aside class="insights" aria-label="热销排行与平台公告">
          <section class="card hot-board" aria-label="热销排行">
            <header>
              <h3>热销排行</h3>
              <p>实时关注上架热度，销量驱动选品</p>
            </header>
            <ol v-if="topHotProducts.length">
              <li v-for="(item, index) in topHotProducts" :key="item.id">
                <span class="rank">{{ index + 1 }}</span>
                <div class="detail">
                  <p class="name">{{ item.name }}</p>
                  <p class="meta">销量 {{ item.sales ?? 0 }} 件 · 库存 {{ item.stock ?? 0 }}</p>
                </div>
              </li>
            </ol>
            <p v-else class="empty">暂无热销数据</p>
          </section>

          <section class="card announcement-board" aria-label="平台公告">
            <header>
              <h3>平台公告</h3>
              <p>物流政策、系统维护、活动预告实时掌握</p>
            </header>
            <ul v-if="announcementList.length">
              <li v-for="announcement in announcementList" :key="announcement.id">
                <span class="type">{{ announcement.type }}</span>
                <div class="detail">
                  <p class="title">{{ announcement.title }}</p>
                  <p class="time">{{ formatAnnouncementTime(announcement.publishedAt) }}</p>
                </div>
              </li>
            </ul>
            <p v-else class="empty">暂无公告</p>
          </section>
        </aside>
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

    <section class="promotions" v-if="homepageContent?.promotions?.length">
      <header class="section-header">
        <div>
          <p class="section-eyebrow">促销活动</p>
          <h2>多渠道权益，驱动下单转化</h2>
        </div>
        <p class="section-subtitle">联合供应商补贴、限时秒杀、会员优惠等玩法覆盖全场景</p>
      </header>
      <div class="promotion-grid">
        <article v-for="promotion in homepageContent.promotions" :key="promotion.id" class="promotion-card">
          <div class="promotion-top">
            <span v-if="formatDiscount(promotion.discountRate)" class="discount">
              {{ formatDiscount(promotion.discountRate) }}
            </span>
            <div class="tags" v-if="promotion.tags?.length">
              <span v-for="tag in promotion.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
          <h3>{{ promotion.title }}</h3>
          <p>{{ promotion.description }}</p>
          <footer>
            <span>{{ formatDateRange(promotion.startDate, promotion.endDate) }}</span>
          </footer>
        </article>
      </div>
    </section>

    <section class="recommendations" v-if="recommendationShowcase.length">
      <header class="section-header">
        <div>
          <p class="section-eyebrow">严选推荐</p>
          <h2>推荐好物 · 口碑热度双在线</h2>
        </div>
        <p class="section-subtitle">根据上新节奏与销量表现优选尖货，助力快速挑选</p>
      </header>
      <div class="recommendation-grid">
        <ProductCard v-for="product in recommendationShowcase" :key="product.id" :product="product" />
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

.spotlight {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.spotlight-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 1.75rem;
}

.banner-column {
  position: relative;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.5rem;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
  padding: 1.75rem;
}

.loading-card,
.empty-card {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  font-weight: 600;
  color: rgba(28, 28, 30, 0.75);
}

.error-card {
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
  border: 1px solid rgba(185, 28, 28, 0.2);
}

.banner-viewport {
  position: relative;
  overflow: hidden;
  min-height: 320px;
  border-radius: 1.75rem;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.25), rgba(111, 169, 173, 0.35));
}

.banner-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 1fr);
  gap: 2rem;
  align-items: stretch;
  height: 100%;
  padding: 2.25rem;
  color: #1c1c1e;
}

.banner-media {
  border-radius: 1.5rem;
  background-size: cover;
  background-position: center;
  min-height: 240px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.banner-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
}

.banner-body h2 {
  font-size: clamp(1.5rem, 2.2vw, 2.2rem);
  font-weight: 700;
}

.banner-body .description {
  color: rgba(28, 28, 30, 0.7);
  line-height: 1.6;
}

.eyebrow {
  font-size: 0.8rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(92, 44, 12, 0.6);
  font-weight: 600;
}

.banner-link {
  align-self: flex-start;
  padding: 0.6rem 1.4rem;
  border-radius: 999px;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 12px 24px rgba(242, 142, 28, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.banner-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(242, 142, 28, 0.3);
}

.banner-controls {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
  padding: 0 1rem;
}

.banner-controls .nav {
  pointer-events: auto;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: #1c1c1e;
  font-size: 1.5rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.15);
  transition: transform 0.2s ease;
}

.banner-controls .nav:hover {
  transform: translateY(-2px);
}

.banner-controls .dots {
  position: absolute;
  left: 50%;
  bottom: 1.25rem;
  transform: translateX(-50%);
  display: flex;
  gap: 0.5rem;
  pointer-events: auto;
}

.banner-controls .dots button {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.2s ease;
}

.banner-controls .dots button.active {
  background: #f28e1c;
}

.insights {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.hot-board header,
.announcement-board header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}

.hot-board header h3,
.announcement-board header h3 {
  font-size: 1.1rem;
  font-weight: 700;
}

.hot-board header p,
.announcement-board header p {
  font-size: 0.85rem;
  color: rgba(28, 28, 30, 0.65);
}

.hot-board ol {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.hot-board li {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.hot-board .rank {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(242, 177, 66, 0.18);
  color: #c2410c;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.hot-board .detail .name {
  font-weight: 600;
  font-size: 0.95rem;
}

.hot-board .detail .meta {
  font-size: 0.8rem;
  color: rgba(28, 28, 30, 0.6);
}

.announcement-board ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.announcement-board li {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem;
  align-items: start;
}

.announcement-board .type {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  background: rgba(111, 169, 173, 0.15);
  color: #0f766e;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.announcement-board .detail .title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1c1c1e;
}

.announcement-board .detail .time {
  font-size: 0.75rem;
  color: rgba(28, 28, 30, 0.55);
}

.card .empty {
  font-size: 0.85rem;
  color: rgba(28, 28, 30, 0.6);
}

.promotions {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
}

.section-eyebrow {
  font-size: 0.8rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(92, 44, 12, 0.6);
  font-weight: 600;
}

.section-subtitle {
  color: rgba(28, 28, 30, 0.6);
  max-width: 420px;
  font-size: 0.9rem;
  line-height: 1.5;
}

.promotion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.5rem;
}

.promotion-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.5rem;
  padding: 1.75rem;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.promotion-card h3 {
  font-size: 1.1rem;
  font-weight: 700;
}

.promotion-card p {
  color: rgba(28, 28, 30, 0.68);
  line-height: 1.6;
}

.promotion-card footer {
  margin-top: auto;
  font-size: 0.8rem;
  color: rgba(28, 28, 30, 0.6);
}

.promotion-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.discount {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.4rem 0.85rem;
  border-radius: 999px;
  background: rgba(242, 142, 28, 0.15);
  color: #c2410c;
  font-weight: 700;
  font-size: 0.95rem;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tags .tag {
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  background: rgba(111, 169, 173, 0.16);
  color: #0f766e;
  font-size: 0.75rem;
  font-weight: 600;
}

.recommendations {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.5rem;
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

@media (max-width: 1024px) {
  .spotlight-grid {
    grid-template-columns: 1fr;
  }

  .banner-card {
    grid-template-columns: 1fr;
    padding: 2rem;
  }

  .insights {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .insights > .card {
    flex: 1 1 260px;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 1.75rem;
  }

  .spotlight {
    gap: 1.25rem;
  }

  .banner-card {
    padding: 1.75rem;
  }

  .banner-controls .nav {
    display: none;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .promotion-grid,
  .recommendation-grid {
    grid-template-columns: 1fr;
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
  .actions button.secondary,
  .banner-body .description,
  .hot-board header p,
  .announcement-board header p,
  .announcement-board .detail .time,
  .section-subtitle,
  .card .empty {
    color: rgba(226, 232, 240, 0.75);
  }

  .card,
  .overview-card,
  .filters,
  .banner-viewport,
  .promotion-card,
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

  .banner-controls .nav {
    background: rgba(15, 23, 42, 0.75);
    color: #f9fafb;
  }

  .banner-controls .dots button {
    background: rgba(148, 163, 184, 0.4);
  }

  .banner-controls .dots button.active {
    background: #fbbf24;
  }

  .announcement-board .type {
    background: rgba(45, 212, 191, 0.18);
    color: #5eead4;
  }
}
</style>
