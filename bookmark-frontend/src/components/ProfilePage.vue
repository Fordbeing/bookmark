<template>
  <el-dialog
    v-model="visible"
    :title="isEditing ? '✏️ 编辑资料' : '👤 个人中心'"
    width="480px"
    class="profile-modal"
  >
    <!-- 查看模式 -->
    <div v-if="!isEditing" class="profile-content">
      <!-- 用户头像卡片 -->
      <div class="user-card">
        <div class="avatar">
          <img v-if="user.avatar" :src="user.avatar" />
          <span v-else>{{ user.username?.charAt(0)?.toUpperCase() || '?' }}</span>
        </div>
        <div class="user-info">
          <h2>{{ user.username || '未知用户' }}</h2>
          <p>{{ user.email || '未绑定邮箱' }}</p>
        </div>
        <el-button size="small" @click="isEditing = true" class="edit-btn">编辑</el-button>
      </div>

      <!-- 使用统计 -->
      <div class="stats-section">
        <h3>📊 使用统计</h3>
        <div class="stats-grid">
          <div class="stat-item blue">
            <div class="stat-header">
              <span class="stat-label">📚 书签</span>
              <span class="stat-value">{{ stats.bookmarkCount }}/{{ limitsInfo?.bookmarkLimit || 50 }}</span>
            </div>
            <el-progress 
              :percentage="Math.min(stats.bookmarkCount / (limitsInfo?.bookmarkLimit || 50) * 100, 100)" 
              :stroke-width="5"
              :color="stats.bookmarkCount >= (limitsInfo?.bookmarkLimit || 50) * 0.9 ? '#ef4444' : '#3b82f6'"
              :show-text="false"
            />
          </div>
          <div class="stat-item green">
            <div class="stat-header">
              <span class="stat-label">📁 分类</span>
              <span class="stat-value">{{ stats.categoryCount }}/{{ limitsInfo?.categoryLimit || 7 }}</span>
            </div>
            <el-progress 
              :percentage="Math.min(stats.categoryCount / (limitsInfo?.categoryLimit || 7) * 100, 100)" 
              :stroke-width="5"
              :color="stats.categoryCount >= (limitsInfo?.categoryLimit || 7) * 0.85 ? '#ef4444' : '#22c55e'"
              :show-text="false"
            />
          </div>
          <div class="stat-item amber full-width">
            <span class="stat-label">⭐ 已收藏</span>
            <span class="stat-value">{{ stats.favoriteCount }} 个</span>
          </div>
        </div>
      </div>

      <!-- 激活码区域 -->
      <div class="activation-section">
        <!-- 管理员版本 -->
        <template v-if="limitsInfo?.isAdmin">
          <h3>🎁 激活码管理</h3>
          <div class="admin-actions">
            <el-button type="primary" @click="showCreateCodeDialog = true">
              ➕ 创建激活码
            </el-button>
            <el-button 
              v-if="activationCodes.length > 0"
              @click="showAdminCodesDialog = true"
            >
              📄 已创建 ({{ activationCodes.length }})
            </el-button>
          </div>
        </template>

        <!-- 普通用户版本 -->
        <template v-else>
          <h3>🎁 激活码</h3>
          <div class="redeem-box">
            <el-input 
              v-model="activationCode" 
              placeholder="输入激活码"
              @keyup.enter="redeemCode"
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
          <el-button 
            v-if="limitsInfo?.activations && limitsInfo.activations.length > 0"
            link 
            type="primary"
            @click="showActivationHistoryDialog = true"
            class="history-link"
          >
            查看我的激活码 ({{ limitsInfo.activations.length }})
          </el-button>
        </template>
      </div>

      <!-- 退出按钮 -->
      <el-button type="danger" class="logout-btn" @click="handleLogout">
        🚪 退出登录
      </el-button>
    </div>

    <!-- 编辑模式 -->
    <div v-else class="edit-content">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" disabled />
          <p class="help-text">邮箱暂不支持修改</p>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="editForm.newPassword" type="password" placeholder="留空则不修改" show-password />
        </el-form-item>
        <el-form-item label="确认密码" v-if="editForm.newPassword">
          <el-input v-model="editForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>

      <div class="edit-actions">
        <el-button @click="isEditing = false">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
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
    width="400px"
  >
    <el-form :model="createForm" label-position="top">
      <div class="create-grid">
        <el-form-item label="额外书签数">
          <el-input-number v-model="createForm.extraBookmarks" :min="0" :max="1000" />
        </el-form-item>
        <el-form-item label="额外分类数">
          <el-input-number v-model="createForm.extraCategories" :min="0" :max="100" />
        </el-form-item>
      </div>
      <el-form-item label="过期日期">
        <el-date-picker
          v-model="createForm.expiryDate"
          type="date"
          placeholder="选择日期"
          style="width: 100%"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="最大使用次数">
        <el-input-number v-model="createForm.maxUses" :min="1" :max="1000" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreateCodeDialog = false">取消</el-button>
      <el-button type="primary" @click="handleCreateCode" :loading="createLoading">生成</el-button>
    </template>
  </el-dialog>

  <!-- 管理员 - 已创建激活码列表 -->
  <el-dialog
    v-model="showAdminCodesDialog"
    title="📄 已创建的激活码"
    width="550px"
  >
    <div class="codes-list">
      <div v-for="code in activationCodes" :key="code.id" class="code-item">
        <div class="code-header">
          <code>{{ code.code }}</code>
          <el-button size="small" @click="copyCode(code.code)">复制</el-button>
        </div>
        <div class="code-tags">
          <span v-if="code.extraBookmarks" class="tag blue">+{{ code.extraBookmarks }} 书签</span>
          <span v-if="code.extraCategories" class="tag green">+{{ code.extraCategories }} 分类</span>
          <span class="tag gray">{{ code.usedCount }}/{{ code.maxUses }} 次</span>
        </div>
        <div class="code-expiry">过期: {{ formatCodeExpiryDate(code) }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="showAdminCodesDialog = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 激活码历史 -->
  <el-dialog
    v-model="showActivationHistoryDialog"
    title="📄 我的激活码"
    width="450px"
  >
    <div class="codes-list">
      <div 
        v-for="activation in limitsInfo?.activations" 
        :key="activation.id" 
        class="code-item"
        :class="{ expired: isExpired(activation.expireTime) }"
      >
        <div class="code-tags">
          <span v-if="activation.extraBookmarks" class="tag blue">+{{ activation.extraBookmarks }} 书签</span>
          <span v-if="activation.extraCategories" class="tag green">+{{ activation.extraCategories }} 分类</span>
          <span class="tag" :class="isExpired(activation.expireTime) ? 'gray' : 'purple'">
            {{ isExpired(activation.expireTime) ? '已过期' : '有效' }}
          </span>
        </div>
        <div class="code-expiry">过期: {{ formatDate(activation.expireTime) }}</div>
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
  expiryDate: null,
  maxUses: 10
});
const createLoading = ref(false);
const activationCodes = ref([]);

// 弹窗状态
const showActivationHistoryDialog = ref(false);
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

const loadLimits = async () => {
  try {
    const result = await getMyLimitsAPI();
    if (result.data) {
      limitsInfo.value = result.data;
    }
  } catch (error) {
    console.error('加载限额信息失败:', error);
  }
};

const redeemCode = async () => {
  if (!activationCode.value.trim()) {
    ElMessage.warning('请输入激活码');
    return;
  }

  redeemLoading.value = true;
  try {
    await redeemActivationCodeAPI(activationCode.value.trim());
    ElMessage.success('激活码兑换成功！');
    activationCode.value = '';
    await loadLimits();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '兑换失败');
  } finally {
    redeemLoading.value = false;
  }
};

const isExpired = (expiryDate) => {
  if (!expiryDate) return false;
  return new Date(expiryDate) < new Date();
};

const formatDate = (dateString) => {
  if (!dateString) return '永久有效';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
};

const formatCodeExpiryDate = (code) => {
  if (!code.expireDays) return '永久有效';
  const createDate = new Date(code.createTime);
  const expiryDate = new Date(createDate);
  expiryDate.setDate(expiryDate.getDate() + code.expireDays);
  return expiryDate.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
};

const handleCreateCode = async () => {
  if (!createForm.value.expiryDate) {
    ElMessage.warning('请选择过期日期');
    return;
  }

  createLoading.value = true;
  try {
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const expiryDate = new Date(createForm.value.expiryDate);
    const diffTime = expiryDate.getTime() - now.getTime();
    const expireDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (expireDays <= 0) {
      ElMessage.error('过期日期必须大于当前日期');
      createLoading.value = false;
      return;
    }

    await createActivationCodeAPI({
      extraBookmarks: createForm.value.extraBookmarks,
      extraCategories: createForm.value.extraCategories,
      expireDays: expireDays,
      maxUses: createForm.value.maxUses
    });
    ElMessage.success('激活码创建成功！');
    showCreateCodeDialog.value = false;
    createForm.value.expiryDate = null;
    await loadActivationCodes();
  } catch (error) {
    ElMessage.error('创建失败：' + (error.response?.data?.message || '未知错误'));
  } finally {
    createLoading.value = false;
  }
};

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

const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code);
    ElMessage.success('已复制');
  } catch (error) {
    ElMessage.error('复制失败');
  }
};

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    loadUser();
    loadLimits();
    if (limitsInfo.value?.isAdmin) {
      loadActivationCodes();
    }
    isEditing.value = false;
  }
}, { immediate: true });

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
  padding: 20px 24px;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 22px;
  font-weight: 600;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
}

.user-info h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.user-info p {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.edit-btn {
  flex-shrink: 0;
}

/* 统计区域 */
.stats-section h3,
.activation-section h3 {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 12px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-item {
  padding: 12px;
  border-radius: 10px;
}

.stat-item.blue {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
}

.stat-item.green {
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
}

.stat-item.amber {
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
}

.stat-item.full-width {
  grid-column: span 2;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 12px;
  color: #4b5563;
  font-weight: 500;
}

.stat-value {
  font-size: 12px;
  font-weight: 600;
}

.stat-item.blue .stat-value { color: #2563eb; }
.stat-item.green .stat-value { color: #16a34a; }
.stat-item.amber .stat-value { color: #d97706; }

/* 激活码区域 */
.admin-actions {
  display: flex;
  gap: 10px;
}

.redeem-box {
  display: flex;
  gap: 10px;
}

.redeem-box .el-input {
  flex: 1;
}

.history-link {
  margin-top: 8px;
  padding: 0;
}

/* 退出按钮 */
.logout-btn {
  width: 100%;
  margin-top: 4px;
}

/* 编辑模式 */
.edit-content {
  padding-top: 8px;
}

.help-text {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.edit-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.edit-actions .el-button {
  flex: 1;
}

/* 创建激活码表单 */
.create-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* 激活码列表 */
.codes-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.code-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.code-item.expired {
  opacity: 0.6;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.code-header code {
  font-family: 'Consolas', monospace;
  font-size: 13px;
  color: #7c3aed;
  background: #f3e8ff;
  padding: 4px 10px;
  border-radius: 4px;
}

.code-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.tag.blue { background: #dbeafe; color: #1d4ed8; }
.tag.green { background: #dcfce7; color: #15803d; }
.tag.purple { background: #f3e8ff; color: #7c3aed; }
.tag.gray { background: #f3f4f6; color: #6b7280; }

.code-expiry {
  font-size: 11px;
  color: #9ca3af;
}

/* 滚动条 */
.codes-list::-webkit-scrollbar {
  width: 4px;
}

.codes-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 2px;
}

.codes-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
</style>
