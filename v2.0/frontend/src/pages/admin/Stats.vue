<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div>
        <h1 class="text-xl font-semibold text-slate-800">统计看板（教务）</h1>
        <p class="text-xs text-slate-400 mt-1">
          数据重算于 {{ meta.materializedAt || '—' }}
          <span v-if="meta.refreshed" class="text-brand-600 ml-1">（因 {{ meta.refreshReason }} 已自动重算）</span>
          <span v-else-if="meta.stale" class="text-amber-600 ml-1">（数据过期，读取时已触发重算）</span>
        </p>
      </div>
      <div class="flex items-center gap-2">
        <select v-model="period" class="input !w-28" @change="load">
          <option value="term">本学期</option>
          <option value="month">本月</option>
          <option value="week">本周</option>
        </select>
        <button class="btn-secondary" :disabled="loading" @click="load">刷新</button>
        <button class="btn-secondary" :disabled="loading" @click="recompute">重算</button>
        <button class="btn-secondary" @click="exportCsv">导出 CSV</button>
      </div>
    </div>

    <div v-if="error" class="card p-3 text-sm text-red-600">{{ error }}</div>

    <div class="grid grid-cols-2 lg:grid-cols-5 gap-3">
      <StatCard label="预警关闭率" :value="ov.closeRate" unit="%">
        <template #label>预警关闭率
          <InfoTooltip title="预警关闭率（指标 1）" definition="周期内新增且已关闭 ÷ 周期内新增；辅助口径为消化能力。"
            sql="SELECT SUM(close_time IS NOT NULL)*100.0/COUNT(*) FROM alert_record WHERE term_id=? AND create_time BETWEEN ? AND ?" />
        </template>
      </StatCard>
      <StatCard label="平均处置时长" :value="ov.avgHandleHours" unit="小时" :hint="`中位 ${ov.medianHandleHours ?? '—'} h`">
        <template #label>平均处置时长
          <InfoTooltip title="平均处置时长（指标 2）" definition="周期内关闭预警 close��time − create_time 均值；辅助看中位数与分档分布。" />
        </template>
      </StatCard>
      <StatCard label="红黄占比" :value="ov.riskRatio" unit="%">
        <template #label>红黄占比
          <InfoTooltip title="风险面（指标 5）" definition="（红+橙+黄）÷ 学生数；与上一自然周快照对比得到变化。" />
        </template>
      </StatCard>
      <StatCard label="干预组改善率" :value="effect.treatRate" unit="%" :hint="`对照组 ${fmt(effect.controlRate)}%`">
        <template #label>干预组改善率
          <InfoTooltip title="干预后等级下降比例（指标 3）" definition="周期内干预且完成 T+7 复评的记录中，等级下降数 ÷ 有效复评数。"
            sql="SELECT SUM(rank(level_after) &lt; rank(level_before)) FROM intervention_record WHERE effect_check_time BETWEEN create_time + 7 DAY AND create_time + 14 DAY" />
        </template>
      </StatCard>
      <StatCard label="对照组差值" :value="effect.diffPp" unit="pp" hint="TREAT − CONTROL（准实验）">
        <template #label>对照组差值
          <InfoTooltip title="对照组（可归因）" definition="干预组改善率 − 对照组改善率（百分点）；目标演示 ≥20pp，属准实验非净效应。" />
        </template>
      </StatCard>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">课程集中风险 Top5
            <InfoTooltip title="课程集中风险" definition="按课程汇总当前风险等级分布，取红+橙最多的前 5 门；点击柱条可筛选该课程。" />
          </h2>
          <span class="text-[11px] text-slate-400">红+橙人数</span>
        </div>
        <BarChart :rows="courseTop5" title="红+橙人数 Top5 课程" unit=" 人" @row-click="onTopClick" />
      </section>

      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">课程因子校准
            <InfoTooltip title="course_factor 校准" definition="横轴为课程历史挂科率，纵轴为 P0-2 初始化后的课程因子（0.90–1.15）；偏离 1.00 越多表示历史挂科对风险分影响越大。点击点可筛选课程。" />
          </h2>
          <span class="text-[11px] text-slate-400">{{ calibration.length }} 门课</span>
        </div>
        <ScatterPlot :points="calibration" @point-click="onPointClick" />
      </section>

      <section class="card p-4">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-sm font-medium text-slate-700">处置时效分布
            <InfoTooltip title="处置时长分档" definition="已关闭预警按 close_time − create_time 分为 <24h / 24–72h / >72h 三档。" />
          </h2>
          <span class="text-[11px] text-slate-400">件</span>
        </div>
        <BarChart :rows="handleRows" title="关闭预警的处置时长分档" unit=" 件" />
      </section>

      <section class="card p-4">
        <div class="flex items-center justify-between mb-2 flex-wrap gap-2">
          <h2 class="text-sm font-medium text-slate-700">课程风险分布
            <InfoTooltip title="课程分布" definition="各课程当前红/橙/黄/绿人数堆叠；点击柱条可筛选该课程并查看明细。" />
          </h2>
          <div class="flex items-center gap-2">
            <select v-model="selectedCourse" class="input !w-44 !py-1 text-xs">
              <option value="ALL">全部课程</option>
              <option v-for="c in courseOptions" :key="c" :value="c">{{ c }}</option>
            </select>
            <span class="text-[11px] text-slate-400">{{ courseRows.length }} 门课</span>
          </div>
        </div>
        <StackedBars :rows="courseRows" :height="300" @row-click="onCourseClick" />
        <div v-if="detail" class="mt-3 rounded-lg bg-slate-50 p-3 text-xs text-slate-600">
          <b>{{ detail.name }}</b>：红 {{ detail.red }}｜橙 {{ detail.orange }}｜黄 {{ detail.yellow }}｜绿 {{ detail.green }}
          ｜合计 {{ detail.total }}｜风险占比 {{ fmt(detail.riskRatio) }}%
        </div>
      </section>
    </div>

    <section class="card p-4">
      <h2 class="text-sm font-medium text-slate-700 mb-2">AI 质量卡（P2 接入）</h2>
      <p class="text-xs text-slate-500">
        当前版本尚未产生 Agent 运行数据；P2 完成后在此展示工具选择准确率、数字一致率、平均步数与 P95 时延。
      </p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { statsApi } from '../../api/stats'
import StatCard from '../../components/stats/StatCard.vue'
import InfoTooltip from '../../components/stats/InfoTooltip.vue'
import BarChart from '../../components/stats/BarChart.vue'
import StackedBars from '../../components/stats/StackedBars.vue'
import ScatterPlot from '../../components/stats/ScatterPlot.vue'

const loading = ref(false)
const error = ref('')
const period = ref('term')
const ov = ref({})
const distribution = ref({})
const effect = ref({})
const meta = ref({})
const selectedCourse = ref('ALL')
const detail = ref(null)

const fmt = (v) => (v === null || v === undefined ? '—' : Number(v).toFixed(2))
const allCourses = computed(() => distribution.value.byCourse || [])
const courseOptions = computed(() => allCourses.value.map((c) => c.name))
const courseTop5 = computed(() => allCourses.value
  .filter((c) => selectedCourse.value === 'ALL' || c.name === selectedCourse.value)
  .map((c) => ({ label: shortName(c.name), value: c.red + c.orange }))
  .sort((a, b) => b.value - a.value)
  .slice(0, 5))
const handleRows = computed(() => {
  const b = ov.value.handleBuckets || {}
  return [
    { label: '<24h', value: b.lt24 || 0 },
    { label: '24–72h', value: b.h24to72 || 0 },
    { label: '>72h', value: b.gt72 || 0 }
  ]
})
const courseRows = computed(() => allCourses.value
  .filter((c) => selectedCourse.value === 'ALL' || c.name === selectedCourse.value)
  .map((c) => ({
    label: shortName(c.name), name: c.name,
    red: c.red, orange: c.orange, yellow: c.yellow, green: c.green,
    total: c.total, riskRatio: c.riskRatio
  })))
const calibration = computed(() => (distribution.value.calibration || [])
  .filter((c) => selectedCourse.value === 'ALL' || c.courseName === selectedCourse.value)
  .map((c) => ({
    x: Number(c.failRate) || 0,
    y: Number(c.courseFactor) || 1,
    label: c.courseName,
    short: shortName(c.courseName),
    ok: Number(c.courseFactor) <= 1.15
  })))

function shortName(name) {
  if (!name) return ''
  return name.length > 8 ? name.slice(0, 7) + '…' : name
}

function onTopClick(short) {
  const hit = allCourses.value.find((c) => shortName(c.name) === short)
  if (hit) selectedCourse.value = hit.name
}

function onPointClick(data) {
  if (data && data.label) selectedCourse.value = data.label
}

function onCourseClick(short) {
  const hit = allCourses.value.find((c) => shortName(c.name) === short)
  if (hit) {
    selectedCourse.value = hit.name
    detail.value = {
      name: hit.name, red: hit.red, orange: hit.orange, yellow: hit.yellow,
      green: hit.green, total: hit.total, riskRatio: hit.riskRatio
    }
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [o, d, e] = await Promise.all([
      statsApi.overview({ period: period.value }),
      statsApi.distribution({ period: period.value }),
      statsApi.effect({ period: period.value })
    ])
    ov.value = o.current || {}
    meta.value = {
      materializedAt: o.materializedAt, stale: o.stale,
      refreshed: o.refreshed, refreshReason: o.refreshReason
    }
    distribution.value = d.current || {}
    effect.value = e.current || {}
  } catch (e2) {
    error.value = e2.message || String(e2)
  } finally {
    loading.value = false
  }
}

async function recompute() {
  try {
    error.value = ''
    await statsApi.recompute()
    await load()
  } catch (e3) {
    error.value = e3.message || String(e3)
  }
}

async function exportCsv() {
  try {
    await statsApi.exportCsv({ type: 'compare', period: period.value })
  } catch (e4) {
    error.value = e4.message || String(e4)
  }
}

onMounted(load)
</script>
