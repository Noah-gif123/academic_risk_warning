<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div>
        <h1 class="text-xl font-semibold text-slate-800">统计看板</h1>
        <p class="text-xs text-slate-400 mt-1">
          数据重算于 {{ overview.materializedAt || '—' }}
          <span v-if="overview.stale" class="text-amber-600 ml-1">（过期，读取时已触发重算）</span>
        </p>
      </div>
      <div class="flex items-center gap-2">
        <select v-model="period" class="input !w-28" @change="load">
          <option value="term">本学期</option>
          <option value="month">本月</option>
          <option value="week">本周</option>
        </select>
        <button class="btn-secondary" :disabled="loading" @click="load">刷新</button>
        <button class="btn-secondary" @click="exportCsv">导出 CSV</button>
      </div>
    </div>

    <div v-if="error" class="card p-3 text-sm text-red-600">{{ error }}</div>

    <div class="grid grid-cols-2 lg:grid-cols-5 gap-3">
      <StatCard label="风险学生" :value="ov.total" unit="人" :hint="`覆盖 ${ov.classCount || 0} 个教学班`" />
      <StatCard label="红黄占比" :value="ov.riskRatio" unit="%" hint="红+橙+黄 ÷ 学生数">
        <template #label>红黄占比
          <InfoTooltip title="风险面变化率（指标 5）" definition="红黄灯占比 =（红+橙+黄）÷ 学生数；与上一自然周快照对比得到变化（百分点）。"
            sql="SELECT SUM(red+orange+yellow)/SUM(student_count) FROM stat_daily_class WHERE stat_date BETWEEN ..." />
        </template>
      </StatCard>
      <StatCard label="预警关闭率" :value="ov.closeRate" unit="%" hint="周期内新增中已关闭占比">
        <template #label>预警关闭率
          <InfoTooltip title="预警关闭率（指标 1）" definition="周期内新增且已关闭的预警数 ÷ 周期内新增预警数；辅助口径为消化能力（周期内关闭总数 ÷ 新增总数）。"
            sql="SELECT SUM(close_time IS NOT NULL)*100.0/COUNT(*) FROM alert_record WHERE create_time BETWEEN ? AND ?" />
        </template>
      </StatCard>
      <StatCard label="平均处置时长" :value="ov.avgHandleHours" unit="小时" hint="close_time − create_time">
        <template #label>平均处置时长
          <InfoTooltip title="平均处置时长（指标 2）" definition="周期内关闭的预警，close_time − create_time 的均值（小时）；辅助口径为中位数与分档分布。"
            sql="SELECT AVG(TIMESTAMPDIFF(MINUTE, create_time, close_time))/60 FROM alert_record WHERE close_time BETWEEN ? AND ?" />
        </template>
      </StatCard>
      <StatCard label="对照组差值" :value="effect.diffPp" unit="pp" :hint="`干预 ${fmt(effect.treatRate)}% vs 对照 ${fmt(effect.controlRate)}%`">
        <template #label>对照组差值
          <InfoTooltip title="对照组（可归因）" definition="干预组改善率 − 对照组改善率（百分点）。TREAT 为干预组，CONTROL 为本周期仅复评的延迟干预组；属准实验，非净效应。"
            sql="SELECT cohort, SUM(improved)/SUM(due) FROM stat_intervention_effect GROUP BY cohort" />
        </template>
      </StatCard>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">班级风险趋势
            <InfoTooltip title="趋势" definition="按自然周聚合：红黄占比（折线）与新增预警（柱）。数据来自 stat_daily_class 与 alert_record。" />
          </h2>
          <span class="text-[11px] text-slate-400">{{ trendStatus }}</span>
        </div>
        <TrendChart :points="trend" />
      </section>

      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">多门预警学生变化
            <InfoTooltip title="多门预警" definition="同一自然周内被 ≥2 门课程同时预警的学生数；用于观察跨课程风险扩散。" />
          </h2>
          <span class="text-[11px] text-slate-400">人 / 周</span>
        </div>
        <BarChart :rows="multiWarnRows" title="同周被 ≥2 门课预警的学生数" unit=" 人" />
      </section>

      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">干预成效漏斗
            <InfoTooltip title="成效漏斗" definition="周期内新增预警 → 已处置 → 已关闭 → 已干预 → 已改善；用于定位闭环堵点。" />
          </h2>
          <span class="text-[11px] text-slate-400">{{ funnelStatus }}</span>
        </div>
        <FunnelChart :stages="funnel" />
      </section>

      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">对照组对比
            <InfoTooltip title="干预组 vs 对照组" definition="T+7（第 7–14 天）复评等级下降算改善；干预组改善率与延迟干预对照组改善率之差 ≥20pp 说明干预有可观测效果（准实验）。" />
          </h2>
          <span class="text-[11px] text-slate-400">改善率 %</span>
        </div>
        <BarChart :rows="cohortRows" title="TREAT vs CONTROL 改善率" unit="%" />
        <p class="text-xs text-slate-500 mt-2">
          差值 <b class="text-brand-600">{{ fmt(effect.diffPp) }}pp</b>
          ｜到期复评 {{ effect.due || 0 }} 例，未到期 {{ effect.pending || 0 }} 例
        </p>
      </section>
    </div>

    <section class="card p-4">
      <div class="flex items-center justify-between mb-2">
        <h2 class="text-sm font-medium text-slate-700">班级风险对比（Top 12）
          <InfoTooltip title="班级对比" definition="按教学班聚合当前风险等级分布与平均风险分；红/橙/黄/绿堆叠。" />
        </h2>
        <span class="text-[11px] text-slate-400">{{ compare.length }} 个教学班</span>
      </div>
      <StackedBars :rows="compareRows" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { statsApi } from '../../api/stats'
import StatCard from '../../components/stats/StatCard.vue'
import InfoTooltip from '../../components/stats/InfoTooltip.vue'
import TrendChart from '../../components/stats/TrendChart.vue'
import BarChart from '../../components/stats/BarChart.vue'
import FunnelChart from '../../components/stats/FunnelChart.vue'
import StackedBars from '../../components/stats/StackedBars.vue'

const loading = ref(false)
const error = ref('')
const period = ref('term')
const ov = ref({})
const trend = ref([])
const funnel = ref([])
const effect = ref({})
const compare = ref([])
const statuses = ref({})
const overviewMeta = ref({})

const overview = computed(() => overviewMeta.value || {})
const fmt = (v) => (v === null || v === undefined ? '—' : Number(v).toFixed(2))
const trendStatus = computed(() => statuses.value.trend || '')
const funnelStatus = computed(() => statuses.value.funnel || '')
const multiWarnRows = computed(() => trend.value.map((p) => ({ label: String(p.weekStart).slice(5), value: p.multiWarn || 0 })))
const cohortRows = computed(() => [
  { label: '干预组', value: effect.value.treatRate },
  { label: '对照组', value: effect.value.controlRate }
].filter((r) => r.value !== null && r.value !== undefined))
const compareRows = computed(() => compare.value
  .map((r) => ({
    label: `${r.classCode}`,
    red: r.red, orange: r.orange, yellow: r.yellow, green: r.green, total: r.studentCount,
    avgRisk: r.avgRiskScore
  }))
  .sort((a, b) => (b.red + b.orange) - (a.red + a.orange))
  .slice(0, 12))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [o, t, f, e, c] = await Promise.all([
      statsApi.overview({ period: period.value }),
      statsApi.trend({ period: period.value }),
      statsApi.funnel({ period: period.value }),
      statsApi.effect({ period: period.value }),
      statsApi.compare({ period: period.value })
    ])
    ov.value = o.current || {}
    overviewMeta.value = { materializedAt: o.materializedAt, stale: o.stale }
    trend.value = t.current?.points || []
    funnel.value = f.current?.stages || []
    effect.value = e.current || {}
    compare.value = c.current?.rows || []
    statuses.value = { trend: t.dataStatus + (t.reason ? ' · ' + t.reason : ''), funnel: f.dataStatus }
  } catch (e2) {
    error.value = e2.message || String(e2)
  } finally {
    loading.value = false
  }
}

async function exportCsv() {
  try {
    await statsApi.exportCsv({ type: 'overview', period: period.value })
  } catch (e3) {
    error.value = e3.message || String(e3)
  }
}

onMounted(load)
</script>
