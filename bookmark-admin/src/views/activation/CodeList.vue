<template>
  <div class="activation-codes">
    <!-- 操作栏 -->
    <div class="card action-card">
      <div class="action-row">
        <div class="filter-group">
          <input
            type="text"
            v-model="searchQuery"
            class="input search-input"
            placeholder="搜索激活码..."
            @keyup.enter="loadCodes"
          />
          <select v-model="statusFilter" class="input status-select" @change="handleFilterChange">
            <option value="">全部状态</option>
            <option :value="1">有效</option>
            <option :value="0">已禁用</option>
          </select>
          <button class="btn btn-secondary" @click="loadCodes">
            🔍 搜索
          </button>
        </div>
        <div class="action-buttons">
          <button class="btn btn-primary" @click="showCreateModal = true">
            ➕ 创建激活码
          </button>
          <button class="btn btn-secondary" @click="showBatchModal = true">
            📋 批量创建
          </button>
        </div>
      </div>
    </div>

    <!-- 激活码列表 -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">激活码列表</h3>
        <span class="text-secondary text-sm">共 {{ total }} 条</span>
      </div>
      <div class="card-body">
        <div class="table-container">
          <table class="table" v-if="codes.length > 0">
            <thead>
              <tr>
                <th>激活码</th>
                <th>额外书签</th>
                <th>额外分类</th>
                <th>有效期(天)</th>
                <th>使用情况</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="code in codes" :key="code.id">
                <td>
                  <div class="code-cell">
                    <code class="code-value">{{ code.code }}</code>
                    <button class="copy-btn" @click="copyCode(code.code)" title="复制">📋</button>
                  </div>
                </td>
                <td class="text-success font-semibold">+{{ code.extraBookmarks }}</td>
                <td class="text-success font-semibold">+{{ code.extraCategories }}</td>
                <td>{{ code.expireDays }} 天</td>
                <td>
                  <div class="usage-cell">
                    <div class="usage-bar">
                      <div 
                        class="usage-fill" 
                        :style="{ width: (code.usedCount / code.maxUses * 100) + '%' }"
                      ></div>
                    </div>
                    <span class="usage-text">{{ code.usedCount }} / {{ code.maxUses }}</span>
                  </div>
                </td>
                <td>
                  <span :class="code.status === 1 ? 'badge badge-success' : 'badge badge-danger'">
                    {{ code.status === 1 ? '有效' : '已禁用' }}
                  </span>
                </td>
                <td>{{ formatDate(code.createTime) }}</td>
                <td>
                  <div class="action-buttons">
                    <button 
                      class="btn btn-sm btn-secondary"
                      @click="viewUsage(code)"
                    >
                      使用记录
                    </button>
                    <button 
                      class="btn btn-sm"
                      :class="code.status === 1 ? 'btn-warning' : 'btn-success'"
                      @click="toggleStatus(code)"
                    >
                      {{ code.status === 1 ? '禁用' : '启用' }}
                    </button>
                    <button 
                      class="btn btn-sm btn-danger"
                      @click="deleteCode(code)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="loading" class="loading">
            <div class="spinner"></div>
          </div>

          <div v-if="!loading && codes.length === 0" class="empty-state">
            <div class="empty-state-icon">🎫</div>
            <div class="empty-state-text">暂无激活码</div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
            上一页
          </button>
          <button
            v-for="page in visiblePages"
            :key="page"
            class="page-btn"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 创建激活码弹窗 -->
    <div class="modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">创建激活码</h3>
          <button class="modal-close" @click="showCreateModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="input-group mb-4">
            <label class="input-label">额外书签数量</label>
            <input type="number" v-model.number="createForm.extraBookmarks" class="input" min="1" />
          </div>
          <div class="input-group mb-4">
            <label class="input-label">额外分类数量</label>
            <input type="number" v-model.number="createForm.extraCategories" class="input" min="1" />
          </div>
          <div class="input-group mb-4">
            <label class="input-label">有效期(天)</label>
            <input type="number" v-model.number="createForm.expireDays" class="input" min="1" />
          </div>
          <div class="input-group">
            <label class="input-label">最大使用次数</label>
            <input type="number" v-model.number="createForm.maxUses" class="input" min="1" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showCreateModal = false">取消</button>
          <button class="btn btn-primary" @click="createCode">创建</button>
        </div>
      </div>
    </div>

    <!-- 批量创建弹窗 -->
    <div class="modal-overlay" v-if="showBatchModal" @click.self="showBatchModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">批量创建激活码</h3>
          <button class="modal-close" @click="showBatchModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="input-group mb-4">
            <label class="input-label">生成数量</label>
            <input type="number" v-model.number="batchForm.count" class="input" min="1" max="100" />
          </div>
          <div class="input-group mb-4">
            <label class="input-label">额外书签数量</label>
            <input type="number" v-model.number="batchForm.extraBookmarks" class="input" min="1" />
          </div>
          <div class="input-group mb-4">
            <label class="input-label">额外分类数量</label>
            <input type="number" v-model.number="batchForm.extraCategories" class="input" min="1" />
          </div>
          <div class="input-group mb-4">
            <label class="input-label">有效期(天)</label>
            <input type="number" v-model.number="batchForm.expireDays" class="input" min="1" />
          </div>
          <div class="input-group">
            <label class="input-label">每码最大使用次数</label>
            <input type="number" v-model.number="batchForm.maxUses" class="input" min="1" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showBatchModal = false">取消</button>
          <button class="btn btn-primary" @click="batchCreate">批量创建</button>
        </div>
      </div>
    </div>

    <!-- 使用记录弹窗 -->
    <div class="modal-overlay" v-if="showUsageModal" @click.self="showUsageModal = false">
      <div class="modal" style="max-width: 600px;">
        <div class="modal-header">
          <h3 class="modal-title">使用记录 - {{ selectedCode?.code }}</h3>
          <button class="modal-close" @click="showUsageModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="usageRecords.length > 0" class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>使用时间</th>
                  <th>过期时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in usageRecords" :key="record.id">
                  <td>{{ record.username }}</td>
                  <td>{{ formatDate(record.createTime) }}</td>
                  <td>{{ formatDate(record.expireTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <div class="empty-state-text">暂无使用记录</div>
          </div>
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
import { activationApi } from '@/api/activation'
import ConfirmModal from '@/components/ConfirmModal.vue'
import dayjs from 'dayjs'

const codes = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

const showCreateModal = ref(false)
const showBatchModal = ref(false)
const showUsageModal = ref(false)
const selectedCode = ref(null)
const usageRecords = ref([])

const createForm = reactive({
  extraBookmarks: 100,
  extraCategories: 10,
  expireDays: 30,
  maxUses: 1
})

const batchForm = reactive({
  count: 10,
  extraBookmarks: 100,
  extraCategories: 10,
  expireDays: 30,
  maxUses: 1
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

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

function formatDate(date) {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

async function copyCode(code) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(code);
    } else {
      // Fallback for insecure context (HTTP)
      const textArea = document.createElement("textarea");
      textArea.value = code;
      textArea.style.position = "fixed"; 
      textArea.style.left = "-9999px";
      textArea.style.top = "0";
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      const successful = document.execCommand('copy');
      document.body.removeChild(textArea);
      if (!successful) throw new Error('Copy failed');
    }
    showMessage('复制成功', '激活码已复制到剪贴板', 'success')
  } catch (err) {
    console.error('Copy failed:', err)
    showMessage('复制失败', '您的浏览器不支持自动复制，请手动复制', 'danger')
  }
}

async function loadCodes() {
  loading.value = true
  try {
    const response = await activationApi.getList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined,
      status: statusFilter.value !== '' ? statusFilter.value : undefined
    })
    if (response.code === 200) {
      codes.value = response.data.records || response.data.list || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load codes:', error)
    // 模拟数据
    codes.value = generateMockCodes()
    total.value = 50
  } finally {
    loading.value = false
  }
}

function generateMockCodes() {
  const mockCodes = []
  for (let i = 1; i <= 10; i++) {
    mockCodes.push({
      id: i,
      code: `CODE${Math.random().toString(36).substring(2, 10).toUpperCase()}`,
      extraBookmarks: 100,
      extraCategories: 10,
      expireDays: 30,
      maxUses: 5,
      usedCount: Math.floor(Math.random() * 5),
      status: i % 4 === 0 ? 0 : 1,
      createTime: dayjs().subtract(i, 'day').toISOString()
    })
  }
  return mockCodes
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadCodes()
}

function handleFilterChange() {
  currentPage.value = 1
  loadCodes()
}

const confirmModal = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'warning',
  confirmText: '确定',
  onConfirm: () => {}
})

function showMessage(title, message, type = 'info') {
  confirmModal.title = title
  confirmModal.message = message
  confirmModal.type = type
  confirmModal.confirmText = '知道了'
  confirmModal.onConfirm = () => {}
  confirmModal.visible = true
}

async function createCode() {
  try {
    await activationApi.create(createForm)
    showCreateModal.value = false
    loadCodes()
    showMessage('创建成功', '激活码已创建', 'success')
  } catch (error) {
    showMessage('创建失败', '无法创建激活码，请重试', 'danger')
  }
}

async function batchCreate() {
  try {
    await activationApi.batchCreate(batchForm)
    showBatchModal.value = false
    loadCodes()
    showMessage('批量创建成功', `已成功创建 ${batchForm.count} 个激活码`, 'success')
  } catch (error) {
    showMessage('批量创建失败', '无法批量创建激活码，请重试', 'danger')
  }
}

async function toggleStatus(code) {
  const newStatus = code.status === 1 ? 0 : 1
  try {
    await activationApi.update(code.id, { status: newStatus })
    code.status = newStatus
  } catch (error) {
    showMessage('操作失败', '无法更新激活码状态', 'danger')
  }
}

function deleteCode(code) {
  confirmModal.title = '删除激活码'
  confirmModal.message = `确定要删除激活码「${code.code}」吗？此操作不可恢复。`
  confirmModal.type = 'danger'
  confirmModal.confirmText = '删除'
  confirmModal.onConfirm = async () => {
    try {
      await activationApi.delete(code.id)
      loadCodes()
    } catch (error) {
      showMessage('删除失败', '无法删除激活码', 'danger')
    }
  }
  confirmModal.visible = true
}

async function viewUsage(code) {
  selectedCode.value = code
  showUsageModal.value = true
  
  try {
    const response = await activationApi.getUsage(code.id)
    if (response.code === 200) {
      usageRecords.value = response.data || []
    }
  } catch (error) {
    usageRecords.value = []
  }
}

onMounted(() => {
  loadCodes()
})
</script>

<style scoped>
.activation-codes {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.action-card {
  padding: 20px;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.search-input {
  max-width: 250px;
}

.status-select {
  width: 120px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.code-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.code-value {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 8px;
  background: var(--bg-page);
  border-radius: 4px;
  color: var(--primary);
}

.copy-btn {
  background: transparent;
  font-size: 14px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.copy-btn:hover {
  opacity: 1;
}

.usage-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.usage-bar {
  width: 60px;
  height: 6px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
}

.usage-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 3px;
  transition: width 0.3s;
}

.usage-text {
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
