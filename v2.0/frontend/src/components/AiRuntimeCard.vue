<template>
  <section class="card p-5">
    <div class="flex items-center justify-between gap-2 flex-wrap">
      <div><h2 class="text-sm font-semibold text-[#17324b]">AI 运行状态</h2><p class="text-[11px] text-slate-400 mt-1">真实模型、降级与今日调用量；失败不会影响报告页面。</p></div>
      <button class="btn-ghost text-xs" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新' }}</button>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-2 mt-3">
      <div class="rounded-lg bg-[#f8fbfd] p-2.5"><p class="text-[10px] text-slate-400">总开关</p><b class="text-sm" :class="data.enabled ? 'text-emerald-600' : 'text-slate-500'">{{ data.enabled ? '启用' : '关闭' }}</b></div>
      <div class="rounded-lg bg-[#f8fbfd] p-2.5"><p class="text-[10px] text-slate-400">直连模型</p><b class="text-sm" :class="data.directReady ? 'text-emerald-600' : 'text-amber-600'">{{ data.directReady ? '就绪' : '未配置' }}</b></div>
      <div class="rounded-lg bg-[#f8fbfd] p-2.5"><p class="text-[10px] text-slate-400">今日调用</p><b class="text-sm text-slate-700">{{ usage.todayCalls ?? '—' }}</b></div>
      <div class="rounded-lg bg-[#f8fbfd] p-2.5"><p class="text-[10px] text-slate-400">模板降级</p><b class="text-sm" :class="usage.templateFallback ? 'text-amber-600' : 'text-slate-700'">{{ usage.templateFallback ?? '—' }}</b></div>
    </div>
    <div class="flex flex-wrap gap-1.5 mt-3">
      <span v-for="(v, k) in data.scenes || {}" :key="k" class="tag" :class="v === 'TEMPLATE' ? 'bg-slate-100 text-slate-500' : 'bg-[#e9f6f2] text-[#0b7d6e]'">{{ k }}：{{ v }}</span>
    </div>
    <p v-if="error" class="text-[11px] text-red-500 mt-2">{{ error }}</p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { aiApi } from '../api/ai'
const data = ref({})
const loading = ref(false)
const error = ref('')
const usage = computed(() => data.value.usage || {})
async function load() {
  loading.value = true
  error.value = ''
  try { data.value = await aiApi.health() }
  catch (e) { error.value = e.message || 'AI 状态读取失败' }
  finally { loading.value = false }
}
onMounted(load)
</script>
