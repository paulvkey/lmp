<template>
  <div class="container">
    <!-- 左侧菜单 -->
    <div class="left-menu">
      <!-- 左侧菜单顶部的内容展示 -->
      <div class="top-info">
        <!-- 左侧菜单栏 -->
        <div class="left-menus">
          <!-- 新对话菜单 -->
          <NewChat/>
          <!-- 收藏对话菜单 -->
          <div class="collected-chat"></div>
          <!-- 历史对话菜单 -->
          <div class="history-chats"></div>
          <!-- 历史对话详情 -->
          <div class="history-chat"></div>
        </div>
      </div>

      <!-- 左侧底部用户信息 -->
      <div class="user-profile">
        <!-- 左侧底部用户信息展示 -->
        <div class="user-info"></div>
        <!-- 左侧底部用户信息功能菜单 -->
        <div class="user-menus"></div>
      </div>
    </div>

    <!-- 右侧对话 -->
    <div class="right-chat">
      <!-- 顶不横栏 -->
      <div class="top-bar"></div>
      <!-- 对话区域 -->
      <div class="chat-wrapper">
        <!-- 对话展示区 -->
        <div class="message-wrapper"></div>
        <!-- 对话输入区 -->
        <div class="chat-input">
          <!-- 文件上传展示区 -->
          <div class="files-wrapper"></div>
          <!-- 内容输入区 -->
          <div class="text-input"></div>
          <!-- 按钮功能区 -->
          <div class="chat-btns"></div>
        </div>
        <!-- 滚动底部按钮 -->
        <div class="scroll-bottom"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import NewChat from '@/components/pages/NewChat.vue'

</script>

<style>

</style>
