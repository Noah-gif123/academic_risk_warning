<script setup>
import { ref, watch } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { agentAsk } from '../../api/http.js'

/**
 * 智能体工具问答（W3）：教师用自然语言问学情，DataQueryAgent 自主调用查询工具后作答。
 * 面板同时展示"调了哪些工具、参数是什么、耗时多久"，让结论可核对。
 */
const props = defineProps({
  studentId: { type: Number, default: null },
  courseId: { type: Number, default: null }
})

const { teacherToken } = useAuth()
const question = ref('')
const loading = ref(false)
const answer = ref('')
const trace = ref([])
const usedTools = ref([])
const rounds = ref(0)
const durationMs = ref(0)
const evidence = ref([])
const error = ref('')

const EXAMPLES = [
  '这个学生现在风险有多高？主要风险来自哪里？',
  '他哪几门课最危险？',
  '他有哪些薄弱知识点？该先补什么？',
  '和上次相比，他的情况是变好还是变差？'
]

async function ask(text) {
  const q = (text ?? question.value).trim()
  if (!q || loading.value || !props.studentId) return
  question.value = q
  loading.value = true
  error.value = ''
  answer.value = ''
  trace.value = []
  try {
    const res = await agentAsk(teacherToken.value, props.studentId, props.courseId, q)
    if (res?.success) {
      answer.value = res.answer || ''
      trace.value = res.toolTrace || []
      usedTools.value = res.usedTools || []
      rounds.value = res.rounds || 0
      durationMs.value = res.durationMs || 0
      evidence.value = res.evidence || []
    } else {
      error.value = res?.message || '智能体未能作答'
    }
  } catch (e) {
    error.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

watch(() => props.studentId, () => {
  answer.value = ''
  trace.value = []
  error.value = ''
  question.value = ''
})

function fmtMs(ms) {
  if (ms == null) return '-'
  return ms < 1000 ? ms + ' ms' : (ms / 1000).toFixed(1) + ' s'
}

function fmtArgs(args) {
  if (!args) return '{}'
  try {
    return JSON.stringify(args)
  } catch {
    return String(args)
  }
}
</script>

<template>
  <div class="ata-wrap">
    <div class="ata-hint">
      用自然语言问这个学生的情况，智能体会自己去查成绩 / 预警 / 画像 / 趋势 / 历史记忆再回答
    </div>

    <div class="ata-input-row">
      <input
        v-model="question"
        class="ata-input"
        placeholder="例如：这个学生现在风险有多高？主要风险来自哪里？"
        :disabled="loading || !studentId"
        @keydown.enter="ask()" />
      <button class="ata-btn" :disabled="loading || !studentId || !question.trim()" @click="ask()">
        {{ loading ? '智能体查询中…' : '提问' }}
      </button>
    </div>

    <div class="ata-examples">
      <button
        v-for="(ex, i) in EXAMPLES"
        :key="i"
        class="ata-chip"
        :disabled="loading || !studentId"
        @click="ask(ex)">
        {{ ex }}
      </button>
    </div>

    <div v-if="error" class="ata-error">{{ error }}</div>

    <div v-if="answer" class="ata-answer">
      <div class="ata-answer-head">
        <span class="ata-answer-title">🤖 智能体回答</span>
        <span class="ata-meta">
          工具轮次 {{ rounds }} · 调用 {{ trace.length }} 次 · {{ fmtMs(durationMs) }}
        </span>
      </div>
      <p class="ata-answer-text">{{ answer }}</p>

      <div v-if="evidence.length" class="ata-evidence">
        <span class="ata-evidence-label">关键数据：</span>
        <span v-for="(ev, i) in evidence" :key="i" class="ata-evidence-tag">{{ ev }}</span>
      </div>

      <div v-if="trace.length" class="ata-trace">
        <div class="ata-trace-title">🔧 工具调用轨迹（{{ usedTools.join(' → ') }}）</div>
        <div v-for="(t, i) in trace" :key="i" class="ata-trace-item" :class="{ failed: !t.success }">
          <span class="ata-trace-step">{{ i + 1 }}</span>
          <span class="ata-trace-name">{{ t.name }}</span>
          <span class="ata-trace-args">{{ fmtArgs(t.arguments) }}</span>
          <span class="ata-trace-ms">{{ fmtMs(t.durationMs) }}</span>
          <span class="ata-trace-status">{{ t.success ? '✓' : '✗ ' + (t.error || '失败') }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="ata-loading">智能体正在按需查询数据，请稍候…</div>
  </div>
</template>

<style scoped>
.ata-wrap { display: flex; flex-direction: column; gap: 10px; }
.ata-hint { font-size: 0.78rem; color: var(--text-tertiary, #888); }
.ata-input-row { display: flex; gap: 8px; }
.ata-input {
  flex: 1; padding: 8px 12px; border: 1px solid var(--border-light, #e5e5e5);
  border-radius: 8px; font-size: 0.85rem; background: var(--bg-card, #fff);
  color: var(--text-primary, #333);
}
.ata-input:focus { outline: none; border-color: #6366f1; }
.ata-btn {
  padding: 8px 16px; border-radius: 8px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
  font-size: 0.82rem; font-weight: 600;
}
.ata-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ata-examples { display: flex; flex-wrap: wrap; gap: 6px; }
.ata-chip {
  font-size: 0.72rem; padding: 4px 10px; border-radius: 12px; cursor: pointer;
  border: 1px solid #e0e7ff; background: #eef2ff; color: #4338ca;
}
.ata-chip:disabled { opacity: 0.5; cursor: not-allowed; }
.ata-error { font-size: 0.8rem; color: #b91c1c; background: #fef2f2; border-radius: 8px; padding: 8px 10px; }
.ata-loading { font-size: 0.8rem; color: #6b7280; }
.ata-answer { background: #f8fafc; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; }
.ata-answer-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 6px; }
.ata-answer-title { font-size: 0.82rem; font-weight: 700; color: #374151; }
.ata-meta { font-size: 0.7rem; color: #9ca3af; }
.ata-answer-text { margin: 0; font-size: 0.85rem; line-height: 1.7; color: #1f2937; white-space: pre-wrap; }
.ata-evidence { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.ata-evidence-label { font-size: 0.7rem; color: #6b7280; }
.ata-evidence-tag {
  font-size: 0.68rem; padding: 2px 8px; border-radius: 4px;
  background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857;
}
.ata-trace { margin-top: 10px; border-top: 1px dashed #e5e7eb; padding-top: 8px; }
.ata-trace-title { font-size: 0.72rem; color: #6b7280; font-weight: 600; margin-bottom: 6px; }
.ata-trace-item {
  display: flex; align-items: center; gap: 8px; font-size: 0.72rem;
  padding: 3px 0; color: #374151;
}
.ata-trace-item.failed { color: #b91c1c; }
.ata-trace-step {
  width: 16px; height: 16px; border-radius: 50%; background: #e0e7ff; color: #4338ca;
  display: inline-flex; align-items: center; justify-content: center; font-size: 0.65rem; flex-shrink: 0;
}
.ata-trace-name { font-weight: 600; }
.ata-trace-args { color: #9ca3af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 260px; }
.ata-trace-ms { color: #9ca3af; margin-left: auto; }
.ata-trace-status { flex-shrink: 0; }
</style>
