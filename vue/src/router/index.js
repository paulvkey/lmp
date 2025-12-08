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
      component: () => import('../views/HomePage.vue'),
    },
    {
      path: '/login',
      name: 'login',
      meta: { title: '登录' },
      component: () => import('../views/UserLogin.vue'),
    },
    {
      path: '/register',
      name: 'register',
      meta: { title: '注册' },
      component: () => import('../views/UserRegister.vue'),
    },
    {
      path: '/user-profile',
      name: 'user-profile',
      meta: { title: '个人中心' },
      component: () => import('../views/UserProfile.vue'),
    },
    {
      path: '/:pathMatch(.*)*', // 匹配所有未定义路由
      name: '404',
      component: () => import('../views/NotFound.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

router.afterEach((to) => {
  document.title = to.meta.title
  return true
})

export default router
