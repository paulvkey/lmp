<template>
  <div class="new-chat">
    <svg
      class="new-chat-svg"
      xmlns="http://www.w3.org/2000/svg"
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <path
        d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"
      ></path>
      <polyline points="22,6 12,13 2,6"></polyline>
    </svg>
    <span class="new-chat-text left-menu-text">新对话</span>
  </div>
</template>

<script>
import "../../assets/css/LeftMenu.css"

export default {
  name: 'NewChat',
}
</script>

<style scoped>

.new-chat-svg {
  flex-shrink: 0;
}

.new-chat-text {
  padding: 2px 0;
  margin-left: 10px;
}

</style>
