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

      <!-- 数据管理 -->
      <el-tab-pane label="💾 数据管理" name="data">
        <div class="space-y-4">
          <!-- 导出区域 -->
          <div class="bg-blue-50 p-4 rounded-lg">
            <h3 class="font-medium text-gray-800 mb-2">⬇️ 导出数据</h3>
            <p class="text-xs text-gray-600 mb-3">导出所有书签、分类和标签为 JSON 文件</p>
            <el-button type="primary" @click="exportData" :loading="exportLoading">
              <el-icon class="mr-1"><Download /></el-icon>导出书签
            </el-button>
          </div>

          <!-- 导入区域 -->
          <div class="bg-green-50 p-4 rounded-lg">
            <h3 class="font-medium text-gray-800 mb-2">⬆️ 导入数据</h3>
            <p class="text-xs text-gray-600 mb-3">支持从 Chrome、Edge 浏览器或本应用导出的 JSON 格式导入</p>
            
            <div class="flex gap-3 items-center mb-3">
              <el-select v-model="importType" placeholder="选择导入源" style="width: 180px">
                <el-option label="🌐 Chrome 浏览器" value="CHROME" />
                <el-option label="🌊 Edge 浏览器" value="EDGE" />
                <el-option label="📄 JSON 格式" value="JSON" />
              </el-select>
              <el-upload
                ref="uploadRef"
                action="#"
                :accept="importType === 'JSON' ? '.json' : '.html'"
                :auto-upload="false"
                :on-change="handleFileChange"
                :show-file-list="false"
              >
                <el-button type="success" :disabled="!importType">
                  <el-icon class="mr-1"><Upload /></el-icon>选择文件
                </el-button>
              </el-upload>
            </div>

            <!-- 导入提示 -->
            <div v-if="importType" class="text-xs text-gray-500 mb-2">
              <template v-if="importType === 'CHROME'">
                💡 在 Chrome 中打开 <code>chrome://bookmarks</code> → … → 导出书签
              </template>
              <template v-else-if="importType === 'EDGE'">
                💡 在 Edge 中打开 <code>edge://favorites</code> → … → 导出收藏夹
              </template>
              <template v-else>
                💡 选择本应用之前导出的 JSON 备份文件
              </template>
            </div>

            <!-- 已选文件 -->
            <div v-if="selectedFile" class="flex items-center gap-2 bg-white p-2 rounded border mt-2">
              <el-icon><Document /></el-icon>
              <span class="flex-1 truncate text-sm">{{ selectedFile.name }}</span>
              <el-button type="primary" size="small" @click="doImport" :loading="importLoading">
                开始导入
              </el-button>
              <el-button size="small" @click="selectedFile = null">取消</el-button>
            </div>

            <!-- 导入结果 -->
            <div v-if="importResult" class="mt-3 p-3 rounded border" :class="importResult.successCount > 0 ? 'bg-green-100 border-green-300' : 'bg-yellow-100 border-yellow-300'">
              <p class="text-sm font-medium">
                ✅ 成功导入 {{ importResult.successCount }} 个书签
                <span v-if="importResult.categoriesCreated > 0">，创建 {{ importResult.categoriesCreated }} 个分类</span>
              </p>
              <p v-if="importResult.skippedCount > 0" class="text-xs text-gray-600 mt-1">
                跳过 {{ importResult.skippedCount }} 个重复项
              </p>
            </div>
          </div>

          <!-- 清除数据 -->
          <div class="bg-red-50 p-4 rounded-lg flex justify-between items-center">
            <div>
              <h3 class="font-medium text-red-800">🗑️ 清除所有数据</h3>
              <p class="text-xs text-red-600">此操作不可撤销，将删除所有书签、分类和标签</p>
            </div>
            <el-button type="danger" size="small" @click="clearAll">清除</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 关于 -->
      <el-tab-pane label="ℹ️ 关于" name="about">
        <div class="space-y-4 text-center">
          <div class="text-4xl">🔖</div>
          <h2 class="text-2xl font-bold">书签管理工具</h2>
          <p class="text-gray-600">v1.0.0</p>

          <el-divider />

          <div class="text-left space-y-2">
            <p><strong>功能：</strong> 智能管理和组织你的书签</p>
            <p><strong>开发者：</strong> Bookmark Team</p>
            <p><strong>许可证：</strong> MIT</p>
            <p><strong>更新日期：</strong> 2024-01-01</p>
          </div>

          <el-divider />

          <div class="space-y-2">
            <el-button link>访问官网</el-button>
            <el-button link>报告问题</el-button>
            <el-button link>隐私政策</el-button>
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
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { UploadFilled, Download, Upload, Document } from '@element-plus/icons-vue';
import { getTagListAPI, createTagAPI, updateTagAPI, deleteTagAPI } from '../api/tag';
import { getSettingsAPI, updateSettingsAPI } from '../api/settings';
import { downloadExportData, importBookmarksFileAPI, clearAllDataAPI } from '../api/dataManagement';

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

// 数据管理相关
const importType = ref('');
const selectedFile = ref(null);
const importLoading = ref(false);
const exportLoading = ref(false);
const importResult = ref(null);

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
</style>
