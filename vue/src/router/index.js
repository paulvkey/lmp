import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'root',
      redirect: '/home',
    },
    {
      path: '/home',
      name: 'home',
      meta: { title: '首页' },
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      meta: { title: '登录' },
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      meta: { title: '注册' },
      component: () => import('../views/RegisterView.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      meta: { title: '个人中心' },
      component: () => import('../views/SettingView.vue'),
    },
    {
      path: '/:pathMatch(.*)*', // 匹配所有未定义路由
      name: '404',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.afterEach((to) => {
  document.title = to.meta.title
  return true
})

export default router
