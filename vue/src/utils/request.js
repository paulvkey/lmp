import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例（带默认配置）
const instance = axios.create({
  baseURL: 'http://localhost:8090', // 基础路径
  timeout: 3000, // 超时时间
  headers: {
    'Content-Type': 'application/json;charset=UTF-8', // 默认请求头
  },
})

// 请求拦截器（统一处理请求配置）
instance.interceptors.request.use(
  (config) => {
    if (!config) config = {}
    if (!config.headers) config.headers = {}
    // 仅当 headers 中完全没有 Content-Type 时才设置默认值
    if (!('Content-Type' in config.headers)) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8'
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器（统一处理响应和错误）
instance.interceptors.response.use(
  (response) => {
    // 处理响应数据（如字符串转JSON）
    let res = response.data
    if (typeof res === 'string') {
      res = res ? JSON.parse(res) : res
    }
    return res // 返回处理后的数据
  },
  (error) => {
    // 统一错误提示（避免在封装函数中重复处理）
    if (error.response && error.response.status === 404) {
      ElMessage.error('未找到请求接口！')
    } else if (error.response && error.response.status === 500) {
      console.log(error)
    } else {
      ElMessage.error(error.message || '请求失败，请稍后重试')
    }
    return Promise.reject(error)
  },
)

// 统一请求封装
const request = async (method, url, data = null, config = {}) => {
  try {
    // 合并基础配置和用户传入的配置（用户配置优先级更高）
    const baseConfig = {
      method: method.toLowerCase(),
      url,
    }
    // 根据请求方法处理数据参数
    if (baseConfig.method === 'get') {
      baseConfig.params = data
    } else {
      baseConfig.data = data
    }
    // 合并用户传入的配置（如headers、onUploadProgress等）
    const finalConfig = { ...baseConfig, ...config }
    // 发送请求
    return await instance(finalConfig)
  } catch (error) {
    console.error(`[${method}] ${url} 请求失败:`, error)
    throw error
  }
}

export default request
