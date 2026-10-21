<template>
  <div class="space-y-4">
    <div v-if="!embedded" class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/home')">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">测验与作业</h1>
        <span class="tag bg-slate-100 text-slate-500">教学班 #{{ teachingClassId }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      题目支持从 Word 导入 → 校对 → 发布；客观题（单选/多选/判断/填空）交卷即判，简答题由你批阅并记录判卷人。
      作业与测验不允许迟交、不允许教师代交，截止后未交的按缺考记 0 分。
    </p>

    <!-- 新建 -->
    <div class="card p-4" v-if="!editing">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">新建测验 / 作业</h2>
        <span class="text-xs text-slate-400">先建草稿，导入题目校对后再发布</span>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
        <label class="text-sm">类型
          <select v-model="form.paperType" class="input mt-1">
            <option v-for="t in paperTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </label>
        <label class="text-sm md:col-span-2">名称
          <input v-model="form.paperName" class="input mt-1" placeholder="如 第1次随堂测验：计算机基础" />
        </label>
        <label class="text-sm">挂到课时（可选）
          <select v-model="form.lessonId" class="input mt-1">
            <option :value="null">不挂课时</option>
            <option v-for="l in lessons" :key="l.id" :value="l.id">
              第{{ l.weekNo }}周 第{{ l.sessionNo }}次 {{ l.title || '' }}
            </option>
          </select>
        </label>
        <label class="text-sm">截止时间（必填）
          <input v-model="form.endTime" type="datetime-local" class="input mt-1" />
        </label>
        <label class="text-sm">限时（分钟）
          <input v-model.number="form.durationMinutes" type="number" class="input mt-1" placeholder="留空不限时" />
        </label>
        <label class="text-sm">计入评分项
          <select v-model="form.schemeItemId" class="input mt-1">
            <option :value="null">仅记录不计分</option>
            <option v-for="s in schemeItems" :key="s.id" :value="s.id">{{ s.itemName }}（{{ s.weight }}%）</option>
          </select>
        </label>
        <label class="text-sm">批阅后可见答案
          <select v-model="form.showAnswer" class="input mt-1">
            <option value="NEVER">永远不可见</option>
            <option value="AFTER_MARK">批阅后可见</option>
          </select>
        </label>
      </div>
      <div class="flex flex-wrap items-center gap-4 mt-3 text-sm text-slate-600">
        <label class="flex items-center gap-1">
          <input type="checkbox" :checked="form.showScore === 1" @change="form.showScore = $event.target.checked ? 1 : 0" />
          向学生显示成绩
        </label>
        <label class="flex items-center gap-1">
          <input type="checkbox" :checked="form.allowViewPaper === 1" @change="form.allowViewPaper = $event.target.checked ? 1 : 0" />
          允许学生查看试卷
        </label>
        <button class="btn-primary text-sm" @click="createDraft">建立草稿</button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="card p-4">
      <h2 class="font-medium text-slate-700 mb-3">本班测验与作业（{{ papers.length }}）</h2>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">名称</th><th class="text-left py-2 w-20">类型</th>
            <th class="text-left py-2 w-24">状态</th><th class="text-right py-2 w-20">题数</th>
            <th class="text-right py-2 w-24">满分</th><th class="text-left py-2 w-32">截止</th>
            <th class="text-right py-2 w-28">交卷/批完</th><th class="text-right py-2 w-56">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in papers" :key="p.id" class="border-b border-slate-100">
            <td class="py-2">{{ p.paperName }}
              <span v-if="p.schemeItemName" class="tag bg-brand-50 text-brand-600 ml-1">{{ p.schemeItemName }}</span>
            </td>
            <td class="py-2 text-slate-500">{{ typeLabel(p.paperType) }}</td>
            <td class="py-2">
              <span class="tag" :class="statusClass(p.status)">{{ statusLabel(p.status) }}</span>
            </td>
            <td class="py-2 text-right">{{ p.questionCount }}</td>
            <td class="py-2 text-right">{{ p.fullScore }}</td>
            <td class="py-2 text-slate-500">{{ fmt(p.endTime) }}</td>
            <td class="py-2 text-right">{{ p.submitCount }} / {{ p.markedCount }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <button v-if="p.status !== 'PUBLISHED'" class="text-brand-600 hover:underline mr-2" @click="edit(p)">编辑题目</button>
              <button v-if="p.status === 'DRAFT'" class="text-emerald-600 hover:underline mr-2" @click="doPublish(p, true)">发布</button>
              <button v-if="p.status === 'PUBLISHED'" class="text-amber-600 hover:underline mr-2" @click="doPublish(p, false)">撤回</button>
              <button v-if="p.status === 'PUBLISHED'" class="text-slate-500 hover:underline mr-2" @click="doClose(p)">截止</button>
              <button class="text-brand-600 hover:underline"
                      @click="$router.push({ path: '/teacher/grading', query: { paperId: p.id } })">交卷与批改</button>
            </td>
          </tr>
          <tr v-if="!papers.length"><td colspan="8" class="py-6 text-center text-slate-400">还没有测验或作业</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 题目编辑 / 校对 -->
    <div class="card p-4 space-y-3" v-if="editing">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div>
          <h2 class="font-medium text-slate-700">{{ editing.paperName }}</h2>
          <p class="text-xs text-slate-400">
            {{ typeLabel(editing.paperType) }} ｜ 题数 {{ questions.length }} ｜ 合计 {{ totalScore }} 分 ｜
            {{ editing.status === 'DRAFT' ? '草稿（可改题）' : '已发布（不可改题）' }}
          </p>
        </div>
        <div class="flex items-center gap-2">
          <input ref="importInput" type="file" class="hidden" accept=".docx,.txt" @change="onImport" />
          <button class="btn-ghost text-sm" :disabled="editing.status !== 'DRAFT'" @click="$refs.importInput.click()">
            导入 Word / 文本
          </button>
          <button class="btn-primary text-sm" :disabled="editing.status !== 'DRAFT'" @click="saveQuestions">保存题目</button>
          <button class="text-sm text-slate-400 hover:text-slate-600" @click="editing = null">收起</button>
        </div>
      </div>

      <div v-if="importWarnings.length" class="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded-lg p-2">
        <p class="font-medium mb-1">导入提示（{{ importWarnings.length }}）</p>
        <p v-for="(w, i) in importWarnings" :key="i">· {{ w }}</p>
      </div>

      <div class="space-y-3">
        <div v-for="(q, i) in questions" :key="i" class="border border-slate-200 rounded-lg p-3 space-y-2">
          <div class="flex flex-wrap items-center gap-2">
            <span class="text-xs text-slate-400 w-10">第 {{ i + 1 }} 题</span>
            <select v-model="q.qType" class="input w-24">
              <option v-for="t in questionTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
            <input v-model="q.groupName" class="input w-48" placeholder="大题名称（可选）" />
            <input v-model.number="q.fullScore" type="number" class="input w-20" />
            <span class="text-xs text-slate-400">分</span>
            <button class="text-xs text-slate-400 hover:text-red-500 ml-auto" @click="questions.splice(i, 1)">删除</button>
          </div>
          <textarea v-model="q.stem" rows="2" class="input" placeholder="题干"></textarea>

          <!-- 选项 -->
          <div v-if="q.qType === 'SINGLE' || q.qType === 'MULTI'" class="pl-6 space-y-1">
            <div v-for="(o, j) in q.options" :key="j" class="flex items-center gap-2">
              <span class="text-xs text-slate-400 w-6">{{ o.key }}</span>
              <input v-model="o.text" class="input" placeholder="选项内容" />
              <button class="text-xs text-slate-400 hover:text-red-500" @click="q.options.splice(j, 1); reKey(q)">删除</button>
            </div>
            <button class="text-xs text-brand-600 hover:underline" @click="addOption(q)">+ 添加选项</button>
          </div>

          <!-- 答案 -->
          <div class="pl-6 text-sm">
            <template v-if="q.qType === 'SINGLE'">
              <span class="text-xs text-slate-500 mr-2">正确答案</span>
              <select v-model="q.answerKeys[0]" class="input w-24 inline-block">
                <option v-for="o in q.options" :key="o.key" :value="o.key">{{ o.key }}</option>
              </select>
            </template>
            <template v-else-if="q.qType === 'MULTI'">
              <span class="text-xs text-slate-500 mr-2">正确答案（多选）</span>
              <label v-for="o in q.options" :key="o.key" class="inline-flex items-center gap-1 mr-3">
                <input type="checkbox" :value="o.key" v-model="q.answerKeys" /> {{ o.key }}
              </label>
            </template>
            <template v-else-if="q.qType === 'JUDGE'">
              <span class="text-xs text-slate-500 mr-2">正确答案</span>
              <select v-model="q.answerBool" class="input w-24 inline-block">
                <option :value="true">正确</option>
                <option :value="false">错误</option>
              </select>
            </template>
            <template v-else-if="q.qType === 'BLANK'">
              <span class="text-xs text-slate-500 mr-2">各空答案（每行一个，按顺序）</span>
              <textarea v-model="q.answerText" rows="2" class="input mt-1" placeholder="中央处理器&#10;丢失"></textarea>
            </template>
            <template v-else>
              <span class="text-xs text-slate-500 mr-2">参考答案（供批阅参考）</span>
              <textarea v-model="q.answerText" rows="2" class="input mt-1"></textarea>
            </template>
          </div>

          <div class="pl-6">
            <input v-model="q.analysis" class="input" placeholder="解析（可选）" />
          </div>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" @click="addQuestion">+ 添加题目</button>
        <span class="text-xs text-slate-400">合计 {{ totalScore }} 分（保存时会重算试卷满分）</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { paperApi, questionTypes, paperTypes } from '../../api/paper'
import { contentApi } from '../../api/content'
import { scoreApi } from '../../api/score'

const route = useRoute()
const props = defineProps({
  teachingClassId: { type: Number, default: 0 },
  offeringId: { type: Number, default: 0 },
  embedded: { type: Boolean, default: false },
  paperTypeFilter: { type: String, default: '' }
})
const teachingClassId = props.teachingClassId || Number(route.query.teachingClassId || 1)
const offeringId = props.offeringId || Number(route.query.offeringId || 0)

const papers = ref([])
const lessons = ref([])
const schemeItems = ref([])
const editing = ref(null)
const questions = ref([])
const importWarnings = ref([])
const importInput = ref(null)
const message = ref('')
const ok = ref(true)

const form = reactive({
  paperType: 'QUIZ', paperName: '', lessonId: null, endTime: '', durationMinutes: null,
  schemeItemId: null, showAnswer: 'AFTER_MARK', showScore: 1, allowViewPaper: 1
})

const totalScore = computed(() => Math.round(questions.value.reduce((s, q) => s + Number(q.fullScore || 0), 0) * 100) / 100)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function typeLabel(t) { return (paperTypes.find((x) => x.value === t) || {}).label || t }
function statusLabel(s) { return { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已截止' }[s] || s }
function statusClass(s) {
  return { DRAFT: 'bg-slate-100 text-slate-500', PUBLISHED: 'bg-emerald-50 text-emerald-600', CLOSED: 'bg-slate-200 text-slate-600' }[s]
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '不限' }
function datetimeLocalToIso(v) { return v ? (v.length === 16 ? v + ':00' : v) : null }

async function load() {
  const list = await paperApi.list(teachingClassId)
  papers.value = props.paperTypeFilter ? list.filter((p) => p.paperType === props.paperTypeFilter) : list
  lessons.value = await contentApi.lessons(teachingClassId)
  if (offeringId) {
    const scheme = await scoreApi.scheme(offeringId)
    const flat = []
    const walk = (list) => (list || []).forEach((it) => {
      if (it.children && it.children.length) walk(it.children)
      else flat.push(it)
    })
    if (scheme) walk(scheme.items)
    schemeItems.value = flat
  }
}

async function createDraft() {
  if (!form.paperName) { toast('请填写名称', false); return }
  if (!form.endTime) { toast('作业与测验不允许迟交，必须设置截止时间', false); return }
  try {
    const p = await paperApi.save({
      teachingClassId, paperType: form.paperType, paperName: form.paperName,
      lessonId: form.lessonId, endTime: datetimeLocalToIso(form.endTime),
      durationMinutes: form.durationMinutes || null, schemeItemId: form.schemeItemId,
      showAnswer: form.showAnswer, showScore: form.showScore, allowViewPaper: form.allowViewPaper
    })
    toast('草稿已建立，接着导入或添加题目')
    await load()
    await edit(p)
    form.paperName = ''
  } catch (e) { toast(e.message, false) }
}

function parseOptions(json) {
  try { return JSON.parse(json || '[]') } catch (e) { return [] }
}
function parseAnswer(q) {
  let ans = {}
  try { ans = JSON.parse(q.answerJson || '{}') } catch (e) { ans = {} }
  return ans
}

async function edit(p) {
  const full = await paperApi.get(p.id)
  editing.value = full
  importWarnings.value = []
  questions.value = (full.questions || []).map((q) => {
    const ans = parseAnswer(q)
    const base = {
      qType: q.qType, stem: q.stem, groupName: q.groupName || '', fullScore: Number(q.fullScore || 2),
      analysis: q.analysis || '', options: parseOptions(q.optionsJson),
      answerKeys: Array.isArray(ans.keys) ? ans.keys.slice() : [],
      answerBool: typeof ans.value === 'boolean' ? ans.value : true,
      answerText: Array.isArray(ans.answers) ? ans.answers.join('\n') : (ans.ref || '')
    }
    if (base.qType === 'SINGLE' && !base.answerKeys.length) base.answerKeys = ['A']
    return base
  })
}

function reKey(q) {
  q.options.forEach((o, i) => { o.key = String.fromCharCode(65 + i) })
  q.answerKeys = q.answerKeys.filter((k) => q.options.some((o) => o.key === k))
}
function addOption(q) {
  q.options.push({ key: String.fromCharCode(65 + q.options.length), text: '' })
}
function addQuestion() {
  questions.value.push({
    qType: 'SINGLE', stem: '', groupName: '', fullScore: 2, analysis: '',
    options: [{ key: 'A', text: '' }, { key: 'B', text: '' }, { key: 'C', text: '' }, { key: 'D', text: '' }],
    answerKeys: ['A'], answerBool: true, answerText: ''
  })
}

function toDto(q, i) {
  const dto = {
    qNo: i + 1, sortNo: i + 1, qType: q.qType, stem: q.stem, analysis: q.analysis || null,
    fullScore: q.fullScore, groupName: q.groupName || null
  }
  if (q.qType === 'SINGLE' || q.qType === 'MULTI') {
    dto.optionsJson = JSON.stringify(q.options)
    const keys = q.qType === 'SINGLE' ? [q.answerKeys[0]].filter(Boolean) : q.answerKeys.slice().sort()
    dto.answerJson = JSON.stringify({ keys })
  } else if (q.qType === 'JUDGE') {
    dto.answerJson = JSON.stringify({ value: !!q.answerBool })
  } else if (q.qType === 'BLANK') {
    const answers = String(q.answerText || '').split('\n').map((s) => s.trim()).filter(Boolean)
    dto.answerJson = JSON.stringify({ answers, ignoreCase: true, trim: true })
  } else {
    dto.answerJson = JSON.stringify({ ref: q.answerText || '' })
  }
  return dto
}

async function saveQuestions() {
  if (!questions.value.length) { toast('至少需要一道题', false); return }
  try {
    const p = await paperApi.save({
      id: editing.value.id, teachingClassId, paperType: editing.value.paperType,
      paperName: editing.value.paperName, lessonId: editing.value.lessonId,
      startTime: editing.value.startTime, endTime: editing.value.endTime,
      durationMinutes: editing.value.durationMinutes, schemeItemId: editing.value.schemeItemId,
      allowViewPaper: editing.value.allowViewPaper, showScore: editing.value.showScore,
      showAnswer: editing.value.showAnswer, remark: editing.value.remark,
      questions: questions.value.map(toDto)
    })
    editing.value = p
    toast('题目已保存，试卷满分 ' + p.fullScore + ' 分')
    await load()
  } catch (e) { toast(e.message, false) }
}

async function onImport(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  try {
    const res = await paperApi.importQuestions(file)
    applyImport(res)
  } catch (err) { toast(err.message, false) }
  e.target.value = ''
}

function applyImport(res) {
  importWarnings.value = res.warnings || []
  const parsed = (res.questions || []).map((q) => {
    const ans = parseAnswer(q)
    const base = {
      qType: q.qType, stem: q.stem, groupName: q.groupName || '', fullScore: Number(q.fullScore || 2),
      analysis: q.analysis || '', options: parseOptions(q.optionsJson),
      answerKeys: Array.isArray(ans.keys) ? ans.keys.slice() : [],
      answerBool: typeof ans.value === 'boolean' ? ans.value : true,
      answerText: Array.isArray(ans.answers) ? ans.answers.join('\n') : (ans.ref || '')
    }
    return base
  })
  questions.value = parsed
  toast('已解析 ' + parsed.length + ' 道题，请校对后保存')
}

async function doPublish(p, publish) {
  try {
    await paperApi.publish(p.id, publish)
    toast(publish ? '已发布，学生可见' : '已撤回为草稿')
    await load()
    if (editing.value && editing.value.id === p.id) await edit(p)
  } catch (e) { toast(e.message, false) }
}
async function doClose(p) {
  try {
    const res = await paperApi.close(p.id)
    toast('已截止，登记缺考 ' + res.absentCount + ' 人')
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
