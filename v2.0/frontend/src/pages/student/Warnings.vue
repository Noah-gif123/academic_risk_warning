<template>
  <div class="space-y-4">
    <!-- 标题 -->
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <h1 class="text-lg font-semibold text-slate-800">学情预警</h1>
        <span v-if="data.termName" class="tag bg-slate-100 text-slate-500">{{ data.termName }}</span>
        <span v-if="data.engineVersion" class="text-xs text-slate-400">引擎 {{ data.engineVersion }}</span>
        <span v-if="data.lastEvalTime" class="text-xs text-slate-400">最近判定 {{ fmt(data.lastEvalTime) }}</span>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新' }}</button>
        <button class="btn-ghost text-sm" @click="$router.push('/student/notifications')">消息中心</button>
      </div>
    </div>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <p class="text-xs text-slate-400">
      这里的结论由考勤与成绩自动判定：先看缺课是否超过上限（有效课次 ÷ 3），再结合加权分与扣分给出红 / 橙 / 黄分级。
      结论有疑问可以在详情里提交申诉，由任课教师核实。
    </p>

    <!-- 概览 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
      <div v-for="c in tiles" :key="c.key" class="card p-4">
        <p class="text-xs text-slate-400">{{ c.label }}</p>
        <p class="text-2xl font-semibold mt-1" :class="c.color">{{ c.value }}</p>
        <p class="text-xs text-slate-400 mt-1 truncate">{{ c.hint }}</p>
      </div>
    </div>

    <!-- 总体建议 -->
    <div v-if="data.advice?.length" class="card p-4">
      <h2 class="font-medium text-slate-700 mb-2">给你的建议</h2>
      <ul class="space-y-1.5">
        <li v-for="(a, i) in data.advice" :key="i" class="text-sm text-slate-600 flex gap-2">
          <span class="text-brand-500">·</span><span>{{ a }}</span>
        </li>
      </ul>
    </div>

    <!-- 课程结论 -->
    <div class="grid md:grid-cols-2 xl:grid-cols-3 gap-3">
      <div v-for="it in data.items" :key="it.enrollmentId" class="card p-4 space-y-2.5">
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <p class="font-medium text-slate-800 truncate" :title="it.courseName">{{ it.courseName }}</p>
            <p class="text-xs text-slate-400 truncate">
              {{ it.classCode }}<span v-if="it.teacherName"> · {{ it.teacherName }}</span>
            </p>
          </div>
          <span class="tag shrink-0" :class="riskLevelBadge(it.level)">{{ riskLevelLabel(it.level) }}</span>
        </div>

        <p class="text-sm" :class="textColor(it.level)">{{ it.conclusion }}</p>

        <div class="grid grid-cols-3 gap-2 text-xs text-slate-500">
          <div>
            <p class="text-slate-400">缺课 / 上限</p>
            <p class="text-slate-700 font-medium">{{ it.absentCount ?? 0 }} / {{ it.allowedAbsent ?? '—' }}</p>
          </div>
          <div>
            <p class="text-slate-400">综合分</p>
            <p class="text-slate-700 font-medium">{{ it.totalScore == null ? '—' : num(it.totalScore) }}</p>
          </div>
          <div>
            <p class="text-slate-400">期末需考</p>
            <p class="font-medium" :class="it.finalUnreachable ? 'text-red-600' : 'text-slate-700'">
              {{ it.finalUnreachable ? '不可达' : (it.finalExamNeeded == null ? '—' : num(it.finalExamNeeded)) }}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-2 pt-0.5">
          <button class="text-sm text-brand-600 hover:underline" @click="openDetail(it)">查看详情</button>
          <button v-if="canAppeal(it)" class="text-sm text-amber-600 hover:underline" @click="openDetail(it, true)">
            申诉
          </button>
          <span v-if="it.studentResponse" class="tag bg-slate-100 text-slate-500">已申诉·待核实</span>
          <span v-else-if="it.status === 'CLOSED'" class="tag bg-slate-100 text-slate-500">老师已处理</span>
        </div>
      </div>

      <div v-if="!loading && !data.items?.length" class="card p-8 text-center text-slate-400 md:col-span-2 xl:col-span-3">
        本学期暂无在修课程，等有考勤与成绩数据后会自动生成学情结论。
      </div>
    </div>

    <!-- 判定口径 -->
    <div class="card p-4">
      <details>
        <summary class="cursor-pointer text-sm font-medium text-slate-700">判定口径（怎么算出这个结论的）</summary>
        <ul class="mt-2 space-y-1.5">
          <li v-for="(g, i) in data.guidelines" :key="i" class="text-xs text-slate-500 flex gap-2">
            <span class="text-slate-300">{{ i + 1 }}.</span><span>{{ g }}</span>
          </li>
        </ul>
      </details>
    </div>

    <!-- 详情抽屉 -->
    <div v-if="detail" class="fixed inset-0 z-40 bg-slate-900/30 flex justify-end" @click.self="detail = null">
      <div class="w-full max-w-xl h-full bg-white shadow-xl overflow-y-auto">
        <div class="sticky top-0 bg-white border-b border-slate-200 px-5 py-3 flex items-center justify-between">
          <div class="flex items-center gap-2 flex-wrap">
            <h2 class="font-semibold text-slate-800">{{ detail.item.courseName }}</h2>
            <span class="tag" :class="riskLevelBadge(detail.item.level)">{{ riskLevelLabel(detail.item.level) }}</span>
            <span v-if="detail.item.studentResponse" class="tag bg-amber-50 text-amber-700 border border-amber-200">
              已申诉·待核实
            </span>
          </div>
          <button class="text-slate-400 hover:text-slate-600" @click="detail = null">✕</button>
        </div>

        <div class="p-5 space-y-4 text-sm">
          <p class="text-xs text-slate-400">
            {{ detail.item.classCode }}<span v-if="detail.item.teacherName"> · 任课教师 {{ detail.item.teacherName }}</span>
          </p>

          <p class="text-slate-700">{{ detail.item.conclusion }}</p>

          <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">缺课 / 上限</p>
              <p class="text-lg font-semibold">{{ detail.item.absentCount ?? 0 }} / {{ detail.item.allowedAbsent ?? '—' }}</p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">迟到 / 请假</p>
              <p class="text-lg font-semibold">{{ detail.item.lateCount ?? 0 }} / {{ detail.item.leaveCount ?? 0 }}</p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">综合分</p>
              <p class="text-lg font-semibold">{{ detail.item.totalScore == null ? '—' : num(detail.item.totalScore) }}</p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">期末需考</p>
              <p class="text-lg font-semibold" :class="detail.item.finalUnreachable ? 'text-red-600' : ''">
                {{ detail.item.finalUnreachable ? '不可达' : (detail.item.finalExamNeeded == null ? '—' : num(detail.item.finalExamNeeded)) }}
              </p>
            </div>
          </div>

          <!-- 三点依据（学生版措辞） -->
          <div class="border border-slate-200 rounded-lg p-3 space-y-1.5 text-xs text-slate-600">
            <p class="font-medium text-slate-700 text-sm">判定依据</p>
            <p>
              <span class="tag bg-brand-50 text-brand-700 mr-1">缺课硬线</span>
              <span v-if="detail.item.hardFail" class="text-red-600 font-medium">
                缺课 {{ detail.item.absentCount }} 次已超过上限 {{ detail.item.allowedAbsent }} 次，按学校规定认定硬性不合格
              </span>
              <span v-else-if="detail.item.level === 'GREEN'" class="text-emerald-600">缺课在允许范围内</span>
              <span v-else>缺课 {{ detail.item.absentCount ?? 0 }} 次、上限 {{ detail.item.allowedAbsent ?? '—' }} 次，已接近或触及红线</span>
            </p>
            <p>
              <span class="tag bg-indigo-50 text-indigo-700 mr-1">成绩加权分</span>
              <template v-if="detail.item.totalScore != null || detail.item.channelBScore != null">
                加权分 {{ detail.item.totalScore == null ? '—' : num(detail.item.totalScore) }}，
                风险分 {{ num(detail.item.riskScore) }}
                <span v-if="detail.item.channelBRank">，班内第 {{ detail.item.channelBRank }} / {{ detail.item.channelBClassSize }}</span>
              </template>
              <span v-else class="text-slate-400">还没有可用的成绩数据，本次只依据考勤行为</span>
            </p>
            <p>
              <span class="tag bg-slate-100 text-slate-500 mr-1">置信度</span>
              {{ pct(detail.item.confidence) }}（{{ outputModeLabel(detail.item.outputMode) }}）
              <span v-if="detail.item.outputMode === 'BEHAVIOR_ONLY' || detail.item.outputMode === 'INSUFFICIENT'"
                    class="text-amber-600">· 数据不完整，结论仅供参考</span>
            </p>
            <p v-if="detail.item.reason" class="text-slate-500 pt-1 border-t border-slate-100">
              系统描述：{{ detail.item.reason }}
            </p>
          </div>

          <!-- 建议 -->
          <div class="border border-slate-200 rounded-lg p-3">
            <p class="font-medium text-slate-700 mb-2">建议怎么做</p>
            <ul class="space-y-1.5">
              <li v-for="(a, i) in detail.advice" :key="i" class="text-xs text-slate-600 flex gap-2">
                <span class="text-brand-500">{{ i + 1 }}.</span><span>{{ a }}</span>
              </li>
            </ul>
          </div>

          <!-- 申诉 -->
          <div class="border border-slate-200 rounded-lg p-3 space-y-2">
            <p class="font-medium text-slate-700">申诉</p>
            <template v-if="detail.item.studentResponse">
              <p class="text-xs text-slate-500">
                已于 {{ fmt(detail.item.studentResponseTime) }} 提交：
                <span class="text-slate-700">{{ detail.item.studentResponse }}</span>
              </p>
              <p class="text-xs text-slate-400">任课教师核实后会更新结论，结果会出现在消息中心。</p>
            </template>
            <template v-else-if="canAppeal(detail.item)">
              <textarea v-model="appealReason" class="input w-full h-20" maxlength="300"
                        placeholder="说明你的实际情况（如：请假已获批但未录入、课程调整、成绩有误等）"></textarea>
              <div class="flex items-center gap-2">
                <button class="btn-primary text-sm" :disabled="appealing" @click="submitAppeal">
                  {{ appealing ? '提交中…' : '提交申诉' }}
                </button>
                <span class="text-xs text-slate-400">每门课只能提交一次</span>
              </div>
            </template>
            <p v-else-if="!detail.item.alertId" class="text-xs text-slate-400">
              这门课目前没有预警，不需要申诉；如对成绩或考勤记录有疑问，可以直接联系任课教师{{ detail.item.teacherName ? '（' + detail.item.teacherName + '）' : '' }}核对。
            </p>
            <p v-else class="text-xs text-slate-400">
              该预警已处理结束（老师已关闭或忽略），无需再申诉；如仍有疑问请联系任课教师或辅导员。
            </p>
          </div>

          <!-- 时间线 -->
          <div class="border border-slate-200 rounded-lg p-3">
            <p class="font-medium text-slate-700 mb-2">过程记录</p>
            <ol class="relative border-l border-slate-200 ml-2 space-y-3">
              <li v-for="(t, i) in detail.timeline" :key="i" class="ml-3">
                <span class="absolute -left-1 w-2 h-2 rounded-full bg-slate-300 mt-1.5"></span>
                <p class="text-xs text-slate-400">{{ fmt(t.eventTime) }}</p>
                <p class="text-sm text-slate-700">{{ t.label }}</p>
              </li>
              <li v-if="!detail.timeline?.length" class="ml-3 text-xs text-slate-400">暂无过程记录</li>
            </ol>
          </div>

          <!-- 口径 -->
          <details class="border border-slate-200 rounded-lg p-3">
            <summary class="cursor-pointer text-sm font-medium text-slate-700">判定口径</summary>
            <ul class="mt-2 space-y-1.5">
              <li v-for="(g, i) in detail.guidelines" :key="i" class="text-xs text-slate-500 flex gap-2">
                <span class="text-slate-300">{{ i + 1 }}.</span><span>{{ g }}</span>
              </li>
            </ul>
          </details>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { studentWarningApi } from '../../api/alert'
import { outputModeLabel, riskLevelBadge, riskLevelLabel } from '../../labels'

const loading = ref(false)
const appealing = ref(false)
const message = ref('')
const ok = ref(true)

const data = reactive({ studentNo: '', studentName: '', termName: '', engineVersion: '', lastEvalTime: null, summary: {}, items: [], advice: [], guidelines: [] })
const detail = ref(null)
const appealReason = ref('')

const tiles = computed(() => {
  const s = data.summary || {}
  return [
    { key: 'red', label: '红色预警', value: s.red ?? 0, color: 'text-red-600', hint: '缺课超限或期末不可达' },
    { key: 'orange', label: '橙色预警', value: s.orange ?? 0, color: 'text-orange-500', hint: '风险偏高，需要改善' },
    { key: 'yellow', label: '黄色预警', value: s.yellow ?? 0, color: 'text-amber-600', hint: '存在风险，注意保持' },
    { key: 'green', label: '正常课程', value: s.green ?? 0, color: 'text-emerald-600', hint: '在安全区内' }
  ]
})

function num(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return Number.isFinite(n) ? (Math.round(n * 10) / 10).toString() : String(v)
}

function pct(v) {
  if (v === null || v === undefined) return '—'
  return Math.round(Number(v) * 100) + '%'
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

function textColor(level) {
  const m = {
    RED: 'text-red-600',
    ORANGE: 'text-orange-600',
    YELLOW: 'text-amber-700',
    GREEN: 'text-slate-600'
  }
  return m[level] || 'text-slate-600'
}

function canAppeal(it) {
  if (!it || !it.alertId) return false
  if (it.studentResponse) return false
  return it.status !== 'CLOSED' && it.status !== 'DISMISSED'
}

function flash(text, success = true) {
  message.value = text
  ok.value = success
  setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 4000)
}

async function load() {
  loading.value = true
  try {
    const d = await studentWarningApi.overview()
    Object.assign(data, d || {})
    data.items = d?.items || []
    data.advice = d?.advice || []
    data.guidelines = d?.guidelines || []
    data.summary = d?.summary || {}
  } catch (e) {
    flash(e.message, false)
  } finally {
    loading.value = false
  }
}

async function openDetail(it, focusAppeal = false) {
  try {
    detail.value = await studentWarningApi.detail(it.enrollmentId)
    appealReason.value = ''
    if (focusAppeal && canAppeal(it)) {
      setTimeout(() => {
        const box = document.querySelector('.fixed textarea')
        if (box) {
          box.focus()
          box.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }, 120)
    }
  } catch (e) {
    flash(e.message, false)
  }
}

async function submitAppeal() {
  const it = detail.value?.item
  if (!it) return
  const reason = appealReason.value.trim()
  if (reason.length < 5) {
    flash('请把情况写清楚一些（至少 5 个字）', false)
    return
  }
  appealing.value = true
  try {
    await studentWarningApi.appeal(it.alertId, reason)
    await openDetail(it)
    await load()
    flash('申诉已提交，任课教师核实后会更新结论')
  } catch (e) {
    flash(e.message, false)
  } finally {
    appealing.value = false
  }
}

onMounted(load)
</script>
