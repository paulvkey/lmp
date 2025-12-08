import { defineStore } from 'pinia'

// 定义信息存储
export const useHomeStatusStore = defineStore('homeStatus', {
  // 状态（存储信息）
  state: () => ({
    currentMenu: 'new',
    isNewSession: false,
    showUserMenu: false,
    isRenameDialogShow: false,
    renamingSessionId: null,
  }),

  actions: {
    clearHomeStatus() {
      this.currentMenu = 'new'
      this.isNewSession = false
      this.showUserMenu = false
      this.isRenameDialogShow = false
      this.renamingSessionId = null
    },
    initIsNewSession() {
      if (this.currentMenu === 'new') {
        this.isNewSession = true
      }
    },
    setCurrentMenu(newCurrentMenu) {
      this.currentMenu = newCurrentMenu
    }
  },

  // 持久化配置
  persist: {
    enabled: false,
    strategies: [
      {
        key: 'homeStatus',
        storage: sessionStorage,
        paths: [],
      },
    ],
  },
})
