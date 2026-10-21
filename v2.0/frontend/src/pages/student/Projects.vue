<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/student/home')">← 返回首页</button>
        <h1 class="text-lg font-semibold text-slate-800">我的项目</h1>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      项目提交以文件为主（Word / PDF / 代码 / PPT / 压缩包 / 仓库链接），逾期不允许补交。
      团队项目由组长提交，贡献表也只能由组长提交一次，提交后不可修改。
    </p>

    <div v-if="!items.length" class="card p-10 text-center text-slate-400 text-sm">
      暂时没有项目。老师发布后这里会出现卡片。
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div v-for="p in items" :key="p.id" class="card p-4 space-y-2">
        <div class="flex items-start justify-between gap-2">
          <h3 class="font-medium text-slate-800">{{ p.projectName }}</h3>
          <span class="tag" :class="p.isTeam === 1 ? 'bg-brand-50 text-brand-600' : 'bg-slate-100 text-slate-500'">
            {{ p.isTeam === 1 ? '团队' : '个人' }}
          </span>
        </div>
        <p class="text-xs text-slate-500">{{ p.courseName }} ｜ {{ p.teacherName }}</p>
        <p class="text-xs text-slate-400">截止 {{ fmt(p.endTime) }} ｜ 满分 {{ p.fullScore }}</p>
        <p class="text-xs" :class="lineClass(p)">
          {{ statusLine(p) }}
        </p>
        <button class="btn-primary text-xs mt-1" @click="open(p)">进入项目</button>
      </div>
    </div>

    <!-- 详情 -->
    <div class="card p-4 space-y-4" v-if="active">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">{{ active.projectName }}</h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="active = null">收起</button>
      </div>
      <p class="text-xs text-slate-400">{{ active.description }}</p>
      <p class="text-xs text-slate-500">
        允许提交：{{ active.allowFormats }} ｜ 截止 {{ fmt(active.endTime) }} ｜ 满分 {{ active.fullScore }}
        <span v-if="active.reason" class="text-amber-600 ml-2">{{ active.reason }}</span>
      </p>

      <!-- 组队 -->
      <div v-if="active.isTeam === 1">
        <h3 class="text-sm font-medium text-slate-600 mb-2">我的小组</h3>
        <div v-if="active.myTeam" class="text-sm space-y-1">
          <p>{{ active.myTeam.teamName }}（{{ active.myTeam.memberCount }} 人）· 组长 {{ active.myTeam.leaderStudentName }}</p>
          <p class="text-xs text-slate-500">
            成员：{{ active.teamMembers.map((m) => m.studentName + (m.leader ? '（组长）' : '')).join('、') }}
          </p>
        </div>
        <div v-else-if="active.canCreateTeam" class="space-y-2">
          <div class="flex flex-wrap items-end gap-2">
            <label class="text-xs">组名
              <input v-model="teamForm.teamName" class="input mt-1 w-40" placeholder="第1组" />
            </label>
            <button class="btn-primary text-sm" @click="createTeam">创建小组（我是组长）</button>
          </div>
          <div class="border border-slate-200 rounded-lg p-2 max-h-40 overflow-y-auto">
            <p class="text-xs text-slate-500 mb-1">勾选组员（可选，之后不能再加人）</p>
            <label v-for="c in classmates" :key="c.id" class="flex items-center gap-2 text-sm">
              <input type="checkbox" :value="c.id" v-model="teamForm.memberIds" />
              {{ c.studentNo }} · {{ c.studentName }}
            </label>
          </div>
        </div>
        <p v-else class="text-xs text-slate-400">你还没有小组，或现在不能组队。</p>
      </div>

      <!-- 提交 -->
      <div v-if="active.canSubmit">
        <h3 class="text-sm font-medium text-slate-600 mb-2">提交材料</h3>
        <div class="space-y-2">
          <div class="flex flex-wrap items-center gap-2">
            <input ref="fileInput" type="file" multiple class="hidden" @change="onPick" />
            <button class="btn-ghost text-sm" :disabled="uploading" @click="$refs.fileInput.click()">
              {{ uploading ? '上传中…' : '选择文件上传' }}
            </button>
            <span class="text-xs text-slate-400">已上传 {{ myFiles.length }} 个文件</span>
          </div>
          <ul class="text-sm space-y-1">
            <li v-for="(f, i) in myFiles" :key="i" class="flex items-center gap-2">
              📎 {{ f.name }}
              <span class="text-xs text-slate-400">{{ sizeText(f.size) }}</span>
              <button class="text-xs text-slate-400 hover:text-red-500" @click="myFiles.splice(i, 1)">移除</button>
            </li>
          </ul>
          <label class="text-xs block">代码仓库链接（可选，若允许 LINK）
            <input v-model="submitForm.repoUrl" class="input mt-1" placeholder="https://github.com/..." />
          </label>
          <label class="text-xs block">提交说明（可选）
            <input v-model="submitForm.remark" class="input mt-1" />
          </label>
          <button class="btn-primary text-sm" @click="doSubmit">
            {{ active.mySubmission ? '重新提交（截止前可改）' : '提交项目' }}
          </button>
        </div>
      </div>

      <!-- 我的提交与得分 -->
      <div v-if="active.mySubmission">
        <h3 class="text-sm font-medium text-slate-600 mb-2">我的提交</h3>
        <div class="text-sm space-y-1">
          <p>状态：{{ active.mySubmission.submitStatus === 'MARKED' ? '已批阅' : '已提交' }}
            <span class="text-xs text-slate-400 ml-2">{{ fmt(active.mySubmission.submitTime) }}</span></p>
          <p v-for="(f, i) in active.mySubmission.files" :key="i" class="text-xs text-slate-500">📎 {{ f.name }}</p>
          <p v-if="active.mySubmission.repoUrl" class="text-xs text-slate-500">🔗 {{ active.mySubmission.repoUrl }}</p>
          <p v-if="active.mySubmission.projectScore != null" class="text-sm">
            项目总分 <span class="font-medium text-emerald-600">{{ active.mySubmission.projectScore }}</span>
            <span v-if="active.mySubmission.comment" class="text-xs text-slate-500 ml-2">评语：{{ active.mySubmission.comment }}</span>
          </p>
        </div>
        <table class="w-full text-sm mt-2" v-if="active.mySubmission.members && active.mySubmission.members.length > 1">
          <thead class="text-slate-400">
            <tr>
              <th class="text-left py-1">成员</th><th class="text-right py-1 w-24">占比</th>
              <th class="text-right py-1 w-28">个人得分</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in active.mySubmission.members" :key="m.studentId" class="border-t border-slate-100">
              <td class="py-1">{{ m.studentName }}<span v-if="m.leader" class="tag bg-brand-50 text-brand-600 ml-1">组长</span></td>
              <td class="py-1 text-right">{{ m.ratio == null ? '—' : pct(m.ratio) }}</td>
              <td class="py-1 text-right font-medium">{{ m.personalScore == null ? '—' : m.personalScore }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 贡献表 -->
      <div v-if="active.isTeam === 1 && active.isLeader">
        <h3 class="text-sm font-medium text-slate-600 mb-2">贡献表（组长提交，仅一次）</h3>
        <div v-if="active.contributionSubmitted" class="text-sm text-emerald-600">
          已提交并锁定，任何角色都不能修改。
        </div>
        <div v-else class="space-y-2">
          <table class="w-full text-sm">
            <thead class="text-slate-400">
              <tr>
                <th class="text-left py-1">成员</th><th class="text-right py-1 w-28">占比（%）</th>
                <th class="text-left py-1 w-52">备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in active.teamMembers" :key="m.studentId" class="border-t border-slate-100">
                <td class="py-1">{{ m.studentName }}<span v-if="m.leader" class="tag bg-brand-50 text-brand-600 ml-1">组长</span></td>
                <td class="py-1 text-right">
                  <input v-model.number="ratioInput[m.studentId]" type="number" min="0" max="100" class="input w-24" />
                </td>
                <td class="py-1"><input v-model="noteInput[m.studentId]" class="input" placeholder="如 后端 / 文档" /></td>
              </tr>
            </tbody>
          </table>
          <p class="text-xs" :class="totalPercent === 100 ? 'text-emerald-600' : 'text-red-600'">
            合计 {{ totalPercent }}%（必须 100%）
          </p>
          <label class="text-xs block">备注
            <input v-model="contributionRemark" class="input mt-1" placeholder="可选" />
          </label>
          <button class="btn-primary text-sm" @click="submitContribution">提交贡献表</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { myProjectApi } from '../../api/project'

const items = ref([])
const active = ref(null)
const classmates = ref([])
const myFiles = ref([])
const uploading = ref(false)
const fileInput = ref(null)
const message = ref('')
const ok = ref(true)
const teamForm = reactive({ teamName: '', memberIds: [] })
const submitForm = reactive({ repoUrl: '', remark: '' })
const ratioInput = reactive({})
const noteInput = reactive({})
const contributionRemark = ref('')

const totalPercent = computed(() => {
  if (!active.value) return 0
  const sum = (active.value.teamMembers || []).reduce((s, m) => s + Number(ratioInput[m.studentId] || 0), 0)
  return Math.round(sum * 100) / 100
})

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '不限' }
function pct(r) { return Math.round(Number(r) * 10000) / 100 + '%' }
function sizeText(n) {
  const v = Number(n || 0)
  if (v < 1024) return v + ' B'
  if (v < 1024 * 1024) return (v / 1024).toFixed(1) + ' KB'
  return (v / 1024 / 1024).toFixed(1) + ' MB'
}
function statusLine(p) {
  if (p.mySubmission && p.mySubmission.submitStatus === 'MARKED') {
    return '已批阅 · 我的得分 ' + (p.mySubmission.projectScore == null ? '—' : p.mySubmission.projectScore)
  }
  if (p.mySubmission) return '已提交，待批阅'
  if (p.isTeam === 1 && !p.myTeam) return '需要先组队'
  return p.reason || (p.canSubmit ? '待提交' : '未开放')
}
function lineClass(p) {
  if (p.mySubmission && p.mySubmission.submitStatus === 'MARKED') return 'text-emerald-600'
  if (p.mySubmission) return 'text-amber-600'
  return 'text-slate-500'
}

async function load() {
  items.value = await myProjectApi.list()
}

async function open(p) {
  active.value = await myProjectApi.get(p.id)
  myFiles.value = (active.value.mySubmission && active.value.mySubmission.files
    ? active.value.mySubmission.files.map((f) => ({ id: Number(f.fileId), name: f.name, size: Number(f.size || 0) }))
    : [])
  submitForm.repoUrl = (active.value.mySubmission && active.value.mySubmission.repoUrl) || ''
  submitForm.remark = (active.value.mySubmission && active.value.mySubmission.remark) || ''
  teamForm.memberIds = []
  if (active.value.canCreateTeam) {
    classmates.value = await myProjectApi.teammates(active.value.teachingClassId)
  }
  if (active.value.isLeader && !active.value.contributionSubmitted) {
    const n = Math.max(1, (active.value.teamMembers || []).length)
    const base = Math.floor(10000 / n) / 100
    ;(active.value.teamMembers || []).forEach((m, i) => {
      ratioInput[m.studentId] = i === n - 1 ? Math.round((100 - base * (n - 1)) * 100) / 100 : base
      noteInput[m.studentId] = ''
    })
  }
}

async function createTeam() {
  try {
    await myProjectApi.createTeam(active.value.id, {
      teamName: teamForm.teamName || null,
      memberIds: teamForm.memberIds
    })
    toast('小组已创建，你是组长')
    const id = active.value.id
    await load()
    active.value = await myProjectApi.get(id)
  } catch (e) { toast(e.message, false) }
}

async function onPick(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  uploading.value = true
  try {
    for (const f of files) {
      const saved = await myProjectApi.upload(active.value.teachingClassId, f)
      myFiles.value.push({ id: saved.id, name: saved.fileName, size: saved.fileSize })
    }
    toast('已上传 ' + files.length + ' 个文件')
  } catch (err) { toast(err.message, false) }
  uploading.value = false
  e.target.value = ''
}

async function doSubmit() {
  try {
    await myProjectApi.submit(active.value.id, {
      fileIds: myFiles.value.map((f) => f.id),
      repoUrl: submitForm.repoUrl || null,
      remark: submitForm.remark || null
    })
    toast('提交成功')
    const id = active.value.id
    await load()
    active.value = await myProjectApi.get(id)
  } catch (e) { toast(e.message, false) }
}

async function submitContribution() {
  if (Math.round(totalPercent.value * 100) / 100 !== 100) {
    toast('贡献占比合计必须为 100%，当前 ' + totalPercent.value + '%', false)
    return
  }
  const okConfirm = window.confirm('贡献表提交后会立即锁定：本人、其他组员、任课教师都不能再修改。确认提交？')
  if (!okConfirm) return
  try {
    await myProjectApi.contribution(active.value.id, {
      items: (active.value.teamMembers || []).map((m) => ({
        studentId: m.studentId,
        ratio: Number(ratioInput[m.studentId] || 0) / 100,
        note: noteInput[m.studentId] || null
      })),
      remark: contributionRemark.value || null
    })
    toast('贡献表已提交并锁定')
    const id = active.value.id
    active.value = await myProjectApi.get(id)
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
