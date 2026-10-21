<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/student/courses')">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">{{ d ? d.courseName : '加载中…' }}</h1>
        <span v-if="d" class="tag bg-slate-100 text-slate-500">{{ d.teacherName }} ｜ {{ d.termName }}</span>
        <span v-if="d" class="tag" :class="d.statusText === '进行中' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-200 text-slate-600'">
          {{ d.statusText }}
        </span>
      </div>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
    </div>

    <!-- 页签 -->
    <div class="flex flex-wrap gap-2" v-if="d">
      <button v-for="t in visibleTabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key">{{ t.label }}</button>
    </div>

    <!-- 导学 -->
    <div class="card p-5 space-y-2" v-if="d && tab === 'guide'">
      <h2 class="font-medium text-slate-700">课程导学</h2>
      <p v-if="!d.guideContent" class="text-sm text-slate-400">老师还没有发布导学内容。</p>
      <div v-else>
        <div v-if="d.guideFormat === 'HTML'" class="text-sm leading-7" v-html="d.guideContent"></div>
        <pre v-else class="text-sm whitespace-pre-wrap">{{ d.guideContent }}</pre>
        <p class="text-xs text-slate-400 mt-3">发布于 {{ fmt(d.guidePublishTime) }}</p>
      </div>
    </div>

    <!-- 教学目标 -->
    <div class="card p-4" v-if="d && tab === 'objectives'">
      <h2 class="font-medium text-slate-700 mb-3">教学目标</h2>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2 w-16">序号</th><th class="text-left py-2 w-28">编号</th>
            <th class="text-left py-2">内容</th><th class="text-left py-2 w-28">类型</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(o, i) in d.objectives" :key="i" class="border-b border-slate-100">
            <td class="py-2">{{ i + 1 }}</td>
            <td class="py-2 text-slate-500">{{ o.objCode }}</td>
            <td class="py-2 truncate max-w-xl" :title="o.objContent">{{ o.objContent }}</td>
            <td class="py-2 text-slate-500">{{ o.objType || '—' }}</td>
          </tr>
          <tr v-if="!d.objectives.length"><td colspan="4" class="py-6 text-center text-slate-400">老师还没有填写教学目标</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 目录 -->
    <div class="card p-4 space-y-2" v-if="d && tab === 'catalog'">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">课程目录（{{ d.lessons.length }} 次课）</h2>
        <span class="text-xs text-slate-400">点开某一次课，按 课前 / 课中 / 课后 查看</span>
      </div>
      <div class="space-y-2">
        <div v-for="l in d.lessons" :key="l.lessonId" class="border border-slate-200 rounded-lg">
          <div class="flex flex-wrap items-center gap-3 px-3 py-2 cursor-pointer hover:bg-slate-50"
               @click="expanded = expanded === l.lessonId ? null : l.lessonId">
            <span class="text-xs text-slate-400 w-28">第 {{ l.weekNo }} 周 · 第 {{ l.sessionNo }} 次</span>
            <span class="text-xs text-slate-500 w-24">{{ l.sessionDate }}</span>
            <span class="text-sm flex-1 min-w-48">{{ l.title || '（本次课未填写标题）' }}</span>
            <span class="text-xs text-slate-400">
              课前 {{ l.before.length }} ｜ 课中 {{ l.during.length }} ｜ 课后 {{ l.after.length }}
            </span>
          </div>
          <div v-if="expanded === l.lessonId" class="border-t border-slate-100 p-3 space-y-3 bg-slate-50/50">
            <div v-for="ph in phases" :key="ph.value" class="space-y-1">
              <p class="text-xs font-medium text-slate-600">{{ ph.label }}</p>
              <div v-for="r in l[ph.key]" :key="r.id"
                   class="flex flex-wrap items-center gap-2 text-sm bg-white border border-slate-200 rounded-lg px-3 py-1.5">
                <span class="tag bg-slate-100 text-slate-500">{{ typeLabel(r.resType) }}</span>
                <span class="flex-1 min-w-32 truncate">{{ r.title }}</span>
                <button v-if="r.fileId" class="text-xs text-brand-600 hover:underline"
                        @click="download(r)">下载</button>
                <a v-if="r.resType === 'LINK'" :href="r.extUrl" target="_blank" class="text-xs text-brand-600 hover:underline">打开</a>
                <template v-if="r.resType === 'PAPER'">
                  <span class="text-xs" :class="r.taskStatus === 'MARKED' ? 'text-emerald-600' : 'text-amber-600'">
                    {{ paperStatus(r.taskStatus) }}
                  </span>
                </template>
              </div>
              <p v-if="!l[ph.key].length" class="text-xs text-slate-300">暂无</p>
            </div>
          </div>
        </div>
        <p v-if="!d.lessons.length" class="py-6 text-center text-slate-400 text-sm">老师还没有建立目录</p>
      </div>
    </div>

    <!-- 测验 / 作业 -->
    <div class="card p-4" v-if="d && (tab === 'quiz' || tab === 'homework')">
      <h2 class="font-medium text-slate-700 mb-3">{{ tab === 'quiz' ? '随堂测验' : '课后作业' }}</h2>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">名称</th><th class="text-left py-2 w-40">开始 / 截止</th>
            <th class="text-right py-2 w-20">题量</th><th class="text-right py-2 w-20">满分</th>
            <th class="text-left py-2 w-24">状态</th><th class="text-right py-2 w-24">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in filteredPapers" :key="p.id" class="border-b border-slate-100">
            <td class="py-2">{{ p.paperName }}</td>
            <td class="py-2 text-slate-500 text-xs">{{ fmt(p.startTime) }}<br />{{ fmt(p.endTime) }}</td>
            <td class="py-2 text-right">{{ p.questionCount }}</td>
            <td class="py-2 text-right">{{ p.fullScore }}</td>
            <td class="py-2">
              <span class="tag" :class="paperTagClass(p)">{{ paperStatusText(p) }}</span>
            </td>
            <td class="py-2 text-right">
              <button v-if="p.canAnswer" class="text-brand-600 hover:underline"
                      @click="$router.push({ path: '/student/take', query: { paperId: p.id } })">开始作答</button>
              <button v-else-if="p.myStatus !== 'NOT_STARTED'" class="text-slate-500 hover:underline"
                      @click="$router.push({ path: '/student/take', query: { paperId: p.id } })">查看结果</button>
              <span v-else class="text-xs text-slate-400">{{ p.reason }}</span>
            </td>
          </tr>
          <tr v-if="!filteredPapers.length"><td colspan="6" class="py-6 text-center text-slate-400">暂无{{ tab === 'quiz' ? '测验' : '作业' }}</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 项目 -->
    <div class="card p-4" v-if="d && tab === 'project'">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">项目</h2>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/student/projects')">去提交项目</button>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">项目</th><th class="text-left py-2 w-24">形式</th>
            <th class="text-left py-2 w-40">截止</th><th class="text-right py-2 w-20">满分</th>
            <th class="text-left py-2 w-28">我的状态</th><th class="text-right py-2 w-24">得分</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in d.projects" :key="p.id" class="border-b border-slate-100">
            <td class="py-2">{{ p.projectName }}</td>
            <td class="py-2 text-slate-500">{{ p.isTeam === 1 ? '团队' : '个人' }}</td>
            <td class="py-2 text-slate-500 text-xs">{{ fmt(p.endTime) }}</td>
            <td class="py-2 text-right">{{ p.fullScore }}</td>
            <td class="py-2">
              <span class="tag" :class="p.myStatus === 'MARKED' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'">
                {{ p.myStatus === 'MARKED' ? '已批阅' : (p.myStatus === 'SUBMITTED' ? '已提交' : '未提交') }}
              </span>
            </td>
            <td class="py-2 text-right">{{ p.myScore == null ? '—' : p.myScore }}</td>
          </tr>
          <tr v-if="!d.projects.length"><td colspan="6" class="py-6 text-center text-slate-400">暂无项目</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { portalApi } from '../../api/portal'
import { http } from '../../api/http'

const route = useRoute()
const teachingClassId = Number(route.params.teachingClassId || route.query.teachingClassId || 0)

const d = ref(null)
const error = ref('')
const tab = ref('guide')
const expanded = ref(null)

const phases = [
  { value: 'BEFORE', key: 'before', label: '课前预习' },
  { value: 'DURING', key: 'during', label: '课中随堂' },
  { value: 'AFTER', key: 'after', label: '课后作业' }
]
const allTabs = [
  { key: 'guide', label: '导学', flag: 'showGuide' },
  { key: 'objectives', label: '教学目标', flag: 'showObjectives' },
  { key: 'catalog', label: '目录', flag: 'showCatalog' },
  { key: 'quiz', label: '测验', flag: 'showQuiz' },
  { key: 'homework', label: '作业', flag: 'showHomework' },
  { key: 'project', label: '项目', flag: 'showProject' }
]
const visibleTabs = computed(() => (d.value ? allTabs.filter((t) => d.value[t.flag] !== false) : []))
const filteredPapers = computed(() => {
  if (!d.value) return []
  const want = tab.value === 'quiz' ? 'QUIZ' : 'HOMEWORK'
  return (d.value.papers || []).filter((p) => p.paperType === want)
})

function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function typeLabel(t) { return { FILE: '文件', LINK: '外链', TEXT: '富文本', PAPER: '测验/作业', PROJECT: '项目' }[t] || t }
function paperStatus(st) { return { NOT_STARTED: '待完成', SUBMITTED: '已交卷', MARKED: '已批阅', ABSENT: '缺考' }[st] || st }
function paperStatusText(p) {
  if (p.myStatus === 'ABSENT') return '缺考（0 分）'
  if (p.myStatus === 'MARKED') return '已批阅' + (p.myScore != null ? ' · ' + p.myScore : '')
  if (p.myStatus === 'SUBMITTED') return '已交卷待批'
  return p.canAnswer ? '待作答' : (p.reason || '未开始')
}
function paperTagClass(p) {
  if (p.myStatus === 'ABSENT') return 'bg-red-50 text-red-600'
  if (p.myStatus === 'MARKED') return 'bg-emerald-50 text-emerald-600'
  if (p.canAnswer) return 'bg-brand-50 text-brand-600'
  return 'bg-slate-100 text-slate-500'
}
async function download(r) {
  try { await http.download(`/files/${r.fileId}`, r.title) }
  catch (e) { error.value = e.message }
}

onMounted(async () => {
  try { d.value = await portalApi.detail(teachingClassId) }
  catch (e) { error.value = e.message }
})
</script>
