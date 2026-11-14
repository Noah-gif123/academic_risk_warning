<template>
  <div class="space-y-5">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <h1>学情预警</h1>
          <span v-if="data.termName" class="tag bg-white text-slate-500 border border-[#e6edf5]">{{ data.termName }}</span>
          <span v-if="data.engineVersion" class="text-[11px] text-slate-400">引擎 {{ data.engineVersion }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2 max-w-3xl leading-6">
          结论由考勤与成绩自动判定：缺课是否超限、加权分与扣分共同决定红 / 橙 / 黄分级。有疑问可在详情内提交申诉。
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">{{ loading ? '刷新中…' : '刷新' }}</button>
        <button class="btn-primary text-sm" @click="$router.push('/student/reports')">学习评价</button>
        <button class="btn-ghost text-sm" @click="$router.push('/student/notifications')">消息中心</button>
      </div>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div v-for="c in tiles" :key="c.key" class="card p-4 relative overflow-hidden">
        <span class="absolute left-0 top-0 bottom-0 w-1" :class="c.key === 'red' ? 'bg-red-400' : c.key === 'orange' ? 'bg-amber-400' : c.key === 'yellow' ? 'bg-yellow-400' : 'bg-emerald-400'"></span>
        <div class="flex items-start justify-between">
          <p class="text-[11px] text-slate-400">{{ c.label }}</p>
          <span class="w-2 h-2 rounded-full" :class="c.key === 'red' ? 'bg-red-400' : c.key === 'orange' ? 'bg-amber-400' : c.key === 'yellow' ? 'bg-yellow-400' : 'bg-emerald-400'"></span>
        </div>
        <p class="text-[30px] leading-none font-semibold mt-3" :class="c.color">{{ c.value }}</p>
        <p class="text-[11px] text-slate-400 mt-2 truncate">{{ c.hint }}</p>
      </div>
    </section>

    <section v-if="data.advice?.length" class="card p-5">
      <div class="flex items-center gap-2 mb-3">
        <span class="w-1.5 h-1.5 rounded-full bg-[#0f9d8a]"></span>
        <h2>给你的建议</h2>
      </div>
      <div class="grid md:grid-cols-3 gap-3">
        <div v-for="(a, i) in data.advice" :key="i" class="rounded-xl bg-[#f7fbfd] border border-[#eaf1f7] px-4 py-3">
          <p class="text-[10px] font-semibold text-[#0f9d8a] mb-1">建议 {{ i + 1 }}</p>
          <p class="text-xs text-slate-600 leading-6">{{ a }}</p>
        </div>
      </div>
    </section>

    <section>
      <div class="flex items-end justify-between mb-3">
        <div>
          <h2>课程结论</h2>
          <p class="text-[11px] text-slate-400 mt-1">共 {{ data.items?.length || 0 }} 门在修课程，按风险等级排列</p>
        </div>
        <span class="text-[11px] text-slate-400">数据来自当前学期考勤与成绩</span>
      </div>

      <div class="grid md:grid-cols-2 xl:grid-cols-3 gap-3">
        <article v-for="it in data.items" :key="it.enrollmentId"
                 class="card p-4 relative overflow-hidden hover:-translate-y-0.5 transition-transform duration-150">
          <span class="absolute left-0 top-0 bottom-0 w-1"
                :class="it.level === 'RED' ? 'bg-red-400' : it.level === 'ORANGE' ? 'bg-amber-400' : it.level === 'YELLOW' ? 'bg-yellow-400' : 'bg-emerald-400'"></span>
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <p class="font-semibold text-[#24435c] truncate" :title="it.courseName">{{ it.courseName }}</p>
              <p class="text-[11px] text-slate-400 truncate mt-1">
                {{ it.classCode }}<span v-if="it.teacherName"> · {{ it.teacherName }}</span>
              </p>
            </div>
            <span class="tag shrink-0" :class="riskLevelBadge(it.level)">{{ riskLevelLabel(it.level) }}</span>
          </div>

          <div class="mt-3 rounded-lg bg-[#f8fbfd] border border-[#eef4f9] px-3 py-2">
            <p class="text-xs leading-5" :class="textColor(it.level)">{{ it.conclusion }}</p>
          </div>

          <div class="grid grid-cols-3 gap-2 mt-3">
            <div>
              <p class="text-[10px] text-slate-400">缺课 / 上限</p>
              <p class="text-sm font-semibold mt-1" :class="it.hardFail ? 'text-red-600' : 'text-slate-700'">{{ it.absentCount ?? 0 }} / {{ it.allowedAbsent ?? '—' }}</p>
            </div>
            <div>
              <p class="text-[10px] text-slate-400">过程分</p>
              <p class="text-sm font-semibold text-slate-700 mt-1">
                {{ it.processScore == null ? (it.totalScore == null ? '—' : num(it.totalScore)) : num(it.processScore) }}
              </p>
            </div>
            <div>
              <p class="text-[10px] text-slate-400">需考 / 还差</p>
              <p class="text-sm font-semibold mt-1" :class="it.finalUnreachable ? 'text-red-600' : 'text-slate-700'">
                {{ it.finalUnreachable ? '不可达' : (it.finalExamNeeded == null ? '—' : num(it.finalExamNeeded)) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2 mt-4 pt-3 border-t border-[#eef4f9] flex-wrap">
            <button class="text-xs font-medium text-[#0f9d8a] hover:underline" @click="openDetail(it)">查看详情</button>
            <button class="text-xs font-medium text-[#3c6f9b] hover:underline" @click="openReport(it)">课程报告</button>
            <button v-if="canAppeal(it)" class="text-xs font-medium text-amber-600 hover:underline" @click="openDetail(it, true)">申诉</button>
            <span v-if="it.teacherReply" class="tag bg-emerald-50 text-emerald-700 border border-emerald-200">老师已回复</span>
            <span v-else-if="it.studentResponse" class="tag bg-amber-50 text-amber-700 border border-amber-200">已申诉·待核实</span>
            <span v-else-if="it.status === 'CLOSED'" class="tag bg-slate-100 text-slate-500">老师已处理</span>
          </div>
        </article>

        <div v-if="!loading && !data.items?.length" class="card p-10 text-center text-slate-400 md:col-span-2 xl:col-span-3">
          本学期暂无在修课程，等有考勤与成绩数据后会自动生成学情结论。
        </div>
      </div>
    </section>

    <section class="card p-4">
      <details>
        <summary class="cursor-pointer">判定口径（怎么算出这个结论的）</summary>
        <ul class="mt-3 space-y-2">
          <li v-for="(g, i) in data.guidelines" :key="i" class="text-xs text-slate-500 flex gap-2">
            <span class="text-slate-300">{{ i + 1 }}.</span><span>{{ g }}</span>
          </li>
        </ul>
      </details>
    </section>

    <!-- 详情抽屉 -->
    <div v-if="detail" class="fixed inset-0 z-40 bg-slate-900/30 flex justify-end" @click.self="detail = null">
      <div class="w-full max-w-xl h-full bg-white shadow-xl overflow-y-auto">
        <div class="sticky top-0 bg-white border-b border-slate-200 px-5 py-3 flex items-center justify-between">
          <div class="flex items-center gap-2 flex-wrap">
            <h2 class="font-semibold text-slate-800">{{ detail.item.courseName }}</h2>
            <span class="tag" :class="riskLevelBadge(detail.item.level)">{{ riskLevelLabel(detail.item.level) }}</span>
            <span v-if="detail.item.teacherReply" class="tag bg-emerald-50 text-emerald-700 border border-emerald-200">
              老师已回复
            </span>
            <span v-else-if="detail.item.studentResponse" class="tag bg-amber-50 text-amber-700 border border-amber-200">
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
              <p class="text-xs text-slate-400">过程分</p>
              <p class="text-lg font-semibold">
                {{ detail.item.processScore == null ? (detail.item.totalScore == null ? '—' : num(detail.item.totalScore)) : num(detail.item.processScore) }}
              </p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">需考 / 还差</p>
              <p class="text-lg font-semibold" :class="detail.item.finalUnreachable ? 'text-red-600' : ''">
                {{ detail.item.finalUnreachable ? '不可达' : (detail.item.finalExamNeeded == null ? '—' : num(detail.item.finalExamNeeded)) }}
              </p>
            </div>
          </div>

          <!-- AI 预警解释卡（M9） -->
          <AlertExplainCard :alert-id="detail.item.alertId" role="student" />
          <!-- 课程答疑（M11 / RAG）：只在本课程资料里检索，带引用、无依据拒答 -->
          <CourseQaBox :teaching-class-id="detail.item.teachingClassId" />

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
              <p v-if="detail.item.teacherReply" class="text-xs text-emerald-700 bg-emerald-50 border border-emerald-200 rounded p-2">
                老师回复（{{ fmt(detail.item.teacherReplyTime) }}）：{{ detail.item.teacherReply }}
              </p>
              <p v-else class="text-xs text-slate-400">任课教师核实后会在这里给出回复，结果也会出现在消息中心。</p>
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

    <StudentReportModal :open="reportOpen" :student="reportStudent" :courses="reportCourses"
                        mode="student" @close="reportOpen = false" />
  </div>
</template>

<script setup>
import AlertExplainCard from '../../components/AlertExplainCard.vue'
import CourseQaBox from '../../components/CourseQaBox.vue'
import StudentReportModal from '../../components/StudentReportModal.vue'
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
const reportOpen = ref(false)
const reportStudent = ref(null)
const reportCourses = ref([])

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

function openReport(it) {
  reportStudent.value = {
    studentName: data.studentName,
    studentNo: data.studentNo,
    adminClassName: it.classCode,
    riskScore: it.riskScore
  }
  reportCourses.value = [{
    teachingClassId: it.teachingClassId,
    courseName: it.courseName,
    teacherName: it.teacherName,
    riskLevel: it.level,
    riskHint: it.reason,
    absentCount: it.absentCount,
    allowedAbsent: it.allowedAbsent,
    hardFail: it.hardFail,
    fail: it.finalUnreachable,
    totalScore: it.totalScore,
    deductPoints: null,
    weightedScore: it.processScore
  }]
  reportOpen.value = true
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
