<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import type { AdminOrderSummary, PageResponse } from '@/types'

const loading = ref(false)
const error = ref<string | null>(null)
const actionMessage = ref<string | null>(null)
const actionError = ref<string | null>(null)
const approvingId = ref<number | null>(null)

const orders = ref<AdminOrderSummary[]>([])

const filters = reactive({
  receipt: 'all' as 'all' | 'pending' | 'confirmed',
  orderNo: '',
})

const pagination = reactive({
  page: 0,
  size: 8,
  total: 0,
})

const receiptOptions = [
  { value: 'all', label: '全部订单' },
  { value: 'pending', label: '未收货' },
  { value: 'confirmed', label: '已收货' },
] as const

const totalPages = computed(() =>
  pagination.total > 0 ? Math.ceil(pagination.total / pagination.size) : 0
)

watch(
  () => filters.receipt,
  () => {
    pagination.page = 0
    clearFeedback()
    loadOrders()
  }
)

watch(
  () => filters.orderNo,
  (newValue, oldValue) => {
    if (newValue.trim() === '' && oldValue && oldValue.trim() !== '') {
      pagination.page = 0
      clearFeedback()
      loadOrders()
    }
  }
)

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
})

function formatCurrency(amount?: number | null) {
  if (typeof amount !== 'number' || Number.isNaN(amount)) {
    return '¥0.00'
  }
  return currencyFormatter.format(amount)
}

function formatDateTime(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function resolveConsumerConfirmedParam() {
  if (filters.receipt === 'pending') return false
  if (filters.receipt === 'confirmed') return true
  return undefined
}

function clearFeedback() {
  actionMessage.value = null
  actionError.value = null
}

async function loadOrders() {
  loading.value = true
  error.value = null
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: pagination.size,
    }
    const consumerConfirmed = resolveConsumerConfirmedParam()
    if (typeof consumerConfirmed === 'boolean') {
      params.consumerConfirmed = consumerConfirmed
    }
    const orderNo = filters.orderNo.trim()
    if (orderNo) {
      params.orderNo = orderNo
    }

    const { data } = await api.get<PageResponse<AdminOrderSummary>>('/orders/admin', {
      params,
    })

    const content = Array.isArray(data?.content) ? data.content : []
    const normalized = content.map((item) => ({
      ...item,
      items: Array.isArray(item.items) ? item.items : [],
    })) as AdminOrderSummary[]
    orders.value = normalized

    const totalElements = Number((data as PageResponse<AdminOrderSummary> | undefined)?.totalElements)
    pagination.total = Number.isFinite(totalElements) ? totalElements : normalized.length

    const pageNumber = Number((data as PageResponse<AdminOrderSummary> | undefined)?.number)
    if (Number.isFinite(pageNumber)) {
      pagination.page = pageNumber
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载订单失败'
    error.value = message
    orders.value = []
  } finally {
    loading.value = false
  }
}

function canApprove(order: AdminOrderSummary) {
  return order.canApprove && approvingId.value !== order.id
}

async function approveOrder(order: AdminOrderSummary) {
  if (!order.id || !order.canApprove) {
    actionError.value = order.approvalDisabledReason ?? '该订单暂无法确认'
    return
  }

  approvingId.value = order.id
  actionError.value = null
  actionMessage.value = null
  try {
    await api.put(`/orders/${order.id}/approve-payment`)
    actionMessage.value = `订单 ${order.orderNo} 已完成结算，已向供应商支付`;
    await loadOrders()
  } catch (err) {
    const message = err instanceof Error ? err.message : '确认订单失败'
    actionError.value = message
  } finally {
    approvingId.value = null
  }
}

function goToPage(page: number) {
  if (page < 0 || (totalPages.value > 0 && page >= totalPages.value)) {
    return
  }
  if (page === pagination.page) return
  pagination.page = page
  clearFeedback()
  loadOrders()
}

function nextPage() {
  goToPage(pagination.page + 1)
}

function prevPage() {
  goToPage(pagination.page - 1)
}

function submitSearch() {
  pagination.page = 0
  clearFeedback()
  loadOrders()
}

function resetOrderSearch() {
  if (!filters.orderNo) return
  clearFeedback()
  filters.orderNo = ''
}

function receiptTagClass(order: AdminOrderSummary) {
  return order.consumerConfirmed ? 'tag tag--success' : 'tag tag--warning'
}

function payoutTagClass(order: AdminOrderSummary) {
  if (!order.payoutStatus) return 'tag'
  if (order.payoutStatus === '待批准') return 'tag tag--info'
  if (order.payoutStatus === '已批准') return 'tag tag--success'
  if (order.payoutStatus === '已退款') return 'tag tag--danger'
  return 'tag'
}

onMounted(() => {
  loadOrders()
})
</script>

<template>
  <section class="admin-orders">
    <header class="page-header">
      <div>
        <h1>订单管理</h1>
        <p class="subtitle">
          订单资金在消费者付款后托管于平台，消费者确认收货后由管理员结算并自动收取 5% 平台佣金。
        </p>
      </div>
      <form class="filters" @submit.prevent="submitSearch">
        <label>
          <span>收货状态</span>
          <select v-model="filters.receipt">
            <option v-for="option in receiptOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="search-field">
          <span>订单编号</span>
          <div class="search-field__control">
            <input
              v-model="filters.orderNo"
              type="search"
              placeholder="输入订单编号"
              autocomplete="off"
            />
            <button type="submit" :disabled="loading">搜索</button>
            <button
              v-if="filters.orderNo"
              type="button"
              class="ghost"
              @click="resetOrderSearch"
              :disabled="loading"
            >
              清除
            </button>
          </div>
        </label>
      </form>
    </header>

    <transition name="fade">
      <p v-if="actionMessage" class="feedback feedback--success">{{ actionMessage }}</p>
    </transition>
    <transition name="fade">
      <p v-if="actionError" class="feedback feedback--error">{{ actionError }}</p>
    </transition>

    <p v-if="error" class="feedback feedback--error">{{ error }}</p>
    <p v-else-if="loading" class="feedback feedback--info">订单加载中，请稍候…</p>

    <ul v-if="!loading && orders.length" class="order-list">
      <li v-for="order in orders" :key="order.id" class="order-card">
        <header class="order-card__head">
          <div>
            <h2>订单编号：{{ order.orderNo }}</h2>
            <p class="meta">
              下单时间：{{ formatDateTime(order.orderTime) }} · 商品数量：{{ order.totalQuantity ?? 0 }} 件
            </p>
          </div>
          <div class="status-block">
            <span :class="receiptTagClass(order)">收货状态：{{ order.receiptStatus }}</span>
            <span :class="payoutTagClass(order)" v-if="order.payoutStatus">资金状态：{{ order.payoutStatus }}</span>
          </div>
        </header>

        <section class="order-card__body">
          <dl class="metrics">
            <div>
              <dt>订单金额</dt>
              <dd>{{ formatCurrency(order.totalAmount) }}</dd>
            </div>
            <div>
              <dt>供应商结算</dt>
              <dd>{{ formatCurrency(order.supplierPayoutAmount) }}</dd>
            </div>
            <div>
              <dt>平台佣金 (5%)</dt>
              <dd>{{ formatCurrency(order.commissionAmount) }}</dd>
            </div>
            <div v-if="order.adminHoldingAmount && order.adminHoldingAmount > 0">
              <dt>托管余额</dt>
              <dd>{{ formatCurrency(order.adminHoldingAmount) }}</dd>
            </div>
          </dl>

          <section class="details">
            <div class="detail-column">
              <h3>物流节点</h3>
              <ul class="timeline">
                <li><span>支付时间</span><strong>{{ formatDateTime(order.paymentTime) }}</strong></li>
                <li><span>发货时间</span><strong>{{ formatDateTime(order.shippingTime) }}</strong></li>
                <li><span>运送时间</span><strong>{{ formatDateTime(order.inTransitTime) }}</strong></li>
                <li><span>送达时间</span><strong>{{ formatDateTime(order.deliveryTime) }}</strong></li>
                <li><span>消费者确认</span><strong>{{ formatDateTime(order.consumerConfirmationTime) }}</strong></li>
                <li><span>管理员结算</span><strong>{{ formatDateTime(order.adminApprovalTime) }}</strong></li>
              </ul>
            </div>
            <div class="detail-column">
              <h3>收货信息</h3>
              <p class="muted">
                {{ order.recipientName ?? '—' }} {{ order.recipientPhone ?? '' }}
              </p>
              <p class="muted">{{ order.shippingAddress ?? '—' }}</p>
              <p class="muted" v-if="order.consumerName">消费者：{{ order.consumerName }}</p>
              <p class="muted" v-if="order.managingAdminName">托管管理员：{{ order.managingAdminName }}</p>
            </div>
          </section>

          <section class="items">
            <h3>订单商品</h3>
            <ul>
              <li v-for="item in order.items" :key="item.id" class="item-row">
                <div class="item-info">
                  <strong>{{ item.productName ?? '未知商品' }}</strong>
                  <p class="muted">供应商：{{ item.supplierName ?? '—' }}</p>
                </div>
                <div class="item-metrics">
                  <span>数量：{{ item.quantity ?? 0 }}</span>
                  <span>单价：{{ formatCurrency(item.unitPrice) }}</span>
                  <span>小计：{{ formatCurrency(item.totalPrice) }}</span>
                </div>
              </li>
            </ul>
          </section>
        </section>

        <footer class="order-card__footer">
          <div class="footer-notes">
            <p v-if="order.approvalDisabledReason" class="muted">
              {{ order.approvalDisabledReason }}
            </p>
          </div>
          <div class="footer-actions">
            <button
              type="button"
              :disabled="!canApprove(order)"
              @click="approveOrder(order)"
            >
              {{ approvingId === order.id ? '结算中…' : '确认结算' }}
            </button>
          </div>
        </footer>
      </li>
    </ul>

    <p v-else-if="!loading" class="empty">当前筛选条件下暂无订单。</p>

    <nav v-if="!loading && totalPages > 1" class="pagination">
      <button type="button" @click="prevPage" :disabled="pagination.page === 0">上一页</button>
      <span>第 {{ pagination.page + 1 }} / {{ totalPages }} 页</span>
      <button type="button" @click="nextPage" :disabled="pagination.page + 1 >= totalPages">下一页</button>
    </nav>
  </section>
</template>

<style scoped>
.admin-orders {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.5rem;
}

.page-header h1 {
  font-size: 1.75rem;
  margin-bottom: 0.25rem;
}

.subtitle {
  color: rgba(0, 0, 0, 0.6);
  max-width: 52rem;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 1rem;
}

.filters label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.7);
}

.filters select {
  min-width: 12rem;
  padding: 0.4rem 0.6rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.search-field__control {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.search-field__control input {
  min-width: 14rem;
  padding: 0.4rem 0.6rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.filters button {
  padding: 0.45rem 0.95rem;
  border-radius: 999px;
  border: 1px solid transparent;
  background: #1d4ed8;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.filters button.ghost {
  background: transparent;
  color: rgba(0, 0, 0, 0.65);
  border-color: rgba(0, 0, 0, 0.12);
}

.filters button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feedback {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  font-weight: 600;
}

.feedback--success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.feedback--error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.feedback--info {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.order-list {
  display: grid;
  gap: 1.25rem;
  list-style: none;
  padding: 0;
}

.order-card {
  border-radius: 1.25rem;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.order-card__head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.order-card__head h2 {
  font-size: 1.3rem;
  margin-bottom: 0.35rem;
}

.order-card__head .meta {
  color: rgba(0, 0, 0, 0.55);
}

.status-block {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0.3rem 0.75rem;
  font-weight: 600;
  font-size: 0.85rem;
  background: rgba(15, 23, 42, 0.06);
  color: rgba(15, 23, 42, 0.7);
}

.tag--success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.tag--warning {
  background: rgba(234, 179, 8, 0.15);
  color: #b45309;
}

.tag--info {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.tag--danger {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.order-card__body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 1rem;
}

.metrics dt {
  font-weight: 700;
  color: rgba(0, 0, 0, 0.65);
}

.metrics dd {
  margin: 0.3rem 0 0;
  font-size: 1.1rem;
  font-weight: 700;
}

.details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
  gap: 1.25rem;
}

.detail-column {
  border: 1px dashed rgba(0, 0, 0, 0.08);
  border-radius: 1rem;
  padding: 1rem;
  background: rgba(249, 250, 251, 0.8);
  display: grid;
  gap: 0.75rem;
}

.timeline {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.timeline span {
  color: rgba(0, 0, 0, 0.6);
}

.timeline strong {
  display: block;
  color: rgba(0, 0, 0, 0.85);
}

.items ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.85rem;
}

.item-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.item-row:last-child {
  border-bottom: none;
}

.item-info strong {
  font-weight: 700;
}

.item-info .muted {
  margin-top: 0.25rem;
}

.item-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.65);
}

.order-card__footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.footer-actions button {
  padding: 0.55rem 1.5rem;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.footer-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.muted {
  color: rgba(0, 0, 0, 0.55);
}

.empty {
  text-align: center;
  color: rgba(0, 0, 0, 0.55);
  padding: 2rem 0;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
}

.pagination button {
  padding: 0.45rem 1.1rem;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #fff;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .metrics {
    grid-template-columns: repeat(auto-fit, minmax(8rem, 1fr));
  }

  .filters select {
    min-width: auto;
  }

  .search-field__control input {
    min-width: 10rem;
  }
}
</style>
