<template>
  <el-dialog
    v-model="visible"
    title="✏️ 编辑书签"
    width="600px"
    class="edit-bookmark-dialog"
    @close="handleClose"
  >
    <div v-if="bookmark">
      <el-form :model="form" label-width="100px">
        <!-- 图标显示 -->
        <div class="flex justify-center mb-6">
          <div class="w-16 h-16 rounded-lg bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-white text-3xl font-bold">
            {{ form.title?.charAt(0)?.toUpperCase() || '📌' }}
          </div>
        </div>

        <!-- 基础信息 -->
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="输入书签标题" />
        </el-form-item>

        <el-form-item label="链接" required>
          <el-input v-model="form.url" placeholder="输入完整链接" />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable>
            <el-option 
              v-for="cat in categories" 
              :key="cat.id" 
              :label="cat.name" 
              :value="cat.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="输入书签描述" />
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

        <!-- 元数据 -->
        <el-divider />
        <div class="text-sm text-gray-600 space-y-2">
          <p>📅 创建时间: {{ formatDate(bookmark.createTime) }}</p>
          <p>✏️ 更新时间: {{ formatDate(bookmark.updateTime) || '未更新' }}</p>
          <p>👁️ 访问次数: {{ bookmark.visitCount || 0 }}</p>
        </div>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleDelete" type="danger">删除</el-button>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCategoryListAPI } from '../api/category';
import { getTagListAPI } from '../api/tag';

const props = defineProps({
  visible: Boolean,
  bookmark: Object,
});

const emit = defineEmits(['update:visible', 'save', 'delete', 'close']);

const visible = ref(props.visible);
const form = ref({});
const categories = ref([]);
const tagOptions = ref([]);

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

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN');
};

// 解析 tags（后端可能返回 JSON 字符串或数组）
const parseTags = (tags) => {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  try {
    return JSON.parse(tags);
  } catch {
    return [];
  }
};

watch(() => props.visible, (val) => {
  visible.value = val;
  if (val) {
    loadCategories();
    loadTags();
    if (props.bookmark) {
      form.value = {
        ...props.bookmark,
        tags: parseTags(props.bookmark.tags),
        starred: props.bookmark.isFavorite === 1
      };
    }
  }
});

watch(() => props.bookmark, (val) => {
  if (val) {
    form.value = {
      ...val,
      tags: parseTags(val.tags),
      starred: val.isFavorite === 1
    };
  }
});

const handleSave = () => {
  if (!form.value.title?.trim()) {
    ElMessage.warning('请输入标题');
    return;
  }
  if (!form.value.url?.trim()) {
    ElMessage.warning('请输入链接');
    return;
  }
  
  // 构建正确的数据格式
  const saveData = {
    id: form.value.id,
    title: form.value.title,
    url: form.value.url,
    description: form.value.description || '',
    categoryId: form.value.categoryId || null,
    tags: form.value.tags || [],
    isFavorite: form.value.starred ? 1 : 0
  };
  
  emit('save', saveData);
  handleClose();
};

const handleDelete = () => {
  ElMessageBox.confirm('确定要删除这个书签吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    emit('delete', form.value.id);
    handleClose();
  }).catch(() => {});
};

const handleClose = () => {
  visible.value = false;
  emit('update:visible', false);
  emit('close');
};

onMounted(() => {
  loadCategories();
});
</script>

<style scoped>
.edit-bookmark-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}
</style>

