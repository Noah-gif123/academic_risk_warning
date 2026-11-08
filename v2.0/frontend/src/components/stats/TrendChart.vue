<template>
  <div>
    <svg :viewBox="`0 0 ${W} ${H}`" class="w-full h-56" role="img" aria-label="风险趋势">
      <g v-for="t in [0, 25, 50, 75, 100]" :key="t">
        <line :x1="PL" :x2="W - PR" :y1="yOf(t)" :y2="yOf(t)" stroke="#e2e8f0" stroke-width="1" />
        <text :x="PL - 6" :y="yOf(t) + 3" text-anchor="end" class="fill-slate-400" font-size="9">{{ t }}</text>
      </g>
      <rect v-for="(p, i) in points" :key="'b' + i" :x="barX(i)" :y="H - PB - barH(p)"
            :width="barW" :height="barH(p)" fill="#bfdbfe" rx="2">
        <title>{{ p.weekStart }} 新增预警 {{ p.alertNew || 0 }}</title>
      </rect>
      <polyline :points="linePoints" fill="none" stroke="#2563eb" stroke-width="2" />
      <circle v-for="(c, i) in coords" :key="'c' + i" :cx="c.x" :cy="c.y" r="3" fill="#1d4ed8">
        <title>{{ c.weekStart }} 红黄占比 {{ fmt(c.riskRatio) }}%</title>
      </circle>
      <text v-for="(p, i) in points" :key="'x' + i" :x="barX(i) + barW / 2" :y="H - 8"
            text-anchor="middle" class="fill-slate-400" font-size="9">{{ shortWeek(p.weekStart) }}</text>
    </svg>
    <div class="flex items-center gap-4 text-[11px] text-slate-500 mt-1">
      <span><i class="inline-block w-3 h-2 bg-blue-200 mr-1 align-middle" />新增预警（右轴缩放）</span>
      <span><i class="inline-block w-3 h-0.5 bg-blue-600 mr-1 align-middle" />红黄占比 %</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ points: { type: Array, default: () => [] } })
const W = 640
const H = 220
const PL = 36
const PR = 12
const PT = 16
const PB = 26
const maxAlert = computed(() => Math.max(1, ...props.points.map((p) => Number(p.alertNew) || 0)))
const barW = computed(() => (props.points.length ? ((W - PL - PR) / props.points.length) * 0.5 : 0))
const barX = (i) => PL + (i + 0.25) * ((W - PL - PR) / Math.max(1, props.points.length)) - barW.value / 2
const barH = (p) => ((Number(p.alertNew) || 0) / maxAlert.value) * (H - PT - PB) * 0.6
const yOf = (v) => PT + (1 - v / 100) * (H - PT - PB)
const coords = computed(() => props.points.map((p, i) => ({
  ...p,
  x: PL + (props.points.length <= 1 ? 0 : (i * (W - PL - PR)) / (props.points.length - 1)),
  y: yOf(Number(p.riskRatio) || 0)
})))
const linePoints = computed(() => coords.value.map((c) => `${c.x},${c.y}`).join(' '))
const fmt = (v) => (v === null || v === undefined ? '—' : Number(v).toFixed(2))
const shortWeek = (s) => (s ? String(s).slice(5) : '')
</script>
