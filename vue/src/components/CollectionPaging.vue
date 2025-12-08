<template>
  <!-- 仅在有数据且非加载状态时显示 -->
  <div class="pagination-container" v-if="visible">
    <!-- 自定义每页显示条数选择器 -->
    <div class="page-size-selector">
      <span>每页显示</span>
      <div class="custom-select" :class="{ disabled }" @click.stop="toggleOptions" ref="selectRef">
        <!-- 选中值展示 -->
        <div class="select-value">{{ currentPageSize }} 条</div>
        <!-- 下拉箭头 -->
        <div class="select-arrow" :class="{ active: showOptions }">
          <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" width="12" height="12">
            <path
              d="M20,50 L50,80 L80,50"
              stroke="#606266"
              stroke-width="4"
              fill="none"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </div>
        <!-- 自定义选项列表（向上弹窗优先） -->
        <div
          class="select-options"
          v-if="showOptions"
          :style="optionsStyle"
          ref="optionsRef"
          @click.stop
        >
          <div
            class="option-item"
            v-for="size in pageSizes"
            :key="size"
            :class="{ selected: size === currentPageSize }"
            @click="handleCustomSizeChange(size)"
          >
            {{ size }} 条
          </div>
        </div>
      </div>
    </div>

    <!-- 分页导航 -->
    <div class="page-navigation">
      <button
        class="page-btn"
        @click="handlePrevious"
        :disabled="!collection.pageInfo.hasPreviousPage || disabled"
      >
        上一页
      </button>
      <span class="page-info">
        第 {{ collection.pageInfo.pageNum }} 页 / 共 {{ collection.pageInfo.pages }} 页 （总计
        {{ collection.pageInfo.total }} 条）
      </span>
      <button
        class="page-btn"
        @click="handleNext"
        :disabled="!collection.pageInfo.hasNextPage || disabled"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, defineEmits, defineProps, nextTick, onMounted, ref, watch } from 'vue'
import { useCollectionStore } from '@/store/collection.js'

const collection = useCollectionStore()

// 定义接收的 props
const props = defineProps({
  pageSizes: { type: Array, default: () => [5, 10] },
  visible: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['page-size-change', 'page-num-change'])

// 自定义下拉框状态
const currentPageSize = ref(collection.pageInfo.pageSize)
const showOptions = ref(false)
const optionsRef = ref(null)
const selectRef = ref(null)

// 切换选项框显示状态
const toggleOptions = () => {
  if (props.disabled) return
  showOptions.value = !showOptions.value
}

// 监听 props 中 pageSize 变化
watch(
  () => collection.pageInfo.pageSize,
  (newVal) => {
    currentPageSize.value = newVal
  },
  { immediate: true },
)

// 动态生成选项列表的样式（固定向上）
const optionsStyle = computed(() => ({
  top: 'auto',
  bottom: 'calc(100% + 4px)',
  margin: 0,
}))

// 选择每页条数
const handleCustomSizeChange = (size) => {
  currentPageSize.value = size
  emit('page-size-change', size)
  showOptions.value = false
}

// 上一页/下一页
const handlePrevious = () => {
  emit('page-num-change', collection.pageInfo.pageNum - 1)
}
const handleNext = () => {
  emit('page-num-change', collection.pageInfo.pageNum + 1)
}

onMounted(() => {
  // 使用箭头函数确保this指向正确
  const handleClickOutside = (e) => {
    // 如果弹窗未显示，直接返回
    if (!showOptions.value) return
    // 确保元素已渲染
    nextTick(() => {
      const selectEl = selectRef.value
      const optionsEl = optionsRef.value
      // 双重保险：判断元素是否存在且点击目标是否在内部
      const isInSelect = selectEl && selectEl.contains(e.target)
      const isInOptions = optionsEl && optionsEl.contains(e.target)
      // 点击外部且元素存在时关闭
      if (!isInSelect && !isInOptions) {
        showOptions.value = false
      }
    })
  }

  // 使用捕获阶段监听（避免被冒泡阻止）
  document.addEventListener('click', handleClickOutside, true)
  // 组件卸载时清理事件
  return () => {
    document.removeEventListener('click', handleClickOutside, true)
  }
})
</script>

<style scoped>
/* 向上弹窗样式 */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding: 0 6px;
  position: relative;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.custom-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background-color: #fff;
  cursor: pointer;
  min-width: 70px;
}

.pagination-container,
.page-size-selector,
.custom-select,
.page-info {
  font-size: 13px;
}

.custom-select.disabled {
  cursor: not-allowed;
  background-color: #f5f5f5;
  border-color: #d9d9d9;
  color: #909399;
}

.select-value {
  flex: 1;
}

.select-arrow {
  margin-left: 2px;
  margin-top: 2px;
  transition: transform 0.2s ease;
  transform: rotate(180deg);
}

.select-arrow.active {
  margin-bottom: 4px;
  transform: rotate(360deg);
}

/* 下拉选项列表 */
.select-options {
  position: absolute;
  left: 0;
  width: 100%;
  padding: 2px 0;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  z-index: 2000;
  overflow: visible;
  clip: auto;
}

/* 单个选项样式 */
.option-item {
  padding: 2px 12px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 6px;
  margin: 2px 4px;
}

.option-item:hover {
  background-color: #e6f7ff;
  color: #1890ff;
}

.option-item.selected {
  background-color: #1890ff;
  color: #fff;
}

/* 分页导航样式 */
.page-navigation {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  padding: 4px 8px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;
  font-size: 12px;
}

.page-btn:hover:not(:disabled) {
  background-color: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.page-btn:disabled {
  cursor: not-allowed;
  color: #909399;
  background-color: #f5f5f5;
  border-color: #d9d9d9;
}

.page-info {
  color: #606266;
}
</style>
