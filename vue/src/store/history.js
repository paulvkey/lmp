import { defineStore } from 'pinia'

// 定义信息存储
export const useHistoryStore = defineStore('history', {
  // 状态（存储信息）
  state: () => ({
    isExpanded: false,
    isLoading: false,
    historyList: [],
    selectedSessionId: null,
    activeSessionMenuId: null,
    isLoadingSession: false,
    isSessionCollected: false,
    hasLoadedHistory: false,
    currentSessionId: null,
    historySet: new Set(),
  }),

  actions: {
    initHistorySet() {
      this.historySet.clear();
      if (this.historyList.length > 0) {
        this.historyList.filter(history => {
          if (!this.historySet.has(history.id)) {
            this.historySet.add(history.id);
          }
        });
      }
    },
    clearCurrent() {
      this.activeSessionMenuId = null
      this.selectedSessionId = null
      this.currentSessionId = null
    },
    clearHistory() {
      this.isExpanded = false
      this.isLoading = false
      this.historyList = []
      this.selectedSessionId = null
      this.activeSessionMenuId = null
      this.isLoadingSession = false
      this.isSessionCollected = false
      this.hasLoadedHistory = false
      this.currentSessionId = null
      this.historySet.clear();
    },
  },

  // 持久化配置
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'history',
        storage: localStorage,
        paths: [
          'isExpanded',
          'isLoading',
          'historyList',
          'selectedSessionId',
          'activeSessionMenuId',
          'isLoadingSession',
          'isSessionCollected',
          'hasLoadedHistory',
          'currentSessionId'
        ],
      },
    ],
  },
})
