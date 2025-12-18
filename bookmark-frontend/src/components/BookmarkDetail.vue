<template>
  <div class="book-card-detail">
    <h2 class="text-2xl font-bold text-gray-800 mb-4">{{ bookmark.title }}</h2>
    
    <div class="grid grid-cols-2 gap-4 mb-6">
      <div class="p-4 bg-gray-50 rounded-lg">
        <p class="text-xs text-gray-500 mb-1">网址</p>
        <a :href="bookmark.url" target="_blank" class="text-blue-600 hover:underline truncate">
          {{ bookmark.url }}
        </a>
      </div>
      
      <div class="p-4 bg-gray-50 rounded-lg">
        <p class="text-xs text-gray-500 mb-1">分类</p>
        <span class="text-gray-800">{{ bookmark.category }}</span>
      </div>
    </div>

    <div class="p-4 bg-gray-50 rounded-lg mb-6">
      <p class="text-xs text-gray-500 mb-2">描述</p>
      <p class="text-gray-700">{{ bookmark.description }}</p>
    </div>

    <div class="flex gap-2">
      <el-button type="primary">编辑</el-button>
      <el-button type="danger">删除</el-button>
      <el-button @click="openBookmark">打开网站</el-button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  bookmark: {
    type: Object,
    required: true,
  },
});

const openBookmark = () => {
  window.open(props.bookmark.url, '_blank');
};
</script>

<style scoped>
.book-card-detail {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
