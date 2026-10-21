<template>
  <div class="card p-10 text-center">
    <p class="text-slate-500 text-sm">该页面属于第一阶段后续任务，尚未实现</p>
    <p class="text-xs text-slate-400 mt-2">{{ route.meta.title }} · {{ route.path }}</p>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
