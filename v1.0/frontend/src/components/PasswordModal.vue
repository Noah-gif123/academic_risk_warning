<script setup>
import { ref } from 'vue'
import { useAuth } from '../composables/useAuth.js'
import { postJson } from '../api/http.js'

const { identity, teacherToken, studentToken } = useAuth()

const emit = defineEmits(['close'])
const profileTab = ref('info')
const pwdOld = ref('')
const pwdNew = ref('')
const pwdConfirm = ref('')
const pwdMessage = ref('')
const pwdMessageType = ref('info')
const pwdLoading = ref(false)

function setPwdMessage(text, type = 'info') {
  pwdMessage.value = text
  pwdMessageType.value = type
}

async function handleChangePassword() {
  if (!pwdOld.value) { setPwdMessage('请输入旧密码', 'error'); return }
  if (!pwdNew.value) { setPwdMessage('请输入新密码', 'error'); return }
  if (pwdNew.value.length < 6) { setPwdMessage('新密码长度不能少于6位', 'error'); return }
  if (pwdNew.value !== pwdConfirm.value) { setPwdMessage('两次输入的新密码不一致', 'error'); return }
  const confirmed = confirm('确定要修改密码吗？此操作不可撤销。')
  if (!confirmed) return

  pwdLoading.value = true
  setPwdMessage('')
  try {
    const path = identity.value === 'teacher' ? '/teacher/update-password' : '/student/update-password'
    const token = identity.value === 'teacher' ? teacherToken.value : studentToken.value
    const res = await postJson(path, { oldPassword: pwdOld.value, newPassword: pwdNew.value }, token)
    if (!res.success) { setPwdMessage(res.message || '修改失败', 'error'); return }
    setPwdMessage('密码修改成功！', 'success')
    pwdOld.value = ''; pwdNew.value = ''; pwdConfirm.value = ''
    setTimeout(() => emit('close'), 1500)
  } catch { setPwdMessage('请求失败，请检查网络', 'error') }
  finally { pwdLoading.value = false }
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card" style="max-width:480px;">
      <h3>个人中心</h3>
      <div class="profile-tabs">
        <button :class="{ active: profileTab === 'info' }" @click="profileTab = 'info'">个人信息</button>
        <button :class="{ active: profileTab === 'password' }" @click="profileTab = 'password'">修改密码</button>
      </div>

      <div v-if="profileTab === 'password'" style="margin-top:16px;">
        <div class="input-group"><label>旧密码</label><input v-model="pwdOld" type="password" placeholder="请输入旧密码" /></div>
        <div class="input-group"><label>新密码</label><input v-model="pwdNew" type="password" placeholder="请输入新密码（至少6位）" /></div>
        <div class="input-group"><label>确认密码</label><input v-model="pwdConfirm" type="password" placeholder="请再次输入新密码" /></div>
        <p v-if="pwdMessage" class="msg" :class="pwdMessageType">{{ pwdMessage }}</p>
        <button class="btn-primary" :disabled="pwdLoading" @click="handleChangePassword" style="width:100%;">
          {{ pwdLoading ? '修改中...' : '确认修改' }}
        </button>
      </div>

      <button class="btn-ghost" style="margin-top:16px;" @click="emit('close')">关闭</button>
    </div>
  </div>
</template>

<style scoped>
/* PasswordModal — 继承全局 .modal-overlay/.modal-card/.input-group/.btn-primary/.btn-ghost */
.modal-card h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.profile-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--border-light);
  margin-bottom: 16px;
}

.profile-tabs button {
  flex: 1;
  padding: 10px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-tertiary);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.profile-tabs button.active {
  color: var(--accent-primary);
  border-bottom-color: var(--accent-primary);
}

.profile-tabs button:hover { color: var(--text-primary); }

.form-row { display: flex; gap: 12px; }
.form-row .input-group { flex: 1; }

.avatar-upload { margin-bottom: 16px; display: flex; align-items: center; gap: 12px; }
.avatar-upload .avatar-preview {
  width: 64px; height: 64px; border-radius: 50%;
  background: var(--accent-light); display: flex; align-items: center; justify-content: center;
  font-size: 28px; border: 2px solid var(--border-light);
}
</style>
