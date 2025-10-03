<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/services/api'
import type {
  Announcement,
  HomepageContent,
  NewsItem,
  ProductOverview,
  ProductSummary,
} from '@/types'

const overview = ref<ProductOverview | null>(null)
const announcements = ref<Announcement[]>([])
const news = ref<NewsItem[]>([])
const hotProducts = ref<ProductSummary[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function loadOverview() {
  const { data } = await api.get<ProductOverview>('/products/overview')
  overview.value = data
}

async function loadHomeContent() {
  const { data } = await api.get<HomepageContent>('/content/home')
  announcements.value = data.announcements
  news.value = data.news
  hotProducts.value = data.hotSales.slice(0, 5)
}

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([loadOverview(), loadHomeContent()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载管理数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

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
      <RouterLink class="manage-link" to="/admin/consumers">采购账号管理</RouterLink>
    </header>

    <div v-if="loading" class="placeholder">正在加载平台数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
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

.manage-link {
  padding: 0.6rem 1.4rem;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.18);
  color: #b45309;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.2s ease;
}

.manage-link:hover {
  background: rgba(249, 115, 22, 0.28);
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
