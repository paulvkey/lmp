<template>
  <teleport to="body">
    <div
      class="dialog-cover files-dialog-wrapper"
      v-if="chat.showAllFiles"
      @click="chat.showAllFiles = false"
    >
      <div class="files-dialog" @click.stop>
        <div class="files-dialog-header">
          <h3>所有文件 ({{ chat.uploadedFiles.length }})</h3>
          <button class="files-dialog-close" @click="chat.showAllFiles = false">×</button>
        </div>
        <div class="files-dialog-content">
          <div class="files-list">
            <div
              class="files-item"
              v-for="(file, index) in chat.uploadedFiles"
              :key="`${file.name}-${file.size}-${index}`"
              :title="file.name"
            >
              <span class="file-icon">
                <img
                  v-if="file.previewUrl"
                  :src="file.previewUrl"
                  :alt="file.name"
                  class="file-image-preview"
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
                class="files-remove-file"
                @click.stop="removeFile(index)"
                :title="`移除 ${file.name}`"
              >
                ×
              </button>
            </div>
          </div>
        </div>
        <div class="files-dialog-footer">
          <button class="files-dialog-btn confirm" @click="chat.showAllFiles = false">
            关闭
          </button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import '@/assets/css/home/UploadFilesBox.css'
import { formatFileSize } from '@/utils/fileUtils.js'
import { useChatStore } from '@/store/chat.js'

const chat = useChatStore()

const removeFile = (index) => {
  if (index < 0 || index >= chat.uploadedFiles.length) {
    console.warn('无效的文件索引:', index)
    return
  }
  const file = chat.uploadedFiles[index]
  if (file.isImage && file.previewUrl) {
    URL.revokeObjectURL(file.previewUrl)
  }
  chat.uploadedFiles.splice(index, 1)
  if (chat.uploadedFiles.length <= 6) {
    chat.showAllFiles = false
  }
}
</script>

<style scoped></style>
