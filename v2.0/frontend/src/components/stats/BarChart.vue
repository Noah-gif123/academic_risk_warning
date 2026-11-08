<template>
  <div>
    <svg :viewBox="`0 0 ${W} ${H}`" class="w-full h-52" role="img" :aria-label="title">
      <line :x1="PL" :x2="W - PR" :y1="H - PB" :y2="H - PB" stroke="#e2e8f0" />
      <rect v-for="(r, i) in rows" :key="i" :x="xOf(i)" :y="H - PB - hOf(r)" :width="bw" :height="hOf(r)"
            :fill="colors[i % colors.length]" rx="3" opacity="0.85">
        <title>{{ r.label }}：{{ fmt(r.value) }}{{ unit }}</title>
      </rect>
      <text v-for="(r, i) in rows" :key="'v' + i" :x="xOf(i) + bw / 2" :y="H - PB - hOf(r) - 4"
            text-anchor="middle" class="fill-slate-600" font-size="9">{{ fmt(r.value) }}</text>
      <text v-for="(r, i) in rows" :key="'l' + i" :x="xOf(i) + bw / 2" :y="H - 8"
            text-anchor="middle" class="fill-slate-400" font-size="9">{{ r.label }}</text>
    </svg>
    <div class="text-[11px] text-slate-400 mt-1">{{ title }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  rows: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  unit: { type: String, default: '' }
})
const W = 640
const H = 200
const PL = 36
const PR = 12
const PT = 14
const PB = 26
const colors = ['#2563eb', '#60a5fa', '#f59e0b', '#10b981', '#8b5cf6', '#ef4444']
const max = computed(() => Math.max(1, ...props.rows.map((r) => Number(r.value) || 0)))
const bw = computed(() => (props.rows.length ? ((W - PL - PR) / props.rows.length) * 0.55 : 0))
const xOf = (i) => PL + (i + 0.225) * ((W - PL - PR) / Math.max(1, props.rows.length)) - bw.value / 2
const hOf = (r) => ((Number(r.value) || 0) / max.value) * (H - PT - PB)
const fmt = (v) => (v === null || v === undefined ? '—' : Number(v).toFixed(1))
</script>
