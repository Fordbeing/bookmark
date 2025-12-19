<template>
  <el-dialog
    v-model="visible"
    :title="isEditing ? '✏️ 编辑资料' : '👤 个人资料'"
    width="450px"
    class="profile-modal"
  >
    <!-- 查看模式 -->
    <div v-if="!isEditing">
      <!-- 头像和基本信息 -->
      <div class="flex items-center gap-4 pb-5 border-b mb-5">
        <div class="w-16 h-16 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-2xl text-white shadow-lg">
          <img v-if="user.avatar" :src="user.avatar" class="w-full h-full rounded-full object-cover" />
          <span v-else>{{ user.username?.charAt(0)?.toUpperCase() || '?' }}</span>
        </div>
        <div class="flex-1">
          <h2 class="text-lg font-bold text-gray-800">{{ user.username || '未知用户' }}</h2>
          <p class="text-gray-500 text-sm">{{ user.email || '未绑定邮箱' }}</p>
        </div>
        <el-button size="small" @click="isEditing = true">编辑</el-button>
      </div>

      <!-- 使用情况 -->
      <div class="space-y-4 mb-5">
        <h3 class="text-sm font-medium text-gray-700 flex items-center gap-2">
          📊 使用情况
        </h3>
        <!-- 书签用量 -->
        <div class="bg-blue-50 p-4 rounded-xl">
          <div class="flex justify-between items-center mb-2">
            <span class="text-sm text-gray-700">📚 书签</span>
            <span class="text-sm font-medium" :class="stats.bookmarkCount >= 50 ? 'text-red-600' : 'text-blue-600'">
              {{ stats.bookmarkCount }}/50
            </span>
          </div>
          <el-progress 
            :percentage="Math.min(stats.bookmarkCount / 50 * 100, 100)" 
            :stroke-width="10"
            :color="stats.bookmarkCount >= 45 ? '#ef4444' : stats.bookmarkCount >= 35 ? '#f59e0b' : '#3b82f6'"
            :show-text="false"
          />
          <p v-if="stats.bookmarkCount >= 45" class="text-xs text-red-500 mt-2">
            ⚠️ 已接近上限，请考虑清理
          </p>
        </div>
        <!-- 分类用量 -->
        <div class="bg-green-50 p-4 rounded-xl">
          <div class="flex justify-between items-center mb-2">
            <span class="text-sm text-gray-700">📁 分类</span>
            <span class="text-sm font-medium" :class="stats.categoryCount >= 7 ? 'text-red-600' : 'text-green-600'">
              {{ stats.categoryCount }}/7
            </span>
          </div>
          <el-progress 
            :percentage="Math.min(stats.categoryCount / 7 * 100, 100)" 
            :stroke-width="10"
            :color="stats.categoryCount >= 6 ? '#ef4444' : stats.categoryCount >= 5 ? '#f59e0b' : '#22c55e'"
            :show-text="false"
          />
        </div>
        <!-- 收藏统计 -->
        <div class="flex items-center justify-between px-4 py-3 bg-amber-50 rounded-xl">
          <span class="text-sm text-gray-700">⭐ 已收藏</span>
          <span class="text-sm font-medium text-amber-600">{{ stats.favoriteCount }} 个书签</span>
        </div>
      </div>

      <!-- 退出按钮 -->
      <el-button type="danger" class="w-full" @click="handleLogout">
        退出登录
      </el-button>
    </div>

    <!-- 编辑模式 -->
    <div v-else>
      <el-form :model="editForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" disabled />
          <p class="text-xs text-gray-400 mt-1">邮箱暂不支持修改</p>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="editForm.newPassword" type="password" placeholder="留空则不修改密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" v-if="editForm.newPassword">
          <el-input v-model="editForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>

      <div class="flex gap-2 mt-4">
        <el-button class="flex-1" @click="isEditing = false">取消</el-button>
        <el-button type="primary" class="flex-1" @click="saveProfile" :loading="saving">保存</el-button>
      </div>
    </div>

    <template #footer v-if="!isEditing">
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const props = defineProps({
  modelValue: Boolean,
  bookmarks: {
    type: Array,
    default: () => []
  },
  categories: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue', 'logout']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const user = ref({});
const isEditing = ref(false);
const saving = ref(false);
const editForm = ref({
  username: '',
  email: '',
  newPassword: '',
  confirmPassword: ''
});

const loadUser = () => {
  try {
    const userData = localStorage.getItem('user');
    if (userData) {
      user.value = JSON.parse(userData);
      editForm.value.username = user.value.username || '';
      editForm.value.email = user.value.email || '';
    }
  } catch (error) {
    console.error('加载用户信息失败:', error);
  }
};

const stats = computed(() => {
  return {
    bookmarkCount: props.bookmarks.length,
    categoryCount: props.categories.length,
    favoriteCount: props.bookmarks.filter(b => b.isFavorite === 1).length
  };
});

const saveProfile = async () => {
  if (!editForm.value.username.trim()) {
    ElMessage.warning('请输入用户名');
    return;
  }
  if (editForm.value.newPassword && editForm.value.newPassword !== editForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  saving.value = true;
  try {
    // TODO: 调用后端 API 更新用户信息
    // await updateUserAPI({ username: editForm.value.username, password: editForm.value.newPassword });
    
    // 更新本地存储
    user.value.username = editForm.value.username;
    localStorage.setItem('user', JSON.stringify(user.value));
    
    ElMessage.success('资料已更新');
    isEditing.value = false;
    editForm.value.newPassword = '';
    editForm.value.confirmPassword = '';
  } catch (error) {
    ElMessage.error('更新失败');
  } finally {
    saving.value = false;
  }
};

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    ElMessage.success('已退出登录');
    visible.value = false;
    emit('logout');
  }).catch(() => {});
};

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadUser();
    isEditing.value = false;
  }
}, { immediate: true });

onMounted(() => {
  loadUser();
});
</script>

<style scoped>
.profile-modal :deep(.el-dialog__body) {
  padding-top: 16px;
}
</style>
