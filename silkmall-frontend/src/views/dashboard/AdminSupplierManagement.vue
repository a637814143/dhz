<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'
import type { PageResponse } from '@/types'

interface SupplierRecord {
  id: number
  username: string
  email: string | null
  phone: string | null
  address: string | null
  companyName: string | null
  businessLicense: string | null
  contactPerson: string | null
  supplierLevel: string | null
  status: string | null
  enabled: boolean | null
  walletBalance?: number | null
  joinDate?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

const loading = ref(false)
const error = ref<string | null>(null)
const suppliers = ref<SupplierRecord[]>([])
const total = ref(0)
const PAGE_SIZE = 8

const filters = reactive({
  keyword: '',
  status: 'all' as 'all' | string,
  level: 'all' as 'all' | string,
  enabled: 'all' as 'all' | 'true' | 'false',
})

const pagination = reactive({
  page: 0,
  size: PAGE_SIZE,
})

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]

const levelOptions = [
  { label: '全部等级', value: 'all' },
  { label: '青铜', value: 'BRONZE' },
  { label: '白银', value: 'SILVER' },
  { label: '黄金', value: 'GOLD' },
  { label: '铂金', value: 'PLATINUM' },
  { label: '钻石', value: 'DIAMOND' },
]

const enabledOptions = [
  { label: '启停状态', value: 'all' },
  { label: '启用', value: 'true' },
  { label: '停用', value: 'false' },
]

const createDialogOpen = ref(false)
const editDialogOpen = ref(false)
const detailDialogOpen = ref(false)

const createError = ref<string | null>(null)
const editError = ref<string | null>(null)

const submittingCreate = ref(false)
const submittingEdit = ref(false)
const deletingId = ref<number | null>(null)

const createForm = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  address: '',
  companyName: '',
  businessLicense: '',
  contactPerson: '',
  supplierLevel: 'BRONZE',
  status: 'PENDING',
  enabled: true,
})

const editForm = reactive({
  id: 0,
  username: '',
  email: '',
  phone: '',
  address: '',
  companyName: '',
  businessLicense: '',
  contactPerson: '',
  supplierLevel: '',
  status: '',
  enabled: true as boolean | null,
})

const detailRecord = ref<SupplierRecord | null>(null)

const hasSuppliers = computed(() => suppliers.value.length > 0)

const totalPages = computed(() => {
  if (pagination.size <= 0) return 0
  return Math.ceil(total.value / pagination.size)
})

const pageIndicator = computed(() => {
  if (totalPages.value <= 0) return '0/0'
  return `${pagination.page + 1}/${totalPages.value}`
})

const canGoPreviousPage = computed(
  () => totalPages.value > 0 && pagination.page > 0
)

const canGoNextPage = computed(
  () => totalPages.value > 0 && pagination.page + 1 < totalPages.value
)

async function loadSuppliers() {
  loading.value = true
  error.value = null
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: PAGE_SIZE,
      sortBy: 'createdAt',
      sortDirection: 'DESC',
    }
    const keyword = filters.keyword.trim()
    if (keyword) params.keyword = keyword
    if (filters.status !== 'all') params.status = filters.status
    if (filters.level !== 'all') params.level = filters.level
    if (filters.enabled !== 'all') params.enabled = filters.enabled === 'true'

    const { data } = await api.get<PageResponse<SupplierRecord>>('/suppliers', { params })
    suppliers.value = Array.isArray(data.content) ? data.content : []
    total.value = typeof data.totalElements === 'number' ? data.totalElements : suppliers.value.length
    pagination.page = typeof data.number === 'number' ? data.number : pagination.page
    pagination.size = PAGE_SIZE
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载供应商数据失败'
  } finally {
    loading.value = false
  }
}

function resetCreateForm() {
  createForm.username = ''
  createForm.password = ''
  createForm.email = ''
  createForm.phone = ''
  createForm.address = ''
  createForm.companyName = ''
  createForm.businessLicense = ''
  createForm.contactPerson = ''
  createForm.supplierLevel = 'BRONZE'
  createForm.status = 'PENDING'
  createForm.enabled = true
  createError.value = null
}

function resetEditForm() {
  editForm.id = 0
  editForm.username = ''
  editForm.email = ''
  editForm.phone = ''
  editForm.address = ''
  editForm.companyName = ''
  editForm.businessLicense = ''
  editForm.contactPerson = ''
  editForm.supplierLevel = ''
  editForm.status = ''
  editForm.enabled = true
  editError.value = null
}

function openCreateDialog() {
  resetCreateForm()
  createDialogOpen.value = true
}

function closeDialogs() {
  createDialogOpen.value = false
  editDialogOpen.value = false
  detailDialogOpen.value = false
  createError.value = null
  editError.value = null
}

async function fetchSupplierDetail(id: number) {
  const { data } = await api.get<SupplierRecord>(`/suppliers/${id}`)
  return data
}

async function openEditDialog(record: SupplierRecord) {
  try {
    const detail = await fetchSupplierDetail(record.id)
    editForm.id = detail.id
    editForm.username = detail.username ?? ''
    editForm.email = detail.email ?? ''
    editForm.phone = detail.phone ?? ''
    editForm.address = detail.address ?? ''
    editForm.companyName = detail.companyName ?? ''
    editForm.businessLicense = detail.businessLicense ?? ''
    editForm.contactPerson = detail.contactPerson ?? ''
    editForm.supplierLevel = detail.supplierLevel ?? 'BRONZE'
    editForm.status = detail.status ?? 'PENDING'
    editForm.enabled = detail.enabled ?? true
    editError.value = null
    editDialogOpen.value = true
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载供应商详情失败'
    window.alert(message)
  }
}

async function openDetailDialog(record: SupplierRecord) {
  try {
    detailRecord.value = await fetchSupplierDetail(record.id)
    detailDialogOpen.value = true
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载供应商详情失败'
    window.alert(message)
  }
}

function formatStatus(status?: string | null) {
  if (!status) return '未设置'
  switch (status.toUpperCase()) {
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    case 'PENDING':
      return '待审核'
    default:
      return status
  }
}

function formatLevel(level?: string | null) {
  if (!level) return '未评级'
  const normalized = level.toUpperCase()
  const mapping: Record<string, string> = {
    BRONZE: '青铜',
    SILVER: '白银',
    GOLD: '黄金',
    PLATINUM: '铂金',
    DIAMOND: '钻石',
  }
  return mapping[normalized] ?? level
}

function formatEnabled(enabled: boolean | null | undefined) {
  if (enabled === null || enabled === undefined) return '未知'
  return enabled ? '启用' : '停用'
}

function formatNumber(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '0'
  return new Intl.NumberFormat('zh-CN').format(value)
}

function formatDateTime(value?: string | null) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('zh-CN', { hour12: false })
}

function membershipBadge(level?: string | null) {
  const normalized = level?.toUpperCase()
  switch (normalized) {
    case 'DIAMOND':
      return 'badge-diamond'
    case 'PLATINUM':
      return 'badge-platinum'
    case 'GOLD':
      return 'badge-gold'
    case 'SILVER':
      return 'badge-silver'
    case 'BRONZE':
      return 'badge-bronze'
    default:
      return 'badge-unknown'
  }
}

async function submitCreate() {
  const username = createForm.username.trim()
  const password = createForm.password.trim()
  const email = createForm.email.trim()
  const companyName = createForm.companyName.trim()

  if (!username) {
    createError.value = '请填写登录账号'
    return
  }
  if (password.length < 6) {
    createError.value = '密码长度至少 6 位'
    return
  }
  if (!email) {
    createError.value = '请填写联系邮箱'
    return
  }
  if (!companyName) {
    createError.value = '请填写公司名称'
    return
  }

  const payload: Record<string, unknown> = {
    username,
    password,
    email,
    phone: createForm.phone.trim() || null,
    address: createForm.address.trim() || null,
    companyName,
    businessLicense: createForm.businessLicense.trim() || null,
    contactPerson: createForm.contactPerson.trim() || null,
    supplierLevel: createForm.supplierLevel,
    status: createForm.status,
    enabled: createForm.enabled,
  }

  submittingCreate.value = true
  createError.value = null
  try {
    await api.post('/users/suppliers', payload)
    closeDialogs()
    await loadSuppliers()
  } catch (err) {
    createError.value = err instanceof Error ? err.message : '创建供应商失败'
  } finally {
    submittingCreate.value = false
  }
}

async function submitEdit() {
  if (!editForm.id) return
  const username = editForm.username.trim()
  const email = editForm.email.trim()
  const companyName = editForm.companyName.trim()

  if (!username) {
    editError.value = '账号不能为空'
    return
  }
  if (!email) {
    editError.value = '邮箱不能为空'
    return
  }
  if (!companyName) {
    editError.value = '公司名称不能为空'
    return
  }

  const payload: Record<string, unknown> = {
    username,
    email,
    phone: editForm.phone.trim() || null,
    address: editForm.address.trim() || null,
    companyName,
    businessLicense: editForm.businessLicense.trim() || null,
    contactPerson: editForm.contactPerson.trim() || null,
    supplierLevel: editForm.supplierLevel || null,
    status: editForm.status || null,
    enabled: editForm.enabled,
  }

  submittingEdit.value = true
  editError.value = null
  try {
    await api.put(`/users/suppliers/${editForm.id}`, payload)
    closeDialogs()
    await loadSuppliers()
  } catch (err) {
    editError.value = err instanceof Error ? err.message : '更新供应商失败'
  } finally {
    submittingEdit.value = false
  }
}

async function deleteSupplier(id: number) {
  if (!window.confirm('确定要删除该供应商吗？此操作不可撤销。')) return
  deletingId.value = id
  try {
    await api.delete(`/suppliers/${id}`)
    await loadSuppliers()
  } catch (err) {
    const message = err instanceof Error ? err.message : '删除供应商失败'
    window.alert(message)
  } finally {
    deletingId.value = null
  }
}

function changePage(target: number) {
  if (target < 0 || target === pagination.page) return
  if (totalPages.value && target >= totalPages.value) return
  pagination.page = target
  loadSuppliers()
}

watch(
  () => ({ ...filters }),
  () => {
    pagination.page = 0
    loadSuppliers()
  }
)

onMounted(() => {
  loadSuppliers()
})
</script>

<template>
  <section class="management-shell">
    <header class="page-header">
      <div>
        <h1>供应商账号管理</h1>
        <p>集中展示并维护所有供应商资料，支持新增、编辑、查看与删除操作。</p>
      </div>
      <button type="button" class="primary" @click="openCreateDialog">新增供应商</button>
    </header>

    <section class="filters" aria-label="筛选条件">
      <div class="filter-group">
        <label>
          <span>关键词</span>
          <input v-model="filters.keyword" type="search" placeholder="按账号、公司、邮箱或电话搜索" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          <span>供应商等级</span>
          <select v-model="filters.level">
            <option v-for="option in levelOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          <span>启停状态</span>
          <select v-model="filters.enabled">
            <option v-for="option in enabledOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
      </div>
    </section>

    <div v-if="loading" class="placeholder">正在加载供应商数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
      <section class="panel">
        <table v-if="hasSuppliers" class="data-table">
          <thead>
            <tr>
              <th scope="col">ID</th>
              <th scope="col">账号 / 公司</th>
              <th scope="col">联系人</th>
              <th scope="col">联系方式</th>
              <th scope="col">等级</th>
              <th scope="col">审核状态</th>
              <th scope="col">启用</th>
              <th scope="col" class="actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in suppliers" :key="record.id">
              <td>{{ record.id }}</td>
              <td>
                <div class="cell-main">
                  <strong>{{ record.companyName ?? record.username }}</strong>
                  <span class="sub">{{ record.username }}</span>
                  <span v-if="record.businessLicense" class="sub">许可证：{{ record.businessLicense }}</span>
                </div>
              </td>
              <td>
                <div class="cell-main">
                  <span>{{ record.contactPerson ?? '—' }}</span>
                  <span v-if="record.address" class="sub">{{ record.address }}</span>
                </div>
              </td>
              <td>
                <div class="cell-main">
                  <span>{{ record.phone ?? '—' }}</span>
                  <span v-if="record.email" class="sub">{{ record.email }}</span>
                </div>
              </td>
              <td>
                <span :class="['level-badge', membershipBadge(record.supplierLevel)]">
                  {{ formatLevel(record.supplierLevel) }}
                </span>
              </td>
              <td>
                <span class="status-pill" :class="record.status?.toUpperCase()">
                  {{ formatStatus(record.status) }}
                </span>
              </td>
              <td>
                <span :class="['status-pill', record.enabled ? 'is-active' : 'is-disabled']">
                  {{ formatEnabled(record.enabled) }}
                </span>
              </td>
              <td class="actions">
                <button type="button" class="link" @click="openDetailDialog(record)">查看</button>
                <button type="button" class="link" @click="openEditDialog(record)">编辑</button>
                <button
                  type="button"
                  class="link is-danger"
                  :disabled="deletingId === record.id"
                  @click="deleteSupplier(record.id)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty">暂无供应商记录，点击右上角按钮新增供应商账号。</p>

        <nav
          v-if="hasSuppliers && totalPages > 0"
          class="pagination-footer"
          aria-label="供应商分页"
        >
          <span class="pagination-status">共 {{ formatNumber(total) }} 个供应商</span>
          <div class="pagination-controls">
            <button
              type="button"
              class="pager-button"
              :disabled="!canGoPreviousPage || loading"
              @click="changePage(pagination.page - 1)"
            >
              &lt;
            </button>
            <span class="page-indicator">{{ pageIndicator }}</span>
            <button
              type="button"
              class="pager-button"
              :disabled="!canGoNextPage || loading"
              @click="changePage(pagination.page + 1)"
            >
              &gt;
            </button>
          </div>
        </nav>
      </section>
    </template>

    <teleport to="body">
      <div v-if="createDialogOpen" class="dialog-backdrop" role="dialog" aria-modal="true">
        <div class="dialog">
          <header class="dialog__header">
            <h2>新增供应商</h2>
            <button type="button" class="close-btn" aria-label="关闭" @click="closeDialogs">×</button>
          </header>
          <form class="dialog__content" @submit.prevent="submitCreate">
            <div class="grid">
              <label>
                <span>登录账号</span>
                <input v-model="createForm.username" type="text" placeholder="请输入供应商登录账号" required />
              </label>
              <label>
                <span>登录密码</span>
                <input v-model="createForm.password" type="password" placeholder="至少 6 位字符" required />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>邮箱</span>
                <input v-model="createForm.email" type="email" placeholder="name@example.com" required />
              </label>
              <label>
                <span>联系电话</span>
                <input v-model="createForm.phone" type="tel" placeholder="请输入联系电话" />
              </label>
            </div>
            <label>
              <span>联系地址</span>
              <input v-model="createForm.address" type="text" placeholder="请输入联系地址" />
            </label>
            <div class="grid">
              <label>
                <span>公司名称</span>
                <input v-model="createForm.companyName" type="text" placeholder="请输入公司名称" required />
              </label>
              <label>
                <span>营业执照编号</span>
                <input v-model="createForm.businessLicense" type="text" placeholder="可选，营业执照信息" />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>联系人</span>
                <input v-model="createForm.contactPerson" type="text" placeholder="可选，联系人姓名" />
              </label>
              <label>
                <span>供应商等级</span>
                <select v-model="createForm.supplierLevel">
                  <option value="BRONZE">青铜</option>
                  <option value="SILVER">白银</option>
                  <option value="GOLD">黄金</option>
                  <option value="PLATINUM">铂金</option>
                  <option value="DIAMOND">钻石</option>
                </select>
              </label>
              <label>
                <span>审核状态</span>
                <select v-model="createForm.status">
                  <option value="PENDING">待审核</option>
                  <option value="APPROVED">已通过</option>
                  <option value="REJECTED">已驳回</option>
                </select>
              </label>
            </div>
            <label>
              <span>启用状态</span>
              <select v-model="createForm.enabled">
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </label>
            <p v-if="createError" class="form-error">{{ createError }}</p>
            <footer class="dialog__footer">
              <button type="button" @click="closeDialogs">取消</button>
              <button type="submit" class="primary" :disabled="submittingCreate">
                {{ submittingCreate ? '创建中…' : '确认创建' }}
              </button>
            </footer>
          </form>
        </div>
      </div>

      <div v-if="editDialogOpen" class="dialog-backdrop" role="dialog" aria-modal="true">
        <div class="dialog">
          <header class="dialog__header">
            <h2>编辑供应商</h2>
            <button type="button" class="close-btn" aria-label="关闭" @click="closeDialogs">×</button>
          </header>
          <form class="dialog__content" @submit.prevent="submitEdit">
            <div class="grid">
              <label>
                <span>登录账号</span>
                <input v-model="editForm.username" type="text" placeholder="请输入登录账号" required />
              </label>
              <label>
                <span>邮箱</span>
                <input v-model="editForm.email" type="email" placeholder="name@example.com" required />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>联系电话</span>
                <input v-model="editForm.phone" type="tel" placeholder="请输入联系电话" />
              </label>
              <label>
                <span>联系地址</span>
                <input v-model="editForm.address" type="text" placeholder="请输入联系地址" />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>公司名称</span>
                <input v-model="editForm.companyName" type="text" placeholder="请输入公司名称" required />
              </label>
              <label>
                <span>营业执照编号</span>
                <input v-model="editForm.businessLicense" type="text" placeholder="可选，营业执照信息" />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>联系人</span>
                <input v-model="editForm.contactPerson" type="text" placeholder="可选，联系人姓名" />
              </label>
              <label>
                <span>供应商等级</span>
                <select v-model="editForm.supplierLevel">
                  <option value="BRONZE">青铜</option>
                  <option value="SILVER">白银</option>
                  <option value="GOLD">黄金</option>
                  <option value="PLATINUM">铂金</option>
                  <option value="DIAMOND">钻石</option>
                </select>
              </label>
              <label>
                <span>审核状态</span>
                <select v-model="editForm.status">
                  <option value="PENDING">待审核</option>
                  <option value="APPROVED">已通过</option>
                  <option value="REJECTED">已驳回</option>
                </select>
              </label>
            </div>
            <label>
              <span>启用状态</span>
              <select v-model="editForm.enabled">
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </label>
            <p v-if="editError" class="form-error">{{ editError }}</p>
            <footer class="dialog__footer">
              <button type="button" @click="closeDialogs">取消</button>
              <button type="submit" class="primary" :disabled="submittingEdit">
                {{ submittingEdit ? '保存中…' : '保存修改' }}
              </button>
            </footer>
          </form>
        </div>
      </div>

      <div v-if="detailDialogOpen && detailRecord" class="dialog-backdrop" role="dialog" aria-modal="true">
        <div class="dialog dialog--wide">
          <header class="dialog__header">
            <h2>供应商详情</h2>
            <button type="button" class="close-btn" aria-label="关闭" @click="closeDialogs">×</button>
          </header>
          <section class="dialog__content detail">
            <dl>
              <div>
                <dt>账号</dt>
                <dd>{{ detailRecord.username }}</dd>
              </div>
              <div>
                <dt>公司名称</dt>
                <dd>{{ detailRecord.companyName ?? '—' }}</dd>
              </div>
              <div>
                <dt>联系方式</dt>
                <dd>
                  <span>{{ detailRecord.phone ?? '—' }}</span>
                  <span v-if="detailRecord.email" class="sub">{{ detailRecord.email }}</span>
                </dd>
              </div>
              <div>
                <dt>联系地址</dt>
                <dd>{{ detailRecord.address ?? '—' }}</dd>
              </div>
              <div>
                <dt>联系人</dt>
                <dd>{{ detailRecord.contactPerson ?? '—' }}</dd>
              </div>
              <div>
                <dt>营业执照</dt>
                <dd>{{ detailRecord.businessLicense ?? '—' }}</dd>
              </div>
              <div>
                <dt>等级</dt>
                <dd>{{ formatLevel(detailRecord.supplierLevel) }}</dd>
              </div>
              <div>
                <dt>审核状态</dt>
                <dd>{{ formatStatus(detailRecord.status) }}</dd>
              </div>
              <div>
                <dt>启用状态</dt>
                <dd>{{ formatEnabled(detailRecord.enabled) }}</dd>
              </div>
              <div>
                <dt>账户余额</dt>
                <dd>{{ typeof detailRecord.walletBalance === 'number' ? detailRecord.walletBalance.toFixed(2) : '—' }}</dd>
              </div>
              <div>
                <dt>加入时间</dt>
                <dd>{{ formatDateTime(detailRecord.joinDate) }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(detailRecord.createdAt) }}</dd>
              </div>
              <div>
                <dt>最近更新</dt>
                <dd>{{ formatDateTime(detailRecord.updatedAt) }}</dd>
              </div>
            </dl>
          </section>
          <footer class="dialog__footer">
            <button type="button" @click="closeDialogs">关闭</button>
          </footer>
        </div>
      </div>
    </teleport>
  </section>
</template>

<style scoped>
.management-shell {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
}

.page-header h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
}

.page-header p {
  margin: 0.35rem 0 0;
  color: rgba(71, 85, 105, 0.75);
}

.primary {
  padding: 0.6rem 1.4rem;
  border-radius: 999px;
  background: linear-gradient(135deg, #f97316, #fbbf24);
  color: #fff;
  border: none;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 15px 30px rgba(249, 115, 22, 0.25);
}

.filters {
  background: rgba(255, 255, 255, 0.95);
  padding: 1.25rem 1.5rem;
  border-radius: 20px;
  box-shadow: 0 15px 30px rgba(15, 23, 42, 0.06);
}

.filter-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.filter-group label {
  display: grid;
  gap: 0.4rem;
  font-size: 0.9rem;
}

.filter-group input,
.filter-group select {
  padding: 0.55rem 0.75rem;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: #fff;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.9rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  vertical-align: top;
}

.data-table th {
  font-weight: 600;
  color: rgba(15, 23, 42, 0.65);
}

.cell-main {
  display: grid;
  gap: 0.25rem;
}

.cell-main .sub {
  font-size: 0.8rem;
  color: rgba(71, 85, 105, 0.7);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.16);
  color: rgba(30, 41, 59, 0.75);
}

.status-pill.APPROVED {
  background: rgba(34, 197, 94, 0.16);
  color: #166534;
}

.status-pill.REJECTED {
  background: rgba(248, 113, 113, 0.16);
  color: #991b1b;
}

.status-pill.PENDING {
  background: rgba(56, 189, 248, 0.16);
  color: #075985;
}

.status-pill.is-active {
  background: rgba(34, 197, 94, 0.16);
  color: #166534;
}

.status-pill.is-disabled {
  background: rgba(248, 113, 113, 0.16);
  color: #991b1b;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  background: rgba(79, 70, 229, 0.12);
  color: #4338ca;
}

.level-badge.badge-gold {
  background: rgba(234, 179, 8, 0.18);
  color: #854d0e;
}

.level-badge.badge-platinum {
  background: rgba(148, 163, 184, 0.18);
  color: #0f172a;
}

.level-badge.badge-silver {
  background: rgba(148, 163, 184, 0.22);
  color: #334155;
}

.level-badge.badge-bronze {
  background: rgba(217, 119, 6, 0.18);
  color: #78350f;
}

.level-badge.badge-diamond {
  background: rgba(14, 165, 233, 0.18);
  color: #0f172a;
}

.level-badge.badge-unknown {
  background: rgba(148, 163, 184, 0.18);
  color: rgba(30, 41, 59, 0.7);
}

.actions {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}

.actions .link {
  border: none;
  background: none;
  color: #2563eb;
  cursor: pointer;
  padding: 0;
}

.actions .link.is-danger {
  color: #dc2626;
}

.placeholder {
  padding: 1.6rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  text-align: center;
  color: rgba(71, 85, 105, 0.75);
}

.placeholder.is-error {
  background: rgba(248, 113, 113, 0.18);
  color: #7f1d1d;
}

.empty {
  text-align: center;
  color: rgba(71, 85, 105, 0.75);
  padding: 1rem 0;
}

.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem 1.5rem;
  margin-top: 1.2rem;
}

.pagination-status {
  color: rgba(71, 85, 105, 0.75);
  font-weight: 600;
}

.pagination-controls {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
}

.pager-button {
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.55);
  background: rgba(226, 232, 240, 0.6);
  color: rgba(30, 41, 59, 0.8);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.pager-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  min-width: 3.2rem;
  text-align: center;
  font-weight: 600;
  color: rgba(30, 41, 59, 0.75);
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: grid;
  place-items: center;
  padding: 2rem 1rem;
  z-index: 50;
}

.dialog {
  width: min(760px, 100%);
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 25px 60px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.dialog--wide {
  width: min(880px, 100%);
}

.dialog__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.75);
}

.dialog__content {
  padding: 1.25rem 1.5rem;
  display: grid;
  gap: 1rem;
  overflow-y: auto;
}

.dialog__content.detail {
  gap: 0.8rem;
}

.dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem 1.5rem;
  border-top: 1px solid rgba(226, 232, 240, 0.75);
}

.dialog__footer button {
  padding: 0.55rem 1.3rem;
  border-radius: 999px;
  border: 1px solid transparent;
  cursor: pointer;
  background: rgba(148, 163, 184, 0.18);
}

.dialog__footer .primary {
  background: linear-gradient(135deg, #f97316, #fbbf24);
  color: #fff;
  border: none;
}

.close-btn {
  border: none;
  background: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

label {
  display: grid;
  gap: 0.4rem;
  font-size: 0.9rem;
}

input,
select {
  padding: 0.55rem 0.75rem;
  border-radius: 12px;
  border: 1px solid rgba(203, 213, 225, 0.8);
  background: #fff;
}

.form-error {
  margin: 0;
  color: #dc2626;
  font-size: 0.85rem;
}

.detail dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem 1.5rem;
}

.detail dt {
  font-weight: 600;
  color: rgba(30, 41, 59, 0.75);
}

.detail dd {
  margin: 0.2rem 0 0;
  color: rgba(30, 41, 59, 0.85);
}

.detail dd .sub {
  display: block;
  color: rgba(71, 85, 105, 0.7);
  font-size: 0.85rem;
}

@media (max-width: 720px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filters {
    padding: 1rem;
  }

  .dialog {
    max-height: 95vh;
  }
}
</style>
