<template>
  <el-dialog
    v-model="visible"
    title="分享分类"
    width="450px"
    :close-on-click-modal="true"
  >
    <div class="share-modal">
      <!-- 未创建分享时 -->
      <template v-if="!shareInfo">
        <div class="share-form">
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
              <span class="info-label">密码保护</span>
              <span class="info-value">{{ shareInfo.hasPassword ? '是' : '否' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">有效期至</span>
              <span class="info-value">{{ shareInfo.expireTime || '永久有效' }}</span>
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
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="danger" @click="cancelShare">取消分享</el-button>
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
  categoryId: Number,
  categoryName: String
});

const emit = defineEmits(['update:modelValue', 'shared']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const password = ref('');
const expireDays = ref(null);
const shareInfo = ref(null);
const shareUrl = ref('');

// 重置状态
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    shareInfo.value = null;
    password.value = '';
    expireDays.value = null;
  }
});

// 创建分享
const createShare = async () => {
  if (!props.categoryId) {
    ElMessage.error('请选择要分享的分类');
    return;
  }

  loading.value = true;
  try {
    const result = await request.post('/share/create', {
      categoryId: props.categoryId,
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
    ElMessage.error('创建分享失败');
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
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
    ElMessage.success('链接已复制');
  }
};

// 取消分享
const cancelShare = async () => {
  // 这里需要shareId，暂时关闭弹窗
  visible.value = false;
  ElMessage.info('请在"我的分享"中管理分享链接');
};
</script>

<style scoped>
.share-modal {
  padding: 10px 0;
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
