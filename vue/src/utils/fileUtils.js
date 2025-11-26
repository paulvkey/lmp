// 获取图片宽高（返回Promise）
export const getImageDimensions = (file) => {
  return new Promise((resolve) => {
    // 只处理图片类型
    if (!file.type.startsWith('image/')) {
      resolve({isImage: false, width: 0, height: 0, preview: '' }) // 非图片返回0
      return
    }

    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        // naturalWidth/naturalHeight 是图片原始宽高（不受缩放影响）
        resolve({
          isImage: true,
          width: img.naturalWidth,
          height: img.naturalHeight,
          preview: URL.createObjectURL(file)
        })
      }
      img.onerror = () => {
        // 图片加载失败（如文件损坏）
        resolve({isImage: false, width: 0, height: 0, preview: '' })
      }
      img.src = e.target.result // 加载dataURL
    }
    reader.readAsDataURL(file) // 读取文件为dataURL
  })
}

// 格式化文件大小
export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
