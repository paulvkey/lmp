<template>
  <!-- 左侧菜单顶部的内容展示 -->
  <div class="left-menu-info">
    <!-- 左侧顶部标题 -->
    <div class="left-title">对话平台</div>

    <!-- 左侧菜单栏 -->
    <div class="left-menus">
      <!-- 新对话菜单 -->
      <div class="new-chat menu-item" :class="{ active: true }" @click.stop="handleNewChat">
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
        <span class="left-menu-text">新对话</span>
      </div>

      <!-- 收藏对话菜单 -->
      <div
        class="collected-chat menu-item"
        :class="{ active: homeStatus.currentMenu === 'collection' }"
        @click.stop="handleCollection"
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
        <span class="left-menu-text">收藏对话</span>
      </div>
      <!-- 收藏对话详情弹窗 -->
      <CollectionBox :loadFunc="loadCollections" />

      <hr class="menu-divider" />

      <!-- 历史对话菜单 -->
      <div
        class="history-chats menu-item"
        :class="{ active: homeStatus.currentMenu === 'history' }"
        @click.stop="handleHistoryList"
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
        <span class="left-menu-text">历史对话</span>
        <svg
          class="expand-icon"
          :style="{ transform: history.isExpanded ? 'rotate(90deg)' : 'rotate(0)' }"
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
      <!-- 历史对话详情 -->
      <HistoryDetail
        :loadFunc="loadHistoryList"
        :collectionFunc="loadCollections"
        :newChatFunc="handleNewChat"
      />
    </div>
  </div>
</template>

<script setup>
import '@/assets/css/home/LeftMenu.css'
import { inject, nextTick, provide } from 'vue'
import { ElMessage } from 'element-plus'
import CollectionBox from '@/components/home/CollectionBox.vue'
import HistoryDetail from '@/components/home/HistoryDetail.vue'
import request from '@/utils/request.js'
import { checkLogin } from '@/utils/commonUtils.js'
import { useUserProfileStore } from '@/store/userProfile.js'
import { useHomeStatusStore } from '@/store/homeStatus.js'
import { useHistoryStore } from '@/store/history.js'
import { useCollectionStore } from '@/store/collection.js'

const userProfile = useUserProfileStore()
const homeStatus = useHomeStatusStore()
const history = useHistoryStore()
const collection = useCollectionStore()

const resetCurrentChat = inject("resetCurrentChat")

const SCROLL_DELAY = 200

/* 函数 */
// 新对话点击事件
const handleNewChat = async () => {
  // 重置当前对话窗口状态
  resetCurrentChat()
  // 切换菜单状态
  homeStatus.currentMenu = 'new'
  // 清除历史对话相关高亮
  history.selectedSessionId = null
  history.activeSessionMenuId = null
  history.needReloadSession = false
}

// 收藏对话点击事件
const handleCollection = async () => {
  if (!checkLogin(userProfile)) {
    ElMessage.warning('请先登录，再查看收藏的对话')
    return
  }
  if (homeStatus.currentMenu === 'history' || history.selectedSessionId) {
    // 重置当前对话状态
    resetCurrentChat()
    // 清除历史对话选中状态
    history.selectedSessionId = null
    history.loadedSessionId = null
  }
  history.currentMenu = 'collection'
  collection.pageInfo.pageNum = 1
  await loadCollections()
  if (collection.pageInfo.list.length === 0) {
    ElMessage.info('暂无收藏的对话')
    await handleNewChat()
  } else {
    collection.isDialogShow = true
  }
}

// 加载收藏列表数据
const loadCollections = async () => {
  collection.isLoading = true
  try {
    const response = await request('get', `/collection/user/${userProfile.userId}`, null, {
      params: { pageNum: collection.pageInfo.pageNum, pageSize: collection.pageInfo.pageSize },
    })
    // 同步所有分页状态
    collection.pageInfo = {
      list: response.data.list || [],
      total: response.data.total || 0,
      pageNum: response.data.pageNum || 1,
      pageSize: response.data.pageSize || 5,
      pages: response.data.pages || 0,
      hasPreviousPage: response.data.hasPreviousPage || false,
      hasNextPage: response.data.hasNextPage || false,
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    collection.clearPageInfo()
  } finally {
    collection.isLoading = false
  }
}

// 处理历史对话点击事件
const handleHistoryList = async () => {
  if (!checkLogin(userProfile)) {
    ElMessage.warning('请先登录，再查看历史对话')
    return
  }

  history.isExpanded = !history.isExpanded
  if (history.isExpanded && history.historyList.length === 0) {
    await loadHistoryList()
  }
}

// 加载历史对话列表数据
const loadHistoryList = async () => {
  const REQUEST_TIMEOUT = 6000; // 3秒请求超时

  // 重复调用处理（超时兜底）
  if (history.isLoading) {
    await new Promise((resolve) => {
      let timerCount = 0;
      const check = () => {
        // 超过2秒（20 * 100）强制resolve，避免永久等待
        if (timerCount >= 100) {
          resolve();
          history.isLoading = false;
          return;
        }
        if (!history.isLoading) {
          resolve();
        } else {
          timerCount++;
          setTimeout(check, 20);
        }
      };
      check();
    });
    return;
  }

  history.isLoading = true;
  try {
    const requestPromise = request('get', `/session/history/${userProfile.userId}`);
    // 超时竞争，避免请求卡死
    const response = await Promise.race([
      requestPromise,
      new Promise((_, reject) =>
        setTimeout(() => reject(new Error('请求超时')), REQUEST_TIMEOUT)
      )
    ]);

    history.historyList = response.data
      .map((item, index) => ({ indexId: `${item.id}_${index}`, ...item }))
      .sort((a, b) =>
        a.isPinned === b.isPinned
          ? new Date(b.updatedAt) - new Date(a.updatedAt)
          : b.isPinned - a.isPinned,
      );
    await nextTick();
  } catch (e) {
    ElMessage.error('加载历史对话失败，请稍后重试');
    history.historyList = [];
  } finally {
    history.isLoading = false;
  }
};

const doScroll = (targetElement, sessionContainer) => {
  const realScrollContainer = sessionContainer.querySelector('.history-list') || sessionContainer;
  // 计算目标元素在【真实滚动容器】中的相对位置
  const containerRect = realScrollContainer.getBoundingClientRect()
  const elementRect = targetElement.getBoundingClientRect()
  const relativeTop = elementRect.top - containerRect.top
  // 平滑滚动（增加边界判断，避免滚动值为负）
  realScrollContainer.scrollTo({
    top: Math.max(relativeTop - 10, 0),
    behavior: 'smooth'
  })
  // 强制高亮选中项
  targetElement.classList.add('history-item-selected')
}

// 左侧历史对话列表定位具体对话
const scrollToSessionItem = async (targetSessionId) => {
  if (!targetSessionId) {
    console.warn('缺少目标对话ID，无法定位对话项')
    return
  }

  try {
    homeStatus.currentMenu = 'history'
    history.isExpanded = true
    // 先高亮目标对话（视觉反馈优先）
    history.selectedSessionId = targetSessionId
    // 检查对话是否存在于当前列表中，不存在则重新加载
    let sessionExists = history.historyList.some((item) => item.id === targetSessionId)
    if (!sessionExists) {
      await loadHistoryList()
      await nextTick()
      sessionExists = history.historyList.some((item) => item.id === targetSessionId)
      // 重新设置选中状态（确保加载后高亮有效）
      history.selectedSessionId = sessionExists ? targetSessionId : null
    }

    // 如果对话存在，等待DOM更新后滚动到目标位置
    if (sessionExists) {
      await new Promise(resolve => setTimeout(resolve, SCROLL_DELAY))
      const targetElement = document.querySelector(`[data-session-id="${targetSessionId}"]`)
      // 先获取外层容器，再找真实滚动容器
      const sessionContainer = document.querySelector('.history-list-wrapper')
      if (!targetElement || !sessionContainer) {
        setTimeout(() => {
          const retryTarget = document.querySelector(`[data-session-id="${targetSessionId}"]`)
          const retryContainer = document.querySelector('.history-list-wrapper')
          if (retryTarget && retryContainer) {
            doScroll(retryTarget, retryContainer)
          }
        }, SCROLL_DELAY)
        return
      }
      doScroll(targetElement, sessionContainer)
    } else {
      console.warn(`未找到ID为${targetSessionId}的对话`)
    }
  } catch (e) {
    // 不处理
  }
}

provide("handleNewChat", handleNewChat)
provide("scrollToSessionItem", scrollToSessionItem)
</script>

<style scoped></style>
