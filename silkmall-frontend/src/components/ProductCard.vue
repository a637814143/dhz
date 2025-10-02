<script setup lang="ts">
import { computed } from 'vue'
import type { ProductSummary } from '@/types'

const props = defineProps<{ product: ProductSummary }>()

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
</script>

<template>
  <article class="product-card">
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
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.12);
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
