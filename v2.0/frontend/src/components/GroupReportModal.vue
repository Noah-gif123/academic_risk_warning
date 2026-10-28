<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 flex items-start justify-center bg-black/40 p-4 overflow-y-auto"
         @click.self="close">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-3xl my-8">
        <!-- 头 -->
        <div class="flex items-start justify-between gap-3 px-5 py-3 border-b border-slate-100">
          <div>
            <h2 class="font-semibold text-slate-800">{{ title || '群体学情报告' }}</h2>
            <p class="text-xs text-slate-400 mt-0.5">
              {{ scope === 'ADMIN_CLASS' ? '行政班视角（一个班 × 多门课）' : '教学班视角（一门课 × 一个班）' }}
              <span v-if="report" class="ml-2 px-1.5 py-0.5 rounded"
                    :class="report.source === 'TEMPLATE' ? 'bg-slate-100 text-slate-500' : 'bg-brand-50 text-brand-600'">
                {{ report.source === 'TEMPLATE' ? '规则生成' : 'AI 生成' }}
              </span>
            </p>
          </div>
          <div class="flex items-center gap-2">
            <button class="btn-ghost text-sm" :disabled="busy" @click="generate">
              {{ busy ? '生成中…' : (report ? '重新生成' : '生成报告') }}
            </button>
            <button class="btn-ghost text-sm" @click="close">关闭</button>
          </div>
        </div>

        <!-- 内容 -->
        <div class="px-5 py-4 space-y-3">
          <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

          <div v-if="!report && !busy" class="text-sm text-slate-400 py-10 text-center">
            还没有这份报告，点右上「生成报告」（约 3–15 秒）
          </div>

          <div v-if="busy && !report" class="text-sm text-slate-400 py-10 text-center">正在汇总事实并生成…</div>

          <template v-if="report">
            <div class="rounded-lg bg-slate-50 px-4 py-3">
              <p class="text-sm text-slate-700 leading-relaxed">{{ report.summary }}</p>
            </div>
            <div v-for="(s, i) in report.sections" :key="i" class="rounded-lg border border-slate-100 px-4 py-3">
              <p class="text-sm font-medium text-slate-700 mb-1">{{ s.title }}</p>
              <p class="text-sm text-slate-600 whitespace-pre-line leading-relaxed">{{ s.text }}</p>
            </div>
            <p class="text-xs text-slate-400">
              生成时间 {{ fmt(report.createTime) }} ｜ 报告里的数字均来自系统判定与成绩数据，可逐项对账
            </p>
          </template>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { groupReportApi } from '../api/ai'

const props = defineProps({
  open: { type: Boolean, default: false },
  scope: { type: String, default: 'CLASS' },
  targetId: { type: [Number, String], default: null },
  title: { type: String, default: '' }
})
const emit = defineEmits(['close'])

const report = ref(null)
const busy = ref(false)
const error = ref('')

function fmt(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

function close() {
  emit('close')
}

async function loadLatest() {
  report.value = null
  error.value = ''
  if (!props.targetId) return
  try {
    report.value = await groupReportApi.latest(props.scope, props.targetId)
  } catch (e) {
    error.value = e?.message || '读取失败'
  }
}

async function generate() {
  if (!props.targetId || busy.value) return
  busy.value = true
  error.value = ''
  try {
    report.value = await groupReportApi.generate(props.scope, props.targetId)
  } catch (e) {
    error.value = e?.message || '生成失败'
  } finally {
    busy.value = false
  }
}

watch(
  () => [props.open, props.targetId, props.scope],
  ([isOpen]) => {
    if (isOpen) {
      loadLatest()
    }
  },
  { immediate: true }
)
</script>
