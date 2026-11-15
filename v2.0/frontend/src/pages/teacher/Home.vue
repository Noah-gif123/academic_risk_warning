<template>
  <div class="space-y-6">
    <section class="card p-6 flex items-start justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-semibold">教学驾驶舱，{{ name }}</h1>
        <p class="text-slate-500 mt-1">先处理风险学生和待批改，再看课程。</p>
      </div>
      <label class="text-sm">学期
        <select v-model="termId" class="input mt-1 w-56" @change="load">
          <option :value="''">全部学期</option>
          <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
        </select>
      </label>
    </section>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-5 gap-3">
      <div v-for="k in kpis" :key="k.label" class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1" :class="k.accent"></span>
        <p class="text-[11px] text-slate-400">{{ k.label }}</p>
        <p class="text-[28px] leading-none font-semibold mt-3" :class="k.color">{{ k.value }}</p>
        <p class="text-[11px] text-slate-400 mt-2">{{ k.hint }}</p>
      </div>
    </section>

    <section class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <div><h2>多智能体教学状态</h2><p class="text-[11px] text-slate-400 mt-1">课程、预警、批改与报告的真实运行状态</p></div>
        <button class="text-xs text-[#0f9d8a] hover:underline" @click="$router.push('/teacher/reports')">学情报告 →</button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-2">
        <div v-for="a in agentCards" :key="a.name" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] px-3 py-3">
          <p class="text-base leading-none">{{ a.icon }}</p>
          <p class="text-xs font-medium text-[#2c465c] mt-2">{{ a.name }}</p>
          <p class="text-[10px] mt-1" :class="a.ok ? 'text-[#0f9d8a]' : 'text-slate-400'">{{ a.status }}</p>
        </div>
      </div>
    </section>

    <!-- 我的教学课程 -->
    <section>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-base font-semibold text-slate-700">我的教学课程（{{ courses.length }}）</h2>
        <div class="flex items-center gap-3">
          <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/teacher/courses')">全部课程</button>
          <button class="text-xs text-brand-600 hover:underline" @click="load">刷新</button>
        </div>
      </div>
      <div v-if="!courses.length" class="card p-10 text-center text-slate-400 text-sm">
        还没有教学班。等教务在「排课与教学班」里为你分配教学班后，这里会显示课程卡片。
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <div v-for="c in sortedCourses" :key="c.offeringId" class="card overflow-hidden hover:shadow-md transition flex flex-col">
          <div class="h-24 bg-gradient-to-r from-[#0f9d8a] to-[#0b7d6e] flex items-start justify-between p-3">
            <span class="tag bg-white/85 text-slate-700">{{ courseTypeLabel(c.courseType) }}</span>
            <span class="tag" :class="c.allClosed ? 'bg-white/80 text-slate-600' : 'bg-white/90 text-brand-700'">
              {{ c.allClosed ? '已结课' : '进行中' }}
            </span>
          </div>
          <div class="p-4 flex-1 flex flex-col justify-between space-y-2">
            <div class="space-y-1.5">
              <h3 class="text-base font-semibold text-slate-800 leading-snug">{{ c.courseName }}</h3>
              <p class="text-sm text-slate-500">{{ c.termName }}</p>
              <p class="text-sm text-slate-400">负责 {{ c.classCount }} 个教学班 ｜ 学生 {{ c.studentCount }} 人</p>
            </div>
            <div class="flex items-center justify-between pt-2 border-t border-slate-100">
              <button class="text-sm text-brand-600 hover:underline font-medium"
                      @click="$router.push({ path: '/teacher/course/' + c.offeringId })">进入课程 →</button>
              <div class="flex items-center gap-2">
                <button class="text-xs" :class="pinned.includes(c.offeringId) ? 'text-amber-600' : 'text-slate-400'"
                        @click="togglePin(c.offeringId)">
                  {{ pinned.includes(c.offeringId) ? '已置顶' : '置顶' }}
                </button>
                <button class="text-xs text-brand-600 hover:underline"
                        @click="$router.push({ path: '/teacher/attendance', query: { teachingClassId: (c.classes && c.classes[0] || {}).teachingClassId } })">
                  考勤
                </button>
                <button class="text-xs text-slate-400 hover:text-red-500"
                        @click="doClose(c)">结课</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 待批改 -->
    <section>
      <h2 class="text-base font-semibold text-slate-700 mb-3">待批改</h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div v-for="g in pendingGroups" :key="g.key" class="card p-4 space-y-2">
          <div class="flex items-center justify-between">
            <h3 class="font-medium text-slate-700">{{ g.label }}</h3>
            <span class="tag" :class="g.total ? 'bg-amber-50 text-amber-600' : 'bg-slate-100 text-slate-500'">
              {{ g.total }} 份待处理
            </span>
          </div>
          <div v-if="!g.items.length" class="text-xs text-slate-400 py-2">没有待批改</div>
          <div v-for="it in g.items.slice(0, 2)" :key="g.key + it.id" class="border border-slate-100 rounded-lg p-2 space-y-1">
            <p class="text-sm text-slate-700">{{ it.title }}</p>
            <p class="text-xs text-slate-400">{{ it.courseName }} ｜ {{ it.className }} ｜ 截止 {{ fmt(it.endTime) }}</p>
            <div class="flex items-center justify-between">
              <span class="text-xs text-amber-600">{{ it.pendingCount }} 份待处理</span>
              <button class="text-xs text-brand-600 hover:underline" @click="goGrade(it, g.key)">去批改</button>
            </div>
          </div>
          <div v-if="g.items.length > 2" class="flex items-center justify-between pt-1 border-t border-slate-100">
            <span class="text-xs text-slate-400">还有 {{ g.items.length - 2 }} 条未显示</span>
            <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/teacher/scores')">
              查看全部（{{ g.items.length }}）
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { teacherPortalApi } from '../../api/teacherPortal'
import { teacherAlertApi } from '../../api/alert'
import { myPaperApi } from '../../api/paper'
import { session } from '../../session'
import { courseTypeLabel } from '../../labels'

const router = useRouter()
const name = computed(() => session.realName || '老师')
const terms = ref([])
const termId = ref('')
const courses = ref([])
const pending = ref({ pendingHomeworks: [], pendingQuizzes: [], pendingProjects: [], homeworkPending: 0, quizPending: 0, projectPending: 0 })
const riskRows = ref([])
const pinned = ref(JSON.parse(localStorage.getItem('teacherPinnedCourses') || '[]'))
const message = ref('')
const ok = ref(true)

const sortedCourses = computed(() => {
  const arr = [...courses.value]
  arr.sort((a, b) => {
    const pa = pinned.value.indexOf(a.offeringId)
    const pb = pinned.value.indexOf(b.offeringId)
    if (pa >= 0 || pb >= 0) {
      return (pb < 0 ? -1 : pb) - (pa < 0 ? -1 : pa) || 0
    }
    return String(a.courseName || '').localeCompare(String(b.courseName || ''))
  })
  return arr
})

const pendingGroups = computed(() => [
  { key: 'homework', label: '作业', items: pending.value.pendingHomeworks || [], total: pending.value.homeworkPending },
  { key: 'quiz', label: '测试', items: pending.value.pendingQuizzes || [], total: pending.value.quizPending },
  { key: 'project', label: '项目', items: pending.value.pendingProjects || [], total: pending.value.projectPending }
])

const classCount = computed(() => new Set(courses.value.flatMap(c => (c.classes || []).map(x => x.teachingClassId))).size)
const studentCount = computed(() => courses.value.reduce((n, c) => n + Number(c.studentCount || 0), 0))
const pendingTotal = computed(() => Number(pending.value.homeworkPending || 0) + Number(pending.value.quizPending || 0) + Number(pending.value.projectPending || 0))
const riskStudentCount = computed(() => new Set(riskRows.value.map(r => String(r.studentId))).size)
const kpis = computed(() => [
  { label: '我的课程', value: courses.value.length, hint: '当前学期', accent: 'bg-[#20b7a4]', color: 'text-[#17324b]' },
  { label: '教学班', value: classCount.value, hint: '本人任教', accent: 'bg-[#7aa7c7]', color: 'text-[#17324b]' },
  { label: '学生', value: studentCount.value, hint: '课程去重前合计', accent: 'bg-indigo-300', color: 'text-[#17324b]' },
  { label: '风险学生', value: riskStudentCount.value, hint: '当前预警名单', accent: 'bg-red-300', color: 'text-red-600' },
  { label: '待批改', value: pendingTotal.value, hint: '作业 / 测试 / 项目', accent: 'bg-amber-300', color: 'text-amber-600' }
])
const agentCards = computed(() => [
  { icon: '🩺', name: '数据体检', status: courses.value.length ? '已覆盖' : '未运行', ok: courses.value.length > 0 },
  { icon: '📊', name: '成绩分析', status: `${studentCount.value} 名学生`, ok: studentCount.value > 0 },
  { icon: '🧭', name: '学情研判', status: riskRows.value.length ? `${riskRows.value.length} 条预警` : '暂无预警', ok: riskRows.value.length > 0 },
  { icon: '📝', name: '作业批改', status: pendingTotal.value ? `${pendingTotal.value} 份待处理` : '已清空', ok: pendingTotal.value === 0 },
  { icon: '📄', name: '报告生成', status: '待生成', ok: false },
  { icon: '✅', name: '质量校验', status: '真实快照', ok: true }
])

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '不限' }

function togglePin(offeringId) {
  const i = pinned.value.indexOf(offeringId)
  if (i >= 0) pinned.value.splice(i, 1)
  else pinned.value.unshift(offeringId)
  localStorage.setItem('teacherPinnedCourses', JSON.stringify(pinned.value))
}

async function load() {
  try {
    const [h, alerts] = await Promise.all([
      teacherPortalApi.home(termId.value || undefined),
      teacherAlertApi.list().catch(() => [])
    ])
    pending.value = h
    courses.value = h.courses
    riskRows.value = alerts || []
  } catch (e) { toast(e.message, false) }
}

async function doClose(c) {
  const classIds = (c.classes || []).filter((x) => x.status !== 'CLOSED').map((x) => x.teachingClassId)
  if (!classIds.length) { toast('该课程的教學班都已结课', false); return }
  if (!window.confirm('结课后该课程在学生端不再显示（数据保留）。确认结课？')) return
  try {
    for (const id of classIds) {
      await teacherPortalApi.closeClass(id)
    }
    toast('已结课')
    await load()
  } catch (e) { toast(e.message, false) }
}

function goGrade(item, kind) {
  if (kind === 'project') {
    router.push({ path: '/teacher/projects', query: { teachingClassId: item.teachingClassId } })
  } else {
    router.push({ path: '/teacher/grading', query: { paperId: item.id } })
  }
}

onMounted(async () => {
  try {
    terms.value = await teacherPortalApi.terms()
    const cur = terms.value.find((t) => t.isCurrent === 1)
    if (cur) termId.value = cur.id
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
