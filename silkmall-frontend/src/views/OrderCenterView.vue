<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import api from '@/services/api'
import type { OrderDetail, OrderItemDetail, ProductReview, ReturnRequest } from '@/types'
import { useAuthState } from '@/services/authState'

const orderIdInput = ref('')
const loadingOrder = ref(false)
const orderDetail = ref<OrderDetail | null>(null)
const orderError = ref<string | null>(null)
const actionMessage = ref<string | null>(null)
const actionError = ref<string | null>(null)

const reviews = ref<ProductReview[]>([])
const returnRequests = ref<ReturnRequest[]>([])

const activeReviewItemId = ref<number | null>(null)
const activeReturnItemId = ref<number | null>(null)
const editingReviewId = ref<number | null>(null)

const { state } = useAuthState()
const currentUserRole = computed(() => state.user?.userType ?? null)

const reviewForm = reactive({
  rating: 5,
  comment: '',
})

const returnForm = reactive({
  reason: '',
})

const submittingReview = ref(false)
const submittingReturn = ref(false)

const reviewMap = computed(() => {
  const map = new Map<number, ProductReview[]>()
  reviews.value.forEach((review) => {
    if (!map.has(review.orderItemId)) {
      map.set(review.orderItemId, [])
    }
    map.get(review.orderItemId)!.push(review)
  })
  map.forEach((list) =>
    list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  )
  return map
})

const latestReturnMap = computed(() => {
  const map = new Map<number, ReturnRequest>()
  returnRequests.value.forEach((request) => {
    const existing = map.get(request.orderItemId)
    if (!existing || new Date(existing.requestedAt).getTime() < new Date(request.requestedAt).getTime()) {
      map.set(request.orderItemId, request)
    }
  })
  return map
})

const hasHoldingAmount = computed(() => {
  const amount = orderDetail.value?.adminHoldingAmount
  return typeof amount === 'number' && !Number.isNaN(amount)
})

const pendingReturnMap = computed(() => {
  const map = new Map<number, ReturnRequest>()
  returnRequests.value.forEach((request) => {
    if (request.status === 'PENDING' || request.status === 'APPROVED') {
      map.set(request.orderItemId, request)
    }
  })
  return map
})

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
})

function formatCurrency(amount?: number | null) {
  if (typeof amount !== 'number' || Number.isNaN(amount)) return '¥0.00'
  return currencyFormatter.format(amount)
}

function formatDateTime(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function resetMessages() {
  actionMessage.value = null
  actionError.value = null
}

async function fetchOrder() {
  resetMessages()
  orderError.value = null
  const trimmed = orderIdInput.value.trim()
  if (!trimmed) {
    orderError.value = '请输入订单ID'
    return
  }

  const orderId = Number(trimmed)
  if (!Number.isInteger(orderId) || orderId <= 0) {
    orderError.value = '订单ID必须为正整数'
    return
  }

  loadingOrder.value = true
  try {
    const { data } = await api.get<OrderDetail>(`/orders/${orderId}`)
    orderDetail.value = data
    await Promise.all([fetchReviews(data.id), fetchReturnRequests(data.id)])
    actionMessage.value = '订单信息加载成功'
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载订单失败'
    orderError.value = message
    orderDetail.value = null
    reviews.value = []
    returnRequests.value = []
  } finally {
    loadingOrder.value = false
  }
}

async function fetchReviews(orderId: number) {
  try {
    const { data } = await api.get<ProductReview[]>(`/reviews/orders/${orderId}`)
    reviews.value = data
  } catch (err) {
    console.warn('加载评价信息失败', err)
    reviews.value = []
  }
}

async function fetchReturnRequests(orderId: number) {
  try {
    const { data } = await api.get<ReturnRequest[]>(`/returns/orders/${orderId}`)
    returnRequests.value = data
  } catch (err) {
    console.warn('加载退货信息失败', err)
    returnRequests.value = []
  }
}

function openReviewForm(item: OrderItemDetail) {
  resetMessages()
  const existing = reviews.value.find(
    (review) => review.orderItemId === item.id && canEditReview(review)
  )
  if (existing) {
    startEditExistingReview(existing)
    return
  }
  if (!canCreateReviewForItem(item)) {
    actionError.value = '您已经评价过该商品'
    return
  }
  activeReviewItemId.value = item.id
  editingReviewId.value = null
  reviewForm.rating = 5
  reviewForm.comment = ''
}

function cancelReviewForm() {
  activeReviewItemId.value = null
  editingReviewId.value = null
}

async function submitReview() {
  if (!activeReviewItemId.value) return
  resetMessages()

  const rating = Number(reviewForm.rating)
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
    actionError.value = '请给出 1 - 5 分的评分'
    return
  }

  submittingReview.value = true
  try {
    const payload = {
      rating,
      comment: reviewForm.comment.trim() || null,
    }
    if (editingReviewId.value) {
      const { data } = await api.put<ProductReview>(`/reviews/${editingReviewId.value}`, payload)
      upsertReview(data)
      actionMessage.value = '评价更新成功'
    } else {
      const { data } = await api.post<ProductReview>(`/reviews/order-items/${activeReviewItemId.value}`, payload)
      upsertReview(data)
      actionMessage.value = '评价提交成功'
    }
    cancelReviewForm()
  } catch (err) {
    const message = err instanceof Error ? err.message : '提交评价失败'
    actionError.value = message
  } finally {
    submittingReview.value = false
  }
}

function openReturnForm(item: OrderItemDetail) {
  resetMessages()
  activeReturnItemId.value = item.id
  returnForm.reason = ''
}

function cancelReturnForm() {
  activeReturnItemId.value = null
}

async function submitReturnRequest() {
  if (!activeReturnItemId.value) return
  resetMessages()

  const reason = returnForm.reason.trim()
  if (!reason) {
    actionError.value = '请填写退货原因'
    return
  }

  submittingReturn.value = true
  try {
    const { data } = await api.post<ReturnRequest>(
      `/returns/order-items/${activeReturnItemId.value}`,
      { reason }
    )
    returnRequests.value.push(data)
    actionMessage.value = '退货申请已提交'
    cancelReturnForm()
  } catch (err) {
    const message = err instanceof Error ? err.message : '提交退货申请失败'
    actionError.value = message
  } finally {
    submittingReturn.value = false
  }
}

const hasOrder = computed(() => !!orderDetail.value)

function reviewRoleLabel(role?: string | null) {
  if (!role) return '评价人'
  switch (role.toUpperCase()) {
    case 'CONSUMER':
      return '消费者'
    case 'SUPPLIER':
      return '商家'
    case 'ADMIN':
      return '管理员'
    default:
      return role
  }
}

function canEditReview(review: ProductReview) {
  if (!state.user) return false
  const role = currentUserRole.value
  if (role === 'admin') {
    return review.authorRole?.toUpperCase() === 'ADMIN' && review.authorId === state.user.id
  }
  if (role === 'consumer') {
    return review.authorRole?.toUpperCase() === 'CONSUMER' && review.authorId === state.user.id
  }
  if (role === 'supplier') {
    return review.authorRole?.toUpperCase() === 'SUPPLIER' && review.authorId === state.user.id
  }
  return false
}

function canDeleteReview(review: ProductReview) {
  if (!state.user) return false
  if (currentUserRole.value === 'admin') {
    return true
  }
  return canEditReview(review)
}

function canCreateReviewForItem(item: OrderItemDetail) {
  if (currentUserRole.value !== 'consumer' || !state.user) return false
  return !reviews.value.some(
    (review) =>
      review.orderItemId === item.id &&
      review.authorRole?.toUpperCase() === 'CONSUMER' &&
      review.authorId === state.user!.id
  )
}

function startEditExistingReview(review: ProductReview) {
  activeReviewItemId.value = review.orderItemId
  editingReviewId.value = review.id
  reviewForm.rating = review.rating
  reviewForm.comment = review.comment ?? ''
}

async function deleteReview(review: ProductReview) {
  resetMessages()
  try {
    await api.delete(`/reviews/${review.id}`)
    reviews.value = reviews.value.filter((item) => item.id !== review.id)
    actionMessage.value = '评价已删除'
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除评价失败'
    actionError.value = message
  }
}

function upsertReview(review: ProductReview) {
  const index = reviews.value.findIndex((item) => item.id === review.id)
  if (index >= 0) {
    reviews.value.splice(index, 1, review)
  } else {
    reviews.value.push(review)
  }
}
</script>

<template>
  <section class="order-center">
    <header class="order-header">
      <h1>订单服务中心</h1>
      <p class="subtitle">支持查看订单详情、提交评价与退货申请</p>
    </header>

    <form class="order-search" @submit.prevent="fetchOrder">
      <label class="search-label" for="orderId">订单ID</label>
      <div class="search-controls">
        <input
          id="orderId"
          v-model="orderIdInput"
          type="text"
          inputmode="numeric"
          placeholder="请输入订单ID"
          :disabled="loadingOrder"
        />
        <button type="submit" :disabled="loadingOrder">
          {{ loadingOrder ? '查询中…' : '查询订单' }}
        </button>
      </div>
      <p v-if="orderError" class="feedback feedback--error">{{ orderError }}</p>
    </form>

    <transition name="fade">
      <p v-if="actionMessage" class="feedback feedback--success">{{ actionMessage }}</p>
    </transition>
    <transition name="fade">
      <p v-if="actionError" class="feedback feedback--error">{{ actionError }}</p>
    </transition>

    <section v-if="hasOrder" class="order-detail">
      <div class="order-meta">
        <div>
          <h2>订单编号：{{ orderDetail!.orderNo }}</h2>
          <p>下单时间：{{ formatDateTime(orderDetail!.orderTime) }}</p>
        </div>
        <div class="order-status">
          <span class="status-tag">状态：{{ orderDetail!.status }}</span>
          <span>总金额：{{ formatCurrency(orderDetail!.totalAmount) }}</span>
          <span>商品数量：{{ orderDetail!.totalQuantity }}</span>
        </div>
        <div
          v-if="orderDetail!.payoutStatus || hasHoldingAmount || orderDetail!.managingAdminName"
          class="order-status order-status--secondary"
        >
          <span v-if="orderDetail!.payoutStatus" class="status-tag status-tag--secondary">
            资金状态：{{ orderDetail!.payoutStatus }}
          </span>
          <span v-if="hasHoldingAmount">托管金额：{{ formatCurrency(orderDetail!.adminHoldingAmount) }}</span>
          <span v-if="orderDetail!.managingAdminName">管理员：{{ orderDetail!.managingAdminName }}</span>
        </div>
      </div>

      <div class="address-card" v-if="orderDetail!.shippingAddress">
        <h3>收货信息</h3>
        <p>{{ orderDetail!.recipientName }} {{ orderDetail!.recipientPhone }}</p>
        <p>{{ orderDetail!.shippingAddress }}</p>
      </div>

      <div class="timeline-card">
        <h3>物流与确认节点</h3>
        <ul>
          <li><span>支付时间</span><strong>{{ formatDateTime(orderDetail!.paymentTime) }}</strong></li>
          <li><span>发货时间</span><strong>{{ formatDateTime(orderDetail!.shippingTime) }}</strong></li>
          <li><span>运送时间</span><strong>{{ formatDateTime(orderDetail!.inTransitTime) }}</strong></li>
          <li><span>送达时间</span><strong>{{ formatDateTime(orderDetail!.deliveryTime) }}</strong></li>
          <li>
            <span>确认收货</span>
            <strong>{{ formatDateTime(orderDetail!.consumerConfirmationTime) }}</strong>
          </li>
          <li><span>管理员批准</span><strong>{{ formatDateTime(orderDetail!.adminApprovalTime) }}</strong></li>
        </ul>
      </div>

      <section class="items-section">
        <h3>订单商品</h3>
        <ul class="item-list">
          <li v-for="item in orderDetail!.orderItems" :key="item.id" class="item-card">
            <header class="item-head">
              <div>
                <h4>{{ item.product.name }}</h4>
                <p class="muted">下单数量：{{ item.quantity }} 件</p>
              </div>
              <div class="price-info">
                <span>单价：{{ formatCurrency(item.unitPrice) }}</span>
                <span>小计：{{ formatCurrency(item.totalPrice) }}</span>
              </div>
            </header>

            <section class="item-body">
              <div class="item-actions">
                <div class="action-block">
                  <strong>商品评价</strong>
                  <div v-if="reviewMap.get(item.id)?.length" class="review-stack">
                    <article v-for="review in reviewMap.get(item.id)!" :key="review.id" class="review-entry">
                      <header>
                        <span class="review-author">{{ review.authorName ?? review.consumerName ?? '用户' }}</span>
                        <span class="review-role">{{ reviewRoleLabel(review.authorRole) }}</span>
                        <span class="muted">评分：{{ review.rating }} 分</span>
                        <span class="muted">{{ formatDateTime(review.createdAt) }}</span>
                      </header>
                      <p v-if="review.comment" class="muted">{{ review.comment }}</p>
                      <footer class="review-actions">
                        <button
                          v-if="canEditReview(review)"
                          type="button"
                          class="link-button"
                          @click="startEditExistingReview(review)"
                        >
                          编辑
                        </button>
                        <button
                          v-if="canDeleteReview(review)"
                          type="button"
                          class="link-button danger"
                          @click="deleteReview(review)"
                        >
                          删除
                        </button>
                      </footer>
                    </article>
                  </div>
                  <template v-if="canCreateReviewForItem(item)">
                    <button
                      class="link-button"
                      type="button"
                      @click="openReviewForm(item)"
                      :disabled="activeReviewItemId === item.id"
                    >
                      我要评价
                    </button>
                  </template>
                  <p v-else-if="!(reviewMap.get(item.id)?.length)" class="muted">暂无评价记录</p>
                </div>

                <div class="action-block">
                  <strong>退货申请</strong>
                  <template v-if="latestReturnMap.get(item.id)">
                    <p class="muted">
                      最近状态：{{ latestReturnMap.get(item.id)!.status }}
                      <span v-if="latestReturnMap.get(item.id)!.resolution">— {{ latestReturnMap.get(item.id)!.resolution }}</span>
                    </p>
                    <p class="muted">申请时间：{{ formatDateTime(latestReturnMap.get(item.id)!.requestedAt) }}</p>
                  </template>
                  <template v-else>
                    <p class="muted">尚未发起退货申请</p>
                  </template>
                  <button
                    class="link-button"
                    type="button"
                    @click="openReturnForm(item)"
                    :disabled="pendingReturnMap.has(item.id) || activeReturnItemId === item.id"
                  >
                    申请退货
                  </button>
                  <p v-if="pendingReturnMap.has(item.id)" class="muted warning">
                    存在待处理的退货申请
                  </p>
                </div>
              </div>

              <form
                v-if="activeReviewItemId === item.id"
                class="inline-form"
                @submit.prevent="submitReview"
              >
                <label>
                  评分
                  <select v-model.number="reviewForm.rating" :disabled="submittingReview">
                    <option v-for="score in 5" :key="score" :value="score">{{ score }} 分</option>
                  </select>
                </label>
                <label>
                  评价内容（可选）
                  <textarea
                    v-model="reviewForm.comment"
                    rows="3"
                    maxlength="300"
                    placeholder="分享您的购物体验"
                    :disabled="submittingReview"
                  />
                </label>
                <div class="form-actions">
                  <button type="submit" :disabled="submittingReview">
                    {{ submittingReview ? '提交中…' : editingReviewId ? '保存修改' : '提交评价' }}
                  </button>
                  <button type="button" class="ghost" @click="cancelReviewForm" :disabled="submittingReview">
                    取消
                  </button>
                </div>
              </form>

              <form
                v-if="activeReturnItemId === item.id"
                class="inline-form"
                @submit.prevent="submitReturnRequest"
              >
                <label>
                  退货原因
                  <textarea
                    v-model="returnForm.reason"
                    rows="3"
                    maxlength="300"
                    placeholder="请说明退货原因"
                    :disabled="submittingReturn"
                  />
                </label>
                <div class="form-actions">
                  <button type="submit" :disabled="submittingReturn">{{ submittingReturn ? '提交中…' : '提交申请' }}</button>
                  <button type="button" class="ghost" @click="cancelReturnForm" :disabled="submittingReturn">
                    取消
                  </button>
                </div>
              </form>
            </section>
          </li>
        </ul>
      </section>

      <section v-if="returnRequests.length" class="timeline">
        <h3>退货记录</h3>
        <ul>
          <li v-for="request in returnRequests" :key="request.id">
            <strong>{{ request.productName }}</strong>
            <span class="muted">状态：{{ request.status }}</span>
            <span class="muted">申请时间：{{ formatDateTime(request.requestedAt) }}</span>
            <span v-if="request.resolution" class="muted">处理结果：{{ request.resolution }}</span>
          </li>
        </ul>
      </section>
    </section>
  </section>
</template>

<style scoped>
.order-center {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.order-header h1 {
  font-size: 1.75rem;
  margin-bottom: 0.25rem;
}

.subtitle {
  color: rgba(0, 0, 0, 0.6);
}

.order-search {
  display: grid;
  gap: 0.75rem;
  padding: 1.25rem;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 1rem;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
}

.search-label {
  font-weight: 600;
}

.search-controls {
  display: flex;
  gap: 1rem;
}

.search-controls input {
  flex: 1;
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
  font-size: 1rem;
}

.search-controls button {
  padding: 0 1.5rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.search-controls button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.feedback {
  margin: 0;
  padding: 0.5rem 0.75rem;
  border-radius: 0.75rem;
  font-size: 0.95rem;
}

.feedback--error {
  color: #b42318;
  background: rgba(180, 35, 24, 0.08);
}

.feedback--success {
  color: #05603a;
  background: rgba(5, 96, 58, 0.08);
}

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
  border-radius: 1.25rem;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.05);
}

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.order-status {
  display: grid;
  gap: 0.25rem;
  text-align: right;
}

.order-status--secondary {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.95rem;
}

.status-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  background: rgba(242, 142, 28, 0.12);
  color: #7a3b0c;
  font-weight: 600;
}

.status-tag--secondary {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.address-card {
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(242, 142, 28, 0.08);
  border: 1px solid rgba(242, 142, 28, 0.2);
}

.timeline-card {
  padding: 1.25rem;
  border-radius: 1rem;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: #f9fafb;
}

.timeline-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.5rem;
}

.timeline-card li {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 0.95rem;
  color: #374151;
}

.timeline-card li span {
  color: #6b7280;
}

.timeline-card li strong {
  font-weight: 600;
}

.items-section h3,
.timeline h3 {
  margin-bottom: 0.75rem;
}

.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 1rem;
}

.item-card {
  padding: 1.25rem;
  border-radius: 1rem;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: #fafafa;
}

.item-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.price-info {
  display: grid;
  gap: 0.25rem;
  text-align: right;
  white-space: nowrap;
}

.muted {
  color: rgba(0, 0, 0, 0.58);
  font-size: 0.95rem;
}

.warning {
  color: #b54708;
}

.item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
  margin-top: 1rem;
}

.action-block {
  display: grid;
  gap: 0.4rem;
}

.review-stack {
  display: grid;
  gap: 0.75rem;
}

.review-entry {
  padding: 0.75rem;
  border-radius: 0.75rem;
  background: rgba(250, 250, 250, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.review-entry header {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
}

.review-author {
  font-weight: 600;
}

.review-role {
  background: rgba(217, 119, 6, 0.12);
  color: #b45309;
  font-size: 0.75rem;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
}

.review-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.link-button.danger {
  color: #b91c1c;
}

.link-button {
  border: none;
  background: none;
  color: #d97706;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.link-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.inline-form {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.inline-form label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
}

.inline-form select,
.inline-form textarea {
  padding: 0.65rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(0, 0, 0, 0.12);
  font-size: 1rem;
  font-family: inherit;
}

.inline-form textarea {
  resize: vertical;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
}

.form-actions button {
  padding: 0.6rem 1.5rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #f28e1c, #f5c342);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.form-actions button.ghost {
  background: transparent;
  color: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.form-actions button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.timeline ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.timeline li {
  display: grid;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  background: rgba(0, 0, 0, 0.03);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .order-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-status {
    text-align: left;
  }

  .item-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .price-info {
    text-align: left;
  }

  .item-actions {
    flex-direction: column;
  }

  .form-actions {
    flex-direction: column;
  }

  .search-controls {
    flex-direction: column;
  }

  .search-controls button {
    width: 100%;
  }
}
</style>
