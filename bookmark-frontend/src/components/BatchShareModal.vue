<template>
  <el-dialog
    v-model="visible"
    title="🔗 批量分享书签"
    width="500px"
    :close-on-click-modal="true"
  >
    <div class="batch-share-modal">
      <!-- 未创建分享时 -->
      <template v-if="!shareInfo">
        <div class="selected-info">
          <div class="info-header">
            <span class="info-icon">📚</span>
            <span>已选择 <strong>{{ bookmarkIds.length }}</strong> 个书签</span>
          </div>
          <div class="bookmark-preview" v-if="selectedBookmarks.length > 0">
            <div 
              v-for="bookmark in selectedBookmarks.slice(0, 5)" 
              :key="bookmark.id"
              class="preview-item"
            >
              <span class="preview-title">{{ bookmark.title }}</span>
            </div>
            <div v-if="selectedBookmarks.length > 5" class="preview-more">
              +{{ selectedBookmarks.length - 5 }} 更多...
            </div>
          </div>
        </div>

        <el-divider />

        <div class="share-form">
          <div class="form-item">
            <label>分享标题</label>
            <el-input 
              v-model="shareTitle" 
              placeholder="输入分享标题（可选）"
            />
          </div>
          <div class="form-item">
            <label>设置密码（可选）</label>
            <el-input 
              v-model="password" 
              type="password" 
              placeholder="不设置则公开访问"
              show-password
            />
          </div>
          <div class="form-item">
            <label>有效期</label>
            <el-select v-model="expireDays" style="width: 100%">
              <el-option label="永久有效" :value="null" />
              <el-option label="1天" :value="1" />
              <el-option label="7天" :value="7" />
              <el-option label="30天" :value="30" />
              <el-option label="90天" :value="90" />
            </el-select>
          </div>
        </div>
      </template>

      <!-- 已创建分享时 -->
      <template v-else>
        <div class="share-result">
          <div class="share-success-icon">🎉</div>
          <p class="share-success-text">分享链接已生成</p>
          
          <div class="share-link-box">
            <el-input 
              v-model="shareUrl" 
              readonly
              class="share-link-input"
            >
              <template #append>
                <el-button @click="copyLink">复制</el-button>
              </template>
            </el-input>
          </div>

          <div class="share-info">
            <div class="info-item">
              <span class="info-label">包含书签</span>
              <span class="info-value">{{ savedBookmarkCount }} 个</span>
            </div>
            <div class="info-item">
              <span class="info-label">密码保护</span>
              <span class="info-value">{{ password ? '是' : '否' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">有效期</span>
              <span class="info-value">{{ expireDays ? expireDays + '天' : '永久有效' }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <template v-if="!shareInfo">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="createShare" :loading="loading">
          生成分享链接
        </el-button>
      </template>
      <template v-else>
        <el-button @click="handleClose">关闭</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import request from '../api/request';

const props = defineProps({
  modelValue: Boolean,
  bookmarkIds: {
    type: Array,
    default: () => []
  },
  selectedBookmarks: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue', 'shared']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const shareTitle = ref('');
const password = ref('');
const expireDays = ref(null);
const shareInfo = ref(null);
const shareUrl = ref('');
const savedBookmarkCount = ref(0); // 保存书签数量，因为父组件可能清空选中

// 重置状态
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    shareInfo.value = null;
    shareTitle.value = '';
    password.value = '';
    expireDays.value = null;
    savedBookmarkCount.value = props.bookmarkIds?.length || 0; // 保存当前选中数量
  }
});

// 创建批量分享
const createShare = async () => {
  if (!props.bookmarkIds || props.bookmarkIds.length === 0) {
    ElMessage.error('请选择要分享的书签');
    return;
  }

  loading.value = true;
  try {
    const result = await request.post('/share/batch', {
      bookmarkIds: props.bookmarkIds,
      title: shareTitle.value || '分享的书签',
      password: password.value || null,
      expireDays: expireDays.value
    });

    if (result.data) {
      shareInfo.value = result.data;
      shareUrl.value = window.location.origin + result.data.shareUrl;
      ElMessage.success('分享链接已生成');
      emit('shared');
    }
  } catch (error) {
    console.error('创建分享失败:', error);
    ElMessage.error(error.response?.data?.message || '创建分享失败');
  } finally {
    loading.value = false;
  }
};

// 复制链接
const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value);
    ElMessage.success('链接已复制');
  } catch (error) {
    // 降级方案
    const textarea = document.createElement('textarea');
    textarea.value = shareUrl.value;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    try {
      document.execCommand('copy');
      ElMessage.success('链接已复制');
    } catch (e) {
      ElMessage.error('复制失败，请手动复制');
    }
    document.body.removeChild(textarea);
  }
};

// 关闭并通知父组件
const handleClose = () => {
  visible.value = false;
};
</script>

<style scoped>
.batch-share-modal {
  padding: 10px 0;
}

.selected-info {
  background: linear-gradient(135deg, #e0f2fe, #dbeafe);
  border-radius: 12px;
  padding: 16px;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  color: #1e40af;
  margin-bottom: 12px;
}

.info-icon {
  font-size: 24px;
}

.bookmark-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.preview-item {
  background: rgba(255, 255, 255, 0.8);
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}

.preview-title {
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.preview-more {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
  padding: 6px;
}

.share-form .form-item {
  margin-bottom: 20px;
}

.share-form .form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.share-result {
  text-align: center;
}

.share-success-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.share-success-text {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.share-link-box {
  margin-bottom: 20px;
}

.share-link-input :deep(.el-input__inner) {
  background: #f5f7fa;
}

.share-info {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 15px;
  text-align: left;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.info-item:not(:last-child) {
  border-bottom: 1px solid #eee;
}

.info-label {
  color: #666;
}

.info-value {
  color: #333;
  font-weight: 500;
}
</style>
