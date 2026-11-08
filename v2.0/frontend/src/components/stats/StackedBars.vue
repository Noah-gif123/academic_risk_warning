<template>
  <EChart :option="option" :height="height" @chart-click="onClick" />
</template>

<script setup>
import { computed } from 'vue'
import EChart from './EChart.vue'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  height: { type: Number, default: 280 }
})
const emit = defineEmits(['row-click'])

const option = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: ['红', '橙', '黄', '绿'], bottom: 0, textStyle: { fontSize: 10 } },
  grid: { left: 96, right: 30, top: 12, bottom: 34 },
  xAxis: { type: 'value', axisLabel: { fontSize: 9 } },
  yAxis: { type: 'category', data: props.rows.map((r) => r.label), inverse: true, axisLabel: { fontSize: 9 } },
  series: [
    { name: '红', type: 'bar', stack: 'total', itemStyle: { color: '#ef4444' }, data: props.rows.map((r) => r.red || 0) },
    { name: '橙', type: 'bar', stack: 'total', itemStyle: { color: '#f97316' }, data: props.rows.map((r) => r.orange || 0) },
    { name: '黄', type: 'bar', stack: 'total', itemStyle: { color: '#eab308' }, data: props.rows.map((r) => r.yellow || 0) },
    { name: '绿', type: 'bar', stack: 'total', itemStyle: { color: '#22c55e' }, data: props.rows.map((r) => r.green || 0) }
  ]
}))

function onClick(params) {
  if (params && params.name) emit('row-click', params.name)
}
</script>
