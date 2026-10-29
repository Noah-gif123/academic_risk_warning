<template>
  <div class="fixed inset-0 z-40 bg-slate-900/40 flex items-start justify-center px-4 py-8 overflow-auto">
    <div class="bg-white rounded-xl shadow-xl max-w-4xl w-full p-5 space-y-4">
      <div class="flex items-start justify-between gap-3">
        <div>
          <h3 class="font-semibold text-slate-800">AI 出题（按课程资料）</h3>
          <p class="text-xs text-slate-400 mt-1">
            资料来自本课程知识库（课件 pptx / 文档 docx / txt）。AI 只出<b>草稿</b>：每题都要过五道闸门，
            你在这里审、改、弃，采纳后才进试卷——分值、发布、批阅与手工出题完全一样。
          </p>
        </div>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$emit('close')">关闭</button>
      </div>

      <!-- 第一步：选资料 + 参数 -->
      <div v-if="!drafts.length" class="space-y-3">
        <div class="border border-slate-200 rounded-lg p-3 space-y-2">
          <div class="flex items-center justify-between flex-wrap gap-2">
            <p class="text-sm font-medium text-slate-700">出题范围（{{ selectedDocs.length }} / {{ sources.length }} 份资料）</p>
            <div class="flex items-center gap-2 text-xs">
              <button class="text-brand-600 hover:underline" @click="selectAllDocs(true)">全选</button>
              <button class="text-slate-400 hover:underline" @click="selectAllDocs(false)">清空</button>
              <span class="text-slate-400">不选 = 用全部资料</span>
            </div>
          </div>
          <div v-if="loadingSources" class="text-xs text-slate-400">正在读取本课程资料…</div>
          <div v-else-if="!sources.length" class="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded p-2">
            {{ sourcesMessage }}
          </div>
          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-1.5">
            <label v-for="d in sources" :key="d.docId"
                   class="flex items-center gap-2 text-sm text-slate-600 px-2 py-1 rounded hover:bg-slate-50">
              <input type="checkbox" :value="d.docId" v-model="selectedDocs" />
              <span class="truncate">{{ d.docName }}</span>
              <span class="text-[11px] text-slate-400 shrink-0">{{ d.chunkCount }} 段</span>
              <span class="text-[11px] px-1.5 rounded shrink-0"
                    :class="d.sourceType === 'RESOURCE' ? 'bg-brand-50 text-brand-600' : 'bg-slate-100 text-slate-500'">
                {{ d.sourceType === 'RESOURCE' ? '课件/资料' : '导学' }}
              </span>
            </label>
          </div>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          <label class="text-sm">题量
            <input v-model.number="form.count" type="number" min="1" max="10" class="input mt-1" />
          </label>
          <label class="text-sm">难度
            <select v-model="form.difficulty" class="input mt-1">
              <option value="easy">易</option>
              <option value="medium">中</option>
              <option value="hard">难</option>
            </select>
          </label>
          <label class="text-sm">大题名称
            <input v-model="form.groupName" class="input mt-1" placeholder="AI 出题（可留空）" />
          </label>
          <div class="text-sm">题型
            <div class="flex flex-wrap gap-2 mt-2">
              <label v-for="t in allTypes" :key="t.value" class="inline-flex items-center gap-1 text-xs">
                <input type="checkbox" :value="t.value" v-model="form.types" /> {{ t.label }}
              </label>
            </div>
          </div>
        </div>

        <label class="text-sm block">补充要求（可选）
          <input v-model="form.instruction" class="input mt-1" placeholder="例如：多考函数参数与常见报错，不要出纯记忆题" />
        </label>

        <p v-if="error" class="text-xs text-red-600 bg-red-50 border border-red-200 rounded p-2">{{ error }}</p>

        <div class="flex items-center justify-between">
          <p class="text-xs text-slate-400">
            生成需要 10–30 秒（模型逐题思考并标注依据）；生成结果会落库，随时可在本弹窗重看。
          </p>
          <button class="btn-primary text-sm" :disabled="generating || !sources.length" @click="generate">
            {{ generating ? '正在出题…' : '开始出题' }}
          </button>
        </div>
      </div>

      <!-- 第二步：审题 -->
      <div v-else class="space-y-3">
        <div class="flex items-center justify-between flex-wrap gap-2">
          <p class="text-sm text-slate-600">
            生成 {{ drafts.length }} 题 ｜ 通过闸门 <b class="text-emerald-600">{{ passedCount }}</b> ｜
            被拦下 <b class="text-amber-600">{{ drafts.length - passedCount }}</b> ｜
            已勾选 <b class="text-brand-600">{{ checkedCount }}</b>
            <span class="text-xs text-slate-400 ml-2">（{{ genMeta }}）</span>
          </p>
          <div class="flex items-center gap-2">
            <button class="text-xs text-slate-400 hover:text-slate-600" @click="reset">重新出题</button>
            <button class="btn-primary text-sm" :disabled="!checkedCount || adopting" @click="adopt">
              {{ adopting ? '采纳中…' : `采纳选中（${checkedCount} 题）加入试卷` }}
            </button>
          </div>
        </div>

        <div v-if="error" class="text-xs text-red-600 bg-red-50 border border-red-200 rounded p-2">{{ error }}</div>

        <div class="space-y-3 max-h-[60vh] overflow-auto pr-1">
          <div v-for="(d, i) in drafts" :key="d.id || i"
               class="border rounded-lg p-3 space-y-2"
               :class="d.passed ? 'border-slate-200' : 'border-amber-300 bg-amber-50/40'">
            <div class="flex flex-wrap items-center gap-2">
              <input type="checkbox" v-model="d.checked" />
              <span class="text-xs text-slate-400 w-10">第 {{ i + 1 }} 题</span>
              <span class="tag bg-slate-100 text-slate-600">{{ d.typeLabel }}</span>
              <span class="tag bg-slate-100 text-slate-500">{{ difficultyLabel(d.difficulty) }}</span>
              <input v-model.number="d.fullScore" type="number" class="input w-16" />
              <span class="text-xs text-slate-400">分</span>
              <span class="tag" :class="d.passed ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-100 text-amber-700'">
                {{ d.passed ? '五道闸门通过' : '未通过：' + failText(d.quality) }}
              </span>
              <button class="text-xs text-slate-400 hover:text-red-500 ml-auto"
                      :disabled="d.rejected"
                      @click="rejectOne(d)">弃用</button>
            </div>

            <textarea v-model="d.stem" rows="2" class="input" placeholder="题干"></textarea>

            <div v-if="d.qType === 'SINGLE' || d.qType === 'MULTI'" class="pl-4 space-y-1">
              <div v-for="o in d.options" :key="o.key" class="flex items-center gap-2">
                <span class="text-xs text-slate-400 w-5">{{ o.key }}</span>
                <input v-model="o.text" class="input" />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-2 pl-4">
              <label class="text-xs text-slate-500">答案
                <input v-model="d.answerText" class="input mt-1 text-sm" :placeholder="answerHint(d.qType)" />
              </label>
              <label class="text-xs text-slate-500 md:col-span-2">解析
                <input v-model="d.analysis" class="input mt-1 text-sm" placeholder="解析（可选）" />
              </label>
            </div>

            <div class="flex flex-wrap items-center gap-3 text-[11px] text-slate-400 pl-4">
              <span v-if="d.sourceRef">依据：{{ d.sourceRef }}</span>
              <span v-for="c in (d.quality && d.quality.checks) || []" :key="c.code"
                    :class="c.pass ? 'text-slate-400' : 'text-amber-700'"
                    :title="c.detail">
                {{ c.pass ? '✓' : '✗' }}{{ c.name }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { aiQuestionApi } from '../api/ai'
import { notify } from '../toast'

const props = defineProps({
  teachingClassId: { type: [Number, String], required: true }
})
const emit = defineEmits(['close', 'adopt'])

const allTypes = [
  { value: 'SINGLE', label: '单选' },
  { value: 'MULTI', label: '多选' },
  { value: 'JUDGE', label: '判断' },
  { value: 'BLANK', label: '填空' },
  { value: 'SHORT', label: '简答' }
]

const sources = ref([])
const sourcesMessage = ref('')
const loadingSources = ref(false)
const selectedDocs = ref([])
const form = ref({ count: 5, difficulty: 'medium', types: ['SINGLE', 'JUDGE'], groupName: '', instruction: '' })
const drafts = ref([])
const generating = ref(false)
const adopting = ref(false)
const error = ref('')
const genMeta = ref('')

const passedCount = computed(() => drafts.value.filter((d) => d.passed).length)
const checkedCount = computed(() => drafts.value.filter((d) => d.checked && !d.rejected).length)

onMounted(loadSources)

async function loadSources() {
  loadingSources.value = true
  try {
    const r = await aiQuestionApi.sources(props.teachingClassId)
    sources.value = r.docs || []
    sourcesMessage.value = r.message || '这⻔课还没有入库的资料'
  } catch (e) {
    error.value = e?.message || '读取资料失败'
  } finally {
    loadingSources.value = false
  }
}

function selectAllDocs(all) {
  selectedDocs.value = all ? sources.value.map((d) => d.docId) : []
}

async function generate() {
  if (generating.value) return
  generating.value = true
  error.value = ''
  try {
    const r = await aiQuestionApi.generate({
      teachingClassId: props.teachingClassId,
      docIds: selectedDocs.value,
      count: form.value.count,
      types: form.value.types,
      difficulty: form.value.difficulty,
      groupName: form.value.groupName,
      instruction: form.value.instruction
    })
    const list = (r.drafts || []).map((d) => ({
      ...d,
      checked: !!d.quality?.passed,
      rejected: false,
      answerText: answerToText(d),
      options: (d.options || []).map((o) => ({ ...o }))
    }))
    drafts.value = list
    genMeta.value = `素材 ${r.materialCount || 0} 段 · ${r.tokens || 0} tokens · 约 ${Number(r.cost || 0).toFixed(4)} 元`
    if (!list.length) {
      error.value = r.message || '模型没有按约定格式返回题目，请重试一次'
    } else {
      notify(r.message || `已生成 ${list.length} 道草稿题`, list.some((d) => d.passed))
    }
  } catch (e) {
    error.value = e?.message || '出题失败'
    notify(error.value, false)
  } finally {
    generating.value = false
  }
}

function answerToText(d) {
  const a = d.answer || {}
  if (d.qType === 'SINGLE' || d.qType === 'MULTI') return (a.keys || []).join(',')
  if (d.qType === 'JUDGE') return a.value === false ? '错误' : '正确'
  if (d.qType === 'BLANK') return (a.answers || []).join('|')
  return a.ref || ''
}

function answerHint(type) {
  if (type === 'SINGLE') return '如 B'
  if (type === 'MULTI') return '如 A,C'
  if (type === 'JUDGE') return '正确 / 错误'
  if (type === 'BLANK') return '多个空用 | 分隔'
  return '参考要点'
}

function failText(quality) {
  const bad = (quality?.checks || []).filter((c) => !c.pass).map((c) => c.name)
  return bad.join('、') || '—'
}

function difficultyLabel(v) {
  return v === 'easy' ? '易' : v === 'hard' ? '难' : '中'
}

function reset() {
  drafts.value = []
  error.value = ''
  genMeta.value = ''
}

async function rejectOne(d) {
  d.rejected = true
  d.checked = false
  if (d.id) {
    try {
      await aiQuestionApi.reject(d.id)
    } catch (e) {
      notify(e?.message || '弃用失败', false)
    }
  }
}

/** 采纳：先标记状态（留痕），再把题目回传给试卷编辑页，交给原有保存链路 */
async function adopt() {
  const picked = drafts.value.filter((d) => d.checked && !d.rejected)
  if (!picked.length) return
  adopting.value = true
  error.value = ''
  try {
    const ids = picked.filter((d) => d.id).map((d) => d.id)
    if (ids.length) {
      // 顺便把教师在弹窗里改过的题干/答案写回草稿，留痕可查
      for (const d of picked) {
        if (!d.id) continue
        await aiQuestionApi.updateDraft(d.id, {
          stem: d.stem,
          qType: d.qType,
          optionsJson: JSON.stringify(d.options || []),
          answerJson: JSON.stringify(textToAnswer(d)),
          analysis: d.analysis || '',
          fullScore: d.fullScore
        })
      }
      await aiQuestionApi.adopt(ids)
    }
    emit('adopt', picked.map((d) => toPaperQuestion(d)))
    notify(`已采纳 ${picked.length} 道题，别忘了点「保存题目」`, true)
    emit('close')
  } catch (e) {
    error.value = e?.message || '采纳失败'
    notify(error.value, false)
  } finally {
    adopting.value = false
  }
}

function textToAnswer(d) {
  const t = (d.answerText || '').trim()
  if (d.qType === 'SINGLE' || d.qType === 'MULTI') {
    return { keys: t.split(/[,，\s]+/).map((s) => s.trim().toUpperCase()).filter(Boolean) }
  }
  if (d.qType === 'JUDGE') return { value: !t.startsWith('错') && !t.startsWith('×') }
  if (d.qType === 'BLANK') return { answers: t.split(/[|｜]/).map((s) => s.trim()).filter(Boolean) }
  return { ref: t }
}

/** 转成试卷编辑页认的题目结构（见 pages/teacher/Papers.vue 的 questions） */
function toPaperQuestion(d) {
  const a = textToAnswer(d)
  const q = {
    qType: d.qType,
    groupName: d.groupName || '',
    fullScore: Number(d.fullScore) || 2,
    stem: d.stem,
    analysis: d.analysis || '',
    options: (d.options || []).map((o) => ({ key: o.key, text: o.text })),
    answerKeys: [],
    answerBool: true,
    answerText: ''
  }
  if (d.qType === 'SINGLE') q.answerKeys = [a.keys[0] || '']
  else if (d.qType === 'MULTI') q.answerKeys = a.keys.slice()
  else if (d.qType === 'JUDGE') q.answerBool = a.value
  else if (d.qType === 'BLANK') q.answerText = a.answers.join('\n')
  else q.answerText = a.ref
  return q
}
</script>
