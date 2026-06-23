import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/tasks',
      name: 'Tasks',
      component: () => import('../views/TasksView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, _, next) => {
  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn()

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    next('/tasks')
    return
  }

  next()
})

export default router