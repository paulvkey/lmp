<template>
  <div class="right-chat-info">
    <!-- 顶部横栏 -->
    <div class="right-top-bar">
      <div class="top-bar-content">
        <div class="chat-title-wrapper" @click="renameTitle">
          <span class="chat-title">{{ chat.chatTitle }}</span>
        </div>
        <button class="tuning-params" @click="tuningParams">调节参数</button>
        <!-- TODO 增加调节参数弹窗 -->
        <TuningParams />
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
              <div v-else v-html="formatUserMessage(msg.content)" />
            </div>
          </div>
          <div class="system-msg-wrapper" v-else>
            <div class="chat-msg">
              <MarkdownRender :content="msg.content"/>
            </div>
          </div>
        </div>
      </div>

      <!-- 对话输入区 -->
      <div class="chat-input-wrapper" ref="chatInputWrapperRef">
        <div class="chat-input-panel">
          <!-- 文件上传展示区 -->
          <div class="upload-files-wrapper" v-if="chat.uploadedFiles.length > 0">
            <div class="uploaded-files">
              <div
                class="uploaded-file-item"
                v-for="(file, index) in chat.visibleFiles"
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
              <button class="more-files-btn"></button>
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
            :disabled="isInputDisabled"
          ></textarea>

          <!-- 按钮功能区 -->
          <div class="chat-buttons">
            <div class="chat-left-buttons">
              <button
                class="deep-thinking"
                @click="chat.isDeepActive = !chat.isDeepActive"
                :class="{ active: chat.isDeepActive }"
                :disabled="isInputDisabled"
              >
                深度思考
              </button>
              <button
                class="network-search"
                @click="chat.isNetworkActive = !chat.isNetworkActive"
                :class="{ active: chat.isNetworkActive }"
                :disabled="isInputDisabled"
              >
                联网搜索
              </button>

              <!-- 上传文件按钮 -->
              <button
                class="upload-file"
                @click="handleFileUploadClick"
                :disabled="isInputDisabled"
              >
                上传文件
              </button>

              <!-- 隐藏的文件上传输入 -->
              <input
                type="file"
                ref="chatInputFileRef"
                class="file-input"
                @change="handleFileSelected"
                style="display: none"
                multiple
                :disabled="isInputDisabled"
              />
            </div>

            <!-- 发送按钮 -->
            <button
              class="send-button"
              @click="sendMessage"
              :class="{ 'send-active': chat.inputData.trim() || chat.uploadedFiles.length > 0 }"
              :disabled="chat.isSending || isInputDisabled"
            >
              <template v-if="isInputDisabled">
                <!-- 加载动画 -->
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <circle
                    cx="12"
                    cy="12"
                    r="10"
                    stroke-dasharray="100"
                    stroke-dashoffset="0"
                    transform="rotate(0 12 12)"
                  >
                    <animate
                      attributeName="stroke-dashoffset"
                      values="0;150"
                      dur="1.5s"
                      repeatCount="indefinite"
                    />
                    <animate
                      attributeName="transform"
                      type="rotate"
                      values="0 12 12;360 12 12"
                      dur="1.5s"
                      repeatCount="indefinite"
                    />
                  </circle>
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
      <UploadFilesBox />

      <!-- 滚动底部按钮 -->
      <div
        class="scroll-bottom"
        @click="scrollToBottom"
        v-show="chat.showScrollBtn"
        :class="{ active: chat.showScrollBtn }"
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
  </div>

  <RenameBox />
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import 'katex/dist/katex.min.css'
import '@/assets/css/home/RightChat.css'
import request from '@/utils/request.js'
import { checkLogin } from '@/utils/commonUtils.js'
import { formatFileSize, getImageDimensions } from '@/utils/fileUtils.js'
import RenameBox from '@/components/RenameBox.vue'
import TuningParams from '@/components/home/TuningParams.vue'
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

// 常量定义
const chatMsgWrapperRef = ref(null)
const chatInputWrapperRef = ref(null)
const chatMsgInputRef = ref(null)
const chatInputFileRef = ref(null)
// AbortController用于取消fetch请求
const streamControllerRef = ref(null)
const streamMsgIdRef = ref('')

// 新对话/发送请求锁
const requestLock = ref(false)
// 全局AbortController：用于取消重复的异步请求
const globalAbortCtrl = ref(new AbortController())

const isInputDisabled = computed(() => {
  // 禁用条件：非新对话菜单 || 正在发送 || 有请求锁
  return !(homeStatus.currentMenu === 'new') || requestLock.value
})

const renameTitle = () => {
  homeStatus.isRenameDialogShow = true
  chat.newChatTitle = chat.chatTitle
}

const tuningParams = () => {}

const chatMsgScrollTop = () => {
  chatMsgWrapperRef.value.scrollTop = 0
}

const chatMsgInputFocus = () => {
  chatMsgInputRef.value?.focus()
}

const formatUserMessage = (content) => {
  if (!content) return ''
  // 仅转义 HTML 特殊字符（一次即可）
  let escaped = content
    .replace(/&/g, '&amp;') // & → &amp;
    .replace(/</g, '&lt;') // < → &lt;
    .replace(/>/g, '&gt;') // > → &gt;
    .replace(/"/g, '&quot;') // " → &quot;
    .replace(/'/g, '&#039;') // ' → &#039;
  // 处理空白字符（空格、制表符、换行）
  escaped = escaped
    .replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;') // 制表符 → 4个空格
    .replace(/ /g, '&nbsp;') // 空格 → 非换行空格
    .replace(/\n/g, '<br>') // 换行 → <br>
  return escaped
}

const checkScrollBottomBtn = () => {
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (!chatMsgWrapper) return

  // 滚动容器的关键高度计算
  const { scrollTop, scrollHeight, clientHeight } = chatMsgWrapper
  // 阈值：距离底部超过10px时显示按钮（避免轻微滚动就触发）
  const isNotAtBottom = scrollTop + clientHeight < scrollHeight - 10

  // 更新显示状态（避免频繁更新，仅状态变化时赋值）
  if (chat.showScrollBtn !== isNotAtBottom) {
    chat.showScrollBtn = isNotAtBottom
  }
}

// 滚动到底部
const scrollToBottom = () => {
  chat.showScrollBtn = false
  if (chat.isScrolling) return
  chat.isScrolling = true
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (!chatMsgWrapper) {
    chat.isScrolling = false
    return
  }
  try {
    // 先强制设置scrollTop（跳过平滑滚动的延迟问题）
    chatMsgWrapper.scrollTop = chatMsgWrapper.scrollHeight
    // 平滑滚动兜底
    chatMsgWrapper.scrollTo({
      top: chatMsgWrapper.scrollHeight,
      left: 0,
      behavior: 'auto',
    })
    setTimeout(() => {
      chatMsgWrapper.scrollTop = chatMsgWrapper.scrollHeight
      chat.isScrolling = false
      checkScrollBottomBtn()
    }, 500)
  } catch (e) {
    chat.isScrolling = false
    chatMsgWrapper.scrollTop = chatMsgWrapper.scrollHeight
  }
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
    // 清空input值，允许重复选择同一文件
    e.target.value = ''
  }
}

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

// 发送消息
const sendMessage = async () => {
  if (requestLock.value || !beforeSendMessage()) return
  requestLock.value = true
  try {
    const response = await request('post', `/session/new`, chat.modelInfo, {
      signal: globalAbortCtrl.value.signal,
    })
    if (response.code === 200) {
      console.log(response)
      updateInfoByResponse(response.data)
      await getAndParseChatData(response.data, globalAbortCtrl.value.signal)
      updateHistoryByResponse(response.data)
    } else {
      ElMessage.error('发送消息异常: ' + response.msg)
    }
  } catch (e) {
    // 不做处理
  } finally {
    chat.resetChat()
    homeStatus.isNewSession = false
    requestLock.value = false
    await nextTick()
    scrollToBottom()
  }
}

const beforeSendMessage = () => {
  if (chat.isSending) return false
  const inputData = chat.inputData.trim()
  const hasUploadFiles = chat.uploadedFiles.length > 0
  if (!hasUploadFiles && inputData.length <= 0) return false

  chat.initModelInfo(userProfile)
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
      content: fileMessage.content,
      type: fileMessage.type,
      role: fileMessage.isUser ? 1 : 2
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
    content: message.content,
    type: message.type,
    role: message.isUser ? 1 : 2
  })
  chat.inputData = ''
  chat.isSending = true
  chat.modelInfo.newSession = homeStatus.isNewSession
  chat.modelInfo.messageType = 1
  chat.modelInfo.isDeepThink = chat.isDeepActive ? 1 : 0
  chat.modelInfo.isNetworkSearch = chat.isNetworkActive ? 1 : 0
  chat.modelInfo.fileIds = chat.uploadedFiles.map((f) => f.id).join(',')
  return true
}

const updateInfoByResponse = (response) => {
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

const getAndParseChatData = async (requestData, abortSignal) => {
  // 取消已有流式请求（避免重复）
  if (streamControllerRef.value) {
    streamControllerRef.value.abort()
  }
  streamControllerRef.value = new AbortController()
  const combinedSignal = AbortSignal.any([abortSignal, streamControllerRef.value.signal])

  try {
    // 创建流式消息项（初始空内容+加载状态）
    streamMsgIdRef.value = Date.now() + '-stream'
    const streamMsg = {
      id: streamMsgIdRef.value,
      content: '',
      isUser: false,
      type: 'text',
      isStreaming: true,
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
      body: JSON.stringify(requestData),
    })

    let errorMsg = '抱歉，请求异常，请重试'
    if (!response.ok) {
      const msgIndex = chat.messageList.findIndex((item) => item.id === streamMsgIdRef.value)
      if (msgIndex !== -1) {
        chat.messageList[msgIndex].content = `${errorMsg}`
        chat.messageList[msgIndex].isStreaming = false
      }
      // 让finally执行最终滚动
      throw new Error(errorMsg)
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
      console.log(buffer)
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
          // 处理分块消息（chunk：实时接收流式内容）
          if (eventName === 'chunk') {
            if (parsedData.message?.content) {
              const msgIndex = chat.messageList.findIndex(
                (item) => item.id === streamMsgIdRef.value,
              )
              if (msgIndex !== -1) {
                chat.$patch({
                  messageList: chat.messageList.map((item, index) => {
                    if (index === msgIndex) {
                      return {
                        ...item,
                        content: item.content + parsedData.message.content
                      }
                    }
                    return item
                  })
                })
                await nextTick()
              }
            }
          }

          // 处理结束事件（finished：完成事件单独返回格式）
          else if (eventName === 'finished') {
            const msgIndex = chat.messageList.findIndex((item) => item.id === streamMsgIdRef.value)
            if (msgIndex !== -1) {
              // 标记流式结束，优先使用后端返回的最终内容
              // const finalContent = parsedData.data?.messageList?.[0]?.content || chat.messageList[msgIndex].content
              // chat.$patch({
              //   messageList: chat.messageList.map((item, index) => {
              //     if (index === msgIndex) {
              //       return {
              //         ...item,
              //         content: finalContent,
              //         isStreaming: false
              //       }
              //     }
              //     return item
              //   })
              // })
            }
            // 释放资源，结束循环
            reader.releaseLock()
            return
          }

          // 处理错误事件（error：超时/业务异常）
          else if (eventName === 'error') {
            errorMsg = parsedData.msg || errorMsg
            const msgIndex = chat.messageList.findIndex((item) => item.id === streamMsgIdRef.value)
            if (msgIndex !== -1) {
              chat.messageList[msgIndex].content = `${errorMsg}`
              chat.messageList[msgIndex].isStreaming = false
            }
            reader.releaseLock()
            // 让finally执行最终滚动
            throw new Error(errorMsg)
          }
        } catch (e) {
          // 忽略不完整块的JSON解析错误，仅处理业务错误
          if (e.name !== 'SyntaxError') {
            console.error(`解析${eventName}事件失败:`, e)
          }
        }
      }
    }

    // 流式接收意外结束时，标记消息完成
    const msgIndex = chat.messageList.findIndex((item) => item.id === streamMsgIdRef.value)
    if (msgIndex !== -1) {
      chat.messageList[msgIndex].isStreaming = false
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      console.error('流式接收异常:', e)
      ElMessage.error(e.message || '消息接收失败，请重试')
      // 更新消息状态提示错误
      const msgIndex = chat.messageList.findIndex((item) => item.id === streamMsgIdRef.value)
      if (msgIndex !== -1 && !chat.messageList[msgIndex].content.includes('抱歉')) {
        chat.messageList[msgIndex].content += `\n\n ${e.message || '消息接收中断'}`
        chat.messageList[msgIndex].isStreaming = false
      }
    }
  } finally {
    await nextTick()
    setTimeout(() => {
      scrollToBottom()
      chat.isSending = false
      streamControllerRef.value = null
    }, 300)
  }
}

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

watch(
  () => homeStatus.currentMenu,
  async (newMenu) => {
    homeStatus.setCurrentMenu(newMenu)
    homeStatus.initIsNewSession()
    chat.initIsInputEnabled(homeStatus)
    chat.inputData = ''
    if (chat.isInputEnabled) {
      await nextTick()
      chatMsgInputFocus()
    }
  },
  { immediate: true, flush: 'sync' },
)

onMounted(() => {
  homeStatus.initIsNewSession()
  chat.initIsInputEnabled(homeStatus)
  // 监听聊天区域的滚动事件，实时检测是否显示按钮
  const chatMsgWrapper = chatMsgWrapperRef.value
  if (chatMsgWrapper) {
    chatMsgWrapper.addEventListener('scroll', checkScrollBottomBtn)
  }
  // 监听消息列表变化，自动检测滚动位置
  const unwatchMessages = watch(
    () => chat.messageList.length,
    async () => {
      await nextTick()
      checkScrollBottomBtn()
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
      scrollToBottom()
      checkScrollBottomBtn()
    })
  }, 300)
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
  if (streamControllerRef.value) streamControllerRef.value.abort()
  requestLock.value = false
})

func.setChatMsgScrollTop(chatMsgScrollTop)
func.setChatMsgInputFocus(chatMsgInputFocus)
func.setScrollToBottom(scrollToBottom)
func.setTriggerNewChat(triggerNewChat)
</script>

<style scoped></style>
