<template>
  <div class="border border-slate-200 rounded-lg p-3 space-y-2">
    <div class="flex items-center justify-between gap-2">
      <p class="font-medium text-slate-700">课程答疑（带引用）</p>
      <span class="text-[11px] text-slate-400">只根据本课程资料回答，找不到依据会直接拒答</span>
    </div>

    <div class="flex items-center gap-2">
      <input v-model="question" class="input flex-1 text-sm" placeholder="例如：期末考试的占比是多少？缺勤怎么扣分？"
             @keyup.enter="ask" />
      <button class="btn-primary text-sm" :disabled="!question.trim() || busy" @click="ask">
        {{ busy ? '查询中…' : '提问' }}
      </button>
    </div>

    <p v-if="error" class="text-xs text-red-600">{{ error }}</p>

    <div v-if="answer" class="rounded-lg px-3 py-2" :class="refused ? 'bg-amber-50' : 'bg-slate-50'">
      <p class="text-sm text-slate-700 whitespace-pre-line leading-relaxed">{{ answer }}</p>
      <div v-if="citations.length" class="flex flex-wrap gap-1.5 mt-2">
        <span v-for="(c, i) in citations" :key="i"
              class="text-[11px] px-2 py-0.5 rounded bg-white border border-slate-200 text-slate-500">
          依据：{{ c.docName }} 第{{ c.sectionNo }}段
        </span>
      </div>
      <p class="text-[11px] text-slate-400 mt-1">
        {{ refused ? '未检索到可靠依据，已拒答（不编造）' : '命中 ' + hitCount + ' 段材料，最高相关度 ' + topScore }}
      </p>
    </div>

    <div v-if="hits.length && !answer" class="text-[11px] text-slate-400">
      最近一次命中：{{ hits.slice(0, 3).map((h) => h.docName + ' 第' + h.sectionNo + '段').join(' / ') }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { qaApi } from '../api/ai'

const props = defineProps({
  teachingClassId: { type: [Number, String], default: null },
  courseId: { type: [Number, String], default: null }
})

const question = ref('')
const answer = ref('')
const citations = ref([])
const hits = ref([])
const refused = ref(false)
const hitCount = ref(0)
const topScore = ref('—')
const busy = ref(false)
const error = ref('')

async function ask() {
  const q = question.value.trim()
  if (!q || busy.value) return
  busy.value = true
  error.value = ''
  answer.value = ''
  citations.value = []
  try {
    const r = await qaApi.ask({
      question: q,
      teachingClassId: props.teachingClassId || undefined,
      courseId: props.courseId || undefined
    })
    answer.value = r.answer || ''
    citations.value = r.citations || []
    hits.value = r.hits || []
    refused.value = !!r.refused
    hitCount.value = r.hitCount || 0
    topScore.value = r.topScore == null ? '—' : Number(r.topScore).toFixed(3)
  } catch (e) {
    error.value = e?.message || '提问失败'
  } finally {
    busy.value = false
  }
}
</script>
