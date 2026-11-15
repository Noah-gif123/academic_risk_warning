<template>
  <div class="space-y-4">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/courses')">← 全部课程</button>
          <h1>{{ card ? card.courseName : '课程详情' }}</h1>
          <span v-if="card" class="tag bg-white text-slate-500 border border-[#e6edf5]">{{ card.termName }}</span>
          <span v-if="card" class="tag bg-[#e9f6f2] text-[#0b7d6e]">{{ courseTypeLabel(card.courseType) }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2">{{ card ? card.classCount + ' 个教学班 · ' + card.studentCount + ' 名学生' : '课程教学与考核' }}</p>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" @click="$router.push('/teacher/reports')">学情报告</button>
      </div>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span><p class="text-[11px] text-slate-400">教学班</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ classes.length }}</p><p class="text-[11px] text-slate-400 mt-2">本人任教</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">学生</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ card?.studentCount || 0 }}</p><p class="text-[11px] text-slate-400 mt-2">选课人数</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-indigo-300"></span><p class="text-[11px] text-slate-400">进行中</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ activeClasses }}</p><p class="text-[11px] text-slate-400 mt-2">未结课教学班</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-300"></span><p class="text-[11px] text-slate-400">已选考核班</p><p class="text-[28px] leading-none font-semibold text-amber-600 mt-3">{{ selectedClassId || '—' }}</p><p class="text-[11px] text-slate-400 mt-2">统计/考核对象</p></div>
    </section>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-[#0f9d8a] text-white shadow-sm' : 'bg-white text-slate-600 border border-[#e6edf5]'"
              @click="tab = t.key">{{ t.label }}</button>
    </div>

    <!-- 教学班级 -->
    <div class="card p-5" v-if="tab === 'classes'">
      <h2 class="font-medium text-slate-700 mb-3">教学班级</h2>
      <div class="overflow-x-auto border border-[#e6edf5] rounded-xl">
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">班级编号</th><th class="text-left py-2">班级名称</th>
            <th class="text-right py-2">学生数</th><th class="text-right py-2">有效课次</th>
            <th class="text-left py-2">状态</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in classes" :key="c.teachingClassId" class="border-b border-slate-100">
            <td class="py-2">{{ c.classCode }}</td>
            <td class="py-2">{{ c.className }}</td>
            <td class="py-2 text-right">{{ c.studentCount }}</td>
            <td class="py-2 text-right">{{ c.effectiveSessions || '未确认' }}</td>
            <td class="py-2">
              <span class="tag" :class="c.status === 'CLOSED' ? 'bg-slate-200 text-slate-600' : 'bg-emerald-50 text-emerald-600'">
                {{ c.status === 'CLOSED' ? '已结课' : '进行中' }}
              </span>
            </td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline mr-3"
                      @click="$router.push({ path: '/teacher/teaching', query: { teachingClassId: c.teachingClassId, offeringId } })">
                开始教学
              </button>
              <button class="text-slate-500 hover:underline mr-3"
                      @click="$router.push({ path: '/teacher/attendance', query: { teachingClassId: c.teachingClassId } })">考勤</button>
              <button class="text-slate-500 hover:underline mr-3"
                      @click="selectClass(c.teachingClassId); tab = 'stats'">教学统计</button>
              <button class="text-slate-500 hover:underline"
                      @click="selectClass(c.teachingClassId); tab = 'exam'">课程考核</button>
            </td>
          </tr>
          <tr v-if="!classes.length"><td colspan="6" class="py-6 text-center text-slate-400">没有教学班</td></tr>
        </tbody>
      </table>
      </div>
    </div>

    <!-- 教学统计 -->
    <div class="space-y-3" v-if="tab === 'stats'">
      <div class="card p-4 flex flex-wrap items-end justify-between gap-3">
        <label class="text-sm">教学班
          <select v-model="selectedClassId" class="input mt-1 w-56" @change="loadStats">
            <option :value="0">请选择</option>
            <option v-for="c in classes" :key="c.teachingClassId" :value="c.teachingClassId">
              {{ c.classCode }} · {{ c.className }}
            </option>
          </select>
        </label>
        <div class="flex items-center gap-2">
          <button class="btn-ghost text-sm" @click="exportStudents">导出学生数据</button>
          <button class="btn-ghost text-sm" @click="$router.push('/teacher/reports')">查看学情报告</button>
        </div>
      </div>

      <div v-if="stats" class="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div class="card p-4">
          <p class="text-xs text-slate-400">今日学生活跃度</p>
          <p class="text-2xl font-semibold text-slate-800">{{ stats.activeToday }}<span class="text-sm text-slate-400"> / {{ stats.studentCount }}</span></p>
          <p class="text-xs text-slate-400">占比 {{ stats.activeRate ?? '—' }}%</p>
          <p class="text-xs text-slate-400 mt-1">口径：学生账号今日最后登录时间落在今天即计入（登录即视为活跃）</p>
        </div>
        <div class="card p-4">
          <p class="text-xs text-slate-400">学生整体出勤率</p>
          <p class="text-2xl font-semibold text-slate-800">{{ stats.attendanceRate ?? '—' }}%</p>
          <p class="text-xs text-slate-400">已点名 {{ stats.calledSessions }} / {{ stats.effectiveSessions ?? '—' }} 次</p>
        </div>
        <div class="card p-4">
          <p class="text-xs text-slate-400">任务完成度（已发布）</p>
          <p class="text-2xl font-semibold text-slate-800">{{ totalTasks }}</p>
          <p class="text-xs text-slate-400">资料 {{ stats.materialCount }} ｜ 测验 {{ stats.quizCount }} ｜ 作业 {{ stats.homeworkCount }} ｜ 项目 {{ stats.projectCount }}</p>
        </div>
        <div class="card p-4">
          <p class="text-xs text-slate-400">课程平均成绩</p>
          <p class="text-2xl font-semibold text-slate-800">{{ stats.avgScore ?? '未定稿' }}</p>
          <p class="text-xs text-slate-400">已定稿 {{ stats.scoredCount }} 人 ｜ 通过率 {{ stats.passRate ?? '—' }}%</p>
        </div>
      </div>

      <div v-if="stats" class="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div class="card p-4">
          <h3 class="text-sm font-medium text-slate-700 mb-2">测验完成率</h3>
          <p class="text-xs text-slate-500">未开始 {{ stats.notStartedQuiz }} ｜ 进行中 {{ stats.runningQuiz }} ｜ 已截止 {{ stats.finishedQuiz }}</p>
        </div>
        <div class="card p-4">
          <h3 class="text-sm font-medium text-slate-700 mb-2">作业完成率</h3>
          <p class="text-xs text-slate-500">未开始 {{ stats.notStartedHomework }} ｜ 进行中 {{ stats.runningHomework }} ｜ 已截止 {{ stats.finishedHomework }}</p>
        </div>
        <div class="card p-4">
          <h3 class="text-sm font-medium text-slate-700 mb-2">项目完成率</h3>
          <p class="text-xs text-slate-500">未开始 {{ stats.notStartedProject }} ｜ 进行中 {{ stats.runningProject }} ｜ 已截止 {{ stats.finishedProject }}</p>
        </div>
      </div>
    </div>

    <!-- 课程考核 -->
    <div class="space-y-3" v-if="tab === 'exam'">
      <div class="card p-4 flex flex-wrap items-end justify-between gap-3">
        <label class="text-sm">教学班
          <select v-model="selectedClassId" class="input mt-1 w-56" @change="loadStats">
            <option :value="0">请选择</option>
            <option v-for="c in classes" :key="c.teachingClassId" :value="c.teachingClassId">
              {{ c.classCode }} · {{ c.className }}
            </option>
          </select>
        </label>
        <div class="flex items-center gap-2">
          <button class="btn-primary text-sm"
                  @click="$router.push({ path: '/teacher/score-scheme', query: { offeringId } })">规则设置（成绩构成）</button>
          <span class="text-xs text-slate-400">自动型评分项由测验/作业/项目批改后自动入分，手工型由你录入</span>
        </div>
      </div>
      <div v-if="!selectedClassId" class="card p-10 text-center text-slate-400 text-sm">请先选择教学班</div>
      <Scores v-else :teaching-class-id="selectedClassId" embedded initial-tab="matrix" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { teacherPortalApi } from '../../api/teacherPortal'
import { courseTypeLabel } from '../../labels'
import Scores from './Scores.vue'

const route = useRoute()
const offeringId = Number(route.params.offeringId || route.query.offeringId || 0)

const tabs = [
  { key: 'classes', label: '教学班级' },
  { key: 'stats', label: '教学统计' },
  { key: 'exam', label: '课程考核' }
]
const tab = ref('classes')
const home = ref(null)
const stats = ref(null)
const selectedClassId = ref(0)
const message = ref('')
const ok = ref(true)

const card = computed(() => (home.value ? (home.value.courses || []).find((c) => c.offeringId === offeringId) : null))
const classes = computed(() => (card.value ? card.value.classes || [] : []))
const activeClasses = computed(() => classes.value.filter(c => c.status !== 'CLOSED').length)
const totalTasks = computed(() => {
  if (!stats.value) return 0
  return (stats.value.materialCount || 0) + (stats.value.quizCount || 0)
    + (stats.value.homeworkCount || 0) + (stats.value.projectCount || 0)
})

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function selectClass(id) { selectedClassId.value = id }
async function loadStats() {
  if (!selectedClassId.value) return
  try { stats.value = await teacherPortalApi.stats(selectedClassId.value) }
  catch (e) { toast(e.message, false) }
}
async function exportStudents() {
  if (!selectedClassId.value) { toast('请先选择教学班', false); return }
  try { await teacherPortalApi.exportStudents(selectedClassId.value) }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    home.value = await teacherPortalApi.home()
    if (classes.value.length === 1) {
      selectedClassId.value = classes.value[0].teachingClassId
      await loadStats()
    }
  } catch (e) { toast(e.message, false) }
})
</script>
