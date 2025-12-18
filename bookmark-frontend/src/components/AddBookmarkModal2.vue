<template>
  <el-dialog
    v-model="visible"
    title="➕ 手工添加书签"
    width="600px"
    class="add-bookmark-modal"
  >
    <div class="grid grid-cols-1 gap-6">
      <!-- 表单 -->
      <el-form :model="form" label-width="80px">
        <!-- 基础信息 -->
        <el-form-item label="标题" required>
          <el-input
            v-model="form.title"
            placeholder="输入书签标题，如：Google"
            clearable
          />
        </el-form-item>

        <el-form-item label="链接" required>
          <el-input
            v-model="form.url"
            placeholder="输入完整链接，如：https://google.com"
            prefix-icon="Link"
            clearable
            @keyup.enter="handleAdd"
          />
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="form.categoryId"
            placeholder="选择一个分类"
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

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="输入书签描述"
            :rows="2"
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            placeholder="输入或选择标签，按Enter添加"
          >
            <el-option 
              v-for="tag in tagOptions" 
              :key="tag.id" 
              :label="tag.name" 
              :value="tag.name" 
            />
          </el-select>
        </el-form-item>

        <!-- 设置 -->
        <el-form-item label="星标">
          <el-switch v-model="form.starred" />
          <span class="ml-2 text-gray-600">标记为重要</span>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="resetForm">清空</el-button>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleAdd">添加书签</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getCategoryListAPI } from '../api/category';
import { getTagListAPI } from '../api/tag';

const props = defineProps({
  visible: Boolean,
});

const emit = defineEmits(['update:visible', 'add-bookmark']);

const visible = ref(props.visible);
const loading = ref(false);
const categories = ref([]);
const tagOptions = ref([]);

const form = ref({
  title: '',
  url: '',
  description: '',
  categoryId: null,
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

// 监听 props.visible 变化
watch(() => props.visible, (newVal) => {
  visible.value = newVal;
  if (newVal) {
    // 弹窗打开时加载分类和标签
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

const handleAdd = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题');
    return;
  }
  if (!form.value.url.trim()) {
    ElMessage.warning('请输入链接');
    return;
  }
  if (!form.value.url.startsWith('http')) {
    form.value.url = 'https://' + form.value.url;
  }

  loading.value = true;
  try {
    // 构建正确的请求数据
    const bookmarkData = {
      title: form.value.title,
      url: form.value.url,
      description: form.value.description || '',
      categoryId: form.value.categoryId || null,
      tags: form.value.tags || [],
      isFavorite: form.value.starred ? 1 : 0
    };
    
    emit('add-bookmark', bookmarkData);
    ElMessage.success('书签添加成功！');
    resetForm();
    visible.value = false;
    emit('update:visible', false);
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = {
    title: '',
    url: '',
    description: '',
    categoryId: null,
    tags: [],
    starred: false,
  };
};

onMounted(() => {
  loadCategories();
});
</script>

<style scoped>
.add-bookmark-modal :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}
</style>

