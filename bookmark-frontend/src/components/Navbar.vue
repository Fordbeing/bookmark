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
      <!-- 通知按钮 -->
      <div class="relative">
        <button 
          @click="toggleNotifications"
          class="p-2.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-full transition-all relative" 
          title="通知"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path>
          </svg>
          <!-- 红点徽章 -->
          <span 
            v-if="unreadCount > 0" 
            class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse"
          >
            {{ unreadCount > 9 ? '9+' : unreadCount }}
          </span>
        </button>

        <!-- 通知下拉弹窗 -->
        <transition name="dropdown">
          <div 
            v-if="showNotifications" 
            class="absolute right-0 top-12 w-80 bg-white rounded-xl shadow-2xl border border-gray-100 overflow-hidden z-50"
          >
            <!-- 弹窗头部 -->
            <div class="flex items-center justify-between px-4 py-3 bg-gradient-to-r from-blue-500 to-blue-600 text-white">
              <span class="font-semibold">消息通知</span>
              <button 
                v-if="unreadCount > 0"
                @click="markAllAsRead" 
                class="text-xs bg-white/20 hover:bg-white/30 px-2 py-1 rounded transition-all"
              >
                全部已读
              </button>
            </div>

            <!-- 通知列表 -->
            <div class="max-h-80 overflow-y-auto">
              <div v-if="notifications.length === 0" class="py-8 text-center text-gray-400">
                <span class="text-3xl mb-2 block">🔔</span>
                <p class="text-sm">暂无通知</p>
              </div>
              <div 
                v-else
                v-for="item in notifications" 
                :key="item.id"
                @click="handleNotificationClick(item)"
                :class="[
                  'px-4 py-3 border-b border-gray-50 cursor-pointer transition-all hover:bg-gray-50',
                  !item.read ? 'bg-blue-50/50' : ''
                ]"
              >
                <div class="flex items-start gap-3">
                  <span class="text-lg flex-shrink-0">{{ item.icon }}</span>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-800 line-clamp-1">{{ item.title }}</p>
                    <p class="text-xs text-gray-500 mt-1 line-clamp-2">{{ item.content }}</p>
                    <p class="text-[10px] text-gray-400 mt-1">{{ item.time }}</p>
                  </div>
                  <span v-if="!item.read" class="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0 mt-1.5"></span>
                </div>
              </div>
            </div>

            <!-- 弹窗底部 -->
            <div class="px-4 py-2 bg-gray-50 border-t border-gray-100">
              <button 
                @click="showNotifications = false"
                class="w-full text-center text-sm text-gray-500 hover:text-blue-600 transition-colors py-1"
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

  <!-- 点击外部关闭弹窗 -->
  <div 
    v-if="showNotifications" 
    class="fixed inset-0 z-20"
    @click="showNotifications = false"
  ></div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { searchBookmarksAPI } from '../api/search';

const emit = defineEmits(['toggle-settings', 'open-profile', 'search-results']);

const searchKeyword = ref('');
const searching = ref(false);
let searchTimeout = null;

// 通知相关状态
const showNotifications = ref(false);
const notifications = ref([
  {
    id: 1,
    icon: '🎉',
    title: '欢迎使用书签管理',
    content: '感谢您使用书签管理应用，祝您使用愉快！',
    time: '刚刚',
    read: false
  },
  {
    id: 2,
    icon: '💡',
    title: '小技巧',
    content: '长按书签卡片可以快速进行批量操作哦',
    time: '1小时前',
    read: false
  },
  {
    id: 3,
    icon: '🔄',
    title: '系统更新',
    content: '我们优化了页面加载速度和交互体验',
    time: '昨天',
    read: true
  }
]);

// 计算未读数量
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length;
});

// 切换通知弹窗
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value;
};

// 标记全部已读
const markAllAsRead = () => {
  notifications.value.forEach(n => n.read = true);
};

// 点击通知项
const handleNotificationClick = (item) => {
  item.read = true;
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
</style>