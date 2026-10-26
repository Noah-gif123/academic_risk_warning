<template>
  <div class="space-y-4">
    <div v-if="!embedded" class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/home')">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">项目与贡献度</h1>
        <span class="tag bg-slate-100 text-slate-500">教学班 #{{ teachingClassId }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      项目只收文件（Word / PDF / 代码 / PPT / 压缩包 / 仓库链接），逾期不允许补交。
      团队项目由组长提交一份贡献表，提交即锁定——**教师也只能查看，不能修改**；个人得分 = 项目总分 × 贡献占比。
    </p>

    <!-- 新建 -->
    <div class="card p-4" v-if="!active">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">新建项目</h2>
        <span class="text-xs text-slate-400">先建草稿，发布后学生可见</span>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
        <label class="text-sm md:col-span-2">项目名称
          <input v-model="form.projectName" class="input mt-1" placeholder="如 课程综合项目：校园信息系统设计" />
        </label>
        <label class="text-sm">截止时间（必填）
          <input v-model="form.endTime" type="datetime-local" class="input mt-1" />
        </label>
        <label class="text-sm">满分
          <input v-model.number="form.fullScore" type="number" class="input mt-1" />
        </label>
        <label class="text-sm md:col-span-2">要求说明
          <input v-model="form.description" class="input mt-1" placeholder="分组要求、提交内容等" />
        </label>
        <label class="text-sm">形式
          <select v-model.number="form.isTeam" class="input mt-1">
            <option :value="0">个人项目</option>
            <option :value="1">团队项目（有贡献表）</option>
          </select>
        </label>
        <label class="text-sm">挂到课时（可选）
          <select v-model="form.lessonId" class="input mt-1">
            <option :value="null">不挂课时</option>
            <option v-for="l in lessons" :key="l.id" :value="l.id">
              第{{ l.weekNo }}周 第{{ l.sessionNo }}次 {{ l.title || '' }}
            </option>
          </select>
        </label>
      </div>
      <div class="mt-3">
        <p class="text-xs text-slate-500 mb-1">允许的提交形式</p>
        <div class="flex flex-wrap gap-x-4 gap-y-1">
          <label v-for="f in formatOptions" :key="f.value" class="flex items-center gap-1 text-sm">
            <input type="checkbox" :value="f.value" v-model="formats" /> {{ f.label }}
          </label>
        </div>
      </div>
      <div class="flex items-center gap-2 mt-3">
        <button class="btn-primary text-sm" @click="create">建立草稿</button>
        <span class="text-xs text-slate-400">允许形式：{{ formats.join(',') || '未选择' }}</span>
      </div>
    </div>

    <!-- 列表 -->
    <div class="card p-4">
      <h2 class="font-medium text-slate-700 mb-3">本班项目（{{ projects.length }}）</h2>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">项目</th><th class="text-left py-2 w-24">形式</th>
            <th class="text-left py-2 w-24">状态</th><th class="text-left py-2 w-32">截止</th>
            <th class="text-right py-2 w-20">满分</th><th class="text-right py-2 w-24">组/已交</th>
            <th class="text-right py-2 w-28">已批</th><th class="text-right py-2 w-40">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in projects" :key="p.id" class="border-b border-slate-100"
              :class="active && active.id === p.id ? 'bg-brand-50/40' : ''">
            <td class="py-2">{{ p.projectName }}
              <span v-if="p.schemeItemName" class="tag bg-brand-50 text-brand-600 ml-1">{{ p.schemeItemName }}</span>
            </td>
            <td class="py-2 text-slate-500">{{ p.isTeam === 1 ? '团队' : '个人' }}</td>
            <td class="py-2">
              <span class="tag" :class="statusClass(p.status)">{{ statusLabel(p.status) }}</span>
            </td>
            <td class="py-2 text-slate-500">{{ fmt(p.endTime) }}</td>
            <td class="py-2 text-right">{{ p.fullScore }}</td>
            <td class="py-2 text-right">{{ p.teamCount }} / {{ p.submitCount }}</td>
            <td class="py-2 text-right">
              {{ p.markedCount }}
              <span v-if="p.missingContributionCount" class="text-amber-600 ml-1" title="还缺少贡献表的组">
                （缺 {{ p.missingContributionCount }} 份贡献表）
              </span>
            </td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline mr-2" @click="open(p)">查看提交</button>
              <button v-if="p.status === 'DRAFT'" class="text-emerald-600 hover:underline mr-2" @click="doPublish(p, true)">发布</button>
              <button v-if="p.status === 'PUBLISHED'" class="text-amber-600 hover:underline mr-2" @click="doPublish(p, false)">撤回</button>
              <button v-if="p.status === 'PUBLISHED'" class="text-slate-500 hover:underline" @click="doClose(p)">截止</button>
            </td>
          </tr>
          <tr v-if="!projects.length"><td colspan="8" class="py-6 text-center text-slate-400">还没有项目</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 明细 -->
    <div class="card p-4 space-y-4" v-if="active">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">{{ active.projectName }} · 提交与评分</h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="active = null">收起</button>
      </div>
      <p class="text-xs text-slate-400">{{ active.description }}</p>

      <!-- 组与贡献表 -->
      <div v-if="active.isTeam === 1">
        <h3 class="text-sm font-medium text-slate-600 mb-2">分组与贡献表（{{ active.teams.length }} 组）</h3>
        <div class="space-y-2">
          <div v-for="t in active.teams" :key="t.id" class="border border-slate-200 rounded-lg p-3">
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-sm font-medium">{{ t.teamName }}</span>
              <span class="text-xs text-slate-400">组长 {{ t.leaderStudentName }} ｜ {{ t.memberCount }} 人</span>
              <span class="tag" :class="t.contributionLocked ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
                {{ t.contributionLocked ? '贡献表已锁定（不可修改）' : '组长未提交贡献表' }}
              </span>
            </div>
            <table class="w-full text-sm mt-2">
              <thead class="text-slate-400">
                <tr>
                  <th class="text-left py-1">成员</th><th class="text-left py-1 w-24">学号</th>
                  <th class="text-right py-1 w-24">贡献占比</th><th class="text-left py-1 w-40">备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in t.members" :key="m.studentId" class="border-t border-slate-100">
                  <td class="py-1">{{ m.studentName }}<span v-if="m.leader" class="tag bg-brand-50 text-brand-600 ml-1">组长</span></td>
                  <td class="py-1 text-slate-500">{{ m.studentNo }}</td>
                  <td class="py-1 text-right">{{ m.ratio == null ? '—' : pct(m.ratio) }}</td>
                  <td class="py-1 text-slate-500">{{ m.note || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-if="!active.teams.length" class="text-sm text-slate-400">还没有学生组队</p>
        </div>
      </div>

      <!-- 提交与评分 -->
      <div>
        <h3 class="text-sm font-medium text-slate-600 mb-2">提交与评分（{{ active.submissions.length }}）</h3>
        <div class="space-y-2">
          <div v-for="s in active.submissions" :key="s.id" class="border border-slate-200 rounded-lg p-3 space-y-2">
            <div class="flex flex-wrap items-center gap-2 text-sm">
              <span class="font-medium">{{ s.teamName || s.studentName }}</span>
              <span class="text-xs text-slate-400">{{ s.teamName ? '组长 ' + s.studentName + ' 提交' : '个人提交' }}</span>
              <span class="text-xs text-slate-400">{{ fmt(s.submitTime) }}</span>
              <span class="tag" :class="s.submitStatus === 'MARKED' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'">
                {{ s.submitStatus === 'MARKED' ? '已批阅' : '已提交' }}
              </span>
              <span v-if="s.markedByName" class="text-xs text-slate-400">判卷人 {{ s.markedByName }}</span>
            </div>
            <div class="text-sm text-slate-600 space-y-1">
              <p v-for="(f, i) in s.files" :key="i">
                📎
                <button class="text-brand-600 hover:underline" @click="$parent">{{ f.name }}</button>
                <span class="text-xs text-slate-400">（{{ sizeText(f.size) }}）</span>
              </p>
              <p v-if="s.repoUrl">🔗 <a :href="s.repoUrl" target="_blank" class="text-brand-600 hover:underline">{{ s.repoUrl }}</a></p>
              <p v-if="s.remark" class="text-xs text-slate-400">提交说明：{{ s.remark }}</p>
            </div>

            <!-- 成员得分 -->
            <table class="w-full text-sm" v-if="s.members && s.members.length > 1">
              <thead class="text-slate-400">
                <tr>
                  <th class="text-left py-1">成员</th><th class="text-right py-1 w-24">占比</th>
                  <th class="text-right py-1 w-24">个人得分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in s.members" :key="m.studentId" class="border-t border-slate-100">
                  <td class="py-1">{{ m.studentName }}<span v-if="m.leader" class="tag bg-brand-50 text-brand-600 ml-1">组长</span></td>
                  <td class="py-1 text-right">{{ m.ratio == null ? '—' : pct(m.ratio) }}</td>
                  <td class="py-1 text-right font-medium">{{ m.personalScore == null ? '—' : m.personalScore }}</td>
                </tr>
              </tbody>
            </table>

            <div class="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100">
              <span class="text-xs text-slate-400">当前得分</span>
              <span class="text-sm font-medium">{{ s.projectScore == null ? '未评分' : s.projectScore + ' 分' }}</span>
              <span class="text-xs text-slate-400" v-if="s.comment">评语：{{ s.comment }}</span>
              <button class="btn-primary text-sm ml-auto" @click="openGrade(s)">
                {{ s.projectScore == null ? '评分' : '修改评分' }}
              </button>
            </div>
          </div>
          <p v-if="!active.submissions.length" class="text-sm text-slate-400">还没有提交</p>
        </div>
      </div>
    </div>

    <!-- 项目评分弹窗（独立弹窗，不再在列表里内联给分） -->
    <Teleport to="body">
      <div v-if="grading" class="fixed inset-0 z-50 bg-slate-900/45 flex items-center justify-center p-4"
           @click.self="grading = null">
        <div class="bg-white rounded-xl shadow-2xl w-full max-w-xl max-h-[85vh] flex flex-col">
          <div class="flex items-center justify-between px-5 py-3 border-b border-slate-200">
            <div class="flex items-center gap-2 flex-wrap min-w-0">
              <h2 class="font-semibold text-slate-800 truncate">{{ grading.teamName || grading.studentName }}</h2>
              <span class="tag bg-brand-50 text-brand-600 truncate max-w-56">{{ active.projectName }}</span>
              <span class="tag" :class="grading.submitStatus === 'MARKED'
                    ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'">
                {{ grading.submitStatus === 'MARKED' ? '已批阅' : '已提交' }}
              </span>
            </div>
            <button class="text-slate-400 hover:text-slate-600 text-lg leading-none px-1"
                    title="关闭（Esc）" @click="grading = null">✕</button>
          </div>

          <div class="flex-1 overflow-y-auto px-5 py-4 space-y-3 text-sm">
            <p class="text-xs text-slate-400">
              {{ grading.teamName ? '团队提交（组长 ' + grading.studentName + '）' : '个人提交' }}
              ｜ 提交时间 {{ fmt(grading.submitTime) }}
              <span v-if="grading.markedByName">｜ 判卷人 {{ grading.markedByName }}</span>
            </p>

            <div class="space-y-1 text-slate-600">
              <p v-for="(f, i) in grading.files" :key="i">📎 {{ f.name }}
                <span class="text-xs text-slate-400">（{{ sizeText(f.size) }}）</span>
              </p>
              <p v-if="grading.repoUrl">🔗 <a :href="grading.repoUrl" target="_blank" class="text-brand-600 hover:underline">{{ grading.repoUrl }}</a></p>
              <p v-if="grading.remark" class="text-xs text-slate-400">提交说明：{{ grading.remark }}</p>
              <p v-if="!grading.files.length && !grading.repoUrl" class="text-xs text-slate-400">没有上传文件</p>
            </div>

            <table v-if="grading.members && grading.members.length > 1" class="w-full text-sm">
              <thead class="text-slate-400">
                <tr>
                  <th class="text-left py-1">成员</th><th class="text-right py-1 w-24">占比</th>
                  <th class="text-right py-1 w-24">个人得分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in grading.members" :key="m.studentId" class="border-t border-slate-100">
                  <td class="py-1">{{ m.studentName }}<span v-if="m.leader" class="tag bg-brand-50 text-brand-600 ml-1">组长</span></td>
                  <td class="py-1 text-right">{{ m.ratio == null ? '—' : pct(m.ratio) }}</td>
                  <td class="py-1 text-right font-medium">{{ m.personalScore == null ? '—' : m.personalScore }}</td>
                </tr>
              </tbody>
            </table>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
              <label class="text-xs">项目总分（百分制）
                <input v-model.number="grading.score" type="number" min="0" max="100" class="input mt-1 w-full" />
              </label>
              <label class="text-xs md:col-span-2">评语
                <input v-model="grading.comment" class="input mt-1 w-full" placeholder="可选，学生可见" />
              </label>
            </div>
            <p class="text-xs text-slate-400">
              团队项目按贡献占比折算个人得分：等分时等于团队分，占比高的按比例上浮（封顶 100）。
            </p>
          </div>

          <div class="border-t border-slate-200 px-5 py-3 flex items-center justify-end gap-2">
            <button class="btn-ghost text-sm" @click="grading = null">关闭</button>
            <button class="btn-primary text-sm" :disabled="gradingSaving" @click="saveGrade">
              {{ gradingSaving ? '保存中…' : '保存评分' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { projectApi, formatOptions } from '../../api/project'
import { contentApi } from '../../api/content'

const route = useRoute()
const props = defineProps({
  teachingClassId: { type: Number, default: 0 },
  embedded: { type: Boolean, default: false }
})
const teachingClassId = props.teachingClassId || Number(route.query.teachingClassId || 1)

const projects = ref([])
const lessons = ref([])
const active = ref(null)
const formats = ref(['WORD', 'PDF', 'CODE', 'PPT', 'ZIP', 'LINK'])
const drafts = reactive({})
const message = ref('')
const ok = ref(true)

const form = reactive({
  projectName: '', description: '', lessonId: null, endTime: '', isTeam: 0, fullScore: 100
})

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function statusLabel(s) { return { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已截止' }[s] || s }
function statusClass(s) {
  return { DRAFT: 'bg-slate-100 text-slate-500', PUBLISHED: 'bg-emerald-50 text-emerald-600', CLOSED: 'bg-slate-200 text-slate-600' }[s]
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '不限' }
function pct(r) { return Math.round(Number(r) * 10000) / 100 + '%' }
function sizeText(n) {
  const v = Number(n || 0)
  if (v < 1024) return v + ' B'
  if (v < 1024 * 1024) return (v / 1024).toFixed(1) + ' KB'
  return (v / 1024 / 1024).toFixed(1) + ' MB'
}
function datetimeLocalToIso(v) { return v ? (v.length === 16 ? v + ':00' : v) : null }

async function load() {
  projects.value = await projectApi.list(teachingClassId)
  lessons.value = await contentApi.lessons(teachingClassId)
  if (active.value) active.value = projects.value.find((p) => p.id === active.value.id) || null
}

async function create() {
  if (!form.projectName) { toast('请填写项目名称', false); return }
  if (!form.endTime) { toast('项目逾期不允许补交，必须设置截止时间', false); return }
  if (!formats.value.length) { toast('至少选择一种提交形式', false); return }
  try {
    await projectApi.save({
      teachingClassId, projectName: form.projectName, description: form.description,
      lessonId: form.lessonId, endTime: datetimeLocalToIso(form.endTime),
      isTeam: form.isTeam, fullScore: form.fullScore, allowFormats: formats.value.join(',')
    })
    toast('草稿已建立')
    form.projectName = ''
    form.description = ''
    await load()
  } catch (e) { toast(e.message, false) }
}

async function open(p) {
  active.value = await projectApi.get(p.id)
  ;(active.value.submissions || []).forEach((s) => {
    drafts[s.id] = { score: s.projectScore == null ? '' : Number(s.projectScore), comment: s.comment || '' }
  })
}

async function doPublish(p, publish) {
  try {
    await projectApi.publish(p.id, publish)
    toast(publish ? '已发布' : '已撤回')
    await load()
  } catch (e) { toast(e.message, false) }
}
async function doClose(p) {
  try {
    await projectApi.close(p.id)
    toast('已截止')
    await load()
  } catch (e) { toast(e.message, false) }
}

// ---------------- 评分弹窗
const grading = ref(null)
const gradingSaving = ref(false)

function openGrade(s) {
  grading.value = {
    id: s.id,
    teamName: s.teamName,
    studentName: s.studentName,
    submitTime: s.submitTime,
    submitStatus: s.submitStatus,
    markedByName: s.markedByName,
    files: s.files || [],
    repoUrl: s.repoUrl,
    remark: s.remark,
    members: s.members || [],
    score: s.projectScore == null ? '' : Number(s.projectScore),
    comment: s.comment || ''
  }
}

async function saveGrade() {
  if (!grading.value) return
  const score = grading.value.score
  if (score === '' || Number.isNaN(Number(score))) {
    toast('请填写项目总分', false)
    return
  }
  gradingSaving.value = true
  try {
    const res = await projectApi.grade(grading.value.id, { score: Number(score), comment: grading.value.comment })
    toast('评分已保存：项目 ' + res.projectScore + ' 分')
    const keepId = grading.value.id
    const projectId = active.value.id
    active.value = await projectApi.get(projectId)
    ;(active.value.submissions || []).forEach((x) => {
      drafts[x.id] = { score: x.projectScore == null ? '' : Number(x.projectScore), comment: x.comment || '' }
    })
    const updated = (active.value.submissions || []).find((x) => x.id === keepId)
    if (updated) openGrade(updated)
    await load()
  } catch (e) { toast(e.message, false) } finally { gradingSaving.value = false }
}

function onGradingKey(e) {
  if (e.key === 'Escape' && grading.value && !gradingSaving.value) grading.value = null
}

onMounted(async () => {
  window.addEventListener('keydown', onGradingKey)
  try { await load() } catch (e) { toast(e.message, false) }
})

onBeforeUnmount(() => window.removeEventListener('keydown', onGradingKey))

// 评分弹窗打开时锁住页面滚动
watchEffect(() => {
  document.body.style.overflow = grading.value ? 'hidden' : ''
})
</script>
