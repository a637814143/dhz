<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'

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
  enabled: boolean
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

const loading = ref(false)
const error = ref<string | null>(null)
const suppliers = ref<SupplierRecord[]>([])
const total = ref(0)
const PAGE_SIZE = 8

const filters = reactive({
  keyword: '',
  enabled: 'all' as 'all' | 'true' | 'false',
  supplierLevel: 'all' as 'all' | string,
  status: 'all' as 'all' | string,
})

const createDialogOpen = ref(false)
const editDialogOpen = ref(false)

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
  supplierLevel: 'BRONZE',
  status: 'PENDING',
  enabled: true as boolean | null,
})

const submittingCreate = ref(false)
const submittingEdit = ref(false)

const levelOptions = [
  { label: '全部等级', value: 'all' },
  { label: '青铜', value: 'BRONZE' },
  { label: '白银', value: 'SILVER' },
  { label: '黄金', value: 'GOLD' },
  { label: '铂金', value: 'PLATINUM' },
  { label: '钻石', value: 'DIAMOND' },
]

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
]

const enabledOptions = [
  { label: '全部启停', value: 'all' },
  { label: '启用', value: 'true' },
  { label: '停用', value: 'false' },
]

const pagination = reactive({
  page: 0,
  size: PAGE_SIZE,
})

const hasSuppliers = computed(() => suppliers.value.length > 0)

const totalPages = computed(() => {
  if (pagination.size <= 0) return 0
  return Math.ceil(total.value / pagination.size)
})

const pageIndicator = computed(() => {
  if (totalPages.value <= 0) return '0/0'
  return `${pagination.page + 1}/${totalPages.value}`
})

const canGoPreviousPage = computed(() => totalPages.value > 0 && pagination.page > 0)
const canGoNextPage = computed(() => totalPages.value > 0 && pagination.page + 1 < totalPages.value)

async function loadSuppliers() {
  loading.value = true
  error.value = null
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: PAGE_SIZE,
    }
    if (filters.keyword.trim()) {
      params.keyword = filters.keyword.trim()
    }
    if (filters.enabled !== 'all') {
      params.enabled = filters.enabled === 'true'
    }
    if (filters.supplierLevel !== 'all') {
      params.supplierLevel = filters.supplierLevel
    }
    if (filters.status !== 'all') {
      params.status = filters.status
    }

    const requestedPage = params.page as number
    const { data } = await api.get<PageResponse<SupplierRecord>>('/suppliers', { params })
    const items = Array.isArray(data.content) ? data.content : []
    const totalElements = typeof data.totalElements === 'number' ? data.totalElements : 0
    const backendPage = typeof data.number === 'number' ? data.number : requestedPage
    const calculatedTotalPages = PAGE_SIZE > 0 ? Math.ceil(totalElements / PAGE_SIZE) : 0
    const safePage = calculatedTotalPages > 0 ? Math.min(backendPage, calculatedTotalPages - 1) : 0

    if (calculatedTotalPages > 0 && backendPage >= calculatedTotalPages && requestedPage !== safePage) {
      pagination.page = safePage
      await loadSuppliers()
      return
    }

    suppliers.value = items
    total.value = totalElements
    pagination.page = safePage
    pagination.size = PAGE_SIZE
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载供应商数据失败'
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetCreateForm()
  createDialogOpen.value = true
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
}

function openEditDialog(record: SupplierRecord) {
  editForm.id = record.id
  editForm.username = record.username
  editForm.email = record.email ?? ''
  editForm.phone = record.phone ?? ''
  editForm.address = record.address ?? ''
  editForm.companyName = record.companyName ?? ''
  editForm.businessLicense = record.businessLicense ?? ''
  editForm.contactPerson = record.contactPerson ?? ''
  editForm.supplierLevel = record.supplierLevel ?? 'BRONZE'
  editForm.status = record.status ?? 'PENDING'
  editForm.enabled = record.enabled
  editDialogOpen.value = true
}

function closeDialogs() {
  createDialogOpen.value = false
  editDialogOpen.value = false
}

async function submitCreate() {
  if (submittingCreate.value) return
  submittingCreate.value = true
  error.value = null
  try {
    await api.post('/suppliers', {
      username: createForm.username.trim(),
      password: createForm.password,
      email: createForm.email.trim() || null,
      phone: createForm.phone.trim() || null,
      address: createForm.address.trim() || null,
      companyName: createForm.companyName.trim(),
      businessLicense: createForm.businessLicense.trim() || null,
      contactPerson: createForm.contactPerson.trim(),
      supplierLevel: createForm.supplierLevel,
      status: createForm.status,
      enabled: createForm.enabled,
    })
    closeDialogs()
    await loadSuppliers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建供应商失败'
  } finally {
    submittingCreate.value = false
  }
}

async function submitEdit() {
  if (submittingEdit.value || !editForm.id) return
  submittingEdit.value = true
  error.value = null
  try {
    const payload = {
      username: editForm.username.trim(),
      email: editForm.email.trim() || null,
      phone: editForm.phone.trim() || null,
      address: editForm.address.trim() || null,
      companyName: editForm.companyName.trim(),
      businessLicense: editForm.businessLicense.trim() || null,
      contactPerson: editForm.contactPerson.trim(),
      supplierLevel: editForm.supplierLevel,
      status: editForm.status,
      enabled: editForm.enabled,
    }
    await api.put(`/suppliers/${editForm.id}`, payload)
    closeDialogs()
    await loadSuppliers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新供应商失败'
  } finally {
    submittingEdit.value = false
  }
}

async function deleteSupplier(id: number) {
  if (!window.confirm('确定要删除该供应商吗？此操作无法撤销。')) return
  try {
    await api.delete(`/suppliers/${id}`)
    await loadSuppliers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除供应商失败'
  }
}

function supplierLevelLabel(level: string | null) {
  const option = levelOptions.find((item) => item.value === level)
  if (option) return option.label
  return level ? level : '未分级'
}

function supplierStatusLabel(status: string | null) {
  switch (status) {
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已拒绝'
    case 'PENDING':
      return '待审核'
    default:
      return status || '未知'
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

function formatStatus(record: SupplierRecord) {
  return record.enabled ? '启用' : '停用'
}
</script>

<template>
  <section class="management-shell">
    <header class="page-header">
      <div>
        <h1>供应商账号管理</h1>
        <p>查看和维护供应商账号信息，支持添加、编辑、停用与删除。</p>
      </div>
      <button type="button" class="primary" @click="openCreateDialog">新增供应商</button>
    </header>

    <section class="filters" aria-label="筛选器">
      <div class="filter-group">
        <label>
          <span>关键词</span>
          <input v-model="filters.keyword" type="search" placeholder="按账号、邮箱、电话或公司搜索" />
        </label>
        <label>
          <span>启用状态</span>
          <select v-model="filters.enabled">
            <option v-for="option in enabledOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          <span>供应商等级</span>
          <select v-model="filters.supplierLevel">
            <option v-for="option in levelOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          <span>审核状态</span>
          <select v-model="filters.status">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">
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
        <div v-if="hasSuppliers" class="table-wrapper">
          <table class="data-table">
            <thead>
              <tr>
                <th scope="col">ID</th>
                <th scope="col">账号/企业</th>
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
                    <strong>{{ record.username }}</strong>
                    <span v-if="record.companyName" class="sub">{{ record.companyName }}</span>
                    <span v-if="record.businessLicense" class="sub">许可证：{{ record.businessLicense }}</span>
                  </div>
                </td>
                <td>
                  <div class="cell-main">
                    <span>{{ record.phone ?? '—' }}</span>
                    <span v-if="record.email" class="sub">{{ record.email }}</span>
                  </div>
                </td>
                <td>{{ supplierLevelLabel(record.supplierLevel) }}</td>
                <td>{{ supplierStatusLabel(record.status) }}</td>
                <td>
                  <span :class="['status-pill', record.enabled ? 'is-active' : 'is-disabled']">
                    {{ formatStatus(record) }}
                  </span>
                </td>
                <td class="actions">
                  <button type="button" class="link" @click="openEditDialog(record)">编辑</button>
                  <button type="button" class="link is-danger" @click="deleteSupplier(record.id)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="placeholder">暂无供应商数据，点击右上角新增供应商。</div>

        <nav class="pagination-footer" aria-label="分页导航">
          <div class="pagination-status">
            共 {{ total }} 条记录，当前第 {{ pagination.page + 1 }} 页
          </div>
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
                <span>账号</span>
                <input v-model="createForm.username" type="text" placeholder="请输入用户名" required />
              </label>
              <label>
                <span>登录密码</span>
                <input v-model="createForm.password" type="password" placeholder="至少 6 位字符" required />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>邮箱</span>
                <input v-model="createForm.email" type="email" placeholder="name@example.com" />
              </label>
              <label>
                <span>联系电话</span>
                <input v-model="createForm.phone" type="tel" placeholder="请输入联系方式" />
              </label>
            </div>
            <label>
              <span>联系地址</span>
              <input v-model="createForm.address" type="text" placeholder="如：上海市浦东新区" />
            </label>
            <div class="grid">
              <label>
                <span>公司名称</span>
                <input v-model="createForm.companyName" type="text" placeholder="请输入公司名" required />
              </label>
              <label>
                <span>营业执照号</span>
                <input v-model="createForm.businessLicense" type="text" placeholder="可选" />
              </label>
            </div>
            <label>
              <span>联系人</span>
              <input v-model="createForm.contactPerson" type="text" placeholder="请输入联系人" required />
            </label>
            <div class="grid">
              <label>
                <span>供应商等级</span>
                <select v-model="createForm.supplierLevel">
                  <option v-for="option in levelOptions.slice(1)" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>审核状态</span>
                <select v-model="createForm.status">
                  <option v-for="option in statusOptions.slice(1)" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>启用状态</span>
                <select v-model="createForm.enabled">
                  <option :value="true">启用</option>
                  <option :value="false">停用</option>
                </select>
              </label>
            </div>
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
                <span>账号</span>
                <input v-model="editForm.username" type="text" required />
              </label>
              <label>
                <span>邮箱</span>
                <input v-model="editForm.email" type="email" />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>联系电话</span>
                <input v-model="editForm.phone" type="tel" />
              </label>
              <label>
                <span>供应商等级</span>
                <select v-model="editForm.supplierLevel">
                  <option v-for="option in levelOptions.slice(1)" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>
            <label>
              <span>联系地址</span>
              <input v-model="editForm.address" type="text" />
            </label>
            <div class="grid">
              <label>
                <span>公司名称</span>
                <input v-model="editForm.companyName" type="text" required />
              </label>
              <label>
                <span>营业执照号</span>
                <input v-model="editForm.businessLicense" type="text" />
              </label>
            </div>
            <label>
              <span>联系人</span>
              <input v-model="editForm.contactPerson" type="text" required />
            </label>
            <div class="grid">
              <label>
                <span>审核状态</span>
                <select v-model="editForm.status">
                  <option v-for="option in statusOptions.slice(1)" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>启用状态</span>
                <select v-model="editForm.enabled">
                  <option :value="true">启用</option>
                  <option :value="false">停用</option>
                </select>
              </label>
            </div>
            <footer class="dialog__footer">
              <button type="button" @click="closeDialogs">取消</button>
              <button type="submit" class="primary" :disabled="submittingEdit">
                {{ submittingEdit ? '保存中…' : '保存修改' }}
              </button>
            </footer>
          </form>
        </div>
      </div>
    </teleport>
  </section>
</template>

<style scoped>
.management-shell {
  display: grid;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.8rem;
  border-radius: 20px;
  background: linear-gradient(120deg, rgba(45, 212, 191, 0.18), rgba(59, 130, 246, 0.12));
}

.page-header h1 {
  font-size: 1.8rem;
  margin-bottom: 0.35rem;
}

.page-header p {
  color: rgba(15, 23, 42, 0.65);
}

.page-header .primary {
  padding: 0.6rem 1.6rem;
  border-radius: 999px;
  border: none;
  background: linear-gradient(135deg, #0ea5e9, #22d3ee);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.filters {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 18px;
  padding: 1.4rem;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
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
  border: 1px solid rgba(148, 163, 184, 0.4);
  background: #fff;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.table-wrapper {
  border: 1px solid rgba(59, 130, 246, 0.18);
  border-radius: 18px;
  background: rgba(239, 246, 255, 0.85);
  overflow: hidden;
  overflow-x: auto;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 960px;
}

.data-table th,
.data-table td {
  padding: 0.9rem 1rem;
  text-align: left;
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
}

.data-table thead {
  background: rgba(96, 165, 250, 0.12);
}

.data-table tbody tr:hover {
  background: rgba(96, 165, 250, 0.08);
}

.cell-main {
  display: grid;
  gap: 0.3rem;
}

.cell-main .sub {
  color: rgba(71, 85, 105, 0.8);
  font-size: 0.9rem;
}

.status-pill {
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
}

.status-pill.is-active {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status-pill.is-disabled {
  background: rgba(248, 113, 113, 0.15);
  color: #b91c1c;
}

.actions {
  display: flex;
  gap: 0.75rem;
}

.actions .link {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
}

.actions .is-danger {
  color: #e11d48;
}

.placeholder {
  text-align: center;
  padding: 2.5rem;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.85);
  color: rgba(71, 85, 105, 0.9);
}

.placeholder.is-error {
  color: #b91c1c;
  background: rgba(254, 242, 242, 0.9);
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
  background: rgba(15, 23, 42, 0.35);
  display: grid;
  place-items: center;
  padding: 1.5rem;
  z-index: 1000;
}

.dialog {
  background: #fff;
  border-radius: 20px;
  width: min(720px, 100%);
  max-height: 90vh;
  overflow: auto;
  box-shadow: 0 40px 70px rgba(15, 23, 42, 0.18);
}

.dialog__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.2rem 1.6rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.dialog__content {
  display: grid;
  gap: 1rem;
  padding: 1.6rem;
}

.dialog__content label {
  display: grid;
  gap: 0.4rem;
}

.dialog__content input,
.dialog__content select {
  padding: 0.6rem 0.75rem;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.4);
}

.dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
  padding: 0 1.6rem 1.6rem;
}

.dialog__footer button {
  border: none;
  border-radius: 999px;
  padding: 0.55rem 1.4rem;
  cursor: pointer;
}

.dialog__footer .primary {
  background: linear-gradient(135deg, #0ea5e9, #22d3ee);
  color: #fff;
}

.close-btn {
  border: none;
  background: none;
  font-size: 1.8rem;
  line-height: 1;
  cursor: pointer;
}

.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}
</style>
