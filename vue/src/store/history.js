import { defineStore } from 'pinia'

// 定义信息存储
export const useHistoryStore = defineStore('history', {
  // 状态（存储信息）
  state: () => ({
    isExpanded: false,
    isLoading: false,
    historyList: [],
    needReloadSession: false,
    selectedSessionId: null,
    activeSessionMenuId: null,
    isLoadingSession: false,
    isSessionCollected: false,
    hasLoadedHistory: false,
  }),

  actions: {
    clearHistory() {
      this.isExpanded = false
      this.isLoading = false
      this.historyList = []
      this.needReloadSession = false
      this.selectedSessionId = null
      this.activeSessionMenuId = null
      this.isLoadingSession = false
      this.isSessionCollected = false
      this.hasLoadedHistory = false;
    }
  },

  // 持久化配置
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'history',
        storage: localStorage,
      },
    ],
  },
})
