<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import type { ProductSummary, PurchaseOrderPayload, PurchaseOrderResult } from '@/types'
import { useAuthState } from '@/services/authState'

const props = defineProps<{
  open: boolean
  product: ProductSummary | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success', payload: PurchaseOrderResult): void
}>()

const { state } = useAuthState()

const form = reactive({
  consumerLookupId: '',
  recipientName: '',
  recipientPhone: '',
  shippingAddress: '',
  paymentMethod: 'WECHAT',
  quantity: 1,
  remark: '',
})

const submitting = ref(false)
const error = ref<string | null>(null)

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetForm()
    }
  }
)

const maxQuantity = computed(() => props.product?.stock ?? 0)

const totalAmount = computed(() => {
  if (!props.product) return 0
  return Number((props.product.price * Math.max(1, form.quantity)).toFixed(2))
})

function resetForm() {
  form.consumerLookupId = generateConsumerLookupId()
  form.recipientName = ''
  form.recipientPhone = ''
  form.shippingAddress = ''
  form.paymentMethod = 'WECHAT'
  form.quantity = 1
  form.remark = ''
  error.value = null
}

function generateConsumerLookupId() {
  const random = Math.random().toString(36).slice(2, 10).toUpperCase()
  const timestamp = Date.now().toString().slice(-4)
  return `C${timestamp}${random}`.slice(0, 12)
}

function close() {
  if (submitting.value) return
  emit('close')
}

function validate() {
  if (!props.product) {
    error.value = '无法识别所选商品'
    return false
  }

  if (!state.user?.id) {
    error.value = '请先登录消费者账号再下单'
    return false
  }

  if (!form.recipientName.trim()) {
    error.value = '请填写收货人姓名'
    return false
  }

  if (!form.recipientPhone.trim()) {
    error.value = '请填写收货人电话'
    return false
  }

  if (!form.shippingAddress.trim()) {
    error.value = '请填写收货地址'
    return false
  }

  if (!Number.isInteger(form.quantity) || form.quantity < 1) {
    error.value = '购买数量至少为 1'
    return false
  }

  if (props.product.stock < form.quantity) {
    error.value = '购买数量超过库存，请调整数量'
    return false
  }

  error.value = null
  return true
}

async function submit() {
  if (!validate() || !props.product) return

  submitting.value = true
  try {
    const payload: PurchaseOrderPayload = {
      consumer: { id: state.user!.id },
      consumerLookupId: form.consumerLookupId,
      recipientName: form.recipientName.trim(),
      recipientPhone: form.recipientPhone.trim(),
      shippingAddress: form.shippingAddress.trim(),
      paymentMethod: form.paymentMethod || null,
      remark: form.remark.trim() || null,
      orderItems: [
        {
          product: { id: props.product.id },
          quantity: form.quantity,
        },
      ],
    }

    const { data } = await api.post<PurchaseOrderResult>('/orders', payload)
    emit('success', data)
    submitting.value = false
    emit('close')
    return
  } catch (err) {
    const message = err instanceof Error ? err.message : '创建订单失败'
    error.value = message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <teleport to="body">
    <div v-if="open" class="dialog-backdrop" role="dialog" aria-modal="true">
      <div class="dialog">
        <header class="dialog__header">
          <h3>立即购买</h3>
          <button type="button" class="close-btn" aria-label="关闭" @click="close">×</button>
        </header>

        <section v-if="product" class="dialog__content">
          <div class="product-summary">
            <img v-if="product.mainImage" :src="product.mainImage" :alt="product.name" loading="lazy" />
            <div>
              <h4>{{ product.name }}</h4>
              <p>库存：{{ product.stock }} 件</p>
              <p>单价：¥{{ product.price.toFixed(2) }}</p>
            </div>
          </div>

          <form class="purchase-form" @submit.prevent="submit">
            <label>
              <span>消费者ID</span>
              <input v-model="form.consumerLookupId" type="text" readonly />
              <small class="hint">系统已为您生成订单查询编号</small>
            </label>

            <div class="grid">
              <label>
                <span>收货人姓名</span>
                <input v-model="form.recipientName" type="text" placeholder="如：张三" required />
              </label>
              <label>
                <span>联系电话</span>
                <input v-model="form.recipientPhone" type="tel" placeholder="请填写联系方式" required />
              </label>
            </div>

            <label>
              <span>收货地址</span>
              <textarea
                v-model="form.shippingAddress"
                rows="2"
                placeholder="请输入详细收货地址"
                required
              ></textarea>
            </label>

            <div class="grid">
              <label>
                <span>支付方式</span>
                <select v-model="form.paymentMethod">
                  <option value="WECHAT">微信支付</option>
                  <option value="ALIPAY">支付宝</option>
                  <option value="BANK">银行转账</option>
                  <option value="COD">货到付款</option>
                </select>
              </label>
              <label>
                <span>购买数量</span>
                <input
                  v-model.number="form.quantity"
                  type="number"
                  min="1"
                  :max="maxQuantity"
                  :placeholder="`最多 ${maxQuantity} 件`"
                  required
                />
              </label>
            </div>

            <label>
              <span>备注（可选）</span>
              <textarea v-model="form.remark" rows="2" placeholder="填写特殊需求或留言"></textarea>
            </label>

            <div class="summary">
              <span>应付金额：</span>
              <strong>¥{{ totalAmount.toFixed(2) }}</strong>
            </div>

            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button type="button" class="secondary" @click="close" :disabled="submitting">取消</button>
              <button type="submit" class="primary" :disabled="submitting">
                {{ submitting ? '提交中...' : '确认下单' }}
              </button>
            </div>
          </form>
        </section>
        <p v-else class="empty">未找到商品信息，请关闭后重试。</p>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: grid;
  place-items: center;
  padding: 1.5rem;
  z-index: 1000;
}

.dialog {
  width: min(640px, 100%);
  background: #fff;
  border-radius: 1.25rem;
  box-shadow: 0 32px 80px rgba(15, 23, 42, 0.18);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.dialog__header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
}

.close-btn {
  border: none;
  background: transparent;
  font-size: 1.5rem;
  cursor: pointer;
  line-height: 1;
  color: rgba(15, 23, 42, 0.6);
  transition: color 0.2s ease;
}

.close-btn:hover {
  color: rgba(15, 23, 42, 0.85);
}

.dialog__content {
  padding: 1.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-summary {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.product-summary img {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 1rem;
}

.product-summary h4 {
  margin: 0;
  font-size: 1.1rem;
}

.product-summary p {
  margin: 0.25rem 0 0;
  color: rgba(15, 23, 42, 0.65);
  font-size: 0.9rem;
}

.purchase-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.purchase-form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: rgba(15, 23, 42, 0.75);
}

.purchase-form .hint {
  font-size: 0.75rem;
  color: rgba(15, 23, 42, 0.6);
}

.purchase-form input,
.purchase-form textarea,
.purchase-form select {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 0.75rem;
  padding: 0.65rem 0.85rem;
  font-size: 0.95rem;
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

.purchase-form input:focus,
.purchase-form textarea:focus,
.purchase-form select:focus {
  outline: none;
  border-color: rgba(92, 44, 12, 0.45);
  box-shadow: 0 0 0 3px rgba(92, 44, 12, 0.15);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(242, 177, 66, 0.12);
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  font-size: 1rem;
}

.summary strong {
  font-size: 1.25rem;
  color: #5c2c0c;
}

.error {
  color: #b91c1c;
  background: rgba(248, 113, 113, 0.12);
  border-radius: 0.75rem;
  padding: 0.65rem 0.85rem;
  font-size: 0.9rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.actions button {
  border: none;
  border-radius: 999px;
  padding: 0.65rem 1.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

.actions .primary {
  background: linear-gradient(135deg, rgba(242, 177, 66, 0.85), rgba(111, 169, 173, 0.85));
  color: #fff;
  box-shadow: 0 12px 30px rgba(242, 177, 66, 0.28);
}

.actions .primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 16px 40px rgba(242, 177, 66, 0.35);
}

.actions .secondary {
  background: rgba(15, 23, 42, 0.08);
  color: rgba(15, 23, 42, 0.75);
}

.actions .secondary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
}

.empty {
  padding: 2rem;
  text-align: center;
  color: rgba(15, 23, 42, 0.65);
}

@media (max-width: 540px) {
  .dialog__header {
    padding: 1rem 1.25rem;
  }

  .dialog__content {
    padding: 1.25rem;
  }

  .product-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-summary img {
    width: 100%;
    height: auto;
  }
}
</style>
