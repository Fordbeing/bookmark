<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.key">
        <div class="stat-icon" :style="{ background: stat.bgColor }">
          {{ stat.icon }}
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber(stat.value) }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-change" :class="stat.changeType" v-if="stat.change">
          {{ stat.change > 0 ? '+' : '' }}{{ stat.change }}%
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 用户增长趋势 -->
      <div class="card chart-card">
        <div class="card-header">
          <h3 class="card-title">用户增长趋势</h3>
          <div class="chart-controls">
            <button 
              v-for="period in periods" 
              :key="period.value"
              class="period-btn"
              :class="{ active: selectedPeriod === period.value }"
              @click="selectedPeriod = period.value"
            >
              {{ period.label }}
            </button>
          </div>
        </div>
        <div class="card-body">
          <div ref="userChartRef" class="chart-container"></div>
        </div>
      </div>

      <!-- 书签增长趋势 -->
      <div class="card chart-card">
        <div class="card-header">
          <h3 class="card-title">书签增长趋势</h3>
        </div>
        <div class="card-body">
          <div ref="bookmarkChartRef" class="chart-container"></div>
        </div>
      </div>
    </div>

    <!-- 快捷操作和最近活动 -->
    <div class="bottom-grid">
      <!-- 快捷操作 -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">快捷操作</h3>
        </div>
        <div class="card-body">
          <div class="quick-actions">
            <router-link to="/users" class="quick-action-item">
              <span class="action-icon">👥</span>
              <span class="action-text">用户管理</span>
            </router-link>
            <router-link to="/activation-codes" class="quick-action-item">
              <span class="action-icon">🎫</span>
              <span class="action-text">激活码管理</span>
            </router-link>
            <router-link to="/bookmarks" class="quick-action-item">
              <span class="action-icon">🔖</span>
              <span class="action-text">内容审核</span>
            </router-link>
            <router-link to="/settings" class="quick-action-item">
              <span class="action-icon">⚙️</span>
              <span class="action-text">系统设置</span>
            </router-link>
          </div>
        </div>
      </div>

      <!-- 系统状态 -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">系统状态</h3>
        </div>
        <div class="card-body">
          <div class="system-status">
            <div class="status-item" v-for="item in systemStatus" :key="item.name">
              <div class="status-info">
                <span class="status-dot" :class="item.status"></span>
                <span class="status-name">{{ item.name }}</span>
              </div>
              <span class="status-text" :class="item.status">{{ item.statusText }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近活动 -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">最近活动</h3>
        </div>
        <div class="card-body">
          <div class="activity-list">
            <div class="activity-item" v-for="activity in recentActivities" :key="activity.id">
              <div class="activity-icon" :style="{ background: activity.bgColor }">
                {{ activity.icon }}
              </div>
              <div class="activity-content">
                <div class="activity-text">{{ activity.text }}</div>
                <div class="activity-time">{{ activity.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/dashboard'

const userChartRef = ref(null)
const bookmarkChartRef = ref(null)
let userChart = null
let bookmarkChart = null

const selectedPeriod = ref(7)
const periods = [
  { label: '7天', value: 7 },
  { label: '30天', value: 30 }
]

// 趋势数据
const trendData = ref({
  dates: [],
  userTrend: [],
  bookmarkTrend: []
})

const stats = reactive([
  { key: 'users', icon: '👥', label: '总用户数', value: 0, change: null, changeType: '', bgColor: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
  { key: 'bookmarks', icon: '🔖', label: '总书签数', value: 0, change: null, changeType: '', bgColor: 'linear-gradient(135deg, #10b981, #34d399)' },
  { key: 'onlineUsers', icon: '🟢', label: '在线用户', value: 0, change: null, changeType: '', bgColor: 'linear-gradient(135deg, #22c55e, #4ade80)' },
  { key: 'todayUsers', icon: '📈', label: '今日新增', value: 0, change: null, changeType: '', bgColor: 'linear-gradient(135deg, #f59e0b, #fbbf24)' },
  { key: 'activeUsers', icon: '🔥', label: '活跃用户(7天)', value: 0, change: null, changeType: '', bgColor: 'linear-gradient(135deg, #ef4444, #f87171)' }
])

const systemStatus = reactive([
  { name: '后端服务', status: 'healthy', statusText: '运行正常' },
  { name: 'MySQL', status: 'healthy', statusText: '运行正常' },
  { name: 'Redis', status: 'healthy', statusText: '运行正常' },
  { name: 'Elasticsearch', status: 'healthy', statusText: '运行正常' }
])

const recentActivities = ref([])

function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

function updateUserChart() {
  if (!userChart || !trendData.value.dates.length) return
  
  userChart.setOption({
    xAxis: {
      data: trendData.value.dates
    },
    series: [{
      data: trendData.value.userTrend
    }]
  })
}

function updateBookmarkChart() {
  if (!bookmarkChart || !trendData.value.dates.length) return
  
  bookmarkChart.setOption({
    xAxis: {
      data: trendData.value.dates
    },
    series: [{
      data: trendData.value.bookmarkTrend
    }]
  })
}

function initUserChart() {
  if (!userChartRef.value) return
  
  userChart = echarts.init(userChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', fontSize: 12 }
    },
    series: [{
      data: trendData.value.userTrend,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#6366f1' },
      itemStyle: { color: '#6366f1' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(99, 102, 241, 0.3)' },
          { offset: 1, color: 'rgba(99, 102, 241, 0.05)' }
        ])
      }
    }]
  }
  
  userChart.setOption(option)
}

function initBookmarkChart() {
  if (!bookmarkChartRef.value) return
  
  bookmarkChart = echarts.init(bookmarkChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: trendData.value.dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', fontSize: 12 }
    },
    series: [{
      data: trendData.value.bookmarkTrend,
      type: 'bar',
      barWidth: '60%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#10b981' },
          { offset: 1, color: '#34d399' }
        ])
      }
    }]
  }
  
  bookmarkChart.setOption(option)
}

async function loadDashboardData() {
  try {
    const response = await dashboardApi.getOverview()
    if (response.code === 200 && response.data) {
      const data = response.data
      stats[0].value = data.totalUsers || 0
      stats[1].value = data.totalBookmarks || 0
      stats[2].value = data.onlineUsers || 0
      stats[3].value = data.todayNewUsers || 0
      stats[4].value = data.activeUsers || 0
    }
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
  }
}

async function loadTrendData() {
  try {
    const response = await dashboardApi.getTrends(selectedPeriod.value)
    if (response.code === 200 && response.data) {
      trendData.value = {
        dates: response.data.dates || [],
        userTrend: response.data.userTrend || [],
        bookmarkTrend: response.data.bookmarkTrend || []
      }
      updateUserChart()
      updateBookmarkChart()
    }
  } catch (error) {
    console.error('Failed to load trend data:', error)
  }
}

function handleResize() {
  userChart?.resize()
  bookmarkChart?.resize()
}

// 加载最近活动
async function loadRecentActivities() {
  try {
    const response = await dashboardApi.getRecentActivities(10)
    if (response.code === 200 && response.data) {
      recentActivities.value = response.data
    }
  } catch (error) {
    console.error('Failed to load recent activities:', error)
  }
}

// 活动自动刷新定时器
let activityRefreshTimer = null

watch(selectedPeriod, () => {
  loadTrendData()
})

onMounted(() => {
  loadDashboardData()
  loadRecentActivities()
  initUserChart()
  initBookmarkChart()
  loadTrendData()
  window.addEventListener('resize', handleResize)
  
  // 每30秒刷新一次活动数据
  activityRefreshTimer = setInterval(loadRecentActivities, 30000)
})

onUnmounted(() => {
  userChart?.dispose()
  bookmarkChart?.dispose()
  window.removeEventListener('resize', handleResize)
  
  // 清除定时器
  if (activityRefreshTimer) {
    clearInterval(activityRefreshTimer)
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.05), transparent);
  border-radius: 50%;
  transform: translate(30%, -30%);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-change {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 20px;
}

.stat-change.up {
  color: var(--success);
  background: var(--success-bg);
}

.stat-change.down {
  color: var(--danger);
  background: var(--danger-bg);
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-card {
  min-height: 360px;
}

.chart-controls {
  display: flex;
  gap: 8px;
}

.period-btn {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 20px;
  background: var(--bg-page);
  color: var(--text-secondary);
  transition: var(--transition);
}

.period-btn:hover {
  background: var(--border);
}

.period-btn.active {
  background: var(--primary);
  color: white;
}

.chart-container {
  width: 100%;
  height: 260px;
}

/* 底部网格 */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1.5fr;
  gap: 20px;
}

/* 快捷操作 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border-radius: var(--radius-lg);
  background: var(--bg-page);
  color: var(--text-primary);
  text-decoration: none;
  transition: var(--transition);
}

.quick-action-item:hover {
  background: var(--primary-bg);
  color: var(--primary);
  transform: translateY(-2px);
}

.action-icon {
  font-size: 28px;
}

.action-text {
  font-size: 13px;
  font-weight: 500;
}

/* 系统状态 */
.system-status {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-page);
  border-radius: var(--radius);
}

.status-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.healthy {
  background: var(--success);
  box-shadow: 0 0 8px var(--success);
}

.status-dot.warning {
  background: var(--warning);
  box-shadow: 0 0 8px var(--warning);
}

.status-dot.error {
  background: var(--danger);
  box-shadow: 0 0 8px var(--danger);
}

.status-name {
  font-size: 14px;
  font-weight: 500;
}

.status-text {
  font-size: 13px;
}

.status-text.healthy {
  color: var(--success);
}

.status-text.warning {
  color: var(--warning);
}

.status-text.error {
  color: var(--danger);
}

/* 活动列表 */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 280px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.activity-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-text {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .bottom-grid {
    grid-template-columns: 1fr 1fr;
  }
  
  .bottom-grid > .card:last-child {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
  
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
