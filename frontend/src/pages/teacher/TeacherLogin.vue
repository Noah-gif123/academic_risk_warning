<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth.js'
import { useCaptcha } from '../../composables/useCaptcha.js'
import { postJson, getJson } from '../../api/http.js'

const router = useRouter()
const { setTeacherLogin, isTeacherLoggedIn, teacherToken } = useAuth()
const { captchaCode, captchaInput, generateCode, drawCaptcha, refreshCaptcha, verifyCaptcha } = useCaptcha()

const teacherNo = ref('')
const password = ref('')
const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const captchaCanvas = ref(null)

onMounted(() => {
  if (isTeacherLoggedIn.value) { router.replace('/teacher/courses'); return }
  generateCode()
  nextTick(() => drawCaptcha(captchaCanvas.value))
})

function doRefreshCaptcha() {
  refreshCaptcha(captchaCanvas)
}

function setMessage(text, type = 'info') { message.value = text; messageType.value = type }

async function handleLogin() {
  const err = verifyCaptcha()
  if (err) { setMessage(err, 'error'); doRefreshCaptcha(); return }

  loading.value = true
  setMessage('')
  try {
    const res = await postJson('/teacher/login', { teacherNo: teacherNo.value, password: password.value })
    if (!res.success) { setMessage(res.message || '登录失败', 'error'); doRefreshCaptcha(); return }
    setTeacherLogin(res.token, res.data)
    setMessage(`欢迎，${res.data?.teacherName || res.data?.teacherNo}`, 'success')
    router.push('/teacher/courses')
  } catch { setMessage('无法连接后端，请确认 Spring Boot 已在 8080 端口启动', 'error') }
  finally { loading.value = false }
}
</script>

<template>
  <section class="login-split">
    <div class="login-split-left">
      <div class="login-panel-top">
        <div class="login-brand-icon">🎓</div>
        <h1>学情预警系统</h1>
        <p class="login-brand-sub">Academic Risk Warning System</p>
      </div>
      <div class="login-panel-mid">
        <p class="login-welcome">👨‍🏫 教师工作台</p>
        <p class="login-desc">实时掌握学生学业动态，精准识别风险，及时干预</p>
        <div class="login-features">
          <div class="login-feat"><span>📊</span> 多维学情分析，自动化评估</div>
          <div class="login-feat"><span>🚨</span> 红·橙·黄三级预警机制</div>
          <div class="login-feat"><span>🔄</span> 预警处理闭环管理</div>
          <div class="login-feat"><span>📋</span> 日报周报自动汇总</div>
        </div>
      </div>
      <div class="login-panel-bot"><span>v2.0 · 数据驱动的学业风险管理平台</span></div>
    </div>
    <div class="login-split-right">
      <div class="login-header">
        <div class="login-icon">👨‍🏫</div>
        <h2>教师登录</h2>
      </div>
      <form @submit.prevent="handleLogin">
        <div class="input-group"><label>工号</label><input v-model="teacherNo" placeholder="请输入工号" required /></div>
        <div class="input-group"><label>密码</label><input v-model="password" type="password" placeholder="请输入密码" required /></div>
        <div class="input-group captcha-group">
          <label>验证码</label>
          <div class="captcha-wrapper">
            <input v-model="captchaInput" type="text" class="captcha-input" placeholder="请输入验证码" maxlength="4" required />
            <canvas ref="captchaCanvas" class="captcha-canvas" @click="doRefreshCaptcha"></canvas>
          </div>
        </div>
        <button type="submit" class="btn-primary" :disabled="loading" style="width:100%;">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>
      <button class="btn-back-landing" @click="router.push('/')">← 返回首页</button>
    </div>
    <p v-if="message" class="msg" :class="messageType" style="position:absolute;bottom:20px;left:50%;transform:translateX(-50%);">{{ message }}</p>
  </section>
</template>
