<template>
  <div class="login-page min-h-screen bg-gradient-to-br from-blue-600 to-blue-800 flex items-center justify-center p-4">
    <div class="login-container bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="text-5xl mb-3">🔖</div>
        <h1 class="text-3xl font-bold text-gray-800">书签管理</h1>
        <p class="text-gray-500 mt-2">智能管理你的在线书签</p>
      </div>

      <!-- Tab切换 -->
      <div class="flex gap-2 mb-6 bg-gray-100 p-1 rounded-lg">
        <button
          @click="isLogin = true"
          :class="[
            'flex-1 py-2 rounded-md font-medium transition-all duration-200',
            isLogin
              ? 'bg-blue-500 text-white'
              : 'text-gray-600 hover:text-gray-800'
          ]"
        >
          登录
        </button>
        <button
          @click="isLogin = false"
          :class="[
            'flex-1 py-2 rounded-md font-medium transition-all duration-200',
            !isLogin
              ? 'bg-blue-500 text-white'
              : 'text-gray-600 hover:text-gray-800'
          ]"
        >
          注册
        </button>
      </div>

      <!-- 表单 -->
      <el-form :model="form" label-width="0" @submit.prevent="handleSubmit">
        <!-- 邮箱 -->
        <el-form-item class="mb-4">
          <el-input
            v-model="form.email"
            placeholder="邮箱地址"
            size="large"
            prefix-icon="Message"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <!-- 注册时的用户名 -->
        <el-form-item v-if="!isLogin" class="mb-4">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item class="mb-4">
          <el-input
            v-model="form.password"
            placeholder="密码"
            type="password"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <!-- 记住密码（登录时） -->
        <el-form-item v-if="isLogin" class="mb-6">
          <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
          <a href="#" class="float-right text-blue-500 hover:text-blue-700">忘记密码？</a>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-button
          type="primary"
          size="large"
          class="w-full !font-medium"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ isLogin ? '登录' : '注册' }}
        </el-button>
      </el-form>

      <!-- 第三方登录 -->
      <div class="mt-6">
        <div class="relative mb-4">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-gray-300"></div>
          </div>
          <div class="relative flex justify-center text-sm">
            <span class="px-2 bg-white text-gray-500">或者</span>
          </div>
        </div>

        <div class="flex gap-2">
          <button class="flex-1 p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition">
            <span class="text-2xl">🐱</span>
          </button>
          <button class="flex-1 p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition">
            <span class="text-2xl">📘</span>
          </button>
          <button class="flex-1 p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition">
            <span class="text-2xl">🔴</span>
          </button>
        </div>
      </div>

      <!-- 条款 -->
      <p v-if="!isLogin" class="text-xs text-gray-500 text-center mt-6">
        注册表示你同意我们的
        <a href="#" class="text-blue-500 hover:text-blue-700">服务条款</a>
        和
        <a href="#" class="text-blue-500 hover:text-blue-700">隐私政策</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const isLogin = ref(true);
const loading = ref(false);
const form = ref({
  email: '',
  password: '',
  username: '',
  rememberMe: false,
});

const handleSubmit = async () => {
  if (!form.value.email || !form.value.password) {
    ElMessage.warning('请填写邮箱和密码');
    return;
  }

  if (!isLogin.value && !form.value.username) {
    ElMessage.warning('请填写用户名');
    return;
  }

  loading.value = true;
  try {
    // 模拟API调用
    setTimeout(() => {
      ElMessage.success(isLogin.value ? '登录成功' : '注册成功');
      // 这里应该保存用户信息到localStorage或发送到后端
      localStorage.setItem('user', JSON.stringify({
        email: form.value.email,
        username: form.value.username,
        isLoggedIn: true,
      }));
      // 跳转到主页（实际应用中使用路由）
      window.location.href = '/';
    }, 1000);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  animation: slideIn 0.5s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
