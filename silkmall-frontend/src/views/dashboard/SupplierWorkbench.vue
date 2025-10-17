<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/services/api'
import { useAuthState } from '@/services/authState'
import type { CategoryOption, HomepageContent, PageResponse, ProductSummary } from '@/types'

interface SupplierProfile {
  id: number
  username: string
  companyName?: string | null
  email?: string | null
  phone?: string | null
  address?: string | null
  contactPerson?: string | null
  businessLicense?: string | null
  supplierLevel?: string | null
  status?: string | null
}

const { state } = useAuthState()

const profile = ref<SupplierProfile | null>(null)
const products = ref<ProductSummary[]>([])
const homeContent = ref<HomepageContent | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const categories = ref<CategoryOption[]>([])
const walletBalance = ref<number | null>(null)
const redeemCodeInput = ref('')
const redeeming = ref(false)
const redeemMessage = ref<string | null>(null)
const redeemError = ref<string | null>(null)

const profileDialogOpen = ref(false)
const profileSubmitting = ref(false)
const profileFormMessage = ref<string | null>(null)
const profileFormError = ref<string | null>(null)

const profileForm = reactive({
  username: '',
  companyName: '',
  contactPerson: '',
  businessLicense: '',
  email: '',
  phone: '',
  address: '',
})

const productDialogOpen = ref(false)
const savingProduct = ref(false)
const productFormError = ref<string | null>(null)
const productFormMessage = ref<string | null>(null)
const deletingProductId = ref<number | null>(null)

const productForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  price: '',
  stock: 0,
  categoryId: null as number | null,
  status: 'ON_SALE',
  mainImage: '',
})

const categoryNameInput = ref('')
const categorySaving = ref(false)
const categoryFeedback = ref<string | null>(null)
const categoryError = ref<string | null>(null)

async function loadProfile() {
  if (!state.user) return
  const { data } = await api.get<SupplierProfile>(`/suppliers/${state.user.id}`)
  profile.value = data
}

async function loadProducts() {
  if (!state.user) return
  const { data } = await api.get<PageResponse<ProductSummary>>(`/products/supplier/${state.user.id}`, {
    params: { page: 0, size: 6 },
  })
  products.value = data.content ?? []
}

async function loadHomeContent() {
  const { data } = await api.get<HomepageContent>('/content/home')
  homeContent.value = data
}

function toCategoryOption(raw: unknown, fallbackName?: string): CategoryOption | null {
  if (!raw || typeof raw !== 'object') {
    return null
  }

  const source = raw as Record<string, unknown>
  const idRaw = source.id ?? source.categoryId ?? source.value
  const id = typeof idRaw === 'number' ? idRaw : Number(idRaw)

  if (!Number.isFinite(id)) {
    return null
  }

  const rawName = source.name ?? source.categoryName ?? fallbackName ?? ''
  const name = typeof rawName === 'string' ? rawName.trim() : String(rawName ?? '').trim()

  return {
    id,
    name: name.length > 0 ? name : `分类 ${id}`,
  }
}

function sortCategoryOptions(options: CategoryOption[]): CategoryOption[] {
  return [...options].sort((a, b) => {
    const nameA = (a.name ?? '').trim()
    const nameB = (b.name ?? '').trim()
    return nameA.localeCompare(nameB, 'zh-CN')
  })
}

function extractCategoryArray(payload: unknown): unknown[] {
  if (Array.isArray(payload)) {
    return payload
  }

  if (!payload || typeof payload !== 'object') {
    return []
  }

  const container = payload as Record<string, unknown>
  const nestedSources: unknown[] = []

  const directData = container['data']
  if (Array.isArray(directData)) {
    return directData
  }

  if (directData && typeof directData === 'object' && directData !== payload) {
    nestedSources.push(directData)
  }

  const candidateKeys = ['records', 'content', 'list', 'items', 'rows', 'result']
  for (const key of candidateKeys) {
    const value = container[key]
    if (Array.isArray(value)) {
      return value
    }
    if (value && typeof value === 'object' && value !== payload) {
      nestedSources.push(value)
    }
  }

  for (const source of nestedSources) {
    const extracted = extractCategoryArray(source)
    if (extracted.length > 0) {
      return extracted
    }
  }

  return []
}

function extractSingleCategory(payload: unknown): unknown {
  const list = extractCategoryArray(payload)
  if (list.length > 0) {
    return list[0]
  }

  if (payload && typeof payload === 'object') {
    const container = payload as Record<string, unknown>
    const directData = container['data']
    if (directData && directData !== payload) {
      return extractSingleCategory(directData)
    }
  }

  return payload
}

function normaliseCategoryOptions(payload: unknown): CategoryOption[] {
  const rawList = extractCategoryArray(payload)

  const deduped = new Map<number, CategoryOption>()
  for (const item of rawList) {
    const option = toCategoryOption(item)
    if (option) {
      deduped.set(option.id, option)
    }
  }

  return sortCategoryOptions([...deduped.values()])
}

function mergeCategoryOption(option: CategoryOption) {
  categories.value = sortCategoryOptions([
    ...categories.value.filter((existing) => existing.id !== option.id),
    option,
  ])
}

async function loadCategories() {
  try {
    const { data } = await api.get<CategoryOption[]>('/categories/options')
    categories.value = normaliseCategoryOptions(data)
  } catch (err) {
    console.warn('加载分类失败', err)
    categories.value = []
  }
}

async function loadWallet() {
  if (!state.user) return
  try {
    const { data } = await api.get<{ balance: number }>('/wallet')
    walletBalance.value = data.balance
  } catch (err) {
    console.warn('加载钱包失败', err)
    walletBalance.value = null
  }
}

function openProfileDialog() {
  const current = profile.value
  profileForm.username = current?.username ?? state.user?.username ?? ''
  profileForm.companyName = current?.companyName ?? ''
  profileForm.contactPerson = current?.contactPerson ?? ''
  profileForm.businessLicense = current?.businessLicense ?? ''
  profileForm.email = current?.email ?? ''
  profileForm.phone = current?.phone ?? ''
  profileForm.address = current?.address ?? ''
  profileFormMessage.value = null
  profileFormError.value = null
  profileDialogOpen.value = true
}

function closeProfileDialog() {
  profileDialogOpen.value = false
  profileSubmitting.value = false
  profileFormMessage.value = null
  profileFormError.value = null
}

async function submitProfileUpdate() {
  if (!state.user) {
    profileFormError.value = '请先登录供应商账号'
    return
  }

  const companyName = profileForm.companyName.trim()
  if (!companyName) {
    profileFormError.value = '请填写企业名称'
    return
  }

  const contactPerson = profileForm.contactPerson.trim()
  if (!contactPerson) {
    profileFormError.value = '请填写联系人信息'
    return
  }

  const username = (profileForm.username || state.user.username).trim()
  if (!username) {
    profileFormError.value = '账号信息缺失，请联系管理员'
    return
  }

  const payload = {
    id: state.user.id,
    username,
    companyName,
    contactPerson,
    businessLicense: profileForm.businessLicense.trim() || null,
    email: profileForm.email.trim() || null,
    phone: profileForm.phone.trim() || null,
    address: profileForm.address.trim() || null,
  }

  profileSubmitting.value = true
  profileFormError.value = null
  profileFormMessage.value = null
  try {
    const { data } = await api.put<SupplierProfile>(`/suppliers/${state.user.id}`, payload)
    profile.value = data
    profileFormMessage.value = '基础信息已更新'
    const authUser = state.user
    if (authUser) {
      state.user = {
        ...authUser,
        username: data.username ?? authUser.username,
        email: data.email ?? null,
        phone: data.phone ?? null,
      }
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '更新基础信息失败'
    profileFormError.value = message
  } finally {
    profileSubmitting.value = false
  }
}

async function bootstrap() {
  loading.value = true
  error.value = null
  try {
    await Promise.all([loadProfile(), loadProducts(), loadHomeContent(), loadCategories(), loadWallet()])
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载供应商数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  bootstrap()
})

const totalStock = computed(() => products.value.reduce((acc, item) => acc + (item.stock ?? 0), 0))
const totalSales = computed(() => products.value.reduce((acc, item) => acc + (item.sales ?? 0), 0))
const onSaleProducts = computed(() => products.value.filter((item) => item.status === 'ON_SALE').length)

function formatCurrency(amount?: number | null) {
  if (typeof amount !== 'number' || Number.isNaN(amount)) return '¥0.00'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(amount)
}

function productStatus(status?: string | null) {
  if (!status) return '未知'
  return status === 'ON_SALE' ? '在售' : '未上架'
}

function resetProductForm() {
  productForm.id = null
  productForm.name = ''
  productForm.description = ''
  productForm.price = ''
  productForm.stock = 0
  productForm.categoryId = null
  productForm.status = 'ON_SALE'
  productForm.mainImage = ''
  productFormError.value = null
  productFormMessage.value = null
}

async function openProductForm(product?: ProductSummary) {
  productFormError.value = null
  productFormMessage.value = null
  await loadCategories()
  if (product) {
    productForm.id = product.id
    productForm.name = product.name
    productForm.description = product.description ?? ''
    productForm.price = product.price?.toString() ?? ''
    productForm.stock = product.stock ?? 0
    const categoryRef = (product as any).categoryId ?? (product as any).category?.id ?? null
    if (typeof categoryRef === 'number') {
      productForm.categoryId = categoryRef
    } else if (categoryRef !== null && categoryRef !== undefined) {
      const parsed = Number(categoryRef)
      productForm.categoryId = Number.isFinite(parsed) ? parsed : null
    } else {
      productForm.categoryId = null
    }
    productForm.status = product.status ?? 'ON_SALE'
    productForm.mainImage = product.mainImage ?? ''
  } else {
    resetProductForm()
  }
  productDialogOpen.value = true
}

function cancelProductForm() {
  productDialogOpen.value = false
  resetProductForm()
}

async function saveProduct() {
  if (!state.user) {
    productFormError.value = '请先登录供应商账号'
    return
  }
  const name = productForm.name.trim()
  if (!name) {
    productFormError.value = '请填写商品名称'
    return
  }
  const price = Number(productForm.price)
  if (!Number.isFinite(price) || price <= 0) {
    productFormError.value = '请输入有效的商品价格'
    return
  }
  const stock = Number(productForm.stock)
  if (!Number.isInteger(stock) || stock < 0) {
    productFormError.value = '库存必须为非负整数'
    return
  }

  const payload: Record<string, unknown> = {
    name,
    description: productForm.description.trim() || null,
    price,
    stock,
    status: productForm.status,
    mainImage: productForm.mainImage.trim() || null,
    supplier: { id: state.user.id },
  }
  if (productForm.categoryId) {
    payload.category = { id: productForm.categoryId }
  }

  savingProduct.value = true
  productFormError.value = null
  try {
    if (productForm.id) {
      await api.put(`/products/${productForm.id}`, payload)
      productFormMessage.value = '商品信息已更新'
    } else {
      await api.post('/products', payload)
      productFormMessage.value = '商品已创建并保存'
    }
    await loadProducts()
    productDialogOpen.value = false
    resetProductForm()
  } catch (err) {
    const message = err instanceof Error ? err.message : '保存商品失败'
    productFormError.value = message
  } finally {
    savingProduct.value = false
  }
}

async function deleteProduct(productId: number) {
  if (!window.confirm('确定删除该商品吗？')) return
  deletingProductId.value = productId
  try {
    await api.delete(`/products/${productId}`)
    await loadProducts()
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除商品失败'
    window.alert(message)
  } finally {
    deletingProductId.value = null
  }
}

async function redeemWallet() {
  redeemMessage.value = null
  redeemError.value = null
  const code = redeemCodeInput.value.trim()
  if (!code) {
    redeemError.value = '请输入兑换码'
    return
  }
  redeeming.value = true
  try {
    const { data } = await api.post<{ balance: number }>('/wallet/redeem', { code })
    walletBalance.value = data.balance
    redeemCodeInput.value = ''
    redeemMessage.value = '兑换成功，余额已更新'
  } catch (err) {
    const message = err instanceof Error ? err.message : '兑换失败'
    redeemError.value = message
  } finally {
    redeeming.value = false
  }
}

async function createCategory() {
  categoryFeedback.value = null
  categoryError.value = null
  const name = categoryNameInput.value.trim()
  if (!name) {
    categoryError.value = '请输入分类名称'
    return
  }
  categorySaving.value = true
  try {
    const { data } = await api.post('/categories', {
      name,
      description: null,
      sortOrder: 0,
      enabled: true,
    })
    const option = toCategoryOption(extractSingleCategory(data), name)

    if (!option) {
      categoryError.value = '分类创建成功，但解析新分类失败，请刷新后重试'
      await loadCategories()
      return
    }

    mergeCategoryOption(option)
    categoryNameInput.value = ''
    categoryFeedback.value = '分类创建成功'
  } catch (err) {
    const message = err instanceof Error ? err.message : '创建分类失败'
    categoryError.value = message
  } finally {
    categorySaving.value = false
  }
}

const statusOptions = [
  { value: 'ON_SALE', label: '在售' },
  { value: 'OFF_SALE', label: '未上架' },
]
</script>

<template>
  <section class="workbench-shell">
    <header class="workbench-header">
      <div>
        <h1>{{ profile?.companyName ?? state.user?.username ?? '供应商工作台' }}</h1>
        <p>掌握商品库存、销售表现与平台促销资源，助力高效运营。</p>
      </div>
      <div class="metrics">
        <div>
          <span>总库存</span>
          <strong>{{ totalStock }}</strong>
        </div>
        <div>
          <span>累计销量</span>
          <strong>{{ totalSales }}</strong>
        </div>
        <div>
          <span>上架商品</span>
          <strong>{{ onSaleProducts }}</strong>
        </div>
        <div>
          <span>钱包余额</span>
          <strong>{{ formatCurrency(walletBalance ?? 0) }}</strong>
        </div>
      </div>
    </header>

    <div v-if="loading" class="placeholder">正在加载工作台数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
      <section class="panel profile" aria-labelledby="supplier-info">
        <div class="panel-title-row">
          <div class="panel-title" id="supplier-info">基础信息</div>
          <button type="button" class="panel-action-button" @click="openProfileDialog">编辑基础信息</button>
        </div>
        <ul>
          <li><span>企业名称</span><strong>{{ profile?.companyName ?? '—' }}</strong></li>
          <li><span>联系人</span><strong>{{ profile?.contactPerson ?? '—' }}</strong></li>
          <li><span>联系人邮箱</span><strong>{{ profile?.email ?? '—' }}</strong></li>
          <li><span>联系电话</span><strong>{{ profile?.phone ?? '—' }}</strong></li>
          <li><span>联系地址</span><strong>{{ profile?.address ?? '—' }}</strong></li>
          <li><span>营业执照</span><strong>{{ profile?.businessLicense ?? '—' }}</strong></li>
          <li><span>等级</span><strong>{{ profile?.supplierLevel ?? '未评级' }}</strong></li>
          <li><span>审核状态</span><strong>{{ profile?.status ?? '待审核' }}</strong></li>
        </ul>
        <div class="redeem-box">
          <label>
            <span>兑换码</span>
            <input
              v-model="redeemCodeInput"
              type="text"
              placeholder="输入兑换码获取余额"
              :disabled="redeeming"
            />
          </label>
          <div class="redeem-actions">
            <button type="button" @click="redeemWallet" :disabled="redeeming">
              {{ redeeming ? '兑换中…' : '兑换余额' }}
            </button>
            <p v-if="redeemMessage" class="redeem-success">{{ redeemMessage }}</p>
            <p v-if="redeemError" class="redeem-error">{{ redeemError }}</p>
          </div>
        </div>
      </section>

      <section class="panel products" aria-labelledby="product-list">
        <div class="panel-title-row">
          <div class="panel-title" id="product-list">商品概览</div>
          <button type="button" class="primary" @click="openProductForm()">新增商品</button>
        </div>
        <div v-if="products.length" class="product-table">
          <table>
            <thead>
              <tr>
                <th scope="col">商品名称</th>
                <th scope="col">售价</th>
                <th scope="col">库存</th>
                <th scope="col">销量</th>
                <th scope="col">状态</th>
                <th scope="col">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in products" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ formatCurrency(item.price) }}</td>
                <td>{{ item.stock }}</td>
                <td>{{ item.sales }}</td>
                <td><span class="status-pill">{{ productStatus(item.status) }}</span></td>
                <td class="actions">
                  <button type="button" class="link-button" @click="openProductForm(item)">编辑</button>
                  <button
                    type="button"
                    class="link-button danger"
                    @click="deleteProduct(item.id)"
                    :disabled="deletingProductId === item.id"
                  >
                    删除
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty">暂无商品，请尽快完成商品录入与上架。</p>

        <form v-if="productDialogOpen" class="product-form" @submit.prevent="saveProduct">
          <div class="grid">
            <label>
              <span>商品名称</span>
              <input v-model="productForm.name" type="text" placeholder="请输入商品名称" />
            </label>
            <label>
              <span>商品状态</span>
              <select v-model="productForm.status">
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>

          <div class="grid">
            <label>
              <span>售价（CNY）</span>
              <input v-model="productForm.price" type="number" min="0" step="0.01" placeholder="如：199" />
            </label>
            <label>
              <span>库存数量</span>
              <input v-model.number="productForm.stock" type="number" min="0" placeholder="库存" />
            </label>
          </div>

          <label>
            <span>所属分类</span>
            <select v-model.number="productForm.categoryId">
              <option :value="null">未选择分类</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>

          <label>
            <span>主图地址</span>
            <input v-model="productForm.mainImage" type="text" placeholder="可选：图片链接" />
          </label>

          <label>
            <span>商品描述</span>
            <textarea v-model="productForm.description" rows="3" placeholder="介绍商品亮点"></textarea>
          </label>

          <p v-if="productFormError" class="error">{{ productFormError }}</p>
          <p v-if="productFormMessage" class="success">{{ productFormMessage }}</p>

          <div class="form-actions">
            <button type="submit" :disabled="savingProduct">
              {{ savingProduct ? '保存中…' : productForm.id ? '保存修改' : '创建商品' }}
            </button>
            <button type="button" class="ghost" @click="cancelProductForm" :disabled="savingProduct">
              取消
            </button>
          </div>
        </form>

        <div class="category-create">
          <h4>快速创建分类</h4>
          <div class="category-row">
            <input v-model="categoryNameInput" type="text" placeholder="分类名称" :disabled="categorySaving" />
            <button type="button" @click="createCategory" :disabled="categorySaving">
              {{ categorySaving ? '创建中…' : '添加分类' }}
            </button>
          </div>
          <p v-if="categoryFeedback" class="success">{{ categoryFeedback }}</p>
          <p v-if="categoryError" class="error">{{ categoryError }}</p>
        </div>
      </section>

      <section class="panel promotions" aria-labelledby="promotion-list">
        <div class="panel-title" id="promotion-list">平台促销建议</div>
        <ul class="promotion-list">
          <li v-for="promo in homeContent?.promotions ?? []" :key="promo.productId">
            <div>
              <strong>{{ promo.title }}</strong>
              <p>{{ promo.description }}</p>
            </div>
            <span class="badge">{{ Math.round(promo.discountRate * 100) }}% OFF</span>
          </li>
        </ul>
      </section>
    </template>

    <div v-if="profileDialogOpen" class="modal-backdrop" @click.self="closeProfileDialog">
      <section class="modal" role="dialog" aria-modal="true" aria-labelledby="supplier-profile-title">
        <header class="modal-header">
          <h3 id="supplier-profile-title">编辑基础信息</h3>
          <button type="button" class="icon-button" @click="closeProfileDialog" aria-label="关闭">
            ×
          </button>
        </header>
        <form class="modal-body" @submit.prevent="submitProfileUpdate">
          <label>
            <span>企业名称</span>
            <input v-model="profileForm.companyName" type="text" placeholder="请输入企业名称" />
          </label>
          <label>
            <span>联系人</span>
            <input v-model="profileForm.contactPerson" type="text" placeholder="请输入联系人" />
          </label>
          <label>
            <span>联系人邮箱</span>
            <input v-model="profileForm.email" type="email" placeholder="请输入联系人邮箱" />
          </label>
          <label>
            <span>联系电话</span>
            <input v-model="profileForm.phone" type="tel" placeholder="请输入联系电话" />
          </label>
          <label>
            <span>联系地址</span>
            <input v-model="profileForm.address" type="text" placeholder="请输入联系地址" />
          </label>
          <label>
            <span>营业执照</span>
            <input v-model="profileForm.businessLicense" type="text" placeholder="请输入营业执照编号" />
          </label>
          <p v-if="profileFormMessage" class="modal-success">{{ profileFormMessage }}</p>
          <p v-if="profileFormError" class="modal-error">{{ profileFormError }}</p>
          <footer class="modal-actions">
            <button type="submit" class="primary-button" :disabled="profileSubmitting">
              {{ profileSubmitting ? '保存中…' : '保存信息' }}
            </button>
            <button type="button" class="ghost-button" @click="closeProfileDialog" :disabled="profileSubmitting">
              取消
            </button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.workbench-shell {
  display: grid;
  gap: 2.5rem;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  gap: 2rem;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.12), rgba(56, 189, 248, 0.12));
  padding: 2.25rem;
  border-radius: 24px;
}

.workbench-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.workbench-header p {
  color: rgba(15, 23, 42, 0.66);
}

.metrics {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.metrics div {
  background: rgba(255, 255, 255, 0.9);
  padding: 1rem 1.4rem;
  border-radius: 16px;
  box-shadow: 0 12px 26px rgba(14, 165, 233, 0.18);
  min-width: 140px;
}

.metrics span {
  display: block;
  font-size: 0.85rem;
  color: rgba(15, 23, 42, 0.6);
}

.metrics strong {
  font-size: 1.4rem;
  font-weight: 700;
}

.placeholder {
  padding: 2rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  text-align: center;
  color: rgba(15, 23, 42, 0.6);
}

.placeholder.is-error {
  background: rgba(248, 113, 113, 0.12);
  color: #7f1d1d;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 1.8rem;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 1.5rem;
}

.panel-title {
  font-weight: 700;
  font-size: 1.1rem;
  color: rgba(15, 23, 42, 0.78);
}

.panel-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.panel-action-button {
  border: 1px solid rgba(14, 165, 233, 0.24);
  background: rgba(56, 189, 248, 0.12);
  color: #0f172a;
  border-radius: 999px;
  padding: 0.5rem 1.2rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease, color 0.2s ease;
}

.panel-action-button:hover {
  background: rgba(14, 165, 233, 0.18);
  color: #0e7490;
  transform: translateY(-1px);
}

.panel-action-button:focus-visible {
  outline: 2px solid rgba(14, 165, 233, 0.45);
  outline-offset: 2px;
}

.panel-action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.primary {
  border: none;
  border-radius: 0.75rem;
  padding: 0.55rem 1.5rem;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.panel.profile ul {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 0.9rem;
}

.panel.profile li {
  display: flex;
  justify-content: space-between;
  color: rgba(15, 23, 42, 0.65);
}

.panel.profile strong {
  color: rgba(15, 23, 42, 0.85);
}

.redeem-box {
  display: grid;
  gap: 0.75rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  padding-top: 1rem;
}

.redeem-box label {
  display: grid;
  gap: 0.35rem;
  font-weight: 600;
}

.redeem-box input {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.redeem-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.redeem-actions button {
  padding: 0.55rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #0ea5e9, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.redeem-actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.redeem-success {
  color: #15803d;
}

.redeem-error {
  color: #b91c1c;
}

.product-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.product-table th,
.product-table td {
  padding: 0.75rem 0.5rem;
  text-align: left;
}

.product-table thead {
  color: rgba(15, 23, 42, 0.55);
}

.product-table tbody tr:nth-child(odd) {
  background: rgba(14, 165, 233, 0.08);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.18);
  color: #0ea5e9;
  font-weight: 600;
}

.product-table td.actions {
  display: flex;
  gap: 0.5rem;
}

.link-button {
  border: none;
  background: none;
  color: #2563eb;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.link-button.danger {
  color: #b91c1c;
}

.product-form {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.product-form .grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.product-form label {
  display: grid;
  gap: 0.4rem;
  font-weight: 600;
}

.product-form input,
.product-form select,
.product-form textarea {
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  font-size: 0.95rem;
}

.product-form textarea {
  resize: vertical;
}

.product-form .form-actions {
  display: flex;
  gap: 0.75rem;
}

.product-form .form-actions button {
  padding: 0.55rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.product-form .form-actions button.ghost {
  background: transparent;
  color: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(15, 23, 42, 0.15);
}

.product-form .form-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.product-form .error {
  color: #b91c1c;
}

.product-form .success {
  color: #15803d;
}

.category-create {
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 0.75rem;
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.category-row input {
  flex: 1;
  min-width: 200px;
  padding: 0.6rem 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.category-row button {
  padding: 0.55rem 1.4rem;
  border-radius: 0.75rem;
  border: none;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.category-row button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.category-create .success {
  color: #15803d;
}

.category-create .error {
  color: #b91c1c;
}

.empty {
  color: rgba(15, 23, 42, 0.6);
}

.promotion-list {
  list-style: none;
  padding: 0;
  display: grid;
  gap: 1rem;
}

.promotion-list li {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.25rem;
  border-radius: 16px;
  border: 1px solid rgba(14, 165, 233, 0.15);
  background: rgba(240, 249, 255, 0.85);
}

.promotion-list strong {
  display: block;
  margin-bottom: 0.25rem;
}

.promotion-list p {
  color: rgba(15, 23, 42, 0.6);
  font-size: 0.9rem;
}

.badge {
  font-weight: 700;
  color: #0284c7;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 30;
}

.modal {
  background: #fff;
  border-radius: 20px;
  max-width: 520px;
  width: 100%;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.2rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
}

.modal-body label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  font-size: 0.95rem;
}

.modal-body input {
  padding: 0.65rem 0.8rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 23, 42, 0.18);
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.primary-button {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  color: #fff;
  border: none;
  border-radius: 0.75rem;
  padding: 0.65rem 1.4rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.primary-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.22);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ghost-button {
  border: 1px solid rgba(15, 23, 42, 0.18);
  background: transparent;
  color: #0f172a;
  border-radius: 0.75rem;
  padding: 0.65rem 1.3rem;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.ghost-button:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.15);
}

.ghost-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.icon-button {
  border: none;
  background: transparent;
  font-size: 1.4rem;
  cursor: pointer;
  color: rgba(15, 23, 42, 0.6);
}

.icon-button:hover {
  color: rgba(15, 23, 42, 0.85);
}

.modal-success {
  color: #15803d;
  font-size: 0.9rem;
}

.modal-error {
  color: #b91c1c;
  font-size: 0.9rem;
}

@media (max-width: 900px) {
  .workbench-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .metrics {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
