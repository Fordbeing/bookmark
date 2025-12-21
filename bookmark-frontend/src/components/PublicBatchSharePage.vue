<template>
  <div class="public-share-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 密码验证 -->
    <div v-else-if="needPassword && !verified" class="password-form">
      <div class="form-card">
        <div class="form-icon">🔒</div>
        <h2>此分享需要密码访问</h2>
        <el-input 
          v-model="password" 
          type="password" 
          placeholder="请输入访问密码"
          show-password
          @keyup.enter="verifyPassword"
        />
        <el-button type="primary" @click="verifyPassword" :loading="verifying">
          验证密码
        </el-button>
        <p v-if="passwordError" class="error-text">{{ passwordError }}</p>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">😢</div>
      <h2>{{ error }}</h2>
      <p>请检查链接是否正确</p>
    </div>

    <!-- 分享内容 -->
    <div v-else-if="shareData" class="share-content">
      <header class="share-header">
        <h1>📚 {{ shareData.title }}</h1>
        <div class="share-meta">
          <span>📖 {{ shareData.bookmarks?.length || 0 }} 个书签</span>
          <span>👁️ {{ shareData.viewCount }} 次访问</span>
          <span>📅 分享于 {{ formatDate(shareData.createTime) }}</span>
        </div>
      </header>

      <div class="bookmarks-grid" v-if="shareData.bookmarks && shareData.bookmarks.length > 0">
        <a 
          v-for="bookmark in shareData.bookmarks" 
          :key="bookmark.id"
          :href="bookmark.url"
          target="_blank"
          class="bookmark-card"
        >
          <div class="bookmark-icon">
            <img v-if="bookmark.iconUrl" :src="bookmark.iconUrl" @error="(e) => e.target.style.display='none'" />
            <span v-else>{{ bookmark.title?.charAt(0)?.toUpperCase() || '?' }}</span>
          </div>
          <div class="bookmark-content">
            <div class="bookmark-title">{{ bookmark.title }}</div>
            <div class="bookmark-url">{{ extractDomain(bookmark.url) }}</div>
            <div v-if="bookmark.description" class="bookmark-desc">{{ bookmark.description }}</div>
          </div>
        </a>
      </div>

      <el-empty v-else description="此分享暂无书签" />

      <footer class="share-footer">
        <p>由 <strong>书签管理系统</strong> 提供技术支持</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import request from '../api/request';

const route = useRoute();
const shareCode = ref('');
const loading = ref(true);
const needPassword = ref(false);
const verified = ref(false);
const password = ref('');
const passwordError = ref('');
const verifying = ref(false);
const error = ref('');
const shareData = ref(null);

onMounted(async () => {
  shareCode.value = route.params.code;
  if (!shareCode.value) {
    error.value = '无效的分享链接';
    loading.value = false;
    return;
  }
  await checkShare();
});

// 检查分享状态
const checkShare = async () => {
  try {
    const result = await request.get(`/public/share/batch/${shareCode.value}/check`);
    if (result.data) {
      needPassword.value = result.data.needPassword;
      if (!needPassword.value) {
        await loadShareContent();
      }
    }
  } catch (e) {
    error.value = '分享不存在或已过期';
  } finally {
    loading.value = false;
  }
};

// 验证密码
const verifyPassword = async () => {
  if (!password.value) {
    passwordError.value = '请输入密码';
    return;
  }
  
  verifying.value = true;
  passwordError.value = '';
  
  try {
    await loadShareContent();
    verified.value = true;
  } catch (e) {
    passwordError.value = e.response?.data?.message || '密码错误';
  } finally {
    verifying.value = false;
  }
};

// 加载分享内容
const loadShareContent = async () => {
  const params = needPassword.value ? { password: password.value } : {};
  const result = await request.get(`/public/share/batch/${shareCode.value}`, { params });
  if (result.data) {
    shareData.value = result.data;
  }
};

// 格式化日期
const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleDateString('zh-CN');
};

// 提取域名
const extractDomain = (url) => {
  try {
    const domain = new URL(url).hostname;
    return domain.replace('www.', '');
  } catch {
    return url;
  }
};
</script>

<style scoped>
.public-share-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: white;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.password-form {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

.form-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  max-width: 400px;
  width: 100%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}

.form-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.form-card h2 {
  margin-bottom: 30px;
  color: #333;
}

.form-card .el-input {
  margin-bottom: 20px;
}

.form-card .el-button {
  width: 100%;
}

.error-text {
  color: #f56c6c;
  margin-top: 15px;
}

.share-content {
  max-width: 1200px;
  margin: 0 auto;
}

.share-header {
  text-align: center;
  color: white;
  margin-bottom: 40px;
}

.share-header h1 {
  font-size: 2.5rem;
  margin-bottom: 15px;
}

.share-meta {
  display: flex;
  gap: 30px;
  justify-content: center;
  opacity: 0.8;
  flex-wrap: wrap;
}

.bookmarks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.bookmark-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  gap: 15px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.bookmark-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
}

.bookmark-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 20px;
  flex-shrink: 0;
}

.bookmark-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.bookmark-content {
  flex: 1;
  min-width: 0;
}

.bookmark-title {
  font-weight: 600;
  font-size: 16px;
  color: #333;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bookmark-url {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.bookmark-desc {
  font-size: 13px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.share-footer {
  text-align: center;
  margin-top: 60px;
  color: rgba(255,255,255,0.7);
}

.share-footer strong {
  color: white;
}
</style>
