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
    historyIds: [],
  }),

  getters: {
    getHistoryIdx: (state) => (targetId) => {
      if (!targetId || !state.historyIds.length) return -1;
      return state.historyIds.findIndex((id) => {
        return id === targetId;
      });
    },
  },

  actions: {
    initHistoryIds() {
      this.historyIds = [];
      if (this.historyList.length > 0) {
        this.historyList.filter(history => {
          this.historyIds.push(history.id)
        });
      }
    },
    deleteHistoryId(targetId) {
      if (!targetId || !this.historyIds.length) return;
      this.historyIds = this.historyIds.filter(id => id !== targetId);
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
      this.historyIds = [];
    },
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
