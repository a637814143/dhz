<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import type { AdminOrderSummary, PageResponse, ReturnRequest } from '@/types'

const loading = ref(false)
const error = ref<string | null>(null)
const actionMessage = ref<string | null>(null)
const actionError = ref<string | null>(null)
const approvingId = ref<number | null>(null)

const orders = ref<AdminOrderSummary[]>([])
const returnApprovals = ref<ReturnRequest[]>([])
const returnApprovalsLoading = ref(false)
const returnApprovalsError = ref<string | null>(null)
const returnApprovalMessage = ref<string | null>(null)
const returnApprovalError = ref<string | null>(null)
const processingReturnId = ref<number | null>(null)
const adminResolutionDrafts = reactive<Record<number, string>>({})

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

const pendingReturnRequests = computed(() =>
  returnApprovals.value.filter(
    (item) => (item.status ?? '').toUpperCase() === 'AWAITING_ADMIN'
  )
)

const processedReturnRequests = computed(() => {
  return returnApprovals.value
    .filter((item) => (item.status ?? '').toUpperCase() !== 'AWAITING_ADMIN')
    .slice()
    .sort((a, b) => {
      const timeA = new Date(a.adminProcessedAt ?? a.processedAt ?? a.requestedAt ?? '').getTime()
      const timeB = new Date(b.adminProcessedAt ?? b.processedAt ?? b.requestedAt ?? '').getTime()
      return timeB - timeA
    })
})

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

function clearReturnFeedback() {
  returnApprovalMessage.value = null
  returnApprovalError.value = null
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

async function loadReturnApprovals(status: 'all' | 'pending' = 'all') {
  returnApprovalsLoading.value = true
  returnApprovalsError.value = null
  try {
    const params: Record<string, string> = {}
    if (status) {
      params.status = status
    }
    const { data } = await api.get<ReturnRequest[]>('/returns/admin', { params })
    const list = Array.isArray(data) ? data : []
    returnApprovals.value = list
    list.forEach((item) => {
      if (!item?.id) return
      const existing = adminResolutionDrafts[item.id]
      const value = typeof existing === 'string' ? existing : item.adminResolution ?? ''
      adminResolutionDrafts[item.id] = value
    })
  } catch (err) {
    returnApprovals.value = []
    returnApprovalsError.value = err instanceof Error ? err.message : '加载退货审批失败'
  } finally {
    returnApprovalsLoading.value = false
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
  if (order.cancelled) return 'tag tag--danger'
  if (!order.payoutStatus) return 'tag'
  if (order.payoutStatus === '待批准') return 'tag tag--info'
  if (order.payoutStatus === '已批准') return 'tag tag--success'
  if (order.payoutStatus === '已退款' || order.payoutStatus === '账单已取消') return 'tag tag--danger'
  return 'tag'
}

function adminResolutionDraft(id: number) {
  const value = adminResolutionDrafts[id]
  return typeof value === 'string' ? value : ''
}

function updateAdminResolutionDraft(id: number, value: string) {
  adminResolutionDrafts[id] = value
}

function returnStageLabel(status?: string | null) {
  if (!status) return '未知状态'
  const normalized = status.trim().toUpperCase()
  switch (normalized) {
    case 'AWAITING_ADMIN':
      return '待审批'
    case 'COMPLETED':
      return '已退款'
    case 'REJECTED':
      return '已拒绝'
    case 'APPROVED':
      return '供应商已确认'
    case 'PENDING':
      return '待供应商处理'
    default:
      return status
  }
}

async function confirmReturnApproval(request: ReturnRequest) {
  if (!request?.id) return

  processingReturnId.value = request.id
  returnApprovalError.value = null
  returnApprovalMessage.value = null
  try {
    const note = adminResolutionDraft(request.id).trim()
    await api.put(`/returns/${request.id}/status`, {
      status: 'COMPLETED',
      adminResolution: note,
    })
    const orderHint = request.orderId ? `订单 ${request.orderId}` : '该订单'
    returnApprovalMessage.value = `${orderHint}的退款已完成，并返还至消费者钱包`
    await Promise.all([loadReturnApprovals(), loadOrders()])
  } catch (err) {
    returnApprovalError.value = err instanceof Error ? err.message : '确认退款失败'
  } finally {
    processingReturnId.value = null
  }
}

onMounted(() => {
  loadOrders()
  loadReturnApprovals()
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

    <section class="return-approvals" aria-labelledby="return-approvals-title">
      <div class="panel-title-row">
        <div class="panel-title" id="return-approvals-title">退货审批</div>
        <button
          type="button"
          class="panel-action-button"
          @click="() => {
            clearReturnFeedback()
            loadReturnApprovals()
          }"
          :disabled="returnApprovalsLoading"
        >
          {{ returnApprovalsLoading ? '刷新中…' : '刷新列表' }}
        </button>
      </div>
      <transition name="fade">
        <p v-if="returnApprovalMessage" class="return-feedback return-feedback--success">
          {{ returnApprovalMessage }}
        </p>
      </transition>
      <transition name="fade">
        <p v-if="returnApprovalError" class="return-feedback return-feedback--error">
          {{ returnApprovalError }}
        </p>
      </transition>

      <p v-if="returnApprovalsLoading" class="feedback feedback--info">退货记录加载中…</p>
      <p v-else-if="returnApprovalsError" class="return-feedback return-feedback--error">
        {{ returnApprovalsError }}
      </p>
      <template v-else>
        <p
          v-if="!pendingReturnRequests.length && !processedReturnRequests.length"
          class="empty"
        >
          暂无售后退货记录。
        </p>
        <template v-else>
          <section v-if="pendingReturnRequests.length" class="return-section">
            <h2 class="section-subtitle">待审批</h2>
            <ul class="return-list">
              <li v-for="request in pendingReturnRequests" :key="request.id" class="return-card">
                <header class="return-card__head">
                  <div>
                    <h3>{{ request.productName ?? '商品' }}</h3>
                    <p class="return-meta">申请时间：{{ formatDateTime(request.requestedAt) }}</p>
                    <p v-if="request.consumerName" class="return-meta">
                      消费者：{{ request.consumerName }}
                    </p>
                  </div>
                  <span class="status-pill">{{ returnStageLabel(request.status) }}</span>
                </header>
                <section class="return-card__body">
                  <p>
                    <strong>退货原因：</strong>
                    <span>{{ request.reason?.trim() ? request.reason : '未提供' }}</span>
                  </p>
                  <p v-if="request.refundAmount">
                    <strong>退款总额：</strong>
                    <span>
                      {{ formatCurrency(request.refundAmount) }} （平台支付
                      {{ formatCurrency(request.commissionAmount) }}，供应商支付
                      {{ formatCurrency(request.supplierShareAmount) }}）
                    </span>
                  </p>
                  <p v-if="request.resolution">
                    <strong>供应商说明：</strong>
                    <span>{{ request.resolution }}</span>
                  </p>
                  <label>
                    <span>管理员备注</span>
                    <textarea
                      :value="adminResolutionDraft(request.id)"
                      rows="3"
                      placeholder="可填写退款原因或沟通记录"
                      :disabled="processingReturnId === request.id"
                      @input="updateAdminResolutionDraft(
                        request.id,
                        ($event.target as HTMLTextAreaElement).value
                      )"
                    ></textarea>
                  </label>
                </section>
                <footer class="return-card__footer">
                  <button
                    type="button"
                    class="primary"
                    :disabled="processingReturnId === request.id"
                    @click="confirmReturnApproval(request)"
                  >
                    {{ processingReturnId === request.id ? '提交中…' : '确认退款' }}
                  </button>
                </footer>
              </li>
            </ul>
          </section>

          <section v-if="processedReturnRequests.length" class="return-section history">
            <h2 class="section-subtitle">历史记录</h2>
            <ul class="return-list return-list--compact">
              <li v-for="request in processedReturnRequests" :key="request.id" class="return-card">
                <header class="return-card__head">
                  <div>
                    <h3>{{ request.productName ?? '商品' }}</h3>
                    <p class="return-meta">申请时间：{{ formatDateTime(request.requestedAt) }}</p>
                    <p class="return-meta" v-if="request.adminProcessedAt">
                      完成时间：{{ formatDateTime(request.adminProcessedAt) }}
                    </p>
                  </div>
                  <span class="status-pill">{{ returnStageLabel(request.status) }}</span>
                </header>
                <section class="return-card__body">
                  <p>
                    <strong>退款总额：</strong>
                    <span>
                      {{ formatCurrency(request.refundAmount) }} （平台支付
                      {{ formatCurrency(request.commissionAmount) }}，供应商支付
                      {{ formatCurrency(request.supplierShareAmount) }}）
                    </span>
                  </p>
                  <p v-if="request.resolution">
                    <strong>供应商说明：</strong>
                    <span>{{ request.resolution }}</span>
                  </p>
                  <p v-if="request.adminResolution">
                    <strong>管理员说明：</strong>
                    <span>{{ request.adminResolution }}</span>
                  </p>
                </section>
              </li>
            </ul>
          </section>
        </template>
      </template>
    </section>

    <p v-if="error" class="feedback feedback--error">{{ error }}</p>
    <p v-else-if="loading" class="feedback feedback--info">订单加载中，请稍候…</p>

    <ul v-if="!loading && orders.length" class="order-list">
      <li
        v-for="order in orders"
        :key="order.id"
        :class="['order-card', { 'order-card--cancelled': order.cancelled }]"
      >
        <header class="order-card__head">
          <div>
            <h2>订单编号：{{ order.orderNo }}</h2>
            <p class="meta">
              下单时间：{{ formatDateTime(order.orderTime) }} · 商品数量：{{ order.totalQuantity ?? 0 }} 件
            </p>
          </div>
          <div class="status-block">
            <span :class="receiptTagClass(order)">收货状态：{{ order.receiptStatus }}</span>
            <span v-if="order.cancellationLabel" class="cancellation-tag">{{ order.cancellationLabel }}</span>
            <span
              :class="payoutTagClass(order)"
              v-if="order.payoutStatus && !order.cancellationLabel"
            >
              资金状态：{{ order.payoutStatus }}
            </span>
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
            <p v-if="order.cancellationLabel" class="muted cancellation-note">
              该订单已取消并完成退款处理
            </p>
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

.return-approvals {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 1rem;
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
  background: rgba(248, 250, 252, 0.8);
}

.return-approvals .panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.return-approvals .panel-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.9);
}

.return-approvals .panel-action-button {
  border: none;
  border-radius: 0.75rem;
  padding: 0.5rem 1.2rem;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #38bdf8);
  color: #fff;
}

.return-feedback {
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  font-weight: 600;
}

.return-feedback--success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.return-feedback--error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.return-section {
  display: grid;
  gap: 1rem;
}

.section-subtitle {
  font-size: 1.05rem;
  font-weight: 700;
  color: rgba(15, 23, 42, 0.85);
}

.return-list {
  list-style: none;
  display: grid;
  gap: 1rem;
  padding: 0;
  margin: 0;
}

.return-list--compact .return-card {
  padding: 1rem;
}

.return-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 1rem;
  background: rgba(241, 245, 249, 0.6);
  padding: 1.25rem;
  display: grid;
  gap: 1rem;
}

.return-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.return-card__head h3 {
  margin: 0;
  font-size: 1.1rem;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  padding: 0.35rem 0.75rem;
  font-weight: 600;
  font-size: 0.85rem;
}

.return-card__body {
  display: grid;
  gap: 0.75rem;
  color: rgba(15, 23, 42, 0.78);
}

.return-card__body strong {
  font-weight: 700;
  margin-right: 0.3rem;
}

.return-card__body textarea {
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.15);
  padding: 0.6rem 0.75rem;
  font-family: inherit;
  resize: vertical;
  min-height: 5rem;
}

.return-card__body textarea:disabled {
  background: rgba(226, 232, 240, 0.7);
  cursor: not-allowed;
}

.return-card__footer {
  display: flex;
  justify-content: flex-end;
}

.return-card__footer .primary {
  border: none;
  border-radius: 0.75rem;
  padding: 0.55rem 1.4rem;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.return-card__footer .primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  position: relative;
  overflow: hidden;
}

.order-card > * {
  position: relative;
  z-index: 1;
}

.order-card--cancelled::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.08);
  pointer-events: none;
}

.order-card--cancelled {
  border-color: rgba(15, 23, 42, 0.12);
  box-shadow: none;
}

.cancellation-tag {
  background: rgba(239, 68, 68, 0.18);
  color: #991b1b;
  border: 1px solid rgba(239, 68, 68, 0.35);
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-weight: 700;
  font-size: 0.8rem;
  letter-spacing: 0.02em;
}

.cancellation-note {
  font-weight: 600;
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
