<template>
  <div class="rounded-xl border border-dashed border-[#dbe7f2] bg-[#fbfdff] px-4 py-8 text-center" role="status">
    <p class="text-sm text-slate-500">{{ title }}</p>
    <p v-if="hint" class="text-[11px] text-slate-400 mt-1">{{ hint }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({ title: { type: String, default: '暂无数据' }, hint: { type: String, default: '' } })
</script>
