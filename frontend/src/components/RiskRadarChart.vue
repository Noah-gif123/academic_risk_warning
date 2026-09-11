<script setup>
import { computed } from 'vue'

/**
 * 风险画像雷达图（纯 SVG）
 * 展示 5 维风险分：该生(实线) vs 班级均值(虚线)，分数越高风险越大
 * 数据形状与后端 /alert/student/{id}/radar 返回一致：
 *   student:  { academic, homework, attendance, knowledge, history, ... }
 *   classAvg: { academic, homework, attendance, knowledge, history }
 *   scope:    COURSE | COMPREHENSIVE | ANY_COURSE | NONE
 *   missingDimensions: ['attendance', ...]  该维度暂无数据（图中按 0 分显示）
 */
const props = defineProps({
  student: { type: Object, default: null },
  classAvg: { type: Object, default: null },
  hasData: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无风险维度数据（该生既无有效预警，也没有每日快照）' },
  scope: { type: String, default: '' },
  missingDimensions: { type: Array, default: () => [] },
  scopeText: { type: String, default: '' }
})

const CX = 170
const CY = 130
const R = 84

const DIMS = [
  { key: 'academic', label: '学业成绩' },
  { key: 'homework', label: '作业完成' },
  { key: 'attendance', label: '出勤表现' },
  { key: 'knowledge', label: '知识掌握' },
  { key: 'history', label: '历史风险' }
]

const DIM_LABEL = Object.fromEntries(DIMS.map((d) => [d.key, d.label]))

// 数据口径说明：区分"本课程/综合/其他课程兜底"
const scopeHint = computed(() => {
  if (props.scopeText) return props.scopeText
  if (props.scope === 'ANY_COURSE') return '该课程暂无有效预警，以下为最近一次其他课程的风险画像'
  if (props.scope === 'COMPREHENSIVE') return '以下为综合预警（未分课程）的风险画像'
  return ''
})

// 缺失维度提示：避免把"没数据"误读成"低风险"
const missingHint = computed(() => {
  const keys = props.missingDimensions || []
  if (!keys.length) return ''
  const names = keys.map((k) => DIM_LABEL[k] || k).join('、')
  return `以下维度暂无数据（图中按 0 分显示，不代表低风险）：${names}`
})

function clamp(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return 0
  return Math.min(100, Math.max(0, Math.round(n * 10) / 10))
}

function angle(i) {
  return -Math.PI / 2 + (Math.PI * 2 * i) / DIMS.length
}

function point(i, r) {
  return [CX + r * Math.cos(angle(i)), CY + r * Math.sin(angle(i))]
}

function seriesValues(series) {
  return DIMS.map((d) => clamp(series?.[d.key]))
}

function toPoints(vals, rScale = 1) {
  return vals.map((v, i) => point(i, R * rScale * (v / 100)))
}

const viewBox = '0 0 340 260'

const gridRings = [0.2, 0.4, 0.6, 0.8, 1]

const gridPolygons = computed(() =>
  gridRings.map((frac) =>
    toPoints(DIMS.map(() => frac * 100)).map((p) => p.join(',')).join(' ')
  )
)

const studentPoints = computed(() => {
  const vals = seriesValues(props.student)
  return toPoints(vals).map((p) => p.join(',')).join(' ')
})

const hasClassAvg = computed(() => {
  if (!props.classAvg) return false
  // 班级均值所有维度均为 0 或 null 时, 视为没有有效均值数据
  return DIMS.some((d) => {
    const v = Number(props.classAvg[d.key])
    return !Number.isNaN(v) && v > 0
  })
})

const classAvgPoints = computed(() => {
  if (!hasClassAvg.value) return ''
  return toPoints(seriesValues(props.classAvg)).map((p) => p.join(',')).join(' ')
})

const classAvgDots = computed(() => {
  if (!hasClassAvg.value) return []
  return toPoints(seriesValues(props.classAvg))
})

// 轴端点（顶点位置）与标签摆放
const axisEnds = computed(() => DIMS.map((d, i) => point(i, R)))

const labelSpots = computed(() =>
  DIMS.map((d, i) => {
    const [x, y] = point(i, R + 22)
    const cos = Math.cos(angle(i))
    const anchor = Math.abs(cos) < 0.4 ? 'middle' : cos > 0 ? 'start' : 'end'
    // 顶/底顶点文本放到顶点外侧更协调
    const sin = Math.sin(angle(i))
    const labelY = sin < -0.6 ? y - 4 : y + 5
    return { key: d.key, text: d.label, x, y: labelY, anchor }
  })
)

const hasStudent = computed(() => props.hasData && props.student)

const studentDots = computed(() => toPoints(seriesValues(props.student)))

// 供取数工具展示
function rounded(v) {
  return clamp(v)
}

// 图形下方的数字说明（避免靠颜色猜分）
const valueRows = computed(() => {
  const rows = []
  const build = (title, cls, source) => ({
    title,
    cls,
    values: DIMS.map((d) => `${d.label}${rounded(source?.[d.key])}`).join(' · ')
  })
  if (hasStudent.value) {
    rows.push(build('该生风险分', 'me', props.student))
  }
  if (hasClassAvg.value) {
    rows.push(build('班级均值', 'avg', props.classAvg))
  }
  return rows
})
defineExpose({ rounded })
</script>

<template>
  <div class="rr-wrap">
    <div v-if="hasStudent" class="rr-chart">
      <svg :viewBox="viewBox" class="rr-svg" role="img" aria-label="五维风险雷达图">
        <!-- 网格环 -->
        <polygon
          v-for="(pts, idx) in gridPolygons"
          :key="idx"
          :points="pts"
          class="rr-grid-ring"
        />
        <!-- 轴线 -->
        <line
          v-for="(ep, i) in axisEnds"
          :key="'axis' + i"
          :x1="CX" :y1="CY" :x2="ep[0]" :y2="ep[1]"
          class="rr-axis"
        />
        <!-- 班级均值：先绘制，学生多边形盖在上面，避免被大均值遮住 -->
        <polygon v-if="hasClassAvg" :points="classAvgPoints" class="rr-shape rr-avg-halo">
          <title>班级均值</title>
        </polygon>
        <polygon v-if="hasClassAvg" :points="classAvgPoints" class="rr-shape rr-avg">
          <title>班级均值</title>
        </polygon>
        <!-- 学生风险分 -->
        <polygon :points="studentPoints" class="rr-shape rr-me-halo">
          <title>该生五维风险分</title>
        </polygon>
        <polygon :points="studentPoints" class="rr-shape rr-me">
          <title>该生五维风险分</title>
        </polygon>
        <circle
          v-for="(p, i) in classAvgDots"
          :key="'avg-dot' + i"
          :cx="p[0]" :cy="p[1]"
          r="2.2"
          class="rr-dot avg-dot"
        >
          <title>班级均值：{{ DIMS[i]?.label }}</title>
        </circle>
        <!-- 学生实际数据点 -->
        <circle
          v-for="(p, i) in studentDots"
          :key="'dot' + i"
          :cx="p[0]" :cy="p[1]"
          r="2.6"
          class="rr-dot"
        >
          <title>{{ DIMS[i]?.label }}：{{ rounded(props.student?.[DIMS[i]?.key]) }} 分</title>
        </circle>
        <!-- 维度标签 -->
        <text
          v-for="ls in labelSpots"
          :key="ls.key"
          :x="ls.x" :y="ls.y"
          :text-anchor="ls.anchor"
          class="rr-label"
        >{{ ls.text }}</text>
      </svg>

      <div class="rr-legend">
        <span class="rr-legend-item"><i class="rr-legend-dot me"></i>该生风险分（紫色实线）</span>
        <span v-if="hasClassAvg" class="rr-legend-item"><i class="rr-legend-dot avg"></i>班级均值（蓝色虚线）</span>
        <span v-else class="rr-hint">数值 0~100，越高风险越大</span>
      </div>

      <div class="rr-values">
        <div v-for="row in valueRows" :key="row.title" class="rr-value-row">
          <span class="rr-value-title" :class="row.cls">{{ row.title }}：</span>
          <span class="rr-value-text">{{ row.values }}</span>
        </div>
      </div>

      <div v-if="scopeHint" class="rr-note">{{ scopeHint }}</div>
      <div v-if="missingHint" class="rr-note rr-note-warn">{{ missingHint }}</div>
    </div>
    <div v-else class="rr-empty">{{ emptyText }}</div>
  </div>
</template>

<style scoped>
.rr-wrap {
  display: flex;
  justify-content: center;
}
.rr-chart {
  width: 100%;
  max-width: 360px;
}
.rr-svg {
  width: 100%;
  height: auto;
  display: block;
}
.rr-grid-ring {
  fill: none;
  stroke: var(--border-light, #e2e8f0);
  stroke-width: 1;
}
.rr-axis {
  stroke: var(--border-light, #e2e8f0);
  stroke-width: 1;
}
.rr-shape {
  fill-opacity: 0.75;
}
/* 白色描边打底，让紫/蓝线不会被网格或重叠色块淹没 */
.rr-me-halo,
.rr-avg-halo {
  fill: none;
  stroke: #fff;
  stroke-width: 5;
  stroke-linejoin: round;
}
.rr-shape.me {
  fill: rgba(124, 58, 237, 0.12);
  stroke: #7c3aed;
  stroke-width: 2.6;
  stroke-linejoin: round;
}
.rr-shape.avg {
  fill: rgba(14, 165, 233, 0.1);
  stroke: #0ea5e9;
  stroke-width: 2.4;
  stroke-dasharray: 6 4;
  stroke-linejoin: round;
}
.rr-dot {
  fill: #7c3aed;
  stroke: #fff;
  stroke-width: 1.3;
}
.rr-dot.avg-dot {
  fill: #0ea5e9;
  stroke: #fff;
  stroke-width: 1.2;
}
.rr-label {
  font-size: 11px;
  fill: var(--text-secondary, #475569);
}
.rr-legend {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
}
.rr-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-secondary, #475569);
}
.rr-legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  display: inline-block;
}
.rr-legend-dot.me { background: #7c3aed; }
.rr-legend-dot.avg {
  background: transparent;
  border: 2px dashed #0ea5e9;
}
.rr-hint {
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
}
.rr-values {
  margin-top: 10px;
  padding: 8px 10px;
  background: var(--bg-muted, #f8fafc);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.9;
}
.rr-value-title {
  font-weight: 600;
}
.rr-value-title.me { color: #7c3aed; }
.rr-value-title.avg { color: #0ea5e9; }
.rr-value-text {
  color: var(--text-secondary, #475569);
}
.rr-note {
  margin-top: 8px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--text-tertiary, #94a3b8);
  text-align: center;
}
.rr-note-warn {
  color: #b45309;
}
.rr-empty {
  padding: 26px 8px;
  text-align: center;
  color: var(--text-tertiary, #94a3b8);
  font-size: 13px;
}
</style>
