<template>
  <el-dialog
    v-model="visible"
    title="📤 导入导出"
    width="550px"
    class="data-dialog"
    @close="handleClose"
  >
    <div class="data-content">
      <!-- 导出区域 -->
      <div class="section export-section">
        <div class="section-header">
          <span class="section-icon">⬇️</span>
          <div>
            <h3>导出数据</h3>
            <p>导出所有书签、分类和标签为 JSON 文件</p>
          </div>
        </div>
        <el-button type="primary" @click="exportData" :loading="exportLoading">
          <el-icon><Download /></el-icon>
          导出书签
        </el-button>
      </div>

      <!-- 导入区域 -->
      <div class="section import-section">
        <div class="section-header">
          <span class="section-icon">⬆️</span>
          <div>
            <h3>导入数据</h3>
            <p>支持从 Chrome、Edge 浏览器导入</p>
          </div>
        </div>
        
        <div class="import-controls">
          <el-select v-model="importType" placeholder="选择导入源" style="width: 160px">
            <el-option label="🌐 Chrome" value="CHROME" />
            <el-option label="🌊 Edge" value="EDGE" />
            <el-option label="📄 JSON" value="JSON" />
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
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
          </el-upload>
        </div>

        <!-- 导入提示 -->
        <div v-if="importType" class="import-tip">
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
        <div v-if="selectedFile" class="selected-file">
          <el-icon><Document /></el-icon>
          <span class="file-name">{{ selectedFile.name }}</span>
          <el-button type="primary" size="small" @click="doImport" :loading="importLoading">
            开始导入
          </el-button>
          <el-button size="small" @click="selectedFile = null">取消</el-button>
        </div>

        <!-- 导入结果 -->
        <div v-if="importResult" class="import-result" :class="{ success: importResult.successCount > 0 }">
          <p class="result-main">
            ✅ 成功导入 {{ importResult.successCount }} 个书签
            <span v-if="importResult.categoriesCreated > 0">，创建 {{ importResult.categoriesCreated }} 个分类</span>
          </p>
          <p v-if="importResult.skippedCount > 0" class="result-sub">
            跳过 {{ importResult.skippedCount }} 个重复项
          </p>
        </div>
      </div>

      <!-- 危险区域 -->
      <div class="section danger-section">
        <div class="section-header">
          <span class="section-icon">🗑️</span>
          <div>
            <h3>清除所有数据</h3>
            <p>此操作不可撤销，将删除所有数据</p>
          </div>
        </div>
        <el-button type="danger" @click="clearAll">清除</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Download, Upload, Document } from '@element-plus/icons-vue';
import { downloadExportData, importBookmarksFileAPI, clearAllDataAPI } from '../api/dataManagement';

const props = defineProps({
  modelValue: Boolean,
});

const emit = defineEmits(['update:modelValue', 'data-changed']);

const visible = ref(props.modelValue);
const importType = ref('');
const selectedFile = ref(null);
const importLoading = ref(false);
const exportLoading = ref(false);
const importResult = ref(null);

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal;
}, { immediate: true });

watch(visible, (newVal) => {
  emit('update:modelValue', newVal);
});

const handleClose = () => {
  emit('update:modelValue', false);
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
    emit('data-changed');
  } catch (error) {
    ElMessage.error('导入失败: ' + (error.response?.data?.message || error.message || '格式不正确'));
  } finally {
    importLoading.value = false;
  }
};

// 清除所有数据
const clearAll = async () => {
  try {
    // 第一步：确认操作
    await ElMessageBox.confirm(
      '确定要清除所有数据吗？这将删除所有书签、分类和标签，此操作不可撤销！',
      '警告',
      {
        confirmButtonText: '继续',
        cancelButtonText: '取消',
        type: 'error'
      }
    );
    
    // 第二步：输入密码
    const { value: password } = await ElMessageBox.prompt(
      '请输入您的登录密码以确认操作',
      '密码验证',
      {
        confirmButtonText: '确认清除',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入密码',
        inputValidator: (val) => {
          if (!val || !val.trim()) return '请输入密码';
          return true;
        }
      }
    );
    
    await clearAllDataAPI(password);
    ElMessage.success('所有数据已清除');
    emit('data-changed');
  } catch (error) {
    if (error !== 'cancel' && error?.action !== 'cancel') {
      if (error.response?.data?.code === 401) {
        ElMessage.error('密码错误');
      } else {
        ElMessage.error('清除失败：' + (error.response?.data?.message || error.message || '未知错误'));
      }
    }
  }
};
</script>

<style scoped>
.data-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.data-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  padding: 16px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.export-section {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
}

.import-section {
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  flex-direction: column;
  align-items: stretch;
}

.danger-section {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
}

.section-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
}

.section-icon {
  font-size: 24px;
}

.section-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.section-header p {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.import-controls {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.import-tip {
  font-size: 12px;
  color: #6b7280;
  margin-top: 10px;
}

.import-tip code {
  background: #d1fae5;
  color: #065f46;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 10px 14px;
  border-radius: 8px;
  margin-top: 12px;
  border: 1px solid #d1d5db;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.import-result {
  margin-top: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #fef3c7;
  border: 1px solid #fcd34d;
}

.import-result.success {
  background: #d1fae5;
  border-color: #6ee7b7;
}

.result-main {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  margin: 0;
}

.result-sub {
  font-size: 12px;
  color: #6b7280;
  margin: 4px 0 0 0;
}
</style>
