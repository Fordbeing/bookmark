<template>
  <div class="user-list">
    <!-- 搜索和筛选 (同一行) -->
    <div class="card filter-card">
      <div class="filter-row">
        <input
          type="text"
          v-model="searchQuery"
          class="input search-input"
          placeholder="搜索用户名、邮箱、手机号..."
          @keyup.enter="handleSearch"
        />
        <button class="btn btn-primary" @click="handleSearch">搜索</button>
        <select v-model="filters.status" class="input select-input" @change="handleFilter">
          <option value="">全部状态</option>
          <option value="1">正常</option>
          <option value="0">已禁用</option>
        </select>
        <select v-model="filters.loginType" class="input select-input" @change="handleFilter">
          <option value="">登录方式</option>
          <option value="1">邮箱</option>
          <option value="2">微信</option>
          <option value="3">手机</option>
        </select>
        <select v-model="filters.isAdmin" class="input select-input" @change="handleFilter">
          <option value="">全部用户</option>
          <option value="1">管理员</option>
          <option value="0">普通用户</option>
        </select>
      </div>
    </div>

    <!-- 用户表格 -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">用户列表</h3>
        <div class="card-actions">
          <span class="total-count">共 {{ total }} 条记录</span>
        </div>
      </div>
      <div class="card-body">
        <div class="table-container">
          <table class="table" v-if="!loading && users.length > 0">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户信息</th>
                <th>联系方式</th>
                <th>登录方式</th>
                <th>状态</th>
                <th>注册时间</th>
                <th>最后登录</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>
                  <div class="user-info-cell">
                    <div class="user-avatar">{{ user.username?.charAt(0)?.toUpperCase() || '?' }}</div>
                    <div class="user-details">
                      <div class="user-name">{{ user.username }}</div>
                      <div class="user-nickname">{{ user.nickname || '-' }}</div>
                    </div>
                    <span v-if="user.isAdmin === 1" class="badge badge-primary">管理员</span>
                  </div>
                </td>
                <td>
                  <div class="contact-cell">
                    <div v-if="user.email">📧 {{ user.email }}</div>
                    <div v-if="user.phone">📱 {{ user.phone }}</div>
                  </div>
                </td>
                <td>
                  <span class="badge" :class="getLoginTypeBadge(user.loginType)">
                    {{ getLoginTypeText(user.loginType) }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="user.status === 1 ? 'badge-success' : 'badge-danger'">
                    {{ user.status === 1 ? '正常' : '已禁用' }}
                  </span>
                </td>
                <td>{{ formatDate(user.createTime) }}</td>
                <td>{{ formatDate(user.lastLoginTime) || '-' }}</td>
                <td>
                  <div class="action-buttons">
                    <router-link :to="`/users/${user.id}`" class="btn btn-sm btn-secondary">
                      详情
                    </router-link>
                    <button
                      class="btn btn-sm"
                      :class="user.status === 1 ? 'btn-warning' : 'btn-success'"
                      @click="toggleUserStatus(user)"
                    >
                      {{ user.status === 1 ? '禁用' : '启用' }}
                    </button>
                    <button
                      v-if="user.isAdmin !== 1"
                      class="btn btn-sm btn-secondary"
                      @click="setAdmin(user)"
                    >
                      设为管理员
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="loading" class="loading">
            <div class="spinner"></div>
          </div>

          <div v-if="!loading && users.length === 0" class="empty-state">
            <div class="empty-state-icon">👥</div>
            <div class="empty-state-text">暂无用户数据</div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <div class="page-size-selector">
            <span>每页</span>
            <select v-model="pageSize" class="input page-size-select" @change="handlePageSizeChange">
              <option :value="10">10</option>
              <option :value="15">15</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
            </select>
            <span>条</span>
          </div>
          
          <div class="pagination">
            <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(1)">首页</button>
            <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">上一页</button>
            <button
              v-for="page in visiblePages"
              :key="page"
              class="page-btn"
              :class="{ active: page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
            <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">下一页</button>
            <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(totalPages)">末页</button>
          </div>
          
          <div class="page-info">
            第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条
          </div>
        </div>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <ConfirmModal
      v-model:visible="confirmModal.visible"
      :title="confirmModal.title"
      :message="confirmModal.message"
      :type="confirmModal.type"
      :confirm-text="confirmModal.confirmText"
      @confirm="confirmModal.onConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { userApi } from '@/api/user'
import ConfirmModal from '@/components/ConfirmModal.vue'
import dayjs from 'dayjs'

const users = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

const filters = reactive({
  status: '',
  loginType: '',
  isAdmin: ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const visiblePages = computed(() => {
  const pages = []
  const maxVisible = 5
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2))
  let end = Math.min(totalPages.value, start + maxVisible - 1)
  
  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1)
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

function getLoginTypeText(type) {
  const types = { 1: '邮箱', 2: '微信', 3: '手机' }
  return types[type] || '未知'
}

function getLoginTypeBadge(type) {
  const badges = { 1: 'badge-info', 2: 'badge-success', 3: 'badge-warning' }
  return badges[type] || 'badge-default'
}

function formatDate(date) {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

async function loadUsers() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined,
      status: filters.status || undefined,
      loginType: filters.loginType || undefined,
      isAdmin: filters.isAdmin || undefined
    }
    
    const response = await userApi.getList(params)
    if (response.code === 200) {
      users.value = response.data.records || response.data.list || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load users:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadUsers()
}

function handleFilter() {
  currentPage.value = 1
  loadUsers()
}

function handlePageSizeChange() {
  currentPage.value = 1
  loadUsers()
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadUsers()
}

// 确认弹窗状态
const confirmModal = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'warning',
  confirmText: '确定',
  onConfirm: () => {}
})

function showConfirm(options) {
  Object.assign(confirmModal, {
    visible: true,
    title: options.title || '确认操作',
    message: options.message || '',
    type: options.type || 'warning',
    confirmText: options.confirmText || '确定',
    onConfirm: options.onConfirm || (() => {})
  })
}

function toggleUserStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  
  showConfirm({
    title: `${action}用户`,
    message: `确定要${action}用户 「${user.username}」 吗？${newStatus === 0 ? '禁用后该用户将无法登录系统。' : ''}`,
    type: newStatus === 0 ? 'warning' : 'success',
    confirmText: action,
    onConfirm: async () => {
      try {
        const response = await userApi.updateStatus(user.id, newStatus)
        if (response.code === 200) {
          user.status = newStatus
        }
      } catch (error) {
        console.error('Failed to update user status:', error)
      }
    }
  })
}

function setAdmin(user) {
  showConfirm({
    title: '设置管理员',
    message: `确定要将用户 「${user.username}」 设为管理员吗？管理员拥有系统所有权限。`,
    type: 'warning',
    confirmText: '确定',
    onConfirm: async () => {
      try {
        const response = await userApi.setAdmin(user.id, 1)
        if (response.code === 200) {
          user.isAdmin = 1
        }
      } catch (error) {
        console.error('Failed to set admin:', error)
      }
    }
  })
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-card {
  padding: 16px 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  flex: 0 1 250px;
  min-width: 180px;
}

.select-input {
  flex: 0 0 auto;
  width: auto !important;
  min-width: 120px;
}

.total-count {
  font-size: 13px;
  color: var(--text-secondary);
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.user-details {
  min-width: 0;
}

.user-name {
  font-weight: 600;
  color: var(--text-primary);
}

.user-nickname {
  font-size: 12px;
  color: var(--text-muted);
}

.contact-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-warning {
  background: var(--warning);
  color: white;
}

.btn-warning:hover {
  background: #d97706;
}

/* 分页容器 */
.pagination-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  border-top: 1px solid var(--border-light);
  margin-top: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.page-size-select {
  width: 65px;
  padding: 6px 8px;
}

.pagination {
  display: flex;
  gap: 4px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: var(--radius);
  cursor: pointer;
  transition: var(--transition);
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 响应式 */
@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input {
    max-width: none;
  }
  
  .pagination-container {
    flex-direction: column;
    align-items: center;
  }
}
</style>
