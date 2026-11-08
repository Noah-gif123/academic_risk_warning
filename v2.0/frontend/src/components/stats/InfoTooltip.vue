<template>
  <span class="inline-flex items-center gap-1">
    <slot />
    <span class="relative inline-flex">
      <button type="button" class="w-4 h-4 rounded-full bg-slate-200 text-slate-600 text-[10px] leading-none
                   flex items-center justify-center hover:bg-brand-100" @click="open = !open">?</button>
      <span v-if="open" class="absolute right-0 top-6 z-30 w-80 card p-3 text-xs text-slate-600 space-y-1
                   shadow-lg text-left">
        <b class="text-slate-800">{{ title }}</b>
        <p>{{ definition }}</p>
        <pre v-if="sql" class="bg-slate-50 rounded p-2 text-[10px] overflow-x-auto whitespace-pre-wrap">{{ sql }}</pre>
      </span>
    </span>
  </span>
</template>

<script setup>
import { ref } from 'vue'
defineProps({
  title: { type: String, required: true },
  definition: { type: String, default: '' },
  sql: { type: String, default: '' }
})
const open = ref(false)
</script>
