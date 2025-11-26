import { defineStore } from 'pinia'
import request from '@/utils/request.js'
import { ElMessage } from 'element-plus'
import defaultAvatar from '@/assets/image/avatar.jpg'

// 定义用户信息存储
export const useUserStore = defineStore('user', {
  // 状态（存储用户信息）
  state: () => ({
    userInfo: {},
    isLogin: false,
    token: null,
  }),

  // 计算属性（可选，用于派生状态）
  getters: {
    userId: (state) => state.userInfo?.userId || 0,
    username: (state) => state.userInfo?.username || '游客',
    avatar: (state) => state.userInfo?.avatar || defaultAvatar,
    phone: (state) => state.userInfo?.phone || '',
    email: (state) => state.userInfo?.email || '',
    sex: (state) => state.userInfo?.sex || 3,
    birthday: (state) => state.userInfo?.birthday || null,
    status: (state) => state.userInfo?.status || 1,
    lastLoginIp: (state) => state.userInfo?.lastLoginIp || null,
    lastLoginTime: (state) => state.userInfo?.lastLoginTime || null,
    bio: (state) => state.userInfo?.bio || '',
    token: (state) => state?.token || '',
  },

  // 方法（用于修改状态，支持异步）
  actions: {
    // 从接口获取用户信息并存储
    async fetchUserInfo(username) {
      try {
        const response = await request('post', '/user/info', { username: username })
        if (response.code === 200) {
          this.userInfo = response.data
          this.isLogin = true
          return response.data
        } else {
          ElMessage.error(`获取用户信息失败: ${response.msg}`)
          this.isLogin = false
          return null
        }
      } catch (error) {
        console.error('用户信息请求失败', error)
        this.isLogin = false
        return null
      }
    },

    // 设置用户信息
    setUserInfo(info) {
      this.userInfo = info
      this.isLogin = true
    },

    setUserToken(token) {
      this.token = token
    },

    // 清除用户信息（如退出登录）
    clearUserInfo() {
      this.userInfo = null
      this.isLogin = false
      this.token = null
    },
  },

  // 持久化配置（刷新页面后保留状态）
  persist: {
    enabled: true, // 开启持久化
    strategies: [
      {
        key: 'userInfo', // 存储的key
        storage: localStorage, // 存储方式（localStorage/sessionStorage）
      },
    ],
  },
})
