<template>
  <!-- 用户信息 -->
  <div class="user-profile" @click.stop="toggleUserMenu">
    <img ref="avatarRef" alt="头像" class="user-avatar" />
    <div v-if="userProfile.isLogin">
      <span ref="usernameRef" class="user-name" />
    </div>
    <div v-else>
      <span ref="usernameRef" class="user-name">登录</span>
    </div>
  </div>

  <!-- 用户功能菜单 -->
  <div class="user-menus" v-if="homeStatus.showUserMenu" @click.stop>
    <div class="user-login" v-if="userProfile.isLogin">
      <div class="user-item" @click="goToUserProfile">个人中心</div>
      <div class="user-item" @click="logout">退出登录</div>
    </div>
    <div class="user-item" @click="goToLogin" v-else>登录账号</div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import '@/assets/css/home/UserProfile.css'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { ElMessage } from 'element-plus'
import router from '@/router/index.js'
import { useHistoryStore } from '@/store/history.js'
import { useChatStore } from '@/store/chat.js'
import { useCollectionStore } from '@/store/collection.js'
import { checkLogin } from '@/utils/commonUtils.js'

const userProfile = useUserProfileStore()
const homeStatus = useHomeStatusStore()
const history = useHistoryStore()
const collection = useCollectionStore()
const chat = useChatStore()
const usernameRef = ref(null)
const avatarRef = ref(null)

const setUserInfo = async () => {
  await nextTick()
  if (usernameRef.value) {
    usernameRef.value.textContent = userProfile.username
  }
  if (avatarRef.value) {
    avatarRef.value.src = userProfile.avatar
  }
}

watch(() => [userProfile.username, userProfile.avatar], setUserInfo, { immediate: true })

const toggleUserMenu = async () => {
  if (!checkLogin(userProfile)) {
    goToLogin()
  } else {
    // 菜单切换逻辑
    homeStatus.showUserMenu = !homeStatus.showUserMenu
    if (homeStatus.showUserMenu) {
      const avatarRect = avatarRef.value.getBoundingClientRect()
      const userMenuEl = document.querySelector('.user-menus')
      if (userMenuEl) {
        userMenuEl.style.left = `${avatarRect.left}px`
        userMenuEl.style.bottom = `${window.innerHeight - avatarRect.top}px`
      }
    }
  }
}

const goToLogin = () => {
  homeStatus.showUserMenu = false
  window.open('/login', '_blank')
}

const goToUserProfile = () => {
  homeStatus.showUserMenu = false
  window.open('/user-profile', '_blank')
}

const logout = () => {
  userProfile.clearUserProfile()
  homeStatus.clearHomeStatus()
  history.clearHistory()
  collection.clearCollection()
  chat.clearChat()
  ElMessage.success('已退出登录')
  setTimeout(() => {
    window.open('/home', '_blank')
  }, 300)
}
</script>

<style scoped></style>
