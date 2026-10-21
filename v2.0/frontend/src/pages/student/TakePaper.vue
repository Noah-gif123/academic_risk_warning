<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/student/exams')">← 返回</button>
        <h1 class="text-lg font-semibold text-slate-800">{{ paper ? paper.paperName : '加载中…' }}</h1>
        <span v-if="paper" class="tag bg-slate-100 text-slate-500">{{ typeLabel(paper.paperType) }}</span>
        <span v-if="paper" class="tag bg-slate-100 text-slate-500">满分 {{ paper.fullScore }}</span>
      </div>
      <div class="flex items-center gap-3">
        <span v-if="taking" class="text-sm font-medium" :class="remain <= 60 ? 'text-red-600' : 'text-slate-600'">
          剩余 {{ mmss }}
        </span>
        <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
      </div>
    </div>

    <p v-if="paper" class="text-xs text-slate-400">
      {{ paper.courseName }} ｜ {{ paper.teacherName }} ｜ 截止 {{ fmt(paper.endTime) }}
      <span v-if="paper.durationMinutes"> ｜ 限时 {{ paper.durationMinutes }} 分钟</span>
      ｜ 截止后不允许补交，未交按缺考记 0 分
    </p>

    <!-- 不可作答 -->
    <div v-if="paper && !taking && !result" class="card p-6 text-center space-y-2">
      <p class="text-slate-600">{{ paper.reason || '当前不能作答' }}</p>
      <button v-if="paper.myStatus !== 'NOT_STARTED'" class="btn-ghost text-sm" @click="loadResult">查看结果</button>
    </div>

    <!-- 作答 -->
    <div v-if="taking" class="space-y-3">
      <div v-for="(q, i) in paper.questions" :key="q.id" class="card p-4 space-y-2">
        <div class="flex items-start gap-2">
          <span class="text-xs text-slate-400 w-12 shrink-0">第 {{ i + 1 }} 题</span>
          <div class="flex-1">
            <p class="text-sm text-slate-800 whitespace-pre-wrap">{{ q.stem }}</p>
            <p class="text-xs text-slate-400 mt-1">{{ typeLabel(q.qType) }} ｜ {{ q.fullScore }} 分</p>

            <div class="mt-2 space-y-1" v-if="q.qType === 'SINGLE' || q.qType === 'MULTI'">
              <label v-for="o in options(q)" :key="o.key" class="flex items-center gap-2 text-sm">
                <input v-if="q.qType === 'SINGLE'" type="radio" :name="'q' + q.id" :value="o.key"
                       :checked="answers[q.id].keys[0] === o.key"
                       @change="pickSingle(q, o.key)" />
                <input v-else type="checkbox" :value="o.key" v-model="answers[q.id].keys" />
                <span>{{ o.key }}. {{ o.text }}</span>
              </label>
            </div>

            <div class="mt-2" v-else-if="q.qType === 'JUDGE'">
              <label class="flex items-center gap-2 text-sm">
                <input type="radio" :name="'q' + q.id" :checked="answers[q.id].value === true"
                       @change="answers[q.id].value = true" /> 正确
              </label>
              <label class="flex items-center gap-2 text-sm">
                <input type="radio" :name="'q' + q.id" :checked="answers[q.id].value === false"
                       @change="answers[q.id].value = false" /> 错误
              </label>
            </div>

            <div class="mt-2" v-else-if="q.qType === 'BLANK'">
              <div v-for="(b, bi) in answers[q.id].answers" :key="bi" class="flex items-center gap-2 mb-1">
                <span class="text-xs text-slate-400 w-14">第 {{ bi + 1 }} 空</span>
                <input v-model="answers[q.id].answers[bi]" class="input" />
              </div>
              <p class="text-xs text-slate-400">本题共 {{ answers[q.id].answers.length }} 空，由老师出题时确定，不能自行增删。</p>
            </div>

            <div class="mt-2" v-else>
              <textarea v-model="answers[q.id].text" rows="4" class="input" placeholder="请作答"></textarea>
            </div>
          </div>
        </div>
      </div>

      <div class="card p-4 flex items-center justify-between">
        <span class="text-xs text-slate-400">提交后不能修改；客观题立即判分，简答题等老师批阅。</span>
        <button class="btn-primary text-sm" :disabled="submitting" @click="submit(false)">
          {{ submitting ? '提交中…' : '交卷' }}
        </button>
      </div>
    </div>

    <!-- 结果 -->
    <div v-if="result" class="space-y-3">
      <div class="card p-4 grid grid-cols-2 md:grid-cols-5 gap-4 text-sm">
        <div><p class="text-xs text-slate-400">状态</p><p class="font-medium">{{ resultStatus }}</p></div>
        <div><p class="text-xs text-slate-400">客观题</p><p class="font-medium">{{ result.objectiveScore ?? '—' }}</p></div>
        <div><p class="text-xs text-slate-400">主观题</p><p class="font-medium">{{ result.subjectiveScore ?? '—' }}</p></div>
        <div><p class="text-xs text-slate-400">总分</p>
          <p class="font-medium text-emerald-600">{{ result.totalScore ?? '待老师批阅' }}</p></div>
        <div><p class="text-xs text-slate-400">用时</p><p class="font-medium">{{ durationText }}</p></div>
      </div>
      <p v-if="result.remark" class="text-xs text-slate-500">批注：{{ result.remark }}</p>

      <div v-if="!result.answers || !result.answers.length" class="card p-6 text-center text-slate-400 text-sm">
        老师未开放试卷查看，只能看到成绩。
      </div>

      <div v-for="a in (result.answers || [])" :key="a.id" class="card p-4 space-y-1">
        <div class="flex items-center gap-2">
          <span class="text-xs text-slate-400 w-12">第 {{ a.qNo }} 题</span>
          <span class="tag bg-slate-100 text-slate-500">{{ typeLabel(a.qType) }}</span>
          <span class="text-sm flex-1">{{ a.stem }}</span>
          <span v-if="!a.markedByType" class="text-xs text-amber-600">待老师批阅 · 满分 {{ a.fullScore }}</span>
          <span v-else class="text-xs" :class="a.isCorrect === 1 ? 'text-emerald-600' : 'text-red-600'">
            {{ a.score ?? 0 }} / {{ a.fullScore }}
          </span>
        </div>
        <p class="text-sm pl-14"><span class="text-xs text-slate-400">我的作答：</span>{{ answerText(a.answerJson, a.qType) }}</p>
        <p v-if="a.stdAnswerJson" class="text-sm pl-14 text-emerald-700">
          <span class="text-xs text-slate-400">标准答案：</span>{{ answerText(a.stdAnswerJson, a.qType) }}
        </p>
        <p v-if="a.analysis" class="text-sm pl-14 text-slate-500"><span class="text-xs text-slate-400">解析：</span>{{ a.analysis }}</p>
        <p v-if="a.comment" class="text-sm pl-14 text-brand-600"><span class="text-xs text-slate-400">评语：</span>{{ a.comment }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { myPaperApi } from '../../api/paper'

const route = useRoute()
const paperId = Number(route.query.paperId || 0)

const paper = ref(null)
const result = ref(null)
const answers = reactive({})
const message = ref('')
const ok = ref(true)
const submitting = ref(false)
const remain = ref(0)
/** 本地时间字符串（不要用 toISOString，那是 UTC，会让“用时”多出 8 小时） */
function nowLocal() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
const startedAt = ref(nowLocal())
let timer = null

const taking = computed(() => !!(paper.value && paper.value.canAnswer && !result.value))
const mmss = computed(() => {
  const s = Math.max(0, remain.value)
  const m = Math.floor(s / 60)
  const ss = s % 60
  return m + ':' + String(ss).padStart(2, '0')
})
const resultStatus = computed(() => {
  if (!result.value) return ''
  return { ABSENT: '缺考（0 分）', MARKED: '已批阅', SUBMITTED: '已交卷，待批阅' }[result.value.submitStatus] || result.value.submitStatus
})
const durationText = computed(() => {
  const d = result.value && result.value.durationSeconds
  if (!d) return '—'
  return Math.floor(d / 60) + ' 分 ' + (d % 60) + ' 秒'
})

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function typeLabel(t) { return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', BLANK: '填空', SHORT: '简答' }[t] || t }
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '不限' }
function options(q) {
  try { return JSON.parse(q.optionsJson || '[]') } catch (e) { return [] }
}
function answerText(json, type) {
  if (!json) return '（未作答）'
  let v
  try { v = JSON.parse(json) } catch (e) { return json }
  if (type === 'SINGLE' || type === 'MULTI') return (v.keys || []).join('、') || '（未选）'
  if (type === 'JUDGE') return v.value === true ? '正确' : (v.value === false ? '错误' : '（未选）')
  if (type === 'BLANK') return (v.answers || []).join(' ｜ ') || '（未填）'
  return v.text || '（未答）'
}

/** 填空题的空数：优先用后端给的口径（老师标准答案条数），没有再按题干的下划线组数兜底 */
function blankCount(q) {
  if (q.blankCount) return Math.max(1, Number(q.blankCount))
  const groups = String(q.stem || '').match(/[_＿]{2,}/g) || []
  return Math.max(1, groups.length)
}

/** 初始化每题的空答案结构 */
function initAnswers(list) {
  list.forEach((q) => {
    if (answers[q.id]) return
    if (q.qType === 'SINGLE' || q.qType === 'MULTI') answers[q.id] = { keys: [] }
    else if (q.qType === 'JUDGE') answers[q.id] = { value: null }
    else if (q.qType === 'BLANK') answers[q.id] = { answers: new Array(blankCount(q)).fill('') }
    else answers[q.id] = { text: '' }
  })
}
function pickSingle(q, key) {
  answers[q.id].keys = [key]
}

function payload() {
  return {
    startedAt: startedAt.value,
    answers: (paper.value.questions || []).map((q) => {
      const a = answers[q.id] || {}
      let json = null
      if (q.qType === 'SINGLE' || q.qType === 'MULTI') json = JSON.stringify({ keys: a.keys || [] })
      else if (q.qType === 'JUDGE') json = JSON.stringify({ value: a.value })
      else if (q.qType === 'BLANK') json = JSON.stringify({ answers: (a.answers || []).map((x) => x || '') })
      else json = JSON.stringify({ text: a.text || '' })
      return { questionId: q.id, answerJson: json }
    })
  }
}

async function submit(auto) {
  if (submitting.value) return
  submitting.value = true
  try {
    result.value = await myPaperApi.submit(paperId, payload())
    if (timer) clearInterval(timer)
    toast(auto ? '时间到，已自动交卷' : '交卷成功')
  } catch (e) { toast(e.message, false) }
  submitting.value = false
}

async function loadResult() {
  try { result.value = await myPaperApi.result(paperId) }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    paper.value = await myPaperApi.get(paperId)
    if (paper.value.canAnswer) {
      initAnswers(paper.value.questions || [])
      remain.value = paper.value.remainingSeconds || 0
      timer = setInterval(() => {
        remain.value -= 1
        if (remain.value <= 0) {
          clearInterval(timer)
          submit(true)
        }
      }, 1000)
    } else if (paper.value.myStatus && paper.value.myStatus !== 'NOT_STARTED') {
      await loadResult()
    }
  } catch (e) { toast(e.message, false) }
})

onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>
