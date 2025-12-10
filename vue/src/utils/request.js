import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// 环境变量适配（推荐用.env文件管理）
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090'
// 超时时间（可通过环境变量覆盖）
const TIMEOUT = import.meta.env.VITE_API_TIMEOUT ? Number(import.meta.env.VITE_API_TIMEOUT) : 10000
// 是否为生产环境
const isProd = import.meta.env.ENV === 'production'

// 创建axios实例
const instance = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

// 存储请求取消令牌（用于取消重复请求）
const requestCancelTokens = new Map()

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    // 确保config和headers存在
    if (!config) config = {}
    if (!config.headers) config.headers = {}

    // 智能处理Content-Type：FormData自动适配
    if (config.data instanceof FormData) {
      // 移除手动设置的Content-Type，让浏览器自动添加（包含boundary）
      delete config.headers['Content-Type']
    } else if (!('Content-Type' in config.headers)) {
      // 仅当未设置时才用默认值
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }

    // 取消重复请求
    const requestKey = `${config.method}-${config.url}`
    // 取消之前未完成的同类型请求
    if (requestCancelTokens.has(requestKey)) {
      requestCancelTokens.get(requestKey).cancel('重复请求已取消')
      requestCancelTokens.delete(requestKey)
    }
    // 创建新的取消令牌
    const source = axios.CancelToken.source()
    config.cancelToken = source.token
    requestCancelTokens.set(requestKey, source)

    return config
  },
  (error) => {
    ElMessage.error('请求配置异常，请稍后重试')
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    // 移除已完成请求的取消令牌
    const requestKey = `${response.config.method}-${response.config.url}`
    requestCancelTokens.delete(requestKey)

    // 处理响应数据：安全解析JSON字符串
    let res = response.data
    try {
      if (typeof res === 'string') {
        res = res ? JSON.parse(res) : res
      }
    } catch (e) {
      !isProd && console.error('响应数据JSON解析失败：', e)
      console.warn('响应数据格式异常')
      return res
    }

    // 业务码统一处理
    if (res.code !== undefined && res.code !== 200) {
      console.error(res.msg || '业务请求失败')
      return Promise.reject(res)
    }

    return res
  },
  (error) => {
    // 移除失败请求的取消令牌
    if (error.config) {
      const requestKey = `${error.config.method}-${error.config.url}`
      requestCancelTokens.delete(requestKey)
    }

    // 统一错误处理：区分不同错误类型
    let errorMsg = ''
    if (axios.isCancel(error)) {
      // 取消请求的错误（不提示用户）
      !isProd && console.warn('请求已取消：', error.message)
      return Promise.reject(error)
    } else if (!error.response) {
      // 网络错误/超时
      errorMsg = error.message.includes('timeout') ? '请求超时，请检查网络' : '网络异常，请检查连接'
      ElMessage.error(errorMsg)
    } else {
      // HTTP状态码错误
      const status = error.response.status
      switch (status) {
        case 401:
          // 登录过期：弹出登录提示（示例）
          ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning',
          }).then(() => {
            // 跳转到登录页
            window.location.href = '/login'
          })
          errorMsg = '登录状态已过期'
          break
        case 403:
          errorMsg = '暂无权限访问该资源'
          break
        case 404:
          errorMsg = '请求的接口不存在'
          break
        case 500:
          errorMsg = '服务器内部错误，请稍后重试'
          break
        default:
          errorMsg = `请求失败（${status}）：${error.response.data?.msg || '未知错误'}`
      }
      console.error(errorMsg)
      !isProd && console.error(`HTTP错误[${status}]：`, error)
    }

    return Promise.reject(error)
  }
)

// 统一请求封装
const request = async (method, url, data = null, config = {}) => {
  try {
    // 方法容错：确保为小写字符串，默认get
    const reqMethod = (method && method.toLowerCase()) || 'get'

    // 基础配置（核心字段不允许被覆盖）
    const baseConfig = {
      method: reqMethod,
      url,
      // 继承实例的超时时间，用户可通过config覆盖
      timeout: TIMEOUT,
    }

    // 区分请求方法处理参数
    if (reqMethod === 'get') {
      baseConfig.params = data
    } else if (['post', 'put', 'delete', 'patch'].includes(reqMethod)) {
      baseConfig.data = data
    }

    // 安全合并配置：用户配置仅覆盖非核心字段（优先级：config > baseConfig > 实例默认）
    const finalConfig = {
      ...baseConfig,
      ...config,
      // 确保method/url不被用户配置覆盖
      method: baseConfig.method,
      url: baseConfig.url,
      // 合并headers（用户headers优先级更高）
      headers: {
        ...baseConfig.headers,
        ...config.headers,
      },
    }

    // 发送请求
    return await instance(finalConfig)
  } catch (error) {
    !isProd && console.error(`[${method}] ${url} 请求失败：`, error)
    throw error
  }
}

// 导出取消所有请求的方法（可选，如页面卸载时调用）
export const cancelAllRequests = () => {
  requestCancelTokens.forEach((source) => {
    source.cancel('页面卸载，取消所有请求')
  })
  requestCancelTokens.clear()
}

export default request
