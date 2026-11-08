<template>
  <EChart :option="option" :height="270" @chart-click="onClick" />
</template>

<script setup>
import { computed } from 'vue'
import EChart from './EChart.vue'

const props = defineProps({ points: { type: Array, default: () => [] } })
const emit = defineEmits(['point-click'])

const option = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (p) => {
      const raw = p.data
      return `${raw.label}<br/>挂科率 ${raw.value[0]}%<br/>课程因子 ${raw.value[1]}`
    }
  },
  grid: { left: 48, right: 24, top: 18, bottom: 40 },
  xAxis: { type: 'value', name: '挂科率 %', nameLocation: 'middle', nameGap: 24, min: 0, axisLabel: { fontSize: 9 } },
  yAxis: { type: 'value', name: '因子', min: 0.9, max: 1.2, axisLabel: { fontSize: 9 } },
  series: [{
    type: 'scatter',
    symbolSize: 10,
    itemStyle: { color: '#2563eb', opacity: 0.8 },
    label: {
      show: true, position: 'right', fontSize: 8, color: '#475569',
      formatter: (p) => p.data.short
    },
    markLine: {
      silent: true, symbol: 'none',
      lineStyle: { type: 'dashed', color: '#94a3b8' },
      data: [{ yAxis: 1.0, label: { formatter: 'factor=1.00', fontSize: 9 } }]
    },
    data: props.points.map((p) => ({ value: [p.x, p.y], label: p.label, short: p.short }))
  }]
}))

function onClick(params) {
  if (params && params.data) emit('point-click', params.data)
}
</script>
