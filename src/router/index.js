import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import UserList from '../views/UserList.vue'
import PhotoGallery from '../views/PhotoGallery.vue'
import CourseList from '../views/CourseList.vue'

import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/users', name: 'UserList', component: UserList },
  { path: '/photos', name: 'PhotoGallery', component: PhotoGallery },
  { path: '/courses', name: 'CourseList', component: CourseList }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 路由守卫：未登录时只允许访问登录页和注册页，其余一律回到登录页
router.beforeEach((to, from, next) => {
  const loginUser = localStorage.getItem('loginUser')
  if (to.path !== '/login' && to.path !== '/register' && !loginUser) {
    // 直接输入地址访问受保护页面时，也给出提示再回到登录页
    ElMessage.warning('请先登录')
    next('/login')
  } else {
    next()
  }
})

export default router
