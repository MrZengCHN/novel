import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/UserCenterView.vue'),
    },
    {
      path: '/admin/home-manage',
      name: 'home-manage',
      component: () => import('../views/HomeManageView.vue'),
      beforeEnter: (to, from, next) => {
        const { useUserStore } = import('@/stores/userStore').then(m => m)
        // Dynamic import logic inside guard is tricky for pinia outside component?
        // Actually, we can import useUserStore at top level if pinia is installed.
        // But better: use dynamic import or just standard import.
        // Let's use standard import at top.
        // But wait, Pinia store must be used after Pinia is installed. Router is installed before app mount?
        // Yes, but route guard executes when route is visited (app is mounted).
        import('@/stores/userStore').then(({ useUserStore }) => {
          const userStore = useUserStore()
          if (userStore.userInfo && userStore.userInfo.role === 'ADMIN') {
            next()
          } else {
            next('/')
          }
        })
      }
    },
  ],
})

export default router
