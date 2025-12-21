<template>
  <div class="bookmark-list">
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">内容审核</h3>
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchQuery" 
            class="input" 
            placeholder="搜索书签标题、URL..."
            @keyup.enter="handleSearch"
          />
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
        </div>
      </div>
      <div class="card-body">
        <div class="table-container">
          <table class="table" v-if="bookmarks.length > 0">
            <thead>
              <tr>
                <th>书签信息</th>
                <th>用户</th>
                <th>分类</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="bookmark in bookmarks" :key="bookmark.id">
                <td>
                  <div class="bookmark-info">
                    <div class="bookmark-title">{{ bookmark.title }}</div>
                    <a :href="bookmark.url" target="_blank" class="bookmark-url">{{ bookmark.url }}</a>
                  </div>
                </td>
                <td>{{ bookmark.username }}</td>
                <td>{{ bookmark.categoryName || '-' }}</td>
                <td>{{ formatDate(bookmark.createTime) }}</td>
                <td>
                  <button class="btn btn-sm btn-danger" @click="deleteBookmark(bookmark)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="loading" class="loading"><div class="spinner"></div></div>

          <div v-if="!loading && bookmarks.length === 0" class="empty-state">
            <div class="empty-state-icon">🔖</div>
            <div class="empty-state-text">暂无书签</div>
          </div>
        </div>
      </div>
    </div>

    <ConfirmModal
      v-model:visible="confirmModal.visible"
      :title="confirmModal.title"
      :message="confirmModal.message"
      :type="confirmModal.type"
      :confirm-text="confirmModal.confirmText"
      @confirm="confirmModal.onConfirm"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import dayjs from 'dayjs'

const bookmarks = ref([])
const loading = ref(false)
const searchQuery = ref('')

const confirmModal = reactive({
  visible: false,
  title: '',
  message: '',
  type: 'danger',
  confirmText: '删除',
  onConfirm: () => {}
})

function formatDate(date) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

function handleSearch() {
  loadBookmarks()
}

async function loadBookmarks() {
  loading.value = true
  // TODO: 实现 API 调用
  setTimeout(() => {
    bookmarks.value = [
      { id: 1, title: 'GitHub', url: 'https://github.com', username: 'user1', categoryName: '开发工具', createTime: new Date().toISOString() },
      { id: 2, title: 'Google', url: 'https://google.com', username: 'user2', categoryName: '搜索引擎', createTime: new Date().toISOString() }
    ]
    loading.value = false
  }, 500)
}

function deleteBookmark(bookmark) {
  confirmModal.title = '删除书签'
  confirmModal.message = `确定要删除书签「${bookmark.title}」吗？此操作不可恢复。`
  confirmModal.type = 'danger'
  confirmModal.confirmText = '删除'
  confirmModal.onConfirm = () => {
    bookmarks.value = bookmarks.value.filter(b => b.id !== bookmark.id)
  }
  confirmModal.visible = true
}

onMounted(() => {
  loadBookmarks()
})
</script>

<style scoped>
.bookmark-list { display: flex; flex-direction: column; gap: 20px; }
.search-box { display: flex; gap: 12px; }
.search-box .input { width: 300px; }
.bookmark-info { max-width: 400px; }
.bookmark-title { font-weight: 500; margin-bottom: 4px; }
.bookmark-url { font-size: 13px; color: var(--text-muted); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>

