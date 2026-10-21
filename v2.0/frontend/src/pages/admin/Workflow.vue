<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">流程与问卷</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key">
        {{ t.label }}<span v-if="t.key === 'notice' && notices.length">（{{ notices.length }}）</span>
      </button>
    </div>

    <!-- 入课知会 -->
    <div class="card p-4" v-if="tab === 'notice'">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">入课知会（{{ notices.length }}）</h2>
        <span class="text-xs text-slate-400">教师同意学生加入课程后自动产生；教务只收知会，不审批</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">教学班</th>
            <th class="text-left py-2">学生</th><th class="text-left py-2">申请时间</th>
            <th class="text-left py-2">同意时间</th><th class="text-left py-2">说明</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="n in notices" :key="n.id" class="border-b border-slate-100">
            <td class="py-2">{{ n.courseName }}</td>
            <td class="py-2 text-slate-500">{{ n.className }}</td>
            <td class="py-2">{{ n.studentNo }} · {{ n.studentName }}</td>
            <td class="py-2 text-slate-500">{{ fmt(n.applyTime) }}</td>
            <td class="py-2 text-slate-500">{{ fmt(n.reviewTime) }}</td>
            <td class="py-2 text-slate-500">{{ n.statusText }}</td>
          </tr>
          <tr v-if="!notices.length"><td colspan="6" class="py-6 text-center text-slate-400">暂无入课知会</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 导入批次 -->
    <div class="card p-4" v-if="tab === 'batch'">
      <div class="flex items-center justify-between flex-wrap gap-2 mb-3">
        <h2 class="font-medium text-slate-700">导入批次（{{ batches.length }}）</h2>
        <label class="text-sm">状态
          <select v-model="status" class="input mt-1" @change="loadBatches">
            <option value="">全部</option>
            <option value="SUCCESS">成功</option>
            <option value="PARTIAL">部分成功</option>
            <option value="FAILED">失败</option>
            <option value="ROLLED_BACK">已回滚</option>
          </select>
        </label>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">批次号</th><th class="text-left py-2">类型</th>
            <th class="text-left py-2">文件</th><th class="text-right py-2">总/成功/失败</th>
            <th class="text-left py-2">状态</th><th class="text-left py-2">操作人</th>
            <th class="text-left py-2">时间</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in batches" :key="b.id" class="border-b border-slate-100">
            <td class="py-2 text-slate-500">{{ b.batchNo }}</td>
            <td class="py-2">{{ b.importType }}</td>
            <td class="py-2 max-w-56 truncate" :title="b.fileName">{{ b.fileName || '—' }}</td>
            <td class="py-2 text-right">{{ b.totalRows }} / {{ b.successRows }} / {{ b.failedRows }}</td>
            <td class="py-2">
              <span class="tag" :class="statusClass(b.status)">{{ b.statusText }}</span>
            </td>
            <td class="py-2 text-slate-500">{{ b.operatorName || b.operatorRole || '—' }}</td>
            <td class="py-2 text-slate-500">{{ fmt(b.endTime || b.startTime) }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <button v-if="b.status !== 'ROLLED_BACK'" class="text-red-600 hover:underline" @click="rollback(b)">回滚</button>
              <span v-else class="text-xs text-slate-400">已回滚 {{ fmt(b.rollbackTime) }}</span>
            </td>
          </tr>
          <tr v-if="!batches.length"><td colspan="8" class="py-6 text-center text-slate-400">
            还没有导入批次（考勤导入、排课导入会在这里留下记录）
          </td></tr>
        </tbody>
      </table>
    </div>

    <!-- 审计（并到这里，导航不再单列） -->
    <div class="card p-4 space-y-2" v-if="tab === 'audit'">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">审计日志</h2>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/admin/audit')">打开完整审计页 →</button>
      </div>
      <p class="text-sm text-slate-500">
        改分、改考勤、归档与退回、成绩构成锁定、项目评分、导入回滚等关键操作都会留痕；按业务类型 / 动作 / 时间检索请进完整审计页。
      </p>
    </div>

    <!-- 问卷流转 -->
    <div class="card p-4 space-y-2" v-if="tab === 'survey'">
      <h2 class="font-medium text-slate-700">问卷流转</h2>
      <p class="text-sm text-slate-500">
        本版问卷只有「教师风格判定」一种：导员按行政班发布 → 匿名收集 → 教务分发到课程组全体教师 → 汇总。
      </p>
      <p class="text-xs text-slate-400">
        问卷流转属于第二阶段（学情预警）的能力，第一阶段先保留入口占位；届时在这里查看各环节状态与超时提醒。
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminPortalApi } from '../../api/adminPortal'
import { adminApi } from '../../api/admin'

const tabs = [
  { key: 'notice', label: '入课知会' },
  { key: 'batch', label: '导入批次' },
  { key: 'survey', label: '问卷流转' },
  { key: 'audit', label: '审计日志' }
]
const tab = ref('notice')
const notices = ref([])
const batches = ref([])
const status = ref('')
const message = ref('')
const ok = ref(true)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function statusClass(s) {
  return {
    SUCCESS: 'bg-emerald-50 text-emerald-600',
    PARTIAL: 'bg-amber-50 text-amber-600',
    FAILED: 'bg-red-50 text-red-600',
    ROLLED_BACK: 'bg-slate-200 text-slate-600',
    RUNNING: 'bg-brand-50 text-brand-600'
  }[s]
}

async function loadNotices() {
  notices.value = await adminPortalApi.notices()
}
async function loadBatches() {
  batches.value = await adminPortalApi.batches(undefined, status.value || undefined)
}
async function rollback(b) {
  const reason = window.prompt('回滚原因（会记入审计）：', '导入数据有误，整批回滚')
  if (reason === null) return
  try {
    await adminPortalApi.rollback(b.id, reason)
    toast('已回滚：' + b.batchNo)
    await loadBatches()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    await Promise.all([loadNotices(), loadBatches()])
    // 触发一次学期加载，保证下拉可用（这里只用到全部学期，失败也不阻塞）
    await adminApi.listTerms().catch(() => [])
  } catch (e) { toast(e.message, false) }
})
</script>
