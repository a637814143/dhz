<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import api from '@/services/api'
import PurchaseDialog from '@/components/PurchaseDialog.vue'
import type { ProductDetail, ProductReview, ProductSummary, PurchaseOrderResult } from '@/types'
import { useAuthState } from '@/services/authState'

const route = useRoute()
const router = useRouter()
const product = ref<ProductDetail | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const activeImageIndex = ref(0)
const purchaseOpen = ref(false)
const purchaseSuccessMessage = ref<string | null>(null)
const purchaseMessageTimer = ref<number | null>(null)
const addingToCart = ref(false)
const cartSuccessMessage = ref<string | null>(null)
const cartErrorMessage = ref<string | null>(null)
const cartMessageTimer = ref<number | null>(null)
const favoriting = ref(false)
const favoriteSuccessMessage = ref<string | null>(null)
const favoriteErrorMessage = ref<string | null>(null)
const favoriteMessageTimer = ref<number | null>(null)
const authPromptMessage = ref<string | null>(null)
const authPromptTimer = ref<number | null>(null)
const reviews = ref<ProductReview[]>([])
const reviewsLoading = ref(false)
const reviewsError = ref<string | null>(null)
const { isAuthenticated, hasRole } = useAuthState()
const isConsumerAccount = computed(() => hasRole('consumer'))

const statusLabel = computed(() => {
  const labelMap: Record<string, string> = {
    ON_SALE: '上架中',
    OFF_SALE: '已下架',
  }
  return product.value?.status ? labelMap[product.value.status] ?? product.value.status : '未知状态'
})

const statusClass = computed(() => {
  const map: Record<string, string> = {
    ON_SALE: 'is-online',
    OFF_SALE: 'is-offline',
  }
  return product.value?.status ? map[product.value.status] ?? 'is-default' : 'is-default'
})

const formattedPrice = computed(() => {
  if (!product.value) {
    return '--'
  }
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(product.value.price)
})

const galleryImages = computed(() => {
  if (!product.value) {
    return [] as { id: string; url: string }[]
  }
  const images: { id: string; url: string }[] = []
  if (product.value.mainImage) {
    images.push({ id: 'main', url: product.value.mainImage })
  }
  const extras = product.value.images?.filter((img) => Boolean(img?.imageUrl)) ?? []
  extras
    .filter((img) => img.imageUrl !== product.value?.mainImage)
    .forEach((img, index) => {
      images.push({ id: `extra-${img.id ?? index}`, url: img.imageUrl! })
    })
  return images
})

const activeImage = computed(() => galleryImages.value[activeImageIndex.value] ?? null)

const hasGallery = computed(() => galleryImages.value.length > 0)

const fallbackLetter = computed(() => product.value?.name?.charAt(0)?.toUpperCase() ?? '丝')

const isPurchasable = computed(() => {
  if (!product.value) {
    return false
  }
  return product.value.status === 'ON_SALE' && product.value.stock > 1
})

const canAddToCart = computed(() => {
  if (!product.value) {
    return false
  }
  return product.value.status === 'ON_SALE' && (product.value.stock ?? 0) > 0
})

const purchaseButtonText = computed(() => {
  if (!product.value) {
    return '立即购买'
  }
  return isPurchasable.value ? '立即购买' : '暂不可购'
})

const consumerReviews = computed(() => {
  const filtered = reviews.value.filter((review) => {
    const role = review.authorRole?.toUpperCase()
    if (role) {
      return role === 'CONSUMER'
    }
    return Boolean(review.consumerId ?? review.consumerName)
  })

  return [...filtered].sort((a, b) => {
    const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
    const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
    if (timeA !== timeB) {
      return timeB - timeA
    }
    return (b.id ?? 0) - (a.id ?? 0)
  })
})

const purchaseTarget = computed<ProductSummary | null>(() => (product.value ? (product.value as ProductSummary) : null))

const createdAtText = computed(() => {
  if (!product.value?.createdAt) {
    return null
  }
  try {
    return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(product.value.createdAt)
    )
  } catch (err) {
    return product.value.createdAt
  }
})

const updatedAtText = computed(() => {
  if (!product.value?.updatedAt) {
    return null
  }
  try {
    return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(product.value.updatedAt)
    )
  } catch (err) {
    return product.value.updatedAt
  }
})

watch(
  () => galleryImages.value.length,
  (length) => {
    if (length === 0) {
      activeImageIndex.value = 0
      return
    }
    if (activeImageIndex.value >= length) {
      activeImageIndex.value = 0
    }
  },
  { immediate: true }
)

async function loadProduct(idParam: string | string[]) {
  const idValue = Array.isArray(idParam) ? idParam[0] : idParam
  const numericId = Number.parseInt(idValue ?? '', 10)
  if (!Number.isFinite(numericId) || numericId <= 0) {
    error.value = '无法识别的商品编号'
    product.value = null
    reviews.value = []
    reviewsError.value = null
    return
  }

  loading.value = true
  error.value = null

  try {
    const { data } = await api.get<ProductDetail>(`/products/${numericId}`)
    product.value = data
    activeImageIndex.value = 0
    await loadReviews(numericId)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载商品信息失败'
    product.value = null
    reviews.value = []
    reviewsError.value = null
  } finally {
    loading.value = false
  }
}

async function loadReviews(productId: number) {
  reviewsLoading.value = true
  reviewsError.value = null
  try {
    const { data } = await api.get<ProductReview[]>(`/reviews/products/${productId}`)
    reviews.value = Array.isArray(data) ? data : []
  } catch (err) {
    reviews.value = []
    reviewsError.value = err instanceof Error ? err.message : '加载评论失败'
  } finally {
    reviewsLoading.value = false
  }
}

function reviewInitial(review: ProductReview) {
  const name = review.consumerName || review.authorName || review.productName || '丝'
  return name.charAt(0).toUpperCase()
}

function reviewAuthorLabel(review: ProductReview) {
  return review.consumerName || review.authorName || '消费者'
}

function formatReviewTime(value?: string | null) {
  if (!value) return '时间未知'
  try {
    return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  } catch (err) {
    return value
  }
}

function retry() {
  loadProduct(route.params.id)
}

function setActiveImage(index: number) {
  if (index >= 0 && index < galleryImages.value.length) {
    activeImageIndex.value = index
  }
}

function goBack() {
  router.back()
}

function showLoginPrompt(message = '请登录后再操作') {
  authPromptMessage.value = message
  if (authPromptTimer.value) {
    clearTimeout(authPromptTimer.value)
  }
  authPromptTimer.value = window.setTimeout(() => {
    authPromptMessage.value = null
    authPromptTimer.value = null
  }, 5000)
}

function openPurchaseDialog() {
  if (!isPurchasable.value) {
    return
  }
  if (!isAuthenticated.value) {
    showLoginPrompt('请登录后再购买商品')
    return
  }
  if (!isConsumerAccount.value) {
    showLoginPrompt('请注册并登录消费者账户后即可购买')
    return
  }
  purchaseOpen.value = true
}

function closePurchaseDialog() {
  purchaseOpen.value = false
}

function clearPurchaseMessageTimer() {
  if (purchaseMessageTimer.value) {
    clearTimeout(purchaseMessageTimer.value)
    purchaseMessageTimer.value = null
  }
}

function clearCartMessageTimer() {
  if (cartMessageTimer.value) {
    clearTimeout(cartMessageTimer.value)
    cartMessageTimer.value = null
  }
}

function clearFavoriteMessageTimer() {
  if (favoriteMessageTimer.value) {
    clearTimeout(favoriteMessageTimer.value)
    favoriteMessageTimer.value = null
  }
}

async function addToCart() {
  if (!product.value || !canAddToCart.value || addingToCart.value) {
    return
  }
  if (!isAuthenticated.value) {
    showLoginPrompt('请登录后再加入购物车')
    return
  }
  if (!isConsumerAccount.value) {
    showLoginPrompt('请注册并登录消费者账户后即可购买')
    return
  }
  addingToCart.value = true
  cartSuccessMessage.value = null
  cartErrorMessage.value = null
  try {
    await api.post('/cart', { productId: product.value.id, quantity: 1 })
    cartErrorMessage.value = null
    cartSuccessMessage.value = `已将「${product.value.name}」加入购物车`
    clearCartMessageTimer()
    cartMessageTimer.value = window.setTimeout(() => {
      cartSuccessMessage.value = null
      cartMessageTimer.value = null
    }, 4000)
  } catch (err) {
    const message = err instanceof Error ? err.message : '加入购物车失败'
    cartSuccessMessage.value = null
    cartErrorMessage.value = message
    clearCartMessageTimer()
    cartMessageTimer.value = window.setTimeout(() => {
      cartErrorMessage.value = null
      cartMessageTimer.value = null
    }, 6000)
  } finally {
    addingToCart.value = false
  }
}

async function addToFavorites() {
  if (!product.value || favoriting.value) {
    return
  }
  if (!isAuthenticated.value) {
    showLoginPrompt('请登录后再收藏商品')
    return
  }
  if (!isConsumerAccount.value) {
    showLoginPrompt('请注册并登录消费者账户后即可收藏商品')
    return
  }
  favoriting.value = true
  favoriteSuccessMessage.value = null
  favoriteErrorMessage.value = null
  try {
    await api.post('/favorites', { productId: product.value.id })
    favoriteSuccessMessage.value = `已收藏「${product.value.name}」`
    clearFavoriteMessageTimer()
    favoriteMessageTimer.value = window.setTimeout(() => {
      favoriteSuccessMessage.value = null
      favoriteMessageTimer.value = null
    }, 4000)
  } catch (err) {
    const message = err instanceof Error ? err.message : '收藏商品失败'
    favoriteErrorMessage.value = message
    clearFavoriteMessageTimer()
    favoriteMessageTimer.value = window.setTimeout(() => {
      favoriteErrorMessage.value = null
      favoriteMessageTimer.value = null
    }, 6000)
  } finally {
    favoriting.value = false
  }
}

async function handlePurchaseSuccess(order: PurchaseOrderResult) {
  const orderNo = order.orderNo ? `（订单号：${order.orderNo}）` : ''
  const lookup = order.consumerLookupId ? `（查询编号：${order.consumerLookupId}）` : ''
  purchaseSuccessMessage.value = `下单成功！${orderNo}${lookup}`.trim()
  clearPurchaseMessageTimer()
  purchaseMessageTimer.value = window.setTimeout(() => {
    purchaseSuccessMessage.value = null
    purchaseMessageTimer.value = null
  }, 6000)

  if (route.params.id) {
    await loadProduct(route.params.id)
  }
}

onMounted(() => {
  loadProduct(route.params.id)
})

watch(
  () => route.params.id,
  (id, oldId) => {
    if (id !== oldId) {
      loadProduct(id)
    }
  }
)

watch(
  () => product.value,
  (value) => {
    if (!value) {
      purchaseOpen.value = false
    }
  }
)

onBeforeUnmount(() => {
  clearPurchaseMessageTimer()
  clearCartMessageTimer()
  clearFavoriteMessageTimer()
  if (authPromptTimer.value) {
    clearTimeout(authPromptTimer.value)
  }
})
</script>

<template>
  <section class="product-detail">
    <div class="page-container">
      <nav class="breadcrumb" aria-label="面包屑导航">
        <RouterLink to="/">产品中心</RouterLink>
        <span aria-hidden="true">/</span>
        <span>{{ product?.name ?? '商品详情' }}</span>
      </nav>

      <div v-if="loading" class="state-card">
        <p>正在加载商品信息，请稍候...</p>
      </div>

      <div v-else-if="error" class="state-card is-error">
        <p>{{ error }}</p>
        <div class="actions">
          <button type="button" @click="retry">重试</button>
          <RouterLink to="/">返回产品中心</RouterLink>
        </div>
      </div>

      <article v-else-if="product" class="detail-card">
        <header class="detail-header">
          <button type="button" class="back" @click="goBack">返回上一页</button>
          <span class="status" :class="statusClass">{{ statusLabel }}</span>
        </header>

        <transition name="fade">
          <p v-if="authPromptMessage" class="info-message" role="status">{{ authPromptMessage }}</p>
        </transition>
        <transition name="fade">
          <p v-if="cartSuccessMessage" class="success-message" role="status">{{ cartSuccessMessage }}</p>
        </transition>
        <transition name="fade">
          <p v-if="cartErrorMessage" class="error-message" role="alert">{{ cartErrorMessage }}</p>
        </transition>
        <transition name="fade">
          <p v-if="favoriteSuccessMessage" class="success-message" role="status">
            {{ favoriteSuccessMessage }}
          </p>
        </transition>
        <transition name="fade">
          <p v-if="favoriteErrorMessage" class="error-message" role="alert">
            {{ favoriteErrorMessage }}
          </p>
        </transition>
        <transition name="fade">
          <p v-if="purchaseSuccessMessage" class="success-message" role="status">{{ purchaseSuccessMessage }}</p>
        </transition>

        <div class="detail-body">
          <div class="gallery" aria-label="商品图片预览">
            <div class="main-image" role="img" :aria-label="product.name">
              <img v-if="hasGallery && activeImage" :src="activeImage.url" :alt="product.name" loading="lazy" />
              <span v-else class="placeholder">{{ fallbackLetter }}</span>
            </div>
            <ul v-if="hasGallery" class="thumbnail-list" role="list">
              <li v-for="(img, index) in galleryImages" :key="img.id">
                <button
                  type="button"
                  :class="['thumbnail', { active: index === activeImageIndex }]"
                  @click="setActiveImage(index)"
                >
                  <img :src="img.url" :alt="`${product.name} 图片 ${index + 1}`" loading="lazy" />
                </button>
              </li>
            </ul>
          </div>

          <div class="info">
            <h1>{{ product.name }}</h1>
            <p class="price">{{ formattedPrice }}</p>
            <p class="description">{{ product.description || '这款蚕制品暂未填写详细介绍。' }}</p>

            <dl class="meta">
              <div>
                <dt>分类</dt>
                <dd>{{ product.category?.name ?? '未分类' }}</dd>
              </div>
              <div>
                <dt>供应商</dt>
                <dd>
                  {{ product.supplier?.companyName ?? '暂未关联供应商' }}
                  <template v-if="product.supplier?.supplierLevel">（{{ product.supplier.supplierLevel }}）</template>
                </dd>
              </div>
              <div v-if="product.supplier?.contactName || product.supplier?.contactPhone">
                <dt>联系人</dt>
                <dd>
                  {{ product.supplier?.contactName ?? '暂无联系人' }}
                  <template v-if="product.supplier?.contactPhone">（{{ product.supplier.contactPhone }}）</template>
                </dd>
              </div>
              <div v-if="createdAtText">
                <dt>创建时间</dt>
                <dd>{{ createdAtText }}</dd>
              </div>
              <div v-if="updatedAtText">
                <dt>最近更新</dt>
                <dd>{{ updatedAtText }}</dd>
              </div>
            </dl>

            <div class="stats">
              <div>
                <span class="label">库存</span>
                <span class="value">{{ product.stock }}</span>
              </div>
              <div>
                <span class="label">累计销量</span>
                <span class="value">{{ product.sales }}</span>
              </div>
              <div>
                <span class="label">状态</span>
                <span class="value">{{ statusLabel }}</span>
              </div>
            </div>
            <div class="cta">
              <button
                type="button"
                class="add-to-cart"
                :disabled="!canAddToCart || addingToCart"
                @click="addToCart"
              >
                {{ addingToCart ? '加入中…' : '加入购物车' }}
              </button>
              <button
                type="button"
                class="favorite"
                :disabled="favoriting"
                @click="addToFavorites"
              >
                {{ favoriting ? '收藏中…' : '收藏' }}
              </button>
              <button
                type="button"
                class="purchase"
                :disabled="!isPurchasable"
                @click="openPurchaseDialog"
              >
                {{ purchaseButtonText }}
              </button>
              <RouterLink class="back-link" to="/">返回产品中心</RouterLink>
            </div>
          </div>
        </div>
      </article>
    </div>

    <PurchaseDialog
      :open="purchaseOpen"
      :product="purchaseTarget"
      @close="closePurchaseDialog"
      @success="handlePurchaseSuccess"
    />
  </section>

  <section class="review-section" aria-labelledby="consumer-reviews-title">
    <div class="page-container">
      <div class="reviews-card">
        <header class="reviews-header">
          <div>
            <p class="reviews-subtitle">消费者说</p>
            <h2 id="consumer-reviews-title">最新评论</h2>
          </div>
          <span class="reviews-count" aria-live="polite">
            共 {{ consumerReviews.length }} 条评价
          </span>
        </header>

        <div v-if="reviewsLoading" class="reviews-state">正在加载评论…</div>
        <div v-else-if="reviewsError" class="reviews-state is-error">{{ reviewsError }}</div>
        <div v-else-if="consumerReviews.length === 0" class="reviews-state">暂时还没有消费者评论。</div>
        <ul v-else class="reviews-list" role="list">
          <li v-for="review in consumerReviews" :key="review.id" class="review-item">
            <div class="avatar" aria-hidden="true">
              <span>{{ reviewInitial(review) }}</span>
            </div>
            <div class="review-content">
              <header class="review-meta">
                <div>
                  <p class="author">{{ reviewAuthorLabel(review) }}</p>
                  <p class="time">{{ formatReviewTime(review.createdAt) }}</p>
                </div>
                <span class="rating">评分：{{ review.rating }} 分</span>
              </header>
              <p class="comment">
                {{ review.comment && review.comment.trim() ? review.comment : '（未填写文字评价）' }}
              </p>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<style scoped>
.product-detail {
  padding: 3rem 1.5rem 4rem;
  background: linear-gradient(180deg, rgba(255, 250, 245, 0.9), rgba(255, 255, 255, 0.6));
  min-height: 100vh;
}

.page-container {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: rgba(17, 24, 39, 0.6);
}

.breadcrumb a {
  color: #6f9aa9;
  text-decoration: none;
}

.breadcrumb a:hover {
  text-decoration: underline;
}

.state-card {
  padding: 2rem;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  text-align: center;
  color: rgba(17, 24, 39, 0.7);
}

.state-card.is-error {
  color: #b91c1c;
}

.state-card .actions {
  margin-top: 1.25rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.state-card button {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.5rem;
  background: #f2b142;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

.state-card button:hover {
  background: #e3a235;
}

.state-card a {
  color: #6f9aa9;
}

.detail-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.75rem;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.12);
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.detail-header .back {
  border: none;
  background: transparent;
  color: #6f9aa9;
  cursor: pointer;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
}

.detail-header .back:hover {
  text-decoration: underline;
}

.status {
  padding: 0.4rem 1rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  background: rgba(111, 154, 169, 0.12);
  color: #0f172a;
}

.status.is-online {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status.is-offline {
  background: rgba(248, 113, 113, 0.15);
  color: #b91c1c;
}

.detail-body {
  display: grid;
  gap: 2.5rem;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.main-image {
  position: relative;
  border-radius: 1.25rem;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.25), rgba(111, 169, 173, 0.35));
  padding: 1rem;
  display: grid;
  place-items: center;
  min-height: 320px;
}

.main-image img {
  width: 100%;
  max-height: 520px;
  object-fit: contain;
  border-radius: 1rem;
}

.placeholder {
  font-size: 5rem;
  font-weight: 700;
  color: rgba(92, 44, 12, 0.7);
}

.thumbnail-list {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.thumbnail {
  border: none;
  padding: 0;
  background: transparent;
  border-radius: 0.75rem;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.15);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.thumbnail img {
  width: 80px;
  height: 80px;
  object-fit: cover;
}

.thumbnail:hover,
.thumbnail.active {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(242, 177, 66, 0.28);
}

.info {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.info h1 {
  font-size: 2rem;
  font-weight: 700;
  color: #1c1c1e;
}

.price {
  font-size: 1.5rem;
  font-weight: 700;
  color: #c2410c;
}

.description {
  font-size: 1rem;
  line-height: 1.65;
  color: rgba(17, 24, 39, 0.75);
}

.meta {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.meta dt {
  font-size: 0.8rem;
  color: rgba(17, 24, 39, 0.45);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.meta dd {
  margin: 0.25rem 0 0;
  font-weight: 600;
  color: #0f172a;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
}

.stats .label {
  display: block;
  font-size: 0.75rem;
  color: rgba(17, 24, 39, 0.45);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stats .value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
}
.cta {
  margin-top: 1rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.cta .add-to-cart {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.7rem 1.6rem;
  border-radius: 999px;
  border: 1px solid rgba(111, 169, 173, 0.45);
  background: rgba(255, 255, 255, 0.9);
  color: #0f172a;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease,
    color 0.2s ease;
}

.cta .add-to-cart:hover:not(:disabled) {
  background: rgba(111, 169, 173, 0.12);
  box-shadow: 0 16px 30px rgba(111, 169, 173, 0.2);
  transform: translateY(-1px);
}

.cta .add-to-cart:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

.cta .favorite {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.7rem 1.6rem;
  border-radius: 999px;
  border: 1px solid rgba(242, 177, 66, 0.5);
  background: rgba(242, 177, 66, 0.12);
  color: #b45309;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.cta .favorite:hover:not(:disabled) {
  background: rgba(242, 177, 66, 0.2);
  box-shadow: 0 16px 30px rgba(242, 177, 66, 0.18);
  transform: translateY(-1px);
}

.cta .favorite:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

.cta .purchase {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.75rem;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.85), rgba(111, 169, 173, 0.9));
  color: #fff;
  font-weight: 600;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.cta .purchase:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(242, 177, 66, 0.28);
}

.cta .purchase:disabled {
  background: rgba(156, 163, 175, 0.45);
  box-shadow: none;
  cursor: not-allowed;
  transform: none;
}

.cta .back-link {
  color: #6f9aa9;
  font-weight: 600;
  text-decoration: none;
}

.cta .back-link:hover {
  text-decoration: underline;
}

.success-message {
  padding: 0.85rem 1.25rem;
  border-radius: 0.9rem;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  font-weight: 600;
}

.info-message {
  padding: 0.85rem 1.25rem;
  border-radius: 0.9rem;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

.error-message {
  padding: 0.85rem 1.25rem;
  border-radius: 0.9rem;
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
  font-weight: 600;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.review-section {
  padding: 0 1.5rem 4rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6), rgba(255, 250, 245, 0.9));
}

.reviews-card {
  margin: 0 auto;
  width: 100%;
  max-width: 1100px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1.75rem;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.12);
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.reviews-subtitle {
  margin: 0;
  color: rgba(17, 24, 39, 0.55);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-size: 0.8rem;
}

.reviews-header h2 {
  margin: 0.2rem 0 0;
  font-size: 1.4rem;
  color: #0f172a;
}

.reviews-count {
  font-size: 0.95rem;
  color: rgba(17, 24, 39, 0.65);
}

.reviews-state {
  padding: 1rem;
  text-align: center;
  color: rgba(17, 24, 39, 0.7);
  background: rgba(111, 169, 173, 0.06);
  border-radius: 1rem;
}

.reviews-state.is-error {
  color: #b91c1c;
  background: rgba(248, 113, 113, 0.12);
}

.reviews-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.review-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 1rem;
  padding: 1rem;
  border-radius: 1.25rem;
  background: rgba(248, 250, 252, 0.8);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(242, 177, 66, 0.9), rgba(111, 169, 173, 0.9));
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  font-size: 1.1rem;
}

.review-content {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.review-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.author {
  margin: 0;
  font-weight: 700;
  color: #0f172a;
}

.time {
  margin: 0.1rem 0 0;
  color: rgba(17, 24, 39, 0.55);
  font-size: 0.9rem;
}

.rating {
  color: #b45309;
  font-weight: 700;
}

.comment {
  margin: 0;
  color: rgba(17, 24, 39, 0.82);
  line-height: 1.6;
}

@media (max-width: 960px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .main-image {
    min-height: 260px;
  }
}

@media (prefers-color-scheme: dark) {
  .product-detail {
    background: linear-gradient(180deg, rgba(17, 24, 39, 0.9), rgba(17, 24, 39, 0.65));
  }

  .breadcrumb {
    color: rgba(226, 232, 240, 0.7);
  }

  .breadcrumb a {
    color: #8bc4d2;
  }

  .state-card,
  .detail-card {
    background: rgba(30, 41, 59, 0.75);
    color: #e2e8f0;
  }

  .state-card button {
    background: rgba(242, 177, 66, 0.85);
  }

  .status {
    background: rgba(148, 163, 184, 0.25);
    color: #e2e8f0;
  }

  .success-message {
    background: rgba(34, 197, 94, 0.25);
    color: #bbf7d0;
  }

  .info-message {
    background: rgba(59, 130, 246, 0.22);
    color: #bfdbfe;
  }

  .info h1,
  .price,
  .meta dd,
  .stats .value {
    color: #f9fafb;
  }

  .description,
  .meta dt,
  .stats .label {
    color: rgba(226, 232, 240, 0.72);
  }

  .thumbnail {
    box-shadow: 0 10px 24px rgba(0, 0, 0, 0.45);
  }

  .reviews-card {
    background: rgba(30, 41, 59, 0.8);
    box-shadow: 0 16px 36px rgba(0, 0, 0, 0.35);
  }

  .reviews-header h2,
  .author,
  .comment {
    color: #e2e8f0;
  }

  .reviews-subtitle,
  .reviews-count,
  .time {
    color: rgba(226, 232, 240, 0.7);
  }

  .review-item {
    background: rgba(51, 65, 85, 0.65);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.4);
  }
}
</style>
