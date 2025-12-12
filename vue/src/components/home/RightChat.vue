<template>
  <div class="right-chat-info">
    <!-- 顶部横栏 -->
    <div class="right-top-bar">
      <div class="top-bar-content">
        <div class="chat-title-wrapper" @click="renameTitle">
          <span class="chat-title">{{ chat.chatTitle }}</span>
        </div>
        <button v-if="!checkLogin(userProfile)" class="top-login-btn" @click="goToLogin">
          登录账号
        </button>
      </div>
    </div>

    <!-- 对话区域 -->
    <div class="chat-wrapper">
      <!-- 对话内容展示区 -->
      <div class="chat-msg-wrapper" ref="chatMsgWrapperRef">
        <div
          v-for="msg in chat.messageList"
          :key="msg.id"
          :class="['chat-msg-item', msg.isUser ? 'user-msg' : 'system-msg']"
        >
          <div class="user-msg-wrapper" v-if="msg.isUser">
            <div class="chat-msg">
              <!-- 展示文件消息 -->
              <div v-if="msg.type === 'file'">
                <div class="msg-file-list">
                  <div
                    v-for="(file, idx) in JSON.parse(msg.content)"
                    :key="idx"
                    class="msg-file-item"
                  >
                    <!-- 图片预览 -->
                    <img
                      v-if="file.isImage && file.previewUrl"
                      :src="file.previewUrl"
                      :alt="file.name"
                      class="msg-file-preview"
                      @load="handleImageLoad"
                    />
                    <!-- 非图片文件图标 -->
                    <div v-else class="msg-file-icon">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="24"
                        height="24"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path
                          d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                        ></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                      </svg>
                    </div>
                    <div class="msg-file-info">
                      <div class="msg-file-name">{{ file.name }}</div>
                      <div class="msg-file-size">{{ formatFileSize(file.size) }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 展示文本消息 -->
              <div v-else v-html="escapeMsg(msg.content)" />
            </div>
          </div>
          <div class="system-msg-wrapper" v-else>
            <!-- 思考过程 折叠/展开按钮 -->
            <button
              v-if="msg.thinking"
              class="toggle-thinking"
              @click="msg.showThinking = !msg.showThinking"
            >
              {{ msg.thinkingType }}
              <svg
                class="expand-icon"
                :style="{ transform: msg.showThinking ? 'rotate(0)' : 'rotate(-90deg)' }"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="M5,8 L12,16 L19,8"
                />
              </svg>
            </button>

            <!-- 思考内容区域 -->
            <div class="thinking-msg" :class="{ collapsed: !msg.showThinking }">
              <MarkdownRender :content="msg.thinking" />
            </div>

            <!-- 结果消息区域 -->
            <div class="chat-msg">
              <MarkdownRender :content="msg.content" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 对话输入区 -->
    <div class="chat-input-wrapper">
      <div class="chat-input-panel">
        <!-- 文件上传展示区 -->
        <div class="upload-files-wrapper" v-if="chat.uploadedFiles.length > 0">
          <div class="uploaded-files">
            <div
              class="uploaded-file-item"
              v-for="(file, index) in visibleFiles"
              :key="`${file.name}-${file.size}-${index}`"
              :title="file.name"
            >
              <span class="uploaded-file-icon">
                <img
                  v-if="file.isImage && file.previewUrl"
                  :src="file.previewUrl"
                  :alt="file.name"
                  class="uploaded-image-preview"
                />
                <svg
                  v-else
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path
                    d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                  ></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </span>
              <div class="uploaded-file-info">
                <span class="uploaded-file-name">{{ file.name }}</span>
                <!-- 上传进度显示 -->
                <div v-if="file.uploading || file.progress > 0" class="upload-file-progress">
                  <div class="upload-progress-bar" :style="{ width: `${file.progress}%` }"></div>
                </div>
              </div>
              <button
                class="remove-uploaded-file"
                @click.stop="removeUploadedFile(index)"
                :title="`移除 ${file.name}`"
                :disabled="file.uploading"
              >
                ×
              </button>
            </div>
            <button
              class="more-files-btn"
              v-if="hasMoreFiles"
              @click.stop="chat.showAllFiles = true"
            >
              +{{ moreFilesCount }} 更多文件
            </button>
          </div>
        </div>

        <!-- 内容输入区 -->
        <textarea
          id="chat-msg-input"
          class="chat-msg-input"
          name="chatMessage"
          rows="4"
          ref="chatMsgInputRef"
          v-model="chat.inputData"
          placeholder="发送消息（Alt+Enter换行，Enter发送）"
          @keydown="handleKeydown"
        ></textarea>

        <!-- 按钮功能区 -->
        <div class="chat-buttons">
          <div class="chat-left-buttons">
            <button
              class="deep-thinking"
              @click="chat.isDeepActive = !chat.isDeepActive"
              :class="{ active: chat.isDeepActive }"
            >
              深度思考
            </button>
            <button
              class="network-search"
              @click="chat.isNetworkActive = !chat.isNetworkActive"
              :class="{ active: chat.isNetworkActive }"
            >
              联网搜索
            </button>

            <!-- 上传文件按钮 -->
            <button class="upload-file" @click="handleFileUploadClick">上传文件</button>

            <!-- 隐藏的文件上传输入 -->
            <input
              type="file"
              ref="chatInputFileRef"
              class="file-input"
              @change="handleFileSelected"
              style="display: none"
              multiple
            />
          </div>

          <!-- 发送按钮 -->
          <button
            class="send-button"
            @click="handleSendClick"
            :class="{ 'send-active': chat.inputData.trim() || chat.uploadedFiles.length > 0 }"
            :disabled="requestLock.value"
          >
            <template v-if="chat.isSending">
              <!-- 加载动画 -->
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <!-- 外圈圆圈 -->
                <circle cx="12" cy="12" r="10" stroke="currentColor" />
                <!-- 中间小方块 -->
                <rect x="10" y="10" width="4" height="4" fill="currentColor" />
              </svg>
            </template>
            <template v-else>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="currentColor"
                stroke="none"
                style="transform: translate(-5%, 5%)"
              >
                <path d="M22 2L11 13M22 2L15 22L11 13L2 9L22 2Z" />
              </svg>
            </template>
          </button>
        </div>
      </div>
    </div>

    <!-- 上传文件 -->
    <UploadFilesBox />

    <!-- 滚动底部按钮 -->
    <div
      class="scroll-bottom"
      @click="scrollToBottom"
      v-show="chat.showScrollBtn"
      :class="{
        active: chat.showScrollBtn,
        sending: chat.isSending,
      }"
    >
      <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="#fff">
        <path
          d="M30,40 L50,60 L70,40"
          stroke="black"
          stroke-width="4"
          fill="none"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </div>
  </div>

  <RenameBox />
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, toRaw, watch } from 'vue'
import { ElMessage } from 'element-plus'
import 'katex/dist/katex.min.css'
import '@/assets/css/home/RightChat.css'
import router from '@/router/index.js'
import request from '@/utils/request.js'
import { checkLogin, escapeMsg } from '@/utils/commonUtils.js'
import { formatFileSize, getImageDimensions } from '@/utils/fileUtils.js'
import RenameBox from '@/components/RenameBox.vue'
import UploadFilesBox from '@/components/home/UploadFilesBox.vue'
import MarkdownRender from 'markstream-vue'
import 'markstream-vue/index.css'
import '@/assets/css/Global.css'
import { useChatStore } from '@/store/chat.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { useFunctionStore } from '@/store/function.js'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useHistoryStore } from '@/store/history.js'

const userProfile = useUserProfileStore()
const chat = useChatStore()
const homeStatus = useHomeStatusStore()
const history = useHistoryStore()
const func = useFunctionStore()

const STREAM_SCROLL_DELAY = 100
const SCROLL_DEBOUNCE_DELAY = 100
const SCROLLBAR_SHOW_DURATION = 500
const MAX_FILE_SHOW = 5

const scrollDebounceTimer = ref(null)
const scrollbarShowTimer = ref(null)
const chatMsgWrapperRef = ref(null)
const chatMsgInputRef = ref(null)
const chatInputFileRef = ref(null)
// AbortController用于取消fetch请求
const streamAbortCtrl = ref(null)
const streamMsgIdRef = ref('')
// 新对话/发送请求锁
const requestLock = ref(false)
// 全局AbortController：用于取消重复的异步请求
const globalAbortCtrl = ref(new AbortController())

const renameTitle = () => {
  homeStatus.isRenameDialogShow = true
  if (!chat.modelInfo.newSession) {
    homeStatus.renamingSessionId = chat.modelInfo.sessionId
  }
  chat.newChatTitle = chat.chatTitle
}

const chatMsgScrollTop = () => {
  chatMsgWrapperRef.value.scrollTop = 0
}

const chatMsgInputFocus = () => {
  chatMsgInputRef.value?.focus()
}

// 滚动到底部
const scrollToBottom = (options = {}) => {
  const { force = false } = options
  // 如果不是强制滚动，且用户手动滚动过，则不自动滚动
  if (!force && chat.isUserScrolled && !chat.showScrollBtn) return

  chat.showScrollBtn = false
  if (chat.isScrolling) return
  chat.isScrolling = true
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (!chatMsgWrapper) {
    chat.isScrolling = false
    return
  }

  try {
    // 设置scrollTop
    chatMsgWrapper.scrollTop = chatMsgWrapper.scrollHeight
    // 最终校准和滚动
    setTimeout(() => {
      // 强制设置到最大滚动高度
      chatMsgWrapper.scrollTop = chatMsgWrapper.scrollHeight
      chat.isScrolling = false
      chat.isUserScrolled = false
      checkScrollBottomBtn()

      // 如果仍有微小滚动余量，强制修正
      const { scrollTop, scrollHeight, clientHeight } = chatMsgWrapper
      const distanceToBottom = scrollHeight - scrollTop - clientHeight
      if (distanceToBottom > 0) {
        chatMsgWrapper.scrollTop += distanceToBottom
      }
    }, STREAM_SCROLL_DELAY)
  } catch (e) {
    console.warn('滚动异常：' + e)
    chatMsgWrapper.scrollTop = chatMsgWrapper.scrollHeight
    chat.isScrolling = false
  }
}

// 滚动检测逻辑，防抖和手动滚动判断
const checkScrollBottomBtn = () => {
  if (scrollDebounceTimer.value) clearTimeout(scrollDebounceTimer.value)
  if (scrollbarShowTimer.value) clearTimeout(scrollbarShowTimer.value)

  // 滚动时立即显示滚动条
  showScrollbar()
  // 滚动停止后，延迟隐藏滚动条
  scrollbarShowTimer.value = setTimeout(() => {
    hideScrollbar()
  }, SCROLLBAR_SHOW_DURATION)

  // 滚动按钮检测
  scrollDebounceTimer.value = setTimeout(() => {
    const chatMsgWrapper = chatMsgWrapperRef.value
    if (!chatMsgWrapper) return

    const { scrollTop, scrollHeight, clientHeight } = chatMsgWrapper
    const distanceToBottom = scrollHeight - scrollTop - clientHeight
    chat.isUserScrolled = scrollHeight > clientHeight && distanceToBottom > 5
    chat.showScrollBtn = chat.isUserScrolled
  }, SCROLL_DEBOUNCE_DELAY)
}

// 显示滚动条
const showScrollbar = () => {
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (!chatMsgWrapper) return

  // 直接修改样式（优先级最高）
  chatMsgWrapper.style.scrollbarColor = '#ccc transparent'
  chatMsgWrapper.style.setProperty('--scrollbar-thumb-bg', '#ccc')
  // 强制更新滚动条样式
  chatMsgWrapper.classList.add('scrollbar-visible')
  setTimeout(() => {
    chatMsgWrapper.classList.remove('scrollbar-visible')
  }, 0)
}

// 隐藏滚动条
const hideScrollbar = () => {
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (!chatMsgWrapper) return

  chatMsgWrapper.style.scrollbarColor = 'transparent transparent'
  chatMsgWrapper.style.setProperty('--scrollbar-thumb-bg', 'transparent')
}

// 文件上传相关方法
const handleFileUploadClick = () => {
  // 触发文件选择对话框
  chatInputFileRef.value.click()
}

// 多文件上传（并发控制）
const handleFileSelected = async (e) => {
  const files = e.target.files
  if (files.length > 0) {
    // 先添加到界面显示，再进行上传
    const newFiles = []
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const isDuplicate = chat.uploadedFiles.some(
        (existingFile) =>
          existingFile.name === file.name &&
          existingFile.size === file.size &&
          existingFile.type === file.type,
      )
      if (isDuplicate) {
        continue
      }
      const { isImage, width, height, preview } = await getImageDimensions(file)
      // 获取文件属性信息
      const fileInfo = {
        name: file.name,
        size: file.size,
        type: file.type,
        isImage: isImage,
        width: width,
        height: height,
        lastModified: file.lastModified,
        lastModifiedDate: file.lastModifiedDate,
        uploading: false,
        progress: 0,
        uploaded: false,
        error: false,
        errorMessage: '',
        previewUrl: preview,
      }
      newFiles.push(fileInfo)
    }

    // 添加到上传列表
    const startIndex = chat.uploadedFiles.length
    chat.uploadedFiles.push(...newFiles)

    // 执行并发上传
    try {
      // 准备多文件上传的FormData
      const formData = new FormData()
      // 收集文件元数据列表
      const fileMetaList = newFiles.map((file) => ({
        fileName: file.name,
        fileSize: file.size,
        userId: userProfile.userId || 0,
        sessionId: chat.modelInfo.sessionId || 0,
        isImage: file.isImage ? 1 : 0,
        imageWidth: file.width,
        imageHeight: file.height,
      }))
      // 添加文件列表和元数据列表
      formData.append('fileListJson', JSON.stringify(fileMetaList))
      // 添加所有文件
      for (let i = 0; i < files.length; i++) {
        formData.append('multipartFileList', files[i])
      }

      // 执行多文件上传
      const response = await request('post', '/files/upload', formData, {
        headers: { 'Content-Type': undefined },
        onUploadProgress: (progressEvent) => {
          // 这里只能获取整体进度，实际项目中可能需要后端支持单个文件进度
          const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100)
          // 更新所有上传中文件的进度
          newFiles.forEach((_, i) => {
            const index = startIndex + i
            if (chat.uploadedFiles[index]) {
              // 只有未完成的文件才更新进度
              if (!chat.uploadedFiles[index].uploaded && !chat.uploadedFiles[index].error) {
                chat.uploadedFiles[index].progress = percent
                chat.uploadedFiles[index].uploading = percent < 100
              }
            }
          })
        },
      })

      if (response.code === 200) {
        // 更新每个文件的上传结果
        const uploadedResults = response.data
        for (let i = 0; i < uploadedResults.length; i++) {
          const index = startIndex + i
          if (chat.uploadedFiles[index]) {
            chat.uploadedFiles[index] = {
              ...chat.uploadedFiles[index],
              id: uploadedResults[i].id,
              url: uploadedResults[i].filePath,
              uploading: false,
              progress: 100,
              uploaded: true,
              error: false,
            }
          }
        }

        // 收集成功上传的文件ID
        const fileIds = uploadedResults.map((file) => file.id).join(',')
        // 更新模型中的文件ID列表
        if (fileIds) {
          chat.modelInfo.fileIds = chat.modelInfo.fileIds
            ? `${chat.modelInfo.fileIds},${fileIds}`
            : fileIds
        }
      } else {
        ElMessage.error('文件上传失败')
      }
    } catch (error) {
      ElMessage.error(`${error.msg || '未知错误'}`)
      // 更新文件状态为错误
      for (let i = 0; i < newFiles.length; i++) {
        const index = startIndex + i
        if (chat.uploadedFiles[index]) {
          chat.uploadedFiles[index].uploading = false
          chat.uploadedFiles[index].error = true
          chat.uploadedFiles[index].errorMessage = error.msg || '上传失败'
        }
      }
    }
    // 允许重复选择同一文件
    e.target.value = ''
  }
}

// 可见文件
const visibleFiles = computed(() => {
  return chat.uploadedFiles.slice(0, MAX_FILE_SHOW)
})

// 是否有更多文件
const hasMoreFiles = computed(() => {
  return chat.uploadedFiles.length > MAX_FILE_SHOW
})

// 更多文件有多少
const moreFilesCount = computed(() => {
  return Math.max(chat.uploadedFiles.length - MAX_FILE_SHOW, 0)
})

// 移除特定文件
const removeUploadedFile = (index) => {
  if (index < 0 || index >= chat.uploadedFiles.length) {
    console.warn('无效的文件索引:', index)
    return
  }
  const file = chat.uploadedFiles[index]
  if (file.isImage && file.previewUrl) {
    URL.revokeObjectURL(file.previewUrl)
  }
  chat.uploadedFiles.splice(index, 1)
  if (chat.uploadedFiles.length <= MAX_FILE_SHOW) {
    chat.showAllFiles = false
  }
}

// 处理输入框键盘事件
const handleKeydown = (e) => {
  if (e.keyCode === 13) {
    if (e.altKey || e.metaKey) {
      const cursorPos = e.target.selectionStart
      chat.inputData = `${chat.inputData.slice(0, cursorPos)}\n${chat.inputData.slice(cursorPos)}`
      setTimeout(() => {
        e.target.selectionStart = e.target.selectionEnd = cursorPos + 1
      }, 0)
      e.preventDefault()
    } else {
      e.preventDefault()
      sendMessage()
    }
  }
}

// 触发新对话
const triggerNewChat = async () => {
  // 重复点击拦截
  if (requestLock.value) return
  requestLock.value = true
  try {
    // 取消上一次未完成的请求
    globalAbortCtrl.value.abort()
    globalAbortCtrl.value = new AbortController()
    chat.isSending = false
  } finally {
    // 释放锁（无论成败都执行）
    requestLock.value = false
  }
}

// 处理发送按钮点击
const handleSendClick = async () => {
  if (chat.isSending) {
    // 正在发送时，点击则终止当前请求
    await pauseSending()
  } else {
    // 未发送时，执行正常发送逻辑
    await sendMessage()
  }
}

// 发送消息
const sendMessage = async () => {
  if (requestLock.value || !prepareSendMessage()) return
  requestLock.value = true
  try {
    if (chat.modelInfo.newSession) {
      const response = await request('post', `/session/chat/new`, chat.modelInfo, {
        signal: globalAbortCtrl.value.signal,
      })
      if (response.code !== 200) {
        return
      }
      updateInfoByResponse(response.data)
    }
    await getAndParseChatData(globalAbortCtrl.value.signal)
    updateHistoryByResponse()
  } catch (e) {
    console.error('发送消息异常：' + e)
  } finally {
    chat.resetChat()
    homeStatus.isNewSession = false
    requestLock.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 发送消息前数据准备
const prepareSendMessage = () => {
  if (chat.isSending) return false
  const inputData = chat.inputData.trim()
  const hasUploadFiles = chat.uploadedFiles.length > 0
  if (!hasUploadFiles && inputData.length <= 0) return false

  chat.initModelInfo(userProfile)
  chat.modelInfo.messageList = []
  // 没有登陆将之前所有的对话内容都传递
  if (!checkLogin(userProfile)) {
    chat.messageList.forEach((msg) => {
      chat.modelInfo.messageList.push({
        thinking: msg.thinking,
        content: msg.content,
        type: msg.type,
        role: msg.isUser ? 1 : 2,
      })
    })
  }
  if (hasUploadFiles && !chat.filesUploaded) {
    const fileContent = chat.uploadedFiles.map((file) => ({
      name: file.name,
      size: file.size,
      type: file.type,
      previewUrl: file.previewUrl,
    }))
    const fileMessage = {
      id: Date.now() + '-file',
      content: JSON.stringify(fileContent),
      isUser: true,
      type: 'file',
    }
    chat.messageList.push(fileMessage)
    chat.modelInfo.messageList.push({
      thinking: '',
      content: fileMessage.content,
      type: fileMessage.type,
      role: fileMessage.isUser ? 1 : 2,
      fileIds: '',
    })
    chat.filesUploaded = true
  }

  const message = {
    id: Date.now() + '-text',
    content: inputData,
    isUser: true,
    type: 'text',
  }
  chat.messageList.push(message)
  chat.modelInfo.messageList.push({
    thinking: '',
    content: message.content,
    type: message.type,
    role: message.isUser ? 1 : 2,
    fileIds: chat.uploadedFiles.map((f) => f.id).join(',') || '',
  })
  chat.inputData = ''
  chat.isSending = true
  chat.modelInfo.newSession = homeStatus.isNewSession
  chat.modelInfo.messageType = 1
  chat.modelInfo.isDeepThink = chat.isDeepActive ? 1 : 0
  chat.modelInfo.isNetworkSearch = chat.isNetworkActive ? 1 : 0
  return true
}

// 新对话创建之后更新已有的信息
const updateInfoByResponse = (response) => {
  chat.modelInfo.newSession = false
  chat.modelInfo.sessionId = response.sessionId
  chat.modelInfo.sessionTitle = response.sessionTitle
  chat.modelInfo.aiModelId = response.aiModelId
  chat.modelInfo.isDeleted = response.isDeleted
  chat.modelInfo.isPinned = response.isPinned
  chat.modelInfo.isCollected = response.isCollected
  chat.modelInfo.createdAt = response.createdAt
  chat.modelInfo.sendTime = response.sendTime
  chat.modelInfo.lastMessageTime = response.lastMessageTime
  homeStatus.renamingSessionId = response.sessionId
}

// 获取并解析流式消息
const getAndParseChatData = async (abortSignal) => {
  // 取消已有流式请求（避免重复）
  if (streamAbortCtrl.value) {
    streamAbortCtrl.value.abort()
  }
  streamAbortCtrl.value = new AbortController()
  const combinedSignal = AbortSignal.any([abortSignal, streamAbortCtrl.value.signal])

  try {
    // 创建流式消息项（初始空内容+加载状态）
    streamMsgIdRef.value = Date.now() + '-stream'
    const streamMsg = {
      id: streamMsgIdRef.value,
      thinking: '',
      content: '',
      isUser: false,
      type: 'text',
      isStreaming: true,
      showThinking: true,
      thinkingType: '思考完成',
    }
    chat.messageList.push(streamMsg)
    await nextTick()
    scrollToBottom()

    // 使用fetch发起SSE流式请求
    const response = await fetch(`/session/chat`, {
      method: 'POST',
      signal: combinedSignal,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify(chat.modelInfo),
    })

    let errorMsg = '抱歉，请求异常，请重试'
    if (!response.ok) {
      await setMsgEndInfo(streamMsgIdRef.value, errorMsg)
      return
    }

    // 处理SSE流式响应
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      // 解码并拼接数据
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      // 遍历解析每个SSE事件
      for (const eventBlock of events) {
        if (!eventBlock || eventBlock.trim() === '') continue

        // 拆分event和data
        const lines = eventBlock.split('\n').filter((line) => line.trim() !== '')
        let eventName = ''
        let eventData = ''
        for (const line of lines) {
          if (line.startsWith('event:')) {
            // 提取事件名（仅关注chunk/finished/error）
            eventName = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            // 提取数据内容
            eventData = line.substring(5).trim()
          }
        }

        if (!eventData) continue
        try {
          const parsedData = JSON.parse(eventData)
          const msgIndex = getMsgIndex()
          if (eventName === 'chunk') {
            // 处理分块消息（chunk：实时接收流式内容）
            await updateChunkMsg(parsedData, msgIndex)
          } else if (eventName === 'finished') {
            // 处理结束事件（finished：完成事件单独返回格式）
            await updateFinishedMsg(parsedData, msgIndex)
            // 释放资源，结束循环
            reader.releaseLock()
            return
          } else if (eventName === 'error') {
            // 处理错误事件（error：超时/业务异常）
            errorMsg = parsedData.msg || errorMsg
            await setMsgEndInfo(streamMsgIdRef.value, errorMsg)
            reader.releaseLock()
          }
        } catch (e) {
          console.error('解析服务端消息异常：' + e)
        }
      }
    }

    // 流式接收意外结束时，标记消息完成
    await setMsgEndInfo()
  } catch (e) {
    await setMsgEndInfo()
  } finally {
    await nextTick()
    setTimeout(() => {
      scrollToBottom({ force: true })
      chat.isSending = false
      streamAbortCtrl.value = null
    }, STREAM_SCROLL_DELAY)
  }
}

// 获取当前会话消息的位置
const getMsgIndex = (newMsgId = -1) => {
  const list = chat.messageList
  let msgId = streamMsgIdRef.value
  if (newMsgId !== -1) {
    msgId = newMsgId
  }
  // 数组为空/目标id为空，直接返回-1
  if (!list.length || !msgId) return -1

  // 优先校验最后一项
  const lastIndex = list.length - 1
  if (list[lastIndex].id === msgId) {
    return lastIndex
  }
  // 异常场景（目标不是最后一项）→ 反向遍历（从倒数第二项开始）
  for (let i = lastIndex - 1; i >= 0; i--) {
    if (list[i].id === msgId) {
      return i
    }
  }
  return -1
}

// 更新流式消息以及DOM渲染
const updateChunkMsg = async (parsedData, msgIndex) => {
  if (
    !chat.messageList ||
    !chat.messageList.length ||
    msgIndex === -1 ||
    msgIndex >= chat.messageList.length
  ) {
    return
  }
  const targetMsg = chat.messageList[msgIndex]
  if (!targetMsg || !parsedData?.message) {
    return
  }

  const { thinking, content } = parsedData.message
  let isNeedUpdate = false
  const newMsg = { ...targetMsg }
  if (typeof thinking === 'string' && thinking) {
    newMsg.thinkingType = '思考中'
    newMsg.thinking = (targetMsg.thinking || '') + thinking
    isNeedUpdate = true
  }
  if (typeof content === 'string' && content) {
    if (!newMsg.thinkingType || newMsg.thinkingType === '思考中') {
      newMsg.thinkingType = '思考完成'
    }
    newMsg.showThinking = false
    newMsg.content = (targetMsg.content || '') + content
    isNeedUpdate = true
  }

  if (isNeedUpdate) {
    // 用$patch函数式修改，直接操作state，无需拷贝整个数组
    chat.$patch((state) => {
      state.messageList[msgIndex] = newMsg
    })
  }
  await nextTick()
  scrollToBottom()
}

// 更新最终的完成消息但不更新DOM渲染
const updateFinishedMsg = async (parsedData, msgIndex) => {
  if (
    !chat.messageList ||
    !chat.messageList.length ||
    msgIndex === -1 ||
    msgIndex >= chat.messageList.length
  ) {
    return
  }

  const rawChat = toRaw(chat)
  const targetMsg = rawChat.messageList[msgIndex]
  if (!targetMsg || !parsedData?.data || !parsedData.data.messageList) {
    return
  }
  const { thinking, content } = parsedData.data.messageList[0]
  if (typeof thinking === 'string' && thinking.trim() !== '') {
    targetMsg.thinking = thinking
  }
  if (typeof content === 'string' && content.trim() !== '') {
    targetMsg.content = content
  }
  await nextTick()
  scrollToBottom()
}

// 流式消息完成后设置相关的信息
const setMsgEndInfo = async (newMsgId = -1, errorMsg = '') => {
  const msgIndex = getMsgIndex(newMsgId)
  if (msgIndex !== -1) {
    if (errorMsg.trim() !== '') {
      chat.messageList[msgIndex].content = errorMsg
    }
    chat.messageList[msgIndex].showThinking = false
    chat.messageList[msgIndex].isStreaming = false
  }
  await nextTick()
  scrollToBottom()
}

// 消息接收到了之后在历史对话中添加当前会话
const updateHistoryByResponse = () => {
  if (checkLogin(userProfile)) {
    const sessionId = chat.modelInfo.sessionId
    const sessionTitle = chat.modelInfo.sessionTitle
    const updatedAt = chat.modelInfo.updatedAt

    // 检查历史列表中是否已存在该对话
    const existSessionIdx = history.historyList.findIndex((item) => item.id === sessionId)
    if (existSessionIdx !== -1) {
      // 已有对话：更新标题和时间（不新增）
      history.historyList[existSessionIdx] = {
        ...history.historyList[existSessionIdx],
        sessionTitle: sessionTitle,
        updatedAt: updatedAt,
      }
    } else {
      const newSessionData = {
        id: sessionId,
        userId: userProfile.userId,
        sessionTitle: sessionTitle,
        updatedAt: updatedAt,
        isPinned: chat.modelInfo.isPinned || 0,
        isCollected: chat.modelInfo.isCollected || 0,
      }

      // 插入到置顶对话之后、普通对话之前
      const firstNonPinnedIndex = history.historyList.findIndex((item) => item.isPinned === 0)
      const insertIndex =
        firstNonPinnedIndex !== -1 ? firstNonPinnedIndex : history.historyList.length
      history.historyList.splice(insertIndex, 0, newSessionData)
    }

    // 保存新对话ID到localStorage
    localStorage.setItem('currentSessionId', sessionId)
    // 高亮选中新对话
    history.selectedSessionId = sessionId
    history.isExpanded = true
  }
}

// 图片加载完成后重新滚动
const handleImageLoad = () => {
  setTimeout(() => {
    scrollToBottom({ force: true })
  }, 100)
}

// 取消发送
const pauseSending = async () => {
  // 终止流式请求
  if (streamAbortCtrl.value) {
    streamAbortCtrl.value.abort('取消发送')
    streamAbortCtrl.value = null
  }
  // 终止全局请求
  if (globalAbortCtrl.value) {
    globalAbortCtrl.value.abort('取消发送')
    globalAbortCtrl.value = new AbortController()
  }
  // 重置发送状态
  chat.isSending = false
  requestLock.value = false
  // 标记最后一条消息
  const lastMsgIndex = chat.messageList.length - 1
  if (lastMsgIndex >= 0) {
    const lastMsg = chat.messageList[lastMsgIndex]
    if (lastMsg.isStreaming) {
      chat.$patch((state) => {
        state.messageList[lastMsgIndex] = {
          ...lastMsg,
          isStreaming: false,
          content: '已取消发送',
        }
      })
    }
    // 更新后端数据(直接删除对应的消息)
    try {
      await request('patch', `/session/${chat.modelInfo.sessionId}/pause`, null, {})
    } catch (e) {
      console.error('取消发送，处理当前消息异常：' + e)
    }
  }
}

watch(
  () => homeStatus.currentMenu,
  async (newMenu) => {
    homeStatus.setCurrentMenu(newMenu)
    homeStatus.initIsNewSession()
    chat.inputData = ''
    await nextTick()
    chatMsgInputFocus()
  },
  { immediate: true, flush: 'sync' },
)

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  homeStatus.initIsNewSession()
  // 监听聊天区域的滚动事件，实时检测是否显示按钮
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (chatMsgWrapper) {
    chatMsgWrapper.addEventListener('scroll', checkScrollBottomBtn, { passive: true })
    // 监听resize事件，适配窗口大小变化导致的定位偏差(只有用户未手动滚动时，才自动校准)
    window.addEventListener('resize', () => {
      if (!chat.isUserScrolled) {
        scrollToBottom({ force: true })
      }
    })
  }
  // 监听消息列表变化，自动检测滚动位置
  const unwatchMessages = watch(
    () => chat.messageList.length,
    async () => {
      await nextTick()
      checkScrollBottomBtn()
      // 消息数变化时，强制校准滚动位置
      if (!chat.isUserScrolled) {
        scrollToBottom({ force: true })
      }
    },
  )
  // 监听窗口大小变化（比如缩放、移动端旋转），更新按钮状态
  window.addEventListener('resize', checkScrollBottomBtn)
  // 保存清理函数，用于卸载时移除监听
  func.setScrollBtnCleanup(() => {
    unwatchMessages()
    window.removeEventListener('resize', checkScrollBottomBtn)
    if (chatMsgWrapper) {
      chatMsgWrapper.removeEventListener('scroll', checkScrollBottomBtn)
    }
  })

  // 页面刷新后主动初始化滚动和按钮状态
  setTimeout(() => {
    nextTick(() => {
      scrollToBottom({ force: true })
      checkScrollBottomBtn()
    })
  }, STREAM_SCROLL_DELAY)
})

onUnmounted(() => {
  // 执行清理函数
  if (func.scrollBtnCleanup) {
    func.scrollBtnCleanup()
  }
  // 重置按钮状态
  chat.showScrollBtn = false
  chat.isScrolling = false
  globalAbortCtrl.value.abort()
  if (streamAbortCtrl.value) streamAbortCtrl.value.abort()
  requestLock.value = false
  // 清除滚动条定时器
  if (scrollbarShowTimer.value) clearTimeout(scrollbarShowTimer.value)
})

func.setChatMsgScrollTop(chatMsgScrollTop)
func.setChatMsgInputFocus(chatMsgInputFocus)
func.setScrollToBottom(scrollToBottom)
func.setCheckScrollBottomBtn(checkScrollBottomBtn)
func.setTriggerNewChat(triggerNewChat)
</script>

<style scoped></style>
