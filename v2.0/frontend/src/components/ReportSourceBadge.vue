<template>
  <span v-if="source" class="tag" :class="sourceClass" :title="titleText">
    {{ sourceText }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  source: { type: String, default: '' },
  model: { type: String, default: '' },
  promptVersion: { type: String, default: '' }
})
const sourceText = computed(() => {
  if (props.source === 'DIFY') return 'Dify 生成'
  if (props.source === 'LLM') return 'AI 生成'
  if (props.source === 'TEMPLATE') return '规则汇总'
  return props.source
})
const sourceClass = computed(() => props.source === 'TEMPLATE'
  ? 'bg-slate-100 text-slate-500'
  : 'bg-[#e9f6f2] text-[#0b7d6e]')
const titleText = computed(() => [props.model, props.promptVersion ? 'Prompt ' + props.promptVersion : ''].filter(Boolean).join(' · '))
</script>
