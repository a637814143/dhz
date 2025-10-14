<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import api from '@/services/api'
import PurchaseDialog from '@/components/PurchaseDialog.vue'
import type { ProductDetail, ProductSummary, PurchaseOrderResult } from '@/types'

const route = useRoute()
const router = useRouter()
const product = ref<ProductDetail | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const activeImageIndex = ref(0)
const purchaseOpen = ref(false)
const purchaseSuccessMessage = ref<string | null>(null)
const purchaseMessageTimer = ref<number | null>(null)

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
    return '—'
  }
  const raw = product.value.price as number | string | null | undefined
  if (raw === null || raw === undefined) {
    return '—'
  }
  const numeric = typeof raw === 'number' ? raw : Number(raw)
  if (!Number.isFinite(numeric)) {
    return '—'
  }
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(numeric)
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

const purchaseButtonText = computed(() => {
  if (!product.value) {
    return '立即购买'
  }
  return isPurchasable.value ? '立即购买' : '暂不可购'
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
    return
  }

  loading.value = true
  error.value = null

  try {
    const { data } = await api.get<ProductDetail>(`/products/${numericId}`)
    product.value = data
    activeImageIndex.value = 0
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载商品信息失败'
    product.value = null
  } finally {
    loading.value = false
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

function openPurchaseDialog() {
  if (!isPurchasable.value) {
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
                <span class="value">{{ product.stock ?? 0 }}</span>
              </div>
              <div>
                <span class="label">累计销量</span>
                <span class="value">{{ product.sales ?? 0 }}</span>
              </div>
              <div>
                <span class="label">状态</span>
                <span class="value">{{ statusLabel }}</span>
              </div>
            </div>

            <div class="cta">
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
}
</style>
