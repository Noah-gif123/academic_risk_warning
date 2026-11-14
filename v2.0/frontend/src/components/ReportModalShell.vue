<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-[70] bg-slate-900/45 backdrop-blur-sm flex items-center justify-center p-4 overflow-hidden" @click.self="$emit('close')">
      <div class="bg-white rounded-2xl shadow-2xl w-full flex flex-col overflow-hidden"
           :class="wide ? 'max-w-[1200px] max-h-[92vh]' : 'max-w-4xl max-h-[90vh]'">
        <div class="shrink-0 px-5 py-3 border-b border-slate-200 flex items-center justify-between gap-3">
          <div class="min-w-0">
            <h2 class="font-semibold text-slate-800 truncate">{{ title }}</h2>
            <p v-if="subtitle" class="text-xs text-slate-400 mt-0.5 truncate">{{ subtitle }}</p>
          </div>
          <div class="flex gap-2 shrink-0">
            <slot name="actions" />
            <button class="text-slate-400 hover:text-slate-600 px-2" title="关闭" @click="$emit('close')">✕</button>
          </div>
        </div>
        <div class="p-5 overflow-y-auto">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({ open: Boolean, title: String, subtitle: String, wide: Boolean })
defineEmits(['close'])
</script>
