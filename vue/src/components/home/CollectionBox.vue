<template>
  <!-- 收藏弹窗 -->
  <div
    class="dialog-cover coll-dialog-wrapper"
    v-if="collection.isDialogShow"
    @click="collection.isDialogShow = false"
  >
    <div class="coll-dialog" @click.stop>
      <!-- 弹窗头部 -->
      <div class="coll-dialog-header">
        <h3>已收藏对话</h3>
        <button class="coll-dialog-close" @click="collection.isDialogShow = false">×</button>
      </div>
      <!-- 弹窗表格头部 -->
      <div class="coll-table-header">
        <div class="header-row">
          <div class="header-cell title-cell">对话名称</div>
          <div class="header-cell note-cell">收藏备注</div>
          <div class="header-cell time-cell">收藏时间</div>
          <div class="header-cell action-cell">操作</div>
        </div>
      </div>
      <!-- 弹窗表格内容 -->
      <div class="coll-dialog-content">
        <!-- 加载状态 -->
        <div class="loading-coll" v-if="collection.isLoading">加载中...</div>
        <div class="coll-table-body" v-else>
          <!-- 渲染 PageInfo 中的当前页数据 -->
          <div class="table-row" v-for="item in collection.pageInfo.list" :key="item.id">
            <div class="table-cell title-cell">{{ item.sessionTitle }}</div>
            <div class="table-cell note-cell">{{ item.collectionNote }}</div>
            <div class="table-cell time-cell">{{ formatTimeSecond(item.collectedAt) }}</div>
            <div class="table-cell action-cell">
              <button class="operation-btn view-btn" @click="handleViewCollection(item.id)">
                查看
              </button>
              <button class="operation-btn delete-btn" @click="handleDeleteCollection(item.id)">
                删除
              </button>
            </div>
          </div>
          <div
            class="empty-tip"
            v-if="collection.pageInfo.list.length === 0 && !collection.isLoading"
          >
            暂无收藏的对话
          </div>
        </div>
      </div>
      <!-- 分页控制器 -->
      <div class="coll-dialog-pagination">
        <CollectionPaging
          :visible="collection.pageInfo.total > 0 && !collection.isLoading"
          :disabled="collection.isLoading"
          @page-size-change="handlePageSizeChange"
          @page-num-change="handlePageNumChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/assets/css/Global.css'
import '@/assets/css/home/CollectionBox.css'
import CollectionPaging from '@/components/CollectionPaging.vue'
import { formatTimeSecond } from '@/utils/dateUtils.js'
import request from '@/utils/request.js'
import { useCollectionStore } from '@/store/collection.js'
import { useChatStore } from '@/store/chat.js'
import { useFunctionStore } from '@/store/function.js'

const collection = useCollectionStore()
const chat = useChatStore()
const func = useFunctionStore()

const props = defineProps({
  loadFunc: {
    type: Function,
    required: true,
  },
})

const scrollToSessionItem = inject('scrollToSessionItem')

const SCROLL_DELAY = 150

const handleViewCollection = async (id) => {
  try {
    const response = await request('get', `/collection/${id}`)
    const sessionData = response.data.sessionData
    const chatSession = sessionData.chatSession
    const chatMessages = sessionData.chatMessageList
    const messageList = []
    for (let i = 0; i < chatMessages.length; i++) {
      const chatMessage = chatMessages[i]
      const messageData = {
        id: chatMessage.id || Date.now() + Math.random().toString(36).slice(2, 8),
        content: chatMessage.messageContent,
        isUser: chatMessage.messageType === 1,
      }
      messageList.push(messageData)
    }

    chat.chatTitle = chatSession.sessionTitle
    chat.messageList = messageList
    collection.isDialogShow = false
    collection.pageInfo.pageNum = 1

    await nextTick()
    setTimeout(async () => {
      await scrollToSessionItem(chatSession.id)
      await nextTick(func.scrollToBottom())
    }, SCROLL_DELAY)
  } catch (error) {
    console.log(error)
  }
}

const handleDeleteCollection = async (id) => {
  await ElMessageBox.confirm('确定要删除该收藏吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })

  try {
    const response = await request('delete', `/collection/${id}/delete`)
    if (response.code === 200) {
      const currentListLength = collection.pageInfo.list.length
      const isLastPage = collection.pageInfo.pageNum === collection.pageInfo.pages
      if (currentListLength === 1 && isLastPage && collection.pageInfo.pageNum > 1) {
        collection.pageInfo.pageNum--
      }
      ElMessage.success('收藏已删除')
      await props.loadFunc()
    } else {
      ElMessage.success('收藏删除失败')
    }
  } catch (error) {
    console.log(error)
  }
}

// 分页处理函数
const handlePageChange = async (newPageNum, newPageSize) => {
  collection.pageInfo.pageNum = newPageNum
  if (newPageSize) {
    collection.pageInfo.pageSize = newPageSize
  }
  await props.loadFunc()
}

// 改变每页显示条数
const handlePageSizeChange = (newSize) => {
  handlePageChange(1, newSize)
}

// 切换每页条数时，重置到第一页
const handlePageNumChange = (newNum) => {
  handlePageChange(newNum)
}
</script>

<style scoped></style>
