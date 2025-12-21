<template>
  <el-drawer
    v-model="visible"
    title="📊 数据统计"
    direction="rtl"
    size="500px"
    :close-on-click-modal="true"
  >
    <div class="analytics-page" v-loading="loading">
      <!-- 概览卡片 -->
      <div class="overview-cards">
        <div class="stat-card">
          <div class="stat-icon">📚</div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.totalBookmarks || 0 }}</div>
            <div class="stat-label">总书签数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📈</div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.weeklyAdded || 0 }}</div>
            <div class="stat-label">本周新增</div>
          </div>
        </div>
        <div class="stat-card warning">
          <div class="stat-icon">⚠️</div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.deadLinks || 0 }}</div>
            <div class="stat-label">失效链接</div>
          </div>
        </div>
        <div class="stat-card highlight">
          <div class="stat-icon">📌</div>
          <div class="stat-content">
            <div class="stat-value">{{ overview.pinnedCount || 0 }}</div>
            <div class="stat-label">置顶书签</div>
          </div>
        </div>
      </div>

      <!-- 域名分布 -->
      <div class="chart-section">
        <h3>🌐 域名分布 TOP 10</h3>
        <div v-if="domainData.length > 0" class="domain-list">
          <div 
            v-for="(item, index) in domainData" 
            :key="item.domain" 
            class="domain-item"
          >
            <div class="domain-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <div class="domain-name">{{ item.domain }}</div>
            <div class="domain-bar-wrapper">
              <div 
                class="domain-bar" 
                :style="{ width: (item.count / maxDomainCount * 100) + '%' }"
              ></div>
            </div>
            <div class="domain-count">{{ item.count }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </div>

      <!-- 时间线 -->
      <div class="chart-section">
        <h3>📅 收藏时间线（近12个月）</h3>
        <div v-if="timelineData.length > 0" class="timeline-chart">
          <div 
            v-for="item in timelineData" 
            :key="item.month" 
            class="timeline-bar-wrapper"
          >
            <div 
              class="timeline-bar" 
              :style="{ height: (item.count / maxTimelineCount * 100) + '%' }"
              :title="item.month + ': ' + item.count + '个'"
            >
              <span v-if="item.count > 0" class="bar-value">{{ item.count }}</span>
            </div>
            <div class="timeline-label">{{ item.month.slice(5) }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无数据" :image-size="60" />
      </div>

      <!-- 常用书签 -->
      <div class="chart-section">
        <h3>🔥 常访问书签 TOP 10</h3>
        <div v-if="topVisited.length > 0" class="top-visited-list">
          <div 
            v-for="(item, index) in topVisited" 
            :key="item.id" 
            class="visited-item"
            @click="openBookmark(item)"
          >
            <div class="visited-rank">{{ index + 1 }}</div>
            <div class="visited-icon">
              <img v-if="item.iconUrl" :src="item.iconUrl" @error="(e) => e.target.style.display='none'" />
              <span v-else>{{ item.title?.charAt(0)?.toUpperCase() || '?' }}</span>
            </div>
            <div class="visited-content">
              <div class="visited-title">{{ item.title }}</div>
              <div class="visited-url">{{ item.url }}</div>
            </div>
            <div class="visited-count">{{ item.visitCount }} 次</div>
          </div>
        </div>
        <el-empty v-else description="暂无访问记录" :image-size="60" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import request from '../api/request';

const props = defineProps({
  modelValue: Boolean
});

const emit = defineEmits(['update:modelValue']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const overview = ref({});
const domainData = ref([]);
const timelineData = ref([]);
const topVisited = ref([]);

const maxDomainCount = computed(() => {
  return Math.max(...domainData.value.map(d => d.count), 1);
});

const maxTimelineCount = computed(() => {
  return Math.max(...timelineData.value.map(t => t.count), 1);
});

// 监听打开状态，加载数据
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadAllData();
  }
});

const loadAllData = async () => {
  loading.value = true;
  try {
    await Promise.all([
      loadOverview(),
      loadDomainDistribution(),
      loadTimeline(),
      loadTopVisited()
    ]);
  } finally {
    loading.value = false;
  }
};

const loadOverview = async () => {
  try {
    const result = await request.get('/analytics/overview');
    if (result.data) {
      overview.value = result.data;
    }
  } catch (error) {
    console.error('加载概览失败:', error);
  }
};

const loadDomainDistribution = async () => {
  try {
    const result = await request.get('/analytics/domain-distribution');
    if (result.data) {
      domainData.value = result.data;
    }
  } catch (error) {
    console.error('加载域名分布失败:', error);
  }
};

const loadTimeline = async () => {
  try {
    const result = await request.get('/analytics/timeline');
    if (result.data) {
      timelineData.value = result.data;
    }
  } catch (error) {
    console.error('加载时间线失败:', error);
  }
};

const loadTopVisited = async () => {
  try {
    const result = await request.get('/analytics/top-visited');
    if (result.data) {
      topVisited.value = result.data;
    }
  } catch (error) {
    console.error('加载常用书签失败:', error);
  }
};

const openBookmark = (item) => {
  window.open(item.url, '_blank');
};
</script>

<style scoped>
.analytics-page {
  padding: 0 10px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 30px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  color: white;
}

.stat-card.warning {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card.highlight {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon {
  font-size: 28px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-label {
  font-size: 12px;
  opacity: 0.8;
}

.chart-section {
  margin-bottom: 30px;
}

.chart-section h3 {
  font-size: 16px;
  margin-bottom: 15px;
  color: #333;
}

.domain-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.domain-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.domain-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #666;
}

.domain-rank.rank-1 { background: #ffd700; color: #fff; }
.domain-rank.rank-2 { background: #c0c0c0; color: #fff; }
.domain-rank.rank-3 { background: #cd7f32; color: #fff; }

.domain-name {
  width: 120px;
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.domain-bar-wrapper {
  flex: 1;
  height: 20px;
  background: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.domain-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  transition: width 0.3s ease;
}

.domain-count {
  width: 40px;
  text-align: right;
  font-size: 13px;
  font-weight: 500;
  color: #666;
}

.timeline-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 150px;
  padding: 10px 0;
}

.timeline-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.timeline-bar {
  width: 100%;
  background: linear-gradient(0deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  transition: height 0.3s ease;
}

.bar-value {
  font-size: 10px;
  color: white;
  margin-top: 2px;
}

.timeline-label {
  font-size: 10px;
  color: #999;
  margin-top: 5px;
}

.top-visited-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.visited-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.visited-item:hover {
  background: #f0f7ff;
}

.visited-rank {
  width: 20px;
  font-weight: bold;
  color: #999;
}

.visited-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.visited-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.visited-content {
  flex: 1;
  min-width: 0;
}

.visited-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.visited-url {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.visited-count {
  font-size: 12px;
  color: #667eea;
  font-weight: 500;
}
</style>
