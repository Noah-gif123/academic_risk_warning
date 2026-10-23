<template>
  <div class="space-y-4">
    <!-- 标题 -->
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <h1 class="text-lg font-semibold text-slate-800">规则配置</h1>
        <span v-if="active" class="tag bg-brand-50 text-brand-700">生效版本 {{ active.engineVersion }}</span>
        <span v-if="active?.effectiveTime" class="text-xs text-slate-400">
          生效于 {{ fmt(active.effectiveTime) }}
        </span>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" :disabled="loading" @click="load">刷新</button>
        <button class="btn-primary text-sm" @click="startEdit">调整规则</button>
      </div>
    </div>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <p class="text-xs text-slate-400">
      判定口径按版本管理：改规则不会覆盖老版本，而是新增一个版本并切换生效；每次判定都会把引擎版本写进
      <code class="text-slate-500">risk_state</code>，所以能回溯"哪一版规则造成等级变化"。所有变更都进审计日志。
    </p>

    <!-- 当前规则 -->
    <div v-if="active" class="card p-4 space-y-3">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">当前生效的判定口径</h2>
        <span class="text-xs text-slate-400">{{ active.remark || '—' }}</span>
      </div>

      <!-- 阈值条 -->
      <div>
        <div class="flex h-6 rounded overflow-hidden text-[11px] text-white">
          <div class="flex items-center justify-center bg-red-500" :style="{ width: segWidth('red') + '%' }">
            ≥ {{ num(active.thresholdRed) }} 红
          </div>
          <div class="flex items-center justify-center bg-orange-400" :style="{ width: segWidth('orange') + '%' }">
            ≥ {{ num(active.thresholdOrange) }} 橙
          </div>
          <div class="flex items-center justify-center bg-amber-400 text-amber-900" :style="{ width: segWidth('yellow') + '%' }">
            ≥ {{ num(active.thresholdYellow) }} 黄
          </div>
          <div class="flex items-center justify-center bg-emerald-400" :style="{ width: segWidth('green') + '%' }">
            &lt; {{ num(active.thresholdYellow) }} 绿
          </div>
        </div>
        <p class="text-xs text-slate-400 mt-1">
          风险分越高越危险；缺课超过允许上限（有效课次 ÷ 3）直接判红色，不看分数。
        </p>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-2 text-sm">
        <div class="rounded-lg bg-slate-50 p-3">
          <p class="text-xs text-slate-400">扣分系数（缺课 / 迟到 / 请假）</p>
          <p class="font-medium mt-0.5">
            {{ num(active.deductAbsent) }} / {{ num(active.deductLate) }} / {{ num(active.deductLeave) }}
          </p>
        </div>
        <div class="rounded-lg bg-slate-50 p-3">
          <p class="text-xs text-slate-400">请假免扣次数</p>
          <p class="font-medium mt-0.5">{{ active.leaveFreeTimes ?? '—' }} 次 / 学期</p>
        </div>
        <div class="rounded-lg bg-slate-50 p-3">
          <p class="text-xs text-slate-400">期末满分</p>
          <p class="font-medium mt-0.5">{{ num(active.finalFullScore) }} 分</p>
        </div>
        <div class="rounded-lg bg-slate-50 p-3">
          <p class="text-xs text-slate-400">覆盖率阈值 / 置信度下限</p>
          <p class="font-medium mt-0.5">{{ pct(active.coverageThreshold) }} / {{ num(active.confidenceMinOk) }}</p>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-2 pt-1 border-t border-slate-100">
        <button class="btn-ghost text-sm" :disabled="evaluating" @click="reevaluate">
          {{ evaluating ? '判定中…' : '按当前规则重新判定本学期' }}
        </button>
        <span class="text-xs text-slate-400">改完规则想立刻看效果，可以点这里重算；否则下一次判定自动生效</span>
      </div>
    </div>

    <!-- 调整表单 -->
    <div v-if="editing" class="card p-4 space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">新增版本并生效</h2>
        <button class="text-xs text-slate-400 hover:text-slate-600" @click="editing = false">收起</button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-3">
        <label class="text-sm">
          <span class="text-slate-500 text-xs">红色阈值（≥）</span>
          <input v-model="form.thresholdRed" type="number" step="1" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">橙色阈值（≥）</span>
          <input v-model="form.thresholdOrange" type="number" step="1" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">黄色阈值（≥）</span>
          <input v-model="form.thresholdYellow" type="number" step="1" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">版本号（留空自动 +0.1.0）</span>
          <input v-model="form.engineVersion" class="input w-full mt-1" :placeholder="nextVersionHint" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">缺课每次扣分</span>
          <input v-model="form.deductAbsent" type="number" step="0.5" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">迟到每次扣分</span>
          <input v-model="form.deductLate" type="number" step="0.5" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">请假每次扣分（超出免扣）</span>
          <input v-model="form.deductLeave" type="number" step="0.5" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">请假免扣次数</span>
          <input v-model="form.leaveFreeTimes" type="number" step="1" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">期末满分</span>
          <input v-model="form.finalFullScore" type="number" step="1" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">覆盖率阈值（0.3333）</span>
          <input v-model="form.coverageThreshold" type="number" step="0.0001" class="input w-full mt-1" />
        </label>
        <label class="text-sm">
          <span class="text-slate-500 text-xs">置信度下限（0.15）</span>
          <input v-model="form.confidenceMinOk" type="number" step="0.01" class="input w-full mt-1" />
        </label>
        <label class="text-sm md:col-span-2 xl:col-span-2">
          <span class="text-slate-500 text-xs">调整说明（写进审计日志）</span>
          <input v-model="form.remark" class="input w-full mt-1" placeholder="如：期末阶段收紧红色阈值" />
        </label>
      </div>
      <p class="text-xs text-slate-400">
        必须满足 红 &gt; 橙 &gt; 黄；扣分与次数不能为负；覆盖率阈值与置信度下限在 0~1 之间。
      </p>
      <div class="flex items-center gap-2">
        <button class="btn-primary text-sm" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存并生效' }}
        </button>
        <button class="btn-ghost text-sm" @click="editing = false">取消</button>
      </div>
    </div>

    <!-- 版本历史 -->
    <div class="card p-4 space-y-3">
      <h2 class="font-medium text-slate-700">版本历史（{{ versions.length }}）</h2>
      <div class="overflow-x-auto border border-slate-200 rounded-lg">
        <table class="w-full text-sm">
          <thead class="text-slate-500 bg-slate-50 border-b border-slate-200">
            <tr>
              <th class="text-left py-2 pl-3">版本</th>
              <th class="text-left py-2">生效时间</th>
              <th class="text-left py-2">阈值（红 / 橙 / 黄）</th>
              <th class="text-left py-2">扣分（缺 / 迟 / 假）</th>
              <th class="text-left py-2">说明</th>
              <th class="text-left py-2">状态</th>
              <th class="text-right py-2 pr-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in versions" :key="v.id" class="border-b border-slate-100">
              <td class="py-1.5 pl-3 font-medium text-slate-700">{{ v.engineVersion }}</td>
              <td class="py-1.5 text-slate-500">{{ fmt(v.effectiveTime) }}</td>
              <td class="py-1.5 text-slate-600">
                {{ num(v.thresholdRed) }} / {{ num(v.thresholdOrange) }} / {{ num(v.thresholdYellow) }}
              </td>
              <td class="py-1.5 text-slate-600">
                {{ num(v.deductAbsent) }} / {{ num(v.deductLate) }} / {{ num(v.deductLeave) }}
              </td>
              <td class="py-1.5 text-slate-500 text-xs max-w-xs truncate" :title="v.remark">{{ v.remark || '—' }}</td>
              <td class="py-1.5">
                <span class="tag" :class="v.isActive ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'">
                  {{ v.isActive ? '生效中' : '历史版本' }}
                </span>
              </td>
              <td class="py-1.5 pr-3 text-right">
                <button v-if="!v.isActive" class="text-brand-600 hover:underline" :disabled="saving" @click="activate(v)">
                  切换到此版本
                </button>
                <span v-else class="text-xs text-slate-400">—</span>
              </td>
            </tr>
            <tr v-if="!versions.length">
              <td colspan="7" class="py-6 text-center text-slate-400">{{ loading ? '加载中…' : '暂无版本' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminRuleApi } from '../../api/adminRule'
import { riskEngineApi } from '../../api/alert'

const loading = ref(false)
const saving = ref(false)
const evaluating = ref(false)
const editing = ref(false)
const message = ref('')
const ok = ref(true)

const active = ref(null)
const versions = ref([])

const form = reactive({
  engineVersion: '',
  thresholdRed: '',
  thresholdOrange: '',
  thresholdYellow: '',
  deductAbsent: '',
  deductLate: '',
  deductLeave: '',
  leaveFreeTimes: '',
  finalFullScore: '',
  coverageThreshold: '',
  confidenceMinOk: '',
  remark: ''
})

const nextVersionHint = computed(() => {
  const v = active.value?.engineVersion || 'v2.0.0'
  const m = /^[vV]?(\d+)\.(\d+)/.exec(v)
  if (!m) return 'v2.1.0'
  return `v${m[1]}.${Number(m[2]) + 1}.0`
})

/** 阈值条各段宽度（按 0~100 分映射） */
function segWidth(key) {
  const red = Number(active.value?.thresholdRed ?? 75)
  const orange = Number(active.value?.thresholdOrange ?? 60)
  const yellow = Number(active.value?.thresholdYellow ?? 40)
  if (key === 'red') return Math.max(0, 100 - red)
  if (key === 'orange') return Math.max(0, red - orange)
  if (key === 'yellow') return Math.max(0, orange - yellow)
  return Math.max(0, yellow)
}

function num(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return Number.isFinite(n) ? (Math.round(n * 100) / 100).toString() : String(v)
}

function pct(v) {
  if (v === null || v === undefined || v === '') return '—'
  return Math.round(Number(v) * 1000) / 10 + '%'
}

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

function flash(text, success = true) {
  message.value = text
  ok.value = success
  setTimeout(() => {
    if (message.value === text) message.value = ''
  }, 5000)
}

function fillForm(c) {
  form.engineVersion = ''
  form.thresholdRed = c?.thresholdRed ?? ''
  form.thresholdOrange = c?.thresholdOrange ?? ''
  form.thresholdYellow = c?.thresholdYellow ?? ''
  form.deductAbsent = c?.deductAbsent ?? ''
  form.deductLate = c?.deductLate ?? ''
  form.deductLeave = c?.deductLeave ?? ''
  form.leaveFreeTimes = c?.leaveFreeTimes ?? ''
  form.finalFullScore = c?.finalFullScore ?? ''
  form.coverageThreshold = c?.coverageThreshold ?? ''
  form.confidenceMinOk = c?.confidenceMinOk ?? ''
  form.remark = ''
}

async function load() {
  loading.value = true
  try {
    const d = await adminRuleApi.list()
    active.value = d?.active || null
    versions.value = d?.versions || []
  } catch (e) {
    flash(e.message, false)
  } finally {
    loading.value = false
  }
}

function startEdit() {
  fillForm(active.value)
  editing.value = true
}

function bodyOf() {
  const num = (v) => (v === '' || v === null || v === undefined ? undefined : Number(v))
  return {
    engineVersion: form.engineVersion.trim() || undefined,
    thresholdRed: num(form.thresholdRed),
    thresholdOrange: num(form.thresholdOrange),
    thresholdYellow: num(form.thresholdYellow),
    deductAbsent: num(form.deductAbsent),
    deductLate: num(form.deductLate),
    deductLeave: num(form.deductLeave),
    leaveFreeTimes: num(form.leaveFreeTimes),
    finalFullScore: num(form.finalFullScore),
    coverageThreshold: num(form.coverageThreshold),
    confidenceMinOk: num(form.confidenceMinOk),
    remark: form.remark.trim() || undefined
  }
}

async function save() {
  const b = bodyOf()
  if (!(b.thresholdRed > b.thresholdOrange && b.thresholdOrange > b.thresholdYellow)) {
    flash('阈值必须满足 红 > 橙 > 黄', false)
    return
  }
  if (!confirm(`确定新增规则版本并立即生效？\n红 ≥ ${b.thresholdRed} / 橙 ≥ ${b.thresholdOrange} / 黄 ≥ ${b.thresholdYellow}`)) {
    return
  }
  saving.value = true
  try {
    const d = await adminRuleApi.create(b)
    active.value = d?.active || null
    versions.value = d?.versions || []
    editing.value = false
    flash(d?.message || '规则已更新')
  } catch (e) {
    flash(e.message, false)
  } finally {
    saving.value = false
  }
}

async function activate(v) {
  if (!confirm(`切换回 ${v.engineVersion}？切换后下一次判定起生效。`)) return
  saving.value = true
  try {
    const d = await adminRuleApi.activate(v.id)
    active.value = d?.active || null
    versions.value = d?.versions || []
    flash(d?.message || '已切换')
  } catch (e) {
    flash(e.message, false)
  } finally {
    saving.value = false
  }
}

async function reevaluate() {
  evaluating.value = true
  try {
    const r = await riskEngineApi.evaluate({ trigger: 'MANUAL' })
    flash(`已按 ${r.engineVersion} 重新判定：${r.evaluatedCount} 人，新增 ${r.insertedCount}，更新 ${r.updatedCount}，等级变化 ${r.levelChangedCount}（${r.costMs} ms）`)
  } catch (e) {
    flash(e.message, false)
  } finally {
    evaluating.value = false
  }
}

onMounted(load)
</script>
