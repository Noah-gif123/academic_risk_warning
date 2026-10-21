<template>
  <div class="fixed right-5 bottom-6 z-50 flex flex-col items-end gap-2 pointer-events-none">
    <transition-group name="toast">
      <div v-for="t in toasts" :key="t.id"
           class="pointer-events-auto max-w-sm px-4 py-3 rounded-xl shadow-lg border text-sm flex items-start gap-2"
           :class="t.type === 'error'
             ? 'bg-red-50 border-red-200 text-red-700'
             : 'bg-emerald-50 border-emerald-200 text-emerald-700'">
        <span class="mt-[2px]">{{ t.type === 'error' ? '⚠' : '✓' }}</span>
        <span class="flex-1 whitespace-pre-line">{{ t.text }}</span>
        <button class="text-current/60 hover:opacity-70" @click="dismiss(t.id)">×</button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { toasts } from '../toast'

function dismiss(id) {
  const i = toasts.findIndex((t) => t.id === id)
  if (i >= 0) toasts.splice(i, 1)
}
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.18s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
