<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth.js'
import { useCaptcha } from '../../composables/useCaptcha.js'
import { postJson } from '../../api/http.js'

const router = useRouter()
const { setStudentLogin, isStudentLoggedIn } = useAuth()
const { captchaCode, captchaInput, generateCode, drawCaptcha, refreshCaptcha, verifyCaptcha } = useCaptcha()

const studentNo = ref('')
const studentPassword = ref('')
const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const captchaCanvas = ref(null)

onMounted(() => {
  if (isStudentLoggedIn.value) { router.replace('/student/info'); return }
  generateCode()
  nextTick(() => drawCaptcha(captchaCanvas.value))
})

function doRefresh() { refreshCaptcha(captchaCanvas) }
function setMsg(t, type = 'info') { message.value = t; messageType.value = type }

async function handleLogin() {
  const err = verifyCaptcha()
  if (err) { setMsg(err, 'error'); doRefresh(); return }
  loading.value = true; setMsg('')
  try {
    const res = await postJson('/student/login', { studentNo: studentNo.value, password: studentPassword.value })
    if (!res.success) { setMsg(res.message || '登录失败', 'error'); doRefresh(); return }
    setStudentLogin(res.data.token, res.data)
    setMsg(`欢迎，${res.data?.studentName || res.data?.studentNo}`, 'success')
    router.push('/student/info')
  } catch { setMsg('无法连接后端', 'error') }
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
        <p class="login-welcome">👨‍🎓 学生端</p>
        <p class="login-desc">查看学情、接收预警、完成练习，做学习的主人</p>
        <div class="login-features">
          <div class="login-feat"><span>📊</span> 实时学情透视</div>
          <div class="login-feat"><span>🚨</span> 预警及时提醒</div>
          <div class="login-feat"><span>📝</span> 在线完成练习</div>
          <div class="login-feat"><span>🎯</span> 设定学习目标</div>
        </div>
      </div>
      <div class="login-panel-bot"><span>v2.0 · 努力，让未来更美好</span></div>
    </div>
    <div class="login-split-right">
      <div class="login-header">
        <div class="login-icon">👨‍🎓</div>
        <h2>学生登录</h2>
      </div>
      <form @submit.prevent="handleLogin">
        <div class="input-group"><label>学号</label><input v-model="studentNo" placeholder="请输入学号" required /></div>
        <div class="input-group"><label>密码</label><input v-model="studentPassword" type="password" placeholder="请输入密码" required /></div>
        <div class="input-group captcha-group">
          <label>验证码</label>
          <div class="captcha-wrapper">
            <input v-model="captchaInput" type="text" class="captcha-input" placeholder="请输入验证码" maxlength="4" required />
            <canvas ref="captchaCanvas" class="captcha-canvas" @click="doRefresh"></canvas>
          </div>
        </div>
        <button type="submit" class="btn-primary" :disabled="loading" style="width:100%;">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>
      <button class="btn-back-landing" @click="router.push('/')">← 返回首页</button>
    </div>
  </section>
</template>

<style scoped>
/* StudentLogin — 完全继承全局 style.css 中的 .login-split / .login-split-left / .login-split-right 等 */
</style>
