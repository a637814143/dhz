<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import type {
  ConsumerAddress,
  ProductSummary,
  PurchaseOrderPayload,
  PurchaseOrderResult,
} from '@/types'
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
const addressBook = ref<ConsumerAddress[]>([])
const addressBookLoading = ref(false)
const addressBookError = ref<string | null>(null)
const selectedAddressId = ref<number | 'manual'>('manual')

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetForm()
      loadAddressBook()
    }
  }
)

const sizeOrder: Array<'S' | 'M' | 'L' | 'XL' | '2XL' | '3XL'> = ['S', 'M', 'L', 'XL', '2XL', '3XL']
const sizeOptions = computed(() => {
  const raw = props.product?.sizeQuantities ?? null
  if (!raw) return []
  return sizeOrder
    .map((size) => ({
      size,
      quantity: typeof raw[size] === 'number' && Number.isFinite(raw[size]) ? raw[size] : 0,
    }))
    .filter((entry) => entry.quantity >= 0)
})
const hasSizeOptions = computed(() => sizeOptions.value.length > 0)
const selectedSize = ref<string | null>(null)
const selectedSizeQuantity = computed(() => {
  if (!hasSizeOptions.value || !selectedSize.value) return null
  const match = sizeOptions.value.find((item) => item.size === selectedSize.value)
  return match ? match.quantity : null
})
const maxQuantity = computed(() => {
  if (hasSizeOptions.value) {
    return selectedSizeQuantity.value ?? 0
  }
  return props.product?.stock ?? 0
})

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
  addressBook.value = []
  addressBookError.value = null
  addressBookLoading.value = false
  selectedAddressId.value = 'manual'
  selectedSize.value = null
}

async function loadAddressBook() {
  if (!state.user?.id) {
    addressBook.value = []
    addressBookError.value = null
    selectedAddressId.value = 'manual'
    return
  }
  addressBookLoading.value = true
  addressBookError.value = null
  try {
    const { data } = await api.get<ConsumerAddress[]>(`/consumers/${state.user.id}/addresses`)
    const items = data ?? []
    addressBook.value = items.map((item) => ({
      ...item,
      isDefault: Boolean(item.isDefault),
    }))
    if (addressBook.value.length > 0) {
      const preferred =
        addressBook.value.find((entry) => entry.isDefault) ?? addressBook.value[0]
      selectedAddressId.value = preferred.id
      applyAddress(preferred)
    } else {
      selectedAddressId.value = 'manual'
    }
  } catch (err) {
    addressBook.value = []
    selectedAddressId.value = 'manual'
    addressBookError.value = err instanceof Error ? err.message : '加载常用地址失败'
  } finally {
    addressBookLoading.value = false
  }
}

function applyAddress(address: ConsumerAddress | null) {
  if (!address) return
  form.recipientName = address.recipientName ?? ''
  form.recipientPhone = address.recipientPhone ?? ''
  form.shippingAddress = address.shippingAddress ?? ''
}

watch(selectedAddressId, (value) => {
  if (value === 'manual') {
    return
  }
  const match = addressBook.value.find((item) => item.id === value)
  if (match) {
    applyAddress(match)
  }
})

function selectSize(option: { size: string; quantity: number }) {
  selectedSize.value = option.size
  if (option.quantity <= 0) {
    error.value = '该尺码已卖完，请选择其他尺码'
    return
  }
  if (form.quantity > option.quantity) {
    form.quantity = option.quantity
  }
  if (error.value === '请选择尺码' || error.value?.includes('尺码')) {
    error.value = null
  }
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

  if (state.user?.userType !== 'consumer') {
    error.value = '请注册并登录消费者账户后即可购买'
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

  if (hasSizeOptions.value) {
    if (!selectedSize.value) {
      error.value = '请选择尺码'
      return false
    }
    const available = selectedSizeQuantity.value ?? 0
    if (available <= 0) {
      error.value = '该尺码已卖完，请选择其他尺码'
      return false
    }
    if (form.quantity > available) {
      error.value = `所选尺码仅剩 ${available} 件，请调整数量`
      return false
    }
  } else {
    if (props.product.stock < form.quantity) {
      error.value = '购买数量超过库存，请调整数量'
      return false
    }
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
          size: hasSizeOptions.value ? selectedSize.value : null,
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
              <p v-if="hasSizeOptions" class="size-hint">
                已启用尺码库存，请先选择尺码后下单。
              </p>
            </div>
          </div>

          <form class="purchase-form" @submit.prevent="submit">
            <label>
              <span>消费者ID</span>
              <input v-model="form.consumerLookupId" type="text" readonly />
              <small class="hint">系统已为您生成订单查询编号</small>
            </label>

            <section v-if="hasSizeOptions" class="size-selector">
              <header>
                <span>选择尺码</span>
                <small class="hint">请选择具体尺码后再填写购买数量</small>
              </header>
              <div class="size-options" role="group" aria-label="选择尺码">
                <button
                  v-for="option in sizeOptions"
                  :key="option.size"
                  type="button"
                  class="size-chip"
                  :class="{ active: selectedSize === option.size, soldout: option.quantity <= 0 }"
                  @click="selectSize(option)"
                >
                  <strong>{{ option.size }}</strong>
                  <span>{{ option.quantity > 0 ? `剩余 ${option.quantity}` : '已售罄' }}</span>
                </button>
              </div>
            </section>

            <section class="address-selector">
              <header class="address-selector__header">
                <span>选择常用地址</span>
              </header>
              <p v-if="addressBookLoading" class="address-hint">常用地址加载中…</p>
              <p v-else-if="addressBookError" class="address-error">{{ addressBookError }}</p>
              <ul v-else-if="addressBook.length" class="address-options">
                <li v-for="item in addressBook" :key="item.id">
                  <label
                    class="address-option"
                    :class="{ 'is-selected': selectedAddressId === item.id }"
                  >
                    <input
                      v-model="selectedAddressId"
                      type="radio"
                      name="saved-address"
                      :value="item.id"
                    />
                    <div class="address-option__body">
                      <div class="address-option__title">
                        <strong>{{ item.recipientName }}</strong>
                        <span>{{ item.recipientPhone }}</span>
                        <span v-if="item.isDefault" class="address-option__badge">默认</span>
                      </div>
                      <p>{{ item.shippingAddress }}</p>
                    </div>
                  </label>
                </li>
              </ul>
              <p v-else class="address-hint">暂无常用地址，您可以先添加或直接填写。</p>
              <label
                class="address-option manual-option"
                :class="{ 'is-selected': selectedAddressId === 'manual' }"
              >
                <input
                  v-model="selectedAddressId"
                  type="radio"
                  name="saved-address"
                  value="manual"
                />
                <div class="address-option__body">
                  <strong>使用其他地址</strong>
                  <p>手动填写新的收货信息</p>
                </div>
              </label>
            </section>

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
                  :disabled="hasSizeOptions && !selectedSize"
                  required
                />
                <small v-if="hasSizeOptions" class="hint">
                  仅计算所选尺码库存，未选择尺码时无法输入数量。
                </small>
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

.product-summary .size-hint {
  color: #4f46e5;
  font-weight: 600;
}

.purchase-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.size-selector {
  display: grid;
  gap: 0.5rem;
}

.size-selector header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  color: rgba(15, 23, 42, 0.78);
  font-weight: 600;
}

.size-options {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.size-chip {
  border: 1px solid rgba(79, 70, 229, 0.2);
  background: #fff;
  color: #0f172a;
  border-radius: 0.75rem;
  padding: 0.5rem 0.9rem;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.1rem;
  cursor: pointer;
  min-width: 96px;
  transition: border 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.size-chip strong {
  font-size: 0.95rem;
}

.size-chip span {
  font-size: 0.8rem;
  color: rgba(15, 23, 42, 0.65);
}

.size-chip.active {
  border-color: rgba(79, 70, 229, 0.55);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.16);
  transform: translateY(-1px);
}

.size-chip.soldout {
  opacity: 0.55;
  cursor: not-allowed;
}

.size-chip:disabled {
  background: rgba(148, 163, 184, 0.12);
}

.purchase-form label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: rgba(15, 23, 42, 0.75);
}

.address-selector {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(79, 70, 229, 0.06);
}

.address-selector__header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  font-weight: 600;
  color: rgba(15, 23, 42, 0.78);
}

.address-hint {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.address-error {
  margin: 0;
  font-size: 0.85rem;
  color: #b91c1c;
}

.address-options {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.75rem;
}

.address-option {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 0.75rem;
  border-radius: 0.9rem;
  border: 1px solid rgba(79, 70, 229, 0.2);
  background: #fff;
  transition: border 0.2s ease, box-shadow 0.2s ease;
}

.address-option input {
  width: 1rem;
  height: 1rem;
  margin-top: 0.2rem;
}

.address-option__body {
  display: grid;
  gap: 0.35rem;
}

.address-option__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.address-option__title strong {
  font-size: 0.95rem;
  color: rgba(15, 23, 42, 0.85);
}

.address-option__title span {
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.55);
}

.address-option__body p {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.72);
  line-height: 1.4;
}

.address-option__badge {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.16);
  color: #15803d;
  font-size: 0.75rem;
  font-weight: 600;
}

.address-option.is-selected {
  border-color: rgba(79, 70, 229, 0.55);
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.12);
}

.manual-option {
  border-style: dashed;
  background: rgba(79, 70, 229, 0.04);
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
