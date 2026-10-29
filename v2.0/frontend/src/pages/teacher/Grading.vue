<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="goBack">← 返回{{ backLabel }}</button>
        <h1 class="text-lg font-semibold text-slate-800">交卷与批改</h1>
        <span v-if="paper" class="tag bg-brand-50 text-brand-600">{{ paper.paperName }}</span>
        <span v-if="paper" class="tag bg-slate-100 text-slate-500">满分 {{ paper.fullScore }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      单选 / 多选 / 判断由系统自动判分；<b>填空与简答需要你逐题给分</b>，系统记录判卷人（每题一行）。缺考学生记 0 分，无需批改。
    </p>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">提交列表（{{ rows.length }}）</h2>
        <span v-if="paper" class="text-xs text-slate-400">
          应到 {{ paper.totalStudents }} 人 ｜ 已交 {{ paper.submitCount }} ｜ 缺考 {{ paper.absentCount }} ｜ 批完 {{ paper.markedCount }}
        </span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
            <th class="text-left py-2">状态</th><th class="text-left py-2">批阅</th>
            <th class="text-right py-2">客观</th><th class="text-right py-2">主观</th>
            <th class="text-right py-2">总分</th><th class="text-left py-2">交卷时间</th>
            <th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id" class="border-b border-slate-100"
              :class="active && active.id === r.id ? 'bg-brand-50/40' : ''">
            <td class="py-2">{{ r.studentNo }}</td>
            <td class="py-2">{{ r.studentName }}</td>
            <td class="py-2">
              <span class="tag" :class="r.submitStatus === 'ABSENT' ? 'bg-red-50 text-red-600' : 'bg-slate-100 text-slate-500'">
                {{ statusLabel(r.submitStatus) }}
              </span>
            </td>
            <td class="py-2">
              <span class="tag" :class="r.markedStatus === 'ALL_MARKED' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
                {{ markedLabel(r.markedStatus) }}
              </span>
            </td>
            <td class="py-2 text-right">{{ r.objectiveScore ?? '—' }}</td>
            <td class="py-2 text-right">{{ r.subjectiveScore ?? '—' }}</td>
            <td class="py-2 text-right font-medium">{{ r.totalScore ?? '待批' }}</td>
            <td class="py-2 text-slate-500">{{ fmt(r.submitTime) }}</td>
            <td class="py-2 text-right">
              <button class="text-brand-600 hover:underline" :disabled="opening === r.id" @click="open(r)">
                {{ opening === r.id ? '打开中…' : (r.isAbsent === 1 ? '查看' : '批改') }}
              </button>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="9" class="py-6 text-center text-slate-400">还没有学生交卷</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 批改弹窗（独立弹窗，不再在列表下方展开） -->
    <Teleport to="body">
      <div v-if="active" class="fixed inset-0 z-50 bg-slate-900/45 flex items-center justify-center p-4"
           @click.self="backToList">
        <div class="bg-white rounded-xl shadow-2xl w-full max-w-3xl max-h-[88vh] flex flex-col">
          <!-- 弹窗头 -->
          <div class="flex items-center justify-between gap-3 px-5 py-3 border-b border-slate-200">
            <div class="flex items-center gap-2 flex-wrap min-w-0">
              <h2 class="font-semibold text-slate-800">
                {{ active.studentNo }} · {{ active.studentName }}
              </h2>
              <span v-if="paper" class="tag bg-brand-50 text-brand-600 truncate max-w-64">{{ paper.paperName }}</span>
              <span class="tag" :class="active.markedStatus === 'ALL_MARKED'
                    ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
                {{ markedLabel(active.markedStatus) }}
              </span>
              <span class="text-xs text-slate-400">判卷人：{{ active.markedByName || '系统判分' }}</span>
            </div>
            <div class="flex items-center gap-2 shrink-0">
              <span class="text-xs text-slate-400">
                {{ activeIndex + 1 }} / {{ rows.length }}
              </span>
              <button class="text-slate-400 hover:text-slate-600 text-lg leading-none px-1"
                      title="关闭（Esc）" @click="backToList">✕</button>
            </div>
          </div>

          <!-- 弹窗体 -->
          <div class="flex-1 overflow-y-auto px-5 py-4 space-y-3">
            <div v-if="active.isAbsent === 1"
                 class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">
              该学生缺考，已按 0 分记入。如需补正请联系教务在成绩册中处理（本系统不允许教师代交）。
            </div>

            <div v-for="a in active.answers" :key="a.id" class="border border-slate-200 rounded-lg p-3 space-y-2">
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-xs text-slate-400 w-12">第 {{ a.qNo }} 题</span>
                <span class="tag bg-slate-100 text-slate-500">{{ typeLabel(a.qType) }}</span>
                <span class="text-sm flex-1 min-w-48">{{ a.stem }}</span>
                <span class="text-xs text-slate-400">满分 {{ a.fullScore }}</span>
                <span v-if="a.qType === 'BLANK'" class="text-xs text-slate-400">共 {{ blankCount(a.stdAnswerJson) }} 空</span>
                <span v-if="a.markedByType" class="tag"
                      :class="a.markedByType === 'SYSTEM' ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-600'">
                  {{ a.markedByType === 'SYSTEM' ? '系统判' : '教师判' }}{{ a.markedByName ? '·' + a.markedByName : '' }}
                </span>
              </div>

              <div class="text-sm space-y-1 pl-14">
                <p class="text-slate-700"><span class="text-xs text-slate-400">学生作答：</span>{{ answerText(a.answerJson, a.qType) }}</p>
                <p v-if="a.qType === 'SHORT'" class="text-slate-500"><span class="text-xs text-slate-400">参考要点：</span>{{ answerText(a.stdAnswerJson, 'SHORT') }}</p>
                <p v-else-if="a.qType === 'BLANK'" class="text-slate-500"><span class="text-xs text-slate-400">参考答案：</span>{{ answerText(a.stdAnswerJson, 'BLANK') }}</p>
                <p v-else class="text-slate-500"><span class="text-xs text-slate-400">标准答案：</span>{{ answerText(a.stdAnswerJson, a.qType) }}</p>
              </div>

              <!-- AI 批改建议（M15）：只给建议，点「填入评分框」后仍需提交批改 -->
              <div v-if="isManual(a.qType) && aiSuggest[a.id]"
                   class="ml-14 rounded-lg border border-brand-100 bg-brand-50/60 px-3 py-2 space-y-1">
                <div class="flex flex-wrap items-center gap-2 text-sm">
                  <span class="tag bg-white text-brand-600 border border-brand-200">AI 建议</span>
                  <span class="font-medium text-brand-700">
                    {{ aiSuggest[a.id].suggestedScore }} / {{ a.fullScore }} 分
                  </span>
                  <span v-for="c in aiSuggest[a.id].checks || []" :key="c.code" class="text-[11px]"
                        :class="c.pass ? 'text-slate-400' : 'text-amber-700'" :title="c.detail">
                    {{ c.pass ? '✓' : '✗' }}{{ c.name }}
                  </span>
                  <button class="ml-auto text-xs text-brand-600 hover:underline"
                          @click="adoptAi(a, aiSuggest[a.id])">填入评分框</button>
                </div>
                <p class="text-sm text-slate-700">{{ aiSuggest[a.id].comment }}</p>
                <p v-if="aiSuggest[a.id].modelRubric" class="text-xs text-slate-500">
                  给分依据：{{ aiSuggest[a.id].modelRubric }}
                </p>
                <p v-if="aiSuggest[a.id].status === 'APPLIED'" class="text-[11px] text-emerald-600">
                  已采纳（教师定稿 {{ aiSuggest[a.id].finalScore }} 分）· 提交批改后生效
                </p>
              </div>

              <div class="flex flex-wrap items-center gap-2 pl-14">
                <template v-if="isManual(a.qType)">
                  <span class="text-xs text-slate-500">给分</span>
                  <input v-model.number="drafts[a.id].score" type="number" :max="a.fullScore" min="0" class="input w-24" />
                  <span class="text-xs text-slate-400">/ {{ a.fullScore }}</span>
                  <input v-model="drafts[a.id].comment" class="input flex-1 min-w-48" placeholder="评语（可选）" />
                </template>
                <template v-else>
                  <span class="text-sm" :class="a.isCorrect === 1 ? 'text-emerald-600' : 'text-red-600'">
                    {{ a.isCorrect === 1 ? '正确' : '错误' }} · {{ a.score ?? 0 }} / {{ a.fullScore }} 分
                  </span>
                </template>
              </div>
            </div>

            <!-- 批改完成反馈 -->
            <div v-if="done"
                 class="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 space-y-1">
              <p class="font-medium">
                ✓ {{ done.studentName }} 批改完成 · 总分 {{ done.totalScore }} 分
                <span class="ml-2 text-xs">{{ done.allMarked ? '该卷已全部批完' : '仍有主观题未批' }}</span>
              </p>
              <p class="text-xs text-emerald-600">
                已写入判卷人（{{ active.markedByName || '当前账号' }}）与批改时间，列表状态已同步。
              </p>
            </div>
          </div>

          <!-- 弹窗底 -->
          <div class="border-t border-slate-200 px-5 py-3 space-y-2">
            <input v-model="remark" class="input w-full" placeholder="整卷批注（可选）" />
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div class="flex items-center gap-2">
                <button class="btn-ghost text-sm" :disabled="activeIndex <= 0 || saving" @click="step(-1)">← 上一份</button>
                <button class="btn-ghost text-sm" :disabled="activeIndex >= rows.length - 1 || saving" @click="step(1)">下一份 →</button>
                <button v-if="nextUnmarked" class="text-xs text-brand-600 hover:underline" @click="openNext">
                  跳到未批（{{ nextUnmarked.studentName }}）
                </button>
              </div>
              <div class="flex items-center gap-2">
                <button class="btn-ghost text-sm" :disabled="aiBusy || active.isAbsent === 1" @click="suggestAll">
                  {{ aiBusy ? 'AI 批改中…' : (aiHasSuggestions ? '重新生成 AI 建议' : 'AI 批改建议') }}
                </button>
                <button class="btn-ghost text-sm" @click="backToList">关闭</button>
                <button class="btn-primary text-sm" :disabled="active.isAbsent === 1 || saving" @click="submitGrade">
                  {{ saving ? '提交中…' : '提交批改' }}
                </button>
              </div>
            </div>
            <p class="text-xs text-slate-400">
              只提交填空 / 简答的分数；单选、多选、判断由系统判分，不可改。
              AI 建议只供参考：点「填入评分框」后仍要点「提交批改」才写入分数（判卷人记你）。
            </p>
            <p v-if="aiInfo" class="text-xs text-slate-400">{{ aiInfo }}</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { paperApi, questionTypes } from '../../api/paper'
import { gradingAssistApi } from '../../api/ai'
import { notify } from '../../toast'

const route = useRoute()
const router = useRouter()
const paperId = Number(route.query.paperId || 0)

const paper = ref(null)
const rows = ref([])
const active = ref(null)
const drafts = reactive({})
const remark = ref('')
const message = ref('')
const ok = ref(true)
const saving = ref(false)
const opening = ref(0)
const done = ref(null)

/** 返回列表：回到教学详情对应的页签（测验 / 作业 / 项目），而不是笼统的上一页 */
const teachingClassId = computed(() => (paper.value && paper.value.teachingClassId)
  || Number(route.query.teachingClassId || 0))
const offeringId = computed(() => Number(route.query.offeringId || 0))
const backTab = computed(() => {
  const t = paper.value && paper.value.paperType
  if (t === 'HOMEWORK') return 'homework'
  if (t === 'PROJECT') return 'project'
  return 'quiz'
})
const backLabel = computed(() => ({ quiz: '测验列表', homework: '作业列表', project: '项目' }[backTab.value] || '列表'))

function goBack() {
  if (!paper.value || !teachingClassId.value) { router.back(); return }
  router.push({
    path: '/teacher/teaching',
    query: {
      teachingClassId: teachingClassId.value,
      offeringId: offeringId.value || undefined,
      tab: backTab.value
    }
  })
}

const nextUnmarked = computed(() => rows.value.find((r) => r.markedStatus !== 'ALL_MARKED' && r.isAbsent !== 1 && (!active.value || r.id !== active.value.id)) || null)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  notify(msg, isOk)
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function typeLabel(t) { return (questionTypes.find((x) => x.value === t) || {}).label || t }
function statusLabel(s) { return { SUBMITTED: '已交', MARKED: '已批阅', ABSENT: '缺考' }[s] || s }
function markedLabel(s) { return { NOT_MARKED: '未判', SYSTEM_MARKED: '客观已判', ALL_MARKED: '批完' }[s] || s }
/** 需要人工批阅的题型：填空与简答（与后端 GradeEngine.isManual 同一口径） */
function isManual(t) { return t === 'SHORT' || t === 'BLANK' }

/** 填空题空数（按标准答案条数） */
function blankCount(json) {
  try {
    const v = JSON.parse(json || '{}')
    return (v.answers || []).length
  } catch (e) { return 0 }
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }

/** 把 answerJson 转成人能读的文本 */
function answerText(json, type) {
  if (!json) return '（未作答）'
  let v
  try { v = JSON.parse(json) } catch (e) { return json }
  if (type === 'SINGLE' || type === 'MULTI') return (v.keys || []).join('、') || '（未选）'
  if (type === 'JUDGE') return v.value === true ? '正确' : (v.value === false ? '错误' : '（未选）')
  if (type === 'BLANK') return (v.answers || []).join(' ｜ ') || '（未填）'
  return v.text || v.ref || '（未答）'
}

async function load() {
  rows.value = await paperApi.submissions(paperId)
  if (!paper.value) {
    paper.value = await paperApi.get(paperId).catch(() => null)
  }
}

async function open(r) {
  done.value = null
  opening.value = r.id
  try {
    const full = await paperApi.submission(r.id)
    active.value = full
    remark.value = full.remark || ''
    ;(full.answers || []).forEach((a) => {
      drafts[a.id] = { score: a.score == null ? 0 : Number(a.score), comment: a.comment || '' }
    })
    clearAiSuggestions()
    loadAiSuggestions(r.id)
  } catch (e) {
    toast('打开作答失败：' + e.message, false)
  } finally {
    opening.value = 0
  }
}

// ---------------- AI 批改建议（M15）：只建议，采纳后仍要点「提交批改」
const aiSuggest = reactive({})
const aiBusy = ref(false)
const aiInfo = ref('')
const aiHasSuggestions = computed(() => Object.keys(aiSuggest).length > 0)

function clearAiSuggestions() {
  Object.keys(aiSuggest).forEach((k) => delete aiSuggest[k])
  aiInfo.value = ''
}

async function loadAiSuggestions(submissionId) {
  try {
    const list = await gradingAssistApi.list(submissionId)
    ;(list || []).forEach((s) => { aiSuggest[s.answerId] = s })
    if ((list || []).length) {
      aiInfo.value = `已有 ${list.length} 条 AI 建议（点「重新生成」可再算一次）`
    }
  } catch (e) {
    // 没有建议是正常情况，静默即可
  }
}

async function suggestAll() {
  if (!active.value || aiBusy.value) return
  aiBusy.value = true
  try {
    const r = await gradingAssistApi.suggest(active.value.id)
    ;(r.suggestions || []).forEach((s) => { aiSuggest[s.answerId] = s })
    const skipped = (r.skipped || []).length
    aiInfo.value = `${r.message || ''} · ${r.tokens || 0} tokens · 约 ${Number(r.cost || 0).toFixed(4)} 元`
      + (skipped ? ` · 跳过 ${skipped} 题（模型没返回有效分数，请人工评分）` : '')
    notify(r.message || 'AI 建议已生成', true)
  } catch (e) {
    toast(e.message || '生成 AI 建议失败', false)
  } finally {
    aiBusy.value = false
  }
}

async function adoptAi(a, s) {
  drafts[a.id] = { score: Number(s.suggestedScore), comment: s.comment || '' }
  try {
    const r = await gradingAssistApi.apply(s.id, { score: drafts[a.id].score, comment: drafts[a.id].comment })
    aiSuggest[a.id] = { ...s, status: 'APPLIED', finalScore: r.finalScore, finalComment: r.finalComment }
  } catch (e) {
    // 留痕失败不影响填分
  }
  toast('已填入评分框，记得点「提交批改」')
}

/** 当前批改学生在列表中的位置（弹窗里显示 第几份 + 上一份/下一份） */
const activeIndex = computed(() => {
  if (!active.value) return -1
  return rows.value.findIndex((r) => r.id === active.value.id)
})

async function step(delta) {
  const i = activeIndex.value + delta
  if (i >= 0 && i < rows.value.length) {
    await open(rows.value[i])
  }
}

async function openNext() {
  const n = nextUnmarked.value
  if (n) await open(n)
}

function backToList() {
  active.value = null
  done.value = null
}

async function submitGrade() {
  const items = (active.value.answers || [])
    .filter((a) => isManual(a.qType))
    .map((a) => ({ answerId: a.id, score: drafts[a.id].score, comment: drafts[a.id].comment }))
  saving.value = true
  try {
    const res = await paperApi.grade(active.value.id, { items, remark: remark.value })
    active.value = res
    done.value = {
      studentName: res.studentName,
      totalScore: res.totalScore,
      allMarked: res.markedStatus === 'ALL_MARKED'
    }
    toast('批改已保存：' + res.studentName + ' 总分 ' + res.totalScore + ' 分')
    await load()
  } catch (e) { toast(e.message, false) } finally { saving.value = false }
}

/** Esc 关闭弹窗；弹窗打开时锁住页面滚动 */
function onKeydown(e) {
  if (e.key === 'Escape' && active.value && !saving.value) backToList()
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  try { await load() } catch (e) { toast(e.message, false) }
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

// 弹窗打开时禁止背景滚动
watchEffect(() => {
  document.body.style.overflow = active.value ? 'hidden' : ''
})
</script>
