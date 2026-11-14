<template>
  <div class="space-y-5">
    <header class="flex items-end justify-between flex-wrap gap-3">
      <div>
        <h1>我的考试</h1>
        <p class="text-xs text-slate-500 mt-2">作业、测验与考试统一在这里作答；已结束的记录可查看成绩与详情。</p>
      </div>
      <div class="flex items-center gap-3">
        <button class="btn-ghost text-sm" @click="load">刷新</button>
        <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      </div>
    </header>

    <section class="grid grid-cols-2 lg:grid-cols-3 gap-3">
      <div class="card p-4">
        <p class="text-[11px] text-slate-400">未完成</p>
        <p class="text-2xl font-semibold text-[#17324b] mt-1">{{ openItems.length }}</p>
        <p class="text-[11px] text-slate-400 mt-1">待作答 / 待提交</p>
      </div>
      <div class="card p-4">
        <p class="text-[11px] text-slate-400">已结束</p>
        <p class="text-2xl font-semibold text-[#17324b] mt-1">{{ finishedItems.length }}</p>
        <p class="text-[11px] text-slate-400 mt-1">已提交 / 已批阅</p>
      </div>
      <div class="card p-4">
        <p class="text-[11px] text-slate-400">当前范围</p>
        <p class="text-2xl font-semibold text-[#0f9d8a] mt-1">{{ shown.length }}</p>
        <p class="text-[11px] text-slate-400 mt-1">当前页展示</p>
      </div>
    </section>

    <div class="flex flex-wrap items-center gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-[#0f9d8a] text-white shadow-sm' : 'bg-white text-slate-600 border border-[#e6edf5]'"
              @click="tab = t.key; page = 1">
        {{ t.label }}（{{ t.key === 'open' ? openItems.length : finishedItems.length }}）
      </button>

      <div v-if="tab === 'finished'" class="ml-auto flex flex-wrap items-center gap-2">
        <input v-model="q.keyword" class="input w-52" placeholder="关键词（课程 / 名称）" @keyup.enter="page = 1" />
        <input v-model="q.from" type="date" class="input w-40" />
        <span class="text-slate-400">~</span>
        <input v-model="q.to" type="date" class="input w-40" />
        <button class="btn-ghost text-sm" @click="page = 1">搜索</button>
      </div>
    </div>

    <div v-if="!shown.length" class="card p-10 text-center text-slate-400 text-sm">
      {{ tab === 'open' ? '没有未完成的考试 / 测验 / 作业' : '没有已结束的记录' }}
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
      <article v-for="p in shown" :key="p.id" class="card p-4 relative overflow-hidden hover:-translate-y-0.5 transition-transform">
        <span class="absolute left-0 top-0 bottom-0 w-1"
              :class="p.paperType === 'EXAM' ? 'bg-amber-400' : p.paperType === 'QUIZ' ? 'bg-[#20b7a4]' : 'bg-[#7aa7c7]'"></span>
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <p class="text-[11px] text-slate-400 truncate">{{ p.courseName || '—' }}</p>
            <h3 class="mt-1 leading-snug">{{ p.paperName }}</h3>
          </div>
          <span class="tag shrink-0" :class="typeClass(p.paperType)">{{ typeLabel(p.paperType) }}</span>
        </div>
        <p class="text-[11px] text-slate-400 mt-2">发起人 {{ p.teacherName || '—' }}</p>
        <div class="mt-3 rounded-lg bg-[#f8fbfd] border border-[#eef4f9] px-3 py-2 text-[11px] text-slate-500 space-y-1">
          <p>开始 {{ fmt(p.startTime) }}</p>
          <p>结束 {{ fmt(p.endTime) }}</p>
          <p>时长 {{ p.durationMinutes ? p.durationMinutes + ' 分钟' : '不限' }} ｜ 满分 {{ p.fullScore }}</p>
        </div>
        <div class="flex items-center justify-between mt-3 pt-3 border-t border-[#eef4f9]">
          <span class="text-xs" :class="statusClass(p)">{{ statusText(p) }}</span>
          <button v-if="p.canAnswer" class="btn-primary text-xs" @click="go(p.id)">开始作答</button>
          <button v-else class="text-xs text-[#3c6f9b] hover:underline" @click="go(p.id)">
            {{ tab === 'open' ? '查看' : '查看详情' }}
          </button>
        </div>
      </article>
    </div>

    <div class="flex items-center justify-between text-sm text-slate-500" v-if="tab === 'finished' && filtered.length > size">
      <span>共 {{ filtered.length }} 条，第 {{ page }} / {{ pages }} 页</span>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-xs" :disabled="page <= 1" @click="page--">上一页</button>
        <button class="btn-ghost text-xs" :disabled="page >= pages" @click="page++">下一页</button>
      </div>
    </div>

    <!-- 作答 / 看结果：独立弹窗（不再跳转页面） -->
    <Teleport to="body">
      <div v-if="takingId" class="fixed inset-0 z-50 bg-slate-900/45 backdrop-blur-sm overflow-hidden flex items-center justify-center p-4" @click.self="closeTake">
        <div class="bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto mx-auto my-0 relative">
          <button class="absolute right-4 top-4 z-10 text-slate-400 hover:text-slate-600 text-xl leading-none"
                  title="关闭（Esc）" @click="closeTake">✕</button>
          <div class="p-5">
            <TakePaper :paper-id="takingId" embedded @close="closeTake" @submitted="afterSubmit" />
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watchEffect } from 'vue'
import { myPaperApi } from '../../api/paper'
import TakePaper from './TakePaper.vue'

const takingId = ref(0)
const items = ref([])
const error = ref('')
const tab = ref('open')
const page = ref(1)
const size = 9
const q = reactive({ keyword: '', from: '', to: '' })

const tabs = [
  { key: 'open', label: '未完成' },
  { key: 'finished', label: '已结束' }
]

const openItems = computed(() => items.value.filter((p) => !p.myStatus || p.myStatus === 'NOT_STARTED'))
const finishedItems = computed(() => items.value.filter((p) => p.myStatus && p.myStatus !== 'NOT_STARTED'))

const filtered = computed(() => {
  if (tab.value === 'open') return openItems.value
  return finishedItems.value.filter((p) => {
    if (q.keyword) {
      const k = q.keyword.trim()
      const hay = (p.courseName || '') + p.paperName
      if (!hay.includes(k)) return false
    }
    const end = p.endTime ? String(p.endTime).slice(0, 10) : ''
    if (q.from && end && end < q.from) return false
    if (q.to && end && end > q.to) return false
    return true
  })
})
const pages = computed(() => Math.max(1, Math.ceil(filtered.value.length / size)))
const shown = computed(() => {
  if (tab.value === 'open') return filtered.value
  const from = (page.value - 1) * size
  return filtered.value.slice(from, from + size)
})

function typeLabel(t) { return { QUIZ: '测验', HOMEWORK: '作业', EXAM: '考试' }[t] || t }
function typeClass(t) {
  return { QUIZ: 'bg-brand-50 text-brand-600', HOMEWORK: 'bg-slate-100 text-slate-500', EXAM: 'bg-amber-50 text-amber-600' }[t]
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function statusText(p) {
  if (p.myStatus === 'ABSENT') return '缺考（0 分）'
  if (p.myStatus === 'MARKED') return '已批阅' + (p.myScore != null ? ' · ' + p.myScore + ' 分' : '')
  if (p.myStatus === 'SUBMITTED') return '已交卷'
  return p.canAnswer ? '未交卷' : (p.reason || '未开始')
}
function statusClass(p) {
  if (p.myStatus === 'ABSENT') return 'text-red-600'
  if (p.myStatus === 'MARKED') return 'text-emerald-600'
  if (p.myStatus === 'SUBMITTED') return 'text-amber-600'
  return p.canAnswer ? 'text-brand-600' : 'text-slate-400'
}
function go(id) { takingId.value = id }
function closeTake() { takingId.value = 0; load() }
function afterSubmit() { load() }
function onTakeKey(e) { if (e.key === 'Escape' && takingId.value) closeTake() }

async function load() {
  error.value = ''
  try { items.value = await myPaperApi.list() }
  catch (e) { error.value = e.message }
}

onMounted(() => {
  window.addEventListener('keydown', onTakeKey)
  load()
})
onBeforeUnmount(() => window.removeEventListener('keydown', onTakeKey))

// 答题弹窗打开时锁背景滚动
watchEffect(() => {
  document.body.style.overflow = takingId.value ? 'hidden' : ''
})
</script>
