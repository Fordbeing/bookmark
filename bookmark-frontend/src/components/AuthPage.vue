<template>
  <div class="auth-page min-h-screen bg-gradient-to-br from-blue-600 to-blue-900 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <!-- 返回按钮 -->
      <button 
        @click="goBack"
        class="mb-8 text-white hover:text-blue-200 transition-colors flex items-center gap-2"
      >
        <span class="text-lg">←</span> 返回
      </button>

      <!-- 卡片容器 -->
      <div class="bg-white rounded-xl shadow-2xl overflow-hidden">
        <!-- 标签切换 -->
        <div class="flex border-b">
          <button
            @click="isLogin = true"
            :class="[
              'flex-1 py-4 font-semibold transition-all duration-200',
              isLogin 
                ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50' 
                : 'text-gray-600 hover:text-gray-800'
            ]"
          >
            登录
          </button>
          <button
            @click="isLogin = false"
            :class="[
              'flex-1 py-4 font-semibold transition-all duration-200',
              !isLogin 
                ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50' 
                : 'text-gray-600 hover:text-gray-800'
            ]"
          >
            注册
          </button>
        </div>

        <!-- 表单内容 -->
        <div class="p-8">
          <!-- 登录表单 -->
          <div v-if="isLogin" class="space-y-4 animate-fade-in">
            <h2 class="text-2xl font-bold text-gray-800 mb-6">欢迎回来</h2>
            
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">邮箱</label>
              <input
                v-model="loginForm.email"
                type="email"
                placeholder="输入您的邮箱"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
              <input
                v-model="loginForm.password"
                type="password"
                placeholder="输入您的密码"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>

            <div class="flex items-center gap-2">
              <input
                v-model="loginForm.remember"
                type="checkbox"
                id="remember"
                class="rounded"
              />
              <label for="remember" class="text-sm text-gray-600">记住我</label>
            </div>

            <button
              @click="handleLogin"
              :disabled="loading"
              class="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white font-semibold py-2.5 rounded-lg transition-all duration-200"
            >
              {{ loading ? '登录中...' : '登录' }}
            </button>

            <div class="relative my-4">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-gray-300"></div>
              </div>
              <div class="relative flex justify-center text-sm">
                <span class="px-2 bg-white text-gray-600">或使用</span>
              </div>
            </div>

            <div class="grid grid-cols-3 gap-2">
              <button class="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-all text-xl">
                🔵
              </button>
              <button class="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-all text-xl">
                🐙
              </button>
              <button class="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-all text-xl">
                🔴
              </button>
            </div>
          </div>

          <!-- 注册表单 -->
          <div v-else class="space-y-4 animate-fade-in">
            <h2 class="text-2xl font-bold text-gray-800 mb-6">创建账户</h2>
            
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
              <input
                v-model="registerForm.username"
                type="text"
                placeholder="输入用户名"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">邮箱</label>
              <input
                v-model="registerForm.email"
                type="email"
                placeholder="输入您的邮箱"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
              <input
                v-model="registerForm.password"
                type="password"
                placeholder="设置密码（至少8位）"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
              <input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
              />
            </div>

            <div class="flex items-start gap-2">
              <input
                v-model="registerForm.agree"
                type="checkbox"
                id="agree"
                class="rounded mt-1"
              />
              <label for="agree" class="text-sm text-gray-600">
                我同意
                <a href="#" class="text-blue-600 hover:underline">服务条款</a>
                和
                <a href="#" class="text-blue-600 hover:underline">隐私政策</a>
              </label>
            </div>

            <button
              @click="handleRegister"
              :disabled="loading"
              class="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white font-semibold py-2.5 rounded-lg transition-all duration-200"
            >
              {{ loading ? '注册中...' : '创建账户' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 底部链接 -->
      <div class="mt-6 text-center text-white text-sm">
        <p>{{ isLogin ? '没有账户？' : '已有账户？' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { loginAPI, registerAPI } from '../api/auth';

const emit = defineEmits(['back', 'login-success']);

const isLogin = ref(true);
const loading = ref(false);

const loginForm = ref({
  email: '',
  password: '',
  remember: false,
});

const registerForm = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: false,
});

const handleLogin = async () => {
  if (!loginForm.value.email || !loginForm.value.password) {
    ElMessage.warning('请输入邮箱和密码');
    return;
  }

  loading.value = true;
  try {
    const result = await loginAPI(loginForm.value);
    if (result.data && result.data.token) {
      // 保存token和用户信息
      console.log('[Login Success] Token received:', result.data.token.substring(0, 30) + '...');
      localStorage.setItem('token', result.data.token);
      localStorage.setItem('user', JSON.stringify(result.data));
      console.log('[Login Success] Token saved to localStorage');
      console.log('[Login Success] Verify:', localStorage.getItem('token')?.substring(0, 30) + '...');
      ElMessage.success('登录成功！');
      emit('login-success', result.data);
    }
  } catch (error) {
    console.error('登录失败:', error);
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    ElMessage.warning('请填写所有字段');
    return;
  }

  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  if (registerForm.value.password.length < 6) {
    ElMessage.warning('密码长度至少6位');
    return;
  }

  if (!registerForm.value.agree) {
    ElMessage.warning('请同意服务条款');
    return;
  }

  loading.value = true;
  try {
    const result = await registerAPI({
      username: registerForm.value.username,
      email: registerForm.value.email,
      password: registerForm.value.password
    });
    
    if (result.data && result.data.token) {
      // 保存token和用户信息
      localStorage.setItem('token', result.data.token);
      localStorage.setItem('user', JSON.stringify(result.data));
      ElMessage.success('注册成功！');
      emit('login-success', result.data);
    }
  } catch (error) {
    console.error('注册失败:', error);
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  emit('back');
};
</script>

<style scoped>
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-out;
}
</style>
