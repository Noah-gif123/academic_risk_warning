<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/home')">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">考勤与归档</h1>
        <span class="tag bg-slate-100 text-slate-500">教学班 #{{ teachingClassId }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      一节课一张点名单：默认全班出勤，只勾异常（迟到 / 缺课 / 请假）。每行改动都会留痕；
      期中 / 期末归档提交后由教务确认，确认后考勤冻结（教师不能再改）。
    </p>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key">{{ t.label }}</button>
      <div class="ml-auto flex items-center gap-2">
        <input ref="importInput" type="file" class="hidden" accept=".xlsx,.csv" @change="onImport" />
        <button class="btn-ghost text-sm" @click="downloadTemplate">下载导入模板</button>
        <button class="btn-ghost text-sm" @click="$refs.importInput.click()">批量导入</button>
      </div>
    </div>

    <!-- 导入结果 -->
    <div v-if="importResult" class="card p-4 text-sm">
      <div class="flex items-center justify-between">
        <p>导入 {{ importResult.fileName }}：共 {{ importResult.totalRows }} 行，成功 {{ importResult.successRows }}，跳过 {{ importResult.skippedRows }}</p>
        <button class="text-xs text-slate-400 hover:text-slate-600" @click="importResult = null">关闭</button>
      </div>
      <p v-for="(e, i) in importResult.errors" :key="i" class="text-xs text-amber-600 mt-1">· {{ e }}</p>
      <p class="text-xs text-slate-400 mt-1">格式：学号,课次序号,状态(出勤/迟到/缺课/请假),请假类型(事假/病假/公假，可空)</p>
    </div>

    <!-- 点名单 -->
    <div class="card p-4" v-if="tab === 'call'">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">课次（{{ sessions.length }}）</h2>
        <span class="text-xs text-slate-400">点击某一课次开始点名</span>
      </div>
      <div class="max-h-72 overflow-y-auto">
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
            <tr>
              <th class="text-left py-2">周次</th><th class="text-left py-2">次序</th>
              <th class="text-left py-2">日期</th><th class="text-left py-2">点名</th>
              <th class="text-right py-2">出勤</th><th class="text-right py-2">迟到</th>
              <th class="text-right py-2">缺课</th><th class="text-right py-2">请假</th>
              <th class="text-right py-2">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in sessions" :key="s.sessionId" class="border-b border-slate-100"
                :class="active && active.sessionId === s.sessionId ? 'bg-brand-50/40' : ''">
              <td class="py-1.5">第 {{ s.weekNo }} 周</td>
              <td class="py-1.5">第 {{ s.sessionNo }} 次</td>
              <td class="py-1.5 text-slate-500">{{ s.sessionDate }}</td>
              <td class="py-1.5">
                <span class="tag" :class="s.archived ? 'bg-slate-200 text-slate-600' : (s.rollCallStatus === 'CALLED' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600')">
                  {{ s.archived ? '已归档' : (s.rollCallStatus === 'CALLED' ? '已点名' : '未点名') }}
                </span>
              </td>
              <td class="py-1.5 text-right">{{ s.presentCount || 0 }}</td>
              <td class="py-1.5 text-right">{{ s.lateCount || 0 }}</td>
              <td class="py-1.5 text-right">{{ s.absentCount || 0 }}</td>
              <td class="py-1.5 text-right">{{ s.leaveCount || 0 }}</td>
              <td class="py-1.5 text-right">
                <button class="text-brand-600 hover:underline" @click="openRoster(s)">
                  {{ s.rollCallStatus === 'CALLED' ? '修改' : '点名' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 点名单明细 -->
    <div class="card p-4 space-y-3" v-if="tab === 'call' && active">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">
          第 {{ active.weekNo }} 周 · 第 {{ active.sessionNo }} 次（{{ active.sessionDate }}）
          <span class="text-xs ml-2" :class="rosterLocked ? 'text-slate-500' : 'text-emerald-600'">
            {{ active.archived ? '已归档，只读' : (rosterLocked ? '已提交并锁定，如需更正请联系教务' : '默认全班出勤，只勾异常') }}
          </span>
        </h2>
        <div class="flex items-center gap-2">
          <button v-if="!rosterLocked" class="text-xs text-brand-600 hover:underline" @click="resetAll">全部出勤</button>
          <button class="text-xs text-slate-400 hover:text-slate-600" @click="active = null">收起</button>
        </div>
      </div>

      <div class="max-h-96 overflow-y-auto border border-slate-200 rounded-lg">
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
            <tr>
              <th class="text-left py-2 pl-2">学号</th><th class="text-left py-2">姓名</th>
              <th class="text-left py-2 w-72">状态</th><th class="text-left py-2 w-32">请假类型</th>
              <th class="text-right py-2 pr-2">累计缺课</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in roster" :key="r.studentId" class="border-b border-slate-100">
              <td class="py-1.5 pl-2 text-slate-500">{{ r.studentNo }}</td>
              <td class="py-1.5">{{ r.studentName }}</td>
              <td class="py-1.5">
                <select v-model="r.status" class="input w-32" :disabled="rosterLocked" @change="markRow(r)">
                  <option v-for="s in attendanceStatuses" :key="s.value" :value="s.value">{{ s.label }}</option>
                </select>
              </td>
              <td class="py-1.5">
                <select v-model="r.leaveType" class="input w-28" :disabled="rosterLocked || r.status !== 'LEAVE'">
                  <option :value="null">—</option>
                  <option value="PERSONAL">事假</option>
                  <option value="SICK">病假</option>
                  <option value="OFFICIAL">公假</option>
                </select>
              </td>
              <td class="py-1.5 text-right pr-2" :class="r.absentTotal >= maxAbsent ? 'text-red-600' : 'text-slate-500'">
                {{ r.absentTotal }}<span class="text-xs text-slate-400"> / {{ maxAbsent }}</span>
                <span v-if="r.thresholdHint" class="text-xs text-red-600 ml-1">{{ r.thresholdHint }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <input v-model="callNote" class="input flex-1 min-w-64" placeholder="点名备注（可选，会写入留痕）" :disabled="rosterLocked" />
        <button v-if="!rosterLocked" class="btn-primary text-sm" @click="askSubmitCall">确定保存并上传</button>
        <span v-else class="text-sm text-slate-400">该课次考勤已锁定，教师不能再改（需教务更正）</span>
      </div>
    </div>

    <!-- 提交点名确认弹窗 -->
    <div v-if="confirmCall" class="fixed inset-0 z-40 bg-slate-900/40 flex items-center justify-center px-4">
      <div class="bg-white rounded-xl shadow-xl max-w-md w-full p-5 space-y-3">
        <h3 class="font-semibold text-slate-800">确认提交本次点名？</h3>
        <p class="text-sm text-slate-600">
          第 {{ active.weekNo }} 周 · 第 {{ active.sessionNo }} 次（{{ active.sessionDate }}）：
          异常 {{ exceptionCount }} 人，其余按出勤记录。
        </p>
        <p class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2">
          提交后本课次考勤将<strong>锁定，不能再修改</strong>（教师端不再提供编辑入口），
          如需更正只能联系教务处理并留痕。
        </p>
        <div class="flex items-center justify-end gap-2 pt-1">
          <button class="btn-ghost text-sm" :disabled="submitting" @click="confirmCall = false">再检查一下</button>
          <button class="btn-primary text-sm" :disabled="submitting" @click="submitCall">
            {{ submitting ? '提交中…' : '确认提交并锁定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 汇总 -->
    <div class="card p-4" v-if="tab === 'stats'">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">考勤汇总（{{ stats.length }} 人）</h2>
        <span class="text-xs text-slate-400">
          扣分 = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)；N_max = floor(有效课次/3) = {{ maxAbsent }}
        </span>
      </div>
      <div class="max-h-96 overflow-y-auto">
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
            <tr>
              <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
              <th class="text-right py-2">出勤</th><th class="text-right py-2">迟到</th>
              <th class="text-right py-2">缺课</th><th class="text-right py-2">请假</th>
              <th class="text-right py-2">扣分</th><th class="text-right py-2">剩余可缺</th>
              <th class="text-left py-2 pl-4">阈值提示</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in stats" :key="s.studentId" class="border-b border-slate-100">
              <td class="py-1.5 text-slate-500">{{ s.studentNo }}</td>
              <td class="py-1.5">{{ s.studentName }}</td>
              <td class="py-1.5 text-right">{{ s.presentCount }}</td>
              <td class="py-1.5 text-right">{{ s.lateCount }}</td>
              <td class="py-1.5 text-right" :class="s.absentCount > 0 ? 'text-red-600' : ''">{{ s.absentCount }}</td>
              <td class="py-1.5 text-right">{{ s.leaveCount }}</td>
              <td class="py-1.5 text-right">{{ s.deductPoints }}</td>
              <td class="py-1.5 text-right" :class="s.remainingAbsent <= 0 ? 'text-red-600' : ''">{{ s.remainingAbsent }}</td>
              <td class="py-1.5 pl-4 text-xs text-red-600">{{ s.thresholdHint || '' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 归档 -->
    <div class="card p-4 space-y-3" v-if="tab === 'archive'">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">归档批次</h2>
        <div class="flex items-center gap-2">
          <button class="btn-ghost text-sm" :disabled="hasArchive('MID')" @click="askArchive('MID')">提交期中归档</button>
          <button class="btn-ghost text-sm" :disabled="hasArchive('FINAL')" @click="askArchive('FINAL')">提交期末归档</button>
        </div>
      </div>
      <p class="text-xs text-slate-400">
        期中归档覆盖前半学期课次、期末归档覆盖后半（若期中未确认则期末覆盖全部）。提交后等教务审核；
        审核通过后这些课次的考勤变为归档口径（判定依据）并冻结。同一阶段只能提交一次，已提交的按钮会置灰。
      </p>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">阶段</th><th class="text-left py-2">状态</th>
            <th class="text-left py-2">覆盖课次</th><th class="text-right py-2">记录数</th>
            <th class="text-right py-2">点名覆盖率</th><th class="text-left py-2">提交时间</th>
            <th class="text-left py-2">教务意见</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in archives" :key="a.id" class="border-b border-slate-100">
            <td class="py-2">{{ a.phase === 'MID' ? '期中' : '期末' }}</td>
            <td class="py-2">
              <span class="tag" :class="archiveClass(a.status)">{{ archiveLabel(a.status) }}</span>
            </td>
            <td class="py-2 text-slate-500">第 {{ a.sessionFrom }} ~ {{ a.sessionTo }} 次（{{ a.sessionCount }}）</td>
            <td class="py-2 text-right">{{ a.recordCount }}</td>
            <td class="py-2 text-right">{{ a.coverageRate }}%</td>
            <td class="py-2 text-slate-500">{{ fmt(a.submitTime) }}</td>
            <td class="py-2 text-slate-500">{{ a.reviewNote || a.returnReason || '—' }}</td>
          </tr>
          <tr v-if="!archives.length"><td colspan="7" class="py-6 text-center text-slate-400">还没有归档批次</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { attendanceApi, attendanceStatuses } from '../../api/attendance'

const route = useRoute()
const teachingClassId = Number(route.query.teachingClassId || 1)

const tabs = [
  { key: 'call', label: '点名' },
  { key: 'stats', label: '汇总' },
  { key: 'archive', label: '归档' }
]
const tab = ref('call')
const sessions = ref([])
const roster = ref([])
const stats = ref([])
const archives = ref([])
const active = ref(null)
const callNote = ref('')
const message = ref('')
const ok = ref(true)
const importResult = ref(null)
const importInput = ref(null)
const maxAbsent = ref(0)
const confirmCall = ref(false)
const submitting = ref(false)

/** 已提交（CALLED）或已归档 → 只读锁定 */
const rosterLocked = computed(() => !!(active.value && (active.value.archived || active.value.rollCallStatus === 'CALLED')))
const exceptionCount = computed(() => roster.value.filter((r) => r.status !== 'PRESENT').length)

function hasArchive(phase) {
  return archives.value.some((a) => a.phase === phase && a.status !== 'RETURNED')
}
function askArchive(phase) {
  const ok2 = window.confirm(
    (phase === 'MID' ? '期中' : '期末') + '归档提交后将进入教务审核，审核通过即冻结这些课次的考勤。确定提交？'
  )
  if (ok2) submitArchive(phase)
}

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function archiveLabel(s) {
  return { PENDING: '待提交', SUBMITTED: '待教务审核', REVIEWED: '已确认', RETURNED: '已退回' }[s] || s
}
function archiveClass(s) {
  return {
    PENDING: 'bg-slate-100 text-slate-500',
    SUBMITTED: 'bg-amber-50 text-amber-600',
    REVIEWED: 'bg-emerald-50 text-emerald-600',
    RETURNED: 'bg-red-50 text-red-600'
  }[s]
}

async function load() {
  sessions.value = await attendanceApi.sessions(teachingClassId)
  stats.value = await attendanceApi.stats(teachingClassId)
  archives.value = await attendanceApi.archives(teachingClassId)
  maxAbsent.value = stats.value.length ? stats.value[0].maxAbsent : 0
}

async function openRoster(s) {
  active.value = s
  roster.value = await attendanceApi.roster(teachingClassId, s.sessionId)
}
function resetAll() {
  roster.value.forEach((r) => {
    if (!r.archived) {
      r.status = 'PRESENT'
      r.leaveType = null
    }
  })
}
function markRow(r) {
  if (r.status !== 'LEAVE') r.leaveType = null
}

async function submitCall() {
  if (submitting.value) return
  if (roster.value.some((r) => r.archived)) {
    toast('该课次已归档，不能再改考勤', false)
    return
  }
  submitting.value = true
  const items = roster.value
    .filter((r) => r.status !== 'PRESENT' || r.leaveType)
    .map((r) => ({ studentId: r.studentId, status: r.status, leaveType: r.leaveType || null }))
  try {
    await attendanceApi.call({
      teachingClassId, sessionId: active.value.sessionId, note: callNote.value || null, items
    })
    confirmCall.value = false
    toast('点名已提交并锁定（异常 ' + items.length + ' 人）；如需更正请联系教务')
    callNote.value = ''
    await load()
    const again = sessions.value.find((x) => x.sessionId === active.value.sessionId)
    await openRoster(again)
  } catch (e) { confirmCall.value = false; toast(e.message, false) } finally { submitting.value = false }
}

function askSubmitCall() {
  confirmCall.value = true
}

async function downloadTemplate() {
  try {
    await attendanceApi.template(teachingClassId)
  } catch (e) { toast(e.message, false) }
}

async function submitArchive(phase) {
  try {
    await attendanceApi.submitArchive({ teachingClassId, phase, note: null })
    toast((phase === 'MID' ? '期中' : '期末') + '归档已提交，等待教务审核')
    await load()
  } catch (e) { toast(e.message, false) }
}

async function onImport(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  try {
    importResult.value = await attendanceApi.importFile(teachingClassId, file)
    if (importResult.value.successRows) {
      toast('导入成功 ' + importResult.value.successRows + ' 行')
      await load()
    }
  } catch (err) { toast(err.message, false) }
  e.target.value = ''
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
