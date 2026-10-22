<template>
  <div class="space-y-5">
    <!-- 欢迎区 + 本学期概况（概览并入欢迎卡，课程区独占整行） -->
    <section class="card px-6 py-5">
      <div class="flex items-end justify-between flex-wrap gap-4">
        <div>
          <h1 class="text-2xl font-semibold">Hello，{{ name }}</h1>
          <p class="text-slate-500 mt-1">欢迎来到学情预警系统</p>
        </div>
        <label class="text-sm">学期
          <select v-model="termId" class="input mt-1 w-52" @change="reload">
            <option :value="''">全部学期</option>
            <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
          </select>
        </label>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-5 gap-3 mt-4 pt-4 border-t border-slate-100">
        <div>
          <p class="text-xs text-slate-400">本学期课程</p>
          <p class="text-xl font-semibold text-slate-800">{{ total }} 门</p>
        </div>
        <div>
          <p class="text-xs text-slate-400">进行中 / 未开课</p>
          <p class="text-xl font-semibold text-slate-800">{{ runningCount }} / {{ notStartedCount }}</p>
        </div>
        <div>
          <p class="text-xs text-slate-400">待提交项目</p>
          <p class="text-xl font-semibold text-amber-600">{{ pendingProjectCount }}</p>
        </div>
        <div>
          <p class="text-xs text-slate-400">最近截止</p>
          <p class="text-sm text-slate-600 truncate" :title="nearestProject ? nearestProject.projectName : ''">
            {{ nearestProject ? nearestProject.projectName + '（' + fmt(nearestProject.endTime) + '）' : '暂无' }}
          </p>
        </div>
        <div>
          <p class="text-xs text-slate-400">快捷入口</p>
          <div class="flex flex-wrap gap-x-3 gap-y-1 text-xs mt-1">
            <button class="text-brand-600 hover:underline" @click="$router.push('/student/exams')">考试作业</button>
            <button class="text-brand-600 hover:underline" @click="$router.push('/student/attendance')">考勤</button>
            <button class="text-brand-600 hover:underline" @click="$router.push('/student/scores')">成绩</button>
            <button class="text-brand-600 hover:underline" @click="$router.push('/student/questionnaires')">问卷</button>
          </div>
        </div>
      </div>
    </section>

    <!-- 我的课程：整行展示，最多 4 门 -->
    <section class="space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-semibold text-slate-700">我的课程（{{ total }}）</h2>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/student/courses')">
          更多（全部课程 + 加入课程）
        </button>
      </div>

      <div v-if="error" class="card p-4 text-sm text-red-600">{{ error }}</div>
      <div v-else-if="!cards.length" class="card p-10 text-center text-slate-400 text-sm">
        本学期暂无课程。可以到「课程学习」输入邀请码加入课程。
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 max-h-[620px] overflow-y-auto pr-1">
        <div v-for="c in cards" :key="c.teachingClassId" class="card overflow-hidden flex flex-col hover:shadow-md transition">
          <div class="h-32 bg-gradient-to-r from-brand-500 to-brand-700 flex items-start justify-between p-3">
            <span class="tag bg-white/85 text-slate-700">{{ courseTypeLabel(c.courseType) }}</span>
            <span class="tag" :class="c.status === '进行中' ? 'bg-white/90 text-brand-700' : 'bg-white/70 text-slate-600'">
              {{ c.status }}
            </span>
          </div>
          <div class="p-4 pb-5 flex-1 flex flex-col justify-between">
            <div class="space-y-2">
              <h3 class="text-base font-semibold text-slate-800 leading-snug">{{ c.courseName }}</h3>
              <p class="text-sm text-slate-500">{{ c.termName }}</p>
              <p class="text-sm text-slate-400">{{ c.teacherName }}</p>
              <p class="text-sm text-slate-400">{{ c.className }}</p>
            </div>
            <div class="flex items-center justify-between mt-5 pt-3 border-t border-slate-100">
              <span class="text-xs text-slate-400">{{ c.canEnter ? '可以进入学习' : '暂不可进入' }}</span>
              <button class="text-sm text-brand-600 hover:underline font-medium"
                      :disabled="!c.canEnter"
                      @click="$router.push({ path: '/student/course/' + c.teachingClassId })">开始学习 →</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 我的项目 -->
    <section>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-base font-semibold text-slate-700">我的项目（{{ projects.length }}）</h2>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/student/projects')">全部项目</button>
      </div>
      <div v-if="!projects.length" class="card p-8 text-center text-slate-400 text-sm">暂无需要提交的项目。</div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <div v-for="p in projects" :key="p.id" class="card p-4 space-y-2">
          <div class="flex items-start justify-between gap-2">
            <h3 class="font-medium text-slate-800">{{ p.projectName }}</h3>
            <span class="tag" :class="p.isTeam === 1 ? 'bg-brand-50 text-brand-600' : 'bg-slate-100 text-slate-500'">
              {{ p.isTeam === 1 ? '团队' : '个人' }}
            </span>
          </div>
          <p class="text-xs text-slate-500">{{ p.courseName }} ｜ 发起人 {{ p.teacherName }}</p>
          <p class="text-xs text-slate-400">起止 {{ fmt(p.startTime) }} ~ {{ fmt(p.endTime) }} ｜ 满分 {{ p.fullScore }}</p>
          <div class="flex items-center justify-between">
            <span class="text-xs" :class="projectClass(p)">{{ statusText(p) }}</span>
            <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/student/projects')">进入</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { portalApi } from '../../api/portal'
import { myProjectApi } from '../../api/project'
import { session } from '../../session'
import { courseTypeLabel } from '../../labels'

const name = computed(() => session.realName || '同学')
const terms = ref([])
const termId = ref('')
const cards = ref([])
const total = ref(0)
const projects = ref([])
const error = ref('')

const runningCount = computed(() => cards.value.filter((c) => c.status === '进行中').length)
const notStartedCount = computed(() => cards.value.filter((c) => c.status === '未开课').length)
const pendingProjectCount = computed(() => projects.value.filter((p) => !p.mySubmission).length)
const nearestProject = computed(() => {
  const list = projects.value.filter((p) => !p.mySubmission && p.endTime)
  list.sort((a, b) => String(a.endTime).localeCompare(String(b.endTime)))
  return list[0] || null
})

function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function projectClass(p) {
  const s = p.mySubmission && p.mySubmission.submitStatus
  if (s === 'MARKED') return 'text-emerald-600'
  if (s === 'SUBMITTED') return 'text-amber-600'
  return 'text-slate-500'
}
function statusText(p) {
  const s = p.mySubmission && p.mySubmission.submitStatus
  if (s === 'MARKED') return '已批阅 · ' + (p.mySubmission.projectScore ?? '—') + ' 分'
  if (s === 'SUBMITTED') return '已提交，待批阅'
  return p.isTeam === 1 && !p.myTeam ? '需要先组队' : '未提交'
}

async function reload() {
  error.value = ''
  try {
    const page = await portalApi.cards({ termId: termId.value || undefined, page: 1, size: 4 })
    cards.value = page.items
    total.value = page.total
    projects.value = await myProjectApi.list()
  } catch (e) { error.value = e.message }
}

onMounted(async () => {
  try {
    terms.value = await portalApi.terms()
    const cur = terms.value.find((t) => t.isCurrent === 1 || t.current)
    if (cur) termId.value = cur.id
    await reload()
  } catch (e) { error.value = e.message }
})
</script>
