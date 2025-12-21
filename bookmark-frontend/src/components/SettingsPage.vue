<template>
  <el-dialog
    v-model="visible"
    title="⚙️ 设置"
    width="700px"
    class="settings-dialog"
    @close="handleClose"
  >
    <el-tabs v-model="activeTab">
      <!-- 通用设置 -->
      <el-tab-pane label="🎨 主题设置" name="theme">
        <div class="space-y-6">
          <!-- 预设配色方案 -->
          <div class="setting-item bg-gray-50 p-4 rounded-lg">
            <h3 class="font-bold text-gray-800 mb-3">📌 预设配色方案</h3>
            <p class="text-xs text-gray-600 mb-4">快速应用预设的颜色主题</p>
            <div class="grid grid-cols-3 gap-3">
              <button
                v-for="preset in colorPresets"
                :key="preset.name"
                @click="applyPreset(preset)"
                class="p-3 rounded-lg border-2 hover:border-gray-400 transition-all bg-white"
                :style="{ borderColor: settings.primaryColor === preset.primary ? preset.primary : '#e5e7eb' }"
              >
                <div class="flex gap-1 mb-2 justify-center">
                  <div
                    v-for="color in [preset.primary, preset.secondary]"
                    :key="color"
                    class="w-6 h-6 rounded-full"
                    :style="{ backgroundColor: color }"
                  ></div>
                </div>
                <span class="text-sm text-gray-700">{{ preset.name }}</span>
              </button>
            </div>
          </div>

          <!-- 侧边栏样式 -->
          <div class="setting-item bg-blue-50 p-4 rounded-lg">
            <h3 class="font-bold text-gray-800 mb-3">🔷 侧边栏样式</h3>
            <p class="text-xs text-gray-600 mb-4">自定义侧边栏的渐变颜色</p>

            <div class="flex gap-6 items-center">
              <div>
                <label class="text-sm text-gray-700 mb-2 block">起始色</label>
                <input type="color" v-model="settings.sidebarColorFrom" class="w-12 h-10 cursor-pointer border rounded" />
              </div>
              <div>
                <label class="text-sm text-gray-700 mb-2 block">结束色</label>
                <input type="color" v-model="settings.sidebarColorTo" class="w-12 h-10 cursor-pointer border rounded" />
              </div>
              <div class="flex-1 p-3 rounded-lg text-white text-center" :style="{
                background: `linear-gradient(to bottom, ${settings.sidebarColorFrom}, ${settings.sidebarColorTo})`
              }">
                <span class="text-sm">预览效果</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 显示设置 -->
      <el-tab-pane label="👀 显示设置" name="display">
        <div class="space-y-6">
          <div class="setting-item">
            <div class="flex justify-between items-center">
              <div>
                <h3 class="font-medium text-gray-800">书签显示方式</h3>
                <p class="text-xs text-gray-500 mt-1">选择书签的展示风格</p>
              </div>
              <el-select v-model="settings.displayMode" placeholder="选择显示方式">
                <el-option label="卡片视图" value="card" />
                <el-option label="列表视图" value="list" />
                <el-option label="紧凑视图" value="compact" />
              </el-select>
            </div>
          </div>

          <el-divider />

          <div class="setting-item">
            <div class="flex justify-between items-center">
              <div>
                <h3 class="font-medium text-gray-800">自动打开链接</h3>
                <p class="text-xs text-gray-500 mt-1">点击书签时在新标签页打开</p>
              </div>
              <el-switch v-model="settings.autoOpenNewTab" />
            </div>
          </div>

          <el-divider />

          <div class="setting-item">
            <div class="flex justify-between items-center">
              <div>
                <h3 class="font-medium text-gray-800">显示统计信息</h3>
                <p class="text-xs text-gray-500 mt-1">在侧边栏显示书签统计</p>
              </div>
              <el-switch v-model="settings.showStats" />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 标签管理 -->
      <el-tab-pane label="🏷️ 标签管理" name="tags">
        <div class="space-y-4">
          <div class="flex gap-2">
            <el-input v-model="newTagName" placeholder="输入新标签名称" />
            <el-color-picker v-model="newTagColor" />
            <el-button type="primary" @click="addTag" :loading="tagLoading">添加标签</el-button>
          </div>

          <div v-if="tags.length > 0" class="space-y-2">
            <div v-for="tag in tags" :key="tag.id" class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div class="flex items-center gap-3">
                <span 
                  class="w-4 h-4 rounded-full" 
                  :style="{ backgroundColor: tag.color || '#6b7280' }"
                ></span>
                <span class="font-medium">{{ tag.name }}</span>
                <span class="text-xs text-gray-500">使用 {{ tag.usageCount || 0 }} 次</span>
              </div>
              <div class="flex gap-2">
                <el-button type="primary" size="small" @click="editTag(tag)">编辑</el-button>
                <el-button type="danger" size="small" @click="removeTag(tag.id)">删除</el-button>
              </div>
            </div>
          </div>
          <div v-else class="text-center text-gray-500 py-8">
            暂无标签，添加一个吧！
          </div>
        </div>
      </el-tab-pane>

      <!-- 我的分享 -->
      <el-tab-pane label="🔗 我的分享" name="shares">
        <div class="space-y-4">
          <div class="text-sm text-gray-600 mb-4">
            管理您分享的分类链接，可以查看访问次数或取消分享。
          </div>
          
          <div v-if="shareLoading" class="text-center py-8">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
            <p class="text-gray-500 mt-2">加载中...</p>
          </div>
          
          <div v-else-if="myShares.length > 0" class="space-y-3">
            <div 
              v-for="share in myShares" 
              :key="share.id" 
              class="flex items-center justify-between p-4 bg-gray-50 rounded-lg border"
            >
              <div class="flex-1">
                <div class="font-medium text-gray-800 mb-1">
                  📁 分类ID: {{ share.categoryId }}
                </div>
                <div class="text-sm text-gray-500 mb-2">
                  分享码: <code class="bg-gray-200 px-2 py-0.5 rounded">{{ share.shareCode }}</code>
                </div>
                <div class="flex gap-4 text-xs text-gray-500">
                  <span>👁️ 访问 {{ share.viewCount || 0 }} 次</span>
                  <span>🔒 {{ share.password ? '有密码' : '无密码' }}</span>
                  <span>⏰ {{ share.expireTime ? formatExpireTime(share.expireTime) : '永久有效' }}</span>
                </div>
              </div>
              <div class="flex gap-2 ml-4">
                <el-button size="small" @click="copyShareLink(share)">复制链接</el-button>
                <el-button size="small" type="danger" @click="cancelShare(share)">取消分享</el-button>
              </div>
            </div>
          </div>
          
          <div v-else class="text-center py-12 text-gray-500">
            <div class="text-4xl mb-3">📭</div>
            <p>暂无分享记录</p>
            <p class="text-sm mt-1">在侧边栏的分类上悬停，点击🔗按钮可创建分享</p>
          </div>
        </div>
      </el-tab-pane>

      <!-- 关于 -->
      <el-tab-pane label="ℹ️ 关于" name="about">
        <div class="about-container">
          <!-- 品牌区 -->
          <div class="brand-section">
            <div class="brand-icon">🔖</div>
            <h2 class="brand-title">BookUtil</h2>
            <p class="brand-subtitle">智能书签管理工具</p>
            <span class="version-badge">v1.0.0</span>
          </div>

          <!-- 功能轮播 -->
          <div class="feature-carousel">
            <div class="carousel-track" ref="carouselTrack">
              <div 
                v-for="(slide, index) in featureSlides" 
                :key="index"
                class="carousel-slide"
                :class="{ active: currentSlide === index }"
              >
                <span class="slide-icon">{{ slide.icon }}</span>
                <div class="slide-content">
                  <h4>{{ slide.title }}</h4>
                  <p>{{ slide.desc }}</p>
                </div>
              </div>
            </div>
            <div class="carousel-dots">
              <button 
                v-for="(slide, index) in featureSlides" 
                :key="index"
                class="dot"
                :class="{ active: currentSlide === index }"
                @click="goToSlide(index)"
              />
            </div>
          </div>

          <!-- 核心特性 -->
          <div class="highlights-grid">
            <div class="highlight-item">
              <span class="highlight-icon">🔍</span>
              <span class="highlight-text">ES 全文搜索</span>
            </div>
            <div class="highlight-item">
              <span class="highlight-icon">📱</span>
              <span class="highlight-text">微信小程序</span>
            </div>
            <div class="highlight-item">
              <span class="highlight-icon">🔌</span>
              <span class="highlight-text">浏览器扩展</span>
            </div>
            <div class="highlight-item">
              <span class="highlight-icon">📤</span>
              <span class="highlight-text">导入导出</span>
            </div>
          </div>

          <!-- 底部信息 -->
          <div class="about-footer">
            <span>© 2024 BookUtil Team</span>
            <span>•</span>
            <span>更新于 2024-12</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="flex justify-between">
        <el-button @click="resetSettings">重置为默认</el-button>
        <div class="space-x-2">
          <el-button @click="visible = false">关闭</el-button>
          <el-button type="primary" @click="saveSettings">保存设置</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Link, ChatDotRound, Loading } from '@element-plus/icons-vue';
import { getTagListAPI, createTagAPI, updateTagAPI, deleteTagAPI } from '../api/tag';
import { getSettingsAPI, updateSettingsAPI } from '../api/settings';
import { getMySharesAPI, cancelShareAPI } from '../api/share';

const props = defineProps({
  modelValue: Boolean,
});

const emit = defineEmits(['update:modelValue', 'update-settings']);

const visible = ref(props.modelValue);
const activeTab = ref('theme');
const newTagName = ref('');
const newTagColor = ref('#6b7280');
const tags = ref([]);
const tagLoading = ref(false);

// 分享管理相关
const myShares = ref([]);
const shareLoading = ref(false);

// 轮播相关
const currentSlide = ref(0);
const carouselTrack = ref(null);
let carouselTimer = null;

const featureSlides = [
  { icon: '🚀', title: '快速添加', desc: '粘贴链接自动获取网页标题和描述' },
  { icon: '📁', title: '分类管理', desc: '自定义分类整理书签，支持拖拽排序' },
  { icon: '🏷️', title: '标签系统', desc: '自定义彩色标签，多维度管理书签' },
  { icon: '⭐', title: '收藏夹', desc: '一键收藏重要书签，快速访问' },
  { icon: '🔍', title: '全文搜索', desc: 'ES搜索引擎，标题/网址/描述全覆盖' },
  { icon: '🔌', title: '浏览器扩展', desc: '一键保存当前页面，快捷键操作' },
];

const goToSlide = (index) => {
  currentSlide.value = index;
  resetCarouselTimer();
};

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % featureSlides.length;
};

const resetCarouselTimer = () => {
  if (carouselTimer) clearInterval(carouselTimer);
  carouselTimer = setInterval(nextSlide, 4000);
};

onMounted(() => {
  resetCarouselTimer();
});

onUnmounted(() => {
  if (carouselTimer) clearInterval(carouselTimer);
});

// 监听 modelValue 变化
watch(() => props.modelValue, (newVal) => {
  visible.value = newVal;
}, { immediate: true });

// 监听 visible 变化，发送更新事件
watch(visible, (newVal) => {
  emit('update:modelValue', newVal);
});

const colorPresets = [
  { name: '蓝色', primary: '#2563eb', secondary: '#1e40af' },
  { name: '紫色', primary: '#9333ea', secondary: '#6b21a8' },
  { name: '绿色', primary: '#16a34a', secondary: '#15803d' },
  { name: '红色', primary: '#dc2626', secondary: '#991b1b' },
  { name: '橙色', primary: '#ea580c', secondary: '#c2410c' },
  { name: '青色', primary: '#0891b2', secondary: '#0e7490' },
];

const settings = reactive({
  theme: 'light',
  displayMode: 'card',
  autoOpenNewTab: true,
  showStats: true,
  primaryColor: '#2563eb',
  secondaryColor: '#1e40af',
  accentColor: '#f59e0b',
  backgroundColor: '#ffffff',
  sidebarColorFrom: '#2563eb',
  sidebarColorTo: '#1e3a8a',
});

const defaultSettings = { ...settings };

const applyPreset = (preset) => {
  settings.primaryColor = preset.primary;
  settings.secondaryColor = preset.secondary;
  settings.sidebarColorFrom = preset.primary;
  settings.sidebarColorTo = preset.secondary;
};

// 加载标签列表
const loadTags = async () => {
  try {
    const result = await getTagListAPI();
    if (result.data) {
      tags.value = result.data;
    }
  } catch (error) {
    console.error('加载标签失败:', error);
  }
};

// 添加标签
const addTag = async () => {
  if (!newTagName.value.trim()) {
    ElMessage.warning('请输入标签名称');
    return;
  }
  
  tagLoading.value = true;
  try {
    await createTagAPI({
      name: newTagName.value.trim(),
      color: newTagColor.value
    });
    ElMessage.success('标签已添加');
    newTagName.value = '';
    newTagColor.value = '#6b7280';
    await loadTags();
  } catch (error) {
    console.error('添加标签失败:', error);
  } finally {
    tagLoading.value = false;
  }
};

// 编辑标签
const editTag = (tag) => {
  ElMessageBox.prompt('请输入新的标签名称', '编辑标签', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputValue: tag.name
  }).then(async ({ value }) => {
    if (value && value.trim()) {
      try {
        await updateTagAPI(tag.id, { name: value.trim() });
        ElMessage.success('标签已更新');
        await loadTags();
      } catch (error) {
        console.error('更新标签失败:', error);
      }
    }
  }).catch(() => {});
};

// 删除标签
const removeTag = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个标签吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteTagAPI(id);
    ElMessage.success('标签已删除');
    await loadTags();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除标签失败:', error);
    }
  }
};

// ========== 分享管理相关 ==========

// 加载我的分享列表
const loadShares = async () => {
  shareLoading.value = true;
  try {
    const result = await getMySharesAPI();
    if (result.data) {
      myShares.value = result.data;
    }
  } catch (error) {
    console.error('加载分享列表失败:', error);
  } finally {
    shareLoading.value = false;
  }
};

// 取消分享
const cancelShare = async (share) => {
  try {
    await ElMessageBox.confirm('确定要取消这个分享吗？取消后链接将失效。', '提示', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      type: 'warning'
    });
    await cancelShareAPI(share.id);
    ElMessage.success('分享已取消');
    await loadShares();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消分享失败:', error);
    }
  }
};

// 复制分享链接
const copyShareLink = async (share) => {
  const url = window.location.origin + '/public/share/' + share.shareCode;
  try {
    await navigator.clipboard.writeText(url);
    ElMessage.success('链接已复制');
  } catch (error) {
    // 降级方案
    const textarea = document.createElement('textarea');
    textarea.value = url;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    ElMessage.success('链接已复制');
  }
};

// 格式化过期时间
const formatExpireTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleDateString('zh-CN');
};

// 导出数据
const exportData = async () => {
  exportLoading.value = true;
  try {
    const success = await downloadExportData();
    if (success) {
      ElMessage.success('书签已导出');
    }
  } catch (error) {
    ElMessage.error('导出失败: ' + (error.message || '未知错误'));
  } finally {
    exportLoading.value = false;
  }
};

// 选择文件
const handleFileChange = (uploadFile) => {
  selectedFile.value = uploadFile.raw;
  importResult.value = null;
};

// 执行导入
const doImport = async () => {
  if (!selectedFile.value || !importType.value) {
    ElMessage.warning('请选择导入类型和文件');
    return;
  }

  importLoading.value = true;
  try {
    const result = await importBookmarksFileAPI(selectedFile.value, importType.value);
    importResult.value = result.data;
    ElMessage.success(result.message || '导入成功');
    selectedFile.value = null;
    // 触发刷新书签列表
    emit('update-settings', { refreshBookmarks: true });
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.response?.data?.message || error.message || '格式不正确'));
  } finally {
    importLoading.value = false;
  }
};

// 清除所有数据
const clearAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清除所有数据吗？这将删除所有书签、分类和标签，此操作不可撤销！',
      '警告',
      {
        confirmButtonText: '确定清除',
        cancelButtonText: '取消',
        type: 'error'
      }
    );
    await clearAllDataAPI();
    ElMessage.success('所有数据已清除');
    emit('update-settings', { refreshBookmarks: true });
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清除失败');
    }
  }
};

const saveSettings = async () => {
  try {
    // 保存到数据库
    await updateSettingsAPI({
      theme: settings.theme,
      primaryColor: settings.primaryColor,
      secondaryColor: settings.secondaryColor,
      accentColor: settings.accentColor,
      backgroundColor: settings.backgroundColor,
      sidebarColorFrom: settings.sidebarColorFrom,
      sidebarColorTo: settings.sidebarColorTo,
      displayMode: settings.displayMode,
      autoOpenNewTab: settings.autoOpenNewTab ? 1 : 0,
      showStats: settings.showStats ? 1 : 0
    });
    // 发送事件通知父组件
    emit('update-settings', settings);
    ElMessage.success('设置已保存');
  } catch (error) {
    console.error('保存设置失败:', error);
    ElMessage.error('保存设置失败');
  }
};

const resetSettings = () => {
  Object.assign(settings, defaultSettings);
  ElMessage.info('已重置为默认设置');
};

const handleClose = () => {
  emit('update:modelValue', false);
};

// 加载设置和标签
const loadSettings = async () => {
  try {
    const result = await getSettingsAPI();
    if (result.data) {
      const data = result.data;
      settings.theme = data.theme || 'light';
      settings.primaryColor = data.primaryColor || '#2563eb';
      settings.secondaryColor = data.secondaryColor || '#1e40af';
      settings.accentColor = data.accentColor || '#f59e0b';
      settings.backgroundColor = data.backgroundColor || '#ffffff';
      settings.sidebarColorFrom = data.sidebarColorFrom || '#2563eb';
      settings.sidebarColorTo = data.sidebarColorTo || '#1e3a8a';
      settings.displayMode = data.displayMode || 'card';
      settings.autoOpenNewTab = data.autoOpenNewTab === 1 || data.autoOpenNewTab === true;
      settings.showStats = data.showStats === 1 || data.showStats === true;
    }
  } catch (error) {
    console.error('加载设置失败:', error);
  }
};

// 监听弹窗打开，加载数据
watch(visible, (val) => {
  if (val) {
    loadSettings();
    loadTags();
    loadShares(); // 加载分享列表
  }
});

// 监听Tab切换，按需加载分享列表
watch(activeTab, (val) => {
  if (val === 'shares') {
    loadShares();
  }
});

onMounted(() => {
  const token = localStorage.getItem('token');
  if (token) {
    loadTags();
  }
});
</script>

<style scoped>
.settings-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

.setting-item {
  transition: all 0.3s ease;
}

.setting-item:hover {
  transform: translateX(4px);
}

/* 关于页面样式 */
.about-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.brand-section {
  text-align: center;
  background: linear-gradient(135deg, #eff6ff, #faf5ff);
  padding: 24px;
  border-radius: 16px;
}

.brand-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin: 0;
}

.brand-subtitle {
  color: #6b7280;
  margin: 4px 0 12px 0;
  font-size: 14px;
}

.version-badge {
  display: inline-block;
  background: #dbeafe;
  color: #1d4ed8;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

/* 功能轮播 */
.feature-carousel {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 16px;
  padding: 20px;
  overflow: hidden;
}

.carousel-track {
  position: relative;
  min-height: 70px;
}

.carousel-slide {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0;
  transform: translateX(30px);
  transition: all 0.5s ease;
  pointer-events: none;
}

.carousel-slide.active {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
}

.slide-icon {
  font-size: 40px;
  flex-shrink: 0;
}

.slide-content h4 {
  font-size: 18px;
  font-weight: 600;
  color: #92400e;
  margin: 0 0 4px 0;
}

.slide-content p {
  font-size: 13px;
  color: #a16207;
  margin: 0;
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(146, 64, 14, 0.3);
  border: none;
  cursor: pointer;
  padding: 0;
  transition: all 0.3s ease;
}

.dot.active {
  width: 24px;
  border-radius: 4px;
  background: #92400e;
}

/* 核心特性 */
.highlights-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.highlight-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  background: #f9fafb;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.highlight-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
}

.highlight-icon {
  font-size: 24px;
}

.highlight-text {
  font-size: 12px;
  color: #4b5563;
  font-weight: 500;
  text-align: center;
}

/* 底部信息 */
.about-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
