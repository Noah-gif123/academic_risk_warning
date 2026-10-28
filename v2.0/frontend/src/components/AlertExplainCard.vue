<template>
  <div class="border border-slate-200 rounded-lg p-3 space-y-2">
    <div class="flex items-center justify-between gap-2">
      <div class="flex items-center gap-2 flex-wrap">
        <p class="font-medium text-slate-700">预警解释卡</p>
        <span v-if="data" class="tag bg-slate-100 text-slate-500">{{ data.triggerTypeText }}</span>
        <span v-if="data" class="tag"
              :class="data.source === 'TEMPLATE' ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-600'">
          {{ data.source === 'TEMPLATE' ? '规则生成' : 'AI 生成' }}
        </span>
      </div>
      <button class="btn-ghost text-xs" :disabled="busy" @click="generate">
        {{ busy ? '生成中…' : (data ? '重新生成' : '生成解释') }}
      </button>
    </div>

    <p v-if="error" class="text-xs text-red-600">{{ error }}</p>

    <p v-if="!data && !busy" class="text-xs text-slate-400">
      还没有解释卡。点右上「生成解释」，系统会把这张预警的触发原因、量化差距与三条例行动作写清楚（约 3–10 秒）。
    </p>

    <template v-if="data">
      <div v-for="(s, i) in data.sections" :key="i" class="rounded-lg bg-slate-50 px-3 py-2">
        <p class="text-xs font-medium text-slate-600 mb-0.5">{{ s.title }}</p>
        <p class="text-sm text-slate-600 whitespace-pre-line leading-relaxed">{{ s.text }}</p>
      </div>
      <div v-if="data.actions?.length" class="flex flex-wrap gap-2 pt-1">
        <span v-for="(a, i) in data.actions" :key="i"
              class="text-xs px-2 py-1 rounded-lg bg-brand-50 text-brand-700 border border-brand-100">
          {{ i + 1 }}. {{ a.label }}
        </span>
      </div>
      <p class="text-[11px] text-slate-400">
        生成时间 {{ fmt(data.createTime) }} ｜ 数字均来自系统判定与成绩数据，可与「判定依据」逐项对照
      </p>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { alertExplainApi } from '../api/ai'

const props = defineProps({
  alertId: { type: [Number, String], default: null },
  role: { type: String, default: 'teacher' }
})

const data = ref(null)
const busy = ref(false)
const error = ref('')

function fmt(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

async function load() {
  data.value = null
  error.value = ''
  if (!props.alertId) return
  try {
    data.value = await alertExplainApi.latest(props.alertId, props.role)
  } catch (e) {
    error.value = e?.message || '读取失败'
  }
}

async function generate() {
  if (!props.alertId || busy.value) return
  busy.value = true
  error.value = ''
  try {
    data.value = await alertExplainApi.generate(props.alertId, props.role)
  } catch (e) {
    error.value = e?.message || '生成失败'
  } finally {
    busy.value = false
  }
}

watch(() => props.alertId, load, { immediate: true })
</script>
