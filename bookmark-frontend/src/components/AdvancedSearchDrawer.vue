<template>
  <el-drawer
    v-model="visible"
    title="高级搜索"
    direction="rtl"
    size="400px"
    :close-on-click-modal="true"
  >
    <div class="advanced-search">
      <!-- 关键词 -->
      <div class="search-field">
        <label>关键词</label>
        <el-input 
          v-model="searchForm.keyword" 
          placeholder="标题/描述/URL"
          clearable
          @keyup.enter="handleSearch"
        />
      </div>

      <!-- 域名筛选 -->
      <div class="search-field">
        <label>域名</label>
        <el-input 
          v-model="searchForm.domain" 
          placeholder="如: github.com"
          clearable
        />
      </div>

      <!-- 分类筛选 -->
      <div class="search-field">
        <label>分类</label>
        <el-select 
          v-model="searchForm.categoryId" 
          placeholder="选择分类"
          clearable
          style="width: 100%"
        >
          <el-option 
            v-for="cat in categories" 
            :key="cat.id" 
            :label="cat.name" 
            :value="cat.id" 
          />
        </el-select>
      </div>

      <!-- 日期范围 -->
      <div class="search-field">
        <label>添加时间</label>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </div>

      <!-- 链接状态 -->
      <div class="search-field">
        <label>链接状态</label>
        <el-select 
          v-model="searchForm.linkStatus" 
          placeholder="全部"
          clearable
          style="width: 100%"
        >
          <el-option label="未检测" :value="0" />
          <el-option label="正常" :value="1" />
          <el-option label="失效" :value="2" />
          <el-option label="重定向" :value="3" />
          <el-option label="超时" :value="4" />
        </el-select>
      </div>

      <!-- 搜索历史 -->
      <div class="search-history" v-if="searchHistory.length > 0">
        <div class="history-header">
          <span>搜索历史</span>
          <el-button type="text" size="small" @click="clearHistory">清空</el-button>
        </div>
        <div class="history-tags">
          <el-tag 
            v-for="(item, index) in searchHistory" 
            :key="index"
            size="small"
            type="info"
            @click="applyHistory(item)"
            closable
            @close="removeHistory(index)"
            class="history-tag"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="search-actions">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" @click="handleSearch" :loading="loading">
          搜索
        </el-button>
      </div>

      <!-- 搜索结果 -->
      <div v-if="searchResults.length > 0" class="search-results">
        <div class="results-header">
          <span>找到 {{ searchResults.length }} 个结果</span>
        </div>
        <div 
          v-for="item in searchResults" 
          :key="item.id" 
          class="result-item"
          @click="openBookmark(item)"
        >
          <div class="result-icon">
            <img v-if="item.iconUrl" :src="item.iconUrl" @error="(e) => e.target.style.display='none'" />
            <span v-else>{{ item.title?.charAt(0)?.toUpperCase() || '?' }}</span>
          </div>
          <div class="result-content">
            <div class="result-title">{{ item.title }}</div>
            <div class="result-url">{{ item.url }}</div>
          </div>
          <span v-if="item.isPinned === 1" class="pin-icon">📌</span>
          <span v-if="item.linkStatus === 2" class="dead-icon">⚠️</span>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { advancedSearchAPI } from '../api/bookmark';
import { getCategoryListAPI } from '../api/category';

const props = defineProps({
  modelValue: Boolean
});

const emit = defineEmits(['update:modelValue', 'select']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const loading = ref(false);
const categories = ref([]);
const searchResults = ref([]);
const searchHistory = ref(JSON.parse(localStorage.getItem('searchHistory') || '[]'));

const searchForm = ref({
  keyword: '',
  domain: '',
  categoryId: null,
  linkStatus: null
});

const dateRange = ref(null);

// 加载分类
onMounted(async () => {
  try {
    const result = await getCategoryListAPI();
    if (result.data) {
      categories.value = result.data;
    }
  } catch (error) {
    console.error('加载分类失败:', error);
  }
});

// 执行搜索
const handleSearch = async () => {
  loading.value = true;
  try {
    const params = { ...searchForm.value };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }

    const result = await advancedSearchAPI(params);
    if (result.data) {
      searchResults.value = result.data.list || result.data;
    }

    // 保存搜索历史
    if (searchForm.value.keyword && !searchHistory.value.includes(searchForm.value.keyword)) {
      searchHistory.value.unshift(searchForm.value.keyword);
      if (searchHistory.value.length > 10) {
        searchHistory.value.pop();
      }
      localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value));
    }
  } catch (error) {
    console.error('搜索失败:', error);
  } finally {
    loading.value = false;
  }
};

// 重置表单
const resetForm = () => {
  searchForm.value = {
    keyword: '',
    domain: '',
    categoryId: null,
    linkStatus: null
  };
  dateRange.value = null;
  searchResults.value = [];
};

// 应用历史搜索
const applyHistory = (keyword) => {
  searchForm.value.keyword = keyword;
  handleSearch();
};

// 删除单条历史
const removeHistory = (index) => {
  searchHistory.value.splice(index, 1);
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value));
};

// 清空历史
const clearHistory = () => {
  searchHistory.value = [];
  localStorage.removeItem('searchHistory');
};

// 打开书签
const openBookmark = (item) => {
  window.open(item.url, '_blank');
  emit('select', item);
};
</script>

<style scoped>
.advanced-search {
  padding: 0 10px;
}

.search-field {
  margin-bottom: 20px;
}

.search-field label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.search-history {
  margin: 20px 0;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
}

.search-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.search-actions .el-button {
  flex: 1;
}

.search-results {
  margin-top: 30px;
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.results-header {
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.result-item:hover {
  background: #f0f7ff;
}

.result-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  flex-shrink: 0;
}

.result-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-url {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pin-icon, .dead-icon {
  font-size: 16px;
}
</style>
