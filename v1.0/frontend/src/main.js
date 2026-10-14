import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
app.use(router)
app.mount('#app')

// 页面标题同步
router.afterEach((to) => {
  document.title = to.meta.title || '学情预警系统'
})
