<template>
  <div class="announcement-manager">
    <!-- 操作栏 -->
    <div class="card action-card">
      <div class="action-row">
        <div class="filter-group">
          <h3 class="card-title">📢 公告管理</h3>
          <select v-model="statusFilter" class="input status-select" @change="loadAnnouncements">
            <option value="">全部状态</option>
            <option value="1">已发布</option>
            <option value="0">草稿</option>
          </select>
          <select v-model="typeFilter" class="input type-select" @change="loadAnnouncements">
            <option value="">全部类型</option>
            <option value="info">普通公告</option>
            <option value="warning">重要通知</option>
            <option value="maintenance">系统维护</option>
            <option value="update">更新通知</option>
          </select>
        </div>
        <div class="action-buttons">
          <button class="btn btn-primary" @click="openCreateModal">
            ➕ 发布公告
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ announcements.length }}</div>
          <div class="stat-label">全部公告</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-info">
          <div class="stat-value">{{ publishedCount }}</div>
          <div class="stat-label">已发布</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ draftCount }}</div>
          <div class="stat-label">草稿</div>
        </div>
      </div>
    </div>

    <!-- 公告卡片列表 -->
    <div class="announcements-grid" v-if="filteredAnnouncements.length > 0">
      <div 
        v-for="item in filteredAnnouncements" 
        :key="item.id" 
        class="announcement-card"
        :class="{ 'is-draft': item.status === 0 }"
      >
        <div class="card-header-row">
          <span :class="getTypeBadgeClass(item.type)">{{ getTypeName(item.type) }}</span>
          <span :class="item.status === 1 ? 'status-badge published' : 'status-badge draft'">
            {{ item.status === 1 ? '已发布' : '草稿' }}
          </span>
        </div>
        <h4 class="card-title-text">{{ item.title }}</h4>
        <p class="card-content-preview">{{ truncateContent(item.content) }}</p>
        <div class="card-meta">
          <span class="meta-item">🕐 {{ formatDate(item.createTime) }}</span>
          <span class="meta-item" v-if="item.expireTime">⏰ 过期: {{ formatDate(item.expireTime) }}</span>
        </div>
        <div class="card-actions">
          <button class="btn btn-sm btn-secondary" @click="previewAnnouncement(item)">
            👁️ 预览
          </button>
          <button class="btn btn-sm btn-secondary" @click="editAnnouncement(item)">
            ✏️ 编辑
          </button>
          <button 
            v-if="item.status === 0"
            class="btn btn-sm btn-primary" 
            @click="publishAnnouncement(item)"
          >
            🚀 发布
          </button>
          <button 
            v-else
            class="btn btn-sm btn-warning" 
            @click="unpublishAnnouncement(item)"
          >
            📥 撤回
          </button>
          <button class="btn btn-sm btn-danger" @click="deleteAnnouncement(item)">
            🗑️ 删除
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading"><div class="spinner"></div></div>

    <div v-if="!loading && filteredAnnouncements.length === 0" class="empty-state">
      <div class="empty-state-icon">📢</div>
      <div class="empty-state-text">暂无公告</div>
      <button class="btn btn-primary" @click="openCreateModal">发布第一条公告</button>
    </div>

    <!-- 创建/编辑公告弹窗 -->
    <div class="modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
      <div class="modal modal-large">
        <div class="modal-header">
          <h3 class="modal-title">{{ isEditing ? '编辑公告' : '发布公告' }}</h3>
          <button class="modal-close" @click="showCreateModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-col">
              <div class="input-group mb-4">
                <label class="input-label">公告标题 *</label>
                <input type="text" v-model="createForm.title" class="input" placeholder="请输入标题" />
              </div>
              <div class="input-group mb-4">
                <label class="input-label">公告类型</label>
                <select v-model="createForm.type" class="input">
                  <option value="info">📋 普通公告</option>
                  <option value="warning">⚠️ 重要通知</option>
                  <option value="maintenance">🔧 系统维护</option>
                  <option value="update">🆕 更新通知</option>
                </select>
              </div>
              <div class="input-group mb-4">
                <label class="input-label">过期时间（可选）</label>
                <input type="datetime-local" v-model="createForm.expireTime" class="input" />
              </div>
            </div>
          </div>
          <div class="input-group mb-4">
            <label class="input-label">公告内容 *</label>
            <textarea 
              v-model="createForm.content" 
              class="input textarea" 
              placeholder="请输入公告内容，支持换行..."
              rows="8"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showCreateModal = false">取消</button>
          <button class="btn btn-secondary" @click="previewFromForm">👁️ 预览</button>
          <button class="btn btn-secondary" @click="saveDraft">💾 保存草稿</button>
          <button class="btn btn-primary" @click="saveAndPublish">🚀 立即发布</button>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <div class="modal-overlay" v-if="showPreviewModal" @click.self="showPreviewModal = false">
      <div class="modal modal-large">
        <div class="modal-header">
          <h3 class="modal-title">公告预览</h3>
          <button class="modal-close" @click="showPreviewModal = false">✕</button>
        </div>
        <div class="modal-body preview-body">
          <div class="preview-header">
            <span :class="getTypeBadgeClass(previewData.type)">{{ getTypeName(previewData.type) }}</span>
            <h2 class="preview-title">{{ previewData.title }}</h2>
          </div>
          <div class="preview-content">{{ previewData.content }}</div>
          <div class="preview-meta" v-if="previewData.expireTime">
            ⏰ 过期时间: {{ formatDate(previewData.expireTime) }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showPreviewModal = false">关闭</button>
        </div>
      </div>
    </div>

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
import { announcementApi } from '@/api/system'
import ConfirmModal from '@/components/ConfirmModal.vue'
import dayjs from 'dayjs'

const announcements = ref([])
const loading = ref(false)
const showCreateModal = ref(false)
const showPreviewModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const statusFilter = ref('')
const typeFilter = ref('')

const previewData = reactive({
  title: '',
  content: '',
  type: 'info',
  expireTime: ''
})

const createForm = reactive({
  title: '',
  content: '',
  type: 'info',
  expireTime: ''
})

const confirmModal = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'warning',
  confirmText: '确定',
  onConfirm: () => {}
})

// 计算属性
const publishedCount = computed(() => announcements.value.filter(a => a.status === 1).length)
const draftCount = computed(() => announcements.value.filter(a => a.status === 0).length)

const filteredAnnouncements = computed(() => {
  let result = announcements.value
  if (statusFilter.value !== '') {
    result = result.filter(a => a.status === parseInt(statusFilter.value))
  }
  if (typeFilter.value) {
    result = result.filter(a => a.type === typeFilter.value)
  }
  return result
})

// 辅助函数
function formatDate(date) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

function truncateContent(content, maxLength = 100) {
  if (!content) return ''
  return content.length > maxLength ? content.substring(0, maxLength) + '...' : content
}

function getTypeName(type) {
  const types = { 'info': '普通公告', 'warning': '重要通知', 'maintenance': '系统维护', 'update': '更新通知' }
  return types[type] || '未知'
}

function getTypeBadgeClass(type) {
  const classes = { 
    'info': 'type-badge info', 
    'warning': 'type-badge warning', 
    'maintenance': 'type-badge maintenance', 
    'update': 'type-badge update' 
  }
  return classes[type] || 'type-badge'
}

// API 操作
async function loadAnnouncements() {
  loading.value = true
  try {
    const response = await announcementApi.getList({ page: 1, size: 100 })
    if (response.code === 200) {
      announcements.value = response.data.records || []
    }
  } catch (error) {
    console.error('Failed to load announcements:', error)
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  isEditing.value = false
  editingId.value = null
  resetForm()
  showCreateModal.value = true
}

function editAnnouncement(item) {
  isEditing.value = true
  editingId.value = item.id
  createForm.title = item.title
  createForm.content = item.content
  createForm.type = item.type
  createForm.expireTime = item.expireTime ? dayjs(item.expireTime).format('YYYY-MM-DDTHH:mm') : ''
  showCreateModal.value = true
}

function resetForm() {
  createForm.title = ''
  createForm.content = ''
  createForm.type = 'info'
  createForm.expireTime = ''
}

function previewAnnouncement(item) {
  previewData.title = item.title
  previewData.content = item.content
  previewData.type = item.type
  previewData.expireTime = item.expireTime
  showPreviewModal.value = true
}

function previewFromForm() {
  previewData.title = createForm.title || '(无标题)'
  previewData.content = createForm.content || '(无内容)'
  previewData.type = createForm.type
  previewData.expireTime = createForm.expireTime
  showPreviewModal.value = true
}

async function saveDraft() {
  if (!createForm.title || !createForm.content) {
    showMessage('提示', '请填写标题和内容', 'warning')
    return
  }
  
  try {
    if (isEditing.value) {
      await announcementApi.update(editingId.value, { ...createForm, status: 0 })
    } else {
      await announcementApi.create({ ...createForm, status: 0, expireTime: createForm.expireTime || null })
    }
    showCreateModal.value = false
    resetForm()
    loadAnnouncements()
    showMessage('保存成功', '公告已保存为草稿', 'success')
  } catch (error) {
    showMessage('保存失败', '无法保存公告', 'danger')
  }
}

async function saveAndPublish() {
  if (!createForm.title || !createForm.content) {
    showMessage('提示', '请填写标题和内容', 'warning')
    return
  }
  
  try {
    let announcementId
    if (isEditing.value) {
      await announcementApi.update(editingId.value, { ...createForm, status: 0 })
      announcementId = editingId.value
    } else {
      const response = await announcementApi.create({ ...createForm, status: 0, expireTime: createForm.expireTime || null })
      announcementId = response.data?.id || response.data
    }
    
    if (announcementId) {
      await announcementApi.publish(announcementId)
    }
    
    showCreateModal.value = false
    resetForm()
    loadAnnouncements()
    showMessage('发布成功', '公告已发布', 'success')
  } catch (error) {
    console.error('Publish error:', error)
    showMessage('发布失败', '无法发布公告', 'danger')
  }
}

async function publishAnnouncement(item) {
  try {
    await announcementApi.publish(item.id)
    item.status = 1
    showMessage('发布成功', '公告已发布', 'success')
  } catch (error) {
    showMessage('发布失败', '无法发布公告', 'danger')
  }
}

async function unpublishAnnouncement(item) {
  try {
    await announcementApi.update(item.id, { status: 0 })
    item.status = 0
    showMessage('撤回成功', '公告已撤回为草稿', 'success')
  } catch (error) {
    showMessage('撤回失败', '无法撤回公告', 'danger')
  }
}

function deleteAnnouncement(item) {
  confirmModal.title = '删除公告'
  confirmModal.message = `确定要删除公告「${item.title}」吗？此操作不可恢复。`
  confirmModal.type = 'danger'
  confirmModal.confirmText = '删除'
  confirmModal.onConfirm = async () => {
    try {
      await announcementApi.delete(item.id)
      loadAnnouncements()
    } catch (error) {
      showMessage('删除失败', '无法删除公告', 'danger')
    }
  }
  confirmModal.visible = true
}

function showMessage(title, message, type = 'info') {
  confirmModal.title = title
  confirmModal.message = message
  confirmModal.type = type
  confirmModal.confirmText = '知道了'
  confirmModal.onConfirm = () => {}
  confirmModal.visible = true
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.announcement-manager { 
  display: flex; 
  flex-direction: column; 
  gap: 20px; 
}

.action-card { padding: 20px; }

.action-row { 
  display: flex; 
  align-items: center; 
  justify-content: space-between;
  gap: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-title {
  margin: 0;
  font-size: 18px;
}

.status-select, .type-select {
  width: 120px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.stat-icon {
  font-size: 32px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 公告卡片网格 */
.announcements-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.announcement-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  border-left: 4px solid var(--primary);
}

.announcement-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.announcement-card.is-draft {
  border-left-color: #9ca3af;
  opacity: 0.85;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.type-badge {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.type-badge.info { background: #dbeafe; color: #1d4ed8; }
.type-badge.warning { background: #fef3c7; color: #b45309; }
.type-badge.maintenance { background: #fecaca; color: #b91c1c; }
.type-badge.update { background: #d1fae5; color: #047857; }

.status-badge {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
}

.status-badge.published { background: #d1fae5; color: #047857; }
.status-badge.draft { background: #f3f4f6; color: #6b7280; }

.card-title-text {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.card-content-preview {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--text-muted);
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

/* 弹窗 */
.modal-large {
  max-width: 700px;
  width: 90%;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-col {
  flex: 1;
}

.textarea { 
  resize: vertical; 
  min-height: 150px;
  font-family: inherit;
  line-height: 1.6;
}

.mb-4 { margin-bottom: 16px; }

.input-label { 
  font-size: 13px; 
  font-weight: 500; 
  color: var(--text-secondary); 
  display: block; 
  margin-bottom: 8px; 
}

/* 预览 */
.preview-body {
  padding: 24px;
}

.preview-header {
  margin-bottom: 20px;
}

.preview-title {
  font-size: 24px;
  font-weight: 700;
  margin: 12px 0 0 0;
}

.preview-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
  white-space: pre-wrap;
}

.preview-meta {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
  font-size: 13px;
  color: var(--text-secondary);
}

.btn-warning {
  background: var(--warning);
  color: white;
}

.btn-warning:hover {
  background: #d97706;
}
</style>
