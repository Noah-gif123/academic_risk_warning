<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/counselor/home')">← 返回看板</button>
        <h1 class="text-lg font-semibold text-slate-800">个人中心</h1>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="card p-4 space-y-2" v-if="p">
        <h2 class="font-medium text-slate-700 mb-2">基本信息</h2>
        <div class="text-sm space-y-1">
          <p><span class="text-slate-400 w-24 inline-block">姓名</span>{{ p.counselorName }}</p>
          <p><span class="text-slate-400 w-24 inline-block">工号</span>{{ p.counselorNo }}</p>
          <p><span class="text-slate-400 w-24 inline-block">登录号</span>{{ p.accountNo }}</p>
          <p><span class="text-slate-400 w-24 inline-block">学院</span>{{ p.collegeName || '—' }}</p>
          <p><span class="text-slate-400 w-24 inline-block">负责班级</span>{{ p.classCount }} 个</p>
          <p><span class="text-slate-400 w-24 inline-block">手机</span>{{ p.phone || '—' }}</p>
          <p><span class="text-slate-400 w-24 inline-block">邮箱</span>{{ p.email || '—' }}</p>
        </div>
      </div>

      <div class="card p-4 space-y-3">
        <h2 class="font-medium text-slate-700">修改密码</h2>
        <p class="text-xs text-slate-400">初始密码 000000，新密码 6~32 位。</p>
        <label class="text-sm block">原密码
          <input v-model="pwd.oldPassword" type="password" class="input mt-1" />
        </label>
        <label class="text-sm block">新密码
          <input v-model="pwd.newPassword" type="password" class="input mt-1" />
        </label>
        <label class="text-sm block">再输一次
          <input v-model="pwd.confirm" type="password" class="input mt-1" />
        </label>
        <button class="btn-primary text-sm" @click="changePwd">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { counselorApi } from '../../api/counselor'
import { authApi } from '../../api/auth'

const p = ref(null)
const message = ref('')
const ok = ref(true)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) { toast('请填写原密码与新密码', false); return }
  if (pwd.newPassword !== pwd.confirm) { toast('两次输入的新密码不一致', false); return }
  if (pwd.newPassword.length < 6 || pwd.newPassword.length > 32) { toast('新密码长度需 6~32 位', false); return }
  try {
    await authApi.changePassword(pwd.oldPassword, pwd.newPassword)
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
    toast('密码已修改')
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { p.value = await counselorApi.profile() }
  catch (e) { toast(e.message, false) }
})
</script>
