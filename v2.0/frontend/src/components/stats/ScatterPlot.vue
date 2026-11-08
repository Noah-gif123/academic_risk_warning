<template>
  <div>
    <svg :viewBox="`0 0 ${W} ${H}`" class="w-full h-60" role="img" aria-label="课程因子校准散点">
      <line :x1="PL" :x2="W - PR" :y1="H - PB" :y2="H - PB" stroke="#cbd5e1" />
      <line :x1="PL" :x2="PL" :y1="PT" :y2="H - PB" stroke="#cbd5e1" />
      <line :x1="PL" :x2="W - PR" :y1="yOf(1.0)" :y2="yOf(1.0)" stroke="#94a3b8" stroke-dasharray="4 3" />
      <text :x="W - PR" :y="yOf(1.0) - 4" text-anchor="end" class="fill-slate-400" font-size="9">factor = 1.00</text>
      <g v-for="(p, i) in points" :key="i">
        <circle :cx="xOf(p.x)" :cy="yOf(p.y)" r="4" :fill="p.ok ? '#2563eb' : '#ef4444'" opacity="0.8">
          <title>{{ p.label }}：挂科率 {{ p.x }}% / 因子 {{ p.y }}</title>
        </circle>
        <text :x="xOf(p.x) + 6" :y="yOf(p.y) - 5" class="fill-slate-500" font-size="8">{{ p.short }}</text>
      </g>
      <text :x="PL - 6" :y="PT + 4" text-anchor="end" class="fill-slate-400" font-size="9">1.20</text>
      <text :x="PL - 6" :y="yOf(1.05) + 3" text-anchor="end" class="fill-slate-400" font-size="9">1.05</text>
      <text :x="PL - 6" :y="yOf(0.95) + 3" text-anchor="end" class="fill-slate-400" font-size="9">0.95</text>
      <text :x="PL - 6" :y="H - PB" text-anchor="end" class="fill-slate-400" font-size="9">0.90</text>
      <text v-for="t in [0, 10, 20, 30]" :key="'x' + t" :x="xOf(t)" :y="H - 8" text-anchor="middle"
            class="fill-slate-400" font-size="9">{{ t }}%</text>
      <text :x="W / 2" :y="H" text-anchor="middle" class="fill-slate-400" font-size="9">课程历史挂科率</text>
      <text :x="8" :y="H / 2" text-anchor="middle" transform="rotate(-90 8 110)"
            class="fill-slate-400" font-size="9">课程因子</text>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ points: { type: Array, default: () => [] } })
const W = 640
const H = 260
const PL = 42
const PR = 14
const PT = 16
const PB = 28
const maxX = computed(() => Math.max(30, ...props.points.map((p) => Number(p.x) || 0)))
const xOf = (v) => PL + ((Number(v) || 0) / maxX.value) * (W - PL - PR)
const yOf = (v) => PT + ((1.2 - (Number(v) || 1)) / 0.3) * (H - PT - PB)
</script>
