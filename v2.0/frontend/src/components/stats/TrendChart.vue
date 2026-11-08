<template>
  <EChart :option="option" :height="230" />
</template>

<script setup>
import { computed } from 'vue'
import EChart from './EChart.vue'

const props = defineProps({ points: { type: Array, default: () => [] } })

const option = computed(() => {
  const weeks = props.points.map((p) => String(p.weekStart || '').slice(5))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['红黄占比', '新增预警'], bottom: 0, textStyle: { fontSize: 10 } },
    grid: { left: 42, right: 42, top: 18, bottom: 42 },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 12, bottom: 18 }],
    xAxis: { type: 'category', data: weeks, axisLabel: { fontSize: 9 } },
    yAxis: [
      { type: 'value', name: '%', max: 100, axisLabel: { fontSize: 9 } },
      { type: 'value', name: '件', axisLabel: { fontSize: 9 } }
    ],
    series: [
      {
        name: '红黄占比', type: 'line', smooth: true, yAxisIndex: 0,
        symbolSize: 5, itemStyle: { color: '#2563eb' },
        data: props.points.map((p) => (p.riskRatio === null || p.riskRatio === undefined ? null : Number(p.riskRatio)))
      },
      {
        name: '新增预警', type: 'bar', yAxisIndex: 1, barMaxWidth: 18,
        itemStyle: { color: '#bfdbfe' },
        data: props.points.map((p) => p.alertNew || 0)
      }
    ]
  }
})
</script>
