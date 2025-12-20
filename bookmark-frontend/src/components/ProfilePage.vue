<template>
  <el-dialog
    v-model="visible"
    :title="isEditing ? '✏️ 编辑资料' : '👤 个人资料'"
    width="500px"
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
        <el-button size="small" type="primary" @click="isEditing = true">编辑</el-button>
      </div>

      <!-- 使用情况 -->
      <div class="mb-4">
        <h3 class="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-2">
          📊 使用情况
        </h3>
        
        <!-- 书签和分类用量 - 合并显示 -->
        <div class="grid grid-cols-2 gap-2 mb-2">
          <!-- 书签用量 -->
          <div class="bg-gradient-to-br from-blue-50 to-blue-100 p-3 rounded-lg">
            <div class="flex justify-between items-center mb-1">
              <span class="text-xs text-gray-700 font-medium">📚 书签</span>
              <span class="text-xs font-bold" :class="stats.bookmarkCount >= (limitsInfo?.bookmarkLimit || 50) ? 'text-red-600' : 'text-blue-600'">
                {{ stats.bookmarkCount }}/{{ limitsInfo?.bookmarkLimit || 50 }}
              </span>
            </div>
            <el-progress 
              :percentage="Math.min(stats.bookmarkCount / (limitsInfo?.bookmarkLimit || 50) * 100, 100)" 
              :stroke-width="6"
              :color="stats.bookmarkCount >= (limitsInfo?.bookmarkLimit || 50) * 0.9 ? '#ef4444' : '#3b82f6'"
              :show-text="false"
            />
          </div>
          <!-- 分类用量 -->
          <div class="bg-gradient-to-br from-green-50 to-green-100 p-3 rounded-lg">
            <div class="flex justify-between items-center mb-1">
              <span class="text-xs text-gray-700 font-medium">📁 分类</span>
              <span class="text-xs font-bold" :class="stats.categoryCount >= (limitsInfo?.categoryLimit || 7) ? 'text-red-600' : 'text-green-600'">
                {{ stats.categoryCount }}/{{ limitsInfo?.categoryLimit || 7 }}
              </span>
            </div>
            <el-progress 
              :percentage="Math.min(stats.categoryCount / (limitsInfo?.categoryLimit || 7) * 100, 100)" 
              :stroke-width="6"
              :color="stats.categoryCount >= (limitsInfo?.categoryLimit || 7) * 0.85 ? '#ef4444' : '#22c55e'"
              :show-text="false"
            />
          </div>
        </div>
        
        <!-- 收藏统计 -->
        <div class="flex items-center justify-between px-3 py-2 bg-gradient-to-r from-amber-50 to-orange-50 rounded-lg">
          <span class="text-xs text-gray-700 font-medium">⭐ 已收藏</span>
          <span class="text-xs font-bold text-amber-600">{{ stats.favoriteCount }} 个</span>
        </div>
      </div>

      <!-- 激活码功能 - 管理员版本 -->
      <div v-if="limitsInfo?.isAdmin" class="mb-4">
        <h3 class="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-2">
          🎁 激活码管理
        </h3>
        
        <div class="grid grid-cols-2 gap-2">
          <!-- 创建激活码按钮 -->
          <el-button type="primary" size="small" @click="showCreateCodeDialog = true" class="w-full">
            <span class="text-xs">➕ 创建</span>
          </el-button>
          
          <!-- 查看已创建的激活码按钮 -->
          <el-button 
            v-if="activationCodes.length > 0"
            type="info" 
            size="small"
            @click="showAdminCodesDialog = true" 
            class="w-full"
          >
            <span class="text-xs">📄 已创建 ({{ activationCodes.length }})</span>
          </el-button>
          <el-button v-else disabled size="small" class="w-full">
            <span class="text-xs">暂无激活码</span>
          </el-button>
        </div>
      </div>

      <!-- 激活码功能 - 普通用户版本 -->
      <div v-else class="space-y-4 mb-5">
        <h3 class="text-sm font-medium text-gray-700 flex items-center gap-2">
          🎁 激活码
        </h3>
        <!-- 兑换激活码 -->
        <div class="bg-gradient-to-br from-yellow-50 to-amber-50 p-4 rounded-xl border border-yellow-200">
          <h4 class="text-sm font-medium text-gray-800 mb-2">兑换激活码</h4>
          <p class="text-xs text-gray-600 mb-3">输入激活码可增加书签和分类限额</p>
          <div class="flex gap-2">
            <el-input 
              v-model="activationCode" 
              placeholder="请输入激活码"
              @keyup.enter="redeemCode"
              size="default"
              class="flex-1"
            />
            <el-button 
              type="warning" 
              @click="redeemCode" 
              :loading="redeemLoading"
              :disabled="!activationCode.trim()"
            >
              兑换
            </el-button>
          </div>
        </div>

        <!-- 我的激活码 - 按钮 -->
        <div class="mt-3">
          <el-button 
            v-if="limitsInfo?.activations && limitsInfo.activations.length > 0"
            type="info" 
            size="small" 
            @click="showActivationHistoryDialog = true"
            class="w-full"
          >
            📄 查看我的激活码 ({{ limitsInfo.activations.length }})
          </el-button>
          <div v-else class="text-center text-gray-400 text-xs py-2">
            暂无已激活的激活码
          </div>
        </div>
      </div>

      <!-- 退出按钮 -->
      <el-button type="danger" size="small" class="w-full" @click="handleLogout">
        🚪 退出登录
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

  <!-- 管理员 - 创建激活码弹窗 -->
  <el-dialog
    v-model="showCreateCodeDialog"
    title="➕ 创建激活码"
    width="420px"
  >
    <el-form :model="createForm" label-position="top" size="default">
      <div class="grid grid-cols-2 gap-3">
        <el-form-item label="额外书签数">
          <el-input-number v-model="createForm.extraBookmarks" :min="0" :max="1000" class="w-full" />
        </el-form-item>
        <el-form-item label="额外分类数">
          <el-input-number v-model="createForm.extraCategories" :min="0" :max="100" class="w-full" />
        </el-form-item>
      </div>
      <el-form-item label="过期日期">
        <el-date-picker
          v-model="createForm.expiryDate"
          type="date"
          placeholder="选择过期日期"
          class="w-full"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="最大使用次数">
        <el-input-number v-model="createForm.maxUses" :min="1" :max="1000" class="w-full" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="flex gap-2 justify-end">
        <el-button @click="showCreateCodeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCode" :loading="createLoading">生成激活码</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 管理员 - 已创建激活码列表弹窗 -->
  <el-dialog
    v-model="showAdminCodesDialog"
    title="📄 已创建的激活码"
    width="600px"
  >
    <div class="max-h-96 overflow-y-auto space-y-2 pr-2">
      <div 
        v-for="code in activationCodes" 
        :key="code.id"
        class="bg-white p-3 rounded-lg border border-gray-200"
      >
        <div class="flex items-center justify-between mb-2">
          <code class="text-sm font-mono bg-purple-100 text-purple-700 px-2 py-1 rounded">{{ code.code }}</code>
          <el-button size="small" @click="copyCode(code.code)">复制</el-button>
        </div>
        <div class="flex gap-2 flex-wrap text-xs text-gray-600">
          <span v-if="code.extraBookmarks" class="px-2 py-1 bg-blue-50 rounded">+{{ code.extraBookmarks }} 书签</span>
          <span v-if="code.extraCategories" class="px-2 py-1 bg-green-50 rounded">+{{ code.extraCategories }} 分类</span>
          <span class="px-2 py-1 bg-gray-50 rounded">已用 {{ code.usedCount }}/{{ code.maxUses }}</span>
        </div>
        <div class="text-xs text-gray-500 mt-2">
          过期时间: {{ formatCodeExpiryDate(code) }}
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showAdminCodesDialog = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 激活码历史弹窗 -->
  <el-dialog
    v-model="showActivationHistoryDialog"
    title="📄 我的激活码"
    width="500px"
    class="activation-history-dialog"
  >
    <div class="max-h-96 overflow-y-auto space-y-2 pr-2">
      <div 
        v-for="activation in limitsInfo?.activations" 
        :key="activation.id"
        class="bg-white p-3 rounded-lg border"
        :class="isExpired(activation.expireTime) ? 'border-gray-300 opacity-60' : 'border-purple-200'"
      >
        <div class="flex items-start justify-between mb-2">
          <div class="flex gap-2 flex-wrap">
            <span v-if="activation.extraBookmarks" class="inline-block px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded">
              +{{ activation.extraBookmarks }} 书签
            </span>
            <span v-if="activation.extraCategories" class="inline-block px-2 py-1 bg-green-100 text-green-700 text-xs rounded">
              +{{ activation.extraCategories }} 分类
            </span>
          </div>
          <span 
            class="text-xs px-2 py-1 rounded"
            :class="isExpired(activation.expireTime) ? 'bg-gray-200 text-gray-600' : 'bg-purple-100 text-purple-700'"
          >
            {{ isExpired(activation.expireTime) ? '已过期' : '有效' }}
          </span>
        </div>
        <div class="text-xs text-gray-500">
          过期时间: {{ formatDate(activation.expireTime) }}
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showActivationHistoryDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { redeemActivationCodeAPI, getMyLimitsAPI, createActivationCodeAPI, getActivationCodeListAPI } from '../api/activationCode';

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

// 激活码相关
const activationCode = ref('');
const limitsInfo = ref(null);
const redeemLoading = ref(false);

// 管理员创建激活码
const createForm = ref({
  extraBookmarks: 20,
  extraCategories: 5,
  expiryDate: null, // 使用日期选择器
  maxUses: 10
});
const createLoading = ref(false);
const activationCodes = ref([]);

// 激活码历史弹窗
const showActivationHistoryDialog = ref(false);

// 管理员弹窗
const showCreateCodeDialog = ref(false);
const showAdminCodesDialog = ref(false);

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

// 加载限额信息
const loadLimits = async () => {
  try {
    // 添加时间戳参数强制刷新，避免缓存
    const result = await getMyLimitsAPI();
    if (result.data) {
      limitsInfo.value = result.data;
    }
  } catch (error) {
    console.error('加载限额信息失败:', error);
  }
};

// 兑换激活码
const redeemCode = async () => {
  if (!activationCode.value.trim()) {
    ElMessage.warning('请输入激活码');
    return;
  }

  redeemLoading.value = true;
  try {
    const result = await redeemActivationCodeAPI(activationCode.value.trim());
    ElMessage.success('激活码兑换成功！');
    activationCode.value = '';
    // 重新加载限额信息
    await loadLimits();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '兑换失败，请检查激活码是否有效');
  } finally {
    redeemLoading.value = false;
  }
};

// 判断是否过期
const isExpired = (expiryDate) => {
  if (!expiryDate) return false;
  return new Date(expiryDate) < new Date();
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '永久有效';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 格式化激活码过期日期（管理员视图）
const formatCodeExpiryDate = (code) => {
  if (!code.expireDays) return '永久有效';
  
  // 根据创建时间 + 有效天数计算过期时间
  const createDate = new Date(code.createTime);
  const expiryDate = new Date(createDate);
  expiryDate.setDate(expiryDate.getDate() + code.expireDays);
  
  return expiryDate.toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 创建激活码（管理员）
const handleCreateCode = async () => {
  if (!createForm.value.expiryDate) {
    ElMessage.warning('请选择过期日期');
    return;
  }

  createLoading.value = true;
  try {
    // 计算有效天数：过期日期 - 当前日期
    const now = new Date();
    now.setHours(0, 0, 0, 0); // 重置到当天0点
    const expiryDate = new Date(createForm.value.expiryDate);
    const diffTime = expiryDate.getTime() - now.getTime();
    const expireDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (expireDays <= 0) {
      ElMessage.error('过期日期必须大于当前日期');
      createLoading.value = false;
      return;
    }

    const payload = {
      extraBookmarks: createForm.value.extraBookmarks,
      extraCategories: createForm.value.extraCategories,
      expireDays: expireDays,
      maxUses: createForm.value.maxUses
    };

    const result = await createActivationCodeAPI(payload);
    ElMessage.success('激活码创建成功！');
    showCreateCodeDialog.value = false;
    // 重置表单
    createForm.value.expiryDate = null;
    // 刷新激活码列表
    await loadActivationCodes();
  } catch (error) {
    ElMessage.error('创建失败：' + (error.response?.data?.message || '未知错误'));
  } finally {
    createLoading.value = false;
  }
};

// 加载激活码列表（管理员）
const loadActivationCodes = async () => {
  try {
    const result = await getActivationCodeListAPI();
    if (result.data) {
      activationCodes.value = result.data;
    }
  } catch (error) {
    console.error('加载激活码列表失败:', error);
  }
};

// 复制激活码
const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code);
    ElMessage.success('激活码已复制到剪贴板');
  } catch (error) {
    ElMessage.error('复制失败，请手动复制');
  }
};

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadUser();
    loadLimits();
    // 如果是管理员，加载激活码列表
    if (limitsInfo.value?.isAdmin) {
      loadActivationCodes();
    }
    isEditing.value = false;
  }
}, { immediate: true });

// 监听 limitsInfo 变化，当管理员状态确定后加载激活码
watch(() => limitsInfo.value?.isAdmin, (isAdmin) => {
  if (isAdmin) {
    loadActivationCodes();
  }
});

onMounted(() => {
  loadUser();
});
</script>

<style scoped>
.profile-modal :deep(.el-dialog__body) {
  padding-top: 16px;
}
</style>
