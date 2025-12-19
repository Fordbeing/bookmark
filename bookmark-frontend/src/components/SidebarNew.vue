<template>
  <aside
    :class="[
      'sidebar h-screen fixed left-0 top-0 flex flex-col transition-all duration-300 z-40',
      isCollapsed ? 'w-[72px]' : 'w-[280px]'
    ]"
    :style="{ 
      background: `linear-gradient(180deg, ${customColorFrom} 0%, ${customColorTo} 100%)`
    }"
  >
    <!-- 顶部 Logo 区域 -->
    <div class="relative px-4 py-5">
      <!-- 折叠按钮 -->
      <button
        @click="toggleCollapse"
        class="absolute top-4 w-8 h-8 flex items-center justify-center rounded-lg bg-white/20 hover:bg-white/30 transition-all text-white z-50"
        :class="isCollapsed ? 'right-1/2 translate-x-1/2' : 'right-3'"
      >
        <span class="text-sm font-bold">{{ isCollapsed ? '›' : '‹' }}</span>
      </button>
      
      <div v-if="!isCollapsed" class="flex items-center gap-3 pr-10">
        <div class="w-11 h-11 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center shadow-lg">
          <span class="text-2xl">🔖</span>
        </div>
        <div>
          <h1 class="text-lg font-bold text-white tracking-wide">书签管理</h1>
          <p class="text-[11px] text-white/60 font-medium">Bookmark Manager</p>
        </div>
      </div>
      <div v-else class="flex justify-center">
        <div class="w-11 h-11 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
          <span class="text-xl">🔖</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div v-if="!isCollapsed && showStats" class="px-4 pb-4">
      <div class="bg-white/10 backdrop-blur-sm rounded-2xl p-4 border border-white/10">
        <div class="grid grid-cols-2 gap-4">
          <div class="text-center">
            <div class="text-3xl font-bold text-white">{{ totalBookmarks }}</div>
            <div class="text-[11px] text-white/50 mt-1 uppercase tracking-wider">书签总数</div>
          </div>
          <div class="text-center">
            <div class="text-3xl font-bold text-white">{{ categoryCount }}</div>
            <div class="text-[11px] text-white/50 mt-1 uppercase tracking-wider">分类数量</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主导航区域 -->
    <nav class="flex-1 overflow-y-auto px-3 py-2 space-y-1 custom-scrollbar">
      <!-- 快速筛选 -->
      <div v-if="!isCollapsed" class="mb-3">
        <div 
          class="flex items-center gap-2 px-3 py-2 text-xs text-white/50 font-bold cursor-pointer hover:text-white/70 transition-colors select-none"
          @click="quickActionsExpanded = !quickActionsExpanded"
        >
          <span class="w-4 text-center transition-transform duration-200 text-lg" :class="{ 'rotate-90': quickActionsExpanded }">›</span>
          快速筛选
        </div>
        <div v-show="quickActionsExpanded" class="space-y-0.5 mt-1">
          <button 
            @click="showAllBookmarks"
            :class="[
              'w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
              currentFilter === 'all' 
                ? 'bg-white/20 text-white shadow-lg' 
                : 'text-white/80 hover:bg-white/10 hover:text-white'
            ]"
          >
            <span class="w-5 text-center">📚</span>
            <span>全部书签</span>
            <span class="ml-auto text-xs bg-white/20 px-2 py-0.5 rounded-full">{{ totalBookmarks }}</span>
          </button>
          <button 
            @click="showFavorites"
            :class="[
              'w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
              currentFilter === 'favorite' 
                ? 'bg-white/20 text-white shadow-lg' 
                : 'text-white/80 hover:bg-white/10 hover:text-white'
            ]"
          >
            <span class="w-5 text-center">⭐</span>
            <span>收藏夹</span>
          </button>
          <button 
            @click="showTrash"
            :class="[
              'w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
              currentFilter === 'trash' 
                ? 'bg-white/20 text-white shadow-lg' 
                : 'text-white/80 hover:bg-white/10 hover:text-white'
            ]"
          >
            <span class="w-5 text-center">🗑️</span>
            <span>回收站</span>
          </button>
        </div>
      </div>

      <!-- 分类 -->
      <div v-if="!isCollapsed" class="mb-3">
        <div 
          class="flex items-center justify-between px-3 py-2 text-xs text-white/50 font-bold cursor-pointer hover:text-white/70 transition-colors select-none"
          @click="categoryExpanded = !categoryExpanded"
        >
          <div class="flex items-center gap-2">
            <span class="w-4 text-center transition-transform duration-200 text-lg" :class="{ 'rotate-90': categoryExpanded }">›</span>
            分类
          </div>
          <button 
            @click.stop="emit('add-category')"
            class="w-6 h-6 flex items-center justify-center rounded-md bg-white/15 hover:bg-white/25 text-white transition-all text-sm font-bold"
          >+</button>
        </div>
        <div v-show="categoryExpanded" class="space-y-0.5 mt-1">
          <button 
            v-for="category in categories" 
            :key="category.id"
            @click="selectCategory(category.id)"
            :class="[
              'w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
              selectedCategoryId === category.id && currentFilter === 'category'
                ? 'bg-white/20 text-white shadow-lg' 
                : 'text-white/80 hover:bg-white/10 hover:text-white'
            ]"
          >
            <span class="w-5 text-center">{{ category.icon || '📁' }}</span>
            <span class="flex-1 text-left truncate">{{ category.name }}</span>
            <span class="text-xs bg-white/15 px-2 py-0.5 rounded-full">{{ category.count || 0 }}</span>
          </button>
          <div v-if="categories.length === 0" class="px-4 py-3 text-sm text-white/40 text-center">
            暂无分类
          </div>
        </div>
      </div>

      <!-- 更多功能 -->
      <div v-if="!isCollapsed" class="mb-3">
        <div 
          class="flex items-center gap-2 px-3 py-2 text-xs text-white/50 font-bold cursor-pointer hover:text-white/70 transition-colors select-none"
          @click="otherExpanded = !otherExpanded"
        >
          <span class="w-4 text-center transition-transform duration-200 text-lg" :class="{ 'rotate-90': otherExpanded }">›</span>
          更多功能
        </div>
        <div v-show="otherExpanded" class="space-y-0.5 mt-1">
          <button class="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-all duration-200">
            <span class="w-5 text-center">📊</span>
            <span>数据统计</span>
            <span class="ml-auto text-[10px] bg-amber-400 text-amber-900 px-1.5 py-0.5 rounded font-bold">SOON</span>
          </button>
          <button class="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-all duration-200">
            <span class="w-5 text-center">📤</span>
            <span>导入导出</span>
            <span class="ml-auto text-[10px] bg-amber-400 text-amber-900 px-1.5 py-0.5 rounded font-bold">SOON</span>
          </button>
          <button class="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-all duration-200">
            <span class="w-5 text-center">🔌</span>
            <span>浏览器插件</span>
            <span class="ml-auto text-[10px] bg-amber-400 text-amber-900 px-1.5 py-0.5 rounded font-bold">SOON</span>
          </button>
        </div>
      </div>

      <!-- 折叠模式按钮 -->
      <div v-if="isCollapsed" class="space-y-1">
        <button
          @click="showAllBookmarks"
          :class="[
            'w-full p-3 rounded-xl flex justify-center transition-all',
            currentFilter === 'all' ? 'bg-white/20' : 'hover:bg-white/10'
          ]"
          title="全部书签"
        ><span class="text-lg">📚</span></button>
        <button
          @click="showFavorites"
          :class="[
            'w-full p-3 rounded-xl flex justify-center transition-all',
            currentFilter === 'favorite' ? 'bg-white/20' : 'hover:bg-white/10'
          ]"
          title="收藏夹"
        ><span class="text-lg">⭐</span></button>
        <button
          @click="showTrash"
          :class="[
            'w-full p-3 rounded-xl flex justify-center transition-all',
            currentFilter === 'trash' ? 'bg-white/20' : 'hover:bg-white/10'
          ]"
          title="回收站"
        ><span class="text-lg">🗑️</span></button>
        <div class="h-px bg-white/10 my-2"></div>
        <button
          v-for="category in categories.slice(0, 5)" 
          :key="category.id"
          @click="selectCategory(category.id)"
          :class="[
            'w-full p-3 rounded-xl flex justify-center transition-all',
            selectedCategoryId === category.id && currentFilter === 'category' ? 'bg-white/20' : 'hover:bg-white/10'
          ]"
          :title="category.name"
        ><span class="text-lg">{{ category.icon || '📁' }}</span></button>
      </div>
    </nav>

    <!-- 底部操作区域 -->
    <div class="mt-auto px-3 py-4 border-t border-white/10">
      <div v-if="!isCollapsed" class="space-y-1">
        <button 
          @click="emit('open-settings')"
          class="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-all duration-200"
        >
          <span class="w-5 text-center">⚙️</span>
          <span>设置</span>
        </button>
        <button 
          @click="emit('open-profile')"
          class="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white transition-all duration-200"
        >
          <span class="w-5 text-center">👤</span>
          <span>个人中心</span>
        </button>
      </div>
      <div v-else class="space-y-1">
        <button
          @click="emit('open-settings')"
          class="w-full p-3 rounded-xl flex justify-center hover:bg-white/10 transition-all"
          title="设置"
        ><span class="text-lg">⚙️</span></button>
        <button
          @click="emit('open-profile')"
          class="w-full p-3 rounded-xl flex justify-center hover:bg-white/10 transition-all"
          title="个人中心"
        ><span class="text-lg">👤</span></button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { getCategoryListAPI } from '../api/category';

const emit = defineEmits(['open-settings', 'open-profile', 'category-select', 'filter-favorites', 'filter-trash', 'add-category']);

// 定义props以接收书签数据
const props = defineProps({
  bookmarks: {
    type: Array,
    default: () => []
  },
  showStats: {
    type: Boolean,
    default: true
  }
});

const isCollapsed = ref(false);
const customColorFrom = ref('#2563eb');
const customColorTo = ref('#1e3a8a');
const categories = ref([]);
const selectedCategoryId = ref(null);
const currentFilter = ref('all'); // 'all', 'category', 'favorite', 'trash'

// 可收缩区域状态
const categoryExpanded = ref(true);
const quickActionsExpanded = ref(true);
const otherExpanded = ref(false);

// 计算总书签数
const totalBookmarks = computed(() => props.bookmarks.length);

// 计算分类数量
const categoryCount = computed(() => categories.value.length);

// 加载分类列表
const loadCategories = async () => {
  try {
    const result = await getCategoryListAPI();
    if (result.data) {
      categories.value = result.data.map(cat => {
        // 计算每个分类的书签数量
        const count = props.bookmarks.filter(b => b.categoryId === cat.id).length;
        return {
          ...cat,
          count
        };
      });
    }
  } catch (error) {
    console.error('加载分类失败:', error);
  }
};

// 点击分类过滤
const selectCategory = (categoryId) => {
  selectedCategoryId.value = categoryId;
  currentFilter.value = 'category';
  emit('category-select', categoryId);
};

// 点击所有书签
const showAllBookmarks = () => {
  selectedCategoryId.value = null;
  currentFilter.value = 'all';
  emit('category-select', null);
};

// 点击星标书签
const showFavorites = () => {
  currentFilter.value = 'favorite';
  emit('filter-favorites');
};

// 点击回收站
const showTrash = () => {
  currentFilter.value = 'trash';
  emit('filter-trash');
};

// 从 localStorage 加载颜色并加载分类
onMounted(async () => {
  const savedColorFrom = localStorage.getItem('sidebarColorFrom');
  const savedColorTo = localStorage.getItem('sidebarColorTo');
  if (savedColorFrom) customColorFrom.value = savedColorFrom;
  if (savedColorTo) customColorTo.value = savedColorTo;
  
  // 只在有 token 时加载分类列表
  const token = localStorage.getItem('token');
  if (token) {
    await loadCategories();
  }
});

// 监听书签变化，重新计算分类数量
watch(() => props.bookmarks, () => {
  if (categories.value.length > 0) {
    categories.value = categories.value.map(cat => ({
      ...cat,
      count: props.bookmarks.filter(b => b.categoryId === cat.id).length
    }));
  }
}, { deep: true });

// 监听颜色变化（来自 App.vue）
watch(() => customColorFrom.value, (newVal) => {
  if (newVal) localStorage.setItem('sidebarColorFrom', newVal);
});

watch(() => customColorTo.value, (newVal) => {
  if (newVal) localStorage.setItem('sidebarColorTo', newVal);
});

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value;
};

defineExpose({
  isCollapsed,
  customColorFrom,
  customColorTo,
  loadCategories,
  currentFilter
});
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
