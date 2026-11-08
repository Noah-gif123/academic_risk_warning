<template>
  <div ref="el" class="w-full" :style="{ height: height + 'px' }" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from '../../vendor/echarts.esm.min.js'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 220 }
})
const emit = defineEmits(['chart-click'])
const el = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart) return
  chart.setOption(props.option, true)
}

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  chart.on('click', (params) => emit('chart-click', params))
  observer = new ResizeObserver(() => chart && chart.resize())
  observer.observe(el.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
  if (chart) chart.dispose()
  chart = null
})
</script>
