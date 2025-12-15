<template>
  <div class="history-list-wrapper">
    <div class="history-list-detail" v-if="history.isExpanded">
      <div class="history-list-loading" v-if="history.isLoading">加载中...</div>

      <div class="history-list" v-else>
        <div
          v-for="(item, index) in history.historyList"
          :key="item.id"
          :id="`session-${item.id}`"
          :data-session-id="item.id"
          ref="historyItemRef"
          class="history-item"
          :class="{ 'history-item-selected': history.selectedSessionId === item.id }"
          @click.stop="handleHistoryItem(item)"
        >
          <!-- 历史对话标题和三点菜单容器 -->
          <div class="history-item-content">
            <!-- 历史对话标题 -->
            <span
              class="history-title"
              :class="{ pinned: item.isPinned }"
              @click.stop="loadHistorySession(item)"
            >
              <span class="history-title-text" :data-full-title="item.sessionTitle">{{
                item.sessionTitle
              }}</span>
              <span v-if="item.isPinned" class="pin-icon">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="12"
                  height="12"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#333"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <rect x="4" y="5" width="16" height="1" rx="1" ry="1" stroke-width="1" />
                  <line x1="12" y1="13" x2="12" y2="24" />
                  <line x1="12" y1="12" x2="8" y2="19" />
                  <line x1="12" y1="12" x2="16" y2="19" />
                </svg>
              </span>
            </span>

            <!-- 三点菜单按钮 -->
            <button
              class="history-item-dots"
              @click.stop="historyItemMoreOperation(item.id)"
              :aria-label="`${item.sessionTitle}的更多操作`"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="12" r="1"></circle>
                <circle cx="19" cy="12" r="1"></circle>
                <circle cx="5" cy="12" r="1"></circle>
              </svg>
            </button>
          </div>

          <!-- 三点菜单弹出框 -->
          <div
            class="history-item-more"
            v-if="history.activeSessionMenuId === item.id"
            :class="{ active: history.activeSessionMenuId === item.id }"
            @click.stop
          >
            <div class="more-menu-item" @click.stop="pinSession(item.id, !item.isPinned)">
              {{ item.isPinned ? '取消置顶' : '置顶' }}
            </div>
            <div class="more-menu-item" @click.stop="collectSession(item.id)">
              {{ item.isCollected ? '取消收藏' : '收藏' }}
            </div>
            <div class="more-menu-item" @click.stop="renameSession(item.id, item.sessionTitle)">
              重命名
            </div>
            <div class="more-menu-item delete-item" @click.stop="deleteSession(item.id, index)">
              删除
            </div>
          </div>
        </div>

        <div
          class="history-list-empty"
          v-if="history.historyList.length === 0 && !history.isLoading"
        >
          暂无历史对话
        </div>
      </div>
    </div>

    <RenameBox />
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request.js'
import '@/assets/css/home/HistoryDetail.css'
import RenameBox from '@/components/RenameBox.vue'
import { checkLogin } from '@/utils/commonUtils.js'
import { useHistoryStore } from '@/store/history.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useChatStore } from '@/store/chat.js'
import { useFunctionStore } from '@/store/function.js'

const userProfile = useUserProfileStore()
const history = useHistoryStore()
const homeStatus = useHomeStatusStore()
const chat = useChatStore()
const func = useFunctionStore()

const props = defineProps({
  loadFunc: {
    type: Function,
    required: true,
  },
  collectionFunc: {
    type: Function,
    required: true,
  },
  newChatFunc: {
    type: Function,
    required: true,
  },
})

const SCROLL_BOTTOM_DELAY = 300
const isLoadingRequest = ref(false)
if (history.isLoading === undefined || history.isLoading) {
  history.isLoading = false
}

const handleHistoryItem = async (item) => {
  history.selectedSessionId = item.id
  homeStatus.currentMenu = 'history'
  await loadHistorySession(item)
}

// 获取历史对话中某个对话的详情
const loadHistorySession = async (item) => {
  // 如果是同一个对话且已加载过数据，直接更新选中状态并滚动到底部，不重复请求
  history.isSessionCollected = item.isCollected
  if (item.id === history.loadedSessionId && chat.messageList.length > 0) {
    history.selectedSessionId = item.id
    homeStatus.currentMenu = 'history'
    await nextTick()
    setTimeout(() => {
      nextTick(() => {
        func.scrollToBottom({ force: true })
        func.checkScrollBottomBtn()
      })
    }, SCROLL_BOTTOM_DELAY)
    return
  }
  // 如果正在加载中，阻止重复请求
  if (history.isLoadingSession) return
  // 准备加载新对话
  history.isLoadingSession = true
  history.selectedSessionId = item.id
  homeStatus.currentMenu = 'history'
  chat.messageList = []

  try {
    const response = await request('get', `/session/${item.id}`)
    const sessionData = response.data.chatSession
    const messageList = response.data.chatMessageList || []

    history.currentSessionId = item.id
    history.loadedSessionId = item.id
    chat.chatTitle = sessionData.sessionTitle
    chat.messageList = messageList.map((msg) => ({
      id: `${Date.now()}-${Math.random().toString(36).slice(2)}-${msg.type}`,
      thinking: msg.messageThinking || '',
      content: msg.messageContent || '',
      isUser: msg.messageType === 1,
      type: msg.type ? msg.type : 'text',
      thinkingType: '思考完成'
    }))
    chat.modelInfo.newSession = false
    chat.modelInfo.sessionId = sessionData.id
    chat.modelInfo.sessionTitle = sessionData.sessionTitle
    chat.modelInfo.aiModelId = sessionData.aiModelId
    chat.modelInfo.isDeleted = sessionData.isDeleted
    chat.modelInfo.isPinned = sessionData.isPinned
    chat.modelInfo.isCollected = sessionData.isCollected
    chat.modelInfo.createdAt = sessionData.createdAt
    chat.modelInfo.sendTime = sessionData.sendTime
    chat.modelInfo.lastMessageTime = sessionData.lastMessageTime
    history.isSessionCollected = sessionData.isCollected === 1
    await nextTick()
    setTimeout(() => {
      nextTick(() => {
        func.scrollToBottom({ force: true })
        func.checkScrollBottomBtn()
      })
    }, SCROLL_BOTTOM_DELAY)
  } catch (e) {
    chat.messageList = []
    history.loadedSessionId = null
  } finally {
    history.isLoadingSession = false
  }
}

const historyItemMoreOperation = async (itemId) => {
  // 获取当前对话项元素
  const itemEl = document.querySelector(`[data-session-id="${itemId}"]`)
  if (history.activeSessionMenuId === itemId) {
    // 关闭菜单时移除激活类
    itemEl?.classList.remove('history-item-more-active')
    history.activeSessionMenuId = null
    return
  }
  // 先移除其他项的激活类
  document.querySelectorAll('.history-item-more-active').forEach((el) => {
    el.classList.remove('history-item-more-active')
  })
  // 打开菜单时添加激活类
  itemEl?.classList.add('history-item-more-active')
  history.activeSessionMenuId = itemId

  await nextTick(() => {
    setTimeout(() => {
      calculateSessionMenuPosition(itemId)
    }, 0)
  })
}

// 计算对话菜单位置
const calculateSessionMenuPosition = (itemId) => {
  const itemEl = document.querySelector(`[data-session-id="${itemId}"]`)
  if (!itemEl) return
  const menuEl = itemEl.querySelector('.history-item-more')
  if (!menuEl) return

  // 获取元素和视口信息
  const itemRect = itemEl.getBoundingClientRect()
  const viewportHeight = window.innerHeight // 视口高度
  // 强制刷新弹窗尺寸（避免动态内容导致高度计算错误）
  menuEl.style.visibility = 'hidden' // 临时隐藏避免闪烁
  const menuHeight = menuEl.offsetHeight // 弹窗自身高度
  menuEl.style.visibility = '' // 恢复显示

  const baseOffset = 5 // 基础偏移量（控制与元素的间距）
  const upwardAdjust = 3 // 向上弹窗时的额外向下偏移

  // 计算向下弹窗时的底部位置
  const bottomIfDown = itemRect.top + baseOffset + menuHeight
  if (bottomIfDown > viewportHeight) {
    // 向上弹窗：底部对齐元素底部，再向下偏移 upwardAdjust
    // 公式：元素底部 - 弹窗高度 + 向下偏移量（让弹窗整体下移）
    menuEl.style.top = `${itemRect.bottom - menuHeight + upwardAdjust}px`
    // 微调：减少向上的偏移，配合整体下移效果
    menuEl.style.transform = 'translateX(4px) translateY(-2px)'
  } else {
    // 向下弹窗
    menuEl.style.top = `${itemRect.top + baseOffset}px`
    menuEl.style.transform = 'translateX(4px) translateY(0)'
  }

  // 水平方向始终与元素右侧对齐
  menuEl.style.left = `${itemRect.right}px`
  menuEl.style.bottom = 'auto'
}

// 三点功能菜单操作方法
const pinSession = async (id, pinned) => {
  const index = history.historyList.findIndex((item) => item.id === id)
  if (index !== -1) {
    history.historyList[index].isPinned = pinned ? 1 : 0
    const response = await request('patch', `/session/${id}/pinned`, null, {
      params: { isPinned: pinned ? 1 : 0 },
    })
    if (response.code === 200) {
      if (pinned) {
        const [pinnedItem] = history.historyList.splice(index, 1)
        history.historyList.unshift(pinnedItem)
      }
      ElMessage.success(pinned ? '已置顶' : '已取消置顶')
    } else {
      history.historyList[index].isPinned = pinned ? 0 : 1
      ElMessage.error(response.msg)
    }
  }

  history.activeSessionMenuId = null
}

// 三点功能菜单收藏相关
const collectSession = async (sessionId) => {
  if (!sessionId) {
    ElMessage.warning('无法获取对话信息')
    return
  }
  const targetSession = history.historyList.find((item) => item.id === sessionId)
  if (!targetSession) {
    return
  }
  const currentState = targetSession.isCollected
  const newState = !currentState
  try {
    // 切换状态（先乐观更新UI）
    if (newState) {
      await request('put', `/collection/${sessionId}/add`)
    } else {
      await request('delete', `/collection/session/${sessionId}/delete`)
    }
    if (sessionId === history.selectedSessionId) {
      history.isSessionCollected = newState
    }
    const sessionIndex = history.historyList.findIndex((item) => item.id === sessionId)
    if (sessionIndex !== -1) {
      history.historyList[sessionIndex].isCollected = newState
    }
    ElMessage.success(newState ? '收藏成功' : '取消收藏成功')
    history.activeSessionMenuId = null
    await props.collectionFunc()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
    const sessionIndex = history.historyList.findIndex((item) => item.id === sessionId)
    if (sessionIndex !== -1) {
      history.historyList[sessionIndex].isCollected = currentState
    }
  }
}

const renameSession = (sessionId, currentTitle) => {
  // 关闭三点菜单
  history.activeSessionMenuId = null
  homeStatus.renamingSessionId = sessionId
  // 填充当前标题
  chat.newChatTitle = currentTitle
  // 显示弹窗
  homeStatus.isRenameDialogShow = true
}

// 三点功能菜单删除相关
const deleteSession = async (id, index) => {
  const confirmDelete = await ElMessageBox.confirm('确定要删除该历史对话吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })

  if (confirmDelete) {
    try {
      const response = await request('delete', `/session/${id}/delete`)
      if (response.code === 200) {
        history.historyList.splice(index, 1)
        history.deleteHistoryId(id)
        ElMessage.success('对话已删除')
        // 若删除的是当前选中的对话，清除存储
        if (id === history.selectedSessionId) {
          history.clearCurrent()
          homeStatus.setCurrentMenu('new')
          // 直接使用新对话窗口
          await props.newChatFunc()
        }
      } else {
        ElMessage.error(response.msg)
      }
    } catch (e) {
      ElMessage.error('删除对话异常，请重试')
    }
  }
}

// 检测文字是否溢出并添加标识类
const checkTextOverflow = () => {
  // 延迟100ms，确保历史列表完全渲染
  setTimeout(() => {
    const titleElements = document.querySelectorAll('.history-title-text')
    titleElements.forEach((el) => {
      // 强制重绘，确保尺寸计算准确
      el.offsetWidth
      // 判断溢出
      const isOverflowed = el.scrollWidth > el.clientWidth
      el.classList.toggle('text-overflowed', isOverflowed)

      // 重新计算定位（基于元素当前位置）
      const rect = el.getBoundingClientRect()
      // 向上偏移10px，避免遮挡文字
      el.style.setProperty('--el-top', `${rect.top - 4}px`)
      el.style.setProperty('--el-left', `${rect.left - 4}px`)
    })
  }, 100)
}

// 加载数据
const loadHistoryData = async () => {
  if (history.hasLoadedHistory) return
  if (history.isLoading || isLoadingRequest.value || !checkLogin(userProfile)) {
    return
  }
  isLoadingRequest.value = true
  try {
    await props.loadFunc()
    checkTextOverflow()
    history.hasLoadedHistory = true
    history.initHistoryIds()
  } catch (e) {
    history.clearHistory()
  } finally {
    isLoadingRequest.value = false
  }
}

// 控制历史对话仅在“登录状态变化+展开状态”时加载
watch(
  () => [checkLogin(userProfile), history.isExpanded],
  async ([isLogin, isExpanded]) => {
    if (isLogin && isExpanded &&
      !history.hasLoadedHistory && history.historyList.length === 0) {
      // 延迟加载避免初始化多次触发
      setTimeout(async () => {
        await loadHistoryData()
      }, 300)
    } else if (!isLogin) {
      history.clearHistory()
      isLoadingRequest.value = false
    }
  },
  { immediate: true, flush: 'post' }
)

onMounted(() => {
  if (checkLogin(userProfile) && history.isExpanded &&
    !history.hasLoadedHistory && history.historyList.length === 0) {
    setTimeout(loadHistoryData, 300)
  }
  window.addEventListener('resize', checkTextOverflow)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkTextOverflow)
  isLoadingRequest.value = false
})
</script>

<style scoped></style>
