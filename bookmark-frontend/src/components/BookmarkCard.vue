<template>
  <div class="bookmark-card border p-4 rounded shadow hover:shadow-lg transition">
    <div class="flex items-center space-x-4">
      <img :src="icon" alt="icon" class="w-10 h-10 rounded" />
      <div>
        <h3 class="text-lg font-bold">{{ title }}</h3>
        <p class="text-sm text-gray-600">{{ description }}</p>
      </div>
    </div>
    <div class="mt-4 flex justify-between items-center">
      <a :href="link" target="_blank" class="text-blue-500 underline">访问</a>
      <div class="space-x-2">
        <button class="text-yellow-500">编辑</button>
        <button class="text-red-500">删除</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "BookmarkCard",
  props: {
    title: String,
    description: String,
    link: String,
    icon: String,
  },
};
</script>

<style scoped>
.bookmark-card {
  max-width: 300px;
}
</style>