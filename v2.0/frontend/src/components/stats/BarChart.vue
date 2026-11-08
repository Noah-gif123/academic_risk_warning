<template>
  <EChart :option="option" :height="210" @chart-click="onClick" />
</template>

<script setup>
import { computed } from 'vue'
import EChart from './EChart.vue'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  title: { type: String, default: '' },
  unit: { type: String, default: '' }
})
const emit = defineEmits(['row-click'])

const colors = ['#2563eb', '#60a5fa', '#f59e0b', '#10b981', '#8b5cf6', '#ef4444']
const option = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (p) => `${p[0].name}<br/>${props.title}: ${p[0].value}${props.unit}`
  },
  grid: { left: 42, right: 16, top: 18, bottom: 34 },
  xAxis: { type: 'category', data: props.rows.map((r) => r.label), axisLabel: { fontSize: 9, interval: 0, rotate: 20 } },
  yAxis: { type: 'value', name: props.unit, axisLabel: { fontSize: 9 } },
  series: [{
    type: 'bar', barMaxWidth: 28,
    data: props.rows.map((r, i) => ({
      value: r.value,
      itemStyle: { color: colors[i % colors.length], borderRadius: [3, 3, 0, 0] }
    })),
    label: { show: true, position: 'top', fontSize: 9, formatter: (p) => p.value }
  }]
}))

function onClick(params) {
  if (params && params.name) emit('row-click', params.name)
}
</script>
