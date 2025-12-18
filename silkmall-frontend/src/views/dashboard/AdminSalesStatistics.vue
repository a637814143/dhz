<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/services/api'
import type { WeeklySalesReport } from '@/types'

const weeklySales = ref<WeeklySalesReport | null>(null)
const weeklyLoading = ref(false)
const weeklyError = ref<string | null>(null)
const selectedWeeks = ref(6)

function formatCurrency(amount?: number | string | null) {
  const numeric = typeof amount === 'string' ? Number(amount) : amount
  if (typeof numeric !== 'number' || Number.isNaN(numeric)) {
    return '¥0.00'
  }
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(numeric)
}

function formatDate(value?: string | Date | null) {
  if (!value) return '—'
  const date = typeof value === 'string' ? new Date(value) : value
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function formatDateOnly(value?: string | Date | null) {
  if (!value) return '—'
  const date = typeof value === 'string' ? new Date(value) : value
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('zh-CN')
}

function formatWeekRange(start?: string | null, end?: string | null) {
  const startText = formatDateOnly(start)
  const endText = formatDateOnly(end)
  if (startText === '—' && endText === '—') return '—'
  if (endText === '—') return startText
  return `${startText} - ${endText}`
}

function formatNumber(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '0'
  return new Intl.NumberFormat('zh-CN').format(value)
}

async function loadWeeklySales() {
  weeklyLoading.value = true
  weeklyError.value = null
  try {
    const { data } = await api.get<WeeklySalesReport>('/analytics/weekly-sales', {
      params: { weeks: selectedWeeks.value },
    })
    weeklySales.value = data ?? null
  } catch (err) {
    weeklySales.value = null
    weeklyError.value = err instanceof Error ? err.message : '加载周度销售数据失败'
  } finally {
    weeklyLoading.value = false
  }
}

onMounted(() => {
  loadWeeklySales()
})
</script>

<template>
  <section class="admin-shell">
    <header class="admin-header">
      <div>
        <h1>销售统计</h1>
        <p>查看每周订单与商品表现，同名商品会按供应商分开统计。</p>
      </div>
      <nav class="admin-actions">
        <RouterLink class="manage-link" to="/admin/products">商品管理</RouterLink>
        <RouterLink class="manage-link" to="/admin/orders">订单管理</RouterLink>
        <RouterLink class="manage-link" to="/admin/consumers">采购账号管理</RouterLink>
        <RouterLink class="manage-link" to="/admin/suppliers">供应商账号管理</RouterLink>
        <RouterLink class="manage-link" to="/admin/sales">销售统计</RouterLink>
      </nav>
    </header>

    <section class="panel weekly-sales" aria-labelledby="weekly-sales-title">
      <header class="weekly-sales-header">
        <div>
          <div class="panel-title" id="weekly-sales-title">周度销售统计</div>
          <p class="panel-subtitle">查看每周订单与商品表现，同名商品会按供应商分开统计。</p>
        </div>
        <label class="weeks-selector">
          <span>统计周数</span>
          <select v-model.number="selectedWeeks" @change="loadWeeklySales" :disabled="weeklyLoading">
            <option :value="4">4 周</option>
            <option :value="6">6 周</option>
            <option :value="8">8 周</option>
            <option :value="12">12 周</option>
          </select>
        </label>
      </header>
      <div v-if="weeklyLoading" class="placeholder">正在加载周度数据…</div>
      <div v-else-if="weeklyError" class="placeholder is-error">{{ weeklyError }}</div>
      <div v-else-if="!weeklySales?.weeks?.length" class="placeholder">暂无销售记录</div>
      <div v-else class="weekly-grid">
        <article v-for="week in weeklySales?.weeks" :key="week.weekStart" class="week-card">
          <header class="week-card__header">
            <div>
              <div class="week-label">{{ formatWeekRange(week.weekStart, week.weekEnd) }}</div>
              <p class="panel-subtitle">
                订单 {{ formatNumber(week.totalOrders) }} · 销量 {{ formatNumber(week.totalQuantity) }}
              </p>
            </div>
            <div class="week-amount">{{ formatCurrency(week.totalRevenue) }}</div>
          </header>
          <div class="week-columns">
            <div class="week-column">
              <h4>订单明细</h4>
              <table v-if="week.orders?.length" class="compact-table">
                <thead>
                  <tr>
                    <th scope="col">订单号</th>
                    <th scope="col">支付时间</th>
                    <th scope="col">金额</th>
                    <th scope="col">数量</th>
                    <th scope="col">商品/供应商</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="order in week.orders" :key="order.id">
                    <td>{{ order.orderNo }}</td>
                    <td>{{ formatDate(order.paymentTime) }}</td>
                    <td>{{ formatCurrency(order.totalAmount) }}</td>
                    <td>{{ formatNumber(order.totalQuantity) }}</td>
                    <td class="order-items">
                      <div
                        v-for="item in order.items"
                        :key="`${order.id}-${item.productId}-${item.supplierId}-${item.productName}`"
                        class="order-item-line"
                      >
                        <span class="product-name">{{ item.productName || '未命名商品' }}</span>
                        <span class="supplier-chip" v-if="item.supplierName">{{ item.supplierName }}</span>
                        <span class="supplier-chip muted" v-else>无供应商</span>
                        × {{ formatNumber(item.quantity) }}
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
              <p v-else class="empty">本周暂无订单数据</p>
            </div>
            <div class="week-column">
              <h4>商品表现</h4>
              <table v-if="week.productPerformances?.length" class="compact-table">
                <thead>
                  <tr>
                    <th scope="col">商品 / 供应商</th>
                    <th scope="col">销量</th>
                    <th scope="col">销售额</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="product in week.productPerformances" :key="`${product.productId}-${product.supplierId}`">
                    <td>
                      <div class="product-line">
                        <span class="product-name">{{ product.productName || '未命名商品' }}</span>
                        <span class="supplier-chip" v-if="product.supplierName">{{ product.supplierName }}</span>
                        <span class="supplier-chip muted" v-else>无供应商</span>
                      </div>
                    </td>
                    <td>{{ formatNumber(product.quantity) }}</td>
                    <td>{{ formatCurrency(product.salesAmount) }}</td>
                  </tr>
                </tbody>
              </table>
              <p v-else class="empty">暂无商品销量</p>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.admin-shell {
  padding: 24px 24px 48px;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.admin-header h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 0.6px;
}

.admin-header p {
  margin: 6px 0 0;
  color: #5e6a71;
}

.admin-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.manage-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #d8e3e8;
  background: #f8fbfd;
  color: #1f3a56;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
}

.manage-link:hover {
  background: #edf5fa;
  border-color: #c6d8e2;
}

.panel {
  background: #ffffff;
  border: 1px solid #e3e8ef;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 10px 30px rgba(16, 55, 92, 0.06);
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}

.panel-subtitle {
  color: #5b7083;
  margin: 6px 0 0;
  font-size: 14px;
}

.placeholder {
  padding: 24px;
  text-align: center;
  color: #708399;
  background: #f8fafc;
  border: 1px dashed #d7e1ec;
  border-radius: 12px;
}

.placeholder.is-error {
  color: #c0392b;
  background: #fff6f5;
  border-color: #f1c4c0;
}

.weekly-sales {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.weekly-sales-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.weeks-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weeks-selector span {
  color: #4a5b6a;
  font-weight: 600;
}

.weeks-selector select {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  min-width: 120px;
}

.weekly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.week-card {
  border: 1px solid #e7edf3;
  border-radius: 14px;
  padding: 14px;
  background: linear-gradient(180deg, #fcfeff 0%, #f6f9fc 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.week-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.week-label {
  font-weight: 700;
  color: #1f2d3d;
}

.week-amount {
  font-weight: 800;
  color: #0c5b9b;
  font-size: 18px;
}

.week-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.week-column h4 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #1c2f40;
}

.compact-table {
  width: 100%;
  border-collapse: collapse;
  background: #ffffff;
  border: 1px solid #e6eef5;
}

.compact-table th,
.compact-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #e6eef5;
  text-align: left;
  vertical-align: top;
}

.compact-table th {
  background: #f0f6fb;
  color: #33475b;
  font-weight: 700;
  font-size: 13px;
}

.compact-table td {
  color: #1f2d3d;
  font-size: 13px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-item-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.product-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.product-name {
  font-weight: 600;
}

.supplier-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  border-radius: 999px;
  background: #e6f2ff;
  color: #0f4b85;
  font-size: 12px;
  border: 1px solid #c2ddff;
}

.supplier-chip.muted {
  background: #f5f7fa;
  border-color: #e2e8f0;
  color: #6b7b8c;
}

.empty {
  margin: 0;
  padding: 10px;
  color: #7a8a9e;
  background: #f7fafc;
  border: 1px dashed #d9e2ec;
  border-radius: 8px;
}
</style>
