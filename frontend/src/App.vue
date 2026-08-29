<script setup>
import { onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { initAuth, useAuth } from './composables/useAuth.js'
import AppLayout from './components/AppLayout.vue'
import StudentLayout from './components/StudentLayout.vue'
import './style.css'

const router = useRouter()
const route = useRoute()
const { isLoggedIn, isTeacherLoggedIn, isStudentLoggedIn, isAdminLoggedIn, identity, switchIdentity, clearAllAuth } = useAuth()

onMounted(() => initAuth())

function goToLogin() {
  const loginRoutes = { teacher: '/teacher/login', student: '/student/login', admin: '/admin/login' }
  router.push(loginRoutes[identity.value] || '/teacher/login')
}

function handleLogout() {
  clearAllAuth()
  router.push('/')
}

function handleSelectMenu(key) { /* 子组件处理 */ }
</script>

<template>
  <!-- ===== 首页（始终展示，不套任何布局） ===== -->
  <div v-if="route.path === '/'" class="landing-page">
    <header class="landing-nav">
      <div class="landing-nav-left">
        <div class="landing-logo">🎓</div>
        <span class="landing-title">学情预警系统</span>
      </div>
      <div style="display:flex;gap:12px;" v-if="!isLoggedIn">
        <button class="btn-ghost" @click="switchIdentity('teacher'); goToLogin()">👨‍🏫 教师端</button>
        <button class="btn-ghost" @click="switchIdentity('student'); goToLogin()">👨‍🎓 学生端</button>
        <button class="btn-ghost" @click="switchIdentity('admin'); goToLogin()">🛡️ 管理员</button>
      </div>
      <div style="display:flex;gap:12px;align-items:center;" v-else>
        <span style="font-size:14px;color:var(--text-secondary);">👋 欢迎回来</span>
        <button class="btn-ghost" @click="handleLogout" style="color:#ef4444;border-color:#fecaca;">退出登录</button>
      </div>
    </header>

    <section class="landing-hero">
      <div class="hero-text">
        <h1>智能学情预警，精准干预</h1>
        <p class="hero-subtitle">基于多维度数据，实时守护每一位学生的学业成长</p>
      </div>
      <div class="hero-visual">
        <div class="hero-chart-icon">📊</div>
        <div class="hero-rings"><span></span><span></span><span></span></div>
      </div>
    </section>

    <section class="landing-cards">
      <div class="landing-card"><div class="landing-card-icon">📊</div><h3>多维分析</h3><p>综合成绩、签到、作业、考试等多维度数据，全面洞察学生学习状态</p></div>
      <div class="landing-card"><div class="landing-card-icon">🚨</div><h3>实时预警</h3><p>红·橙·黄三级预警体系，自动判定风险等级，及时推送通知</p></div>
      <div class="landing-card"><div class="landing-card-icon">📝</div><h3>精准干预</h3><p>教师处理反馈、学生确认回复，形成学业帮扶闭环管理</p></div>
    </section>

    <section class="landing-about">
      <h2>关于学情预警系统</h2>
      <div class="about-content">
        <p>学情预警系统是一款面向高校的学业风险管理平台，通过数据驱动帮助教师及时发现学习困难学生，并提供精准的学业干预建议。系统采用红·橙·黄三级预警机制，自动判定风险等级，预警信息自动推送给教师和学生。</p>
        <p class="about-footer">—— 让每一位学生都不被落下</p>
      </div>
    </section>

    <footer class="landing-footer">
      <p>© {{ new Date().getFullYear() }} 学情预警系统 · 智慧教育，守护成长</p>
    </footer>
  </div>

  <!-- ===== 学生端（无侧边栏布局） ===== -->
  <StudentLayout v-else-if="isStudentLoggedIn">
    <router-view />
  </StudentLayout>

  <!-- ===== 教师 / 管理员端（带侧边栏布局） ===== -->
  <AppLayout v-else-if="isTeacherLoggedIn || isAdminLoggedIn" @select-menu="handleSelectMenu" @logout="handleLogout">
    <router-view />
  </AppLayout>

  <!-- ===== 未登录页面（登录页等）直接渲染 ===== -->
  <router-view v-else />
</template>

<style scoped>
/* ================================================================
   Landing Page 首页样式
   ================================================================ */
.landing-page {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4ff 0%, #f8fafc 50%, #eef2ff 100%);
  color: var(--text-primary, #0f172a);
  font-family: 'PingFang SC', 'Microsoft YaHei', -apple-system, sans-serif;
  overflow-x: hidden;
}

/* --- 导航栏 --- */
.landing-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 48px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.landing-nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.landing-logo {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.landing-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
  letter-spacing: -0.5px;
}

.btn-ghost {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary, #334155);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-ghost:hover {
  color: var(--accent-primary, #2563eb);
  background: #fff;
  border-color: var(--accent-primary, #2563eb);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}

/* --- Hero 区域 --- */
.landing-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 48px 60px;
  gap: 60px;
}

.hero-text {
  flex: 1;
  max-width: 600px;
}

.hero-text h1 {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.15;
  margin-bottom: 20px;
  color: var(--text-primary, #0f172a);
  letter-spacing: -1px;
}

.hero-text h1::after {
  content: '';
  display: block;
  width: 80px;
  height: 5px;
  margin-top: 20px;
  background: linear-gradient(90deg, #2563eb, #22c55e);
  border-radius: 3px;
}

.hero-subtitle {
  font-size: 20px;
  line-height: 1.6;
  color: var(--text-secondary, #334155);
  max-width: 520px;
}

.hero-visual {
  position: relative;
  width: 320px;
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-chart-icon {
  position: relative;
  z-index: 2;
  width: 140px;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 32px;
  box-shadow: 0 20px 50px rgba(37, 99, 235, 0.2);
  animation: float 4s ease-in-out infinite;
}

.hero-rings {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-rings span {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(37, 99, 235, 0.15);
  animation: ripple 3s ease-out infinite;
}

.hero-rings span:nth-child(1) { width: 180px; height: 180px; animation-delay: 0s; }
.hero-rings span:nth-child(2) { width: 240px; height: 240px; animation-delay: 0.6s; }
.hero-rings span:nth-child(3) { width: 300px; height: 300px; animation-delay: 1.2s; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

@keyframes ripple {
  0% { transform: scale(0.8); opacity: 0.6; }
  100% { transform: scale(1.3); opacity: 0; }
}

/* --- 功能卡片 --- */
.landing-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
  max-width: 1200px;
  margin: 0 auto 80px;
  padding: 0 48px;
}

.landing-card {
  padding: 36px 28px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.25s ease;
}

.landing-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.2);
}

.landing-card-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-bottom: 20px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
}

.landing-card h3 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary, #0f172a);
}

.landing-card p {
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-secondary, #334155);
}

/* --- 关于系统 --- */
.landing-about {
  max-width: 1200px;
  margin: 0 auto 80px;
  padding: 48px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.landing-about h2 {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 24px;
  color: var(--text-primary, #0f172a);
  text-align: center;
}

.about-content p {
  font-size: 16px;
  line-height: 1.9;
  color: var(--text-secondary, #334155);
  margin-bottom: 16px;
  text-align: justify;
}

.about-footer {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: var(--accent-primary, #2563eb);
  margin-top: 24px;
}

/* --- 页脚 --- */
.landing-footer {
  padding: 28px 48px;
  text-align: center;
  background: rgba(255, 255, 255, 0.7);
  border-top: 1px solid rgba(226, 232, 240, 0.8);
  color: var(--text-tertiary, #64748b);
  font-size: 14px;
}

/* --- 响应式 --- */
@media (max-width: 1024px) {
  .landing-hero {
    flex-direction: column;
    text-align: center;
    padding: 60px 32px 40px;
  }

  .hero-text h1::after {
    margin: 20px auto 0;
  }

  .hero-visual {
    width: 260px;
    height: 260px;
  }

  .landing-cards {
    grid-template-columns: repeat(2, 1fr);
    padding: 0 32px;
  }
}

@media (max-width: 768px) {
  .landing-nav {
    padding: 14px 20px;
  }

  .landing-nav-left {
    gap: 8px;
  }

  .landing-title {
    font-size: 18px;
  }

  .btn-ghost {
    padding: 8px 14px;
    font-size: 13px;
  }

  .landing-hero {
    padding: 40px 20px 32px;
  }

  .hero-text h1 {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .hero-visual {
    width: 200px;
    height: 200px;
  }

  .hero-chart-icon {
    width: 100px;
    height: 100px;
    font-size: 48px;
    border-radius: 24px;
  }

  .landing-cards {
    grid-template-columns: 1fr;
    padding: 0 20px;
    gap: 20px;
    margin-bottom: 48px;
  }

  .landing-card {
    padding: 28px 22px;
  }

  .landing-about {
    padding: 32px 24px;
    margin: 0 20px 48px;
  }

  .landing-footer {
    padding: 20px;
  }
}
</style>
