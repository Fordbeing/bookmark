<template>
  <el-drawer
    v-model="visible"
    title="⚠️ 失效链接管理"
    direction="rtl"
    size="450px"
    :close-on-click-modal="true"
  >
    <div class="dead-links-page">
      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" @click="triggerCheck" :loading="checking">
          {{ checking ? '检测中...' : '🔄 立即检测' }}
        </el-button>
        <el-button 
          type="danger" 
          @click="batchDelete" 
          :disabled="deadLinks.length === 0"
        >
          🗑️ 批量删除 ({{ deadLinks.length }})
        </el-button>
      </div>

      <!-- 提示 -->
      <div class="info-tip">
        <el-alert 
          title="系统每天凌晨自动检测所有书签链接状态"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <el-empty 
        v-else-if="deadLinks.length === 0" 
        description="🎉 太棒了！没有失效链接"
        :image-size="100"
      />

      <!-- 失效链接列表 -->
      <div v-else class="dead-links-list">
        <div 
          v-for="item in deadLinks" 
          :key="item.id" 
          class="dead-link-item"
        >
          <div class="item-header">
            <div class="item-icon">
              <img v-if="item.iconUrl" :src="item.iconUrl" @error="(e) => e.target.style.display='none'" />
              <span v-else>{{ item.title?.charAt(0)?.toUpperCase() || '?' }}</span>
            </div>
            <div class="item-content">
              <div class="item-title">{{ item.title }}</div>
              <div class="item-url">{{ item.url }}</div>
            </div>
          </div>
          
          <div class="item-status">
            <el-tag type="danger" size="small">
              {{ item.checkMessage || '链接失效' }}
            </el-tag>
            <span class="check-time" v-if="item.lastCheckTime">
              {{ formatTime(item.lastCheckTime) }}
            </span>
          </div>

          <div class="item-actions">
            <el-button size="small" @click="recheckLink(item)">重新检测</el-button>
            <el-button size="small" type="primary" @click="openLink(item)">尝试打开</el-button>
            <el-button size="small" type="danger" @click="deleteLink(item)">删除</el-button>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getDeadLinksAPI, deleteBookmarkAPI } from '../api/bookmark';
import request from '../api/request';

const props = defineProps({
  modelValue: Boolean
});

const emit = defineEmits(['update:modelValue', 'refresh']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const checking = ref(false);
const deadLinks = ref([]);

// 监听打开状态
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadDeadLinks();
  }
});

// 加载失效链接
const loadDeadLinks = async () => {
  loading.value = true;
  try {
    const result = await getDeadLinksAPI();
    if (result.data) {
      deadLinks.value = result.data;
    }
  } catch (error) {
    console.error('加载失效链接失败:', error);
  } finally {
    loading.value = false;
  }
};

// 触发检测
const triggerCheck = async () => {
  checking.value = true;
  try {
    await request.post('/bookmarks/health/check');
    ElMessage.success('检测任务已启动，请稍后刷新查看结果');
  } catch (error) {
    console.error('触发检测失败:', error);
    ElMessage.error('触发检测失败');
  } finally {
    checking.value = false;
  }
};

// 重新检测单个链接
const recheckLink = async (item) => {
  try {
    const result = await request.post(`/bookmarks/health/check/${item.id}`);
    if (result.data) {
      ElMessage.info(`检测结果: ${result.data.message}`);
      loadDeadLinks();
    }
  } catch (error) {
    console.error('检测失败:', error);
  }
};

// 打开链接
const openLink = (item) => {
  window.open(item.url, '_blank');
};

// 删除链接
const deleteLink = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除书签"${item.title}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteBookmarkAPI(item.id);
    ElMessage.success('已删除');
    loadDeadLinks();
    emit('refresh');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
    }
  }
};

// 批量删除
const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定删除全部 ${deadLinks.value.length} 个失效链接吗？此操作不可恢复！`, 
      '警告', 
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    // 逐个删除
    for (const item of deadLinks.value) {
      await deleteBookmarkAPI(item.id);
    }
    
    ElMessage.success('已删除所有失效链接');
    deadLinks.value = [];
    emit('refresh');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error);
    }
  }
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};
</script>

<style scoped>
.dead-links-page {
  padding: 0 10px;
}

.action-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.info-tip {
  margin-bottom: 20px;
}

.loading-state {
  padding: 20px 0;
}

.dead-links-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.dead-link-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 15px;
  transition: all 0.2s;
}

.dead-link-item:hover {
  border-color: #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  flex-shrink: 0;
}

.item-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  opacity: 0.5;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-url {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.check-time {
  font-size: 12px;
  color: #999;
}

.item-actions {
  display: flex;
  gap: 8px;
}
</style>
