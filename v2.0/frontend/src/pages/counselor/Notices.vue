<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">通知与回执</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      系统只发**汇总通知**（一次一条，覆盖名下全部行政班），支持标记已读；未读条目左侧有红点。
    </p>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">通知（{{ list.length }}）<span v-if="unread" class="text-xs text-red-600 ml-2">未读 {{ unread }}</span></h2>
        <button class="text-xs text-brand-600 hover:underline" @click="load">刷新</button>
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
