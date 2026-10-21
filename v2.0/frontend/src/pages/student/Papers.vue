<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">我的考试</h1>
      <div class="flex items-center gap-3">
        <button class="text-xs text-brand-600 hover:underline" @click="load">刷新</button>
        <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      </div>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key; page = 1">
        {{ t.label }}（{{ t.key === 'open' ? openItems.length : finishedItems.length }}）
      </button>

      <!-- 已结束才显示搜索 -->
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

    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div v-for="p in shown" :key="p.id" class="card p-4 space-y-2">
        <div class="flex items-start justify-between gap-2">
          <h3 class="font-medium text-slate-800">
            {{ p.courseName || '—' }} · {{ p.paperName }}
          </h3>
          <span class="tag" :class="typeClass(p.paperType)">{{ typeLabel(p.paperType) }}</span>
        </div>
        <p class="text-xs text-slate-500">发起人 {{ p.teacherName || '—' }}</p>
        <p class="text-xs text-slate-400">
          开始 {{ fmt(p.startTime) }} ｜ 结束 {{ fmt(p.endTime) }}
        </p>
        <p class="text-xs text-slate-400">
          时长 {{ p.durationMinutes ? p.durationMinutes + ' 分钟' : '不限' }} ｜ 试卷满分 {{ p.fullScore }}
        </p>
        <div class="flex items-center justify-between pt-1">
          <span class="text-xs" :class="statusClass(p)">{{ statusText(p) }}</span>
          <button v-if="p.canAnswer" class="btn-primary text-xs"
                  @click="go(p.id)">开始作答</button>
          <button v-else class="text-xs text-brand-600 hover:underline" @click="go(p.id)">
            {{ tab === 'open' ? '查看' : '查看详情' }}
          </button>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-between text-sm text-slate-500" v-if="tab === 'finished' && filtered.length > size">
      <span>共 {{ filtered.length }} 条，第 {{ page }} / {{ pages }} 页</span>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-xs" :disabled="page <= 1" @click="page--">上一页</button>
        <button class="btn-ghost text-xs" :disabled="page >= pages" @click="page++">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myPaperApi } from '../../api/paper'

const router = useRouter()
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
function go(id) { router.push({ path: '/student/take', query: { paperId: id } }) }

async function load() {
  error.value = ''
  try { items.value = await myPaperApi.list() }
  catch (e) { error.value = e.message }
}

onMounted(load)
</script>
