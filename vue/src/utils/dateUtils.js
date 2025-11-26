export const formatTimeDay = (timeStr) => {
  try {
    if (!timeStr) return ''
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
  } catch (error) {
    console.error('格式化时间失败：', error)
    return '解析时间异常'
  }
}

export const formatTimeSecond = (timeStr) => {
  try {
    if (!timeStr) return ''
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  } catch (error) {
    console.error('格式化时间失败：', error)
    return '解析时间异常'
  }
}
