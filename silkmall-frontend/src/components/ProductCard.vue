<script setup lang="ts">
import { computed, withDefaults } from 'vue'
import type { ProductSummary } from '@/types'

const props = withDefaults(
  defineProps<{ product: ProductSummary; addingToCart?: boolean; favoriting?: boolean }>(),
  {
    addingToCart: false,
    favoriting: false,
  }
)

const emit = defineEmits<{
  (e: 'purchase', product: ProductSummary): void
  (e: 'view-detail', product: ProductSummary): void
  (e: 'add-to-cart', product: ProductSummary): void
  (e: 'favorite', product: ProductSummary): void
}>()

function openDetail() {
  emit('view-detail', props.product)
}

function addToCart() {
  if (props.addingToCart) return
  emit('add-to-cart', props.product)
}

function addToFavorite() {
  if (props.favoriting) return
  emit('favorite', props.product)
}

const statusLabel = computed(() => {
  const labelMap: Record<string, string> = {
    ON_SALE: '上架中',
    OFF_SALE: '已下架',
  }
  return labelMap[props.product.status] ?? props.product.status ?? '未知状态'
})

const statusClass = computed(() => {
  const map: Record<string, string> = {
    ON_SALE: 'is-online',
    OFF_SALE: 'is-offline',
  }
  return map[props.product.status] ?? 'is-default'
})

const fallbackLetter = computed(() => props.product.name?.charAt(0)?.toUpperCase() ?? '丝')

const formattedPrice = computed(() =>
  new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(props.product.price)
)

const canPurchase = computed(() => props.product.status === 'ON_SALE' && props.product.stock > 1)
const canAddToCart = computed(() => props.product.status === 'ON_SALE' && props.product.stock > 0)
</script>

<template>
  <article
    class="product-card"
    role="button"
    tabindex="0"
    :aria-label="`查看商品详情：${product.name}`"
    @click="openDetail"
    @keydown.enter.prevent.stop="openDetail"
    @keydown.space.prevent.stop="openDetail"
  >
    <div class="media">
      <div class="image-frame" role="img" :aria-label="product.name">
        <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" loading="lazy" />
        <span v-else class="placeholder">{{ fallbackLetter }}</span>
      </div>
      <span class="status" :class="statusClass">{{ statusLabel }}</span>
    </div>
    <div class="content">
      <header class="header">
        <h3 class="title">{{ product.name }}</h3>
        <p class="subtitle">{{ product.categoryName ?? '未分类' }}</p>
      </header>
      <p class="description">{{ product.description || '这款蚕制品暂未填写详细介绍。' }}</p>
      <dl class="stats" aria-label="关键指标">
        <div>
          <dt>价格</dt>
          <dd>{{ formattedPrice }}</dd>
        </div>
        <div>
          <dt>库存</dt>
          <dd>{{ product.stock }}</dd>
        </div>
        <div>
          <dt>销量</dt>
          <dd>{{ product.sales }}</dd>
        </div>
      </dl>
      <footer class="meta" v-if="product.supplierName">
        <span>供应商：{{ product.supplierName }}</span>
        <span v-if="product.supplierLevel">等级：{{ product.supplierLevel }}</span>
      </footer>
      <div class="actions">
        <button
          type="button"
          class="add-to-cart"
          :disabled="!canAddToCart || props.addingToCart"
          @click.stop="addToCart"
        >
          {{ props.addingToCart ? '加入中…' : '加入购物车' }}
        </button>
        <button
          type="button"
          class="favorite"
          :disabled="props.favoriting"
          @click.stop="addToFavorite"
        >
          {{ props.favoriting ? '收藏中…' : '收藏商品' }}
        </button>
        <button
          type="button"
          class="buy"
          :disabled="!canPurchase"
          @click.stop="emit('purchase', product)"
        >
          {{ canPurchase ? '点击购买' : '暂不可购' }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  display: grid;
  grid-template-columns: minmax(120px, 140px) 1fr;
  gap: 1.25rem;
  padding: 1.25rem;
  border-radius: 1.25rem;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(6px);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.12);
}

.product-card:focus-visible {
  outline: 3px solid rgba(111, 169, 173, 0.65);
  outline-offset: 4px;
}

.media {
  position: relative;
}

.image-frame {
  position: relative;
  overflow: hidden;
  border-radius: 1rem;
  aspect-ratio: 1;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.25), rgba(111, 169, 173, 0.35));
  display: grid;
  place-items: center;
}

.image-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  font-size: 2rem;
  font-weight: 700;
  color: rgba(92, 44, 12, 0.8);
}

.status {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.9);
  color: rgba(17, 24, 39, 0.7);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}

.status.is-online {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.status.is-offline {
  background: rgba(248, 113, 113, 0.12);
  color: #b91c1c;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1c1c1e;
}

.subtitle {
  font-size: 0.85rem;
  color: rgba(0, 0, 0, 0.45);
}

.description {
  font-size: 0.9rem;
  color: rgba(0, 0, 0, 0.7);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.75rem;
}

.stats dt {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.45);
}

.stats dd {
  font-weight: 700;
  font-size: 1rem;
  color: #1f2937;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: rgba(17, 24, 39, 0.65);
}


.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.actions button {
  border-radius: 999px;
  padding: 0.5rem 1.35rem;
  font-size: 0.9rem;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease,
    background-color 0.2s ease, color 0.2s ease;
}

.actions .add-to-cart {
  border: 1px solid rgba(111, 169, 173, 0.45);
  background: rgba(255, 255, 255, 0.85);
  color: #0f172a;
  cursor: pointer;
}

.actions .add-to-cart:hover:not(:disabled) {
  background: rgba(111, 169, 173, 0.12);
  box-shadow: 0 12px 28px rgba(111, 169, 173, 0.18);
}

.actions .add-to-cart:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  box-shadow: none;
}

.actions .favorite {
  border: 1px solid rgba(242, 177, 66, 0.55);
  background: rgba(242, 177, 66, 0.12);
  color: #b45309;
  cursor: pointer;
}

.actions .favorite:hover:not(:disabled) {
  background: rgba(242, 177, 66, 0.18);
  box-shadow: 0 12px 28px rgba(242, 177, 66, 0.18);
}

.actions .favorite:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  box-shadow: none;
}

.actions .buy {
  border: none;
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.85), rgba(111, 169, 173, 0.85));
  color: #fff;
  box-shadow: 0 16px 36px rgba(242, 177, 66, 0.22);
  cursor: pointer;
}

.actions .buy:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 22px 44px rgba(242, 177, 66, 0.3);
}

.actions .buy:disabled {
  cursor: not-allowed;
  background: rgba(15, 23, 42, 0.1);
  color: rgba(15, 23, 42, 0.45);
  box-shadow: none;
  transform: none;
}

@media (max-width: 768px) {
  .product-card {
    grid-template-columns: 1fr;
  }

  .media {
    display: flex;
    justify-content: center;
  }

  .image-frame {
    width: 160px;
  }
}

@media (prefers-color-scheme: dark) {
  .product-card {
    background: rgba(31, 41, 55, 0.75);
    box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35);
  }

  .title,
  .stats dd {
    color: #f9fafb;
  }

  .subtitle,
  .description,
  .meta,
  .stats dt {
    color: rgba(226, 232, 240, 0.75);
  }

  .status {
    background: rgba(15, 23, 42, 0.75);
    color: rgba(255, 255, 255, 0.7);
  }
}
</style>
