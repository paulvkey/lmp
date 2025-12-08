import { defineStore } from 'pinia'

// 定义信息存储
export const useFunctionStore = defineStore('function', {
  state: () => ({
    chatMsgScrollTop: null,
    chatMsgInputFocus: null,
    scrollToBottom: null,
    scrollBtnCleanup: null,
    triggerNewChat: null,
  }),

  actions: {
    setChatMsgScrollTop(func) {
      this.chatMsgScrollTop = func
    },
    setChatMsgInputFocus(func) {
      this.chatMsgInputFocus = func
    },
    setScrollToBottom(func) {
      this.scrollToBottom = func
    },
    setScrollBtnCleanup(func) {
      this.scrollBtnCleanup = func
    },
    setTriggerNewChat(func) {
      this.triggerNewChat = func
    }
  },

  // 持久化配置
  persist: {
    enabled: false,
    strategies: [
      {
        key: 'function',
        storage: sessionStorage,
        paths: [],
      },
    ],
  },
})
