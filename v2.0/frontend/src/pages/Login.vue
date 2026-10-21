<template>
  <div class="min-h-full grid grid-cols-1 lg:grid-cols-2">
    <!-- 左侧品牌区 -->
    <div class="hidden lg:flex flex-col justify-center px-16 bg-gradient-to-br from-brand-700 to-brand-500 text-white">
      <h1 class="text-4xl font-bold mb-4">学情预警系统</h1>
      <p class="text-white/80 leading-relaxed text-lg">
        四端统一入口：教师发布 → 学生提交 → 教师批改 → 数据入库，<br />
        在学期进行中提前发现学业风险。
      </p>
      <div class="mt-10 space-y-2 text-white/70 text-sm">
        <p>· 成绩构成是课程的总开关，决定活动与学生可见页签</p>
        <p>· 缺课超过三分之一直接挂科，迟到与请假只扣分</p>
        <p>· 教师、导员、教务、学生四端数据范围严格隔离</p>
      </div>
      <p class="mt-10 text-sm text-white/50">v2.0 · 学生 / 教师 / 辅导员 / 教务</p>
    </div>

    <!-- 右侧登录区 -->
    <div class="flex items-center justify-center px-6 py-10">
      <div class="w-full max-w-[420px]">
        <p class="text-xl text-slate-500">欢迎使用</p>
        <h2 class="text-4xl font-bold text-slate-800 mt-1 mb-8 tracking-wide">学情预警系统</h2>

        <form @submit.prevent="submit" class="space-y-4">
          <div>
            <label class="block text-base text-slate-600 mb-1.5">登录号</label>
            <input
              v-model="loginNo"
              class="w-full h-12 px-4 rounded-lg border border-slate-300 text-base outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
              placeholder="学号 / 工号 / 教务账号"
              autocomplete="username" />
          </div>

          <div>
            <label class="block text-base text-slate-600 mb-1.5">密码</label>
            <input
              v-model="password"
              type="password"
              class="w-full h-12 px-4 rounded-lg border border-slate-300 text-base outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
              placeholder="初始密码 000000"
              autocomplete="current-password" />
          </div>

          <div>
            <label class="block text-base text-slate-600 mb-1.5">验证码</label>
            <div class="flex items-center gap-3">
              <input
                v-model="captchaCode"
                class="flex-1 h-12 px-4 rounded-lg border border-slate-300 text-base uppercase tracking-widest outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
                placeholder="请输入右侧 4 位验证码"
                maxlength="4"
                autocomplete="off" />
              <button type="button"
                      class="h-12 w-[132px] shrink-0 rounded-lg border border-slate-300 overflow-hidden bg-slate-50 flex items-center justify-center"
                      title="看不清？点击刷新"
                      @click="refreshCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="w-full h-full object-cover" />
                <span v-else class="text-xs text-slate-400">点击获取</span>
              </button>
            </div>
          </div>

          <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

          <button type="submit"
                  class="w-full h-12 rounded-lg bg-brand-500 text-white text-base font-medium hover:bg-brand-600 disabled:opacity-60"
                  :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>
        </form>

        <div class="mt-8 text-sm text-slate-500 leading-relaxed space-y-1">
          <p class="text-slate-600 font-medium">登录提示：</p>
          <p>1. 教师 / 导员账号为工号（示例 1001 / C001），教务账号为 admin001，学生账号为学号（示例 20250001）。</p>
          <p>2. 首次登录后请点击右上角头像修改密码。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { homeOf, saveSession } from '../session'

const router = useRouter()
const loginNo = ref('')
const password = ref('000000')
const captchaCode = ref('')
const captchaId = ref('')
const captchaImage = ref('')
const loading = ref(false)
const error = ref('')

async function refreshCaptcha() {
  captchaCode.value = ''
  try {
    const data = await authApi.captcha()
    if (data) {
      captchaId.value = data.captchaId
      captchaImage.value = data.image
    }
  } catch (e) {
    captchaImage.value = ''
    error.value = '验证码获取失败，请检查后端是否已启动'
  }
}

async function submit() {
  error.value = ''
  if (!loginNo.value.trim()) { error.value = '请输入登录号'; return }
  if (!captchaCode.value.trim()) { error.value = '请输入验证码'; return }
  loading.value = true
  try {
    const vo = await authApi.login(loginNo.value.trim(), password.value, captchaId.value, captchaCode.value.trim())
    saveSession(vo)
    router.push(homeOf(vo.role))
  } catch (e) {
    error.value = e.message || '登录失败'
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>
