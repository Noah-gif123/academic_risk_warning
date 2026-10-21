<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.back()">← 返回</button>
        <h1 class="text-lg font-semibold text-slate-800">交卷与批改</h1>
        <span v-if="paper" class="tag bg-brand-50 text-brand-600">{{ paper.paperName }}</span>
        <span v-if="paper" class="tag bg-slate-100 text-slate-500">满分 {{ paper.fullScore }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      客观题已自动判分；简答题需要你逐题给分，系统记录判卷人（每题一行）。缺考学生记 0 分，无需批改。
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
              <button class="text-brand-600 hover:underline" @click="open(r)">{{ r.isAbsent === 1 ? '查看' : '批改' }}</button>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="9" class="py-6 text-center text-slate-400">还没有学生交卷</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 批改面板 -->
    <div class="card p-4 space-y-3" v-if="active">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">
          {{ active.studentNo }} · {{ active.studentName }} 的作答
          <span class="text-xs text-slate-400 ml-2">判卷人：{{ active.markedByName || '系统判分' }}</span>
        </h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="active = null">收起</button>
      </div>

      <div v-if="active.isAbsent === 1" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">
        该学生缺考，已按 0 分记入。如需补正请联系教务在成绩册中处理（本系统不允许教师代交）。
      </div>

      <div v-for="a in active.answers" :key="a.id" class="border border-slate-200 rounded-lg p-3 space-y-2">
        <div class="flex flex-wrap items-center gap-2">
          <span class="text-xs text-slate-400 w-12">第 {{ a.qNo }} 题</span>
          <span class="tag bg-slate-100 text-slate-500">{{ typeLabel(a.qType) }}</span>
          <span class="text-sm flex-1 min-w-48">{{ a.stem }}</span>
          <span class="text-xs text-slate-400">满分 {{ a.fullScore }}</span>
          <span v-if="a.markedByType" class="tag"
                :class="a.markedByType === 'SYSTEM' ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-600'">
            {{ a.markedByType === 'SYSTEM' ? '系统判' : '教师判' }}{{ a.markedByName ? '·' + a.markedByName : '' }}
          </span>
        </div>

        <div class="text-sm space-y-1 pl-14">
          <p><span class="text-xs text-slate-400">学生作答：</span>{{ answerText(a.answerJson, a.qType) }}</p>
          <p v-if="a.qType !== 'SHORT'"><span class="text-xs text-slate-400">标准答案：</span>{{ answerText(a.stdAnswerJson, a.qType) }}</p>
          <p v-else><span class="text-xs text-slate-400">参考要点：</span>{{ answerText(a.stdAnswerJson, 'SHORT') }}</p>
        </div>

        <div class="flex flex-wrap items-center gap-2 pl-14">
          <template v-if="a.qType === 'SHORT'">
            <span class="text-xs text-slate-500">给分</span>
            <input v-model.number="drafts[a.id].score" type="number" :max="a.fullScore" min="0" class="input w-24" />
            <input v-model="drafts[a.id].comment" class="input flex-1 min-w-48" placeholder="评语（可选）" />
          </template>
          <template v-else>
            <span class="text-sm" :class="a.isCorrect === 1 ? 'text-emerald-600' : 'text-red-600'">
              {{ a.isCorrect === 1 ? '正确' : '错误' }} · {{ a.score }} 分
            </span>
          </template>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100">
        <input v-model="remark" class="input flex-1 min-w-64" placeholder="整卷批注（可选）" />
        <button class="btn-primary text-sm" :disabled="active.isAbsent === 1" @click="submitGrade">提交批改</button>
        <span class="text-xs text-slate-400">只提交简答题的分数；客观题分数不可改。</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { paperApi, questionTypes } from '../../api/paper'

const route = useRoute()
const paperId = Number(route.query.paperId || 0)

const paper = ref(null)
const rows = ref([])
const active = ref(null)
const drafts = reactive({})
const remark = ref('')
const message = ref('')
const ok = ref(true)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function typeLabel(t) { return (questionTypes.find((x) => x.value === t) || {}).label || t }
function statusLabel(s) { return { SUBMITTED: '已交', MARKED: '已批阅', ABSENT: '缺考' }[s] || s }
function markedLabel(s) { return { NOT_MARKED: '未判', SYSTEM_MARKED: '客观已判', ALL_MARKED: '批完' }[s] || s }
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
  const list = await paperApi.list(route.query.teachingClassId ? Number(route.query.teachingClassId) : 1).catch(() => [])
  paper.value = list.find((p) => p.id === paperId) || null
}

async function open(r) {
  const full = await paperApi.submission(r.id)
  active.value = full
  remark.value = full.remark || ''
  ;(full.answers || []).forEach((a) => {
    drafts[a.id] = { score: a.score == null ? 0 : Number(a.score), comment: a.comment || '' }
  })
}

async function submitGrade() {
  const items = (active.value.answers || [])
    .filter((a) => a.qType === 'SHORT')
    .map((a) => ({ answerId: a.id, score: drafts[a.id].score, comment: drafts[a.id].comment }))
  try {
    const res = await paperApi.grade(active.value.id, { items, remark: remark.value })
    toast('批改已保存，总分 ' + res.totalScore)
    active.value = res
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
