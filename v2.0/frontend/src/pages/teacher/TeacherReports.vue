<template>
  <div class="space-y-5">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <h1>学情报告</h1>
          <span class="tag bg-white text-slate-500 border border-[#e6edf5]">{{ termName || '当前学期' }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2 max-w-3xl leading-6">班级、课程总体、学生个人三类报告统一入口；生成过程与 Agent 状态可追溯，报告读取真实快照。</p>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" :disabled="loading" @click="refresh">{{ loading ? '刷新中…' : '刷新数据' }}</button>
        <button class="btn-primary text-sm" @click="$router.push('/teacher/monitor')">去预警监控</button>
      </div>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div v-for="k in kpis" :key="k.label" class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1" :class="k.accent"></span>
        <p class="text-[11px] text-slate-400">{{ k.label }}</p>
        <p class="text-[28px] leading-none font-semibold mt-3" :class="k.color">{{ k.value }}</p>
        <p class="text-[11px] text-slate-400 mt-2">{{ k.hint }}</p>
      </div>
    </section>

    <section class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <div><h2>多智能体报告编排</h2><p class="text-[11px] text-slate-400 mt-1">当前选择范围生成链路</p></div>
        <span class="text-[11px] text-slate-400">{{ agentCards.filter(a => a.ok).length }} / {{ agentCards.length }} 就绪</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-2">
        <div v-for="a in agentCards" :key="a.name" class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] px-3 py-3">
          <p class="text-base leading-none">{{ a.icon }}</p>
          <p class="text-xs font-medium text-[#2c465c] mt-2 truncate">{{ a.name }}</p>
          <p class="text-[10px] mt-1" :class="a.ok ? 'text-[#0f9d8a]' : 'text-slate-400'">{{ a.status }}</p>
        </div>
      </div>
    </section>

    <section class="card p-4 space-y-4">
      <div class="flex flex-wrap gap-2">
        <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
                :class="tab === t.key ? 'bg-[#0f9d8a] text-white shadow-sm' : 'bg-white text-slate-600 border border-[#e6edf5]'"
                @click="switchTab(t.key)">{{ t.label }}</button>
      </div>

      <div v-if="tab === 'class'" class="flex items-end justify-between flex-wrap gap-3">
        <label class="text-sm">教学班
          <select v-model="classId" class="input mt-1 w-80" @change="onTargetChange">
            <option :value="0">选择教学班</option>
            <option v-for="c in classOptions" :key="c.teachingClassId" :value="c.teachingClassId">{{ c.courseName }} · {{ c.classCode }}</option>
          </select>
        </label>
        <div class="flex items-center gap-2"><button class="btn-ghost text-sm" :disabled="!classId" @click="openGroup('CLASS', classId, false)">打开最新报告</button><button class="btn-primary text-sm" :disabled="!classId" @click="openGroup('CLASS', classId, true)">生成报告</button></div>
      </div>

      <div v-if="tab === 'offering'" class="flex items-end justify-between flex-wrap gap-3">
        <label class="text-sm">课程（我教的全部教学班）
          <select v-model="offeringId" class="input mt-1 w-80" @change="onTargetChange">
            <option :value="0">选择课程</option>
            <option v-for="c in offeringOptions" :key="c.offeringId" :value="c.offeringId">{{ c.courseName }} · {{ c.termName }}</option>
          </select>
        </label>
        <div class="flex items-center gap-2"><button class="btn-ghost text-sm" :disabled="!offeringId" @click="openGroup('OFFERING', offeringId, false)">打开最新报告</button><button class="btn-primary text-sm" :disabled="!offeringId" @click="openGroup('OFFERING', offeringId, true)">生成报告</button></div>
      </div>

      <div v-if="tab === 'student'" class="space-y-3">
        <div class="flex items-end gap-3 flex-wrap">
          <label class="text-sm">教学班
            <select v-model="studentClassId" class="input mt-1 w-72" @change="loadStudents">
              <option :value="0">选择教学班</option>
              <option v-for="c in classOptions" :key="c.teachingClassId" :value="c.teachingClassId">{{ c.courseName }} · {{ c.classCode }}</option>
            </select>
          </label>
          <label class="text-sm">学生
            <select v-model="studentId" class="input mt-1 w-72" @change="loadHistory">
              <option :value="0">选择学生</option>
              <option v-for="s in students" :key="s.studentId + '-' + s.teachingClassId" :value="s.studentId">{{ s.studentNo }} · {{ s.studentName }}</option>
            </select>
          </label>
          <div class="flex items-center gap-2"><button class="btn-ghost text-sm" :disabled="!studentId" @click="openStudentReport(false)">打开学生报告</button><button class="btn-primary text-sm" :disabled="!studentId" @click="openStudentReport(true)">生成报告</button></div>
        </div>
        <p class="text-[11px] text-slate-400">学生列表来自当前教学班的预警名单；没有预警的学生不在列表中。</p>
      </div>
    </section>

    <section class="card p-4">
      <div class="flex items-center justify-between mb-2">
        <div><h2>报告历史</h2><p class="text-[11px] text-slate-400 mt-1">{{ scopeLabel }}</p></div>
        <span class="text-[11px] text-slate-400">{{ history.length }} 份</span>
      </div>
      <div v-if="loadingHistory" class="py-8 text-center text-sm text-slate-400">正在读取报告历史…</div>
      <div v-else-if="!history.length" class="py-8 text-center text-sm text-slate-400">当前范围还没有报告；点击上方「生成报告」。</div>
      <div v-else class="divide-y divide-[#eef4f9]">
        <div v-for="r in history" :key="r.id" class="flex items-center justify-between gap-3 py-2.5">
          <div class="min-w-0">
            <p class="text-xs font-medium text-[#2c465c] truncate">{{ r.summary || r.title || '学情报告' }}</p>
            <p class="text-[10px] text-slate-400 mt-1">{{ fmt(r.createTime || r.generatedAt || r.time) }} · {{ r.source === 'TEMPLATE' ? '规则生成' : 'AI 生成' }}</p>
          </div>
          <span class="tag shrink-0" :class="r.checksTotal ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'">
            {{ r.checksTotal ? `对账 ${r.checksPassed}/${r.checksTotal}` : '报告快照' }}
          </span>
        </div>
      </div>
    </section>

    <GroupReportModal :open="groupOpen" :scope="groupScope" :target-id="groupTarget" :title="groupTitle" :generate-on-open="groupAutoGenerate" @close="groupOpen = false; groupAutoGenerate = false" />
    <StudentReportModal :open="studentOpen" :student="studentMeta" :courses="studentCourses" mode="teacher" :generate-on-open="studentAutoGenerate" @close="studentOpen = false; studentAutoGenerate = false" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { teacherPortalApi } from '../../api/teacherPortal'
import { teacherAlertApi } from '../../api/alert'
import { groupReportApi, staffReportApi } from '../../api/ai'
import GroupReportModal from '../../components/GroupReportModal.vue'
import StudentReportModal from '../../components/StudentReportModal.vue'

const tabs = [
  { key: 'class', label: '班级报告' },
  { key: 'offering', label: '课程总体报告' },
  { key: 'student', label: '学生个人报告' }
]
const tab = ref('class')
const loading = ref(false)
const message = ref('')
const ok = ref(true)
const courses = ref([])
const classId = ref(0)
const offeringId = ref(0)
const studentClassId = ref(0)
const studentId = ref(0)
const students = ref([])
const groupOpen = ref(false)
const groupAutoGenerate = ref(false)
const groupScope = ref('CLASS')
const groupTarget = ref(0)
const groupTitle = ref('')
const studentOpen = ref(false)
const studentAutoGenerate = ref(false)
const studentMeta = ref({})
const studentCourses = ref([])
const history = ref([])
const loadingHistory = ref(false)

const classOptions = computed(() => courses.value.flatMap(c => (c.classes || []).map(x => ({
  teachingClassId: x.teachingClassId,
  classCode: x.classCode || x.className || '教学班',
  className: x.className || x.classCode || '教学班',
  courseName: c.courseName
}))))
const offeringOptions = computed(() => courses.value.map(c => ({
  offeringId: c.offeringId,
  courseName: c.courseName,
  termName: c.termName
})))
const termName = computed(() => courses.value[0]?.termName || '')
const classCount = computed(() => new Set(classOptions.value.map(c => c.teachingClassId)).size)
const studentTotal = computed(() => courses.value.reduce((n, c) => n + Number(c.studentCount || 0), 0))
const kpis = computed(() => [
  { label: '我的课程', value: courses.value.length, hint: '当前学期', accent: 'bg-[#20b7a4]', color: 'text-[#17324b]' },
  { label: '教学班', value: classCount.value, hint: '本人任教', accent: 'bg-[#7aa7c7]', color: 'text-[#17324b]' },
  { label: '学生', value: studentTotal.value, hint: '去重前课程合计', accent: 'bg-indigo-300', color: 'text-[#17324b]' },
  { label: '报告历史', value: history.value.length, hint: '当前选择范围', accent: 'bg-amber-300', color: 'text-amber-600' }
])

const latest = computed(() => history.value[0] || null)
const selectedReady = computed(() => tab.value === 'class' ? !!classId.value : tab.value === 'offering' ? !!offeringId.value : !!studentId.value)
const agentCards = computed(() => [
  { icon: '🩺', name: '数据体检', status: courses.value.length ? '已覆盖' : '未运行', ok: courses.value.length > 0 },
  { icon: '📊', name: '数据分析', status: selectedReady.value ? '已加载' : '待选择', ok: selectedReady.value },
  { icon: '🧭', name: '学情研判', status: latest.value ? '已完成' : '待生成', ok: !!latest.value },
  { icon: '📚', name: 'RAG 证据', status: latest.value ? '报告内引用' : '待生成', ok: !!latest.value },
  { icon: '✅', name: '质量校验', status: latest.value?.checksTotal ? `对账 ${latest.value.checksPassed}/${latest.value.checksTotal}` : '待校验', ok: !!latest.value?.checksTotal },
  { icon: '📄', name: '报告导出', status: latest.value ? '可导出' : '待生成', ok: !!latest.value }
])
const scopeLabel = computed(() => tab.value === 'class'
  ? (classOptions.value.find(c => c.teachingClassId === classId.value)?.courseName || '请选择教学班')
  : tab.value === 'offering'
    ? (offeringOptions.value.find(c => c.offeringId === offeringId.value)?.courseName || '请选择课程')
    : (students.value.find(s => s.studentId === studentId.value)?.studentName || '请选择学生'))

function fmt(v) { return v ? String(v).replace('T', ' ').slice(0, 16) : '—' }
function flash(m, isOk = true) { message.value = m; ok.value = isOk; setTimeout(() => { if (message.value === m) message.value = '' }, 3000) }
function switchTab(k) { tab.value = k; history.value = []; loadHistory() }
function onTargetChange() { history.value = []; loadHistory() }

async function loadHistory() {
  history.value = []
  loadingHistory.value = true
  try {
    if (tab.value === 'class' && classId.value) history.value = await groupReportApi.history('CLASS', classId.value, 10) || []
    else if (tab.value === 'offering' && offeringId.value) history.value = await groupReportApi.history('OFFERING', offeringId.value, 10) || []
    else if (tab.value === 'student' && studentId.value) history.value = await staffReportApi.list(studentId.value) || []
  } catch (e) {
    flash(e?.message || '报告历史读取失败', false)
  } finally {
    loadingHistory.value = false
  }
}

function openGroup(scope, id, generate = false) {
  groupAutoGenerate.value = generate
  const c = scope === 'CLASS'
    ? classOptions.value.find(x => x.teachingClassId === id)
    : offeringOptions.value.find(x => x.offeringId === id)
  if (!c) return
  groupScope.value = scope
  groupTarget.value = id
  groupTitle.value = scope === 'CLASS'
    ? `${c.courseName} · ${c.classCode} · 班级学情报告`
    : `${c.courseName} · 课程总体学情报告`
  groupOpen.value = true
  loadHistory()
}

async function loadStudents() {
  students.value = []
  studentId.value = 0
  history.value = []
  if (!studentClassId.value) return
  try {
    const rows = await teacherAlertApi.list({ teachingClassId: studentClassId.value })
    const seen = new Set()
    students.value = (rows || []).filter(r => {
      const k = String(r.studentId)
      if (seen.has(k)) return false
      seen.add(k)
      return true
    })
  } catch (e) { flash(e?.message || '学生名单加载失败', false) }
}

async function openStudentReport(generate = false) {
  studentAutoGenerate.value = generate
  const s = students.value.find(x => x.studentId === studentId.value)
  if (!s) return
  const c = classOptions.value.find(x => x.teachingClassId === s.teachingClassId) || {}
  let fact = {}
  try {
    const list = await staffReportApi.list(s.studentId)
    const latest = (list || [])[0]
    if (latest) {
      const d = await staffReportApi.detail(s.studentId, latest.id)
      fact = d?.fact || {}
    }
  } catch (e) { /* 报告快照不可用时仍展示预警数据 */ }
  studentMeta.value = { studentId: s.studentId, studentName: s.studentName, studentNo: s.studentNo, adminClassName: s.classCode, riskScore: s.riskScore }
  studentCourses.value = [{
    teachingClassId: s.teachingClassId,
    courseName: s.courseName || c.courseName,
    teacherName: s.teacherName || '',
    riskLevel: s.alertLevel,
    riskHint: s.alertReason,
    absentCount: s.absentCount,
    allowedAbsent: s.allowedAbsent,
    hardFail: s.hardFail,
    totalScore: fact.totalScore ?? s.totalScore ?? s.channelBScore,
    processScore: fact.processScore ?? s.channelBScore,
    weightedScore: fact.weightedScore,
    deductPoints: fact.deductionPoints,
    finalExamNeeded: s.finalExamNeeded
  }]
  studentOpen.value = true
}

async function refresh() {
  loading.value = true
  try {
    const h = await teacherPortalApi.home()
    courses.value = h.courses || []
    if (!classId.value) classId.value = classOptions.value[0]?.teachingClassId || 0
    if (!offeringId.value) offeringId.value = offeringOptions.value[0]?.offeringId || 0
    if (!studentClassId.value) studentClassId.value = classOptions.value[0]?.teachingClassId || 0
    if (!students.value.length) await loadStudents()
    await loadHistory()
  } catch (e) {
    flash(e?.message || '课程数据加载失败', false)
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
</script>
