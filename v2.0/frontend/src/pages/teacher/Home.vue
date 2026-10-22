<template>
  <div class="space-y-6">
    <section class="card p-6 flex items-start justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Hello，{{ name }}</h1>
        <p class="text-slate-500 mt-1">欢迎来到学情预警系统</p>
      </div>
      <label class="text-sm">学期
        <select v-model="termId" class="input mt-1 w-56" @change="load">
          <option :value="''">全部学期</option>
          <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
        </select>
      </label>
    </section>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

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
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div v-for="c in sortedCourses" :key="c.offeringId" class="card overflow-hidden hover:shadow-md transition">
          <div class="h-24 bg-gradient-to-r from-brand-500 to-brand-700 flex items-start justify-between p-3">
            <span class="tag bg-white/85 text-slate-700">{{ courseTypeLabel(c.courseType) }}</span>
            <span class="tag" :class="c.allClosed ? 'bg-white/80 text-slate-600' : 'bg-white/90 text-brand-700'">
              {{ c.allClosed ? '已结课' : '进行中' }}
            </span>
          </div>
          <div class="p-5 space-y-1.5">
            <h3 class="text-lg font-semibold text-slate-800 leading-snug">{{ c.courseName }}</h3>
            <p class="text-sm text-slate-500">{{ c.termName }}</p>
            <p class="text-sm text-slate-400">
              负责 {{ c.classCount }} 个教学班 ｜ 学生 {{ c.studentCount }} 人
            </p>
            <div class="flex items-center justify-between pt-3">
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
          <div v-for="it in g.items" :key="g.key + it.id" class="border border-slate-100 rounded-lg p-2 space-y-1">
            <p class="text-sm text-slate-700">{{ it.title }}</p>
            <p class="text-xs text-slate-400">{{ it.courseName }} ｜ {{ it.className }} ｜ 截止 {{ fmt(it.endTime) }}</p>
            <div class="flex items-center justify-between">
              <span class="text-xs text-amber-600">{{ it.pendingCount }} 份待处理</span>
              <button class="text-xs text-brand-600 hover:underline" @click="goGrade(it, g.key)">去批改</button>
            </div>
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
import { myPaperApi } from '../../api/paper'
import { session } from '../../session'
import { courseTypeLabel } from '../../labels'

const router = useRouter()
const name = computed(() => session.realName || '老师')
const terms = ref([])
const termId = ref('')
const courses = ref([])
const pending = ref({ pendingHomeworks: [], pendingQuizzes: [], pendingProjects: [], homeworkPending: 0, quizPending: 0, projectPending: 0 })
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
    pending.value = await teacherPortalApi.home(termId.value || undefined)
    courses.value = pending.value.courses
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
