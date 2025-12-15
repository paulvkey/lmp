import { defineStore } from 'pinia'
import { nextTick } from 'vue'

// 定义信息存储
export const useChatStore = defineStore('chat', {
  state: () => ({
    chatTitle: '新对话',
    newChatTitle: '',
    inputData: '',
    isSending: false,
    messageList: [],
    uploadedFiles: [],
    showAllFiles: false,
    filesUploaded: false,
    isDeepThink: false,
    isNetworkSearch: false,
    showScrollBtn: false,
    isScrolling: false,
    isUserScrolled: false,
    modelInfo: {
      newSession: true,
      isLogin: false,
      userId: 0,
      sessionId: 0,
      sessionTitle: '',
      aiModelId: 1,
      isDeleted: 0,
      createdAt: null,
      updatedAt: null,
      lastMessageTime: null,
      isPinned: 0,
      isCollected: 0,
      messageType: 1,
      messageList: [],
      sendTime: null,
      tokenCount: 0,
    },
  }),

  actions: {
    initModelInfo(userProfile) {
      this.modelInfo.isLogin = userProfile.isLogin
      this.modelInfo.userId = userProfile.userId
      this.modelInfo.sessionTitle = this.chatTitle
    },
    clearInput() {
      this.inputData = ''
      this.isSending = false
    },
    clearChatFiles() {
      this.inputData = ''
      this.isSending = false
      this.uploadedFiles = []
      this.showAllFiles = false
      this.filesUploaded = false
    },
    clearChat() {
      this.chatTitle = '新对话'
      this.newChatTitle = ''
      this.inputData = ''
      this.isSending = false
      if (this.messageList.length > 0) {
        this.messageList.splice(0, this.messageList.length)
        nextTick().then(() => {
          this.messageList = []
        })
      }
      this.uploadedFiles = []
      this.showAllFiles = false
      this.filesUploaded = false
      this.isDeepThink = false
      this.isNetworkSearch = false
      this.showScrollBtn = false
      this.isScrolling = false
      this.isUserScrolled = false
      this.clearModelInfo()
    },
    clearModelInfo() {
      this.modelInfo = {
        newSession: true,
        isLogin: false,
        userId: 0,
        sessionId: 0,
        sessionTitle: '',
        aiModelId: 1,
        isDeleted: 0,
        createdAt: null,
        updatedAt: null,
        lastMessageTime: null,
        isPinned: 0,
        isCollected: 0,
        messageType: 1,
        messageList: [],
        sendTime: null,
        tokenCount: 0,
        isDeepThink: 0,
        isNetworkSearch: 0,
      }
    },
  },

  // 持久化配置
  persist: {
    enabled: false,
    strategies: [
      {
        key: 'chat',
        storage: sessionStorage,
        paths: [
        ],
      },
    ],
  },
})
