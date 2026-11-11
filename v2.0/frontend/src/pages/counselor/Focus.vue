<template>
  <div class="space-y-4">
    <div class="flex items-end justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-xl font-semibold text-slate-800">预警总览</h1>
        <p class="text-xs text-slate-400 mt-1">
          名下行政班 · {{ overview?.termName || '当前学期' }} · 快照按危险度排序
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="tag bg-slate-100 text-slate-500">{{ overview?.weekNo ? '第 ' + overview.weekNo + ' 周' : '当前' }}</span>
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">
          {{ loading ? '加载中…' : '刷新' }}
        </button>
      </div>
    </div>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute -right-5 -top-5 w-20 h-20 rounded-full bg-brand-50"></span>
        <p class="text-xs text-slate-400">名下学生</p>
        <p class="text-3xl font-semibold text-slate-800 mt-2">{{ overview?.studentCount ?? students.length }}</p>
        <p class="text-xs text-slate-400 mt-1">{{ overview?.classCount ?? classes.length }} 个行政班</p>
      </div>
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute -right-5 -top-5 w-20 h-20 rounded-full bg-red-50"></span>
        <p class="text-xs text-slate-400">风险学生</p>
        <p class="text-3xl font-semibold text-red-600 mt-2">{{ overview?.riskStudentCount ?? riskyCount }}</p>
        <p class="text-xs text-slate-400 mt-1">红/橙/黄均需关注</p>
      </div>
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute -right-5 -top-5 w-20 h-20 rounded-full bg-amber-50"></span>
        <p class="text-xs text-slate-400">重点学生（≥3 门）</p>
        <p class="text-3xl font-semibold text-amber-600 mt-2">{{ focusCount }}</p>
        <p class="text-xs text-slate-400 mt-1">优先约谈</p>
      </div>
      <div class="card p-4 relative overflow-hidden">
        <span class="absolute -right-5 -top-5 w-20 h-20 rounded-full bg-slate-100"></span>
        <p class="text-xs text-slate-400">平均挂科率</p>
        <p class="text-3xl font-semibold text-slate-700 mt-2">{{ pct(overview?.avgFailRate) }}</p>
        <p class="text-xs text-slate-400 mt-1">按已出分课程统计</p>
      </div>
    </div>

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-4">
      <div class="card p-4 xl:col-span-1">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">危险度分布</h2>
          <span class="text-xs text-slate-400">{{ students.length }} 人</span>
        </div>
        <div class="flex items-center gap-4 mt-4">
          <div class="w-28 h-28 rounded-full relative shrink-0" :style="{ background: donutBg }">
            <div class="absolute inset-4 rounded-full bg-white"></div>
            <div class="absolute inset-0 flex flex-col items-center justify-center">
              <b class="text-xl text-slate-800">{{ students.length }}</b>
              <span class="text-[10px] text-slate-400">学生</span>
            </div>
          </div>
          <div class="space-y-2 text-xs flex-1">
            <div v-for="b in buckets" :key="b.label" class="flex items-center justify-between gap-2">
              <span class="flex items-center gap-1.5 text-slate-500">
                <i class="w-2 h-2 rounded-full inline-block" :style="{ background: b.color }"></i>{{ b.label }}
              </span>
              <b class="text-slate-700">{{ b.value }}</b>
            </div>
          </div>
        </div>
        <p class="text-[10px] text-slate-400 mt-4 leading-5">
          有风险学生按危险度分：≥80 高、60–79 中、&lt;60 低；无风险课程为正常。
        </p>
      </div>

      <div class="card p-4 xl:col-span-2">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">班级风险 Top</h2>
          <span class="text-xs text-slate-400">按风险学生数</span>
        </div>
        <div class="mt-4 space-y-3">
          <div v-for="c in classBars" :key="c.adminClassId" class="grid grid-cols-[110px_1fr_44px] gap-3 items-center">
            <span class="text-xs text-slate-600 truncate" :title="c.className">{{ c.className }}</span>
            <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
              <div class="h-full rounded-full bg-gradient-to-r from-amber-400 to-red-500" :style="{ width: classRate(c) + '%' }"></div>
            </div>
            <b class="text-xs text-right text-slate-600">{{ c.riskStudentCount ?? 0 }} 人</b>
          </div>
          <p v-if="!classBars.length" class="text-sm text-slate-400 py-6 text-center">暂无班级数据</p>
        </div>
        <p class="text-[10px] text-slate-400 mt-4">班级风险人数 / 班级人数；点击下方名单可继续查看学生详情。</p>
      </div>
    </div>

    <div class="card p-4 space-y-3">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center gap-3 flex-wrap">
          <h2 class="text-sm font-medium text-slate-700">重点关注学生</h2>
          <span class="tag bg-red-50 text-red-600">有风险 {{ riskyCount }} 人</span>
          <span class="tag bg-amber-50 text-amber-600">≥3 门 {{ focusCount }} 人</span>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <select v-model="adminClassId" class="input w-52" @change="load">
            <option :value="0">全部班级</option>
            <option v-for="c in classes" :key="c.adminClassId" :value="c.adminClassId">
              {{ c.className }}（{{ c.gradeYear }}级）
            </option>
          </select>
          <label class="flex items-center gap-1 text-xs text-slate-500">
            <input type="checkbox" v-model="onlyRisky" /> 只看有风险
          </label>
        </div>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-2 gap-3">
        <div v-for="s in topStudents" :key="s.studentId"
             class="border border-slate-200 rounded-xl p-3 hover:border-brand-300 hover:bg-brand-50/20 transition cursor-pointer"
             @click="open(s)">
          <div class="flex items-start justify-between gap-2">
            <div>
              <p class="text-sm font-medium text-slate-800">{{ s.studentName }}
                <span class="text-xs text-slate-400 font-normal ml-1">{{ s.studentNo }}</span>
              </p>
              <p class="text-xs text-slate-400 mt-1">{{ s.adminClassName }} · {{ s.riskHint || '暂无风险提示' }}</p>
            </div>
            <span class="tag shrink-0" :class="riskBadge(s.riskScore)">{{ riskLabel(s.riskScore) }} {{ num(s.riskScore) }}</span>
          </div>
          <div class="grid grid-cols-4 gap-2 mt-3 text-center">
            <div><p class="text-[10px] text-slate-400">风险课程</p><b class="text-sm text-slate-700">{{ s.riskCourseCount ?? 0 }}</b></div>
            <div><p class="text-[10px] text-slate-400">挂科</p><b class="text-sm text-red-500">{{ s.failCourseCount ?? 0 }}</b></div>
            <div><p class="text-[10px] text-slate-400">缺课</p><b class="text-sm text-slate-700">{{ s.absentTotal ?? 0 }}</b></div>
            <div><p class="text-[10px] text-slate-400">最低总评</p><b class="text-sm text-slate-700">{{ s.minTotalScore ?? '—' }}</b></div>
          </div>
          <div class="flex justify-end mt-2">
            <button class="text-xs text-brand-600 hover:underline" @click.stop="openReport(s)">查看报告</button>
          </div>
        </div>
        <div v-if="!topStudents.length" class="text-sm text-slate-400 py-8 text-center xl:col-span-2">当前筛选下没有风险学生</div>
      </div>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-2">
        <h2 class="text-sm font-medium text-slate-700">全部名单</h2>
        <span class="text-xs text-slate-400">共 {{ shown.length }} 人</span>
      </div>
      <div class="overflow-x-auto border border-slate-200 rounded-lg">
        <table class="w-full text-sm">
          <thead class="text-slate-500 bg-slate-50 border-b border-slate-200">
            <tr>
              <th class="text-left py-2 px-3">学号</th><th class="text-left py-2">姓名</th>
              <th class="text-left py-2">班级</th><th class="text-right py-2">风险课程</th>
              <th class="text-right py-2">挂科</th><th class="text-right py-2">缺课</th>
              <th class="text-right py-2">最低总评</th><th class="text-right py-2">危险度</th>
              <th class="text-right py-2 px-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in shown" :key="s.studentId" class="border-b border-slate-100 hover:bg-slate-50/60">
              <td class="py-2 px-3 text-slate-500">{{ s.studentNo }}</td>
              <td class="py-2">{{ s.studentName }}</td>
              <td class="py-2 text-slate-500">{{ s.adminClassName }}</td>
              <td class="py-2 text-right text-amber-600">{{ s.riskCourseCount ?? 0 }}</td>
              <td class="py-2 text-right text-red-500">{{ s.failCourseCount ?? 0 }}</td>
              <td class="py-2 text-right">{{ s.absentTotal ?? 0 }}</td>
              <td class="py-2 text-right">{{ s.minTotalScore ?? '—' }}</td>
              <td class="py-2 text-right font-medium">{{ num(s.riskScore) }}</td>
              <td class="py-2 px-3 text-right">
                <button class="text-brand-600 hover:underline" @click="open(s)">详情</button>
                <button class="text-brand-600 hover:underline ml-2" @click="openReport(s)">报告</button>
              </td>
            </tr>
            <tr v-if="!shown.length"><td colspan="9" class="py-6 text-center text-slate-400">当前筛选下没有学生</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <StudentDetailPanel v-if="detail" :detail="detail"
                        @close="detail = null"
                        @refer="refer"
                        @terminate="terminate"
                        @saved="reloadDetail" />

    <StudentReportModal :open="reportOpen" :student="reportStudent" :courses="reportCourses"
                        mode="counselor" @close="reportOpen = false" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { counselorApi } from '../../api/counselor'
import StudentDetailPanel from './StudentDetailPanel.vue'
import StudentReportModal from '../../components/StudentReportModal.vue'

const classes = ref([])
const students = ref([])
const overview = ref(null)
const adminClassId = ref(0)
const onlyRisky = ref(true)
const detail = ref(null)
const reportOpen = ref(false)
const reportStudent = ref(null)
const reportCourses = ref([])
const loading = ref(false)
const message = ref('')
const ok = ref(true)

const shown = computed(() => (onlyRisky.value
  ? students.value.filter((s) => (s.riskCourseCount ?? 0) > 0)
  : students.value))
const riskyCount = computed(() => students.value.filter((s) => (s.riskCourseCount ?? 0) > 0).length)
const focusCount = computed(() => students.value.filter((s) => (s.riskCourseCount ?? 0) >= 3).length)
const topStudents = computed(() => shown.value.slice(0, 6))
const buckets = computed(() => {
  const list = students.value
  const isRisk = (s) => (s.riskCourseCount ?? 0) > 0
  return [
    { label: '高风险', color: '#d94b4b', value: list.filter((s) => isRisk(s) && (s.riskScore ?? 0) >= 80).length },
    { label: '中风险', color: '#e7953c', value: list.filter((s) => isRisk(s) && (s.riskScore ?? 0) >= 60 && (s.riskScore ?? 0) < 80).length },
    { label: '低风险', color: '#d9b63a', value: list.filter((s) => isRisk(s) && (s.riskScore ?? 0) < 60).length },
    { label: '正常', color: '#2f9e6f', value: list.filter((s) => !isRisk(s)).length }
  ]
})
const donutBg = computed(() => {
  const total = students.value.length || 1
  let start = 0
  const parts = buckets.value.map((b) => {
    const end = start + b.value / total * 100
    const p = `${b.color} ${start}% ${end}%`
    start = end
    return p
  })
  return `conic-gradient(${parts.join(',')})`
})
const classBars = computed(() => (overview.value?.classes || [])
  .slice()
  .sort((a, b) => (b.riskStudentCount ?? 0) - (a.riskStudentCount ?? 0))
  .slice(0, 6))

function classRate(c) {
  if (!c.studentCount) return 0
  return Math.min(100, Math.round((c.riskStudentCount ?? 0) * 100 / c.studentCount))
}
function num(v) { return v == null ? '—' : Number(v).toFixed(1) }
function pct(v) { return v == null ? '—' : Number(v).toFixed(2) + '%' }
function riskLabel(v) { v = Number(v || 0); return v >= 80 ? '高' : v >= 60 ? '中' : v >= 40 ? '低' : '正常' }
function riskBadge(v) {
  v = Number(v || 0)
  if (v >= 80) return 'bg-red-50 text-red-600'
  if (v >= 60) return 'bg-orange-50 text-orange-600'
  if (v >= 40) return 'bg-amber-50 text-amber-700'
  return 'bg-emerald-50 text-emerald-600'
}

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function load() {
  loading.value = true
  try {
    const o = await counselorApi.overview()
    overview.value = o
    classes.value = o.classes || []
    students.value = await counselorApi.students({ adminClassId: adminClassId.value || undefined })
  } finally {
    loading.value = false
  }
}
async function open(s) { detail.value = await counselorApi.studentDetail(s.studentId) }
async function openReport(s) {
  const d = await counselorApi.studentDetail(s.studentId)
  reportStudent.value = s
  reportCourses.value = d.courses || []
  reportOpen.value = true
}
async function reloadDetail() {
  if (!detail.value) return
  detail.value = await counselorApi.studentDetail(detail.value.studentId)
  await load()
}
async function refer(r) {
  const note = window.prompt('上报说明（教务可见）：', '两次处理均无改善，请教务协助介入')
  if (note === null) return
  try {
    await counselorApi.refer(r.id, note)
    toast('已上报教务')
    await reloadDetail()
  } catch (e) { toast(e.message, false) }
}
async function terminate(r) {
  const reason = window.prompt('终止原因：', '学生已转专业，停止跟踪')
  if (reason === null) return
  try {
    await counselorApi.terminate(r.id, reason)
    toast('已终止干预')
    await reloadDetail()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
