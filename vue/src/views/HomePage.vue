<template>
  <div class="container" @click="hideMenuOutsideClick">
    <!-- 左侧菜单 -->
    <div class="left-menu-wrapper">
      <!-- 左侧上部分菜单信息 -->
      <LeftMenu />

      <!-- 左侧底部用户信息 -->
      <UserProfile />
    </div>

    <!-- 右侧对话 -->
    <div class="right-chat-wrapper">
      <RightChat />
    </div>
  </div>
</template>

<script setup>
import { ref, provide, nextTick, watch, onMounted, onBeforeUnmount } from 'vue'
import '@/assets/css/Global.css'
import { checkLogin, throttle } from '@/utils/commonUtils.js'
import LeftMenu from '@/components/home/LeftMenu.vue'
import UserProfile from '@/components/home/UserProfile.vue'
import RightChat from '@/components/home/RightChat.vue'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { useHistoryStore } from '@/store/history.js'
import { useCollectionStore } from '@/store/collection.js'
import { useChatStore } from '@/store/chat.js'
import { useFunctionStore } from '@/store/function.js'
import { useUserProfileStore } from '@/store/userProfile.js'

/* 变量声明或者定义 */
const userProfile = useUserProfileStore()
const homeStatus = useHomeStatusStore()
const history = useHistoryStore()
const collection = useCollectionStore()
const chat = useChatStore()
const func = useFunctionStore()

/* 函数声明 */
const hideMenuOutsideClick = throttle((e) => {
  // 隐藏用户菜单
  if (
    homeStatus.showUserMenu &&
    !e.target.closest('.user-profile') &&
    !e.target.closest('.user-menus')
  ) {
    homeStatus.showUserMenu = false
  }
  // 隐藏三点功能菜单
  if (
    history.activeSessionMenuId &&
    !e.target.closest('.history-item-more') &&
    !e.target.closest('.history-item-dots')
  ) {
    const activeItem = document.querySelector(`[data-session-id="${history.activeSessionMenuId}"]`)
    activeItem?.classList.remove('history-item-more-active')
    history.activeSessionMenuId = null
  }
  // 点击弹窗外部时
  if (collection.isDialogShow && !e.target.closest('.collection-dialog')) {
    collection.isDialogShow = false
  }
  // 点击所有文件弹窗外部时
  if (chat.showAllFiles && !e.target.closest('.all-files-dialog')) {
    chat.showAllFiles = false
  }
})

// 重置当前对话窗口
const resetCurrentChat = () => {
  chat.clearChat()
  nextTick(() => {
    func.chatMsgScrollTop()
    chat.showScrollBtn = false
  })
  nextTick(() => {
    func.chatMsgInputFocus()
  })
}

// 监听页面刷新
window.addEventListener('beforeunload', () => {
  homeStatus.clearHomeStatus()
  collection.clearCollection()
  chat.clearChat()
  if (!checkLogin(userProfile)) {
    history.clearHistory()
  }
})

onMounted(() => {
  homeStatus.initIsNewSession()
})

provide('resetCurrentChat', resetCurrentChat)
</script>

<style scoped>
.left-menu-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: 14vw;
  min-width: var(--left-menu-min-width);
  max-width: var(--left-menu-max-width);
  border-right: 1px solid #e5e7eb;
  transition: width 0.2s ease;
  overflow: visible;
  z-index: 2000;
}

.right-chat-wrapper {
  z-index: 1000;
}
</style>
