import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/assets/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import markdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import hljs from 'highlight.js'
import 'highlight.js/lib/languages/latex'
import 'highlight.js/styles/github.css'
import 'katex/dist/katex.min.css'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 初始化 Markdown 解析器并配置插件
const md = markdownIt({
  html: true, // 允许解析 HTML 标签
  breaks: true, // 自动将换行符转换为 <br>
  linkify: true, // 自动识别链接
  typographer: true, // 优化排版
  highlight: function (str, lang) {
    if (lang === 'math') {
      lang = 'latex'
    }
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (err) {
        console.error(err)
      }
    }
    // 未指定语言时使用自动识别
    return hljs.highlightAuto(str).value
  },
}).use(markdownItKatex) // 启用公式渲染插件

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局挂载 Markdown 解析器
app.config.globalProperties.$md = md

// 挂载插件
app.use(router)
app.use(pinia)
app.use(ElementPlus, {
  size: 'small',
  zIndex: 1000,
  locale: zhCn,
})

// 挂载应用到DOM
app.mount('#app')
