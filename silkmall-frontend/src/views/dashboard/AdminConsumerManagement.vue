<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/services/api'

interface ConsumerRecord {
  id: number
  username: string
  email: string | null
  phone: string | null
  address: string | null
  realName: string | null
  idCard: string | null
  avatar: string | null
  points: number | null
  membershipLevel: number | null
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
const consumers = ref<ConsumerRecord[]>([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  enabled: 'all' as 'all' | 'true' | 'false',
  membershipLevel: 'all' as 'all' | number,
})

const createDialogOpen = ref(false)
const editDialogOpen = ref(false)

const createForm = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  address: '',
  realName: '',
  idCard: '',
  avatar: '',
  points: 0,
  membershipLevel: 1,
  enabled: true,
})

const editForm = reactive({
  id: 0,
  username: '',
  email: '',
  phone: '',
  address: '',
  realName: '',
  idCard: '',
  avatar: '',
  points: 0,
  membershipLevel: 1,
  enabled: true as boolean | null,
})

const submittingCreate = ref(false)
const submittingEdit = ref(false)

const levelOptions = [
  { label: '全部等级', value: 'all' },
  { label: '普通会员', value: 1 },
  { label: '进阶会员', value: 2 },
  { label: '高级会员', value: 3 },
  { label: '旗舰会员', value: 4 },
  { label: '尊享会员', value: 5 },
]

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '启用', value: 'true' },
  { label: '停用', value: 'false' },
]

const hasConsumers = computed(() => consumers.value.length > 0)

async function loadConsumers() {
  loading.value = true
  error.value = null
  try {
    const params: Record<string, unknown> = {
      page: 0,
      size: 1000,
    }
    if (filters.keyword.trim()) {
      params.keyword = filters.keyword.trim()
    }
    if (filters.enabled !== 'all') {
      params.enabled = filters.enabled === 'true'
    }
    if (filters.membershipLevel !== 'all') {
      params.membershipLevel = filters.membershipLevel
    }

    const { data } = await api.get<PageResponse<ConsumerRecord>>('/consumers', { params })
    consumers.value = data.content ?? []
    total.value = data.totalElements ?? 0
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载消费者数据失败'
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
  createForm.realName = ''
  createForm.idCard = ''
  createForm.avatar = ''
  createForm.points = 0
  createForm.membershipLevel = 1
  createForm.enabled = true
}

function openEditDialog(record: ConsumerRecord) {
  editForm.id = record.id
  editForm.username = record.username
  editForm.email = record.email ?? ''
  editForm.phone = record.phone ?? ''
  editForm.address = record.address ?? ''
  editForm.realName = record.realName ?? ''
  editForm.idCard = record.idCard ?? ''
  editForm.avatar = record.avatar ?? ''
  editForm.points = record.points ?? 0
  editForm.membershipLevel = record.membershipLevel ?? 1
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
    await api.post('/consumers', {
      username: createForm.username.trim(),
      password: createForm.password,
      email: createForm.email.trim() || null,
      phone: createForm.phone.trim() || null,
      address: createForm.address.trim() || null,
      realName: createForm.realName.trim(),
      idCard: createForm.idCard.trim(),
      avatar: createForm.avatar.trim() || null,
      points: Number.isFinite(createForm.points) ? createForm.points : 0,
      membershipLevel: Number.isFinite(createForm.membershipLevel) ? createForm.membershipLevel : 1,
      enabled: createForm.enabled,
    })
    closeDialogs()
    await loadConsumers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建消费者失败'
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
      realName: editForm.realName.trim(),
      idCard: editForm.idCard.trim(),
      avatar: editForm.avatar.trim() || null,
      points: Number.isFinite(editForm.points) ? editForm.points : 0,
      membershipLevel: Number.isFinite(editForm.membershipLevel) ? editForm.membershipLevel : 1,
      enabled: editForm.enabled,
    }
    await api.put(`/consumers/${editForm.id}`, payload)
    closeDialogs()
    await loadConsumers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新消费者失败'
  } finally {
    submittingEdit.value = false
  }
}

async function deleteConsumer(id: number) {
  if (!window.confirm('确定要删除该消费者吗？此操作无法撤销。')) return
  try {
    await api.delete(`/consumers/${id}`)
    await loadConsumers()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除消费者失败'
  }
}

function membershipLabel(level: number | null) {
  if (!level) return '普通会员'
  const option = levelOptions.find((item) => item.value === level)
  return option ? option.label : `等级 ${level}`
}

watch(
  () => ({ ...filters }),
  () => {
    loadConsumers()
  }
)

onMounted(() => {
  loadConsumers()
})

function formatStatus(record: ConsumerRecord) {
  return record.enabled ? '启用' : '停用'
}

function formatPoints(points: number | null) {
  if (typeof points !== 'number' || Number.isNaN(points)) return 0
  return points
}
</script>

<template>
  <section class="management-shell">
    <header class="page-header">
      <div>
        <h1>采购/消费者管理</h1>
        <p>集中维护企业采购账号，支持快速创建、更新与停用消费者资料。</p>
      </div>
      <button type="button" class="primary" @click="openCreateDialog">新增消费者</button>
    </header>

    <section class="filters" aria-label="筛选器">
      <div class="filter-group">
        <label>
          <span>关键词</span>
          <input v-model="filters.keyword" type="search" placeholder="按名称、邮箱或电话搜索" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.enabled">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label>
          <span>会员等级</span>
          <select v-model="filters.membershipLevel">
            <option v-for="option in levelOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
      </div>
    </section>

    <div v-if="loading" class="placeholder">正在加载消费者数据…</div>
    <div v-else-if="error" class="placeholder is-error">{{ error }}</div>
    <template v-else>
      <section class="panel">
        <div v-if="hasConsumers" class="table-wrapper scrollable-table">
          <table class="data-table">
            <thead>
              <tr>
                <th scope="col">ID</th>
              <th scope="col">账号</th>
              <th scope="col">联系方式</th>
              <th scope="col">积分</th>
              <th scope="col">会员等级</th>
              <th scope="col">状态</th>
              <th scope="col" class="actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in consumers" :key="record.id">
              <td>{{ record.id }}</td>
              <td>
                <div class="cell-main">
                  <strong>{{ record.username }}</strong>
                  <span v-if="record.email" class="sub">{{ record.email }}</span>
                  <span v-if="record.address" class="sub">{{ record.address }}</span>
                </div>
              </td>
              <td>
                <div class="cell-main">
                  <span>{{ record.phone ?? '—' }}</span>
                  <span v-if="record.realName" class="sub">{{ record.realName }}</span>
                </div>
              </td>
              <td>{{ formatPoints(record.points) }}</td>
              <td>{{ membershipLabel(record.membershipLevel) }}</td>
              <td>
                <span :class="['status-pill', record.enabled ? 'is-active' : 'is-disabled']">
                  {{ formatStatus(record) }}
                </span>
              </td>
              <td class="actions">
                <button type="button" class="link" @click="openEditDialog(record)">编辑</button>
                <button type="button" class="link is-danger" @click="deleteConsumer(record.id)">删除</button>
              </td>
            </tr>
          </tbody>
          </table>
        </div>
        <p v-else class="empty">暂无消费者数据，点击右上角按钮新增采购账号。</p>
      </section>
    </template>

    <teleport to="body">
      <div v-if="createDialogOpen" class="dialog-backdrop" role="dialog" aria-modal="true">
        <div class="dialog">
          <header class="dialog__header">
            <h2>新增消费者</h2>
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
              <span>收货地址</span>
              <input v-model="createForm.address" type="text" placeholder="如：上海市浦东新区" />
            </label>
            <div class="grid">
              <label>
                <span>真实姓名</span>
                <input v-model="createForm.realName" type="text" placeholder="请输入真实姓名" required />
              </label>
              <label>
                <span>身份证号</span>
                <input v-model="createForm.idCard" type="text" placeholder="请输入身份证号码" required />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>积分</span>
                <input v-model.number="createForm.points" type="number" min="0" />
              </label>
              <label>
                <span>会员等级</span>
                <select v-model.number="createForm.membershipLevel">
                  <option v-for="option in levelOptions.slice(1)" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <label>
                <span>状态</span>
                <select v-model="createForm.enabled">
                  <option :value="true">启用</option>
                  <option :value="false">停用</option>
                </select>
              </label>
            </div>
            <label>
              <span>头像地址</span>
              <input v-model="createForm.avatar" type="url" placeholder="可选，头像图片链接" />
            </label>
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
            <h2>编辑消费者</h2>
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
                <span>会员等级</span>
                <select v-model.number="editForm.membershipLevel">
                  <option v-for="option in levelOptions.slice(1)" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>
            <label>
              <span>收货地址</span>
              <input v-model="editForm.address" type="text" />
            </label>
            <div class="grid">
              <label>
                <span>真实姓名</span>
                <input v-model="editForm.realName" type="text" required />
              </label>
              <label>
                <span>身份证号</span>
                <input v-model="editForm.idCard" type="text" required />
              </label>
            </div>
            <div class="grid">
              <label>
                <span>积分</span>
                <input v-model.number="editForm.points" type="number" min="0" />
              </label>
              <label>
                <span>状态</span>
                <select v-model="editForm.enabled">
                  <option :value="true">启用</option>
                  <option :value="false">停用</option>
                </select>
              </label>
            </div>
            <label>
              <span>头像地址</span>
              <input v-model="editForm.avatar" type="url" />
            </label>
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
  background: linear-gradient(120deg, rgba(96, 165, 250, 0.16), rgba(56, 189, 248, 0.12));
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
  background: linear-gradient(135deg, #2563eb, #38bdf8);
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

.table-wrapper.scrollable-table {
  --consumer-visible-rows: 3;
  --consumer-row-height: 5rem;
  --consumer-header-height: 3.2rem;
  max-height: calc(
    var(--consumer-visible-rows) * var(--consumer-row-height) +
      var(--consumer-header-height)
  );
  overflow-y: auto;
  overscroll-behavior: contain;
}

.table-wrapper.scrollable-table::-webkit-scrollbar {
  width: 6px;
}

.table-wrapper.scrollable-table::-webkit-scrollbar-thumb {
  background: rgba(59, 130, 246, 0.35);
  border-radius: 999px;
}

.table-wrapper.scrollable-table::-webkit-scrollbar-track {
  background: transparent;
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
  gap: 0.2rem;
}

.cell-main .sub {
  font-size: 0.8rem;
  color: rgba(71, 85, 105, 0.7);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
}

.status-pill.is-active {
  background: rgba(34, 197, 94, 0.16);
  color: #166534;
}

.status-pill.is-disabled {
  background: rgba(248, 113, 113, 0.16);
  color: #991b1b;
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
  background: linear-gradient(135deg, #2563eb, #38bdf8);
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
