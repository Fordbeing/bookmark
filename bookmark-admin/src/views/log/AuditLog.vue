<template>
  <div class="audit-log">
    <!-- 操作栏 -->
    <div class="card filter-card">
      <div class="filter-header">
        <h3 class="card-title">📝 操作日志</h3>
        <div class="header-actions">
          <button class="btn btn-secondary" @click="exportLogs">
            📥 导出 CSV
          </button>
        </div>
      </div>
      
      <div class="filter-row">
        <div class="filter-group">
          <label class="filter-label">操作类型</label>
          <select v-model="filters.actionType" class="input filter-select" @change="handleFilterChange">
            <option value="">全部操作</option>
            <option value="登录">登录</option>
            <option value="登出">登出</option>
            <option value="启用用户">启用用户</option>
            <option value="禁用用户">禁用用户</option>
            <option value="设置管理员">设置管理员</option>
            <option value="取消管理员">取消管理员</option>
            <option value="创建激活码">创建激活码</option>
            <option value="修改配置">修改配置</option>
            <option value="发布公告">发布公告</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label class="filter-label">目标类型</label>
          <select v-model="filters.targetType" class="input filter-select" @change="handleFilterChange">
            <option value="">全部目标</option>
            <option value="用户">用户</option>
            <option value="激活码">激活码</option>
            <option value="系统配置">系统配置</option>
            <option value="公告">公告</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label class="filter-label">开始日期</label>
          <input type="date" v-model="filters.startDate" class="input filter-date" @change="handleFilterChange" />
        </div>
        
        <div class="filter-group">
          <label class="filter-label">结束日期</label>
          <input type="date" v-model="filters.endDate" class="input filter-date" @change="handleFilterChange" />
        </div>
        
        <div class="filter-group">
          <label class="filter-label">关键字</label>
          <input 
            type="text" 
            v-model="filters.keyword" 
            class="input filter-keyword" 
            placeholder="搜索..." 
            @keyup.enter="handleFilterChange" 
          />
        </div>
        
        <button class="btn btn-secondary btn-filter" @click="handleFilterChange">
          🔍 搜索
        </button>
        <button class="btn btn-secondary btn-filter" @click="resetFilters">
          🔄 重置
        </button>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="card">
      <div class="card-header">
        <span class="text-secondary text-sm">共 {{ total }} 条记录</span>
        <div class="page-size-select">
          <label>每页显示</label>
          <select v-model="pageSize" class="input select-sm" @change="handlePageSizeChange">
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
      </div>
      <div class="card-body">
        <div class="table-container">
          <table class="table" v-if="logs.length > 0">
            <thead>
              <tr>
                <th>时间</th>
                <th>操作人</th>
                <th>操作类型</th>
                <th>目标</th>
                <th>IP地址</th>
                <th>变更详情</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id" @click="showLogDetail(log)" class="log-row">
                <td>{{ formatDate(log.createTime) }}</td>
                <td>
                  <div class="admin-cell">
                    <span class="admin-name">{{ log.adminName || '系统' }}</span>
                  </div>
                </td>
                <td>
                  <span :class="getActionBadgeClass(log.actionType)">{{ log.actionType }}</span>
                </td>
                <td>
                  <span class="target-text">{{ log.targetType }} {{ log.targetId ? '#' + log.targetId : '' }}</span>
                </td>
                <td>
                  <span class="ip-text">{{ log.ipAddress || '-' }}</span>
                </td>
                <td>
                  <div class="change-cell">
                    <span class="old-value" v-if="log.oldValue">{{ truncate(log.oldValue, 20) }}</span>
                    <span class="arrow" v-if="log.oldValue && log.newValue">→</span>
                    <span class="new-value" v-if="log.newValue">{{ truncate(log.newValue, 20) }}</span>
                    <span v-if="!log.oldValue && !log.newValue">-</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="loading" class="loading"><div class="spinner"></div></div>
          <div v-if="!loading && logs.length === 0" class="empty-state">
            <div class="empty-state-icon">📝</div>
            <div class="empty-state-text">暂无日志</div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(1)">
            首页
          </button>
          <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
            上一页
          </button>
          <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
            下一页
          </button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(totalPages)">
            末页
          </button>
        </div>
      </div>
    </div>

    <!-- 日志详情弹窗 -->
    <div class="modal-overlay" v-if="showDetailModal" @click.self="showDetailModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">日志详情</h3>
          <button class="modal-close" @click="showDetailModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item">
              <div class="detail-label">操作时间</div>
              <div class="detail-value">{{ formatDate(selectedLog?.createTime) }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">操作人</div>
              <div class="detail-value">{{ selectedLog?.adminName || '系统' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">操作类型</div>
              <div class="detail-value">{{ selectedLog?.actionType }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">目标</div>
              <div class="detail-value">{{ selectedLog?.targetType }} #{{ selectedLog?.targetId }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">IP 地址</div>
              <div class="detail-value">{{ selectedLog?.ipAddress || '-' }}</div>
            </div>
            <div class="detail-item">
              <div class="detail-label">User Agent</div>
              <div class="detail-value detail-value-long">{{ selectedLog?.userAgent || '-' }}</div>
            </div>
            <div class="detail-item full-width" v-if="selectedLog?.oldValue">
              <div class="detail-label">原值</div>
              <div class="detail-value detail-value-code">{{ selectedLog?.oldValue }}</div>
            </div>
            <div class="detail-item full-width" v-if="selectedLog?.newValue">
              <div class="detail-label">新值</div>
              <div class="detail-value detail-value-code">{{ selectedLog?.newValue }}</div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showDetailModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { logApi } from '@/api/log'
import dayjs from 'dayjs'

const logs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetailModal = ref(false)
const selectedLog = ref(null)

const filters = reactive({
  actionType: '',
  targetType: '',
  startDate: '',
  endDate: '',
  keyword: ''
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

function formatDate(date) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function truncate(str, maxLength) {
  if (!str) return ''
  return str.length > maxLength ? str.substring(0, maxLength) + '...' : str
}

function getActionBadgeClass(actionType) {
  const typeMap = {
    '登录': 'badge badge-info',
    '登出': 'badge badge-secondary',
    '启用用户': 'badge badge-success',
    '禁用用户': 'badge badge-danger',
    '设置管理员': 'badge badge-warning',
    '取消管理员': 'badge badge-warning',
    '创建激活码': 'badge badge-primary',
    '修改配置': 'badge badge-info',
    '发布公告': 'badge badge-success'
  }
  return typeMap[actionType] || 'badge badge-secondary'
}

async function loadLogs() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      actionType: filters.actionType || undefined,
      targetType: filters.targetType || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      keyword: filters.keyword || undefined
    }
    
    const response = await logApi.getList(params)
    if (response.code === 200) {
      logs.value = response.data.records || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load logs:', error)
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  currentPage.value = 1
  loadLogs()
}

function handlePageSizeChange() {
  currentPage.value = 1
  loadLogs()
}

function resetFilters() {
  filters.actionType = ''
  filters.targetType = ''
  filters.startDate = ''
  filters.endDate = ''
  filters.keyword = ''
  currentPage.value = 1
  loadLogs()
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadLogs()
}

function showLogDetail(log) {
  selectedLog.value = log
  showDetailModal.value = true
}

async function exportLogs() {
  try {
    const params = {
      actionType: filters.actionType || undefined,
      targetType: filters.targetType || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      keyword: filters.keyword || undefined
    }
    
    // 直接构建 CSV 内容（前端导出）
    const headers = ['时间', '操作人', '操作类型', '目标类型', '目标ID', 'IP地址', '原值', '新值']
    const rows = logs.value.map(log => [
      formatDate(log.createTime),
      log.adminName || '系统',
      log.actionType,
      log.targetType || '',
      log.targetId || '',
      log.ipAddress || '',
      log.oldValue || '',
      log.newValue || ''
    ])
    
    let csvContent = '\uFEFF' // BOM for Excel UTF-8 support
    csvContent += headers.join(',') + '\n'
    rows.forEach(row => {
      csvContent += row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',') + '\n'
    })
    
    // 创建下载链接
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `操作日志_${dayjs().format('YYYY-MM-DD_HHmmss')}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (error) {
    console.error('Export failed:', error)
    alert('导出失败，请重试')
  }
}

onMounted(() => { loadLogs() })
</script>

<style scoped>
.audit-log { display: flex; flex-direction: column; gap: 20px; }

.filter-card { padding: 20px; }

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title { margin: 0; font-size: 18px; }

.filter-row { 
  display: flex; 
  flex-wrap: wrap;
  gap: 16px; 
  align-items: flex-end; 
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.filter-select { width: 140px; }
.filter-date { width: 140px; }
.filter-keyword { width: 160px; }
.btn-filter { height: 38px; }

.page-size-select {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.select-sm {
  width: 70px;
  padding: 4px 8px;
  font-size: 13px;
}

.log-row {
  cursor: pointer;
  transition: background 0.2s;
}

.log-row:hover {
  background: var(--bg-page);
}

.admin-cell { display: flex; align-items: center; gap: 8px; }
.admin-name { font-weight: 500; }

.target-text { color: var(--text-secondary); font-size: 13px; }
.ip-text { font-family: monospace; font-size: 12px; color: var(--text-muted); }

.change-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.old-value { color: var(--danger); }
.new-value { color: var(--success); }
.arrow { color: var(--text-muted); }

.page-info { padding: 0 16px; color: var(--text-secondary); }

.badge-primary { background: rgba(99, 102, 241, 0.2); color: #6366f1; }

/* 详情弹窗 */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: var(--text-primary);
}

.detail-value-long {
  word-break: break-all;
  font-size: 12px;
}

.detail-value-code {
  background: var(--bg-page);
  padding: 12px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
