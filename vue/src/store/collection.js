import { defineStore } from 'pinia'

// 定义信息存储
export const useCollectionStore = defineStore('collection', {
  // 状态（存储信息）
  state: () => ({
    isDialogShow: false,
    isLoading: false,
    pageInfo: {
      list: [], // 当前页数据列表
      total: 0, // 总记录数
      pageNum: 1, // 当前页码
      pageSize: 5, // 每页条数
      pages: 0, // 总页数
      hasPreviousPage: false, // 是否有上一页
      hasNextPage: false, // 是否有下一页
    }
  }),

  actions: {
    clearCollection() {
      this.isDialogShow = false
      this.isLoading = false
      this.clearPageInfo()
    },

    clearPageInfo() {
      this.pageInfo.list = []
      this.pageInfo.total = 0
      this.pageInfo.pageNum = 1
      this.pageInfo.pageSize = 5
      this.pageInfo.pages = 0
      this.pageInfo.hasPreviousPage = false
      this.pageInfo.hasNextPage = false
    }
  },

  // 持久化配置
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'collection',
        storage: localStorage,
      },
    ],
  },
})
