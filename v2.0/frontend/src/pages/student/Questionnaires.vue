<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <h1 class="text-lg font-semibold text-slate-800">问卷调查</h1>
        <span class="tag bg-slate-100 text-slate-500">匿名问卷 · 评教 / 满意度 / 心理健康 / 去向 / 学情</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      问卷由导员或教务按行政班发布，<b>匿名作答</b>：系统只记录是否提交，不记录是谁答的。
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
        <p class="text-sm text-slate-700 mb-2">
          {{ i + 1 }}. {{ q.text }}
          <span v-if="q.type === 'TEXT'" class="text-xs text-slate-400 ml-1">（选填）</span>
        </p>
        <textarea v-if="q.type === 'TEXT'" v-model="texts[q.itemCode]" rows="3" maxlength="500"
                  class="input w-full" placeholder="可填写你的想法（选填，500 字以内）"></textarea>
        <div v-else class="flex flex-wrap gap-2">
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
        <p class="text-xs text-slate-400">提交后不可修改；结果与你的身份无关，只用于学校层面的统计。</p>
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
const texts = reactive({})
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
    Object.keys(texts).forEach((k) => delete texts[k])
    ;(d.questions || []).forEach((it) => {
      if (it.type === 'TEXT') texts[it.itemCode] = texts[it.itemCode] || ''
      else answers[it.itemCode] = answers[it.itemCode] || 0
    })
  } catch (e) { toast(e.message, false) }
}

async function submit() {
  if (!current.value) return
  const questions = current.value.questions || []
  const missing = questions.filter((it) => it.type !== 'TEXT' && !answers[it.itemCode])
  if (missing.length) { toast('还有题目没有选择：' + missing[0].text, false); return }
  const items = questions.map((it) => (it.type === 'TEXT'
    ? { itemCode: it.itemCode, text: (texts[it.itemCode] || '').trim() }
    : { itemCode: it.itemCode, score: answers[it.itemCode] }))
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
