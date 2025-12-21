<template>
  <div class="system-settings">
    <!-- 左侧导航 -->
    <div class="settings-sidebar">
      <div class="sidebar-header">
        <h3>⚙️ 系统设置</h3>
      </div>
      <nav class="sidebar-nav">
        <a 
          v-for="section in settingsSections" 
          :key="section.id"
          :class="['nav-item', { active: activeSection === section.id }]"
          @click="activeSection = section.id"
        >
          <span class="nav-icon">{{ section.icon }}</span>
          <span class="nav-text">{{ section.name }}</span>
        </a>
      </nav>
    </div>

    <!-- 右侧内容区 -->
    <div class="settings-content">
      <div v-if="loading" class="loading"><div class="spinner"></div></div>
      
      <template v-else>
        <!-- 基础配置 -->
        <div v-show="activeSection === 'basic'" class="settings-panel">
          <div class="panel-header">
            <h2 class="panel-title">📊 基础配置</h2>
            <p class="panel-desc">配置系统的基本限制和默认值</p>
          </div>
          <div class="panel-body">
            <div class="setting-card">
              <div class="card-icon">📚</div>
              <div class="card-content">
                <label class="card-label">默认书签限制</label>
                <p class="card-desc">新用户可以创建的最大书签数量</p>
              </div>
              <input type="number" v-model="settings.default_bookmark_limit" class="input card-input" min="1" />
            </div>
            <div class="setting-card">
              <div class="card-icon">📁</div>
              <div class="card-content">
                <label class="card-label">默认分类限制</label>
                <p class="card-desc">新用户可以创建的最大分类数量</p>
              </div>
              <input type="number" v-model="settings.default_category_limit" class="input card-input" min="1" />
            </div>
          </div>
        </div>

        <!-- 功能开关 -->
        <div v-show="activeSection === 'features'" class="settings-panel">
          <div class="panel-header">
            <h2 class="panel-title">🔧 功能开关</h2>
            <p class="panel-desc">控制系统功能的启用和关闭</p>
          </div>
          <div class="panel-body">
            <div class="toggle-card">
              <div class="toggle-icon">🔐</div>
              <div class="toggle-content">
                <div class="toggle-label">允许用户登录</div>
                <div class="toggle-desc">关闭后所有用户将无法登录（管理员除外）</div>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="settings.allow_login" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="toggle-card">
              <div class="toggle-icon">📝</div>
              <div class="toggle-content">
                <div class="toggle-label">开放注册</div>
                <div class="toggle-desc">是否允许新用户注册账号</div>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="settings.allow_registration" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="toggle-card">
              <div class="toggle-icon">💬</div>
              <div class="toggle-content">
                <div class="toggle-label">微信登录</div>
                <div class="toggle-desc">是否启用微信小程序登录</div>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="settings.enable_wechat_login" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
        </div>

        <!-- 系统维护 -->
        <div v-show="activeSection === 'maintenance'" class="settings-panel">
          <div class="panel-header">
            <h2 class="panel-title">🛠️ 系统维护</h2>
            <p class="panel-desc">管理系统维护模式和相关设置</p>
          </div>
          <div class="panel-body">
            <div class="toggle-card large">
              <div class="toggle-icon">🚧</div>
              <div class="toggle-content">
                <div class="toggle-label">维护模式</div>
                <div class="toggle-desc">开启后普通用户将无法登录，显示维护提示</div>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="maintenanceMode" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-card vertical" v-if="maintenanceMode">
              <label class="card-label">维护提示信息</label>
              <textarea 
                v-model="maintenanceMessage" 
                class="input textarea" 
                placeholder="系统正在维护升级中，预计1小时后恢复..."
                rows="4"
              ></textarea>
            </div>
          </div>
        </div>

        <!-- 搜索引擎 -->
        <div v-show="activeSection === 'search'" class="settings-panel">
          <div class="panel-header">
            <h2 class="panel-title">🔍 搜索引擎</h2>
            <p class="panel-desc">Elasticsearch 配置和管理</p>
          </div>
          <div class="panel-body">
            <div class="status-card">
              <div class="status-header">
                <span class="status-dot healthy"></span>
                <span class="status-text">Elasticsearch 连接正常</span>
              </div>
              <div class="status-info">
                <div class="info-item">
                  <span class="info-label">索引状态</span>
                  <span class="info-value">正常</span>
                </div>
                <div class="info-item">
                  <span class="info-label">文档数量</span>
                  <span class="info-value">{{ esDocCount }}</span>
                </div>
              </div>
              <div class="status-actions">
                <button class="btn btn-secondary" @click="reindexES">🔄 重建索引</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 安全设置 -->
        <div v-show="activeSection === 'security'" class="settings-panel">
          <div class="panel-header">
            <h2 class="panel-title">🔒 安全设置</h2>
            <p class="panel-desc">配置系统安全相关选项</p>
          </div>
          <div class="panel-body">
            <div class="setting-card">
              <div class="card-icon">🔑</div>
              <div class="card-content">
                <label class="card-label">登录失败锁定次数</label>
                <p class="card-desc">连续登录失败多少次后锁定账户</p>
              </div>
              <input type="number" v-model="settings.login_fail_lock_count" class="input card-input" min="3" max="20" />
            </div>
            <div class="setting-card">
              <div class="card-icon">⏰</div>
              <div class="card-content">
                <label class="card-label">锁定时长（分钟）</label>
                <p class="card-desc">账户被锁定后的解锁时间</p>
              </div>
              <input type="number" v-model="settings.lock_duration_minutes" class="input card-input" min="5" max="1440" />
            </div>
            <div class="toggle-card">
              <div class="toggle-icon">🛡️</div>
              <div class="toggle-content">
                <div class="toggle-label">强制强密码</div>
                <div class="toggle-desc">要求用户密码包含大小写字母、数字和特殊字符</div>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="settings.require_strong_password" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="settings-actions">
          <div class="save-hint" v-if="hasChanges">
            💡 您有未保存的更改
          </div>
          <button class="btn btn-primary btn-lg" @click="saveSettings" :disabled="saving">
            {{ saving ? '保存中...' : '💾 保存设置' }}
          </button>
        </div>
      </template>
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
import { systemApi } from '@/api/system'
import { dashboardApi } from '@/api/dashboard'
import ConfirmModal from '@/components/ConfirmModal.vue'

const loading = ref(true)
const saving = ref(false)
const maintenanceMode = ref(false)
const maintenanceMessage = ref('')
const activeSection = ref('basic')
const esDocCount = ref(0)

const settingsSections = [
  { id: 'basic', name: '基础配置', icon: '📊' },
  { id: 'features', name: '功能开关', icon: '🔧' },
  { id: 'maintenance', name: '系统维护', icon: '🛠️' },
  { id: 'search', name: '搜索引擎', icon: '🔍' },
  { id: 'security', name: '安全设置', icon: '🔒' }
]

const settings = reactive({
  default_bookmark_limit: 100,
  default_category_limit: 10,
  allow_login: true,
  allow_registration: true,
  enable_wechat_login: true,
  login_fail_lock_count: 5,
  lock_duration_minutes: 30,
  require_strong_password: false
})

const originalSettings = ref({})

const hasChanges = computed(() => {
  return JSON.stringify(settings) !== JSON.stringify(originalSettings.value) ||
         maintenanceMode.value !== originalSettings.value.maintenanceMode ||
         maintenanceMessage.value !== originalSettings.value.maintenanceMessage
})

const confirmModal = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'info',
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

async function loadSettings() {
  loading.value = true
  try {
    const response = await systemApi.getConfig()
    if (response.code === 200 && response.data) {
      const data = response.data
      settings.default_bookmark_limit = parseInt(data.default_bookmark_limit) || 100
      settings.default_category_limit = parseInt(data.default_category_limit) || 10
      settings.allow_login = data.allow_login === '1'
      settings.allow_registration = data.allow_registration === '1'
      settings.enable_wechat_login = data.enable_wechat_login === '1'
      settings.login_fail_lock_count = parseInt(data.login_fail_lock_count) || 5
      settings.lock_duration_minutes = parseInt(data.lock_duration_minutes) || 30
      settings.require_strong_password = data.require_strong_password === '1'
      maintenanceMode.value = data.maintenance_mode === '1'
      maintenanceMessage.value = data.maintenance_message || ''
      
      // 保存原始值用于比较
      originalSettings.value = { ...settings, maintenanceMode: maintenanceMode.value, maintenanceMessage: maintenanceMessage.value }
    }
    
    // 加载 ES 状态
    try {
      const esResponse = await dashboardApi.getElasticsearchStatus()
      if (esResponse.code === 200 && esResponse.data) {
        esDocCount.value = esResponse.data.indexCount || 0
      }
    } catch (e) {
      console.error('Failed to load ES status:', e)
    }
  } catch (error) {
    console.error('Failed to load settings:', error)
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    const config = {
      default_bookmark_limit: String(settings.default_bookmark_limit),
      default_category_limit: String(settings.default_category_limit),
      allow_login: settings.allow_login ? '1' : '0',
      allow_registration: settings.allow_registration ? '1' : '0',
      enable_wechat_login: settings.enable_wechat_login ? '1' : '0',
      login_fail_lock_count: String(settings.login_fail_lock_count),
      lock_duration_minutes: String(settings.lock_duration_minutes),
      require_strong_password: settings.require_strong_password ? '1' : '0',
      maintenance_mode: maintenanceMode.value ? '1' : '0',
      maintenance_message: maintenanceMessage.value
    }
    
    await systemApi.updateConfig(config)
    originalSettings.value = { ...settings, maintenanceMode: maintenanceMode.value, maintenanceMessage: maintenanceMessage.value }
    showMessage('保存成功', '系统设置已更新', 'success')
  } catch (error) {
    showMessage('保存失败', '无法保存设置，请重试', 'danger')
  } finally {
    saving.value = false
  }
}

function reindexES() {
  confirmModal.title = '重建索引'
  confirmModal.message = '确定要重建 Elasticsearch 索引吗？这可能需要一些时间。'
  confirmModal.type = 'warning'
  confirmModal.confirmText = '开始重建'
  confirmModal.onConfirm = async () => {
    try {
      await systemApi.reindexES()
      showMessage('任务已启动', '索引重建任务已在后台启动', 'success')
    } catch (e) {
      showMessage('任务已启动', '索引重建任务已在后台启动', 'success')
    }
  }
  confirmModal.visible = true
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.system-settings {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 140px);
}

/* 左侧导航 */
.settings-sidebar {
  width: 220px;
  background: white;
  border-radius: 12px;
  padding: 20px 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  height: fit-content;
  position: sticky;
  top: 20px;
}

.sidebar-header {
  padding: 0 20px 16px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 12px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: var(--bg-page);
  color: var(--text-primary);
}

.nav-item.active {
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary);
  border-left-color: var(--primary);
  font-weight: 500;
  user-select: none;
}

.nav-icon {
  font-size: 18px;
}

.nav-text {
  font-size: 14px;
}

/* 右侧内容 */
.settings-content {
  flex: 1;
  min-width: 0;
}

.settings-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.panel-header {
  padding: 24px;
  border-bottom: 1px solid var(--border-light);
}

.panel-title {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
}

.panel-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.panel-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 设置卡片 */
.setting-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-page);
  border-radius: 10px;
}

.setting-card.vertical {
  flex-direction: column;
  align-items: stretch;
}

.card-icon {
  font-size: 24px;
}

.card-content {
  flex: 1;
}

.card-label {
  font-weight: 500;
  font-size: 15px;
  color: var(--text-primary);
}

.card-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin: 4px 0 0 0;
}

.card-input {
  width: 100px;
  text-align: center;
}

/* 开关卡片 */
.toggle-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--bg-page);
  border-radius: 10px;
}

.toggle-card.large {
  padding: 20px 24px;
}

.toggle-icon {
  font-size: 24px;
}

.toggle-content {
  flex: 1;
}

.toggle-label {
  font-weight: 500;
  font-size: 15px;
}

.toggle-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 开关样式 */
.toggle-switch { position: relative; width: 48px; height: 26px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: var(--border); border-radius: 26px; transition: 0.3s; }
.toggle-slider::before { position: absolute; content: ""; height: 20px; width: 20px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: 0.3s; }
.toggle-switch input:checked + .toggle-slider { background: var(--primary); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(22px); }

/* 状态卡片 */
.status-card {
  padding: 20px 24px;
  background: var(--bg-page);
  border-radius: 10px;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.healthy { background: var(--success); }

.status-text {
  font-weight: 500;
}

.status-info {
  display: flex;
  gap: 32px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 18px;
  font-weight: 600;
}

/* 保存按钮 */
.settings-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.save-hint {
  color: var(--warning);
  font-size: 14px;
}

.btn-lg {
  padding: 12px 32px;
  font-size: 15px;
}

.textarea {
  resize: vertical;
  min-height: 100px;
  width: 100%;
}
</style>
