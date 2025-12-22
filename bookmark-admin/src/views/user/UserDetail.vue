<template>
  <div class="user-detail-wrapper">
    <div class="user-detail" v-if="user">
      <!-- 返回按钮 -->
      <div class="back-row">
        <router-link to="/users" class="back-link">← 返回用户列表</router-link>
      </div>

      <!-- 用户基本信息 -->
      <div class="card user-header-card">
        <div class="user-header">
          <div class="user-avatar-large">
            {{ user.username?.charAt(0)?.toUpperCase() || '?' }}
          </div>
          <div class="user-info">
            <div class="user-name-row">
              <h2 class="user-name">{{ user.username }}</h2>
              <span v-if="user.isAdmin === 1" class="badge badge-primary">管理员</span>
              <span :class="user.status === 1 ? 'badge badge-success' : 'badge badge-danger'">
                {{ user.status === 1 ? '正常' : '已禁用' }}
              </span>
            </div>
            <div class="user-meta">
              <span v-if="user.email">📧 {{ user.email }}</span>
              <span v-if="user.phone">📱 {{ user.phone }}</span>
              <span>📅 注册于 {{ formatDate(user.createTime) }}</span>
            </div>
          </div>
          <div class="user-actions">
            <button 
              class="btn" 
              :class="user.status === 1 ? 'btn-warning' : 'btn-success'"
              @click="toggleStatus"
            >
              {{ user.status === 1 ? '禁用用户' : '启用用户' }}
            </button>
            <button class="btn btn-secondary" @click="resetPassword">重置密码</button>
            <button 
              v-if="user.isAdmin !== 1" 
              class="btn btn-secondary"
              @click="setAdmin"
            >
              设为管理员
            </button>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-value">{{ userStats.bookmarkCount }}</div>
          <div class="stat-label">书签数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ userStats.categoryCount }}</div>
          <div class="stat-label">分类数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ userStats.tagCount }}</div>
          <div class="stat-label">标签数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ userStats.activeActivations }}</div>
          <div class="stat-label">有效激活</div>
        </div>
      </div>

      <!-- 详细信息标签页 -->
      <div class="card">
        <div class="tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.icon }} {{ tab.label }}
          </button>
        </div>

        <div class="tab-content">
          <!-- 用户书签 -->
          <div v-if="activeTab === 'bookmarks'" class="bookmarks-tab">
            <div class="tab-header">
              <span class="tab-count">共 {{ bookmarkTotal }} 条书签</span>
            </div>
            <div v-if="bookmarks.length > 0" class="table-container">
              <table class="table table-compact">
                <thead>
                  <tr>
                    <th style="width: 40px">#</th>
                    <th>标题</th>
                    <th>链接</th>
                    <th style="width: 140px">创建时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(bookmark, index) in bookmarks" :key="bookmark.id">
                    <td class="text-muted">{{ (bookmarkPage - 1) * bookmarkPageSize + index + 1 }}</td>
                    <td>
                      <div class="bookmark-title-cell">
                        <img :src="bookmark.iconUrl || '/favicon.ico'" class="bookmark-icon-sm" @error="handleIconError" />
                        <span class="bookmark-title-text">{{ bookmark.title || '无标题' }}</span>
                      </div>
                    </td>
                    <td>
                      <a :href="bookmark.url" target="_blank" class="bookmark-url-link">{{ truncateUrl(bookmark.url) }}</a>
                    </td>
                    <td class="text-muted">{{ formatDate(bookmark.createTime) }}</td>
                  </tr>
                </tbody>
              </table>
              <!-- 分页 -->
              <div class="mini-pagination">
                <button class="mini-page-btn" :disabled="bookmarkPage === 1" @click="changeBookmarkPage(1)">«</button>
                <button class="mini-page-btn" :disabled="bookmarkPage === 1" @click="changeBookmarkPage(bookmarkPage - 1)">‹</button>
                <span class="page-indicator">{{ bookmarkPage }} / {{ bookmarkTotalPages }} (共{{ bookmarkTotal }}条)</span>
                <button class="mini-page-btn" :disabled="bookmarkPage >= bookmarkTotalPages" @click="changeBookmarkPage(bookmarkPage + 1)">›</button>
                <button class="mini-page-btn" :disabled="bookmarkPage >= bookmarkTotalPages" @click="changeBookmarkPage(bookmarkTotalPages)">»</button>
              </div>
            </div>
            <div v-else class="empty-state">
              <div class="empty-state-icon">🔖</div>
              <div class="empty-state-text">暂无书签</div>
            </div>
          </div>

          <!-- 激活记录 -->
          <div v-if="activeTab === 'activations'" class="activations-tab">
            <div v-if="activations.length > 0" class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>激活码</th>
                    <th>额外书签数</th>
                    <th>额外分类数</th>
                    <th>过期时间</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="act in activations" :key="act.id">
                    <td>{{ act.code }}</td>
                    <td>+{{ act.extraBookmarks }}</td>
                    <td>+{{ act.extraCategories }}</td>
                    <td>{{ formatDate(act.expireTime) }}</td>
                    <td>
                      <span :class="isActivationValid(act) ? 'badge badge-success' : 'badge badge-danger'">
                        {{ getActivationStatusText(act) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-state">
              <div class="empty-state-icon">🎫</div>
              <div class="empty-state-text">暂无激活记录</div>
            </div>
          </div>

          <!-- 登录历史 -->
          <div v-if="activeTab === 'loginHistory'" class="login-history-tab">
            <div v-if="loginHistory.length > 0" class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>登录时间</th>
                    <th>IP地址</th>
                    <th>地点</th>
                    <th>设备</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="log in loginHistory" :key="log.id">
                    <td>{{ formatDate(log.createTime) }}</td>
                    <td>{{ log.ipAddress }}</td>
                    <td>{{ log.location || '-' }}</td>
                    <td>{{ log.device || '-' }}</td>
                    <td>
                      <span :class="log.status === 1 ? 'badge badge-success' : 'badge badge-danger'">
                        {{ log.status === 1 ? '成功' : '失败' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-state">
              <div class="empty-state-icon">📝</div>
              <div class="empty-state-text">暂无登录记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 用户不存在 -->
    <div v-else class="error-container">
      <div class="empty-state">
        <div class="empty-state-icon">❌</div>
        <div class="empty-state-text">用户不存在</div>
        <router-link to="/users" class="btn btn-primary mt-4">返回用户列表</router-link>
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
import { useRoute } from 'vue-router'
import { userApi } from '@/api/user'
import ConfirmModal from '@/components/ConfirmModal.vue'
import dayjs from 'dayjs'

const route = useRoute()

const loading = ref(true)
const user = ref(null)
const activeTab = ref('bookmarks')

const tabs = [
  { key: 'bookmarks', icon: '🔖', label: '用户书签' },
  { key: 'activations', icon: '🎫', label: '激活记录' },
  { key: 'loginHistory', icon: '📝', label: '登录历史' }
]

const userStats = reactive({
  bookmarkCount: 0,
  categoryCount: 0,
  tagCount: 0,
  activeActivations: 0
})

const bookmarks = ref([])
const bookmarkPage = ref(1)
const bookmarkPageSize = ref(10)
const bookmarkTotal = ref(0)
const bookmarkTotalPages = computed(() => Math.ceil(bookmarkTotal.value / bookmarkPageSize.value) || 1)
const activations = ref([])
const loginHistory = ref([])

function formatDate(date) {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function handleIconError(e) {
  e.target.src = '/favicon.ico'
}

function truncateUrl(url) {
  if (!url) return ''
  try {
    const urlObj = new URL(url)
    const path = urlObj.pathname.length > 30 ? urlObj.pathname.substring(0, 30) + '...' : urlObj.pathname
    return urlObj.host + path
  } catch {
    return url.length > 50 ? url.substring(0, 50) + '...' : url
  }
}

// 检查激活记录是否仍然有效
function isActivationValid(activation) {
  if (activation.status !== 1) return false
  if (!activation.expireTime) return true
  const expireDate = dayjs(activation.expireTime)
  return expireDate.isAfter(dayjs())
}

// 获取激活状态文本
function getActivationStatusText(activation) {
  if (activation.status !== 1) return '已禁用'
  if (!activation.expireTime) return '有效'
  const expireDate = dayjs(activation.expireTime)
  if (expireDate.isBefore(dayjs())) return '已过期'
  return '有效'
}

async function loadUserDetail() {
  loading.value = true
  try {
    const response = await userApi.getDetail(route.params.id)
    if (response.code === 200) {
      user.value = response.data.user || response.data
      userStats.bookmarkCount = response.data.bookmarkCount || 0
      userStats.categoryCount = response.data.categoryCount || 0
      userStats.tagCount = response.data.tagCount || 0
      userStats.activeActivations = response.data.activeActivations || 0
    }
  } catch (error) {
    console.error('Failed to load user detail:', error)
    // 使用模拟数据
    user.value = {
      id: route.params.id,
      username: 'testuser',
      nickname: '测试用户',
      email: 'test@example.com',
      phone: '13800138000',
      status: 1,
      isAdmin: 0,
      loginType: 1,
      createTime: dayjs().subtract(30, 'day').toISOString(),
      lastLoginTime: dayjs().subtract(1, 'hour').toISOString()
    }
    userStats.bookmarkCount = 156
    userStats.categoryCount = 8
    userStats.tagCount = 24
    userStats.activeActivations = 2
  } finally {
    loading.value = false
  }
  
  // 加载子数据
  loadBookmarks()
  loadActivations()
  loadLoginHistory()
}

async function loadBookmarks() {
  try {
    console.log('Fetching bookmarks: page=', bookmarkPage.value, 'size=', bookmarkPageSize.value)
    const response = await userApi.getBookmarks(route.params.id, { page: bookmarkPage.value, size: bookmarkPageSize.value })
    console.log('Bookmark response:', response)
    if (response.code === 200) {
      bookmarks.value = response.data.records || response.data.list || response.data || []
      bookmarkTotal.value = response.data.total || 0
      console.log('bookmarkTotal:', bookmarkTotal.value, 'bookmarkTotalPages:', bookmarkTotalPages.value)
    }
  } catch (error) {
    console.error('Load bookmarks error:', error)
    // 模拟数据
    bookmarks.value = [
      { id: 1, title: 'GitHub', url: 'https://github.com', iconUrl: '', createTime: dayjs().subtract(1, 'day').toISOString() },
      { id: 2, title: 'Google', url: 'https://google.com', iconUrl: '', createTime: dayjs().subtract(2, 'day').toISOString() }
    ]
    bookmarkTotal.value = 2
  }
}

function changeBookmarkPage(page) {
  if (page < 1 || page > bookmarkTotalPages.value) return
  bookmarkPage.value = page
  loadBookmarks()
}

async function loadActivations() {
  try {
    const response = await userApi.getActivations(route.params.id)
    if (response.code === 200) {
      activations.value = response.data || []
    }
  } catch (error) {
    activations.value = [
      { id: 1, code: 'ABC123', extraBookmarks: 100, extraCategories: 10, expireTime: dayjs().add(30, 'day').toISOString(), status: 1 }
    ]
  }
}

async function loadLoginHistory() {
  try {
    const response = await userApi.getLoginHistory(route.params.id, { page: 1, size: 20 })
    if (response.code === 200) {
      loginHistory.value = response.data.records || response.data || []
    }
  } catch (error) {
    // API 暂不可用，显示空状态
    console.warn('登录历史功能暂不可用')
    loginHistory.value = []
  }
}

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

function toggleStatus() {
  const newStatus = user.value.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'
  
  showConfirm({
    title: `${action}用户`,
    message: `确定要${action}该用户吗？${newStatus === 0 ? '禁用后该用户将无法登录系统。' : ''}`,
    type: newStatus === 0 ? 'warning' : 'success',
    confirmText: action,
    onConfirm: async () => {
      try {
        const response = await userApi.updateStatus(user.value.id, newStatus)
        if (response.code === 200) {
          user.value.status = newStatus
        }
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

function resetPassword() {
  showConfirm({
    title: '重置密码',
    message: '确定要重置该用户的密码吗？新密码将发送到用户邮箱。',
    type: 'warning',
    confirmText: '重置',
    onConfirm: async () => {
      try {
        await userApi.resetPassword(user.value.id)
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

function setAdmin() {
  showConfirm({
    title: '设置管理员',
    message: '确定要将该用户设为管理员吗？管理员拥有系统所有权限。',
    type: 'warning',
    confirmText: '确定',
    onConfirm: async () => {
      try {
        const response = await userApi.setAdmin(user.value.id, 1)
        if (response.code === 200) {
          user.value.isAdmin = 1
        }
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

onMounted(() => {
  loadUserDetail()
})
</script>

<style scoped>
.user-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.back-row {
  margin-bottom: 4px;
}

.back-link {
  color: var(--text-secondary);
  font-size: 14px;
}

.back-link:hover {
  color: var(--primary);
}

.user-header-card {
  padding: 24px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.user-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 14px;
}

.user-actions {
  display: flex;
  gap: 12px;
}

.btn-warning {
  background: var(--warning);
  color: white;
}

/* 统计行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-item {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
  box-shadow: var(--shadow);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 标签页 */
.tabs {
  display: flex;
  gap: 4px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-light);
}

.tab-btn {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border-radius: var(--radius);
  transition: var(--transition);
}

.tab-btn:hover {
  color: var(--text-primary);
  background: var(--bg-page);
}

.tab-btn.active {
  color: var(--primary);
  background: var(--primary-bg);
}

.tab-content {
  padding: 24px;
}

/* 标签页头部 */
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tab-count {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 紧凑表格 */
.table-compact {
  font-size: 13px;
}

.table-compact th,
.table-compact td {
  padding: 10px 12px;
}

.text-muted {
  color: var(--text-muted);
}

/* 书签标题单元格 */
.bookmark-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bookmark-icon-sm {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  flex-shrink: 0;
}

.bookmark-title-text {
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 250px;
}

.bookmark-url-link {
  color: var(--text-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  max-width: 280px;
}

.bookmark-url-link:hover {
  color: var(--primary);
}

/* 迷你分页 */
.mini-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 0;
  border-top: 1px solid var(--border-light);
  margin-top: 12px;
}

.mini-page-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.mini-page-btn:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.mini-page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-indicator {
  font-size: 13px;
  color: var(--text-secondary);
  min-width: 60px;
  text-align: center;
}

/* 加载/错误状态 */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}
</style>
