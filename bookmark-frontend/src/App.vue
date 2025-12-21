<template>
  <!-- 登录/注册页面 -->
  <AuthPage 
    v-if="currentPage === 'auth'"
    @login-success="handleLoginSuccess"
    @back="currentPage = 'main'"
  />

  <!-- 主应用页面 -->
  <div v-else id="app" class="flex min-h-screen bg-gray-50 font-sans">
    <Sidebar 
      ref="sidebarRef" 
      @open-settings="isSettingsVisible = true" 
      @open-profile="handleProfileClick" 
      @open-extension="isExtensionVisible = true"
      @open-data-management="isDataManagementVisible = true"
      @category-select="handleCategoryFilter"
      @filter-favorites="handleFavoriteFilter"
      @filter-trash="handleTrashFilter"
      @add-category="isCategoryModalVisible = true"
      @open-advanced-search="isAdvancedSearchVisible = true"
      @open-analytics="isAnalyticsVisible = true"
      @open-dead-links="isDeadLinksVisible = true"
      @share-category="openShareModal"
      :bookmarks="bookmarks"
      :showStats="showStats"
      :allBookmarksCount="allBookmarksCount"
    />
    <div class="flex-1 transition-all duration-300 ease-in-out" :style="{ marginLeft: sidebarMargin }">
      <Navbar @toggle-settings="isSettingsVisible = true" @open-profile="handleProfileClick" @search-results="handleSearchResults" />
      <div class="p-8 max-w-6xl mx-auto">

        <!-- 快速添加区域 -->
        <div class="bg-white rounded-xl shadow-md p-6 mb-8 border border-gray-100">
          <div class="flex gap-4 items-center">
            <el-input 
              v-model="inputUrl" 
              placeholder="粘贴链接：https://example.com" 
              size="large"
              clearable
              @keyup.enter="handleAdd"
              class="flex-1"
            >
              <template #prefix>
                <el-icon><Link /></el-icon>
              </template>
            </el-input>
            
            <el-button 
              type="primary" 
              size="large" 
              :loading="loading" 
              @click="handleAdd"
              class="!px-10"
            >
              {{ loading ? '添加中...' : '立即添加' }}
            </el-button>

            <el-button 
              @click="toggleAddModal"
              size="large"
              class="!px-8"
            >
              手工添加
            </el-button>
          </div>
        </div>

        <!-- 书签展示区域 -->
        <div>
          <!-- 标题栏 -->
          <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-gray-800">
              {{ currentFilter.type === 'trash' ? '🗑️ 回收站' : 
                 currentFilter.type === 'favorite' ? '⭐ 星标书签' : '我的书签' }}
            </h2>
            <div class="flex gap-3 items-center">
              <!-- 批量操作 -->
              <template v-if="currentFilter.type !== 'trash'">
                <el-button v-if="!batchMode" size="small" @click="batchMode = true">
                  批量选择
                </el-button>
                <template v-else>
                  <el-button size="small" @click="selectAllBookmarks">
                    {{ selectedIds.length === filteredBookmarks.length ? '取消全选' : '全选' }}
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small" 
                    :disabled="selectedIds.length === 0"
                    @click="batchDeleteBookmarks"
                  >
                    删除选中 ({{ selectedIds.length }})
                  </el-button>
                  <el-button size="small" @click="exitBatchMode">取消</el-button>
                </template>
              </template>
              <!-- 排序选择 -->
              <div v-if="currentFilter.type !== 'trash' && !batchMode" class="flex items-center gap-2">
                <span class="text-sm text-gray-500">排序:</span>
                <el-select v-model="sortBy" size="small" style="width: 110px">
                  <el-option label="创建时间" value="createTime" />
                  <el-option label="访问次数" value="visitCount" />
                  <el-option label="标题" value="title" />
                </el-select>
                <el-button size="small" @click="sortOrder = sortOrder === 'asc' ? 'desc' : 'asc'">
                  {{ sortOrder === 'asc' ? '↑ 升序' : '↓ 降序' }}
                </el-button>
              </div>
              <el-tag type="info">
                共 {{ currentFilter.type === 'trash' ? trashBookmarks.length : filteredBookmarks.length }} 个
              </el-tag>
              <!-- 回收站清空按钮 -->
              <el-button 
                v-if="currentFilter.type === 'trash' && trashBookmarks.length > 0"
                type="danger" 
                size="small"
                @click="handleClearTrash"
              >
                清空回收站
              </el-button>
            </div>
          </div>

          <el-skeleton v-if="initLoading" :rows="3" animated />
          
          <!-- 回收站视图 -->
          <template v-else-if="currentFilter.type === 'trash'">
            <el-empty v-if="trashBookmarks.length === 0" description="回收站是空的" />
            <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              <div 
                v-for="item in trashBookmarks" 
                :key="item.id" 
                class="relative bg-gray-100 rounded-xl p-6 shadow-md border border-gray-200 hover:shadow-lg hover:-translate-y-1 transition-all duration-300"
              >
                <div class="flex items-start gap-4 mb-4">
                  <div class="w-12 h-12 rounded-lg bg-gray-400 flex items-center justify-center flex-shrink-0 text-white text-lg font-bold">
                    {{ item.title?.charAt(0)?.toUpperCase() || '?' }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-lg font-bold text-gray-600 line-clamp-2">{{ item.title || item.url }}</h3>
                    <p class="text-xs text-gray-500 mt-1 truncate">{{ item.url }}</p>
                  </div>
                </div>
                <p class="text-sm text-gray-500 line-clamp-2 mb-4">{{ item.description || '暂无描述...' }}</p>
                <div class="flex justify-end gap-2">
                  <el-button type="success" size="small" @click="handleRestoreBookmark(item.id)">恢复</el-button>
                  <el-button type="danger" size="small" @click="handlePermanentDelete(item.id)">永久删除</el-button>
                </div>
              </div>
            </div>
          </template>

          <!-- 正常书签视图 -->
          <el-empty v-else-if="filteredBookmarks.length === 0" description="没有找到符合条件的书签" />

          <!-- 卡片视图 -->
          <div v-else-if="displayMode === 'card'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div 
              v-for="item in filteredBookmarks" 
              :key="item.id" 
              class="relative bg-white rounded-xl p-5 shadow-md hover:shadow-xl hover:-translate-y-1 transition-all duration-300 cursor-pointer border border-gray-100 flex flex-col"
              :class="{ 'ring-2 ring-blue-500': selectedIds.includes(item.id) }"
              @click="batchMode ? toggleSelect(item.id) : openUrl(item.url, item.id)"
            >
              <!-- 批量选择复选框 -->
              <div v-if="batchMode" class="absolute top-3 right-3 z-10" @click.stop>
                <el-checkbox 
                  :model-value="selectedIds.includes(item.id)" 
                  @change="toggleSelect(item.id)"
                />
              </div>
              <!-- 头部：图标和标题 -->
              <div class="flex items-center gap-3 mb-3">
                <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center flex-shrink-0 text-white font-bold">
                  <img v-if="item.iconUrl" :src="item.iconUrl" class="w-full h-full object-cover rounded-lg" @error="(e) => e.target.style.display='none'" />
                  <span v-else>{{ item.title?.charAt(0)?.toUpperCase() || '?' }}</span>
                </div>
                <div class="min-w-0 flex-1">
                  <h3 class="font-bold text-gray-800 line-clamp-1 hover:text-blue-600 transition-colors">{{ item.title || item.url }}</h3>
                  <p class="text-xs text-gray-400 truncate">{{ item.url }}</p>
                </div>
                <span v-if="item.isPinned === 1" class="text-lg" title="已置顶">📌</span>
                <span v-if="item.isFavorite === 1" class="text-lg">⭐</span>
              </div>
              <!-- 描述 -->
              <p class="text-sm text-gray-600 line-clamp-2 mb-3 flex-1">{{ item.description || '暂无描述...' }}</p>
              <!-- 标签 -->
              <div class="flex flex-wrap gap-1 mb-3" v-if="parseTags(item.tags).length > 0">
                <el-tag v-for="tag in parseTags(item.tags)" :key="tag" size="small" type="info" effect="plain" round class="!text-xs">{{ tag }}</el-tag>
              </div>
              <!-- 底部：时间和操作按钮 -->
              <div class="flex justify-between items-center text-xs text-gray-400 border-t pt-3 mt-auto">
                <div class="flex items-center gap-2">
                  <span class="flex items-center gap-1"><el-icon><Clock /></el-icon>{{ formatDate(item.createTime) }}</span>
                  <span v-if="item.visitCount" class="flex items-center gap-1">👁 {{ item.visitCount }}</span>
                </div>
                <div class="flex gap-1">
                  <el-button 
                    :type="item.isPinned === 1 ? 'warning' : 'default'" 
                    size="small" 
                    plain 
                    @click.stop="togglePinBookmark(item)"
                    :title="item.isPinned === 1 ? '取消置顶' : '置顶'"
                  >
                    {{ item.isPinned === 1 ? '📌' : '📍' }}
                  </el-button>
                  <el-button type="primary" size="small" plain @click.stop="copyUrl(item.url)">复制</el-button>
                  <el-button size="small" @click.stop="editBookmark(item)">编辑</el-button>
                  <el-button type="danger" size="small" plain @click.stop="deleteBookmark(item.id)">删除</el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 列表视图 -->
          <div v-else-if="displayMode === 'list'" class="space-y-2">
            <div 
              v-for="item in filteredBookmarks" 
              :key="item.id" 
              class="bg-white rounded-lg p-4 shadow-sm hover:shadow-md transition-all cursor-pointer border border-gray-100"
              @click="openUrl(item.url, item.id)"
            >
              <!-- 第一行：标题和操作 -->
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-3 flex-1 min-w-0">
                  <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center flex-shrink-0 text-white text-sm font-bold">
                    {{ item.title?.charAt(0)?.toUpperCase() || '?' }}
                  </div>
                  <div class="min-w-0 flex-1">
                    <h3 class="font-bold text-gray-800 truncate hover:text-blue-600">{{ item.title || item.url }}</h3>
                  </div>
                </div>
                <div class="flex gap-1 items-center">
                  <el-button type="primary" size="small" plain @click.stop="copyUrl(item.url)">📋 复制</el-button>
                  <el-button size="small" @click.stop="editBookmark(item)">✏️ 编辑</el-button>
                  <el-button type="danger" size="small" plain @click.stop="deleteBookmark(item.id)">🗑️ 删除</el-button>
                </div>
              </div>
              <!-- 第二行：URL和标签 -->
              <div class="flex items-center gap-3 text-sm">
                <span class="text-gray-400 truncate max-w-md">{{ item.url }}</span>
                <div class="flex gap-1" v-if="parseTags(item.tags).length > 0">
                  <el-tag v-for="tag in parseTags(item.tags).slice(0, 3)" :key="tag" size="small" type="info" effect="plain" round class="!text-xs">{{ tag }}</el-tag>
                </div>
                <span class="ml-auto text-xs text-gray-400 flex items-center gap-2">
                  <span v-if="item.visitCount">👁 {{ item.visitCount }}</span>
                  <span>{{ formatDate(item.createTime) }}</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 紧凑视图 -->
          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-2">
            <div 
              v-for="item in filteredBookmarks" 
              :key="item.id" 
              class="flex items-center gap-3 bg-white rounded-lg p-3 hover:bg-blue-50 transition-all cursor-pointer border border-gray-100"
              @click="openUrl(item.url, item.id)"
            >
              <div class="w-7 h-7 rounded bg-blue-500 flex items-center justify-center flex-shrink-0 text-white text-xs font-bold">
                {{ item.title?.charAt(0)?.toUpperCase() || '?' }}
              </div>
              <div class="flex-1 min-w-0">
                <span class="text-sm font-medium text-gray-800 truncate block hover:text-blue-600">{{ item.title || item.url }}</span>
                <div class="flex gap-1 mt-1" v-if="parseTags(item.tags).length > 0">
                  <el-tag v-for="tag in parseTags(item.tags).slice(0, 2)" :key="tag" size="small" type="info" effect="plain" class="!text-[10px] !px-1">{{ tag }}</el-tag>
                </div>
              </div>
              <div class="flex gap-1 items-center flex-shrink-0">
                <span v-if="item.visitCount" class="text-xs text-gray-400 mr-1">👁 {{ item.visitCount }}</span>
                <el-button type="primary" size="small" plain @click.stop="copyUrl(item.url)">📋 复制</el-button>
                <el-button size="small" @click.stop="editBookmark(item)">✏️ 编辑</el-button>
                <el-button type="danger" size="small" plain @click.stop="deleteBookmark(item.id)">🗑️ 删除</el-button>
              </div>
            </div>
          </div>
          
          <!-- 懒加载：加载更多区域 -->
          <div v-if="currentFilter.type !== 'trash' && !initLoading && filteredBookmarks.length > 0" class="mt-8 flex flex-col items-center">
            <!-- 加载中 -->
            <div v-if="loadingMore" class="flex items-center gap-2 text-gray-500">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <!-- 加载更多按钮 -->
            <el-button 
              v-else-if="hasMore" 
              type="primary" 
              plain 
              @click="loadMore"
              class="mt-4"
            >
              加载更多 (已加载 {{ bookmarks.length }} / {{ totalBookmarks }})
            </el-button>
            <!-- 已加载全部 -->
            <div v-else class="text-gray-400 text-sm py-4">
              ✓ 已加载全部 {{ totalBookmarks }} 个书签
            </div>
          </div>
        </div>
      </div>

      <el-button 
        type="primary" 
        class="fixed bottom-8 right-8 rounded-full shadow-xl hover:shadow-2xl" 
        size="large"
        @click="toggleAddModal"
        circle
      >
        <el-icon><Plus /></el-icon>
      </el-button>

      <!-- 快速添加书签弹窗 -->
      <AddBookmarkModal 
        :visible="isModalVisible"
        @update:visible="isModalVisible = $event"
        @close="toggleModal" 
        @add-bookmark="handleBookmarkAdded" 
      />

      <!-- 手工添加书签弹窗 -->
      <AddBookmarkModal2 
        :visible="isAddPageModalVisible"
        @update:visible="isAddPageModalVisible = $event"
        @add-bookmark="handleBookmarkAdded" 
      />

      <!-- 编辑书签弹窗 -->
      <EditBookmarkModal 
        :visible="isEditModalVisible"
        :bookmark="editingBookmark"
        @update:visible="isEditModalVisible = $event"
        @save="handleBookmarkUpdated"
        @delete="handleBookmarkDeleted"
        @close="isEditModalVisible = false"
      />

      <!-- 设置页面 -->
      <SettingsPage 
        v-model="isSettingsVisible"
        @update-settings="handleSettingsUpdate"
      />

      <!-- 个人资料页面 -->
      <ProfilePage 
        v-model="isProfileVisible"
        :bookmarks="bookmarks"
        :categories="categories"
        @logout="handleLogout"
      />

      <!-- 浏览器扩展页面 -->
      <ExtensionPage 
        v-model="isExtensionVisible"
      />

      <!-- 数据管理页面 -->
      <DataManagementPage 
        v-model="isDataManagementVisible"
        @data-changed="handleDataChanged"
      />

      <!-- 添加分类弹窗 -->
      <AddCategoryModal 
        v-model="isCategoryModalVisible"
        @success="handleCategoryAdded"
      />

      <!-- 高级搜索抽屉 -->
      <AdvancedSearchDrawer 
        v-model="isAdvancedSearchVisible"
        @select="handleBookmarkSelect"
      />

      <!-- 数据统计抽屉 -->
      <AnalyticsDrawer 
        v-model="isAnalyticsVisible"
      />

      <!-- 失效链接管理 -->
      <DeadLinksDrawer 
        v-model="isDeadLinksVisible"
        @refresh="fetchList"
      />

      <!-- 分享弹窗 -->
      <ShareModal 
        v-model="isShareModalVisible"
        :categoryId="sharingCategoryId"
        :categoryName="sharingCategoryName"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { 
  getBookmarkListAPI, 
  createBookmarkAPI, 
  updateBookmarkAPI, 
  deleteBookmarkAPI,
  batchDeleteBookmarksAPI,
  getTrashBookmarksAPI,
  restoreBookmarkAPI,
  permanentDeleteBookmarkAPI,
  clearTrashAPI,
  recordBookmarkVisitAPI,
  pinBookmarkAPI,
  unpinBookmarkAPI
} from './api/bookmark';
import { getCategoryListAPI } from './api/category';
import { ElMessage, ElMessageBox } from 'element-plus';
import Navbar from "./components/Navbar.vue";
import Sidebar from "./components/SidebarNew.vue";
import AddBookmarkModal from "./components/AddBookmarkModal.vue";
import AddBookmarkModal2 from "./components/AddBookmarkModal2.vue";
import EditBookmarkModal from "./components/EditBookmarkModal.vue";
import SettingsPage from "./components/SettingsPage.vue";
import AuthPage from "./components/AuthPage.vue";
import ProfilePage from "./components/ProfilePage.vue";
import AddCategoryModal from "./components/AddCategoryModal.vue";
import ExtensionPage from "./components/ExtensionPage.vue";
import DataManagementPage from "./components/DataManagementPage.vue";
import AdvancedSearchDrawer from "./components/AdvancedSearchDrawer.vue";
import AnalyticsDrawer from "./components/AnalyticsDrawer.vue";
import DeadLinksDrawer from "./components/DeadLinksDrawer.vue";
import ShareModal from "./components/ShareModal.vue";

const currentPage = ref('main'); // 'main' 或 'auth'
const sidebarRef = ref(null);
const sidebarCollapsed = ref(false);
const inputUrl = ref('');
const loading = ref(false);
const initLoading = ref(true);
const isModalVisible = ref(false);
const isAddPageModalVisible = ref(false);
const isEditModalVisible = ref(false);
const isSettingsVisible = ref(false);
const isProfileVisible = ref(false);
const isCategoryModalVisible = ref(false);
const isExtensionVisible = ref(false);
const isDataManagementVisible = ref(false);
const isAdvancedSearchVisible = ref(false);
const isAnalyticsVisible = ref(false);
const isDeadLinksVisible = ref(false);
const isShareModalVisible = ref(false);
const sharingCategoryId = ref(null);
const sharingCategoryName = ref('');
const editingBookmark = ref(null);

// 显示设置
const displayMode = ref(localStorage.getItem('displayMode') || 'card'); // card, list, compact
const autoOpenNewTab = ref(localStorage.getItem('autoOpenNewTab') !== 'false');
const showStats = ref(localStorage.getItem('showStats') !== 'false');

// 排序设置
const sortBy = ref('createTime'); // createTime, visitCount, title
const sortOrder = ref('desc'); // asc, desc

// 批量选择
const batchMode = ref(false);
const selectedIds = ref([]);

// 回收站数据
const trashBookmarks = ref([]);

// 搜索结果
const searchResults = ref(null); // null 表示不在搜索模式

// 监听侧边栏折叠状态
watch(() => sidebarRef.value?.isCollapsed, (val) => {
  if (val !== undefined) {
    sidebarCollapsed.value = val;
  }
}, { deep: true, immediate: true });

// 计算侧边栏外边距（动态）
const sidebarMargin = computed(() => {
  return sidebarCollapsed.value ? '72px' : '280px';
});
// 使用真实API的书签数据
const bookmarks = ref([]);
const categories = ref([]);
const currentFilter = ref({ type: 'all', value: null }); // 'all', 'category', 'favorite'

// 懒加载分页状态
const bookmarkPage = ref(1);
const pageSize = ref(50);
const hasMore = ref(true);
const loadingMore = ref(false);
const totalBookmarks = ref(0);
const allBookmarksCount = ref(0); // 全部书签总数（不受过滤影响，用于侧边栏统计）

// 过滤和排序书签
const filteredBookmarks = computed(() => {
  // 如果有搜索结果，直接返回搜索结果
  if (searchResults.value !== null) {
    return searchResults.value;
  }
  
  // 服务器端已经过滤了分类和收藏，直接使用 bookmarks
  let result = bookmarks.value;
  
  // 排序（客户端排序）
  result = [...result].sort((a, b) => {
    let valA, valB;
    if (sortBy.value === 'createTime') {
      valA = new Date(a.createTime || 0).getTime();
      valB = new Date(b.createTime || 0).getTime();
    } else if (sortBy.value === 'visitCount') {
      valA = a.visitCount || 0;
      valB = b.visitCount || 0;
    } else if (sortBy.value === 'title') {
      valA = (a.title || '').toLowerCase();
      valB = (b.title || '').toLowerCase();
      return sortOrder.value === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);
    }
    return sortOrder.value === 'asc' ? valA - valB : valB - valA;
  });
  
  return result;
});

// 处理分类过滤
const handleCategoryFilter = (categoryId) => {
  // 清空搜索结果
  searchResults.value = null;
  
  if (categoryId === null) {
    console.log('显示所有书签');
    currentFilter.value = { type: 'all', value: null };
  } else {
    console.log('过滤分类:', categoryId);
    currentFilter.value = { type: 'category', value: categoryId };
  }
  // 重新从服务器加载（带分类筛选）
  fetchList(true);
};

// 处理星标过滤
const handleFavoriteFilter = () => {
  console.log('显示星标书签');
  searchResults.value = null;
  currentFilter.value = { type: 'favorite', value: null };
  // 重新从服务器加载（带收藏筛选）
  fetchList(true);
};

// 处理回收站过滤
const handleTrashFilter = async () => {
  console.log('显示回收站');
  currentFilter.value = { type: 'trash', value: null };
  await loadTrashBookmarks();
};

// 加载回收站书签
const loadTrashBookmarks = async () => {
  try {
    const result = await getTrashBookmarksAPI();
    if (result.data) {
      trashBookmarks.value = result.data;
    }
  } catch (error) {
    console.error('加载回收站失败:', error);
  }
};

// 处理搜索结果
const handleSearchResults = (results) => {
  if (results === null) {
    // 清空搜索，恢复正常显示
    searchResults.value = null;
    currentFilter.value = { type: 'all', value: null };
  } else {
    // 显示搜索结果
    searchResults.value = results;
    currentFilter.value = { type: 'search', value: null };
  }
};

// 恢复书签
const handleRestoreBookmark = async (id) => {
  try {
    await restoreBookmarkAPI(id);
    ElMessage.success('书签已恢复');
    await loadTrashBookmarks();
    await fetchList();
  } catch (error) {
    console.error('恢复书签失败:', error);
  }
};

// 永久删除书签
const handlePermanentDelete = async (id) => {
  try {
    await ElMessageBox.confirm('永久删除后无法恢复，确定要删除吗？', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await permanentDeleteBookmarkAPI(id);
    ElMessage.success('书签已永久删除');
    await loadTrashBookmarks();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('永久删除失败:', error);
    }
  }
};

// 清空回收站
const handleClearTrash = async () => {
  try {
    await ElMessageBox.confirm('清空回收站后所有书签将永久删除，无法恢复！', '警告', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await clearTrashAPI();
    ElMessage.success('回收站已清空');
    trashBookmarks.value = [];
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空回收站失败:', error);
    }
  }
};

// 置顶/取消置顶书签
const togglePinBookmark = async (item) => {
  try {
    if (item.isPinned === 1) {
      await unpinBookmarkAPI(item.id);
      item.isPinned = 0;
      ElMessage.success('已取消置顶');
    } else {
      await pinBookmarkAPI(item.id);
      item.isPinned = 1;
      ElMessage.success('已置顶');
    }
    // 重新加载列表以更新排序
    await fetchList();
  } catch (error) {
    console.error('置顶操作失败:', error);
    ElMessage.error('操作失败');
  }
};

// 分类添加成功后刷新分类列表
const handleCategoryAdded = () => {
  if (sidebarRef.value) {
    sidebarRef.value.loadCategories();
  }
};

// 从搜索结果选中书签
const handleBookmarkSelect = (bookmark) => {
  console.log('选中书签:', bookmark);
};

// 打开分享弹窗
const openShareModal = (categoryId, categoryName) => {
  sharingCategoryId.value = categoryId;
  sharingCategoryName.value = categoryName;
  isShareModalVisible.value = true;
};

// 数据变更后刷新列表
const handleDataChanged = () => {
  fetchList();
  if (sidebarRef.value) {
    sidebarRef.value.loadCategories();
  }
};

const toggleModal = () => {
  isModalVisible.value = !isModalVisible.value;
};

const toggleAddModal = () => {
  isAddPageModalVisible.value = !isAddPageModalVisible.value;
};

const handleAdd = async () => {
  if (!inputUrl.value) return ElMessage.warning('请输入网址');
  let finalUrl = inputUrl.value;
  if (!finalUrl.startsWith('http')) finalUrl = 'http://' + finalUrl;

  loading.value = true;
  try {
    await createBookmarkAPI({
      title: finalUrl.split('/')[2] || '新书签',
      url: finalUrl,
      description: '快速添加的书签'
    });
    ElMessage.success('添加成功');
    inputUrl.value = '';
    fetchList(); // 重新加载列表
  } catch (error) {
    console.error(error);
    ElMessage.error('添加失败');
  } finally {
    loading.value = false;
  }
};

const fetchList = async (reset = true) => {
  try {
    if (reset) {
      initLoading.value = true;
      bookmarkPage.value = 1;
      bookmarks.value = [];
      hasMore.value = true;
    }
    
    // 构建 API 参数（根据当前过滤条件）
    const params = { 
      page: bookmarkPage.value, 
      size: pageSize.value 
    };
    
    // 根据过滤类型添加参数
    if (currentFilter.value.type === 'category' && currentFilter.value.value) {
      params.categoryId = currentFilter.value.value;
    } else if (currentFilter.value.type === 'favorite') {
      params.isFavorite = 1;
    }
    
    const result = await getBookmarkListAPI(params);
    if (result.data && result.data.list) {
      if (reset) {
        bookmarks.value = result.data.list;
      } else {
        bookmarks.value = [...bookmarks.value, ...result.data.list];
      }
      totalBookmarks.value = result.data.total || result.data.list.length;
      // 判断是否还有更多数据
      hasMore.value = bookmarks.value.length < totalBookmarks.value;
      
      // 当加载全部书签（非过滤状态）时，更新总数统计
      if (currentFilter.value.type === 'all' && reset) {
        allBookmarksCount.value = totalBookmarks.value;
      }
    }
    // 同时加载分类列表
    if (reset) {
      const categoryResult = await getCategoryListAPI();
      if (categoryResult.data) {
        categories.value = categoryResult.data;
      }
    }
  } catch (error) {
    console.error('获取书签列表失败:', error);
  } finally {
    initLoading.value = false;
    loadingMore.value = false;
  }
};

// 加载更多书签（懒加载）
const loadMore = async () => {
  if (!hasMore.value || loadingMore.value || initLoading.value) return;
  
  loadingMore.value = true;
  bookmarkPage.value++;
  await fetchList(false);
};

const openUrl = async (url, bookmarkId) => {
  // 如果关闭了自动打开链接，则不执行任何操作
  if (!autoOpenNewTab.value) {
    return;
  }
  
  // 记录访问
  if (bookmarkId) {
    try {
      await recordBookmarkVisitAPI(bookmarkId);
      // 更新本地数据
      const bookmark = bookmarks.value.find(b => b.id === bookmarkId);
      if (bookmark) {
        bookmark.visitCount = (bookmark.visitCount || 0) + 1;
      }
    } catch (error) {
      console.error('记录访问失败:', error);
    }
  }
  
  window.open(url, '_blank');
};

// 解析标签（后端可能返回 JSON 字符串或数组）
const parseTags = (tags) => {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  try {
    return JSON.parse(tags);
  } catch {
    return [];
  }
};

const formatDate = (val) => {
  if(!val) return '';
  const date = new Date(val);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60));
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60));
      return `${minutes}分钟前`;
    }
    return `${hours}小时前`;
  }
  if (days === 1) return '昨天';
  if (days < 7) return `${days}天前`;
  
  return `${date.getMonth() + 1}-${date.getDate()}`;
}

const handleBookmarkAdded = async (bookmark) => {
  try {
    await createBookmarkAPI(bookmark);
    ElMessage.success('书签已添加');
    fetchList(); // 重新加载列表
  } catch (error) {
    console.error('添加书签失败:', error);
  }
};

const editBookmark = (item) => {
  editingBookmark.value = item;
  isEditModalVisible.value = true;
};

const handleBookmarkUpdated = async (bookmark) => {
  try {
    await updateBookmarkAPI(bookmark.id, bookmark);
    ElMessage.success('书签已更新');
    fetchList(); // 重新加载列表
  } catch (error) {
    console.error('更新书签失败:', error);
  }
};

const handleBookmarkDeleted = async (id) => {
  try {
    await deleteBookmarkAPI(id);
    ElMessage.success('书签已删除');
    fetchList(); // 重新加载列表
  } catch (error) {
    console.error('删除书签失败:', error);
  }
};

const handleSettingsUpdate = (settings) => {
  // 保存设置到 localStorage
  localStorage.setItem('primaryColor', settings.primaryColor);
  localStorage.setItem('secondaryColor', settings.secondaryColor);
  localStorage.setItem('accentColor', settings.accentColor);
  localStorage.setItem('backgroundColor', settings.backgroundColor);
  localStorage.setItem('sidebarColorFrom', settings.sidebarColorFrom);
  localStorage.setItem('sidebarColorTo', settings.sidebarColorTo);
  localStorage.setItem('theme', settings.theme);
  localStorage.setItem('displayMode', settings.displayMode);
  localStorage.setItem('autoOpenNewTab', settings.autoOpenNewTab ? 'true' : 'false');
  localStorage.setItem('showStats', settings.showStats ? 'true' : 'false');
  
  // 应用显示设置
  displayMode.value = settings.displayMode;
  autoOpenNewTab.value = settings.autoOpenNewTab;
  showStats.value = settings.showStats;
  
  // 立即应用颜色到 CSS 变量
  applyColors(settings);
  
  ElMessage.success('设置已保存');
};

const applyColors = (settings) => {
  // 应用全局主题颜色到根元素
  const root = document.documentElement;
  root.style.setProperty('--primary-color', settings.primaryColor || '#2563eb');
  root.style.setProperty('--secondary-color', settings.secondaryColor || '#1e40af');
  root.style.setProperty('--accent-color', settings.accentColor || '#f59e0b');
  root.style.setProperty('--background-color', settings.backgroundColor || '#ffffff');
  
  // 应用侧边栏颜色到 Sidebar 组件
  if (sidebarRef.value) {
    sidebarRef.value.customColorFrom = settings.sidebarColorFrom || '#2563eb';
    sidebarRef.value.customColorTo = settings.sidebarColorTo || '#1e3a8a';
  }
};

// 页面加载时读取设置并应用
const loadSavedSettings = async () => {
  // 先从 localStorage 应用基本颜色（快速显示）
  const savedSettings = {
    primaryColor: localStorage.getItem('primaryColor') || '#2563eb',
    secondaryColor: localStorage.getItem('secondaryColor') || '#1e40af',
    accentColor: localStorage.getItem('accentColor') || '#f59e0b',
    backgroundColor: localStorage.getItem('backgroundColor') || '#ffffff',
    sidebarColorFrom: localStorage.getItem('sidebarColorFrom') || '#2563eb',
    sidebarColorTo: localStorage.getItem('sidebarColorTo') || '#1e3a8a',
  };
  applyColors(savedSettings);
  
  // 然后从后端加载完整设置
  const token = localStorage.getItem('token');
  if (token) {
    try {
      const { getSettingsAPI } = await import('./api/settings');
      const result = await getSettingsAPI();
      if (result.data) {
        const data = result.data;
        // 更新显示设置
        displayMode.value = data.displayMode || 'card';
        autoOpenNewTab.value = data.autoOpenNewTab === 1 || data.autoOpenNewTab === true;
        showStats.value = data.showStats === 1 || data.showStats === true;
        
        // 更新颜色设置
        const fullSettings = {
          primaryColor: data.primaryColor || '#2563eb',
          secondaryColor: data.secondaryColor || '#1e40af',
          accentColor: data.accentColor || '#f59e0b',
          backgroundColor: data.backgroundColor || '#ffffff',
          sidebarColorFrom: data.sidebarColorFrom || '#2563eb',
          sidebarColorTo: data.sidebarColorTo || '#1e3a8a',
        };
        applyColors(fullSettings);
        
        // 保存到 localStorage
        localStorage.setItem('displayMode', displayMode.value);
        localStorage.setItem('autoOpenNewTab', autoOpenNewTab.value ? 'true' : 'false');
        localStorage.setItem('showStats', showStats.value ? 'true' : 'false');
      }
    } catch (error) {
      console.error('从后端加载设置失败:', error);
    }
  }
};

const copyUrl = (url) => {
  // 使用现代 API 复制到剪贴板
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板');
  }).catch((err) => {
    // 降级方案：使用旧的 API
    const textarea = document.createElement('textarea');
    textarea.value = url;
    document.body.appendChild(textarea);
    textarea.select();
    try {
      document.execCommand('copy');
      ElMessage.success('链接已复制到剪贴板');
    } catch (err) {
      ElMessage.error('复制失败，请重试');
    }
    document.body.removeChild(textarea);
  });
};

const deleteBookmark = (id) => {
  ElMessageBox.confirm('确定删除这个书签吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteBookmarkAPI(id);
      ElMessage.success('删除成功');
      fetchList(); // 重新加载列表
    } catch (error) {
      console.error('删除失败:', error);
    }
  }).catch(() => {});
};

// ========== 批量操作 ==========

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id);
  if (index === -1) {
    selectedIds.value.push(id);
  } else {
    selectedIds.value.splice(index, 1);
  }
};

const selectAllBookmarks = () => {
  if (selectedIds.value.length === filteredBookmarks.value.length) {
    selectedIds.value = [];
  } else {
    selectedIds.value = filteredBookmarks.value.map(b => b.id);
  }
};

const exitBatchMode = () => {
  batchMode.value = false;
  selectedIds.value = [];
};

const batchDeleteBookmarks = async () => {
  if (selectedIds.value.length === 0) return;
  
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 个书签吗？`,
      '批量删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await batchDeleteBookmarksAPI(selectedIds.value);
    ElMessage.success(`成功删除 ${selectedIds.value.length} 个书签`);
    exitBatchMode();
    fetchList();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error);
      ElMessage.error('批量删除失败');
    }
  }
};

const handleProfileClick = () => {
  // 检查是否已登录
  const token = localStorage.getItem('token');
  if (token) {
    // 已登录，显示个人资料页面
    isProfileVisible.value = true;
  } else {
    // 未登录，跳转到登录页面
    currentPage.value = 'auth';
  }
};

const handleLogout = () => {
  // 退出登录后跳转到登录页
  currentPage.value = 'auth';
};

const handleLoginSuccess = (userData) => {
  // 保存用户信息
  localStorage.setItem('user', JSON.stringify(userData));
  // 返回主页面
  currentPage.value = 'main';
  ElMessage.success('欢迎 ' + userData.username);
  // 加载书签列表和分类
  fetchList();
  // 加载侧边栏分类
  if (sidebarRef.value) {
    sidebarRef.value.loadCategories();
  }
};

// 滚动加载更多
const handleScroll = () => {
  // 检查是否滚动到底部附近（距底部 300px 时触发）
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const scrollHeight = document.documentElement.scrollHeight;
  const clientHeight = window.innerHeight;
  
  if (scrollTop + clientHeight >= scrollHeight - 300) {
    // 只在非搜索、非回收站模式下触发懒加载
    if (currentFilter.value.type !== 'trash' && searchResults.value === null) {
      loadMore();
    }
  }
};

onMounted(() => {
  loadSavedSettings();
  
  // 检查登录状态
  const token = localStorage.getItem('token');
  if (!token) {
    // 未登录，跳转到登录页
    currentPage.value = 'auth';
  } else {
    // 已登录，加载书签列表
    fetchList();
  }
  
  // 添加滚动监听器（无限滚动）
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  // 移除滚动监听器
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
#app {
  display: flex;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>