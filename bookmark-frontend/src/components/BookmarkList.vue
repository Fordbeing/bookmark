<template>
  <div class="bookmark-list grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 p-4">
    <BookmarkCard
      v-for="bookmark in bookmarks"
      :key="bookmark.id"
      :title="bookmark.title"
      :description="bookmark.description"
      :link="bookmark.link"
      :icon="bookmark.icon"
    />
  </div>
</template>

<script>
import BookmarkCard from "./BookmarkCard.vue";

export default {
  name: "BookmarkList",
  components: {
    BookmarkCard,
  },
  data() {
    return {
      bookmarks: [
        {
          id: 1,
          title: "Google",
          description: "Search the world's information, including webpages, images, videos and more.",
          link: "https://www.google.com",
          icon: "https://www.google.com/favicon.ico",
        },
        {
          id: 2,
          title: "YouTube",
          description: "Enjoy the videos and music you love, upload original content, and share it all with friends.",
          link: "https://www.youtube.com",
          icon: "https://www.youtube.com/favicon.ico",
        },
      ],
    };
  },
};
</script>

<style scoped>
.bookmark-list {
  padding: 1rem;
}
</style>