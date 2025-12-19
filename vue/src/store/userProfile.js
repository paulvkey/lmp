import { defineStore } from 'pinia'
import defaultAvatar from '@/assets/images/avatar.jpg'

// 定义信息存储
export const useUserProfileStore = defineStore('userProfile', {
  // 状态（存储用户信息）
  state: () => ({
    isLogin: false,
    userId: 0,
    username: '游客',
    avatar: defaultAvatar,
    sex: 3,
    phone: '',
    email: '',
    birthday: null,
    bio: '',
    lastLoginIp: null,
    lastLoginTime: null,
    token: null,
    rememberMe: {
      isRemember: false,
      username: '',
      password: '',
      expires: '',
    },
  }),

  actions: {
    // 设置信息
    setUserProfile(info) {
      this.isLogin = info.userId > 0
      this.userId = info.userId
      this.username = info.username
      this.avatar = info.avatar ? info.avatar : defaultAvatar
      this.sex = info.sex
      this.phone = info.phone
      this.email = info.email
      this.birthday = info.birthday
      this.bio = info.bio
      this.lastLoginIp = info.lastLoginIp
      this.lastLoginTime = info.lastLoginTime
    },
    setUserToken(token) {
      this.token = token
    },
    setRememberMe(username, password, expires) {
      this.rememberMe.username = username
      this.rememberMe.password = password
      this.rememberMe.expires = expires
      this.rememberMe.isRemember = true
    },
    clearRememberMe() {
      this.rememberMe.username = ''
      this.rememberMe.password = ''
      this.rememberMe.expires = ''
      this.rememberMe.isRemember = false
    },
    clearUserProfile() {
      this.userId = 0
      this.username = '游客'
      this.avatar = defaultAvatar
      this.phone = ''
      this.email = ''
      this.sex = 3
      this.birthday = null
      this.lastLoginIp = null
      this.lastLoginTime = null
      this.bio = ''
      this.isLogin = false
      this.token = null
      this.clearRememberMe()
    },
  },

  // 持久化配置
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'userProfile',
        storage: localStorage,
      },
    ],
  },
})
