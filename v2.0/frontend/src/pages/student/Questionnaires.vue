<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <h1 class="text-lg font-semibold text-slate-800">问卷调查</h1>
        <span class="tag bg-slate-100 text-slate-500">教师风格判定（匿名）</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      问卷由导员按行政班发布，用于标定「教师给分尺度」，**匿名作答**：系统只记录是否提交，不记录是谁答的。
    </p>

    <div v-if="!list.length" class="card p-10 text-center text-slate-400 text-sm">
      当前没有需要你作答的问卷。导员发布后这里会出现。
    </div>

    <div v-for="q in list" :key="q.id" class="card p-4 space-y-2">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 class="font-medium text-slate-800">{{ q.title }}</h2>
          <p class="text-xs text-slate-500">{{ q.questionnaireTypeText }} ｜ {{ q.courseName || '' }} ｜ 截止 {{ fmt(q.closeTime) }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="tag" :class="q.statusText === '已完成' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
            {{ q.statusText }}
          </span>
          <button v-if="q.statusText !== '已完成'" class="btn-primary text-sm" @click="open(q)">开始作答</button>
          <button v-else class="text-xs text-slate-400">已提交（匿名）</button>
        </div>
      </div>
    </div>

    <!-- 作答面板 -->
    <div v-if="current" class="card p-4 space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">{{ current.title }}</h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="current = null">收起</button>
      </div>
      <div v-for="(q, i) in current.questions" :key="q.itemCode" class="border-b border-slate-100 pb-3">
        <p class="text-sm text-slate-700 mb-2">{{ i + 1 }}. {{ q.text }}</p>
        <div class="flex flex-wrap gap-2">
          <label v-for="o in scoreOptions" :key="o.value"
                 class="px-3 py-1.5 rounded-lg border text-sm cursor-pointer"
                 :class="answers[q.itemCode] === o.value
                   ? 'bg-brand-50 border-brand-400 text-brand-700'
                   : 'border-slate-200 text-slate-600 hover:bg-slate-50'">
            <input type="radio" class="hidden" :value="o.value" v-model.number="answers[q.itemCode]" />
            {{ o.label }}
          </label>
        </div>
      </div>
      <div class="flex items-center justify-between">
        <p class="text-xs text-slate-400">提交后不可修改；结果与你的身份无关，只用于课程层面的统计。</p>
        <button class="btn-primary text-sm" @click="submit">提交问卷</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { surveyApi, scoreOptions } from '../../api/survey'

const list = ref([])
const current = ref(null)
const answers = reactive({})
const message = ref('')
const ok = ref(true)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '不限' }

async function load() {
  list.value = await surveyApi.list()
}

async function open(q) {
  try {
    const d = await surveyApi.detail(q.id)
    current.value = d
    Object.keys(answers).forEach((k) => delete answers[k])
    ;(d.questions || []).forEach((it) => { answers[it.itemCode] = answers[it.itemCode] || 0 })
  } catch (e) { toast(e.message, false) }
}

async function submit() {
  if (!current.value) return
  const items = (current.value.questions || []).map((it) => ({ itemCode: it.itemCode, score: answers[it.itemCode] }))
  if (items.some((it) => !it.score)) { toast('还有题目没有选择', false); return }
  try {
    await surveyApi.answer(current.value.id, items)
    toast('提交成功，感谢你的反馈')
    current.value = null
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
