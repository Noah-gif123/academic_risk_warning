<template>
  <div class="space-y-4">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <h1>系统通知</h1>
        <p class="text-xs text-slate-500 mt-2">系统按行政班汇总推送，支持标记已读；本页不提供主动群发。</p>
      </div>
      <button class="btn-ghost text-sm" @click="load">刷新通知</button>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-3 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">通知总数</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ list.length }}</p><p class="text-[11px] text-slate-400 mt-2">当前账号</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-red-400"></span><p class="text-[11px] text-slate-400">未读</p><p class="text-[28px] leading-none font-semibold text-red-600 mt-3">{{ unread }}</p><p class="text-[11px] text-slate-400 mt-2">左侧红点标记</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span><p class="text-[11px] text-slate-400">汇总通知</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ summarized }}</p><p class="text-[11px] text-slate-400 mt-2">一次覆盖多个班</p></div>
    </section>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">通知（{{ list.length }}）<span v-if="unread" class="text-xs text-red-600 ml-2">未读 {{ unread }}</span></h2>
        <span class="text-[11px] text-slate-400">系统消息 / 只读</span>
      </div>
      <div v-for="n in list" :key="n.id" class="border-b border-slate-100 py-3 flex items-start gap-3">
        <span class="w-2 h-2 rounded-full mt-2" :class="n.isRead ? 'bg-slate-200' : 'bg-red-500'"></span>
        <div class="flex-1 space-y-1">
          <div class="flex items-center gap-2">
            <p class="text-sm font-medium text-slate-800">{{ n.title }}</p>
            <span class="tag bg-slate-100 text-slate-500">{{ n.bizType }}</span>
            <span v-if="n.aggregateCount > 1" class="tag bg-brand-50 text-brand-600">汇总 {{ n.aggregateCount }} 条</span>
            <span class="text-xs text-slate-400">{{ fmt(n.createTime) }}</span>
          </div>
          <p class="text-sm text-slate-600">{{ n.content }}</p>
          <p class="text-xs text-slate-400">接收对象：{{ n.audienceDesc || '—' }}</p>
        </div>
        <div class="text-right">
          <span v-if="n.isRead" class="text-xs text-slate-400">已读 {{ fmt(n.readTime) }}</span>
          <button v-else class="text-xs text-brand-600 hover:underline" @click="read(n)">标记已读</button>
        </div>
      </div>
      <p v-if="!list.length" class="py-6 text-center text-slate-400 text-sm">暂无通知</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { counselorApi } from '../../api/counselor'

const list = ref([])
const message = ref('')
const ok = ref(true)

const unread = computed(() => list.value.filter((n) => !n.isRead).length)
const summarized = computed(() => list.value.filter((n) => Number(n.aggregateCount || 0) > 1).length)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }

async function load() {
  list.value = await counselorApi.notices()
}
async function read(n) {
  try {
    await counselorApi.markRead(n.id)
    toast('已标记已读')
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
