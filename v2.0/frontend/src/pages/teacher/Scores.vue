<template>
  <div class="space-y-4">
    <div v-if="!embedded" class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/home')">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">成绩与批改</h1>
        <span v-if="matrix" class="tag bg-brand-50 text-brand-600">{{ matrix.courseName }}</span>
        <span v-if="matrix" class="tag bg-slate-100 text-slate-500">{{ matrix.className }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      加权分 W = Σ(得分/满分×100×权重)，考勤扣分 P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)，总评 T = W − P；
      缺课超过有效课次的 1/3 直接硬门槛挂科；缺考/缺交请标记为缺考（按 0 分计），未录入的分项必须补齐才能汇总。
    </p>

    <div class="flex flex-wrap gap-2 items-center">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key">{{ t.label }}</button>
      <div class="ml-auto flex items-center gap-2">
        <span v-if="matrix && matrix.summary.schemeVersion" class="text-xs text-slate-400">
          构成 {{ matrix.summary.schemeVersion }}（{{ matrix.summary.schemeLocked ? '已锁定' : '未锁定' }}）
        </span>
        <button class="btn-ghost text-sm" @click="exportCsv">导出成绩矩阵</button>
        <button class="btn-primary text-sm" @click="commit">汇总生成总评</button>
      </div>
    </div>

    <!-- 待批改 -->
    <div class="card p-4 space-y-3" v-if="tab === 'pending'">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">待批改（{{ pending.paperPending + pending.projectPending }}）</h2>
        <span class="text-xs text-slate-400">客观题已自动判分，这里只列需要人工处理的</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">类型</th><th class="text-left py-2">名称</th>
            <th class="text-right py-2">待处理</th><th class="text-right py-2">已交/总数</th>
            <th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in pending.papers || []" :key="'p' + p.paperId" class="border-b border-slate-100">
            <td class="py-2">测验 / 作业</td>
            <td class="py-2">{{ p.paperName }}</td>
            <td class="py-2 text-right text-amber-600">{{ p.pendingCount }} 份需判主观题</td>
            <td class="py-2 text-right text-slate-500">{{ p.submittedCount }}</td>
            <td class="py-2 text-right">
              <button class="text-brand-600 hover:underline"
                      @click="$router.push({ path: '/teacher/grading', query: { paperId: p.paperId } })">去批改</button>
            </td>
          </tr>
          <tr v-for="p in pending.projects || []" :key="'j' + p.projectId" class="border-b border-slate-100">
            <td class="py-2">项目</td>
            <td class="py-2">{{ p.projectName }}</td>
            <td class="py-2 text-right text-amber-600">{{ p.submittedCount }} 组/人待评分</td>
            <td class="py-2 text-right text-slate-500">—</td>
            <td class="py-2 text-right">
              <button class="text-brand-600 hover:underline"
                      @click="$router.push({ path: '/teacher/projects', query: { teachingClassId } })">去评分</button>
            </td>
          </tr>
          <tr v-if="!((pending.papers || []).length || (pending.projects || []).length)">
            <td colspan="5" class="py-6 text-center text-slate-400">没有待批改的作业 / 测验 / 项目</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 成绩矩阵 -->
    <div class="card p-4 space-y-3" v-if="tab === 'matrix' && matrix">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">成绩矩阵（{{ matrix.rows.length }} 人 × {{ matrix.items.length }} 项）</h2>
        <span class="text-xs text-slate-400">灰色单元格为自动汇总（只读），可直接在输入框里改手工项</span>
      </div>
      <div v-if="matrix.summary.missingItemNames.length" class="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded-lg p-2">
        还有评分项没有录入：{{ matrix.summary.missingItemNames.join('、') }}（汇总前必须补齐）
      </div>

      <div class="overflow-auto max-h-[32rem] border border-slate-200 rounded-lg">
        <table class="text-sm">
          <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
            <tr>
              <th class="text-left py-2 px-2">学号</th>
              <th class="text-left py-2 px-2">姓名</th>
              <th v-for="c in matrix.items" :key="c.schemeItemId" class="text-right py-2 px-2 whitespace-nowrap">
                {{ c.path }}
                <span class="block text-xs font-normal" :class="c.dataSource === 'AUTO' ? 'text-slate-400' : 'text-brand-500'">
                  {{ c.dataSource === 'AUTO' ? '自动' : '手工' }} · {{ c.weight }}%
                </span>
              </th>
              <th class="text-right py-2 px-2">加权分</th>
              <th class="text-right py-2 px-2">扣分</th>
              <th class="text-right py-2 px-2">总评</th>
              <th class="text-left py-2 px-2">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in matrix.rows" :key="r.enrollmentId" class="border-b border-slate-100">
              <td class="py-1 px-2 text-slate-500">{{ r.studentNo }}</td>
              <td class="py-1 px-2">{{ r.studentName }}</td>
              <td v-for="c in matrix.items" :key="c.schemeItemId" class="py-1 px-2 text-right">
                <input v-if="c.dataSource !== 'AUTO'"
                       v-model.number="drafts[r.studentId][c.schemeItemId]"
                       type="number" min="0" :max="c.fullScore"
                       class="input w-20 text-right"
                       @change="markDirty(c.schemeItemId)" />
                <span v-else :class="r.cells[c.schemeItemId] && r.cells[c.schemeItemId].missing ? 'text-red-500' : 'text-slate-500'">
                  {{ cellText(r, c) }}
                </span>
              </td>
              <td class="py-1 px-2 text-right">{{ r.weightedScore }}</td>
              <td class="py-1 px-2 text-right" :class="Number(r.deductionPoints) > 0 ? 'text-red-600' : 'text-slate-500'">
                {{ r.deductionPoints }}
              </td>
              <td class="py-1 px-2 text-right font-medium"
                  :class="r.fail ? 'text-red-600' : 'text-slate-800'">{{ r.totalScore }}</td>
              <td class="py-1 px-2">
                <span class="tag" :class="statusClass(r.scoreStatus)">{{ statusLabel(r.scoreStatus) }}</span>
                <span v-if="r.failReason" class="text-xs text-red-600 ml-1">{{ reasonLabel(r.failReason) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <template v-for="c in dirtyItems" :key="c">
          <button class="btn-primary text-sm" @click="saveItem(c)">
            保存「{{ itemName(c) }}」{{ manualCount(c) }} 行
          </button>
        </template>
        <span v-if="!dirtyItems.length" class="text-xs text-slate-400">改动手工项后，这里会出现保存按钮</span>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-6 gap-3 text-sm border-t border-slate-100 pt-3">
        <div><p class="text-xs text-slate-400">已出分</p><p class="font-medium">{{ matrix.summary.scoredCount }} / {{ matrix.summary.studentCount }}</p></div>
        <div><p class="text-xs text-slate-400">挂科</p><p class="font-medium text-red-600">{{ matrix.summary.failCount }}</p></div>
        <div><p class="text-xs text-slate-400">硬门槛</p><p class="font-medium text-red-600">{{ matrix.summary.hardFailCount }}</p></div>
        <div><p class="text-xs text-slate-400">通过率</p><p class="font-medium">{{ matrix.summary.passRate ?? '—' }}%</p></div>
        <div><p class="text-xs text-slate-400">平均分</p><p class="font-medium">{{ matrix.summary.avgTotal ?? '—' }}</p></div>
        <div><p class="text-xs text-slate-400">及格线</p><p class="font-medium">{{ matrix.summary.passScore }}</p></div>
      </div>
    </div>

    <!-- 总评汇总 -->
    <div class="card p-4 space-y-3" v-if="tab === 'summary' && matrix">
      <h2 class="font-medium text-slate-700">总评汇总</h2>
      <p class="text-xs text-slate-400">
        汇总会把每位学生的加权分、扣分、总评与判定结果写入成绩单（score_result，状态 FINAL），作为后续预警与报表的依据。
      </p>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
        <div><p class="text-xs text-slate-400">学生数</p><p class="font-medium">{{ matrix.summary.studentCount }}</p></div>
        <div><p class="text-xs text-slate-400">已定稿</p><p class="font-medium">{{ committedCount }}</p></div>
        <div><p class="text-xs text-slate-400">挂科 / 硬门槛</p><p class="font-medium text-red-600">{{ matrix.summary.failCount }} / {{ matrix.summary.hardFailCount }}</p></div>
        <div><p class="text-xs text-slate-400">通过率</p><p class="font-medium">{{ matrix.summary.passRate ?? '—' }}%</p></div>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-primary text-sm" @click="commit">汇总生成总评</button>
        <button class="btn-ghost text-sm" @click="exportCsv">导出 CSV</button>
        <span class="text-xs text-slate-400">导出文件含每个分项（百分制）与判定结果，Excel 可直接打开</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
            <th class="text-right py-2">加权分</th><th class="text-right py-2">扣分</th>
            <th class="text-right py-2">总评</th><th class="text-right py-2">缺课/迟到/请假</th>
            <th class="text-left py-2">状态</th><th class="text-left py-2">原因</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in failRows" :key="r.enrollmentId" class="border-b border-slate-100">
            <td class="py-1.5">{{ r.studentNo }}</td>
            <td class="py-1.5">{{ r.studentName }}</td>
            <td class="py-1.5 text-right">{{ r.weightedScore }}</td>
            <td class="py-1.5 text-right">{{ r.deductionPoints }}</td>
            <td class="py-1.5 text-right font-medium text-red-600">{{ r.totalScore }}</td>
            <td class="py-1.5 text-right text-slate-500">{{ r.absentCount }} / {{ r.lateCount }} / {{ r.leaveCount }}</td>
            <td class="py-1.5" :class="statusClass(r.scoreStatus)">{{ statusLabel(r.scoreStatus) }}</td>
            <td class="py-1.5 text-xs text-red-600">{{ reasonLabel(r.failReason) }}</td>
          </tr>
          <tr v-if="!failRows.length"><td colspan="8" class="py-6 text-center text-slate-400">没有挂科学生</td></tr>
        </tbody>
      </table>
      <p class="text-xs text-slate-400">下表只列挂科学生；完整名单请导出 CSV。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { scoreMatrixApi } from '../../api/scoreMatrix'

const route = useRoute()
const props = defineProps({
  teachingClassId: { type: Number, default: 0 },
  embedded: { type: Boolean, default: false },
  initialTab: { type: String, default: 'matrix' }
})
const teachingClassId = props.teachingClassId || Number(route.query.teachingClassId || 1)

const tabs = [
  { key: 'pending', label: '待批改' },
  { key: 'matrix', label: '成绩矩阵' },
  { key: 'summary', label: '总评汇总' }
]
const tab = ref(props.initialTab)
const matrix = ref(null)
const pending = ref({ papers: [], projects: [], paperPending: 0, projectPending: 0 })
const message = ref('')
const ok = ref(true)
const drafts = reactive({})
const dirty = ref([])

const dirtyItems = computed(() => dirty.value)
const failRows = computed(() => (matrix.value ? matrix.value.rows.filter((r) => r.fail) : []))
const committedCount = computed(() => (matrix.value ? matrix.value.rows.filter((r) => r.scoreStatus === 'FINAL').length : 0))

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function statusLabel(s) {
  return { PENDING: '待定稿', FINAL: '已定稿', NO_SCORE: '成绩未齐' }[s] || s
}
function statusClass(s) {
  return { PENDING: 'bg-amber-50 text-amber-600', FINAL: 'bg-emerald-50 text-emerald-600', NO_SCORE: 'bg-red-50 text-red-600' }[s]
}
function reasonLabel(r) {
  return { HARD_LINE: '缺课超 1/3', SCORE: '分数不足', NO_SCORE: '成绩未齐' }[r] || ''
}
function cellText(r, c) {
  const cell = r.cells[c.schemeItemId]
  if (!cell || cell.missing || cell.score == null) return '缺'
  return Number(cell.score).toFixed(1)
}
function itemName(id) {
  const c = matrix.value.items.find((x) => x.schemeItemId === Number(id))
  return c ? c.itemName : id
}
function manualCount(id) {
  return Object.keys(drafts).filter((sid) => drafts[sid][id] !== '').length
}

function fillDrafts() {
  if (!matrix.value) return
  matrix.value.rows.forEach((r) => {
    if (!drafts[r.studentId]) drafts[r.studentId] = {}
    matrix.value.items.forEach((c) => {
      if (c.dataSource === 'AUTO') return
      const cell = r.cells[c.schemeItemId]
      drafts[r.studentId][c.schemeItemId] = cell && cell.score != null && !cell.missing
        ? Number(Number(cell.score).toFixed(1)) : ''
    })
  })
}
function markDirty(itemId) {
  if (!dirty.value.includes(itemId)) dirty.value.push(itemId)
}

async function load() {
  matrix.value = await scoreMatrixApi.matrix(teachingClassId)
  pending.value = await scoreMatrixApi.pending(teachingClassId)
  fillDrafts()
}

async function saveItem(itemId) {
  const items = []
  matrix.value.rows.forEach((r) => {
    const v = drafts[r.studentId][itemId]
    if (v === '' || v === null || v === undefined) return
    items.push({ studentId: r.studentId, score: Number(v) })
  })
  if (!items.length) {
    toast('该评分项还没有填分数', false)
    return
  }
  try {
    matrix.value = await scoreMatrixApi.saveEntries({
      teachingClassId, schemeItemId: Number(itemId), timesNo: 1, items
    })
    dirty.value = dirty.value.filter((x) => x !== itemId)
    fillDrafts()
    toast('已保存 ' + items.length + ' 行')
  } catch (e) { toast(e.message, false) }
}

async function commit() {
  if (!matrix.value) return
  if (matrix.value.summary.missingItemNames.length) {
    toast('还有评分项没录入：' + matrix.value.summary.missingItemNames.join('、'), false)
    return
  }
  if (!window.confirm('汇总后会把总评写入成绩单（可重复汇总覆盖），确认吗？')) return
  try {
    matrix.value = await scoreMatrixApi.commit(teachingClassId)
    fillDrafts()
    toast('总评已生成：' + matrix.value.summary.scoredCount + ' 人，挂科 ' + matrix.value.summary.failCount + ' 人')
    tab.value = 'summary'
  } catch (e) { toast(e.message, false) }
}

async function exportCsv() {
  try { await scoreMatrixApi.exportCsv(teachingClassId) }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
