<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div>
        <h1 class="text-lg font-semibold text-slate-800">消息中心</h1>
        <p class="text-xs text-slate-400 mt-1">
          这里是与<b>你本人</b>相关的消息（按接收人投递）：预警、待办、归档与问卷通知。未读条目左侧有红点，点击即标记已读。
        </p>
      </div>
      <div class="flex items-center gap-3">
        <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
        <button class="text-xs text-brand-600 hover:underline" @click="load">刷新</button>
        <button class="text-xs text-brand-600 hover:underline disabled:text-slate-300"
                :disabled="!unread" @click="readAll">全部标为已读</button>
      </div>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between flex-wrap gap-2 mb-3">
        <div class="flex items-center gap-1">
          <button v-for="t in tabs" :key="t.key"
                  class="px-3 py-1.5 rounded-lg text-sm"
                  :class="tab === t.key ? 'bg-brand-50 text-brand-600 font-medium' : 'text-slate-600 hover:bg-slate-50'"
                  @click="switchTab(t.key)">
            {{ t.text }}<span v-if="t.key === 'unread' && unread" class="ml-1 text-red-600">({{ unread }})</span>
          </button>
        </div>
        <p class="text-xs text-slate-400">
          共 {{ total }} 条<span v-if="unread"> · 未读 {{ unread }} 条</span>
        </p>
      </div>

      <div v-for="n in list" :key="n.id"
           class="border-b border-slate-100 py-3 flex items-start gap-3"
           :class="n.isRead ? '' : 'bg-slate-50/60'">
        <span class="w-2 h-2 rounded-full mt-2 shrink-0" :class="n.isRead ? 'bg-slate-200' : 'bg-red-500'"></span>
        <div class="flex-1 min-w-0 space-y-1">
          <div class="flex items-center gap-2 flex-wrap">
            <p class="text-sm text-slate-800" :class="n.isRead ? '' : 'font-medium'">{{ n.title }}</p>
            <span class="tag bg-slate-100 text-slate-500">{{ bizLabel(n.bizType) }}</span>
            <span v-if="n.aggregateCount > 1" class="tag bg-brand-50 text-brand-600">汇总 {{ n.aggregateCount }} 条</span>
            <span v-if="n.priority === 'HIGH'" class="tag bg-red-50 text-red-600">高优先级</span>
            <span v-if="n.ackRequired && n.ackStatus === 'PENDING'" class="tag bg-amber-50 text-amber-600">待回执</span>
            <span class="text-xs text-slate-400">{{ fmt(n.sendTime) }}</span>
          </div>
          <p class="text-sm text-slate-600 whitespace-pre-line">{{ n.content }}</p>
          <p class="text-xs text-slate-400">接收范围：{{ n.audienceDesc || '—' }}</p>
        </div>
        <div class="text-right shrink-0 space-y-1">
          <p v-if="n.isRead" class="text-xs text-slate-400">已读 {{ fmt(n.readTime) }}</p>
          <button v-else class="text-xs text-brand-600 hover:underline" @click="read(n)">标记已读</button>
          <button v-if="routeOf(n)" class="block text-xs text-slate-500 hover:underline" @click="go(n)">前往处理</button>
        </div>
      </div>

      <p v-if="!list.length" class="py-8 text-center text-slate-400 text-sm">
        {{ tab === 'unread' ? '没有未读消息' : '暂无消息' }}
      </p>

      <div v-if="total > size" class="flex items-center justify-between pt-3 text-xs text-slate-500">
        <span>第 {{ page }} / {{ pages }} 页</span>
        <div class="flex items-center gap-2">
          <button class="px-2 py-1 rounded border border-slate-200 disabled:text-slate-300"
                  :disabled="page <= 1" @click="turn(page - 1)">上一页</button>
          <button class="px-2 py-1 rounded border border-slate-200 disabled:text-slate-300"
                  :disabled="page >= pages" @click="turn(page + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '../api/notification'

const router = useRouter()
const list = ref([])
const total = ref(0)
const unread = ref(0)
const page = ref(1)
const size = ref(10)
const tab = ref('all')
const message = ref('')
const ok = ref(true)

const tabs = [
  { key: 'all', text: '全部' },
  { key: 'unread', text: '未读' }
]
const pages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

const BIZ = {
  ALERT_NEW: '学情预警',
  ALERT_UPGRADE: '预警升级',
  CLASS_ALERT: '班集体提醒',
  ARCHIVE_TASK: '归档待办',
  ARCHIVE_RESULT: '归档结果',
  COVERAGE_REMIND: '覆盖率提醒',
  SCORE_TASK: '打分/成绩待办',
  MULTI_COURSE: '多科目预警',
  REFER_RESULT: '上报结果',
  QUESTIONNAIRE: '问卷',
  SYSTEM: '系统'
}
function bizLabel(t) { return BIZ[t] || t || '消息' }

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }

/** 从 payload_json 里取跳转地址（没有就返回空） */
function routeOf(n) {
  if (!n.payloadJson) return ''
  try {
    const p = JSON.parse(n.payloadJson)
    return p.route || ''
  } catch (e) {
    return ''
  }
}

async function load() {
  const data = await notificationApi.list({ unreadOnly: tab.value === 'unread', page: page.value, size: size.value })
  list.value = data.items || []
  total.value = data.total || 0
  unread.value = data.unread || 0
}

async function switchTab(key) {
  tab.value = key
  page.value = 1
  try { await load() } catch (e) { toast(e.message, false) }
}

async function turn(p) {
  page.value = p
  try { await load() } catch (e) { toast(e.message, false) }
}

async function read(n) {
  try {
    await notificationApi.markRead(n.id)
    await load()
  } catch (e) { toast(e.message, false) }
}

async function readAll() {
  try {
    const n = await notificationApi.markAllRead()
    toast(n > 0 ? `已把 ${n} 条标为已读` : '没有未读消息')
    await load()
  } catch (e) { toast(e.message, false) }
}

async function go(n) {
  if (!n.isRead) { try { await notificationApi.markRead(n.id) } catch (e) { /* 跳转优先 */ } }
  const path = routeOf(n)
  if (path) router.push(path)
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
