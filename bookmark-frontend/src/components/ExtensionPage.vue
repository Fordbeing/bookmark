<template>
  <el-dialog
    v-model="visible"
    title="🔌 浏览器扩展"
    width="650px"
    class="extension-dialog"
    @close="handleClose"
  >
    <div class="dialog-scroll-content">
      <!-- 顶部介绍 - 紧凑版 -->
      <div class="intro-section">
        <div class="intro-left">
          <span class="intro-icon">🔖</span>
          <div>
            <h2>BookUtil 浏览器扩展</h2>
            <div class="feature-tags">
              <span>✨ 一键保存</span>
              <span>📁 分类</span>
              <span>🔍 搜索</span>
              <span>⌨️ 快捷键</span>
            </div>
          </div>
        </div>
        <el-button type="primary" size="large" @click="downloadExtension" class="download-btn">
          <el-icon><Download /></el-icon>
          下载扩展
        </el-button>
      </div>

      <!-- 浏览器选择标签 -->
      <el-tabs v-model="activeBrowser" class="browser-tabs">
        <!-- Chrome -->
        <el-tab-pane name="chrome">
          <template #label>
            <div class="browser-tab-label">
              <img src="https://www.google.com/chrome/static/images/chrome-logo-m100.svg" alt="Chrome" class="browser-icon" />
              <span>Chrome</span>
            </div>
          </template>
          
          <div class="steps-container">
            <div class="step-row">
              <span class="step-num">1</span>
              <div class="step-info">
                <strong>解压下载的 ZIP 文件</strong>到任意文件夹
              </div>
            </div>
            <div class="step-row">
              <span class="step-num">2</span>
              <div class="step-info">
                <span>打开 </span>
                <code class="url-code">chrome://extensions/</code>
                <el-button size="small" link @click="copyText('chrome://extensions/')" title="复制">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
                <el-button size="small" type="primary" @click="openExtensionPage('chrome')" class="jump-btn">
                  前往 →
                </el-button>
              </div>
            </div>
            <div class="step-row">
              <span class="step-num">3</span>
              <div class="step-info">
                <strong>开启右上角「开发者模式」</strong>开关
              </div>
            </div>
            <div class="step-row">
              <span class="step-num">4</span>
              <div class="step-info">
                点击<strong>「加载已解压的扩展程序」</strong>，选择解压的文件夹
              </div>
            </div>
            <div class="step-row success">
              <span class="step-num">✓</span>
              <div class="step-info">
                <strong>完成！</strong>扩展图标会出现在工具栏，建议固定显示
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- Edge -->
        <el-tab-pane name="edge">
          <template #label>
            <div class="browser-tab-label">
              <span class="edge-icon">🌊</span>
              <span>Edge</span>
            </div>
          </template>
          
          <div class="steps-container">
            <div class="step-row">
              <span class="step-num">1</span>
              <div class="step-info">
                <strong>解压下载的 ZIP 文件</strong>到任意文件夹
              </div>
            </div>
            <div class="step-row">
              <span class="step-num">2</span>
              <div class="step-info">
                <span>打开 </span>
                <code class="url-code">edge://extensions/</code>
                <el-button size="small" link @click="copyText('edge://extensions/')" title="复制">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
                <el-button size="small" type="primary" @click="openExtensionPage('edge')" class="jump-btn">
                  前往 →
                </el-button>
              </div>
            </div>
            <div class="step-row">
              <span class="step-num">3</span>
              <div class="step-info">
                <strong>开启左下角「开发人员模式」</strong>开关
              </div>
            </div>
            <div class="step-row">
              <span class="step-num">4</span>
              <div class="step-info">
                点击<strong>「加载解压缩的扩展」</strong>，选择解压的文件夹
              </div>
            </div>
            <div class="step-row success">
              <span class="step-num">✓</span>
              <div class="step-info">
                <strong>完成！</strong>点击工具栏拼图图标可固定扩展
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <!-- 快捷方式 - 紧凑版 -->
      <div class="shortcuts-section">
        <div class="shortcut-item">
          <kbd>Ctrl</kbd><span>+</span><kbd>Shift</kbd><span>+</span><kbd>S</kbd>
          <span class="shortcut-desc">快速保存</span>
        </div>
        <div class="shortcut-item">
          <span class="shortcut-icon">🖱️</span>
          <span class="shortcut-desc">右键 → 保存到 BookUtil</span>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Download, CopyDocument } from '@element-plus/icons-vue';

const props = defineProps({
  modelValue: Boolean,
});

const emit = defineEmits(['update:modelValue']);

const visible = ref(props.modelValue);
const activeBrowser = ref('chrome');

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal;
}, { immediate: true });

watch(visible, (newVal) => {
  emit('update:modelValue', newVal);
});

const handleClose = () => {
  emit('update:modelValue', false);
};

const downloadExtension = () => {
  const link = document.createElement('a');
  link.href = '/bookmark-extension.zip';
  link.download = 'bookmark-extension.zip';
  link.click();
  ElMessage.success('开始下载，请解压后按教程安装');
};

const copyText = (text) => {
  // 降级方案：使用 textarea 复制
  const fallbackCopy = () => {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.top = '0';
    textarea.style.left = '0';
    textarea.style.width = '2em';
    textarea.style.height = '2em';
    textarea.style.padding = '0';
    textarea.style.border = 'none';
    textarea.style.outline = 'none';
    textarea.style.boxShadow = 'none';
    textarea.style.background = 'transparent';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
      const successful = document.execCommand('copy');
      if (successful) {
        ElMessage.success('已复制');
      } else {
        ElMessage.error('复制失败，请手动复制');
      }
    } catch (e) {
      ElMessage.error('复制失败，请手动复制');
    }
    document.body.removeChild(textarea);
  };

  // 优先尝试现代 Clipboard API，如果不支持则使用降级方案
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      ElMessage.success('已复制');
    }).catch(() => {
      fallbackCopy();
    });
  } else {
    // HTTP 环境下直接使用降级方案
    fallbackCopy();
  }
};

// 打开扩展管理页面
const openExtensionPage = (browser) => {
  const url = browser === 'chrome' ? 'chrome://extensions/' : 'edge://extensions/';
  // 复制到剪贴板
  copyText(url);
  // 由于浏览器安全限制，无法直接打开 chrome:// 或 edge:// 协议
  ElMessage({
    message: '由于浏览器安全限制，无法自动打开。地址已复制，请手动粘贴到地址栏打开。',
    type: 'warning',
    duration: 5000
  });
};
</script>

<style scoped>
.extension-dialog :deep(.el-dialog__body) {
  padding: 16px 20px 20px;
  max-height: 70vh;
  overflow: hidden;
}

.dialog-scroll-content {
  max-height: calc(70vh - 80px);
  overflow-y: auto;
  padding-right: 8px;
}

/* 顶部介绍 */
.intro-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.intro-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.intro-icon {
  font-size: 32px;
  background: rgba(255, 255, 255, 0.2);
  padding: 10px;
  border-radius: 12px;
}

.intro-left h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 6px 0;
}

.feature-tags {
  display: flex;
  gap: 8px;
  font-size: 11px;
  opacity: 0.9;
}

.feature-tags span {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
}

.download-btn {
  flex-shrink: 0;
}

/* 浏览器标签 */
.browser-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.browser-tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.browser-icon {
  width: 20px;
  height: 20px;
}

.edge-icon {
  font-size: 18px;
}

/* 步骤容器 */
.steps-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border-left: 3px solid #6366f1;
}

.step-row.success {
  border-left-color: #10b981;
  background: #ecfdf5;
}

.step-num {
  width: 26px;
  height: 26px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.step-row.success .step-num {
  background: linear-gradient(135deg, #10b981, #059669);
}

.step-info {
  flex: 1;
  font-size: 14px;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.url-code {
  background: #1e293b;
  color: #a5f3fc;
  padding: 3px 10px;
  border-radius: 6px;
  font-family: 'Consolas', monospace;
  font-size: 13px;
}

.jump-btn {
  margin-left: 4px;
  font-size: 12px;
}

/* 快捷方式 */
.shortcuts-section {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  padding: 12px;
  background: #f1f5f9;
  border-radius: 10px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.shortcut-item kbd {
  background: linear-gradient(180deg, #fff 0%, #e2e8f0 100%);
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  box-shadow: 0 1px 0 #94a3b8;
}

.shortcut-icon {
  font-size: 16px;
}

.shortcut-desc {
  color: #475569;
}

/* 滚动条样式 */
.dialog-scroll-content::-webkit-scrollbar {
  width: 6px;
}

.dialog-scroll-content::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.dialog-scroll-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.dialog-scroll-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
