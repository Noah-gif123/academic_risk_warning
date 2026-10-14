<script setup>
import { computed } from 'vue'

/**
 * 风险趋势折线图（纯 SVG）
 * 数据形状与后端 /alert/student/{id}/trend 返回的 points 一致：
 *   [{ date: 'yyyy-MM-dd', riskScore, alertLevel, predictedScore }]
 * 折线为该生综合风险分走势，点颜色对应预警等级
 * source: SNAPSHOT（预警快照）| ALERT_RECORD（无快照时按天去重的预警记录）| NONE
 */
const props = defineProps({
  points: { type: Array, default: () => [] },
  emptyText: { type: String, default: '暂无风险趋势数据（尚未生成每日快照）' },
  source: { type: String, default: '' },
  sourceText: { type: String, default: '' }
})

const LEVEL_COLOR = {
  RED: '#ef4444',
  ORANGE: '#f97316',
  YELLOW: '#eab308',
  GREEN: '#22c55e'
}

const W = 620
const H = 228
const PAD_L = 44
const PAD_R = 20
const PAD_T = 14
const PAD_B = 36
const X0 = PAD_L
const X1 = W - PAD_R
const Y0 = PAD_T
const Y1 = H - PAD_B
const PLOT_W = X1 - X0
const PLOT_H = Y1 - Y0

// 数据来源提示：区分"每日快照"与"预警记录兜底"，避免误读曲线密度
const sourceHint = computed(() => {
  if (props.sourceText) return props.sourceText
  if (props.source === 'ALERT_RECORD') return '暂无每日快照，当前曲线由预警记录按天去重生成'
  if (props.source === 'SNAPSHOT') return '数据来源：每日预警快照'
  return ''
})

function clamp(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return null
  return Math.min(100, Math.max(0, n))
}

// 只保留有效的风险分数据点
const data = computed(() =>
  (props.points || [])
    .map((p) => ({ ...p, value: clamp(p.riskScore) }))
    .filter((p) => p.value != null)
)

function fx(i) {
  if (data.value.length <= 1) return X0 + PLOT_W / 2
  return X0 + (PLOT_W * i) / (data.value.length - 1)
}
function fy(v) {
  return Y0 + ((100 - v) / 100) * PLOT_H
}

const linePath = computed(() => {
  if (data.value.length < 2) return ''
  return data.value.map((p, i) => `${i === 0 ? 'M' : 'L'} ${fx(i).toFixed(2)} ${fy(p.value).toFixed(2)}`).join(' ')
})

const areaPath = computed(() => {
  if (data.value.length < 2) return ''
  const base = fy(0)
  return `${linePath.value} L ${fx(data.value.length - 1).toFixed(2)} ${base.toFixed(2)} L ${fx(0).toFixed(2)} ${base.toFixed(2)} Z`
})

const gridLines = computed(() =>
  [0, 20, 40, 60, 80, 100].map((v) => ({ v, y: fy(v) }))
)

const yTicks = [0, 20, 40, 60, 80, 100]

// x 轴刻度：点数多时抽稀，保证首尾可见
const xLabels = computed(() => {
  const n = data.value.length
  if (!n) return []
  const maxLabel = 7
  if (n <= maxLabel) return data.value.map((p, i) => ({ i, text: fmtDate(p.date) }))
  const step = Math.ceil(n / maxLabel)
  const arr = []
  for (let i = 0; i < n; i += step) arr.push({ i, text: fmtDate(data.value[i].date) })
  if (arr[arr.length - 1].i !== n - 1) arr.push({ i: n - 1, text: fmtDate(data.value[n - 1].date) })
  return arr
})

function fmtDate(d) {
  if (!d) return ''
  const s = String(d)
  const m = s.length >= 10 ? s.slice(5, 7) : s
  const day = s.length >= 10 ? s.slice(8, 10) : ''
  return m && day ? `${m}/${day}` : s
}

const levelCount = computed(() => {
  const map = {}
  data.value.forEach((p) => { if (p.alertLevel) map[p.alertLevel] = true })
  return Object.keys(map).filter((k) => LEVEL_COLOR[k])
})

function dotColor(p) {
  return LEVEL_COLOR[p.alertLevel] || '#7c3aed'
}
</script>

<template>
  <div class="rt-wrap">
    <div v-if="data.length" class="rt-chart">
      <svg :viewBox="`0 0 ${W} ${H}`" class="rt-svg" role="img" aria-label="风险趋势折线图">
        <!-- 横网格 + Y 轴刻度 -->
        <line
          v-for="g in gridLines"
          :key="'grid' + g.v"
          :x1="X0" :y1="g.y" :x2="X1" :y2="g.y"
          class="rt-grid-line"
        />
        <text v-for="t in yTicks" :key="'yt' + t" :x="X0 - 6" :y="fy(t) + 3" class="rt-y-tick">
          {{ t }}
        </text>
        <!-- 面积 -->
        <path v-if="areaPath" :d="areaPath" class="rt-area" />
        <!-- 折线 -->
        <path v-if="linePath" :d="linePath" class="rt-line" />
        <!-- 数据点 -->
        <g v-for="(p, i) in data" :key="i">
          <circle :cx="fx(i)" :cy="fy(p.value)" r="4" class="rt-point" :fill="dotColor(p)" />
          <title>{{ p.date }}：综合风险分 {{ p.value }} 分</title>
        </g>
        <!-- X 轴刻度文字 -->
        <text v-for="xl in xLabels" :key="'xl' + xl.i" :x="fx(xl.i)" :y="H - 12" class="rt-x-tick">
          {{ xl.text }}
        </text>
        <!-- X 轴线 -->
        <line :x1="X0" :y1="Y1" :x2="X1" :y2="Y1" class="rt-axis-bottom" />
      </svg>

      <div class="rt-legend">
        <span class="rt-legend-title">点颜色对应风险等级：</span>
        <span v-for="lv in levelCount" :key="lv" class="rt-legend-item">
          <i class="rt-legend-dot" :style="{ background: LEVEL_COLOR[lv] }"></i>{{ lv }}
        </span>
        <span class="rt-hint">综合风险分 0~100，越高越危险</span>
      </div>

      <div v-if="sourceHint" class="rt-source">{{ sourceHint }}</div>
    </div>
    <div v-else class="rt-empty">{{ emptyText }}</div>
  </div>
</template>

<style scoped>
.rt-wrap { width: 100%; }
.rt-chart { width: 100%; }
.rt-svg {
  width: 100%;
  height: auto;
  display: block;
}
.rt-grid-line {
  stroke: var(--border-light, #e2e8f0);
  stroke-width: 1;
}
.rt-axis-bottom {
  stroke: var(--border-light, #e2e8f0);
  stroke-width: 1.4;
}
.rt-y-tick {
  font-size: 10px;
  fill: var(--text-tertiary, #94a3b8);
  text-anchor: end;
}
.rt-x-tick {
  font-size: 10px;
  fill: var(--text-tertiary, #94a3b8);
  text-anchor: middle;
}
.rt-area {
  fill: rgba(124, 58, 237, 0.08);
}
.rt-line {
  fill: none;
  stroke: #7c3aed;
  stroke-width: 2;
  stroke-linejoin: round;
  stroke-linecap: round;
}
.rt-point {
  stroke: #fff;
  stroke-width: 1.5;
}
.rt-legend {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}
.rt-legend-title {
  font-size: 12px;
  color: var(--text-secondary, #475569);
}
.rt-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary, #475569);
}
.rt-legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}
.rt-hint {
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
}
.rt-source {
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
  text-align: center;
}
.rt-empty {
  padding: 26px 8px;
  text-align: center;
  color: var(--text-tertiary, #94a3b8);
  font-size: 13px;
}
</style>
