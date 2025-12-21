<template>
  <nav class="bg-white/80 backdrop-blur-md shadow-sm px-6 py-3 flex items-center sticky top-0 z-30 border-b border-gray-100">
    <!-- 左侧标题 -->
    <div class="flex items-center gap-2 w-48">
      <span class="text-xl">🔖</span>
      <h1 class="text-lg font-bold text-gray-800">书签管理</h1>
    </div>

    <!-- 中间搜索框 -->
    <div class="flex-1 flex justify-center">
      <div class="relative w-full max-w-md">
        <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
        <input
          type="text"
          v-model="searchKeyword"
          @input="handleSearch"
          placeholder="搜索书签..."
          class="w-full pl-11 pr-4 py-2 border border-gray-200 rounded-full focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 bg-gray-50/50 text-sm transition-all"
        />
        <!-- 搜索加载指示器 -->
        <span v-if="searching" class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 text-xs">搜索中...</span>
      </div>
    </div>
    
    <!-- 右侧按钮 -->
    <div class="flex items-center gap-1 w-48 justify-end">
      <!-- 公告按钮 -->
      <div class="relative">
        <button 
          @click="toggleNotifications"
          class="p-2.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-full transition-all relative" 
          title="系统公告"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
          </svg>
          <!-- 新公告红点 -->
          <span 
            v-if="unreadCount > 0" 
            class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse"
          >
            {{ unreadCount > 9 ? '9+' : unreadCount }}
          </span>
        </button>

        <!-- 公告下拉弹窗 -->
        <transition name="dropdown">
          <div 
            v-if="showNotifications" 
            class="absolute right-0 top-12 w-96 bg-white rounded-xl shadow-2xl border border-gray-100 overflow-hidden z-50"
          >
            <!-- 弹窗头部 -->
            <div class="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-blue-500 to-blue-600 text-white">
              <span class="font-semibold flex items-center gap-2">
                <span class="text-lg">📢</span>
                系统公告
              </span>
              <button 
                v-if="unreadCount > 0"
                @click="markAllAsRead" 
                class="text-xs bg-white/20 hover:bg-white/30 px-2 py-1 rounded transition-all"
              >
                全部已读
              </button>
            </div>

            <!-- 公告列表 -->
            <div class="max-h-96 overflow-y-auto">
              <!-- 加载中 -->
              <div v-if="loadingAnnouncements" class="py-8 text-center text-gray-400">
                <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-2"></div>
                <p class="text-sm">加载中...</p>
              </div>
              <!-- 无公告 -->
              <div v-else-if="announcements.length === 0" class="py-8 text-center text-gray-400">
                <span class="text-3xl mb-2 block">📭</span>
                <p class="text-sm">暂无公告</p>
              </div>
              <!-- 公告列表 -->
              <div 
                v-else
                v-for="item in announcements" 
                :key="item.id"
                @click="openAnnouncementDetail(item)"
                :class="[
                  'px-4 py-4 border-b border-gray-50 cursor-pointer transition-all hover:bg-blue-50/50',
                  !isRead(item.id) ? 'bg-blue-50/30' : ''
                ]"
              >
                <div class="flex items-start gap-3">
                  <!-- 公告类型图标 -->
                  <span class="text-2xl flex-shrink-0">{{ getTypeIcon(item.type) }}</span>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <el-tag :type="getTypeTagType(item.type)" size="small" effect="plain">
                        {{ getTypeLabel(item.type) }}
                      </el-tag>
                      <span v-if="!isRead(item.id)" class="w-2 h-2 bg-red-500 rounded-full"></span>
                    </div>
                    <p class="text-sm font-semibold text-gray-800 line-clamp-1">{{ item.title }}</p>
                    <p class="text-xs text-gray-500 mt-1 line-clamp-2">{{ item.content }}</p>
                    <p class="text-[10px] text-gray-400 mt-2">{{ formatTime(item.createTime) }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 弹窗底部 -->
            <div class="px-4 py-2 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
              <button 
                @click="refreshAnnouncements"
                class="text-sm text-blue-600 hover:text-blue-800 transition-colors flex items-center gap-1"
              >
                <span class="text-sm">🔄</span> 刷新
              </button>
              <button 
                @click="showNotifications = false"
                class="text-sm text-gray-500 hover:text-gray-700 transition-colors"
              >
                关闭
              </button>
            </div>
          </div>
        </transition>
      </div>

      <button 
        @click="handleSettingsClick"
        class="p-2.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-full transition-all" 
        title="设置"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
        </svg>
      </button>
      <button 
        @click="emit('open-profile')"
        class="p-2.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-full transition-all" 
        title="个人资料"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
        </svg>
      </button>
    </div>
  </nav>

  <!-- 公告详情弹窗 -->
  <el-dialog
    v-model="showAnnouncementDetail"
    :title="currentAnnouncement?.title || '公告详情'"
    width="500px"
    :close-on-click-modal="true"
  >
    <div v-if="currentAnnouncement" class="announcement-detail">
      <div class="flex items-center gap-2 mb-4">
        <el-tag :type="getTypeTagType(currentAnnouncement.type)" effect="plain">
          {{ getTypeLabel(currentAnnouncement.type) }}
        </el-tag>
        <span class="text-sm text-gray-400">{{ formatTime(currentAnnouncement.createTime) }}</span>
      </div>
      <div class="text-gray-700 leading-relaxed whitespace-pre-wrap">
        {{ currentAnnouncement.content }}
      </div>
    </div>
    <template #footer>
      <el-button @click="showAnnouncementDetail = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 点击外部关闭弹窗 -->
  <div 
    v-if="showNotifications" 
    class="fixed inset-0 z-20"
    @click="showNotifications = false"
  ></div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { searchBookmarksAPI } from '../api/search';
import { getAnnouncementsAPI } from '../api/announcement';

const emit = defineEmits(['toggle-settings', 'open-profile', 'search-results']);

const searchKeyword = ref('');
const searching = ref(false);
let searchTimeout = null;

// 公告相关状态
const showNotifications = ref(false);
const showAnnouncementDetail = ref(false);
const currentAnnouncement = ref(null);
const announcements = ref([]);
const loadingAnnouncements = ref(false);
const readIds = ref(JSON.parse(localStorage.getItem('readAnnouncementIds') || '[]'));

// 轮询定时器
let pollingTimer = null;
const POLLING_INTERVAL = 60000; // 60秒轮询一次

// 计算未读数量
const unreadCount = computed(() => {
  return announcements.value.filter(a => !isRead(a.id)).length;
});

// 判断是否已读
const isRead = (id) => {
  return readIds.value.includes(id);
};

// 标记已读
const markAsRead = (id) => {
  if (!readIds.value.includes(id)) {
    readIds.value.push(id);
    localStorage.setItem('readAnnouncementIds', JSON.stringify(readIds.value));
  }
};

// 标记全部已读
const markAllAsRead = () => {
  const allIds = announcements.value.map(a => a.id);
  readIds.value = [...new Set([...readIds.value, ...allIds])];
  localStorage.setItem('readAnnouncementIds', JSON.stringify(readIds.value));
};

// 获取公告类型图标
const getTypeIcon = (type) => {
  const icons = {
    'info': 'ℹ️',
    'warning': '⚠️',
    'maintenance': '🔧',
    'update': '🎉'
  };
  return icons[type] || '📢';
};

// 获取公告类型标签类型
const getTypeTagType = (type) => {
  const types = {
    'info': 'info',
    'warning': 'warning',
    'maintenance': 'danger',
    'update': 'success'
  };
  return types[type] || 'info';
};

// 获取公告类型标签
const getTypeLabel = (type) => {
  const labels = {
    'info': '通知',
    'warning': '警告',
    'maintenance': '维护',
    'update': '更新'
  };
  return labels[type] || '公告';
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return `${date.getMonth() + 1}月${date.getDate()}日`;
};

// 切换通知弹窗
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value;
  if (showNotifications.value && announcements.value.length === 0) {
    fetchAnnouncements();
  }
};

// 获取公告列表
const fetchAnnouncements = async () => {
  loadingAnnouncements.value = true;
  try {
    const result = await getAnnouncementsAPI(10);
    if (result.data) {
      const oldIds = new Set(announcements.value.map(a => a.id));
      announcements.value = result.data;
      
      // 检查是否有新公告
      const newAnnouncements = result.data.filter(a => !oldIds.has(a.id) && !isRead(a.id));
      if (newAnnouncements.length > 0 && oldIds.size > 0) {
        // 有新公告时可以显示提示
        console.log('有新公告:', newAnnouncements.length);
      }
    }
  } catch (error) {
    console.error('获取公告失败:', error);
  } finally {
    loadingAnnouncements.value = false;
  }
};

// 刷新公告
const refreshAnnouncements = () => {
  fetchAnnouncements();
};

// 打开公告详情
const openAnnouncementDetail = (item) => {
  currentAnnouncement.value = item;
  showAnnouncementDetail.value = true;
  markAsRead(item.id);
};

// 开始轮询
const startPolling = () => {
  fetchAnnouncements();
  pollingTimer = setInterval(fetchAnnouncements, POLLING_INTERVAL);
};

// 停止轮询
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
};

const handleSettingsClick = () => {
  emit('toggle-settings');
};

// 防抖搜索
const handleSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout);
  }
  
  const keyword = searchKeyword.value.trim();
  
  if (!keyword) {
    emit('search-results', null);
    return;
  }
  
  searchTimeout = setTimeout(async () => {
    searching.value = true;
    try {
      const result = await searchBookmarksAPI(keyword);
      if (result.data) {
        emit('search-results', result.data);
      }
    } catch (error) {
      console.error('搜索失败:', error);
    } finally {
      searching.value = false;
    }
  }, 300);
};

onMounted(() => {
  startPolling();
});

onUnmounted(() => {
  stopPolling();
});
</script>

<style scoped>
/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.announcement-detail {
  min-height: 100px;
}
</style>