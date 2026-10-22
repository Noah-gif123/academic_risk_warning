<template>
  <div class="space-y-4">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <h1 class="text-lg font-semibold text-slate-800">预警监控</h1>
        <span class="tag bg-slate-100 text-slate-500">引擎 {{ stats.engineVersion || '—' }}</span>
        <span v-if="stats.lastEvalTime" class="text-xs text-slate-400">最近判定 {{ fmt(stats.lastEvalTime) }}</span>
      </div>
      <div class="flex items-center gap-2">
        <select v-model="filters.teachingClassId" class="input w-52" @change="reload">
          <option value="">全部教学班</option>
          <option v-for="c in classOptions" :key="c.teachingClassId" :value="String(c.teachingClassId)">
            {{ c.classCode }} · {{ c.courseName }}
          </option>
        </select>
        <button class="btn-ghost text-sm" :disabled="loading" @click="reload">刷新</button>
        <button class="btn-primary text-sm" :disabled="evaluating" @click="reevaluate">
          {{ evaluating ? '判定中…' : '重新判定' }}
        </button>
      </div>
    </div>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <p class="text-xs text-slate-400">
      判定口径：先看 A 通道（允许缺课 = 课次 ÷ 3 向下取整，缺课超限即硬性不合格、剩余额度 ≤1 次为危险、期末需考 &gt; 100 分视为不可达），
      再用 B 通道（加权分 = 100 − 扣分 + 3 × 缺课次数）比对红 75 / 橙 60 / 黄 40 分级；置信度不足时只输出行为预警。
    </p>

    <!-- 总览 -->
    <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-3">
      <div v-for="c in cards" :key="c.key" class="card p-4">
        <p class="text-xs text-slate-400">{{ c.label }}</p>
        <p class="text-2xl font-semibold mt-1" :class="c.color">{{ c.value }}</p>
        <p class="text-xs text-slate-400 mt-1 truncate">{{ c.hint }}</p>
      </div>
    </div>

    <!-- 名单 -->
    <div class="card p-4 space-y-3">
      <div class="flex flex-wrap items-center gap-2">
        <div class="flex items-center gap-1">
          <button v-for="lv in levelTabs" :key="lv.value" class="px-3 py-1.5 rounded-lg text-sm"
                  :class="filters.level === lv.value ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
                  @click="filters.level = lv.value">{{ lv.label }}</button>
        </div>
        <select v-model="filters.status" class="input w-32">
          <option value="">全部状态</option>
          <option value="ACTIVE">待处理</option>
          <option value="HANDLING">处理中</option>
          <option value="CLOSED">已关闭</option>
          <option value="DISMISSED">已忽略</option>
        </select>
        <input v-model="filters.keyword" class="input w-48" placeholder="学号 / 姓名" />
        <div class="ml-auto flex items-center gap-2">
          <span v-if="selectedIds.length" class="text-xs text-slate-500">已选 {{ selectedIds.length }} 人</span>
          <button class="btn-ghost text-sm" :disabled="!selectedIds.length || batching" @click="batchRemind">
            {{ batching ? '发送中…' : '批量提醒' }}
          </button>
          <button class="btn-ghost text-sm" :disabled="exporting" @click="exportFile">
            {{ exporting ? '导出中…' : '导出名单' }}
          </button>
        </div>
      </div>

      <div class="overflow-x-auto border border-slate-200 rounded-lg">
        <table class="w-full text-sm">
          <thead class="text-slate-500 bg-slate-50 border-b border-slate-200">
            <tr>
              <th class="py-2 pl-2 w-8">
                <input type="checkbox" :checked="allChecked" @change="toggleAll($event.target.checked)" />
              </th>
              <th class="text-left py-2">学号</th>
              <th class="text-left py-2">姓名</th>
              <th class="text-left py-2">教学班</th>
              <th class="text-left py-2">课程</th>
              <th class="text-left py-2">等级</th>
              <th class="text-right py-2">风险分</th>
              <th class="text-right py-2" title="缺课 / 允许缺课 / 剩余额度">缺课额度</th>
              <th class="text-right py-2">期末需考</th>
              <th class="text-left py-2">触发规则</th>
              <th class="text-left py-2">状态</th>
              <th class="text-right py-2 pr-2">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in pageRows" :key="r.id" class="border-b border-slate-100 hover:bg-slate-50/60">
              <td class="py-1.5 pl-2">
                <input type="checkbox" :value="r.id" v-model="selectedIds" />
              </td>
              <td class="py-1.5 text-slate-500">{{ r.studentNo }}</td>
              <td class="py-1.5">{{ r.studentName }}</td>
              <td class="py-1.5 text-slate-500">{{ r.classCode }}</td>
              <td class="py-1.5 text-slate-500">{{ r.courseName }}</td>
              <td class="py-1.5">
                <span class="tag" :class="riskLevelBadge(r.alertLevel)">{{ riskLevelLabel(r.alertLevel) }}</span>
                <span v-if="r.escalated" class="tag bg-red-500 text-white ml-1">升级</span>
              </td>
              <td class="py-1.5 text-right font-medium">{{ num(r.riskScore) }}</td>
              <td class="py-1.5 text-right" :class="r.hardFail ? 'text-red-600 font-medium' : ''">
                {{ r.absentCount ?? 0 }} / {{ r.allowedAbsent ?? 0 }} / {{ r.remainingAbsent ?? 0 }}
              </td>
              <td class="py-1.5 text-right">
                <span v-if="r.finalUnreachable" class="text-red-600">不可达</span>
                <span v-else>{{ r.finalExamNeeded == null ? '—' : num(r.finalExamNeeded) }}</span>
              </td>
              <td class="py-1.5 text-xs" :title="r.alertReason">{{ triggerRuleLabel(r.triggerRule) }}</td>
              <td class="py-1.5">
                <span class="tag" :class="statusBadge(r.status)">{{ alertStatusLabel(r.status) }}</span>
              </td>
              <td class="py-1.5 pr-2 text-right whitespace-nowrap">
                <button class="text-brand-600 hover:underline" @click="openDetail(r)">详情</button>
                <button v-if="canOperate(r)" class="text-slate-500 hover:underline ml-2" @click="remind(r)">提醒</button>
                <button v-if="canOperate(r)" class="text-slate-500 hover:underline ml-2" @click="claim(r)">认领</button>
              </td>
            </tr>
            <tr v-if="!filteredRows.length">
              <td colspan="12" class="py-8 text-center text-slate-400">
                {{ loading ? '加载中…' : '暂无预警：当前教学班学生都处于安全区' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between flex-wrap gap-2">
        <p class="text-xs text-slate-400">
          共 {{ filteredRows.length }} 条 · 红色 {{ levelCount('RED') }} / 橙色 {{ levelCount('ORANGE') }} / 黄色 {{ levelCount('YELLOW') }}
        </p>
        <div class="flex items-center gap-2 text-sm">
          <select v-model.number="pageSize" class="input w-24" @change="page = 1">
            <option :value="10">10 条</option>
            <option :value="20">20 条</option>
            <option :value="50">50 条</option>
          </select>
          <button class="btn-ghost text-sm" :disabled="page <= 1" @click="page--">上一页</button>
          <span class="text-slate-500">{{ page }} / {{ pageCount }}</span>
          <button class="btn-ghost text-sm" :disabled="page >= pageCount" @click="page++">下一页</button>
        </div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <div v-if="detail" class="fixed inset-0 z-40 bg-slate-900/30 flex justify-end" @click.self="closeDetail">
      <div class="w-full max-w-2xl h-full bg-white shadow-xl overflow-y-auto">
        <div class="sticky top-0 bg-white border-b border-slate-200 px-5 py-3 flex items-center justify-between">
          <div class="flex items-center gap-2 flex-wrap">
            <h2 class="font-semibold text-slate-800">
              {{ detail.alert.studentNo }} {{ detail.alert.studentName }}
            </h2>
            <span class="tag" :class="riskLevelBadge(detail.alert.alertLevel)">
              {{ riskLevelLabel(detail.alert.alertLevel) }}预警
            </span>
            <span class="tag" :class="statusBadge(detail.alert.status)">{{ alertStatusLabel(detail.alert.status) }}</span>
          </div>
          <button class="text-slate-400 hover:text-slate-600" @click="closeDetail">✕</button>
        </div>

        <div class="p-5 space-y-4 text-sm">
          <p class="text-slate-600">{{ detail.alert.classCode }} · {{ detail.alert.courseName }}</p>

          <!-- 关键数字 -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">风险分</p>
              <p class="text-lg font-semibold">{{ num(detail.alert.riskScore) }}</p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">置信度</p>
              <p class="text-lg font-semibold">{{ pct(detail.alert.confidence) }}</p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">输出模式</p>
              <p class="text-sm font-medium mt-1">{{ outputModeLabel(detail.alert.outputMode) }}</p>
            </div>
            <div class="rounded-lg bg-slate-50 p-3">
              <p class="text-xs text-slate-400">最近判定</p>
              <p class="text-sm font-medium mt-1">{{ fmt(stats.lastEvalTime) }}</p>
            </div>
          </div>

          <!-- 三通道证据 -->
          <div class="border border-slate-200 rounded-lg p-3 space-y-2">
            <p class="font-medium text-slate-700">三通道证据</p>
            <div class="text-xs text-slate-600 space-y-1">
              <p>
                <span class="tag bg-brand-50 text-brand-700 mr-1">A 行为硬线</span>
                缺课 {{ detail.alert.absentCount ?? 0 }} 次，允许 {{ detail.alert.allowedAbsent ?? 0 }} 次，
                剩余 {{ detail.alert.remainingAbsent ?? 0 }} 次；
                <span v-if="detail.alert.hardFail" class="text-red-600 font-medium">已超上限，硬性不合格</span>
                <span v-else-if="detail.alert.nearFail" class="text-amber-600 font-medium">接近上限</span>
                <span v-else class="text-emerald-600">额度充足</span>
              </p>
              <p>
                <span class="tag bg-indigo-50 text-indigo-700 mr-1">B 加权分</span>
                <template v-if="detail.alert.channelBScore != null">
                  {{ num(detail.alert.channelBScore) }} 分
                  <span v-if="detail.alert.channelBRank">
                    · 班内第 {{ detail.alert.channelBRank }} / {{ detail.alert.channelBClassSize }}
                  </span>
                </template>
                <span v-else class="text-slate-400">未取得有效成绩，仅行为判定</span>
              </p>
              <p>
                <span class="tag bg-slate-100 text-slate-500 mr-1">C 学业增量</span>
                <span class="text-slate-400">第二阶段未启用（需要至少两学期历史成绩）</span>
              </p>
            </div>
            <p class="text-xs text-slate-500 pt-1 border-t border-slate-100">
              <span class="font-medium">判定依据：</span>{{ detail.alert.alertReason }}
            </p>
          </div>

          <!-- 关联数据 -->
          <div class="border border-slate-200 rounded-lg p-3">
            <p class="font-medium text-slate-700 mb-2">关联考勤与成绩</p>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-2 text-xs text-slate-600">
              <p>缺课 <span class="font-medium text-slate-800">{{ detail.riskState?.absentCount ?? 0 }}</span> 次</p>
              <p>迟到 <span class="font-medium text-slate-800">{{ detail.riskState?.lateCount ?? 0 }}</span> 次</p>
              <p>请假 <span class="font-medium text-slate-800">{{ detail.riskState?.leaveCount ?? 0 }}</span> 次</p>
              <p>
                期末需考
                <span class="font-medium" :class="detail.alert.finalUnreachable ? 'text-red-600' : 'text-slate-800'">
                  {{ detail.alert.finalUnreachable ? '不可达' : (detail.alert.finalExamNeeded == null ? '—' : num(detail.alert.finalExamNeeded)) }}
                </span>
              </p>
            </div>
          </div>

          <!-- 处置 -->
          <div class="border border-slate-200 rounded-lg p-3 space-y-2">
            <p class="font-medium text-slate-700">本次处理</p>
            <div class="flex flex-wrap gap-2">
              <select v-model="form.actionType" class="input w-32">
                <option value="TALK">谈话提醒</option>
                <option value="CONTACT">联系学生</option>
                <option value="GUIDE">学业辅导</option>
                <option value="PARENT">联系家长</option>
                <option value="OTHER">其他</option>
              </select>
              <input v-model="form.content" class="input flex-1 min-w-40" placeholder="处理内容（如：已约谈，缺课因兼职）" />
            </div>
            <input v-model="form.result" class="input w-full" placeholder="处理结果（选填，如：承诺补齐作业）" />
            <div class="flex flex-wrap items-center gap-2 pt-1">
              <button class="btn-primary text-sm" :disabled="acting" @click="submitHandle">记录处理</button>
              <button class="btn-ghost text-sm" :disabled="acting" @click="remind(detail.alert)">提醒学生</button>
              <button class="btn-ghost text-sm" :disabled="acting || !canOperate(detail.alert)" @click="claim(detail.alert)">认领</button>
              <button class="btn-ghost text-sm" :disabled="acting || !canOperate(detail.alert)" @click="openClose">关闭预警</button>
              <button class="btn-ghost text-sm text-red-600" :disabled="acting || !canOperate(detail.alert)" @click="openDismiss">忽略</button>
            </div>
            <div v-if="closing" class="flex flex-wrap gap-2 pt-1">
              <input v-model="closing.note" class="input flex-1 min-w-48" :placeholder="closing.kind === 'close' ? '关闭结论（必填）' : '忽略原因（必填）'" />
              <button class="btn-primary text-sm" :disabled="acting" @click="confirmClose">
                {{ closing.kind === 'close' ? '确认关闭' : '确认忽略' }}
              </button>
              <button class="btn-ghost text-sm" @click="closing = null">取消</button>
            </div>
          </div>

          <!-- 时间线 -->
          <div class="border border-slate-200 rounded-lg p-3">
            <p class="font-medium text-slate-700 mb-2">处理时间线（{{ detail.timeline?.length || 0 }}）</p>
            <ol class="relative border-l border-slate-200 ml-2 space-y-3">
              <li v-for="f in detail.timeline" :key="f.id" class="ml-3">
                <span class="absolute -left-1 w-2 h-2 rounded-full bg-brand-400 mt-1.5"></span>
                <p class="text-xs text-slate-400">{{ fmt(f.eventTime) }} · {{ f.operatorRole || '系统' }}</p>
                <p class="text-sm text-slate-700">
                  {{ f.eventDesc || f.eventType }}
                  <span v-if="f.fromStatus || f.toStatus" class="text-xs text-slate-400">
                    （{{ alertStatusLabel(f.fromStatus) }} → {{ alertStatusLabel(f.toStatus) }}）
                  </span>
                </p>
              </li>
              <li v-if="!detail.timeline?.length" class="ml-3 text-xs text-slate-400">暂无记录</li>
            </ol>
          </div>

          <!-- 处置记录 -->
          <div v-if="detail.interventions?.length" class="border border-slate-200 rounded-lg p-3">
            <p class="font-medium text-slate-700 mb-2">处置记录（{{ detail.interventions.length }}）</p>
            <div v-for="i in detail.interventions" :key="i.id" class="text-xs text-slate-600 border-b border-slate-100 py-1.5">
              <span class="tag bg-slate-100 text-slate-500 mr-1">{{ actionTypeLabel(i.actionType) }}</span>
              {{ i.content || '—' }}
              <span v-if="i.result" class="text-slate-400">｜结果：{{ i.result }}</span>
              <span class="text-slate-400">｜{{ fmt(i.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { teacherAlertApi, riskEngineApi } from '../../api/alert'
import { teacherPortalApi } from '../../api/teacherPortal'
import {
  actionTypeLabel,
  alertStatusLabel,
  outputModeLabel,
  riskLevelBadge,
  riskLevelLabel,
  triggerRuleLabel
} from '../../labels'

const loading = ref(false)
const evaluating = ref(false)
const exporting = ref(false)
const batching = ref(false)
const acting = ref(false)
const message = ref('')
const ok = ref(true)

const rows = ref([])
const stats = reactive({
  total: 0, red: 0, orange: 0, yellow: 0,
  active: 0, handling: 0, closed: 0, escalated: 0,
  todayNew: 0, pending: 0, evaluated: 0, lastEvalTime: null, engineVersion: null
})

const classOptions = ref([])
const filters = reactive({ teachingClassId: '', level: '', status: '', keyword: '' })
const page = ref(1)
const pageSize = ref(10)
const selectedIds = ref([])

const detail = ref(null)
const closing = ref(null)
const form = reactive({ actionType: 'TALK', content: '', result: '' })

const levelTabs = [
  { value: '', label: '全部' },
  { value: 'RED', label: '红色' },
  { value: 'ORANGE', label: '橙色' },
  { value: 'YELLOW', label: '黄色' }
]

const cards = computed(() => [
  { key: 'red', label: '红色预警', value: stats.red, color: 'text-red-600', hint: '缺课超限 / 期末不可达' },
  { key: 'orange', label: '橙色预警', value: stats.orange, color: 'text-orange-500', hint: '风险分 60–74' },
  { key: 'yellow', label: '黄色预警', value: stats.yellow, color: 'text-amber-600', hint: '风险分 40–59' },
  { key: 'pending', label: '待处理', value: stats.pending, color: 'text-brand-600', hint: '含处理中与已升级' },
  { key: 'todayNew', label: '今日新增', value: stats.todayNew, color: 'text-slate-800', hint: '今天新生成的预警' },
  { key: 'evaluated', label: '已判定学生', value: stats.evaluated, color: 'text-slate-800', hint: stats.lastEvalTime ? '最近 ' + fmt(stats.lastEvalTime) : '尚未判定' }
])

const filteredRows = computed(() => {
  const kw = filters.keyword.trim()
  return rows.value.filter((r) => {
    if (filters.level && r.alertLevel !== filters.level) return false
    if (filters.status && r.status !== filters.status) return false
    if (kw && !(String(r.studentNo || '').includes(kw) || String(r.studentName || '').includes(kw))) return false
    return true
  })
})
const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize.value)))
const pageRows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})
const allChecked = computed(() =>
  pageRows.value.length > 0 && pageRows.value.every((r) => selectedIds.value.includes(r.id))
)

function levelCount(lv) {
  return filteredRows.value.filter((r) => r.alertLevel === lv).length
}

function canOperate(r) {
  return r && (r.status === 'ACTIVE' || r.status === 'HANDLING' || r.status === 'ESCALATED')
}

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

function statusBadge(s) {
  const m = {
    ACTIVE: 'bg-red-50 text-red-600 border border-red-200',
    HANDLING: 'bg-brand-50 text-brand-700 border border-brand-200',
    ESCALATED: 'bg-purple-50 text-purple-600 border border-purple-200',
    CLOSED: 'bg-slate-100 text-slate-500',
    DISMISSED: 'bg-slate-100 text-slate-400'
  }
  return m[s] || 'bg-slate-100 text-slate-500'
}

function flash(text, success = true) {
  message.value = text
  ok.value = success
  setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 4000)
}

async function loadClasses() {
  try {
    const home = await teacherPortalApi.home()
    const list = []
    ;(home.courses || []).forEach((c) => {
      ;(c.classes || []).forEach((k) => {
        list.push({
          teachingClassId: k.teachingClassId,
          classCode: k.classCode,
          className: k.className,
          courseName: c.courseName
        })
      })
    })
    classOptions.value = list
    const first = new URLSearchParams(location.hash.split('?')[1] || '').get('teachingClassId')
    if (first && list.some((c) => String(c.teachingClassId) === String(first))) {
      filters.teachingClassId = String(first)
    }
  } catch (e) {
    flash(e.message, false)
  }
}

async function loadStats() {
  const s = await teacherAlertApi.stats(filters.teachingClassId || undefined)
  Object.keys(stats).forEach((k) => {
    if (s && Object.prototype.hasOwnProperty.call(s, k)) stats[k] = s[k]
  })
}

async function loadRows() {
  // 等级 / 状态 / 关键词在前端过滤，保证切换筛选时总览与名单口径一致
  rows.value = await teacherAlertApi.list({
    teachingClassId: filters.teachingClassId || undefined
  })
  selectedIds.value = []
}

async function reload() {
  loading.value = true
  try {
    await loadRows()
    await loadStats()
    page.value = 1
  } catch (e) {
    flash(e.message, false)
  } finally {
    loading.value = false
  }
}

async function reevaluate() {
  evaluating.value = true
  try {
    const r = await riskEngineApi.evaluate({
      teachingClassId: filters.teachingClassId ? Number(filters.teachingClassId) : undefined,
      trigger: 'MANUAL'
    })
    flash(`判定完成：${r.evaluatedCount} 人，新增 ${r.insertedCount}，更新 ${r.updatedCount}，等级变化 ${r.levelChangedCount}（${r.costMs} ms）`)
    await reload()
  } catch (e) {
    flash(e.message, false)
  } finally {
    evaluating.value = false
  }
}

async function openDetail(r) {
  try {
    detail.value = await teacherAlertApi.detail(r.id)
    form.actionType = 'TALK'
    form.content = ''
    form.result = ''
    closing.value = null
  } catch (e) {
    flash(e.message, false)
  }
}

function closeDetail() {
  detail.value = null
  closing.value = null
}

async function submitHandle() {
  if (!detail.value) return
  if (!form.content.trim()) {
    flash('请填写处理内容', false)
    return
  }
  acting.value = true
  try {
    await teacherAlertApi.handle(detail.value.alert.id, {
      actionType: form.actionType,
      content: form.content.trim(),
      result: form.result.trim() || undefined
    })
    form.content = ''
    form.result = ''
    await openDetail({ id: detail.value.alert.id })
    await reload()
    flash('已记录处理')
  } catch (e) {
    flash(e.message, false)
  } finally {
    acting.value = false
  }
}

async function claim(r) {
  acting.value = true
  try {
    await teacherAlertApi.claim(r.id)
    if (detail.value && detail.value.alert.id === r.id) await openDetail(r)
    await reload()
    flash('已认领，状态转为处理中')
  } catch (e) {
    flash(e.message, false)
  } finally {
    acting.value = false
  }
}

async function remind(r) {
  acting.value = true
  try {
    await teacherAlertApi.remind(r.id)
    if (detail.value && detail.value.alert.id === r.id) await openDetail(r)
    flash(`已向 ${r.studentName} 发送提醒`)
  } catch (e) {
    flash(e.message, false)
  } finally {
    acting.value = false
  }
}

function openClose() {
  closing.value = { kind: 'close', note: '' }
}

function openDismiss() {
  closing.value = { kind: 'dismiss', note: '' }
}

async function confirmClose() {
  const c = closing.value
  if (!c || !c.note.trim()) {
    flash(c && c.kind === 'close' ? '关闭结论必填' : '忽略原因必填', false)
    return
  }
  acting.value = true
  try {
    if (c.kind === 'close') await teacherAlertApi.close(detail.value.alert.id, c.note.trim())
    else await teacherAlertApi.dismiss(detail.value.alert.id, c.note.trim())
    await openDetail({ id: detail.value.alert.id })
    await reload()
    flash(c.kind === 'close' ? '预警已关闭' : '预警已忽略')
  } catch (e) {
    flash(e.message, false)
  } finally {
    acting.value = false
  }
}

function toggleAll(checked) {
  if (checked) {
    const ids = new Set(selectedIds.value)
    pageRows.value.forEach((r) => ids.add(r.id))
    selectedIds.value = Array.from(ids)
  } else {
    const cur = new Set(pageRows.value.map((r) => r.id))
    selectedIds.value = selectedIds.value.filter((id) => !cur.has(id))
  }
}

async function batchRemind() {
  if (!selectedIds.value.length) return
  if (!confirm(`确定向选中的 ${selectedIds.value.length} 名学生发送提醒？`)) return
  batching.value = true
  let done = 0
  const failures = []
  for (const id of selectedIds.value) {
    try {
      await teacherAlertApi.remind(id)
      done++
    } catch (e) {
      failures.push(e.message)
    }
  }
  batching.value = false
  await reload()
  flash(
    failures.length
      ? `已提醒 ${done} 人，${failures.length} 条失败：${failures[0]}`
      : `已提醒 ${done} 人`,
    failures.length === 0
  )
}

async function exportFile() {
  exporting.value = true
  try {
    await teacherAlertApi.exportFile({
      teachingClassId: filters.teachingClassId || undefined,
      level: filters.level || undefined,
      status: filters.status || undefined
    })
    flash('名单已导出')
  } catch (e) {
    flash(e.message, false)
  } finally {
    exporting.value = false
  }
}

watch(
  () => [filters.level, filters.status, filters.keyword],
  () => {
    page.value = 1
    selectedIds.value = []
  }
)

watch(pageCount, (n) => {
  if (page.value > n) page.value = n
})

onMounted(async () => {
  await loadClasses()
  await reload()
})
</script>
