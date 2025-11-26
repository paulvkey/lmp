<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <el-image :src="notFoundImage" alt="页面不存在" class="not-found-img" fit="contain" />
      <div class="not-found-tip">页面不存在或已被删除!</div>
      <el-button class="back-btn" @click="handleGoBack" icon="ArrowLeft"> 返回上一页 </el-button>
    </div>
  </div>
</template>

<script setup>
import notFoundImage from '@/assets/image/404.png'

// 返回上一页逻辑（无历史则跳首页）
const handleGoBack = () => {
  window.history.back()
  setTimeout(() => {
    if (window.history.length <= 1) {
      location.href = '/'
    }
  }, 100)
}
</script>

<style scoped>
/* 页面容器：全屏覆盖 + 垂直水平居中 */
.not-found-container {
  width: 100vw; /* 占满视口宽度 */
  height: 100vh; /* 占满视口高度（关键：实现垂直居中的前提） */
  background-color: #ffffff;
  display: flex;
  justify-content: center; /* 保持水平居中 */
  align-items: flex-start; /* 改为顶部对齐（关键） */
  padding: 20px 20px 0;
  box-sizing: border-box; /* 避免内边距撑大容器 */
}

/* 内容区 */
.not-found-content {
  display: flex;
  flex-direction: column; /* 子元素垂直排列 */
  align-items: center; /* 图片和文字依然水平居中 */
  gap: 96px; /* 适当减小元素间距，避免顶部太空 */
  width: 100%; /* 确保在小屏幕上也能水平居中 */
  max-width: 1400px; /* 限制最大宽度，避免图片过宽 */
}

/* 404图片 */
.not-found-img {
  max-width: 1400px;
  height: auto;
  margin: 0 auto;
}

/* 提示文本 */
.not-found-tip {
  font-size: 18px;
  color: #333;
  text-align: center;
}

/* 返回按钮*/
.back-btn {
  padding: 10px 16px;
  font-size: 16px;
  background-color: lightskyblue;
  color: black;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.back-btn:hover {
  background-color: deepskyblue;
}
</style>
