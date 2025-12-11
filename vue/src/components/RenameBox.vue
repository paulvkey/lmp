<template>
  <teleport to="body">
    <!-- 对话名称修改弹窗 -->
    <div
      class="dialog-cover rename-dialog-wrapper"
      v-if="homeStatus.isRenameDialogShow"
      @click="closeDialog"
    >
      <div class="rename-dialog" @click.stop>
        <!-- 弹窗头部 -->
        <div class="rename-dialog-header">
          <h3>编辑对话名称</h3>
          <button class="rename-dialog-close" @click="closeDialog">×</button>
        </div>
        <!-- 弹窗内容 -->
        <div class="rename-dialog-content">
          <input
            v-model="chat.newChatTitle"
            class="title-dialog-input"
            :maxlength="MAX_TITLE_LENGTH"
            @keydown.enter="confirmEditTitle"
            @input="handleTitleDialogInput"
            ref="renameInputRef"
            placeholder="请输入对话名称"
          />
          <div class="rename-dialog-hint" :class="getHintClass(chat.newChatTitle)">
            提示：名称长度不超过{{ MAX_TITLE_LENGTH }}个字符（已输入{{
              chat.newChatTitle.length
            }}个）
          </div>
        </div>
        <!-- 弹窗底部 -->
        <div class="rename-dialog-footer">
          <button class="rename-dialog-btn cancel" @click="closeDialog">取消</button>
          <button class="rename-dialog-btn confirm" @click="confirmEditTitle">确认</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import '@/assets/css/Global.css'
import '@/assets/css/RenameBox.css'
import request from '@/utils/request.js'
import { checkLogin } from '@/utils/commonUtils.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { useChatStore } from '@/store/chat.js'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useHistoryStore } from '@/store/history.js'

const userProfile = useUserProfileStore()
const homeStatus = useHomeStatusStore()
const chat = useChatStore()
const history = useHistoryStore()

const MAX_TITLE_LENGTH = 30
const renameInputRef = ref(null)

// 输入长度限制函数
const useInputLengthLimit = (maxLength) => {
  // 处理输入长度限制
  const handleInput = (value) => {
    if (value.length > maxLength) {
      chat.newChatTitle = value.slice(0, maxLength)
      // 仅在首次达到最大长度时提示
      if (chat.newChatTitle.length === maxLength) {
        ElMessage.warning(`输入已达到最大长度（${maxLength}字）`)
      }
    }
  }
  // 获取提示文本的样式类名
  const getHintClass = (value) => {
    if (value.length === maxLength) return 'error'
    if (value.length > maxLength * 0.8) return 'warning' // 80% 时警告
    return ''
  }

  return { handleInput, getHintClass }
}

// 初始化标题编辑的限制逻辑
const { handleInput: handleTitleInputBase, getHintClass } = useInputLengthLimit(MAX_TITLE_LENGTH)

const handleTitleDialogInput = () => {
  handleTitleInputBase(chat.newChatTitle)
}

watch(
  () => homeStatus.isRenameDialogShow,
  (isShow) => {
    if (isShow) {
      nextTick(() => {
        if (renameInputRef.value) {
          renameInputRef.value.focus()
          renameInputRef.value.select()
        }
      })
    }
  },
  { immediate: true },
)

// 修改确认编辑标题方法，使其能处理对话重命名
const confirmEditTitle = async () => {
  let newChatTitle = chat.newChatTitle.trim()
  if (!newChatTitle) {
    ElMessage.warning('名称不能为空')
    return
  }

  if (newChatTitle.length > MAX_TITLE_LENGTH) {
    newChatTitle = newChatTitle.slice(0, MAX_TITLE_LENGTH)
    chat.chatTitle = newChatTitle
    ElMessage.warning(`名称长度已截断为${MAX_TITLE_LENGTH}个字符`)
  }

  try {
    if (homeStatus.renamingSessionId) {
      await rightChatRename(newChatTitle)
    } else {
      await leftChatRename(newChatTitle)
    }
  } catch (e) {
    ElMessage.error('修改对话名失败，请重试')
    if (homeStatus.renamingSessionId) {
      const sessionIndex = history.historyList.findIndex(
        (item) => item.id === homeStatus.renamingSessionId,
      )
      if (sessionIndex !== -1) {
        history.historyList[sessionIndex].sessionTitle = chat.chatTitle
      }
    }
  } finally {
    closeDialog()
  }
}

const rightChatRename = async (newChatTitle) => {
  // 处理对话重命名
  const index = history.historyList.findIndex(
    (item) => item.id === homeStatus.renamingSessionId,
  )
  if (index !== -1) {
    // 先更新本地显示
    const originalTitle = history.historyList[index].sessionTitle
    history.historyList[index].sessionTitle = newChatTitle
    const response = await request(
      'patch',
      `/session/${homeStatus.renamingSessionId}/rename`,
      null,
      {
        params: {
          sessionTitle: newChatTitle,
        },
      },
    )

    if (response.code === 200) {
      ElMessage.success('修改对话名成功')
      // 如果是当前选中的对话，同时更新聊天标题
      if (homeStatus.renamingSessionId === history.selectedSessionId) {
        chat.chatTitle = newChatTitle
      }
    } else {
      // 更新失败，恢复原始标题
      history.historyList[index].sessionTitle = originalTitle
      ElMessage.error('修改对话名失败，请重试')
    }
  }
}

const leftChatRename = async (newChatTitle) => {
// 处理当前对话标题修改
  if (checkLogin(userProfile)) {
    const response = await request(
      'patch',
      `/session/${chat.modelInfo.sessionId}/rename`,
      null,
      {
        params: {
          sessionTitle: newChatTitle,
        },
      },
    )
    if (response.code === 200) {
      if (newChatTitle) chat.newChatTitle = newChatTitle
      const currentSessionId = chat.modelInfo.sessionId
      if (currentSessionId) {
        // 找到左侧列表中对应的对话项
        const sessionIndex = history.historyList.findIndex(
          (item) => item.id === currentSessionId,
        )
        if (sessionIndex !== -1) {
          history.historyList[sessionIndex].sessionTitle = newChatTitle
        }
      }
      ElMessage.success('修改对话名成功')
    } else {
      ElMessage.error('修改对话名失败，请重试')
    }
  } else {
    if (newChatTitle) chat.chatTitle = newChatTitle
  }
}

// 关闭弹窗逻辑
const closeDialog = () => {
  homeStatus.isRenameDialogShow = false
  homeStatus.renamingSessionId = null
  history.activeSessionMenuId = null
  document.querySelectorAll('.history-item-more-active').forEach((el) => {
    el.classList.remove('history-item-more-active')
  })
}
</script>

<style scoped></style>
