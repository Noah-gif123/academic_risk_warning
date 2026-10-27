<template>
  <div class="space-y-4">
    <!-- 标题 -->
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <h1 class="text-lg font-semibold text-slate-800">学习评价报告</h1>
        <span class="tag bg-slate-100 text-slate-500">个人学情</span>
        <span v-if="currentCourse" class="text-xs text-slate-400">{{ currentCourse }}</span>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新' }}</button>
        <button class="btn-ghost text-sm" @click="$router.push('/student/warnings')">学情预警</button>
      </div>
    </div>

    <p class="text-xs text-slate-400">
      报告里的每个数字都由系统按<b>成绩构成项、每次测验与作业、题目分组、题型、考勤、预警结论</b>计算得出；
      AI 只负责把它写成人话。AI 不可用时会自动改用规则模板，页面照常可用。
    </p>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <!-- 生成区 -->
    <div class="card p-4">
      <div class="flex items-end gap-3 flex-wrap">
        <div class="min-w-[240px]">
          <p class="text-xs text-slate-400 mb-1">选择课程</p>
          <select v-model="pickClassId" class="input w-full">
            <option value="">请选择本学期课程</option>
            <option v-for="c in courses" :key="c.teachingClassId" :value="c.teachingClassId">
              {{ c.courseName }}（{{ c.className }}）
            </option>
          </select>
        </div>
        <button class="btn-primary text-sm" :disabled="!pickClassId || generating" @click="generate">
          {{ generating ? '生成中…（约 10 秒）' : '生成这份报告' }}
        </button>
        <span class="text-xs text-slate-400">
          同门课可以反复生成，旧报告保留，便于对比
        </span>
      </div>
    </div>

    <div class="grid lg:grid-cols-3 gap-4">
      <!-- 报告列表 -->
      <div class="card p-3 lg:col-span-1">
        <p class="text-xs text-slate-400 px-1 pb-2">历史报告（{{ reports.length }}）</p>
        <div v-if="!reports.length" class="text-sm text-slate-400 px-1 py-6 text-center">
          还没有报告，选一门课生成一份
        </div>
        <ul class="space-y-2 max-h-[560px] overflow-y-auto pr-1">
          <li v-for="r in reports" :key="r.id">
            <button
              class="w-full text-left px-3 py-2 rounded-lg border transition"
              :class="active && active.id === r.id ? 'border-brand-400 bg-brand-50' : 'border-slate-200 hover:border-slate-300'"
              @click="open(r.id)">
              <div class="flex items-center justify-between gap-2">
                <span class="text-sm font-medium text-slate-700 truncate">{{ r.courseName }}</span>
                <span class="tag shrink-0" :class="levelClass(r.riskLevel)">{{ levelText(r.riskLevel) }}</span>
              </div>
              <div class="flex items-center gap-2 mt-1 text-xs text-slate-400">
                <span>{{ r.totalScore ?? '-' }} 分</span>
                <span>·</span>
                <span>{{ fmt(r.createTime) }}</span>
                <span class="ml-auto">
                  <span v-if="r.source === 'DIFY'" class="text-brand-500">AI</span>
                  <span v-else-if="r.source === 'LLM'" class="text-brand-500">AI</span>
                  <span v-else class="text-slate-400">规则</span>
                </span>
              </div>
              <p class="text-xs text-slate-500 mt-1 line-clamp-2">{{ r.summary }}</p>
            </button>
          </li>
        </ul>
      </div>

      <!-- 报告详情 -->
      <div class="lg:col-span-2">
        <div v-if="!active" class="card p-10 text-center text-sm text-slate-400">
          左侧选择一份报告查看详情
        </div>
        <div v-else class="card p-5 space-y-4">
          <div class="flex items-start justify-between gap-3 flex-wrap">
            <div>
              <h2 class="text-base font-semibold text-slate-800">{{ active.courseName }} · 学习评价</h2>
              <p class="text-xs text-slate-400 mt-1">
                {{ active.termName }} · {{ active.className }} · {{ active.teacherName }} · {{ fmt(active.createTime) }}
                <span class="ml-1 px-1.5 py-0.5 rounded"
                      :class="active.source === 'TEMPLATE' ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-600'">
                  {{ active.source === 'TEMPLATE' ? '规则生成' : 'AI 生成' }}
                </span>
              </p>
            </div>
            <div class="flex items-center gap-2">
              <span class="tag" :class="levelClass(active.riskLevel)">{{ levelText(active.riskLevel) }}</span>
              <span class="text-sm text-slate-500">总分 {{ active.totalScore ?? '-' }}</span>
            </div>
          </div>

          <div v-for="(s, i) in active.sections" :key="i" class="rounded-lg bg-slate-50 px-4 py-3">
            <p class="text-sm font-medium text-slate-700 mb-1">{{ s.title }}</p>
            <p class="text-sm text-slate-600 whitespace-pre-line leading-relaxed">{{ s.text }}</p>
          </div>

          <!-- 动作 -->
          <div v-if="active.actions?.length" class="flex items-center gap-2 flex-wrap pt-1">
            <button v-for="(a, i) in active.actions" :key="i" class="btn-ghost text-sm" @click="doAction(a)">
              {{ a.label }}
            </button>
          </div>

          <!-- 反馈 -->
          <div class="flex items-center gap-3 pt-2 border-t border-slate-100 flex-wrap">
            <span class="text-xs text-slate-400">这份报告对你有帮助吗？</span>
            <button class="btn-ghost text-xs" :disabled="!!active.useful" @click="sendFeedback(true)">有用</button>
            <button class="btn-ghost text-xs" :disabled="active.useful === false" @click="sendFeedback(false)">没用</button>
            <span v-if="active.useful === true" class="text-xs text-emerald-600">已记录：有用</span>
            <span v-else-if="active.useful === false" class="text-xs text-slate-500">已记录：没用</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reportApi } from '../../api/ai'
import { portalApi } from '../../api/portal'

const router = useRouter()
const loading = ref(false)
const generating = ref(false)
const reports = ref([])
const courses = ref([])
const active = ref(null)
const pickClassId = ref('')
const message = ref('')
const ok = ref(true)

const currentCourse = ref('')

function fmt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

function levelText(l) {
  return { RED: '红灯', YELLOW: '黄灯', GREEN: '绿灯' }[l] || '未判定'
}

function levelClass(l) {
  return {
    RED: 'bg-red-50 text-red-600',
    YELLOW: 'bg-amber-50 text-amber-600',
    GREEN: 'bg-emerald-50 text-emerald-600'
  }[l] || 'bg-slate-100 text-slate-500'
}

async function load() {
  loading.value = true
  try {
    const [list, page] = await Promise.all([reportApi.list(), portalApi.cards({ size: 50 })])
    reports.value = list || []
    courses.value = (page?.items || []).filter((c) => c.status !== 'ARCHIVED')
    if (!active.value && reports.value.length) {
      await open(reports.value[0].id)
    }
  } catch (e) {
    message.value = e?.message || '加载失败'
    ok.value = false
  } finally {
    loading.value = false
  }
}

async function open(id) {
  try {
    active.value = await reportApi.detail(id)
    currentCourse.value = active.value?.courseName || ''
  } catch (e) {
    message.value = e?.message || '打开报告失败'
    ok.value = false
  }
}

async function generate() {
  if (!pickClassId.value) return
  generating.value = true
  message.value = ''
  try {
    const r = await reportApi.generate({ teachingClassId: Number(pickClassId.value), reportType: 'STAGE' })
    active.value = r
    message.value = r.source === 'TEMPLATE'
      ? '已生成（本次走规则模板：AI 通道不可用或校验未通过）'
      : '已生成（AI 撰写，数字已回验）'
    ok.value = true
    const list = await reportApi.list()
    reports.value = list || []
  } catch (e) {
    message.value = e?.message || '生成失败'
    ok.value = false
  } finally {
    generating.value = false
  }
}

async function sendFeedback(useful) {
  if (!active.value) return
  try {
    await reportApi.feedback(active.value.id, useful, '')
    active.value.useful = useful
    message.value = '谢谢反馈，会用来改进报告质量'
    ok.value = true
  } catch (e) {
    message.value = e?.message || '反馈失败'
    ok.value = false
  }
}

function doAction(a) {
  if (!a) return
  if (a.type === 'REDO') {
    router.push('/student/exams')
    return
  }
  if (a.type === 'GUIDE') {
    if (a.payload?.teachingClassId) router.push(`/student/course/${a.payload.teachingClassId}`)
    return
  }
  if (a.type === 'ASK_TEACHER') {
    message.value = `可以直接在「消息中心」给任课老师 ${a.payload?.teacherName || ''} 留言，或在下次课前当面问。`
    ok.value = true
  }
}

onMounted(load)
</script>
