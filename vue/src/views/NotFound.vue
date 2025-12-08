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
import notFoundImage from '@/assets/images/404.png'

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
  display: flex;
  justify-content: center;
  align-items: flex-start;
  width: 100vw;
  height: 100vh;
  padding: 20px 20px 0;
  background-color: #ffffff;
  box-sizing: border-box;
}

/* 内容区 */
.not-found-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 96px;
  width: 100%;
  max-width: 1400px;
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
