<template>
  <el-dialog
    v-model="visible"
    title="➕ 添加分类"
    width="400px"
    class="category-modal"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="分类名称" required>
        <el-input
          v-model="form.name"
          placeholder="输入分类名称"
          clearable
          @keyup.enter="handleAdd"
        />
      </el-form-item>

      <el-form-item label="颜色">
        <el-color-picker v-model="form.color" />
      </el-form-item>

      <el-form-item label="图标">
        <el-select v-model="form.icon" placeholder="选择图标">
          <el-option label="📁 文件夹" value="📁" />
          <el-option label="💼 工作" value="💼" />
          <el-option label="📚 学习" value="📚" />
          <el-option label="🎮 娱乐" value="🎮" />
          <el-option label="🛒 购物" value="🛒" />
          <el-option label="🔧 工具" value="🔧" />
          <el-option label="💡 灵感" value="💡" />
          <el-option label="📰 新闻" value="📰" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleAdd">添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { createCategoryAPI } from '../api/category';

const props = defineProps({
  modelValue: Boolean,
});

const emit = defineEmits(['update:modelValue', 'success']);

const visible = ref(props.modelValue);
const loading = ref(false);

const form = ref({
  name: '',
  color: '#2563eb',
  icon: '📁',
});

watch(() => props.modelValue, (val) => {
  visible.value = val;
}, { immediate: true });

watch(visible, (val) => {
  emit('update:modelValue', val);
});

const handleAdd = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入分类名称');
    return;
  }

  loading.value = true;
  try {
    await createCategoryAPI(form.value);
    ElMessage.success('分类添加成功');
    form.value = { name: '', color: '#2563eb', icon: '📁' };
    visible.value = false;
    emit('success');
  } catch (error) {
    console.error('添加分类失败:', error);
  } finally {
    loading.value = false;
  }
};
</script>
