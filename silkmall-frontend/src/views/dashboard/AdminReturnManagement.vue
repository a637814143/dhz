<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import type { ReturnRequest } from '@/types'

const returnRequests = ref<ReturnRequest[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const actionMessage = ref<string | null>(null)
const actionError = ref<string | null>(null)
const processingId = ref<number | null>(null)
const adminResolutionDrafts = reactive<Record<number, string>>({})

const PAGE_SIZE = 5
const page = ref(0)

const orderedReturnRequests = computed(() => {
  const pending = returnRequests.value.filter(
    (item) => (item.status ?? '').toUpperCase() === 'AWAITING_ADMIN'
  )
  const processed = returnRequests.value
    .filter((item) => (item.status ?? '').toUpperCase() !== 'AWAITING_ADMIN')
    .slice()
    .sort((a, b) => {
      const timeA = new Date(a.adminProcessedAt ?? a.processedAt ?? a.requestedAt ?? '').getTime()
      const timeB = new Date(b.adminProcessedAt ?? b.processedAt ?? b.requestedAt ?? '').getTime()
      return timeB - timeA
    })

  return [...pending, ...processed]
})

const totalPages = computed(() =>
  orderedReturnRequests.value.length > 0
    ? Math.ceil(orderedReturnRequests.value.length / PAGE_SIZE)
    : 0
)

function clampPage(value: number) {
  if (totalPages.value === 0) return 0
  return Math.min(Math.max(value, 0), Math.max(totalPages.value - 1, 0))
}

const paginatedReturns = computed(() => {
  if (!orderedReturnRequests.value.length) return []
  const currentPage = clampPage(page.value)
  const start = currentPage * PAGE_SIZE
  return orderedReturnRequests.value.slice(start, start + PAGE_SIZE)
})

watch(orderedReturnRequests, () => {
  page.value = clampPage(page.value)
})

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

function clearFeedback() {
  actionMessage.value = null
  actionError.value = null
}

async function loadReturnRequests(status: 'all' | 'pending' = 'all') {
  loading.value = true
  error.value = null
  try {
    const params: Record<string, string> = {}
    if (status) {
      params.status = status
    }
    const { data } = await api.get<ReturnRequest[]>('/returns/admin', { params })
    const list = Array.isArray(data) ? data : []
    returnRequests.value = list
    list.forEach((item) => {
      if (!item?.id) return
      const existing = adminResolutionDrafts[item.id]
      const value = typeof existing === 'string' ? existing : item.adminResolution ?? ''
      adminResolutionDrafts[item.id] = value
    })
  } catch (err) {
    returnRequests.value = []
    error.value = err instanceof Error ? err.message : '加载退货审批失败'
  } finally {
    loading.value = false
  }
}

async function confirmReturnApproval(request: ReturnRequest) {
  if (!request?.id) return

  processingId.value = request.id
  actionError.value = null
  actionMessage.value = null
  try {
    const note = adminResolutionDraft(request.id).trim()
    await api.put(`/returns/${request.id}/status`, {
      status: 'COMPLETED',
      adminResolution: note,
    })
    const orderHint = request.orderId ? `订单 ${request.orderId}` : '该订单'
    actionMessage.value = `${orderHint}的退款已完成，并返还至消费者钱包`
    await loadReturnRequests()
  } catch (err) {
    actionError.value = err instanceof Error ? err.message : '确认退款失败'
  } finally {
    processingId.value = null
  }
}

function goToPage(next: number) {
  page.value = clampPage(next)
}

function nextPage() {
  goToPage(page.value + 1)
}

function prevPage() {
  goToPage(page.value - 1)
}

onMounted(() => {
  loadReturnRequests()
})
</script>

<template>
  <section class="admin-returns">
    <header class="page-header">
      <div>
        <h1>退货管理</h1>
        <p class="subtitle">集中处理供应商确认后的退货退款审批，及时完成消费者退款。</p>
      </div>
      <button type="button" class="panel-action-button" :disabled="loading" @click="() => loadReturnRequests()">
        {{ loading ? '刷新中…' : '刷新列表' }}
      </button>
    </header>

    <transition name="fade">
      <p v-if="actionMessage" class="return-feedback return-feedback--success">
        {{ actionMessage }}
      </p>
    </transition>
    <transition name="fade">
      <p v-if="actionError" class="return-feedback return-feedback--error">
        {{ actionError }}
      </p>
    </transition>

    <p v-if="error" class="feedback feedback--error">{{ error }}</p>
    <p v-else-if="loading" class="feedback feedback--info">退货记录加载中…</p>

    <template v-else>
      <p v-if="!paginatedReturns.length" class="empty">暂无售后退货记录。</p>
      <ul v-else class="return-list">
        <li v-for="request in paginatedReturns" :key="request.id" class="return-card">
          <header class="return-card__head">
            <div>
              <h3>{{ request.productName ?? '商品' }}</h3>
              <p class="return-meta">申请时间：{{ formatDateTime(request.requestedAt) }}</p>
              <p v-if="request.consumerName" class="return-meta">消费者：{{ request.consumerName }}</p>
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
            <p v-if="request.adminProcessedAt" class="return-meta">
              完成时间：{{ formatDateTime(request.adminProcessedAt) }}
            </p>
            <label v-if="(request.status ?? '').toUpperCase() === 'AWAITING_ADMIN'">
              <span>管理员备注</span>
              <textarea
                :value="adminResolutionDraft(request.id)"
                rows="3"
                placeholder="可填写退款原因或沟通记录"
                :disabled="processingId === request.id"
                @input="
                  updateAdminResolutionDraft(request.id, ($event.target as HTMLTextAreaElement).value)
                "
              ></textarea>
            </label>
            <p v-else-if="request.adminResolution">
              <strong>管理员说明：</strong>
              <span>{{ request.adminResolution }}</span>
            </p>
          </section>
          <footer class="return-card__footer">
            <button
              type="button"
              class="primary"
              :disabled="processingId === request.id || (request.status ?? '').toUpperCase() !== 'AWAITING_ADMIN'"
              @click="() => {
                clearFeedback()
                confirmReturnApproval(request)
              }"
            >
              {{ processingId === request.id ? '提交中…' : '确认退款' }}
            </button>
          </footer>
        </li>
      </ul>

      <nav v-if="totalPages > 1" class="pagination">
        <button type="button" @click="prevPage" :disabled="page === 0">上一页</button>
        <span>第 {{ page + 1 }} / {{ totalPages }} 页</span>
        <button type="button" @click="nextPage" :disabled="page + 1 >= totalPages">下一页</button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.admin-returns {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.page-header h1 {
  font-size: 1.75rem;
  margin-bottom: 0.25rem;
}

.subtitle {
  color: rgba(0, 0, 0, 0.6);
}

.panel-action-button {
  border: none;
  border-radius: 0.75rem;
  padding: 0.55rem 1.4rem;
  font-weight: 700;
  cursor: pointer;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
}

.panel-action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feedback {
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  font-weight: 600;
}

.feedback--error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.feedback--info {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.return-feedback {
  padding: 0.8rem 1rem;
  border-radius: 0.9rem;
  font-weight: 700;
}

.return-feedback--success {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.return-feedback--error {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.return-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 1rem;
}

.return-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 1rem;
  background: rgba(241, 245, 249, 0.6);
  padding: 1.2rem;
  display: grid;
  gap: 0.9rem;
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
  font-weight: 700;
  cursor: pointer;
}

.return-card__footer .primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.return-meta {
  color: rgba(15, 23, 42, 0.65);
  font-size: 0.92rem;
}

.empty {
  text-align: center;
  color: rgba(0, 0, 0, 0.55);
  padding: 1.5rem 0;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 0.5rem;
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
</style>
