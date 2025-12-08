import { defineStore } from 'pinia'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'

// 定义信息存储
export const useChatStore = defineStore('chat', {
  // 状态（存储信息）
  state: () => ({
    chatTitle: '新对话',
    newChatTitle: '',
    inputData: '',
    messageList: [],
    uploadedFiles: [],
    showAllFiles: false,
    isDeepActive: false,
    isNetworkActive: false,
    showScrollBtn: false,
    isScrolling: false,
    modelInfo: {
      newSession: true,
      isLogin: false,
      userId: 0,
      sessionId: 0,
      sessionTitle: '新对话',
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
      fileIds: '',
      tokenCount: 0,
      isDeepThink: 0,
      isNetworkSearch: 0,
      deepThinkStep: '',
    },
    isInputEnabled: false,
    isSending: false,
    filesUploaded: false,
  }),

  actions: {
    initModelInfo(userProfile) {
      this.modelInfo.isLogin = userProfile.isLogin
      this.modelInfo.userId = userProfile.userId
    },
    resetChat() {
      this.inputData = ''
      this.uploadedFiles = []
      this.showAllFiles = false
      this.isSending = false
    },
    clearChat() {
      this.chatTitle = '新对话'
      this.newChatTitle = ''
      this.inputData = ''
      this.messageList = []
      this.uploadedFiles = []
      this.showAllFiles = false
      this.isDeepActive = false
      this.isNetworkActive = false
      this.showScrollBtn = false
      this.isScrolling = false
      this.clearModelInfo()
      this.isInputEnabled = false
      this.isSending = false
      this.filesUploaded = false
    },
    clearModelInfo() {
      this.modelInfo = {
        newSession: true,
        isLogin: false,
        userId: 0,
        sessionId: 0,
        sessionTitle: '新对话',
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
        fileIds: '',
        tokenCount: 0,
        isDeepThink: 0,
        isNetworkSearch: 0,
        deepThinkStep: '',
      }
    },
    initIsInputEnabled(homeStatus) {
      if (homeStatus.currentMenu === 'new') {
        this.isInputEnabled = true
      }
    },
  },

  // 持久化配置
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'chat',
        storage: sessionStorage,
        paths: [
          // TODO
          'chatTitle',
          'inputData',
          'messageList',
        ],
      },
    ],
  },
})
