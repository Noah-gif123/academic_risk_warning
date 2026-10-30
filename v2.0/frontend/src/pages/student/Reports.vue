<template>
  <div class="space-y-4">
    <!-- 标题 -->
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/student/warnings')">← 返回学情预警</button>
        <h1 class="text-lg font-semibold text-slate-800">学习评价</h1>
        <span class="tag bg-slate-100 text-slate-500">个人学情</span>
      </div>
      <div class="flex items-center gap-2">
        <label class="text-xs text-slate-400">学期</label>
        <select v-model="termId" class="input w-44 text-sm" @change="onTermChange">
          <option v-for="t in terms" :key="t.id" :value="String(t.id)">{{ t.termName }}</option>
        </select>
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新' }}</button>
      </div>
    </div>

    <p class="text-xs text-slate-400">
      报告里的每个数字都由系统按<b>成绩构成项、每次测验与作业、题目分组、题型、考勤、预警结论</b>计算得出，
      AI 只负责把它写成人话；AI 不可用时自动改用规则模板，页面照常可用。
    </p>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <div class="grid lg:grid-cols-3 gap-4">
      <!-- 左：本学期课程 -->
      <div class="card p-3 lg:col-span-1 space-y-2">
        <div class="flex items-center justify-between px-1">
          <p class="text-xs text-slate-400">本学期课程（{{ courses.length }}）</p>
          <span class="text-[11px] text-slate-400">点课程看最近一次报告</span>
        </div>

        <div v-if="!courses.length" class="text-sm text-slate-400 px-1 py-6 text-center">
          这个学期没有选课记录
        </div>

        <ul class="space-y-2">
          <li v-for="c in courses" :key="c.teachingClassId">
            <button class="w-full text-left px-3 py-2 rounded-lg border transition"
                    :class="String(selectedClassId) === String(c.teachingClassId)
                      ? 'border-brand-400 bg-brand-50' : 'border-slate-200 hover:border-slate-300'"
                    @click="pickCourse(c)">
              <div class="flex items-center justify-between gap-2">
                <span class="text-sm font-medium text-slate-700 truncate">{{ c.courseName }}</span>
                <span v-if="latestOf(c.teachingClassId)" class="tag shrink-0"
                      :class="levelClass(latestOf(c.teachingClassId).riskLevel)">
                  {{ levelText(latestOf(c.teachingClassId).riskLevel) }}
                </span>
                <span v-else class="tag shrink-0 bg-slate-100 text-slate-500">未生成</span>
              </div>
              <div class="flex items-center gap-2 mt-1 text-xs text-slate-400">
                <span class="truncate">{{ c.className }}</span>
                <span v-if="latestOf(c.teachingClassId)" class="ml-auto shrink-0">
                  {{ latestOf(c.teachingClassId).totalScore ?? '-' }} 分 · {{ fmt(latestOf(c.teachingClassId).createTime) }}
                </span>
              </div>
            </button>
          </li>
        </ul>

        <!-- 历史报告：默认收起，点按钮才展开 -->
        <div class="pt-2 border-t border-slate-100">
          <button class="w-full flex items-center justify-between px-1 py-1 text-xs text-slate-500 hover:text-slate-700"
                  @click="showHistory = !showHistory">
            <span>历史报告（{{ historyOfSelected.length }}）</span>
            <span class="text-slate-400">{{ showHistory ? '收起 ▲' : '展开 ▼' }}</span>
          </button>
          <div v-if="showHistory" class="mt-1 max-h-56 overflow-y-auto pr-1 space-y-1">
            <p v-if="!historyOfSelected.length" class="text-xs text-slate-400 px-1 py-3 text-center">
              这门课还没有生成过报告
            </p>
            <button v-for="r in historyOfSelected" :key="r.id"
                    class="w-full text-left px-2 py-1.5 rounded border text-xs transition"
                    :class="active && active.id === r.id ? 'border-brand-300 bg-brand-50' : 'border-slate-200 hover:border-slate-300'"
                    @click="open(r.id)">
              <span class="text-slate-600">{{ fmt(r.createTime) }}</span>
              <span class="text-slate-400 ml-1">· {{ r.totalScore ?? '-' }} 分</span>
              <span class="ml-1 px-1 rounded"
                    :class="r.source === 'TEMPLATE' ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-600'">
                {{ r.source === 'TEMPLATE' ? '规则' : 'AI' }}
              </span>
            </button>
          </div>
        </div>
      </div>

      <!-- 右：报告详情 / 空态 -->
      <div class="lg:col-span-2">
        <div v-if="!active" class="card p-8 text-center space-y-3">
          <p class="text-sm text-slate-500">
            {{ selectedCourse ? `《${selectedCourse.courseName}》还没有生成过学习评价` : '先在左侧选一门课程' }}
          </p>
          <p class="text-xs text-slate-400">生成后会显示：结论 / 你的强项 / 需要补的地方 / 下一步做什么</p>
          <button class="btn-primary text-sm" :disabled="!selectedCourse || generating" @click="generate">
            {{ generating ? '生成中…（约 10 秒）' : '生成学习评价' }}
          </button>
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
              <button class="btn-primary text-sm" :disabled="generating" @click="generate">
                {{ generating ? '生成中…' : '重新生成' }}
              </button>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reportApi } from '../../api/ai'
import { portalApi } from '../../api/portal'
import { riskLevelBadge, riskLevelLabel } from '../../labels'

const router = useRouter()
const loading = ref(false)
const generating = ref(false)
const reports = ref([])
const courses = ref([])
const terms = ref([])
const termId = ref('')
const selectedClassId = ref('')
const active = ref(null)
const showHistory = ref(false)
const message = ref('')
const ok = ref(true)

const selectedCourse = computed(() =>
  courses.value.find((c) => String(c.teachingClassId) === String(selectedClassId.value)) || null)

/** 该课程的最近一次报告（列表按时间倒序，取第一条） */
function latestOf(teachingClassId) {
  return reports.value.find((r) => String(r.teachingClassId) === String(teachingClassId)) || null
}

/** 历史报告：只看当前选中课程，避免左栏被拉得很长 */
const historyOfSelected = computed(() =>
  reports.value.filter((r) => String(r.teachingClassId) === String(selectedClassId.value)))

function fmt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

function levelText(l) {
  return riskLevelLabel(l)
}

function levelClass(l) {
  return riskLevelBadge(l)
}

async function load() {
  loading.value = true
  try {
    if (!terms.value.length) {
      terms.value = (await portalApi.terms()) || []
      const cur = terms.value.find((t) => t.isCurrent === 1)
      if (!termId.value) termId.value = String((cur || terms.value[0] || {}).id || '')
    }
    const [list, page] = await Promise.all([
      reportApi.list(),
      portalApi.cards({ termId: termId.value || undefined, size: 50 })
    ])
    reports.value = (list || []).slice().sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
    courses.value = (page?.items || []).filter((c) => c.status !== 'ARCHIVED')

    // 默认选第一门"已生成过报告"的课；都没有就选第一门课
    if (!courses.value.some((c) => String(c.teachingClassId) === String(selectedClassId.value))) {
      const withReport = courses.value.find((c) => latestOf(c.teachingClassId))
      const pick = withReport || courses.value[0]
      selectedClassId.value = pick ? String(pick.teachingClassId) : ''
      active.value = null
      const latest = pick ? latestOf(pick.teachingClassId) : null
      if (latest) await open(latest.id)
    }
  } catch (e) {
    message.value = e?.message || '加载失败'
    ok.value = false
  } finally {
    loading.value = false
  }
}

function onTermChange() {
  selectedClassId.value = ''
  active.value = null
  showHistory.value = false
  load()
}

/** 点课程：自动显示该课程最近一次报告；没有就显示"生成"空态 */
async function pickCourse(c) {
  selectedClassId.value = String(c.teachingClassId)
  showHistory.value = false
  const latest = latestOf(c.teachingClassId)
  if (latest) {
    await open(latest.id)
  } else {
    active.value = null
    message.value = ''
  }
}

async function open(id) {
  try {
    active.value = await reportApi.detail(id)
    if (active.value?.teachingClassId) {
      selectedClassId.value = String(active.value.teachingClassId)
    }
  } catch (e) {
    message.value = e?.message || '打开报告失败'
    ok.value = false
  }
}

async function generate() {
  if (!selectedClassId.value) {
    message.value = '请先选一门课程'
    ok.value = false
    return
  }
  generating.value = true
  message.value = ''
  try {
    const r = await reportApi.generate({ teachingClassId: Number(selectedClassId.value), reportType: 'STAGE' })
    active.value = r
    showHistory.value = true
    message.value = r.source === 'TEMPLATE'
      ? '已生成（本次走规则模板：AI 通道不可用或校验未通过）'
      : '已生成（AI 撰写，数字已回验）'
    ok.value = true
    reports.value = (await reportApi.list()) || []
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
