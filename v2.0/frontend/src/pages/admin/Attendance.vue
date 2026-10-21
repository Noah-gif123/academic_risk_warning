<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <h1 class="text-lg font-semibold text-slate-800">考勤归档</h1>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/admin/coverage')">覆盖率督办 →</button>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      教师提交的期中 / 期末归档在这里审核：确认后该阶段考勤变为归档口径并冻结（教师不能再改）；
      退回后教师可重新提交。点名覆盖率低于 1/3 的教学班列为督办对象。
    </p>

    <div class="card p-4">
      <div class="flex items-center justify-between flex-wrap gap-2 mb-3">
        <h2 class="font-medium text-slate-700">归档批次（{{ archives.length }}）</h2>
        <div class="flex items-center gap-2">
          <label class="text-sm">学期
            <select v-model="termId" class="input mt-1" @change="loadArchives">
              <option :value="''">全部学期</option>
              <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
            </select>
          </label>
          <label class="text-sm">状态
            <select v-model="status" class="input mt-1" @change="loadArchives">
              <option value="">全部</option>
              <option value="SUBMITTED">待审核</option>
              <option value="REVIEWED">已确认</option>
              <option value="RETURNED">已退回</option>
            </select>
          </label>
        </div>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">教学班</th>
            <th class="text-left py-2">教师</th><th class="text-left py-2">阶段</th>
            <th class="text-left py-2">状态</th><th class="text-left py-2">覆盖课次</th>
            <th class="text-right py-2">记录数</th><th class="text-right py-2">覆盖率</th>
            <th class="text-left py-2">提交时间</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in archives" :key="a.id" class="border-b border-slate-100">
            <td class="py-2">{{ a.courseName }}</td>
            <td class="py-2 text-slate-500">{{ a.className }}</td>
            <td class="py-2 text-slate-500">{{ a.teacherName }}</td>
            <td class="py-2">{{ a.phase === 'MID' ? '期中' : '期末' }}</td>
            <td class="py-2">
              <span class="tag" :class="statusClass(a.status)">{{ statusLabel(a.status) }}</span>
            </td>
            <td class="py-2 text-slate-500">第 {{ a.sessionFrom }} ~ {{ a.sessionTo }} 次</td>
            <td class="py-2 text-right">{{ a.recordCount }}</td>
            <td class="py-2 text-right" :class="Number(a.coverageRate) < 33.33 ? 'text-red-600' : 'text-slate-500'">
              {{ a.coverageRate }}%
            </td>
            <td class="py-2 text-slate-500">{{ fmt(a.submitTime) }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <template v-if="a.status === 'SUBMITTED'">
                <button class="text-emerald-600 hover:underline mr-2" @click="confirmArchive(a)">通过</button>
                <button class="text-red-600 hover:underline mr-2" @click="returnArchive(a)">退回</button>
                <button class="text-slate-500 hover:underline" @click="remind(a)">催办（{{ a.remindCount }}）</button>
              </template>
              <span v-else class="text-xs text-slate-400">{{ a.reviewNote || a.returnReason || '—' }}</span>
            </td>
          </tr>
          <tr v-if="!archives.length"><td colspan="10" class="py-6 text-center text-slate-400">没有归档批次</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">点名覆盖率督办（{{ coverage.length }} 个教学班）</h2>
        <span class="text-xs text-slate-400">覆盖率 = 已点名课次 / 已过日期课次；低于 1/3 判低置信度</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">教学班</th>
            <th class="text-left py-2">教师</th><th class="text-right py-2">有效课次</th>
            <th class="text-right py-2">已过课次</th><th class="text-right py-2">已点名</th>
            <th class="text-right py-2">覆盖率</th><th class="text-left py-2">督办</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in coverage" :key="c.teachingClassId" class="border-b border-slate-100">
            <td class="py-2">{{ c.courseName }}</td>
            <td class="py-2 text-slate-500">{{ c.className }}</td>
            <td class="py-2 text-slate-500">{{ c.teacherName }}</td>
            <td class="py-2 text-right">{{ c.effectiveSessions ?? '—' }}</td>
            <td class="py-2 text-right">{{ c.passedSessions }}</td>
            <td class="py-2 text-right">{{ c.calledSessions }}</td>
            <td class="py-2 text-right" :class="c.lowCoverage ? 'text-red-600 font-medium' : 'text-slate-500'">
              {{ c.coverageRate == null ? '—' : c.coverageRate + '%' }}
            </td>
            <td class="py-2">
              <span v-if="c.lowCoverage" class="tag bg-red-50 text-red-600">覆盖率不足，需督办</span>
              <span v-else class="text-xs text-slate-400">正常</span>
              <span v-if="c.archivePending" class="tag bg-amber-50 text-amber-600 ml-1">待归档 {{ c.archivePending }}</span>
            </td>
          </tr>
          <tr v-if="!coverage.length"><td colspan="8" class="py-6 text-center text-slate-400">暂无教学班</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminAttendanceApi } from '../../api/attendance'
import { adminApi } from '../../api/admin'

const terms = ref([])
const archives = ref([])
const coverage = ref([])
const termId = ref('')
const status = ref('SUBMITTED')
const message = ref('')
const ok = ref(true)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function statusLabel(s) {
  return { PENDING: '待提交', SUBMITTED: '待审核', REVIEWED: '已确认', RETURNED: '已退回' }[s] || s
}
function statusClass(s) {
  return {
    PENDING: 'bg-slate-100 text-slate-500',
    SUBMITTED: 'bg-amber-50 text-amber-600',
    REVIEWED: 'bg-emerald-50 text-emerald-600',
    RETURNED: 'bg-red-50 text-red-600'
  }[s]
}

async function loadArchives() {
  archives.value = await adminAttendanceApi.archives(termId.value || undefined, status.value || undefined)
}
async function loadCoverage() {
  coverage.value = await adminAttendanceApi.coverage(termId.value || undefined)
}

async function confirmArchive(a) {
  if (!window.confirm('确认后该阶段考勤将冻结（教师不能再改），确认吗？')) return
  try {
    await adminAttendanceApi.review(a.id, true, { note: '教务核对无误，予以确认' })
    toast('已确认归档')
    await loadArchives()
    await loadCoverage()
  } catch (e) { toast(e.message, false) }
}
async function returnArchive(a) {
  const reason = window.prompt('退回原因（教师会看到）：', '第 ' + (a.sessionFrom || '') + ' 次课点名有遗漏，请补点后重新提交')
  if (reason === null) return
  try {
    await adminAttendanceApi.review(a.id, false, { reason })
    toast('已退回，等待教师重新提交')
    await loadArchives()
  } catch (e) { toast(e.message, false) }
}
async function remind(a) {
  try {
    const r = await adminAttendanceApi.remind(a.id)
    toast('已催办（第 ' + r.remindCount + ' 次）')
    await loadArchives()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    terms.value = await adminApi.listTerms()
    const cur = terms.value.find((t) => t.current)
    if (cur) termId.value = cur.id
    await loadArchives()
    await loadCoverage()
  } catch (e) { toast(e.message, false) }
})
</script>
