<script setup>
import { ref, watch, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { getAgentRuns, getAgentEval } from '../../api/http.js'

/**
 * 智能体运行历史（W1 可观测 + W2 校验/反思）
 * 每次流水线一条记录，展开可看每个智能体的耗时、校验结论、反思重写与失败原因。
 */
const props = defineProps({
  studentId: { type: Number, default: null },
  limit: { type: Number, default: 5 }
})

const { teacherToken } = useAuth()
const runs = ref([])
const loading = ref(false)
const error = ref('')
const expanded = ref(null)

const evalLoading = ref(false)
const evalResult = ref(null)
const evalError = ref('')

const STATUS_TEXT = { SUCCESS: '成功', PARTIAL: '部分成功', FAILED: '失败', RUNNING: '运行中' }
const VALIDATION_TEXT = { PASS: '校验通过', WARN: '校验告警', FAIL: '校验未通过', SKIP: '' }

async function load() {
  if (!props.studentId) return
  loading.value = true
  error.value = ''
  try {
    const res = await getAgentRuns(teacherToken.value, props.studentId, props.limit)
    if (res?.success) {
      runs.value = res.data || []
    } else {
      runs.value = []
      error.value = res?.message || '加载运行记录失败'
    }
  } catch (e) {
    runs.value = []
    error.value = e?.message || '加载运行记录失败'
  } finally {
    loading.value = false
  }
}

/** W2：跑一次 golden set 评测（非 live，仅用已落库结果，不消耗大模型额度） */
async function runEval() {
  evalLoading.value = true
  evalError.value = ''
  try {
    const res = await getAgentEval(teacherToken.value, false, 20)
    if (res?.success) evalResult.value = res.data
    else evalError.value = res?.message || '评测失败'
  } catch (e) {
    evalError.value = e?.message || '评测失败'
  } finally {
    evalLoading.value = false
  }
}

onMounted(() => { load() })
watch(() => props.studentId, () => { expanded.value = null; evalResult.value = null; load() })

function statusText(s) { return STATUS_TEXT[s] || s || '未知' }
function statusClass(s) { return (s || '').toLowerCase() }
function validationText(v) { return VALIDATION_TEXT[v] ?? '' }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
function fmtMs(ms) {
  if (ms == null) return '-'
  return ms < 1000 ? ms + ' ms' : (ms / 1000).toFixed(1) + ' s'
}
function toggle(runId) { expanded.value = expanded.value === runId ? null : runId }

defineExpose({ load })
</script>

<template>
  <div class="arh-wrap">
    <div class="arh-head">
      <span class="arh-hint">每次流水线一条记录，展开可看每个智能体的耗时、校验结论与反思</span>
      <button class="arh-btn" :disabled="evalLoading" @click="runEval">
        {{ evalLoading ? '评测中…' : '🧪 golden set 评测' }}
      </button>
      <button class="arh-btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>

    <div v-if="evalError" class="arh-error">{{ evalError }}</div>
    <div v-else-if="evalResult" class="arh-eval">
      golden set（{{ evalResult.mode }}）：{{ evalResult.passed }}/{{ evalResult.total }} 通过，
      通过率 {{ evalResult.passRate }}%
      <span v-for="c in (evalResult.cases || []).filter(x => !x.pass)" :key="c.caseId" class="arh-eval-fail">
        · {{ c.caseId }}：{{ c.reason }}
      </span>
    </div>

    <div v-if="error" class="arh-error">{{ error }}</div>
    <div v-else-if="loading && !runs.length" class="arh-empty">加载中…</div>
    <div v-else-if="!runs.length" class="arh-empty">
      暂无运行记录（在下方点「生成三维画像」跑一次完整评估后即可看到）
    </div>

    <div v-else class="arh-list">
      <div v-for="run in runs" :key="run.runId" class="arh-item">
        <div class="arh-row" @click="toggle(run.runId)">
          <span class="arh-status" :class="statusClass(run.status)">{{ statusText(run.status) }}</span>
          <span class="arh-pipeline">{{ run.pipeline }}</span>
          <span class="arh-time">{{ fmtTime(run.createTime) }}</span>
          <span class="arh-ms">{{ fmtMs(run.totalMs) }}</span>
          <span class="arh-steps">{{ run.succeededSteps }}/{{ run.totalSteps }} 步成功</span>
          <span class="arh-toggle">{{ expanded === run.runId ? '收起' : '展开' }}</span>
        </div>
        <div v-if="expanded === run.runId" class="arh-steps-box">
          <div v-for="s in (run.steps || [])" :key="s.stepNo" class="arh-step-wrap">
            <div class="arh-step">
              <span class="arh-step-no">{{ s.stepNo }}</span>
              <span class="arh-agent">{{ s.agent }}</span>
              <span class="arh-status sm" :class="statusClass(s.status)">{{ statusText(s.status) }}</span>
              <span v-if="validationText(s.validationStatus)" class="arh-validation" :class="statusClass(s.validationStatus)">
                {{ validationText(s.validationStatus) }}
              </span>
              <span v-if="s.attempts > 1" class="arh-attempts">🔁 反思重写 ×{{ s.attempts }}</span>
              <span class="arh-step-ms">{{ fmtMs(s.durationMs) }}</span>
              <span v-if="s.error" class="arh-step-err" :title="s.error">{{ s.error }}</span>
            </div>
            <div v-if="s.validationDetail && s.validationStatus !== 'PASS'" class="arh-step-note">
              校验：{{ s.validationDetail }}
            </div>
            <div v-if="s.reflection" class="arh-step-note arh-step-reflect">
              反思：{{ s.reflection }}
            </div>
          </div>
          <div v-if="run.error" class="arh-run-err">失败原因：{{ run.error }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.arh-wrap { display: flex; flex-direction: column; gap: 8px; }
.arh-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.arh-hint { flex: 1; font-size: 12px; color: var(--text-tertiary, #94a3b8); }
.arh-btn {
  padding: 4px 12px; font-size: 12px; border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 6px; background: #fff; color: var(--text-secondary, #475569); cursor: pointer;
}
.arh-btn:disabled { opacity: .6; cursor: not-allowed; }
.arh-eval {
  padding: 8px 10px; font-size: 12px; border-radius: 8px;
  background: rgba(124, 58, 237, .08); color: #5b21b6;
}
.arh-eval-fail { color: #b45309; }
.arh-empty { padding: 14px 8px; text-align: center; font-size: 12.5px; color: var(--text-tertiary, #94a3b8); }
.arh-error { padding: 10px; font-size: 12.5px; color: #b91c1c; }
.arh-list { display: flex; flex-direction: column; gap: 6px; }
.arh-item { border: 1px solid var(--border-light, #e2e8f0); border-radius: 8px; overflow: hidden; }
.arh-row {
  display: flex; align-items: center; gap: 10px; padding: 8px 10px;
  font-size: 12.5px; cursor: pointer; background: var(--bg-card-subtle, #f8fafc);
}
.arh-row:hover { background: #eef2f7; }
.arh-status {
  padding: 1px 8px; border-radius: 999px; font-size: 11px; font-weight: 600;
  background: #e2e8f0; color: #475569;
}
.arh-status.success { background: rgba(34, 197, 94, .14); color: #15803d; }
.arh-status.partial { background: rgba(249, 115, 22, .16); color: #c2410c; }
.arh-status.failed { background: rgba(239, 68, 68, .14); color: #b91c1c; }
.arh-status.sm { padding: 0 6px; font-size: 10.5px; }
.arh-status.warn { background: rgba(234, 179, 8, .18); color: #a16207; }
.arh-status.pass { background: rgba(34, 197, 94, .14); color: #15803d; }
.arh-pipeline { font-weight: 600; color: #7c3aed; }
.arh-time, .arh-ms, .arh-steps { color: var(--text-tertiary, #94a3b8); }
.arh-toggle { margin-left: auto; color: #0ea5e9; }
.arh-steps-box { padding: 8px 10px; border-top: 1px dashed var(--border-light, #e2e8f0); }
.arh-step-wrap { padding: 3px 0; }
.arh-step { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.arh-validation { padding: 0 6px; border-radius: 999px; font-size: 10.5px; background: #e2e8f0; color: #475569; }
.arh-attempts { font-size: 11px; color: #7c3aed; }
.arh-step-no {
  width: 18px; height: 18px; line-height: 18px; text-align: center; border-radius: 50%;
  background: #e2e8f0; color: #475569; font-size: 11px;
}
.arh-agent { min-width: 110px; color: var(--text-secondary, #475569); }
.arh-step-ms { color: var(--text-tertiary, #94a3b8); }
.arh-step-err { flex: 1; color: #b91c1c; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.arh-step-note { margin-left: 26px; font-size: 11.5px; color: var(--text-tertiary, #94a3b8); line-height: 1.6; }
.arh-step-reflect { color: #7c3aed; }
.arh-run-err { margin-top: 6px; font-size: 12px; color: #b91c1c; }
</style>
