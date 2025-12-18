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
</template>

<script setup>
import { ref } from 'vue';
import { searchBookmarksAPI } from '../api/search';

const emit = defineEmits(['toggle-settings', 'open-profile', 'search-results']);

const searchKeyword = ref('');
const searching = ref(false);
let searchTimeout = null;

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
    emit('search-results', null); // 清空搜索，显示全部
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
  }, 300); // 300ms 防抖
};
</script>

<style scoped>
</style>