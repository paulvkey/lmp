<template>
  <div class="container" @click="hideUserMenuOnOutsideClick">
    <!-- 左侧固定菜单 -->
    <div class="left-container" :class="{ collapsed: isCollapsed }">
      <!-- 左侧顶部横栏 -->
      <div class="left-top-bar">
        <div class="left-title" v-if="!isCollapsed">对话平台</div>
        <button class="collapse-btn" @click.stop="toggleCollapse">
          <!-- 折叠/展开图标 -->
          <svg
            v-if="isCollapsed"
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="15 3 21 3 21 9"></polyline>
            <polyline points="9 21 3 21 3 15"></polyline>
            <line x1="21" y1="3" x2="14" y2="10"></line>
            <line x1="3" y1="21" x2="10" y2="14"></line>
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="4 14 10 14 10 20"></polyline>
            <polyline points="20 10 14 10 14 4"></polyline>
            <line x1="14" y1="10" x2="21" y2="3"></line>
            <line x1="3" y1="21" x2="10" y2="14"></line>
          </svg>
        </button>
      </div>

      <!-- 菜单项 -->
      <div class="menu-items">
        <!-- 新对话：蓝色高亮 -->
        <div class="menu-item new-menu" :class="{ active: true }" @click.stop="handleNewChat">
          <svg
            class="menu-icon"
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"
            ></path>
            <polyline points="22,6 12,13 2,6"></polyline>
          </svg>
          <span class="menu-text" v-if="!isCollapsed">新对话</span>
        </div>

        <!-- 收藏 -->
        <div
          class="menu-item collection-menu"
          :class="{ active: currentMenu === 'collection' }"
          @click.stop="handleCollectionClick"
        >
          <svg
            class="menu-icon"
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polygon
              points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
            ></polygon>
          </svg>
          <span class="menu-text" v-if="!isCollapsed">对话收藏</span>
        </div>

        <hr class="menu-divider" />

        <!-- 历史对话 -->
        <div
          class="menu-item session-menu"
          :class="{ active: currentMenu === 'session' }"
          @click.stop="handleSessionClick"
        >
          <svg
            class="menu-icon"
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
          </svg>
          <span class="menu-text" v-if="!isCollapsed">历史对话</span>
          <svg
            class="expand-icon"
            :style="{ transform: isSessionExpanded ? 'rotate(90deg)' : 'rotate(0)' }"
            xmlns="http://www.w3.org/2000/svg"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>

        <!-- 历史对话列表 -->
        <div
          class="session-list-container"
          v-if="isSessionExpanded"
          :class="{ collapsing: !isSessionExpanded }"
        >
          <div class="session-loading" v-if="isLoadingSession">加载中...</div>
          <div class="session-list" v-else>
            <div
              v-for="(item, index) in sessionList"
              :key="item.id"
              :id="`session-${item.id}`"
              :data-session-id="item.id"
              ref="sessionItemsRef"
              class="session-item"
              :class="{ 'session-item-selected': selectedSessionId === item.id }"
              @click.stop="handleSessionItemAction(item)"
            >
              <!-- 历史对话标题和三点菜单容器 -->
              <div class="session-item-content">
                <span
                  class="session-title"
                  :class="{ pinned: item.isPinned }"
                  @click.stop="loadSessionConversation(item)"
                >
                  {{ item.sessionTitle }}
                  <span v-if="item.isPinned" class="pin-icon">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="12"
                      height="12"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="black"
                      stroke-width="3"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <rect x="4" y="5" width="16" height="1" rx="1" ry="1" stroke-width="2" />
                      <line x1="12" y1="13" x2="12" y2="24" />
                      <line x1="12" y1="12" x2="8" y2="19" />
                      <line x1="12" y1="12" x2="16" y2="19" />
                    </svg>
                  </span>
                </span>

                <!-- 三点菜单按钮 -->
                <button
                  class="menu-dots"
                  @click.stop="toggleSessionMenu(item.id)"
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
                class="session-item-menu"
                v-if="activeSessionMenuId === item.id"
                :class="{ active: activeSessionMenuId === item.id }"
                @click.stop
              >
                <div class="menu-item" @click.stop="pinSession(item.id, !item.isPinned)">
                  {{ item.isPinned ? '取消置顶' : '置顶' }}
                </div>
                <div class="menu-item" @click.stop="collectSession(item.id)">
                  {{ item.isCollected ? '取消收藏' : '收藏' }}
                </div>
                <div
                  class="menu-item"
                  @click.stop="openSessionRenameDialog(item.id, item.sessionTitle)"
                >
                  重命名
                </div>
                <div class="menu-item delete-item" @click.stop="deleteSession(item.id, index)">
                  删除
                </div>
              </div>
            </div>
            <div class="session-empty" v-if="sessionList.length === 0 && !isLoadingSession">
              暂无历史对话
            </div>
          </div>
        </div>
      </div>

      <!-- 底部用户信息区域 -->
      <div class="user-profile" @click.stop="toggleUserMenu">
        <img ref="avatarRef" alt="头像" class="user-avatar" />
        <span
          ref="usernameRef"
          class="user-name"
          :style="{ visibility: isCollapsed ? 'hidden' : 'visible' }"
        />
      </div>

      <!-- 用户功能菜单 -->
      <div class="user-menu" v-if="showUserMenu" @click.stop>
        <div class="menu-item" @click="goToSettings" v-if="userStore.isLogin">个人中心</div>
        <div class="menu-item" @click="goToLogin" v-else>登录账号</div>
      </div>
    </div>

    <!-- 右侧整体区域 -->
    <div class="right-container">
      <!-- 顶部横栏 -->
      <div class="top-bar" ref="topBarRef">
        <div class="top-bar-content">
          <div class="title-wrapper" @click="openTitleDialog">
            <span class="chat-title">{{ chatTitle }}</span>
          </div>
          <button class="top-bar-btn" @click="collectChat">
            {{ currentChatIsCollected ? '取消收藏' : '收藏对话' }}
          </button>
        </div>
      </div>

      <!-- 对话滚动区域 -->
      <div class="chat-wrapper" ref="chatWrapperRef">
        <div class="message-container" ref="messageContainerRef">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message-item', msg.isUser ? 'user-message' : 'system-message']"
          >
            <div class="message-bubble" v-if="msg.isUser">
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
              <div v-else-if="msg.type === 'text'" v-html="formatUserMessage(msg.content)"></div>
            </div>

            <div class="message-bubble-container" v-else-if="!msg.isUser">
              <div class="loading-indicator" v-if="msg.isStreaming">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div class="message-bubble markdown-body" v-html="renderMarkdown(msg.content)"></div>
            </div>
            <div class="message-bubble" v-else v-html="formatUserMessage(msg.content)"></div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-container" ref="inputContainerRef">
          <div class="input-panel">
            <!-- 上传文件展示区 -->
            <div class="uploaded-files-container" v-if="uploadedFiles.length > 0">
              <div class="uploaded-files">
                <div
                  class="uploaded-file-item"
                  v-for="(file, index) in visibleFiles"
                  :key="`${file.name}-${file.size}-${index}`"
                  :title="file.name"
                >
                  <span class="file-icon">
                    <img
                      v-if="file.isImage && file.previewUrl"
                      :src="file.previewUrl"
                      :alt="file.name"
                      class="image-preview"
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
                  <div class="file-info">
                    <span class="file-name">{{ file.name }}</span>
                    <!-- 上传进度显示 -->
                    <div v-if="file.uploading || file.progress > 0" class="upload-progress">
                      <div
                        class="upload-progress-bar"
                        :style="{ width: `${file.progress}%` }"
                      ></div>
                    </div>
                  </div>
                  <button
                    class="remove-file"
                    @click.stop="removeFile(index)"
                    :title="`移除 ${file.name}`"
                    :disabled="file.uploading"
                  >
                    ×
                  </button>
                </div>

                <!-- 更多文件按钮 -->
                <button
                  class="more-files-btn"
                  v-if="hasMoreFiles"
                  @click.stop="showAllFiles = true"
                >
                  +{{ uploadedFiles.length - 6 }} 更多文件
                </button>
              </div>
            </div>

            <textarea
              ref="messageInputRef"
              v-model="inputData"
              placeholder="发送消息（Alt+Enter换行，Enter发送）"
              class="custom-input"
              @keydown="handleKeydown"
              rows="3"
              :disabled="!isInputEnabled"
            ></textarea>

            <div class="button-group">
              <div class="left-buttons">
                <button
                  class="deep-thinking"
                  @click="isDeepActive = !isDeepActive"
                  :class="{ active: isDeepActive }"
                  :disabled="!isInputEnabled"
                >
                  深度思考
                </button>
                <button
                  class="network-search"
                  @click="isNetworkActive = !isNetworkActive"
                  :class="{ active: isNetworkActive }"
                  :disabled="!isInputEnabled"
                >
                  联网搜索
                </button>
                <!-- 上传文件按钮 -->
                <button
                  class="upload-file"
                  @click="handleFileUploadClick"
                  :disabled="!isInputEnabled"
                >
                  上传文件
                </button>
                <!-- 隐藏的文件上传输入 -->
                <input
                  type="file"
                  ref="fileInputRef"
                  class="file-input"
                  @change="handleFileSelected"
                  style="display: none"
                  multiple
                  :disabled="!isInputEnabled"
                />
              </div>
              <!-- 发送按钮 -->
              <button
                class="send-btn"
                @click="sendMessage"
                :class="{ 'send-active': inputData.trim() || uploadedFiles.length > 0 }"
                :disabled="isSending"
              >
                <template v-if="isSending">
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

        <!-- 滚动到底部按钮 -->
        <div
          class="scroll-to-bottom"
          @click="scrollToBottom"
          v-show="showScrollBtn"
          :class="{ active: showScrollBtn }"
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

    <!-- 所有文件弹窗 -->
    <div class="dialog-overlay" v-if="showAllFiles" @click="showAllFiles = false">
      <div class="dialog all-files-dialog" @click.stop>
        <div class="dialog-header">
          <h3>所有文件 ({{ uploadedFiles.length }})</h3>
          <button class="dialog-close" @click="showAllFiles = false">×</button>
        </div>
        <div class="dialog-content all-files-content">
          <div class="all-files-list">
            <div
              class="all-file-item"
              v-for="(file, index) in uploadedFiles"
              :key="`${file.name}-${file.size}-${index}`"
              :title="file.name"
            >
              <span class="file-icon">
                <img
                  v-if="file.previewUrl"
                  :src="file.previewUrl"
                  :alt="file.name"
                  class="image-preview"
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
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">{{ formatFileSize(file.size) }}</span>
              <button
                class="remove-file"
                @click.stop="removeFile(index)"
                :title="`移除 ${file.name}`"
              >
                ×
              </button>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-btn confirm" @click="showAllFiles = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 对话名称修改弹窗 -->
    <div class="dialog-overlay" v-if="isTitleDialogShow" @click="isTitleDialogShow = false">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ isSessionRename ? '重命名对话' : '编辑对话名称' }}</h3>
          <button class="dialog-close" @click="isTitleDialogShow = false">×</button>
        </div>
        <div class="dialog-content">
          <input
            v-model="tempEditTitle"
            class="title-dialog-input"
            :maxlength="MAX_TITLE_LENGTH"
            @keydown.enter="confirmEditTitle"
            @input="handleTitleDialogInput"
            ref="dialogInputRef"
            placeholder="请输入对话名称"
          />
          <div class="dialog-hint">提示：名称长度不超过{{ MAX_TITLE_LENGTH }}个字符</div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-btn cancel" @click="isTitleDialogShow = false">取消</button>
          <button class="dialog-btn confirm" @click="confirmEditTitle">确认</button>
        </div>
      </div>
    </div>

    <!-- 收藏对话弹窗 -->
    <div
      class="dialog-overlay"
      v-if="isCollectionDialogShow"
      @click="isCollectionDialogShow = false"
    >
      <div class="dialog collection-dialog" @click.stop>
        <div class="dialog-header">
          <h3>已收藏对话</h3>
          <button class="dialog-close" @click="isCollectionDialogShow = false">×</button>
        </div>
        <div class="collection-table-header">
          <div class="table-header-row">
            <div class="header-cell title-cell">对话名称</div>
            <div class="header-cell note-cell">收藏备注</div>
            <div class="header-cell time-cell">收藏时间</div>
            <div class="header-cell action-cell">操作</div>
          </div>
        </div>
        <div class="dialog-content">
          <!-- 加载状态 -->
          <div class="loading-collection" v-if="isLoading">加载中...</div>
          <div class="collection-table-body" v-else>
            <!-- 渲染 PageInfo 中的当前页数据 -->
            <div class="table-row" v-for="item in collectionPage.list" :key="item.id">
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
            <div class="empty-tip" v-if="collectionPage.list.length === 0 && !isLoading">
              暂无收藏的对话
            </div>
          </div>
        </div>
        <!-- 分页控制器 -->
        <div class="dialog-pagination">
          <PaginationComp
            :total="collectionPage.total"
            :page-num="collectionPage.pageNum"
            :pages="collectionPage.pages"
            :has-previous-page="collectionPage.hasPreviousPage"
            :has-next-page="collectionPage.hasNextPage"
            :page-size="pageSize"
            :visible="collectionPage.total > 0 && !isLoading"
            :disabled="isLoading"
            @page-size-change="handlePageSizeChange"
            @page-num-change="handlePageNumChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useUserStore } from '@/store/userInfo.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request.js' // noinspection ES6UnusedImports
import PagingHandler from '@/components/PagingHandler.vue' // noinspection ES6UnusedImports
import { formatFileSize, getImageDimensions } from '@/utils/fileUtils.js' // noinspection ES6UnusedImports
import { formatTimeSecond } from '@/utils/dateUtils.js'
// noinspection ES6UnusedImports
import 'katex/dist/katex.min.css'

// 常量定义
const MAX_TITLE_LENGTH = 30
const SCROLL_DELAY = 50
// 定时刷新历史对话的间隔（5分钟）
const SESSION_REFRESH_INTERVAL = 300000 // 单位：毫秒
const SMOOTH_SCROLL_DELAY = 100
const POSITION_ADJUST_DELAY = 300
// 存储定时器ID，用于后续清除
const sessionTimerRef = ref(null)
const isScrolling = ref(false)

// 用户数据存储
const userStore = useUserStore()
const isCompAlive = ref(true)

// 用户信息相关
const usernameRef = ref(null)
const avatarRef = ref(null)
const showUserMenu = ref(false)

// 收藏相关
const sessionItemsRef = ref([])
const isCollectionDialogShow = ref(false)
const isLoading = ref(false) // 加载状态
const pageSize = ref(5) // 每页显示条数
// 分页状态
const collectionPage = ref({
  list: [], // 当前页数据列表
  total: 0, // 总记录数
  pageNum: 1, // 当前页码
  pageSize: 5, // 每页条数
  pages: 0, // 总页数
  hasPreviousPage: false, // 是否有上一页
  hasNextPage: false, // 是否有下一页
})

// 历史对话相关
const isSessionExpanded = ref(true)
const sessionList = ref([])
const isLoadingSession = ref(false)
const activeSessionMenuId = ref(null)
const selectedSessionId = ref(null)
const isLoadingCurrentSession = ref(false)
const loadedSessionId = ref(null)
// 区分是对话重命名还是当前对话标题修改
const isSessionRename = ref(false)
// 存储当前正在重命名的对话ID
const renamingSessionId = ref(null)
// 当前对话的收藏状态
const currentChatIsCollected = ref(false)

// 文件上传相关
const fileInputRef = ref(null)
const uploadedFiles = ref([])
// 控制所有文件弹窗显示
const showAllFiles = ref(false)
const filesSent = ref(false)

// 计算属性：可见文件（最多6个）
const visibleFiles = computed(() => {
  return uploadedFiles.value.slice(0, 6)
})

// 计算属性：是否有更多文件
const hasMoreFiles = computed(() => {
  return uploadedFiles.value.length > 6
})

// UI状态相关
const isCollapsed = ref(false)
// 默认选中"新对话"
const currentMenu = ref('new')
const inputData = ref('')
const isDeepActive = ref(false)
const isNetworkActive = ref(false)
const messages = ref([])
const showScrollBtn = ref(false)
const chatTitle = ref('新对话')
const isTitleDialogShow = ref(false)
const tempEditTitle = ref('')

const isInputEnabled = ref(true)
const isNewSession = ref(currentMenu.value === 'new')
const isSending = ref(false)

// DOM引用
const topBarRef = ref(null)
const chatWrapperRef = ref(null)
const messageContainerRef = ref(null)
const inputContainerRef = ref(null)
const dialogInputRef = ref(null)
const messageInputRef = ref(null)

// 尺寸相关变量
const inputHeight = ref(200)
const messageContainerHeight = ref(0)
const topBarHeight = ref(0)
let resizeObserver = null
let topBarResizeObserver = null
let scrollTimer = null

// 调用模型
const originalModelData = computed(() => ({
  newSession: true,
  isLogin: userStore.isLogin,
  userId: userStore.userId,
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
  messageList: null,
  sendTime: null,
  fileIds: '',
  tokenCount: 0,
  isDeepThink: 0,
  isNetworkSearch: 0,
  deepThinkStep: '',
}))
const modelData = ref({ ...originalModelData.value })
// 流式消息暂存器
// 结构: { [sessionId]: { id: string, content: string } }
const streamHolder = ref({})

const { proxy } = getCurrentInstance()

// 工具函数
const getElementHeight = (el) => el?.offsetHeight || 0
const getViewHeight = () => document.documentElement.clientHeight || window.innerHeight
const getScrollTop = () => document.documentElement.scrollTop || document.body.scrollTop

// 封装登录检查函数
const checkLoginStatus = () => {
  return userStore.isLogin && userStore.userId > 0
}

// 语法状态机：跟踪当前Markdown解析状态
const createMarkdownState = () => ({
  inList: false, // 是否在列表中（有序/无序）
  listType: null, // 列表类型：'ordered'/'unordered'
  listLevel: 0, // 列表嵌套层级
  inCodeBlock: false, // 是否在代码块中
  codeBlockLang: null, // 代码块语言
  pendingSeparator: 0, // 累积的分割线符号数（用于检测---）
  lastLine: '', // 上一行内容（用于判断换行需求）
})

// 全局状态管理（按sessionId隔离）
const mdStates = {} // { [sessionId]: 状态机实例 }

// renderMarkdown 函数，支持流式数据
const renderMarkdown = (content, isStreaming = false, sessionId) => {
  if (!content) return ''
  let processedContent = content

  if (isStreaming && sessionId) {
    // 初始化状态机
    if (!mdStates[sessionId]) {
      mdStates[sessionId] = createMarkdownState()
    }
    const state = mdStates[sessionId]

    // 按行处理内容（避免跨多行的语法误判）
    const lines = processedContent.split('\n')
    const fixedLines = []

    lines.forEach((line) => {
      // 1. 代码块内不处理（避免干扰代码内容）
      if (line.startsWith('```')) {
        state.inCodeBlock = !state.inCodeBlock
        state.codeBlockLang = state.inCodeBlock ? line.slice(3).trim() : null
        fixedLines.push(line)
        return
      }
      if (state.inCodeBlock) {
        fixedLines.push(line)
        return
      }

      // 2. 处理列表（动态补全换行和缩进）
      const listMatch = line.match(/^(\s*)(- |\d+\. )(.*)/)
      if (listMatch) {
        const [, indent, marker, text] = listMatch
        const currentType = marker.startsWith('-') ? 'unordered' : 'ordered'

        // 列表开始：如果之前不在列表中，补换行
        if (!state.inList) {
          fixedLines.push('') // 列表前补空行
          state.inList = true
          state.listType = currentType
          state.listLevel = 1
        }
        // 列表类型切换：补换行分隔
        else if (state.listType !== currentType) {
          fixedLines.push('') // 不同类型列表间补空行
          state.listType = currentType
          state.listLevel = 1
        }
        // 嵌套层级调整（根据缩进长度）
        const currentLevel = Math.ceil(indent.length / 2) // 2空格=1层级
        if (currentLevel !== state.listLevel) {
          state.listLevel = currentLevel
        }

        fixedLines.push(`${indent}${marker}${text}`)
        state.lastLine = line
        return
      }
      // 非列表行：如果之前在列表中，补换行结束列表
      else if (state.inList) {
        fixedLines.push('') // 列表后补空行
        state.inList = false
        state.listLevel = 0
      }

      // 3. 处理分割线（动态检测连续---）
      const separatorMatch = line.match(/(-+)/g)
      if (separatorMatch) {
        const totalDashes = separatorMatch.reduce((sum, s) => sum + s.length, 0)
        state.pendingSeparator += totalDashes
        // 分割线至少需要3个-，且单独成行
        if (state.pendingSeparator >= 3 && line.trim() === '-'.repeat(state.pendingSeparator)) {
          fixedLines.push('---') // 标准化分割线
          state.pendingSeparator = 0
          state.lastLine = '' // 分割线后重置行状态
          return
        }
      } else {
        state.pendingSeparator = 0 // 非分割线内容重置计数
      }

      // 4. 普通文本：确保与上一行的语法兼容性
      if (state.lastLine && !state.lastLine.endsWith('\n') && line) {
        fixedLines.push(line)
      } else {
        fixedLines.push(line)
      }
      state.lastLine = line
    })

    processedContent = fixedLines.join('\n')

    // 5. 修复未闭合的结构（仅流式传输时）
    if (state.inList) {
      processedContent += '\n...' // 列表未结束标记
    }
    if (state.pendingSeparator > 0 && state.pendingSeparator < 3) {
      processedContent += '-'.repeat(3 - state.pendingSeparator) // 补全分割线
    }
  }

  // 渲染逻辑（保持不变）
  try {
    let html = proxy.$md.render(processedContent)
    if (!isStreaming && sessionId) {
      // 流结束后清理状态机
      delete mdStates[sessionId]
      html = html.replace('...', '') // 移除未结束标记
    }
    return html
  } catch (error) {
    console.error('Markdown解析失败:', error)
    return content
  }
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

// 节流函数
const throttle = (fn, delay = 100) => {
  let lastTime = 0
  return (...args) => {
    const now = Date.now()
    if (now - lastTime > delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

// 用户信息设置
const setUserInfo = async () => {
  await nextTick()
  if (usernameRef.value) {
    usernameRef.value.textContent = userStore.username
  }
  if (avatarRef.value) {
    avatarRef.value.src = userStore.avatar
  }
}

// 监听用户信息变化
watch(() => [userStore.username, userStore.avatar], setUserInfo, { immediate: true })

// 重置当前对话窗口（清除消息、标题等）
const resetCurrentChat = () => {
  // 重置消息列表
  messages.value = []
  // 重置对话标题
  chatTitle.value = '新对话'
  // 清空输入框
  inputData.value = ''
  // 清空上传文件
  uploadedFiles.value = []
  showAllFiles.value = false
  // 重置模型相关状态
  modelData.value = { ...originalModelData.value }
  isDeepActive.value = false
  isNetworkActive.value = false
  // 滚动到顶部
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = 0
    }
    showScrollBtn.value = false
  })
  nextTick(() => {
    messageInputRef.value?.focus()
  })
}

const handleNewChat = async () => {
  // 重置当前对话窗口状态
  resetCurrentChat()
  // 切换菜单状态
  currentMenu.value = 'new'
  isNewSession.value = true
  // 清除历史对话相关高亮
  selectedSessionId.value = null
  activeSessionMenuId.value = null
  localStorage.setItem('needReloadSession', false)
}

watch(currentMenu, (newMenu) => {
  // 只有当前菜单是"新对话"时才启用输入框
  isInputEnabled.value = newMenu === 'new'
  if (!isInputEnabled.value) {
    inputData.value = ''
  }
})

watch(
  () => originalModelData.value,
  (newVal) => {
    modelData.value = {
      ...modelData.value,
      isLogin: newVal.isLogin,
      userId: newVal.userId,
    }
  },
  { deep: true },
)

// 收藏相关方法
// 加载收藏列表（后端分页）
const loadCollections = async () => {
  isLoading.value = true
  try {
    const response = await request('get', `/collection/user/${userStore.userId}`, null, {
      params: { pageNum: collectionPage.value.pageNum, pageSize: pageSize.value },
    })
    // 同步所有分页状态
    collectionPage.value = {
      list: response.data.list || [],
      total: response.data.total || 0,
      pageNum: response.data.pageNum || 1,
      pageSize: response.data.pageSize || pageSize.value,
      pages: response.data.pages || 0,
      hasNextPage: response.data.hasNextPage || false,
      hasPreviousPage: response.data.hasPreviousPage || false,
    }
    pageSize.value = collectionPage.value.pageSize
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    collectionPage.value = { list: [], total: 0, pageNum: 1, pageSize: pageSize.value }
  } finally {
    isLoading.value = false
  }
}

// 分页处理函数
const handlePageChange = async (newPageNum, newPageSize) => {
  collectionPage.value.pageNum = newPageNum
  if (newPageSize) {
    pageSize.value = newPageSize
    collectionPage.value.pageSize = newPageSize
  }
  await loadCollections()
}

// 改变每页显示条数
const handlePageSizeChange = (newSize) => {
  pageSize.value = newSize
  handlePageChange(1, newSize)
}

// 切换每页条数时，重置到第一页
const handlePageNumChange = (newNum) => {
  handlePageChange(newNum)
}

const handleCollectionClick = async () => {
  if (!checkLoginStatus()) {
    ElMessage.warning('请先登录，再查看收藏的对话')
    return
  }
  if (currentMenu.value === 'session' || selectedSessionId.value) {
    // 重置当前对话状态
    resetCurrentChat()
    // 清除历史对话选中状态
    selectedSessionId.value = null
    loadedSessionId.value = null
  }
  currentMenu.value = 'collection'
  collectionPage.value.pageNum = 1
  await loadCollections()
  if (collectionPage.value.list.length === 0) {
    ElMessage.info('暂无收藏的对话')
    await handleNewChat()
  } else {
    isCollectionDialogShow.value = true
  }
}

const scrollToSessionItem = async (targetSessionId, forceReload = false) => {
  if (!targetSessionId) {
    console.warn('缺少目标对话ID，无法定位对话项')
    return
  }

  try {
    // 切换到历史对话菜单，确保菜单状态正确
    currentMenu.value = 'session'
    // 展开历史对话列表（如果未展开）
    isSessionExpanded.value = true
    // 先高亮目标对话（视觉反馈优先）
    selectedSessionId.value = targetSessionId
    // 检查对话是否存在于当前列表中，不存在则重新加载
    let sessionExists = sessionList.value.some((item) => item.id === targetSessionId)
    if (!sessionExists || forceReload) {
      await loadSessionList() // 重新加载对话列表
      sessionExists = sessionList.value.some((item) => item.id === targetSessionId)
      // 重新设置选中状态（确保加载后高亮有效）
      selectedSessionId.value = sessionExists ? targetSessionId : null
    }

    // 如果对话存在，等待DOM更新后滚动到目标位置
    if (sessionExists) {
      await nextTick()
      const targetElement = document.querySelector(`[data-session-id="${targetSessionId}"]`)
      const sessionContainer = document.querySelector('.session-list-container')
      if (targetElement && sessionContainer) {
        // 计算目标元素在容器中的相对位置（考虑容器滚动偏移）
        const containerRect = sessionContainer.getBoundingClientRect()
        const elementRect = targetElement.getBoundingClientRect()
        const relativeTop = elementRect.top - containerRect.top
        // 滚动到目标位置（预留10px顶部间距，优化视觉体验）
        sessionContainer.scrollTop = relativeTop - 10
        // 可选：若需要平滑滚动，可替换为以下代码
        // targetElement.scrollIntoView({
        //   behavior: 'smooth',
        //   block: 'nearest', // 优先保证元素可见，不强制居中
        //   inline: 'start'
        // });
      }
    } else {
      console.warn(`未找到ID为${targetSessionId}的对话`)
    }
  } catch (error) {
    console.error('定位对话项失败：', error)
  }
}

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

    chatTitle.value = chatSession.sessionTitle
    messages.value = messageList
    isCollectionDialogShow.value = false
    collectionPage.value.pageNum = 1

    await scrollToSessionItem(chatSession.id)
    await nextTick(scrollToBottom)
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
      const currentListLength = collectionPage.value.list.length
      const isLastPage = collectionPage.value.pageNum === collectionPage.value.pages
      if (currentListLength === 1 && isLastPage && collectionPage.value.pageNum > 1) {
        collectionPage.value.pageNum--
      }
      ElMessage.success('收藏已删除')
      await loadCollections()
    } else {
      ElMessage.success('收藏删除失败')
    }
  } catch (error) {
    console.log(error)
  }
}

// 历史对话相关方法
// 获取对话历史
const loadSessionList = async () => {
  if (isLoadingSession.value) {
    // 若重复调用，等待前一次加载完成（避免并发问题）
    await new Promise((resolve) => {
      const check = () => {
        if (!isLoadingSession.value) resolve()
        else setTimeout(check, 20)
      }
      check()
    })
    return
  }

  isLoadingSession.value = true
  try {
    const response = await request('get', `/session/history/${userStore.userId}`)
    // 更新响应式数据，并等待Vue同步
    sessionList.value = response.data
      .map((item, index) => ({ indexId: `${item.id}_${index}`, ...item }))
      .sort((a, b) =>
        a.isPinned === b.isPinned
          ? new Date(b.updatedAt) - new Date(a.updatedAt)
          : b.isPinned - a.isPinned,
      )
    await nextTick() // 确保sessionList.value已被Vue更新到DOM
  } catch (error) {
    console.log(error)
  } finally {
    isLoadingSession.value = false
  }
}

const handleSessionClick = async () => {
  if (!checkLoginStatus()) {
    ElMessage.warning('请先登录，再查看历史对话')
    return
  }

  isSessionExpanded.value = !isSessionExpanded.value
  if (isSessionExpanded.value && sessionList.value.length === 0) {
    await loadSessionList()
  }
}

// 获取历史对话中某个对话的详情
const loadSessionConversation = async (item) => {
  // 如果是同一个对话且已加载过数据，直接更新选中状态并滚动到底部，不重复请求
  currentChatIsCollected.value = item.isCollected
  if (item.id === loadedSessionId.value && messages.value.length > 0) {
    selectedSessionId.value = item.id
    currentMenu.value = 'session'
    await nextTick(scrollToBottom)
    return
  }
  // 如果正在加载中，阻止重复请求
  if (isLoadingCurrentSession.value) return
  // 准备加载新对话
  isLoadingCurrentSession.value = true
  selectedSessionId.value = item.id
  currentMenu.value = 'session'
  messages.value = []

  try {
    const response = await request('get', `/session/${item.id}`)
    const sessionData = response.data.chatSession
    const messageList = response.data.chatMessageList || []

    // 保存对话状态
    localStorage.setItem('currentSessionId', item.id)
    localStorage.setItem('needReloadSession', true)
    loadedSessionId.value = item.id
    chatTitle.value = sessionData.sessionTitle
    messages.value = messageList.map((msg) => ({
      content: msg.messageContent,
      isUser: msg.messageType === 1,
      type: msg.type ? msg.type : 'text',
    }))
    modelData.value.sessionId = sessionData.id
    modelData.value.sessionTitle = sessionData.sessionTitle
    modelData.value.aiModelId = sessionData.aiModelId
    modelData.value.isDeleted = sessionData.isDeleted
    modelData.value.isPinned = sessionData.isPinned
    modelData.value.isCollected = sessionData.isCollected
    modelData.value.createdAt = sessionData.createdAt
    modelData.value.sendTime = sessionData.sendTime
    modelData.value.lastMessageTime = sessionData.lastMessageTime
    currentChatIsCollected.value = sessionData.isCollected === 1
    await nextTick(scrollToBottom)
  } catch (error) {
    console.error('加载对话失败:', error)
    ElMessage.error('加载对话失败，请重试')
    messages.value = []
    loadedSessionId.value = null
  } finally {
    isLoadingCurrentSession.value = false
  }
}

const handleSessionItemAction = (item) => {
  handleSessionItemClick(item)
  loadSessionConversation(item)
  currentMenu.value = 'session'
}

const handleSessionItemClick = (item) => {
  selectedSessionId.value = item.id
}

// 计算对话菜单位置
const calculateSessionMenuPosition = (itemId) => {
  const itemEl = document.querySelector(`[data-session-id="${itemId}"]`)
  if (!itemEl) return
  const menuEl = itemEl.querySelector('.session-item-menu')
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

const toggleSessionMenu = (itemId) => {
  // 获取当前对话项元素
  const itemEl = document.querySelector(`[data-session-id="${itemId}"]`)
  if (activeSessionMenuId.value === itemId) {
    // 关闭菜单时移除激活类
    itemEl?.classList.remove('session-item-menu-active')
    activeSessionMenuId.value = null
    return
  }
  // 先移除其他项的激活类
  document.querySelectorAll('.session-item-menu-active').forEach((el) => {
    el.classList.remove('session-item-menu-active')
  })
  // 打开菜单时添加激活类
  itemEl?.classList.add('session-item-menu-active')
  activeSessionMenuId.value = itemId

  nextTick(() => {
    setTimeout(() => {
      calculateSessionMenuPosition(itemId)
    }, 0)
  })
}

// 打开对话重命名弹窗
const openSessionRenameDialog = (sessionId, currentTitle) => {
  // 关闭三点菜单
  activeSessionMenuId.value = null
  // 设置为对话重命名模式
  isSessionRename.value = true
  renamingSessionId.value = sessionId
  // 填充当前标题
  tempEditTitle.value = currentTitle
  // 显示弹窗
  isTitleDialogShow.value = true
  // 聚焦输入框
  nextTick(() => {
    dialogInputRef.value?.focus()
  })
}

// 修改确认编辑标题方法，使其能处理对话重命名
const confirmEditTitle = async () => {
  const newTitle = tempEditTitle.value.trim()
  if (!newTitle) {
    ElMessage.warning('名称不能为空')
    return
  }

  if (newTitle.length > MAX_TITLE_LENGTH) {
    ElMessage.warning(`名称长度不能超过${MAX_TITLE_LENGTH}个字符`)
    return
  }

  if (isSessionRename.value && renamingSessionId.value) {
    // 处理对话重命名
    try {
      const index = sessionList.value.findIndex((item) => item.id === renamingSessionId.value)
      if (index !== -1) {
        // 先更新本地显示
        const originalTitle = sessionList.value[index].sessionTitle
        sessionList.value[index].sessionTitle = newTitle
        const response = await request(
          'patch',
          `/session/${renamingSessionId.value}/rename`,
          null,
          {
            params: {
              sessionTitle: newTitle,
            },
          },
        )

        if (response.code !== 200) {
          // 更新失败，恢复原始标题
          sessionList.value[index].sessionTitle = originalTitle
          ElMessage.error('修改对话名失败，请重试')
        } else {
          ElMessage.success('修改对话名成功')
          // 如果是当前选中的对话，同时更新聊天标题
          if (renamingSessionId.value === selectedSessionId.value) {
            chatTitle.value = newTitle
          }
        }
      }
    } catch (error) {
      console.error('修改对话名失败:', error)
      ElMessage.error('修改对话名失败，请重试')
    }
  } else {
    // 处理当前对话标题修改
    if (checkLoginStatus()) {
      const response = await request(
        'patch',
        `/session/${modelData.value.sessionId}/rename`,
        null,
        {
          params: {
            sessionTitle: newTitle,
          },
        },
      )
      if (response.code === 200) {
        if (newTitle) chatTitle.value = newTitle
        const currentSessionId = modelData.value.sessionId
        if (currentSessionId) {
          // 找到左侧列表中对应的对话项
          const sessionIndex = sessionList.value.findIndex((item) => item.id === currentSessionId)
          if (sessionIndex !== -1) {
            // 更新列表中的标题
            sessionList.value[sessionIndex].sessionTitle = newTitle
          }
        }
        ElMessage.success('修改对话名成功')
      } else {
        ElMessage.error('修改对话名失败，请重试')
      }
    } else {
      if (newTitle) chatTitle.value = newTitle
    }
  }

  // 关闭弹窗并重置状态
  isTitleDialogShow.value = false
  isSessionRename.value = false
  renamingSessionId.value = null
}

// 封装输入长度限制函数
const useInputLengthLimit = (maxLength) => {
  // 处理输入长度限制
  const handleInput = (valueRef) => {
    if (valueRef.value.length > maxLength) {
      valueRef.value = valueRef.value.slice(0, maxLength)
      // 仅在首次达到最大长度时提示
      if (valueRef.value.length === maxLength) {
        ElMessage.warning(`已达到最大长度（${maxLength}字）`)
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

// 初始化标题编辑的限制逻辑（复用同一个hooks）
const { handleInput: handleTitleInputBase } = useInputLengthLimit(MAX_TITLE_LENGTH)

const handleTitleDialogInput = () => {
  handleTitleInputBase(tempEditTitle)
}

// 三点功能菜单操作方法
const pinSession = async (id, pinned) => {
  const index = sessionList.value.findIndex((item) => item.id === id)
  if (index !== -1) {
    sessionList.value[index].isPinned = pinned ? 1 : 0
    const response = await request('patch', `/session/${id}/pinned`, null, {
      params: { isPinned: pinned ? 1 : 0 },
    })
    if (response.code === 200) {
      if (pinned) {
        const [pinnedItem] = sessionList.value.splice(index, 1)
        sessionList.value.unshift(pinnedItem)
      }
      ElMessage.success(pinned ? '已置顶' : '已取消置顶')
    } else {
      sessionList.value[index].isPinned = pinned ? 0 : 1
      ElMessage.error('置顶/取消置顶失败')
    }
  }

  activeSessionMenuId.value = null
}

// 统一的收藏/取消收藏处理函数
const toggleCollection = async (sessionId) => {
  if (!sessionId) {
    ElMessage.warning('无法获取对话信息')
    return
  }
  const targetSession = sessionList.value.find((item) => item.id === sessionId)
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
    // 更新状态
    if (sessionId === selectedSessionId.value) {
      currentChatIsCollected.value = newState
    }
    const sessionIndex = sessionList.value.findIndex((item) => item.id === sessionId)
    if (sessionIndex !== -1) {
      sessionList.value[sessionIndex].isCollected = newState
    }
    ElMessage.success(newState ? '收藏成功' : '取消收藏成功')
    await loadCollections()
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败，请重试')
    if (sessionId === selectedSessionId.value) {
      currentChatIsCollected.value = currentState
    }
    const sessionIndex = sessionList.value.findIndex((item) => item.id === sessionId)
    if (sessionIndex !== -1) {
      sessionList.value[sessionIndex].isCollected = currentState
    }
    throw error
  }
}

// 三点功能菜单收藏相关
const collectSession = async (sessionId) => {
  // 先更新当前状态（如果是当前选中的对话）
  if (sessionId === selectedSessionId.value) {
    currentChatIsCollected.value = !currentChatIsCollected.value
  }
  await toggleCollection(sessionId)
  activeSessionMenuId.value = null
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
        sessionList.value.splice(index, 1)
        ElMessage.success('已删除')
        // 若删除的是当前选中的对话，清除存储
        if (id === selectedSessionId.value) {
          selectedSessionId.value = null
          localStorage.removeItem('currentSessionId')
          // 直接使用新对话窗口
          await handleNewChat()
        }
      } else {
        ElMessage.error('删除对话异常，请重试')
      }
    } catch (error) {
      console.log(error)
    }
  }
  activeSessionMenuId.value = null
}

// 菜单折叠相关
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  nextTick(() => {
    messageContainerHeight.value = getElementHeight(messageContainerRef.value)
    checkScrollPosition()
  })
}

// 布局同步方法
const syncTopBarHeight = () => {
  if (!topBarRef.value || !chatWrapperRef.value) return
  const currentHeight = getElementHeight(topBarRef.value)
  topBarHeight.value = currentHeight
  chatWrapperRef.value.style.paddingTop = `${currentHeight}px`
  checkScrollPosition()
}

// 滚动相关方法
const checkScrollPosition = () => {
  const uploadedFilesHeight = getUploadedFilesHeight()
  const hasScrollNeed =
    messageContainerHeight.value + uploadedFilesHeight > getViewHeight() - inputHeight.value
  if (!hasScrollNeed) {
    showScrollBtn.value = false
    return
  }
  const isAtBottom =
    getScrollTop() + getViewHeight() >= messageContainerHeight.value + uploadedFilesHeight
  showScrollBtn.value = hasScrollNeed && !isAtBottom
}

// 添加获取文件上传区域高度的函数
const getUploadedFilesHeight = () => {
  const container = document.querySelector('.uploaded-files-container')
  return container ? container.offsetHeight : 0
}

const handleScroll = () => {
  clearTimeout(scrollTimer)
  scrollTimer = setTimeout(checkScrollPosition, SCROLL_DELAY)
}

const scrollToBottom = () => {
  if (isScrolling.value) return
  isScrolling.value = true
  showScrollBtn.value = false
  nextTick(() => {
    setTimeout(() => {
      const messageContainer = messageContainerRef.value
      if (!messageContainer) return
      // 获取所有相关元素高度
      const uploadedFilesHeight = getUploadedFilesHeight()
      const containerRect = messageContainer.getBoundingClientRect()
      const windowHeight = window.innerHeight

      // 计算实际需要滚动的距离（基于容器底部相对于视口的位置）
      const scrollDistance =
        containerRect.bottom - windowHeight + topBarHeight.value + uploadedFilesHeight
      // 执行滚动
      window.scrollBy({
        top: scrollDistance,
        behavior: 'smooth',
      })

      setTimeout(() => {
        const currentTop = getScrollTop()
        const actualBottom = messageContainerHeight.value - getViewHeight() + topBarHeight.value
        if (currentTop < actualBottom - 5) {
          window.scrollTo({ top: actualBottom, behavior: 'auto' })
        }
        showScrollBtn.value = false
        isScrolling.value = false
      }, POSITION_ADJUST_DELAY)
    }, SMOOTH_SCROLL_DELAY)
  })
}

// 检查用户是否在底部
const isUserAtBottom = computed(() => {
  if (!messageContainerRef.value) return true
  const { scrollTop, scrollHeight, clientHeight } = messageContainerRef.value
  return scrollHeight - scrollTop - clientHeight < 30
})

// 建立SSE连接接收流式消息
const getStreamData = async (chatData) => {
  // 关闭已有连接（确保资源释放）
  if (window.currentReader) {
    await window.currentReader.cancel().catch((err) => console.error('取消现有流失败:', err))
    window.currentReader = null
  }

  const sessionId = chatData.sessionId
  // 防重复连接检查
  if (window.streamLoading?.[sessionId]) return
  window.streamLoading = { ...(window.streamLoading || {}), [sessionId]: true }

  // 用于累积SSE消息的缓冲区（解决多行数据分割问题）
  let sseBuffer = ''
  // JSON缓冲区（按sessionId隔离，存储不完整的JSON片段） 结构：{ [sessionId]: 累积的JSON字符串 }
  const jsonBuffers = {}

  // 创建带重连机制的流式请求
  const createSource = async () => {
    try {
      const response = await fetch('/session/model', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(chatData),
      })

      if (!response.ok) {
        console.error(`请求失败: ${response.status} ${response.statusText}`)
        window.streamLoading[sessionId] = false
        return
      }

      // 存储当前流和读取器
      window.currentStream = response.body
      const reader = response.body.getReader()
      window.currentReader = reader
      const decoder = new TextDecoder()

      // 处理分块消息的更新逻辑（优化节流和完整度）
      const updateMessage = (() => {
        let pendingContent = ''
        let pendingStreamId = null
        let lastUpdateTime = 0
        let timeout = null

        const doUpdate = (content, streamId) => {
          const msgIndex = messages.value.findIndex((m) => m.id === streamId)
          if (msgIndex === -1) return
          messages.value[msgIndex].content = renderMarkdown(content, true, sessionId)
          nextTick(() => {
            messageContainerHeight.value = getElementHeight(messageContainerRef.value)
            if (isUserAtBottom.value) scrollToBottom()
          })
        }

        // 定义要返回的节流函数（命名为throttled，方便挂载flush）
        const throttled = (content, streamId) => {
          const now = Date.now()
          pendingContent = content
          pendingStreamId = streamId

          // 动态调整节流时间
          const throttleDelay = content.length < 100 ? 100 : 50

          if (now - lastUpdateTime > throttleDelay) {
            doUpdate(content, streamId)
            lastUpdateTime = now
          } else {
            // 清理之前的定时器（用独立变量timeout存储）
            clearTimeout(timeout)
            timeout = setTimeout(() => {
              doUpdate(pendingContent, pendingStreamId)
              lastUpdateTime = Date.now()
            }, throttleDelay)
          }
        }

        // 给返回的函数（throttled）挂载flush方法
        throttled.flush = () => {
          if (pendingStreamId) {
            const finalContent = renderMarkdown(pendingContent, false, sessionId)
            const msgIndex = messages.value.findIndex((m) => m.id === pendingStreamId)
            if (msgIndex !== -1) {
              messages.value[msgIndex].content = finalContent
            }
            // 清理状态和定时器
            pendingContent = ''
            pendingStreamId = ''
            clearTimeout(timeout)
            timeout = null
          }
        }

        // 返回带flush方法的节流函数
        return throttled
      })()

      const splitSSEMessages = (buffer) => {
        const messages = []
        let startIndex = 0
        const separator = '\n\n'

        while (true) {
          const sepIndex = buffer.indexOf(separator, startIndex)
          if (sepIndex === -1) break

          const candidate = buffer.slice(startIndex, sepIndex)
          if (candidate.startsWith('data:')) {
            messages.push(candidate)
            startIndex = sepIndex + separator.length
          } else {
            // 非data开头的内容合并到下一个消息，防止有效内容丢失
            startIndex = sepIndex + 1
          }
        }

        const remaining = buffer.slice(startIndex)
        return { messages, remaining }
      }

      // 循环读取流数据
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          // 流结束：释放资源+强制更新最后内容
          reader.releaseLock()
          window.currentReader = null
          window.streamLoading[sessionId] = false
          updateMessage.flush() // 确保最后内容被渲染
          break
        }

        // 解码当前分块并累积到缓冲区
        const chunk = decoder.decode(value, { stream: true })
        sseBuffer += chunk

        const { messages: completeMessages, remaining } = splitSSEMessages(sseBuffer)
        sseBuffer = remaining

        // 处理每个完整的SSE消息
        completeMessages.forEach((sseMessage) => {
          let eventType = 'chunk' // 默认事件类型
          let data = '' // 累积当前消息的所有data内容

          // 分割消息内的行（处理单行/多行data）
          const lines = sseMessage.split('\n').filter((line) => line.trim() !== '')
          lines.forEach((line) => {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              // 只去除"data:"后的前导空格（不trim整个字段值）
              const lineData = line.slice(5).replace(/^\s+/, '') // 例如"data:  {\"a\":1}" → "{\"a\":1}"
              data += lineData
            }
          })

          // 处理不同事件类型
          switch (eventType) {
            case 'chunk':
              handleChunkEvent(data, updateMessage, jsonBuffers, sessionId)
              break
            case 'complete':
              handleCompleteEvent(data, updateMessage, jsonBuffers, sessionId)
              break
            case 'error':
              handleErrorEvent(data, sessionId)
              break
            default:
              console.warn('未知事件类型:', eventType)
          }
        })
      }
    } catch (error) {
      console.error('流式请求异常:', error)
      // 异常时清理资源
      window.streamLoading[sessionId] = false
      if (window.currentReader) {
        await window.currentReader
          .cancel('请求异常，取消流')
          .catch((err) => console.error('取消流失败:', err))
        window.currentReader.releaseLock()
        window.currentReader = null
      }
      if (jsonBuffers[sessionId]) {
        delete jsonBuffers[sessionId]
      }
    }
  }

  await createSource()
}

// 处理分块消息事件
const handleChunkEvent = (data, updateMessage, jsonBuffers, sessionId) => {
  if (!data) return

  try {
    // 累积JSON缓冲区（不做结构预判，直接尝试解析）
    if (!jsonBuffers[sessionId]) jsonBuffers[sessionId] = ''
    jsonBuffers[sessionId] += data
    const eventData = JSON.parse(jsonBuffers[sessionId])

    // 初始化/更新流式消息
    const currentSessionId = modelData.value.sessionId
    if (!streamHolder.value[currentSessionId]) {
      const tempId = `stream_${Date.now()}`
      streamHolder.value[currentSessionId] = { id: tempId, content: '' }
      messages.value.push({
        id: tempId,
        content: '',
        isUser: false,
        isStreaming: true,
      })
    }
    const stream = streamHolder.value[currentSessionId]
    stream.content += eventData.message?.content || ''
    updateMessage(stream.content, stream.id)
    jsonBuffers[sessionId] = '' // 解析成功后清空缓冲区
  } catch (e) {
    console.warn(
      `JSON拼接中（sessionId: ${sessionId}），当前缓冲区预览：${jsonBuffers[sessionId]?.slice(0, 50)}...`,
      e,
    )
  }
}

const handleCompleteEvent = (data, updateMessage, jsonBuffers, sessionId) => {
  if (!data) return
  try {
    const result = JSON.parse(data)
    const currentSessionId = modelData.value.sessionId
    const stream = streamHolder.value[currentSessionId]
    if (!stream) return

    // 强制渲染最后内容
    updateMessage.flush()

    // 清理所有临时标记
    const cleanContent = stream.content
      .replace(/```\s*$/, '')
      .replace('（未完成）', '')
      .replace(/\$__INCOMPLETE_EQUATION__/g, '')

    // 更新消息为最终状态
    const msgIndex = messages.value.findIndex((m) => m.id === stream.id)
    if (msgIndex !== -1) {
      messages.value[msgIndex] = {
        id: result.messageId || stream.id,
        content: cleanContent,
        isUser: false,
        isStreaming: false,
      }
      // 同步会话元数据
      Object.assign(modelData.value, {
        sessionTitle: result.sessionTitle,
        messageType: result.messageType,
        sendTime: result.sendTime,
        updatedAt: result.updatedAt,
        lastMessageTime: result.lastMessageTime,
      })
    }

    // 清理资源
    delete jsonBuffers[sessionId]
    delete streamHolder.value[currentSessionId]

    nextTick(() => {
      scrollToBottom()
      // 移除残留的临时样式
      document.querySelectorAll('.incomplete').forEach((el) => el.classList.remove('incomplete'))
    })
  } catch (e) {
    delete jsonBuffers[sessionId]
    console.error('complete事件解析失败:', e)
  }
}

// 处理错误事件
const handleErrorEvent = (data, sessionId) => {
  if (!data) return
  try {
    const error = JSON.parse(data)
    console.error('流式请求错误:', error)
    messages.value.push({
      id: `error_${Date.now()}`,
      content: error.msg || '消息加载失败，请重试',
      isUser: false,
      isError: true,
      sessionId,
    })
    nextTick(scrollToBottom)
  } catch (e) {
    console.error('解析error事件失败:', e)
  }
}

// 消息发送相关
const sendMessage = async () => {
  if (isSending.value) return
  let sendingMessage = []
  const textContent = inputData.value.trim()
  const hasFiles = uploadedFiles.value.length > 0
  const hasText = textContent.length > 0
  if (!hasFiles && !hasText) return

  if (hasFiles && !filesSent.value) {
    const fileContent = uploadedFiles.value.map((file) => ({
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
    messages.value.push(fileMessage)
    sendingMessage.push(fileMessage)
    filesSent.value = true
  }

  const message = {
    id: Date.now() + '-text',
    content: textContent,
    isUser: true,
    type: 'text',
  }
  messages.value.push(message)
  sendingMessage.push(message)
  inputData.value = ''
  isSending.value = true

  modelData.value.newSession = isNewSession.value
  modelData.value.messageType = 1
  modelData.value.messageList = sendingMessage.map((msg) => ({
    content: msg.content,
    type: msg.type,
  }))
  modelData.value.isDeepThink = isDeepActive.value ? 1 : 0
  modelData.value.isNetworkSearch = isNetworkActive.value ? 1 : 0
  modelData.value.fileIds = uploadedFiles.value.map((f) => f.id).join(',')

  try {
    const newRes = await request('post', `/session/new`, modelData.value)
    if (newRes.code === 200) {
      setTimeout(() => {
        modelData.value.sessionId = newRes.data.sessionId
        modelData.value.sessionTitle = newRes.data.sessionTitle
        modelData.value.aiModelId = newRes.data.aiModelId
        modelData.value.isDeleted = newRes.data.isDeleted
        modelData.value.isPinned = newRes.data.isPinned
        modelData.value.isCollected = newRes.data.isCollected
        modelData.value.createdAt = newRes.data.createdAt
        modelData.value.sendTime = newRes.data.sendTime
        modelData.value.lastMessageTime = newRes.data.lastMessageTime
        renamingSessionId.value = newRes.data.sessionId

        nextTick(() => {
          messageContainerHeight.value = getElementHeight(messageContainerRef.value)
          scrollToBottom()
        })
      }, 100)

      // 调用流式消息接口
      await getStreamData(newRes.data)

      // 如果登录了就更新历史对话列表
      if (checkLoginStatus()) {
        // 从后端返回结果中获取完整的对话信息（而非前端临时数据）
        const serverSessionId = modelData.value.sessionId
        const serverSessionTitle = modelData.value.sessionTitle
        const serverUpdatedAt = modelData.value.updatedAt

        // 检查历史列表中是否已存在该对话
        const existingSessionIndex = sessionList.value.findIndex(
          (item) => item.id === serverSessionId,
        )

        if (existingSessionIndex !== -1) {
          // 已有对话：更新标题和时间（不新增）
          sessionList.value[existingSessionIndex] = {
            ...sessionList.value[existingSessionIndex],
            sessionTitle: serverSessionTitle,
            updatedAt: serverUpdatedAt,
          }
        } else {
          const newSessionData = {
            id: serverSessionId,
            userId: userStore.userId,
            sessionTitle: serverSessionTitle,
            updatedAt: serverUpdatedAt,
            isPinned: modelData.value.isPinned || 0,
            isCollected: modelData.value.isCollected || 0,
          }

          // 插入到置顶对话之后、普通对话之前
          const firstNonPinnedIndex = sessionList.value.findIndex((item) => item.isPinned === 0)
          const insertIndex =
            firstNonPinnedIndex !== -1 ? firstNonPinnedIndex : sessionList.value.length
          sessionList.value.splice(insertIndex, 0, newSessionData)
        }

        // 保存新对话ID到localStorage
        localStorage.setItem('currentSessionId', serverSessionId)
        // 高亮选中新对话
        selectedSessionId.value = serverSessionId
        isSessionExpanded.value = true
      }
    }
  } catch (error) {
    console.log(error)
  } finally {
    inputData.value = ''
    // 清空上传的文件
    uploadedFiles.value = []
    showAllFiles.value = false
    isSending.value = false
    isNewSession.value = false
    await nextTick(scrollToBottom)
  }
}

const handleKeydown = (e) => {
  if (e.keyCode === 13) {
    if (e.altKey || e.metaKey) {
      const cursorPos = e.target.selectionStart
      inputData.value = `${inputData.value.slice(0, cursorPos)}\n${inputData.value.slice(cursorPos)}`
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

// 对话标题编辑
const openTitleDialog = () => {
  isTitleDialogShow.value = true
  tempEditTitle.value = chatTitle.value
  nextTick(() => {
    setTimeout(() => {
      dialogInputRef.value?.focus()
    }, 0)
  })
}

// 右上角收藏功能
const collectChat = async () => {
  if (!checkLoginStatus()) {
    ElMessage.warning('请先登录，再收藏对话')
    return
  }
  if (messages.value.length <= 1) {
    ElMessage.warning('暂无对话内容，无法收藏')
    return
  }
  const sessionId = modelData.value.sessionId || localStorage.getItem('currentSessionId')
  if (!sessionId) {
    ElMessage.warning('对话尚未创建，请先发送一条消息')
    return
  }
  try {
    await toggleCollection(sessionId)
    ElMessage.closeAll()
  } catch (error) {
    console.log(error)
    ElMessage.error('收藏失败，请重试')
  }
}

// 用户菜单相关
const toggleUserMenu = () => {
  // 折叠状态下直接跳转，不展示菜单
  if (isCollapsed.value) {
    if (userStore.isLogin) {
      goToSettings() // 已登录跳个人中心
    } else {
      goToLogin() // 未登录跳登录页
    }
    return
  }
  // 非折叠状态保持原有菜单切换逻辑
  showUserMenu.value = !showUserMenu.value
  if (showUserMenu.value) {
    const avatarRect = avatarRef.value.getBoundingClientRect()
    const userMenuEl = document.querySelector('.user-menu')
    if (userMenuEl) {
      userMenuEl.style.left = `${avatarRect.left}px`
      userMenuEl.style.bottom = `${window.innerHeight - avatarRect.top}px`
    }
  }
}

const hideUserMenuOnOutsideClick = throttle((e) => {
  // 隐藏用户菜单
  if (showUserMenu.value && !e.target.closest('.user-profile') && !e.target.closest('.user-menu')) {
    showUserMenu.value = false
  }
  // 隐藏三点功能菜单
  if (
    activeSessionMenuId.value &&
    !e.target.closest('.session-item-menu') &&
    !e.target.closest('.menu-dots')
  ) {
    const activeItem = document.querySelector(`[data-session-id="${activeSessionMenuId.value}"]`)
    activeItem?.classList.remove('session-item-menu-active') // 移除激活类
    activeSessionMenuId.value = null
  }
  // 点击弹窗外部时
  if (isCollectionDialogShow.value && !e.target.closest('.collection-dialog')) {
    isCollectionDialogShow.value = false
  }
  // 点击所有文件弹窗外部时
  if (showAllFiles.value && !e.target.closest('.all-files-dialog')) {
    showAllFiles.value = false
  }
})

const goToLogin = () => {
  showUserMenu.value = false
  window.open('/login', '_blank')
}

const goToSettings = () => {
  showUserMenu.value = false
  window.open('/settings', '_blank')
}

// 文件上传相关方法
const handleFileUploadClick = () => {
  // 触发文件选择对话框
  fileInputRef.value.click()
}

// 多文件上传（并发控制）
const handleFileSelected = async (e) => {
  const files = e.target.files
  if (files.length > 0) {
    // 先添加到界面显示，再进行上传
    const newFiles = []
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const isDuplicate = uploadedFiles.value.some(
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
    const startIndex = uploadedFiles.value.length
    uploadedFiles.value.push(...newFiles)

    // 执行并发上传
    try {
      // 准备多文件上传的FormData
      const formData = new FormData()
      // 收集文件元数据列表
      const fileMetaList = newFiles.map((file) => ({
        fileName: file.name,
        fileSize: file.size,
        userId: userStore.userId || 0,
        sessionId: modelData.value.sessionId || 0,
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
            if (uploadedFiles.value[index]) {
              // 只有未完成的文件才更新进度
              if (!uploadedFiles.value[index].uploaded && !uploadedFiles.value[index].error) {
                uploadedFiles.value[index].progress = percent
                uploadedFiles.value[index].uploading = percent < 100
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
          if (uploadedFiles.value[index]) {
            uploadedFiles.value[index] = {
              ...uploadedFiles.value[index],
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
          modelData.value.fileIds = modelData.value.fileIds
            ? `${modelData.value.fileIds},${fileIds}`
            : fileIds
        }
      } else {
        ElMessage.error('文件上传失败')
      }
    } catch (error) {
      console.error('多文件上传失败:', error)
      ElMessage.error(`${error.msg || '未知错误'}`)
      // 更新文件状态为错误
      for (let i = 0; i < newFiles.length; i++) {
        const index = startIndex + i
        if (uploadedFiles.value[index]) {
          uploadedFiles.value[index].uploading = false
          uploadedFiles.value[index].error = true
          uploadedFiles.value[index].errorMessage = error.msg || '上传失败'
        }
      }
    }
    // 清空input值，允许重复选择同一文件
    e.target.value = ''
  }
}

const removeFile = (index) => {
  if (index < 0 || index >= uploadedFiles.value.length) {
    console.warn('无效的文件索引:', index)
    return
  }
  const file = uploadedFiles.value[index]
  if (file.isImage && file.previewUrl) {
    URL.revokeObjectURL(file.previewUrl)
  }
  uploadedFiles.value.splice(index, 1)
  if (uploadedFiles.value.length <= 6) {
    showAllFiles.value = false
  }
}

// 监听与生命周期
watch(
  messages,
  () => {
    nextTick(() => {
      messageContainerHeight.value = getElementHeight(messageContainerRef.value)
      checkScrollPosition()
    })
  },
  { deep: true },
)

// 监听上传文件数量变化，更新滚动按钮状态
watch(
  uploadedFiles,
  () => {
    nextTick(() => {
      const uploadedHeight = getUploadedFilesHeight()
      messageContainerHeight.value = getElementHeight(messageContainerRef.value) + uploadedHeight
      checkScrollPosition()
      // 动态调整消息容器底部内边距
      if (messageContainerRef.value) {
        messageContainerRef.value.style.paddingBottom = `calc(var(--input-height, 160px) + ${uploadedHeight}px)`
      }
    })
  },
  { deep: true },
)

watch(isCollapsed, () => {
  nextTick(() => {
    syncTopBarHeight()
    messageContainerHeight.value = getElementHeight(messageContainerRef.value)
    checkScrollPosition()
  })
})

// 监听用户ID变化，同步到modelData
watch(
  () => userStore?.userId,
  (newUserId) => {
    if (newUserId !== undefined && newUserId !== null) {
      modelData.value.userId = newUserId
      modelData.value.isLogin = !!newUserId
    }
  },
  { immediate: true },
)

// 监听登录状态变化，控制定时刷新
watch(
  () => userStore.isLogin,
  (isLogin) => {
    // 清除已有定时器
    if (sessionTimerRef.value) {
      clearInterval(sessionTimerRef.value)
      sessionTimerRef.value = null
    }

    // 如果已登录，启动定时刷新
    if (isLogin && userStore.userId > 0) {
      loadSessionList()
      sessionTimerRef.value = setInterval(() => {
        if (!isLoadingSession.value) {
          loadSessionList()
        }
      }, SESSION_REFRESH_INTERVAL)
    }
  },
  { immediate: true },
)

// 动态计算对话列表最大高度
const updateSessionListMaxHeight = () => {
  if (!isSessionExpanded.value) return

  const sessionList = document.querySelector('.session-list-container')
  const userProfile = document.querySelector('.user-profile')
  const leftContainer = document.querySelector('.left-container')
  if (!sessionList || !userProfile || !leftContainer) return

  // 基于左侧容器的定位计算（避免页面滚动影响）
  const leftRect = leftContainer.getBoundingClientRect()
  const listRect = sessionList.getBoundingClientRect()
  const profileRect = userProfile.getBoundingClientRect()

  // 计算列表最大高度 = 左侧容器高度 - 列表顶部到左侧容器顶部的距离 - 头像高度 - 预留间距
  const maxHeight = leftRect.height - (listRect.top - leftRect.top) - profileRect.height - 20

  // 应用计算结果（最小高度确保列表可用）
  sessionList.style.maxHeight = maxHeight > 50 ? `${maxHeight}px` : '200px'
  sessionList.style.overflowY = 'auto'
  sessionList.style.overflowX = 'hidden'
}

// 触发时机：列表展开/收起、DOM更新后、窗口大小变化
watch(isSessionExpanded, () => {
  nextTick(updateSessionListMaxHeight)
})
watch(isCollapsed, () => {
  // 延迟确保动画完成后计算
  setTimeout(() => {
    nextTick(updateSessionListMaxHeight)
  }, 300)
})

// 独立的异步初始化函数
const init = async () => {
  await nextTick()
  if (!isCompAlive.value) return
  // 初始化DOM尺寸相关逻辑
  syncTopBarHeight()
  updateSessionListMaxHeight()

  if (topBarRef.value) {
    topBarResizeObserver = new ResizeObserver(() => syncTopBarHeight())
    topBarResizeObserver.observe(topBarRef.value)
  }
  inputHeight.value = getElementHeight(inputContainerRef.value)
  messageContainerHeight.value = getElementHeight(messageContainerRef.value)
  scrollToBottom()
  checkScrollPosition()
  document.documentElement.style.setProperty('--input-height', `${inputHeight.value}px`)

  if (inputContainerRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        inputHeight.value = entry.contentRect.height
        messageContainerHeight.value = getElementHeight(messageContainerRef.value)
        checkScrollPosition()
        document.documentElement.style.setProperty('--input-height', `${inputHeight.value}px`)
      }
    })
    resizeObserver.observe(inputContainerRef.value)
  }

  // 恢复对话状态
  const needReloadSession = localStorage.getItem('needReloadSession')
  if (checkLoginStatus() && needReloadSession === 'true') {
    // 强制等待用户信息完全同步（避免用户ID延迟）
    while (!userStore.userId) {
      await new Promise((resolve) => setTimeout(resolve, 20))
    }
    // 加载历史对话列表，并明确等待响应式更新
    await loadSessionList()
    // 等待Vue响应式更新完成（确保sessionList已同步）
    await nextTick()
    const finalSessions = sessionList.value
    if (finalSessions.length === 0) {
      localStorage.removeItem('currentSessionId')
      await handleNewChat()
      return
    }
    const savedSessionId = localStorage.getItem('currentSessionId')
    const targetSession = finalSessions.find((item) => String(item.id) === savedSessionId)
    if (targetSession) {
      selectedSessionId.value = savedSessionId
      isSessionExpanded.value = true
      await loadSessionConversation(targetSession)
    } else {
      await handleNewChat()
    }
  } else {
    await handleNewChat()
  }
}

onMounted(() => {
  isCompAlive.value = true
  setUserInfo() // 同步用户信息
  nextTick(updateSessionListMaxHeight)
  window.addEventListener('resize', () => {
    nextTick(updateSessionListMaxHeight())
  })
  window.addEventListener('scroll', () => {
    nextTick(handleScroll)
    nextTick(updateSessionListMaxHeight)
  })

  init()
})

onBeforeUnmount(() => {
  isCompAlive.value = false
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', updateSessionListMaxHeight)
  // 清除定时刷新定时器
  if (sessionTimerRef.value) {
    clearInterval(sessionTimerRef.value)
    sessionTimerRef.value = null
  }
  clearTimeout(scrollTimer)
  if (topBarResizeObserver && topBarRef.value) {
    topBarResizeObserver.unobserve(topBarRef.value)
  }
  if (resizeObserver && inputContainerRef.value) {
    resizeObserver.unobserve(inputContainerRef.value)
  }
  if (resizeObserver) resizeObserver.disconnect()
  if (window.currentReader) {
    window.currentReader.cancel()
    window.currentReader = null
  }
  streamHolder.value = {}
  window.streamLoading = {}
  window.streamRetryCount = {}
})
</script>

<style scoped>
/* 基础样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  overflow-x: hidden;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.container {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* 左侧容器样式 */
.left-container {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 1010;
  background-color: #fff;
  width: 200px;
  border-right: 1px solid #e5e7eb;
  transition: width 0.2s ease;
  overflow: visible !important;
}

.left-container.collapsed {
  width: 60px;
}

/* 左侧顶部横栏 */
.left-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  border-bottom: 1px solid #f0f0f0;
  height: 50px;
}

.left-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.collapse-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.collapse-btn:hover {
  color: #1890ff;
  background-color: rgba(22, 163, 255, 0.05);
}

/* 菜单项样式 */
.menu-items {
  padding-top: 10px;
}

.session-list-container,
.menu-items {
  overflow: visible !important;
  clip: auto !important;
  clip-path: none !important;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 10px;
  margin: 4px 8px 4px;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

/* 不同菜单项的激活样式 */
.new-menu.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.collection-menu.active,
.session-menu.active {
  background-color: #e6f7ff;
  color: black;
}

.menu-icon {
  flex-shrink: 0;
}

.menu-text {
  margin-left: 10px;
  white-space: nowrap;
  color: #1e1e1e;
  font-size: 15px;
  padding: 2px 0;
}

.menu-divider {
  border: none;
  border-top: 1px solid #eee;
  margin: 8px 15px;
}

/* 加载状态 */
.loading-collection {
  text-align: center;
  padding: 20px;
  color: #666;
  font-size: 14px;
}

/* 收藏弹窗整体样式 */
::v-deep .collection-dialog {
  width: 800px !important;
  min-width: 400px !important;
  max-width: 60vw !important;
  max-height: 70vh !important;
  margin: -10% auto 0 !important;
  box-sizing: border-box !important;
  display: flex;
  flex-direction: column;
}

/* 表头样式 */
::v-deep .collection-table-header {
  padding: 0 16px;
  border-bottom: 1px solid #eee;
  background-color: #fafafa;
}

::v-deep .header-cell {
  padding: 10px 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

/* 表头列宽设置 */
::v-deep .header-cell.title-cell,
::v-deep .header-cell.note-cell {
  width: 30%;
}

::v-deep .header-cell.time-cell {
  width: 25%;
}

::v-deep .header-cell.action-cell {
  width: 15%;
}

::v-deep .table-header-row {
  display: flex;
  width: 100%;
}

/* 弹窗内容区域 */
::v-deep .collection-dialog .dialog-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
  max-height: calc(70vh - 100px);
}

/* 表格内容样式 */
::v-deep .collection-table-body {
  width: 100%;
}

::v-deep .table-row {
  display: flex;
  width: 100%;
  padding: 4px 0;
  align-items: center;
}

::v-deep .table-row:hover {
  background-color: #f5f7fa;
  border-radius: 8px;
}

::v-deep .table-cell {
  padding: 6px;
  font-size: 13px;
}

/* 内容列宽与表头对应 */
::v-deep .table-cell.title-cell {
  width: 30%;
  word-break: break-all;
}

::v-deep .table-cell.note-cell {
  width: 30%;
  word-break: break-all;
}

::v-deep .table-cell.time-cell {
  width: 25%;
  color: #666;
}

::v-deep .table-cell.action-cell {
  width: 15%;
  display: flex;
  gap: 8px;
}

::v-deep .dialog-pagination {
  padding: 4px 14px 18px 14px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff;
  border-radius: 0 0 12px 12px;
}

.collection-table th,
.collection-table td {
  padding: 6px 6px;
  font-size: 13px;
}

.collection-table th {
  background-color: #f5f7fa;
  font-weight: 500;
}

::v-deep .operation-btn {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

::v-deep .view-btn {
  background-color: #e6f7ff;
  color: #1890ff;
}

::v-deep .view-btn:hover {
  background-color: #bae7ff;
}

::v-deep .delete-btn {
  background-color: #fff2f0;
  color: #f5222d;
}

::v-deep .delete-btn:hover {
  background-color: #ffe3e0;
}

::v-deep .empty-tip {
  text-align: center;
  color: #999;
  padding: 60px 0;
}

/* 历史对话列表样式 */
.session-menu {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.expand-icon {
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.session-menu .expand-icon {
  margin-left: auto;
  margin-right: 10px;
}

.session-list-container {
  position: relative;
  padding: 4px 0;
  max-height: 0;
  transition: max-height 0.3s ease-out;
  scrollbar-width: thin;
  margin: 0 8px;
  border-bottom: 1px solid transparent;
  overflow: visible !important;
}

.session-list-container:not(.collapsing) {
  overflow-y: auto !important;
  overflow-x: hidden !important;
}

/* 滚动条显示（避免被遮挡） */
.session-list-container::-webkit-scrollbar,
.session-list::-webkit-scrollbar {
  width: 3px;
}

/* 滚动条滑块（可拖动部分） */
.session-list-container::-webkit-scrollbar-thumb,
.session-list::-webkit-scrollbar-thumb {
  background-color: rgba(140, 140, 140, 0.3);
  border-radius: 3px;
}

/* 滚动条轨道（背景）*/
.session-list-container::-webkit-scrollbar-track,
.session-list::-webkit-scrollbar-track {
  background: transparent;
}

.session-list {
  position: relative;
  overflow: visible !important;
}

/* 历史对话项内容容器 */
.session-item {
  position: relative;
  padding: 6px 16px 6px 22px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 6px;
  margin-bottom: 6px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: visible !important;
  z-index: 1;
}

.session-item:last-child {
  margin-bottom: 10px;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item:has(.session-item-menu.active) {
  pointer-events: none;
}

.session-item:has(.session-item-menu.active) .menu-dots {
  opacity: 1 !important;
}

/* 三点菜单按钮 */
.session-item .menu-dots {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 2px;
  margin-left: 8px;
  opacity: 0;
  transition: all 0.2s;
  z-index: 1021;
}

.session-item:hover .menu-dots {
  opacity: 1;
}

.session-item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

/* 选中状态样式 */
.session-item.session-item-selected {
  background-color: #e6f7ff !important;
  color: #1890ff !important;
  font-weight: 500;
}

.session-item.session-item-selected .menu-dots {
  opacity: 1 !important;
  color: #1890ff !important;
}

.session-title {
  font-size: 14px;
  line-height: 1.4;
}

.session-loading,
.session-empty {
  padding: 8px 12px;
  color: #999;
  font-size: 14px;
  text-align: center;
}

/* 历史对话项菜单样式 */
.session-item-menu {
  position: fixed;
  left: auto;
  right: 0;
  top: 100%;
  margin: -4px 4px 4px 0;
  width: 120px;
  min-width: 100px;
  max-height: none;
  background-color: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  border: 1px solid #eee;
  padding: 6px;
  animation: slideUp 0.2s ease forwards;
  transform-origin: top right;
  opacity: 0;
  transform: scaleY(0.9);
  clip-path: none !important;
  clip: auto !important;
  pointer-events: auto;
  z-index: 5000 !important;
  overflow: visible !important;
  display: block !important;
  contain: none;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: scaleY(0.9) translateY(-5px);
  }
  to {
    opacity: 1;
    transform: scaleY(1) translateY(0);
  }
}

.session-item:has(.session-item-menu) {
  background-color: inherit !important;
}

.session-item.session-item-selected:has(.session-item-menu) {
  background-color: #e6f7ff !important;
  color: #1890ff !important;
}

.session-item.session-item-selected:has(.session-item-menu.active) {
  background-color: #e6f7ff !important;
  color: #1890ff !important;
}

/* 保持菜单打开时的hover状态 */
.session-item.session-item-menu-active {
  background-color: #f5f7fa !important;
}

/* 修复选中状态下的样式冲突 */
.session-item.session-item-selected.session-item-menu-active {
  background-color: #e6f7ff !important;
}

.session-item-menu .menu-item {
  padding: 4px 18px;
  font-size: 13px;
  line-height: 1.1;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
  border-radius: 6px;
}

.session-item-menu .menu-item:hover {
  background-color: #f5f7fa;
}

/* 删除项红色样式 */
.session-item-menu .delete-item {
  color: #f5222d;
}

.session-item-menu .delete-item:hover {
  background-color: #fff2f0;
}

/* 置顶图标 */
.pin-icon {
  margin-left: 6px;
  color: #1890ff;
}

.pin-icon svg {
  width: 10px;
  height: 10px;
}

.pinned {
  color: black;
}

/* 折叠状态下隐藏历史列表 */
.left-container.collapsed .session-list-container,
.left-container.collapsed .expand-icon {
  display: none;
}

/* 底部用户信息 */
.user-profile {
  position: absolute;
  bottom: 15px;
  left: 0;
  right: 0;
  height: 50px;
  padding: 10px 15px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
  margin: 0 8px;
  border-radius: 10px;
  background-color: #fff;
  z-index: 20;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.03);
  pointer-events: auto;
  background-clip: padding-box;
}

.user-profile:hover {
  background-color: #f5f7fa;
  color: #1890ff;
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 8px;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 用户菜单 */
.user-menu {
  position: fixed;
  bottom: calc(15px + 50px + 2px);
  left: 8px;
  width: calc(200px - 16px);
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  border: 1px solid #eee;
  z-index: 1010;
  animation: slideUp 0.2s ease;
  margin-bottom: 2px;
}

.user-menu .menu-item {
  display: flex;
  align-items: center;
  padding: 6px 15px;
  color: #666;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  justify-content: flex-start;
  text-align: left;
}

.user-menu .menu-item:hover {
  background-color: #f5f7fa;
  color: #1890ff;
}

/* 折叠状态适配 */
.left-container.collapsed .user-menu {
  left: 8px;
}

.left-container.collapsed .user-menu .menu-item {
  justify-content: flex-start !important;
  padding-left: 15px;
}

.left-container.collapsed .menu-text,
.left-container.collapsed .left-title,
.left-container.collapsed .user-name {
  display: none;
}

.left-container.collapsed .menu-item,
.left-container.collapsed .user-profile {
  justify-content: center;
  padding: 10px 0;
}

.left-container.collapsed .user-avatar {
  margin-right: 0;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 右侧区域 */
.right-container {
  flex: 1;
  margin-left: 200px;
  width: calc(100% - 200px);
  transition: margin-left 0.2s ease;
  background-color: #fff;
}

.left-container.collapsed ~ .right-container {
  margin-left: 60px;
  width: calc(100% - 60px);
}

/* 顶部横栏 */
.top-bar {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  z-index: 1006;
  height: 50px;
  background-color: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  transition: left 0.2s ease;
}

.left-container.collapsed ~ .right-container .top-bar {
  left: 60px;
}

.top-bar-content {
  width: 100%;
  height: 100%;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-wrapper {
  cursor: pointer;
}

.chat-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  transition: all 0.2s ease;
}

.chat-title:hover {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.05);
  cursor: pointer;
}

.top-bar-btn {
  background: none;
  border: none;
  color: #666;
  font-size: 14px;
  padding: 4px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.top-bar-btn:hover {
  color: #1890ff;
  background-color: rgba(22, 163, 255, 0.05);
}

/* 对话区域 */
.chat-wrapper {
  width: 80%;
  padding: 60px 20px 20px;
  max-width: min(80%, 1200px);
  margin: 0 auto;
  position: relative;
}

.message-container {
  width: 100%;
  padding-bottom: calc(var(--input-height, 160px) + 20px);
}

.message-item {
  padding-top: 10px;
  margin-bottom: 12px;
  display: flex;
  transition: all 0.2s ease-out;
}

.msg-file-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
}

.msg-file-icon {
  margin-right: 2px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-file-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 4px;
  background-color: #f5f5f5;
  border-radius: 6px;
}

.msg-file-preview {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 2px;
}

.msg-file-info {
  flex: 1;
  min-width: 0;
}

.msg-file-name {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.msg-file-size {
  font-size: 10px;
  color: #666;
}

.message-bubble {
  max-width: 80%;
  border-radius: 8px;
  font-size: 14px;
  word-wrap: break-word;
  padding: 10px 14px;
  line-height: 1.6;
  white-space: normal;
  word-break: normal;
  overflow: visible !important;
}

.message-bubble-container {
  position: relative;
  max-width: 80%;
  border-radius: 8px;
  font-size: 14px;
  word-wrap: break-word;
  padding: 10px 14px;
  line-height: 1.6;
  white-space: normal;
  word-break: normal;
  overflow: visible !important;
}

.system-message {
  justify-content: flex-start;
}

.user-message {
  justify-content: flex-end;
}

.user-message .message-bubble {
  background-color: #e6f7ff;
  color: black;
}

.system-message .message-bubble {
  background-color: #fff;
  color: black;
}

.message-item.system-message.is-streaming .message-bubble {
  position: relative;
  padding-right: 24px;
}

/* 打字机效果动画 */
@keyframes typing {
  from {
    opacity: 0.6;
  }
  to {
    opacity: 1;
  }
}

.message-item.system-message.is-streaming .message-bubble > * {
  animation: typing 0.3s ease-in-out infinite alternate;
  will-change: opacity;
}

/* 加载指示器（三个点动画） */
.loading-indicator {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  gap: 2px;
}

.loading-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #666;
  animation: bounce 1.4s infinite ease-in-out both;
  will-change: transform;
}

.loading-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 消息内容动态增长时的平滑过渡 */
.message-bubble {
  transition:
    height 0.3s ease-out,
    min-height 0.3s ease-out,
    padding 0.3s ease-out,
    margin 0.3s ease-out;
}

/* 错误状态样式 */
.message-item.is-error .message-bubble {
  border: 1px dashed #ff4d4f;
  background-color: #fff8f8;
  color: #ff4d4f;
  cursor: pointer;
  padding: 12px 16px;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.1);
}

.message-item.is-error .message-bubble:hover {
  background-color: #fff1f1;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2);
}

/* 不完整代码块的提示样式 */
.incomplete-content {
  color: #888;
  font-style: italic;
  padding: 0 4px;
  border-radius: 2px;
  background-color: rgba(255, 255, 200, 0.3); /* 浅黄背景，突出未完成状态 */
}

/* 加载中提示 */
.loading-dot::after {
  content: '...';
  animation: dot 1.5s infinite;
  color: #666;
  margin-left: 4px;
  will-change: transform;
}

@keyframes dot {
  0% {
    content: '.';
  }
  50% {
    content: '..';
  }
  100% {
    content: '...';
  }
}

/* 确保流式状态关闭后，加载指示器和打字机动画被移除 */
.message-item.system-message:not(.is-streaming) .loading-indicator {
  display: none !important; /* 流结束后隐藏加载动画 */
}
.message-item.system-message:not(.is-streaming) .message-bubble > * {
  animation: none !important; /* 流结束后停止打字机动画 */
}
/* 清理残留的 incomplete-content 样式（兜底） */
.incomplete-content {
  display: none !important;
}

/* Markdown 标题样式（h1-h6） */
::v-deep .message-bubble h1,
::v-deep .message-bubble h2,
::v-deep .message-bubble h3,
::v-deep .message-bubble h4,
::v-deep .message-bubble h5,
::v-deep .message-bubble h6 {
  margin: 16px 0 8px;
  font-weight: 600;
  color: inherit;
}

::v-deep .message-bubble h1 {
  font-size: 1.5em;
}
::v-deep .message-bubble h2 {
  font-size: 1.3em;
}
::v-deep .message-bubble h3 {
  font-size: 1.1em;
}
::v-deep .message-bubble h4,
::v-deep .message-bubble h5,
::v-deep .message-bubble h6 {
  font-size: 1em;
}

/* 列表样式（有序/无序列表） */
::v-deep .message-bubble ul,
::v-deep .message-bubble ol {
  margin: 10px 0;
  padding-left: 24px; /* 缩进，区分列表项与正文 */
}

::v-deep .message-bubble ul {
  list-style-type: disc;
}
::v-deep .message-bubble ol {
  list-style-type: decimal;
}
::v-deep .message-bubble li {
  margin: 4px 0;
}

/* 链接样式 */
::v-deep .message-bubble a {
  color: #2563eb; /* 蓝色链接，符合常见习惯 */
  text-decoration: underline;
  word-break: break-all; /* 长链接自动换行 */
}

::v-deep .message-bubble a:hover {
  opacity: 0.8;
}

/* 代码块与行内代码样式 */
::v-deep .message-bubble pre {
  margin: 12px 0;
  border-radius: 4px;
  background-color: #f5f5f5; /* 深色背景，区分代码块 */
  color: #333; /* 深色文字，提高对比度 */
  overflow-x: auto; /* 横向滚动，避免代码溢出 */
  font-family: monospace; /* 等宽字体，符合代码展示习惯 */
  white-space: pre; /* 保留代码换行，避免强制折行 */
  /* 底部预留滚动条空间 */
  padding: 12px 12px 16px;
}

::v-deep .message-bubble code {
  padding: 2px 4px;
  border-radius: 2px;
  background-color: #f3f4f6;
  font-family: monospace;
  font-size: 0.9em;
  color: black;
}

::v-deep .message-bubble pre code {
  background: transparent; /* 代码块内的代码不重复添加背景 */
  padding: 0;
}

/* 引用样式 */
::v-deep .message-bubble blockquote {
  margin: 12px 0;
  padding-left: 12px;
  border-left: 3px solid #9ca3af; /* 左侧竖线，标识引用 */
  color: #6b7280; /* 浅色文字，区分引用与正文 */
  font-style: italic; /* 斜体，增强引用视觉效果 */
}

/* 图片样式 */
::v-deep .message-bubble img {
  max-width: 100%; /* 图片不超过容器宽度 */
  border-radius: 4px;
  margin: 8px 0;
  display: block; /* 图片单独占一行 */
}

/* 表格样式 */
::v-deep .message-bubble table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  border: 1px solid #ccc;
}

::v-deep .message-bubble th,
::v-deep .message-bubble td {
  padding: 8px 12px;
  border: 1px solid #f0f0f0;
}

::v-deep .message-bubble th {
  background-color: #f0f0f0; /* 浅灰色，更柔和 */
  color: #333; /* 加深文本色，提升对比度 */
  font-weight: 600;
}

/* 块级公式样式（居中显示，增加边距） */
::v-deep .message-bubble .katex-display {
  text-align: center;
  overflow: visible !important;
  height: auto !important;
  max-height: none !important;
  overflow-x: auto;
  padding: 0.5em 0;
  margin: 0.5em 0;
}

::v-deep .message-bubble .katex-display .katex {
  display: block !important;
  height: auto !important;
  overflow: visible !important;
}

/* 行内公式样式（与文本基线对齐） */
::v-deep .message-bubble .katex {
  font-size: 0.7em !important;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  min-height: 2em;
  vertical-align: middle;
  margin: 0.2em 0.4em;
  overflow: visible;
}

/* 公式字体大小适配 */
::v-deep .message-bubble .katex .base {
  line-height: 1.5 !important;
}

::v-deep .message-bubble .katex-display {
  /* 块级公式默认居中，确保不超出容器 */
  margin: 0.5em 0;
  max-width: 100%;
  overflow-x: auto;
  padding: 0.5em 0;
}

/* 避免公式溢出容器 */
::v-deep .message-bubble .katex-display > .katex {
  max-width: 100%;
  overflow-x: auto;
}

/* 输入区域 */
.input-container {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  z-index: 1000;
  background-color: #fff;
  padding: 4px 20px 10px;
  transition: left 0.2s ease;
}

.left-container.collapsed ~ .right-container .input-container {
  left: 60px;
}

.input-panel {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  padding: 12px;
  border: 1px solid #eee;
  max-width: min(80%, 1200px);
  margin: 0 auto;
  width: 80%;
}

/* 上传文件展示区样式 */
.uploaded-files-container {
  margin-bottom: 10px;
}

.uploaded-files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  background-color: #f5f7fa;
  border: 1px dashed #d9d9d9;
}

.uploaded-file-item {
  position: relative;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #eee;
  border-radius: 6px;
  margin-bottom: 8px;
  flex: 0 0 auto;
  min-width: 10%;
  max-width: 15%;
  box-sizing: border-box;
}

.file-info {
  flex: 1;
  min-width: 30px;
  margin-bottom: 5px;
  display: inline-block;
}

.file-icon {
  margin-right: 2px;
  color: #1890ff;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview {
  width: 24px;
  height: 24px;
  object-fit: cover;
  border-radius: 2px;
}

.file-name {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

/* 上传进度条样式 */
.upload-progress {
  height: 4px;
  background-color: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  width: 100%;
}

.upload-progress-bar {
  height: 100%;
  background-color: #1890ff;
  border-radius: 2px;
  transition: width 0.2s ease;
}

.file-name,
.upload-progress {
  margin-right: 0; /* 统一使用gap控制 */
}

.remove-file {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: rgba(255, 0, 0, 0.1);
  color: #ff4d4f;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 14px;
  line-height: 1;
}

.remove-file:hover {
  background-color: #ff4d4f;
  color: white;
}

/* 更多文件按钮 */
.more-files-btn {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background-color: rgba(255, 255, 255, 0.8);
  border: 1px solid #eee;
  border-radius: 4px;
  font-size: 12px;
  color: #1890ff;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  max-width: 10%;
  width: auto;
  height: auto;
  margin-bottom: 8px;
  box-sizing: border-box;
}

.more-files-btn:hover {
  background-color: #e6f7ff;
  border-color: #91d5ff;
}

.custom-input {
  width: 100%;
  min-height: 80px;
  max-height: 200px;
  border: none;
  resize: vertical;
  padding: 10px;
  font-size: 14px;
  line-height: 1.5;
  outline: none;
}

.custom-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #999;
  border-radius: 10px;
}

.button-group {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.left-buttons {
  display: flex;
  gap: 10px;
}

.deep-thinking,
.network-search,
.upload-file {
  padding: 4px 12px;
  border-radius: 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.deep-thinking:hover,
.network-search:hover,
.upload-file:hover {
  background-color: #e6f7ff;
}

.deep-thinking.active,
.network-search.active {
  background-color: #e6f7ff;
  color: #1890ff;
  border-color: #91d5ff;
}

/* 发送按钮 */
.send-btn {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #d9d9d9;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  padding: 0;
}

.send-btn:disabled {
  background-color: #d9d9d9 !important;
  cursor: not-allowed;
}

.send-btn.send-active {
  background-color: #1890ff;
}

.send-btn.send-active:hover {
  background-color: #096dd9;
}

/* 禁用状态按钮样式 */
.deep-thinking:disabled,
.network-search:disabled,
.upload-file:disabled {
  background-color: #f5f5f5;
  color: #999;
  border-color: #e8e8e8;
  cursor: not-allowed;
}

/* 禁用状态下hover不触发效果 */
.deep-thinking:disabled:hover,
.network-search:disabled:hover,
.upload-file:disabled:hover {
  background-color: #f5f5f5;
  border-color: #e8e8e8;
}

/* 滚动到底部按钮 */
.scroll-to-bottom {
  position: fixed;
  bottom: calc(var(--input-height, 160px) + 30px);
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 3001;
  opacity: 0;
  transition: all 0.2s;
}

.left-container.collapsed ~ .right-container .scroll-to-bottom {
  left: 50%;
}

.scroll-to-bottom.active {
  opacity: 1;
  visibility: visible;
}

/* 所有文件弹窗样式 */
::v-deep .all-files-dialog {
  width: 600px !important;
  max-width: 90vw !important;
  max-height: 70vh !important;
}

::v-deep .all-files-content {
  max-height: calc(70vh - 120px);
  overflow-y: auto;
}

.all-files-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.all-file-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  background-color: #f5f7fa;
  border: 1px solid #e8e8e8;
  max-width: 100%;
  box-sizing: border-box;
}

.all-file-item .file-size {
  margin-left: auto;
  margin-right: 10px;
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.all-file-item .remove-file {
  display: inline-block;
  color: #ff4d4f;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  position: relative;
}

/* 弹窗样式 */
::v-deep .dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1020 !important;
}

.dialog {
  width: 400px;
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.dialog-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-header h3 {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.dialog-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
}

.dialog-content {
  padding: 20px;
}

.title-dialog-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  outline: none;
}

.title-dialog-input:focus {
  border-color: #1890ff;
}

.dialog-hint {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.dialog-footer {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-btn {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.cancel {
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #666;
}

.confirm {
  border: none;
  background-color: #1890ff;
  color: #fff;
}
</style>
