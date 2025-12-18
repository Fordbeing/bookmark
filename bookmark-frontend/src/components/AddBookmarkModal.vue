<template>
  <el-dialog 
    v-model="visible" 
    title="添加新书签" 
    width="500px"
    @close="closeModal"
    @open="loadCategories"
  >
    <el-form 
      ref="formRef" 
      :model="form" 
      label-width="80px"
      @submit.prevent="addBookmark"
    >
      <el-form-item label="标题" prop="title" required>
        <el-input 
          v-model="form.title" 
          placeholder="输入书签标题"
          @keyup.enter="addBookmark"
        />
      </el-form-item>

      <el-form-item label="链接" prop="url" required>
        <el-input 
          v-model="form.url" 
          placeholder="输入完整链接（https://...）"
          @keyup.enter="addBookmark"
          @blur="fetchMetadata"
        >
          <template #prefix>
            <el-icon><Link /></el-icon>
          </template>
          <template #suffix>
            <el-icon v-if="fetchingMetadata" class="is-loading"><Loading /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="分类" prop="categoryId">
        <el-select 
          v-model="form.categoryId" 
          placeholder="选择分类"
          clearable
        >
          <el-option 
            v-for="cat in categories" 
            :key="cat.id" 
            :label="cat.name" 
            :value="cat.id" 
          />
        </el-select>
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input 
          v-model="form.description" 
          type="textarea"
          placeholder="输入书签描述（可选）"
          :rows="3"
        />
      </el-form-item>

      <el-form-item label="标签" prop="tags">
        <el-select 
          v-model="form.tags" 
          multiple 
          filterable 
          allow-create 
          placeholder="输入标签，按Enter添加"
        >
          <el-option 
            v-for="tag in tagOptions" 
            :key="tag.id" 
            :label="tag.name" 
            :value="tag.name" 
          />
        </el-select>
      </el-form-item>

      <el-form-item label="星标">
        <el-switch v-model="form.starred" />
        <span class="ml-2 text-gray-500 text-sm">标记为重要</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeModal">取消</el-button>
        <el-button type="primary" @click="addBookmark" :loading="loading">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Link, Loading } from '@element-plus/icons-vue';
import { getCategoryListAPI } from '../api/category';
import { getTagListAPI } from '../api/tag';
import { fetchUrlMetadataAPI } from '../api/url';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:visible', 'close', 'add-bookmark']);

const visible = ref(props.visible);
const loading = ref(false);
const fetchingMetadata = ref(false);
const categories = ref([]);
const tagOptions = ref([]);
const form = ref({
  title: '',
  url: '',
  categoryId: null,
  description: '',
  tags: [],
  starred: false,
});

// 加载分类列表
const loadCategories = async () => {
  try {
    const result = await getCategoryListAPI();
    if (result.data) {
      categories.value = result.data;
    }
  } catch (error) {
    console.error('加载分类失败:', error);
  }
};

// 加载标签列表
const loadTags = async () => {
  try {
    const result = await getTagListAPI();
    if (result.data) {
      tagOptions.value = result.data;
    }
  } catch (error) {
    console.error('加载标签失败:', error);
  }
};

// 获取 URL 元数据
const fetchMetadata = async () => {
  const url = form.value.url?.trim();
  if (!url) return;
  
  // 如果已经有标题，不重复获取
  if (form.value.title) return;
  
  fetchingMetadata.value = true;
  try {
    const result = await fetchUrlMetadataAPI(url);
    if (result.data) {
      if (!form.value.title && result.data.title) {
        form.value.title = result.data.title;
      }
      if (!form.value.description && result.data.description) {
        form.value.description = result.data.description;
      }
    }
  } catch (error) {
    console.error('获取 URL 元数据失败:', error);
  } finally {
    fetchingMetadata.value = false;
  }
};

// 监听 props.visible 变化
watch(() => props.visible, (newVal) => {
  visible.value = newVal;
  if (newVal) {
    loadCategories();
    loadTags();
  }
}, { immediate: true });

// 监听 visible 变化，发送更新事件
watch(visible, (newVal) => {
  if (!newVal) {
    emit('update:visible', false);
  }
});

const closeModal = () => {
  form.value = {
    title: '',
    url: '',
    categoryId: null,
    description: '',
    tags: [],
    starred: false,
  };
  visible.value = false;
  emit('update:visible', false);
  emit('close');
};

const addBookmark = () => {
  if (!form.value.title || !form.value.url) {
    ElMessage.warning('请填写标题和链接');
    return;
  }

  loading.value = true;
  
  // 构建正确的数据格式
  const bookmarkData = {
    title: form.value.title,
    url: form.value.url,
    description: form.value.description || '',
    categoryId: form.value.categoryId || null,
    tags: form.value.tags || [],
    isFavorite: form.value.starred ? 1 : 0
  };
  
  emit('add-bookmark', bookmarkData);
  loading.value = false;
  closeModal();
};

onMounted(() => {
  loadCategories();
});
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
}
</style>
