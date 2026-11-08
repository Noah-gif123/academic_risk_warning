<template>
  <EChart :option="option" :height="250" />
</template>

<script setup>
import { computed } from 'vue'
import EChart from './EChart.vue'

const props = defineProps({ stages: { type: Array, default: () => [] } })
const option = computed(() => ({
  tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>${p.value}` },
  series: [{
    type: 'funnel',
    left: '8%', right: '8%', top: 12, bottom: 12,
    minSize: '30%', maxSize: '100%',
    sort: 'descending', gap: 2,
    label: { show: true, position: 'inside', fontSize: 11, formatter: (p) => `${p.name} ${p.value}` },
    itemStyle: { borderColor: '#fff', borderWidth: 1 },
    emphasis: { label: { fontSize: 13 } },
    data: props.stages.map((s) => ({ name: s.name, value: s.value }))
  }]
}))
</script>
