<template>
  <!-- 用户信息 -->
  <div class="user-profile" @click.stop="toggleUserMenu">
    <img ref="avatarRef" alt="头像" class="user-avatar" />
    <span ref="usernameRef" class="user-name" />
  </div>

  <!-- 用户功能菜单 -->
  <div class="user-menus" v-if="homeStatus.showUserMenu" @click.stop>
    <div class="user-item" @click="goToUserProfile" v-if="userProfile.isLogin">个人中心</div>
    <div class="user-item" @click="goToLogin" v-else>登录账号</div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import '@/assets/css/home/UserProfile.css'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'

const userProfile = useUserProfileStore()
const homeStatus = useHomeStatusStore()
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

const goToLogin = () => {
  homeStatus.showUserMenu = false
  window.open('/login', '_blank')
}

const goToUserProfile = () => {
  homeStatus.showUserMenu = false
  window.open('/user-profile', '_blank')
}
</script>

<style scoped></style>
