<template>
  <Teleport to="body">
    <div v-if="open && report" class="fixed inset-0 z-[70] bg-slate-900/45 backdrop-blur-sm flex items-center justify-center p-4 overflow-hidden" @click.self="$emit('close')">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden">
        <div class="shrink-0 px-5 py-3 border-b border-slate-200 flex items-center justify-between gap-3">
          <div class="min-w-0">
            <h2 class="font-semibold text-slate-800 truncate">{{ report.courseName }} · 学习评价</h2>
            <p class="text-xs text-slate-400 mt-0.5 truncate">{{ report.termName }} · {{ report.className }} · 报告 #{{ report.id }}</p>
          </div>
          <div class="flex gap-2 shrink-0">
            <button class="btn-ghost text-sm" @click="pdf">导出 PDF</button>
            <button class="text-slate-400 hover:text-slate-600 px-2" @click="$emit('close')">✕</button>
          </div>
        </div>
        <div class="p-5 space-y-4 overflow-y-auto">
          <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <div class="flex items-center justify-between">
              <span class="tag" :class="report.riskLevel === 'RED' ? 'bg-red-50 text-red-600' : 'bg-amber-50 text-amber-600'">{{ report.riskLevel || '—' }}</span>
              <span class="text-sm text-slate-500">总分 {{ report.totalScore ?? '—' }}</span>
            </div>
            <p class="text-sm text-slate-600 mt-2 whitespace-pre-line">{{ report.summary }}</p>
          </div>
          <div v-if="report.metrics?.dimensions?.length" class="rounded-xl border border-slate-200 p-4">
            <p class="text-sm font-medium text-slate-700 mb-2">六维画像</p>
            <div v-for="d in report.metrics.dimensions" :key="d.key" class="grid grid-cols-[64px_1fr_78px] gap-2 items-center text-xs mb-1">
              <span class="text-slate-500">{{ d.label }}</span>
              <div class="h-2 rounded-full bg-slate-100 overflow-hidden flex gap-0.5">
                <div class="h-full bg-brand-500" :style="{ width: (d.student ?? 0) + '%' }"></div>
                <div class="h-full bg-slate-300" :style="{ width: (d.classAvg ?? 0) + '%' }"></div>
              </div>
              <span class="text-right text-slate-600">{{ d.student ?? '—' }} / {{ d.classAvg ?? '—' }}</span>
            </div>
          </div>
          <div v-for="(s, i) in report.sections" :key="i" class="rounded-lg bg-slate-50 px-4 py-3">
            <p class="text-sm font-medium text-slate-700 mb-1">{{ s.title }}</p>
            <p class="text-sm text-slate-600 whitespace-pre-line leading-relaxed">{{ s.text }}</p>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
<script setup>
import { reportApi } from '../api/ai'
const props = defineProps({ open: Boolean, report: Object })
defineEmits(['close'])
async function pdf() {
  if (props.report?.id) await reportApi.pdf(props.report.id)
}
</script>
