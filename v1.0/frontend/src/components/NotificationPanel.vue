<script setup>
import { ref, computed } from 'vue'
import { useAuth } from '../composables/useAuth.js'
import { getNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead } from '../api/http.js'

const { teacherToken, studentToken } = useAuth()
const emit = defineEmits(['close'])

const notifications = ref([])
const unreadCount = ref(0)
const notifyFilter = ref('all')

function activeUser() {
  if (studentToken.value) return { token: studentToken.value, type: 'STUDENT' }
  return { token: teacherToken.value, type: 'TEACHER' }
}

async function loadNotifications() {
  const u = activeUser()
  if (!u.token) return
  try {
    const [listRes, countRes] = await Promise.all([
      getNotifications(u.token, u.type),
      getUnreadCount(u.token, u.type)
    ])
    if (listRes.success) notifications.value = listRes.data || []
    if (countRes.success) unreadCount.value = countRes.data?.count || 0
  } catch { /* ignore */ }
}

async function readNotification(id) {
  const u = activeUser()
  await markNotificationRead(u.token, id, u.type)
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  const n = notifications.value.find(n => n.id === id)
  if (n) n.isRead = true
}

async function readAll() {
  const u = activeUser()
  await markAllNotificationsRead(u.token, u.type)
  unreadCount.value = 0
  notifications.value.forEach(n => n.isRead = true)
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function notificationTypeLabel(t) {
  const map = { ALERT_NEW: '新预警', ALERT_UPGRADE: '等级升级', HANDLE_RESULT: '已处理',
    HANDLE_RESULT_EXERCISE: '布置练习', STUDENT_RESPOND: '学生回应',
    DAILY_SUMMARY: '日报', WEEKLY_SUMMARY: '周报' }
  return map[t] || '系统通知'
}

const filteredNotifications = computed(() => {
  if (notifyFilter.value === 'all') return notifications.value
  if (notifyFilter.value === 'alert') return notifications.value.filter(n => n.type?.startsWith('ALERT_') || n.type === 'ALERT_UPGRADE' || n.type === 'STUDENT_RESPOND')
  if (notifyFilter.value === 'grade') return notifications.value.filter(n => n.type === 'HANDLE_RESULT' || n.type === 'HANDLE_RESULT_EXERCISE')
  return notifications.value.filter(n => !['ALERT_NEW','ALERT_UPGRADE','HANDLE_RESULT','HANDLE_RESULT_EXERCISE','STUDENT_RESPOND'].includes(n.type))
})

loadNotifications()
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card" style="max-width:560px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h3>消息通知</h3>
        <button class="btn-ghost" @click="readAll" v-if="notifications.length">全部已读</button>
      </div>

      <div style="display:flex;gap:8px;margin:12px 0;">
        <button class="btn-ghost" :class="{ active: notifyFilter === 'all' }" @click="notifyFilter = 'all'">全部</button>
        <button class="btn-ghost" :class="{ active: notifyFilter === 'alert' }" @click="notifyFilter = 'alert'">预警</button>
        <button class="btn-ghost" :class="{ active: notifyFilter === 'grade' }" @click="notifyFilter = 'grade'">处理</button>
        <button class="btn-ghost" :class="{ active: notifyFilter === 'system' }" @click="notifyFilter = 'system'">系统</button>
      </div>

      <div v-if="!filteredNotifications.length" style="text-align:center;padding:24px;color:var(--text-secondary);">
        暂无消息
      </div>

      <div v-for="n in filteredNotifications" :key="n.id"
           class="notif-item" :class="{ unread: !n.isRead }" @click="readNotification(n.id)">
        <div class="notif-type">{{ notificationTypeLabel(n.type) }}</div>
        <div class="notif-title">{{ n.title }}</div>
        <div class="notif-content">{{ n.content }}</div>
        <div class="notif-time">{{ formatTime(n.createTime) }}</div>
      </div>

      <button class="btn-ghost" style="margin-top:12px;width:100%;" @click="emit('close')">关闭</button>
    </div>
  </div>
</template>

<style scoped>
.notif-item { padding: 10px 12px; border-bottom: 1px solid var(--border-color); cursor: pointer; border-radius: 6px; }
.notif-item:hover { background: var(--bg-hover); }
.notif-item.unread { background: #eff6ff; border-left: 3px solid var(--primary); }
.notif-type { font-size: 11px; color: var(--text-secondary); font-weight: 600; }
.notif-title { font-weight: 600; margin: 2px 0; }
.notif-content { font-size: 13px; color: var(--text-secondary); }
.notif-time { font-size: 11px; color: var(--text-tertiary); margin-top: 4px; }
</style>
