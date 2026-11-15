<template>
  <div class="space-y-4">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap"><h1>运行轨迹</h1><span class="tag bg-white text-slate-500 border border-[#e6edf5]">Agent 执行记录</span></div>
        <p class="text-xs text-slate-500 mt-2">查看每一步角色、工具、入参、返回与门控；数字可回溯到台账来源步。</p>
      </div>
      <button class="btn-ghost text-sm" :disabled="loading" @click="loadRuns">{{ loading ? '刷新中…' : '刷新' }}</button>
    </header>

    <div v-if="error" class="card p-3 text-sm text-red-600">{{ error }}</div>

    <section class="grid grid-cols-2 lg:grid-cols-3 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">最近运行</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ runs.length }}</p><p class="text-[11px] text-slate-400 mt-2">最多展示 20 条</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-emerald-300"></span><p class="text-[11px] text-slate-400">已完成</p><p class="text-[28px] leading-none font-semibold text-emerald-600 mt-3">{{ runs.filter(r => r.status === 'DONE' || r.status === 'PUBLISHED').length }}</p><p class="text-[11px] text-slate-400 mt-2">成功落库</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-300"></span><p class="text-[11px] text-slate-400">当前步骤</p><p class="text-[28px] leading-none font-semibold text-amber-600 mt-3">{{ detail?.steps?.length || 0 }}</p><p class="text-[11px] text-slate-400 mt-2">run #{{ detail?.id || '—' }}</p></div>
    </section>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-3">
      <aside class="card p-2 lg:col-span-1 max-h-[70vh] overflow-y-auto">
        <div v-for="r in runs" :key="r.id" class="p-3 rounded-xl cursor-pointer text-xs mb-1 border border-transparent"
             :class="detail && detail.id === r.id ? 'bg-[#f0faf7] border-[#b9e8df] text-[#0b7d6e]' : 'hover:bg-slate-50'"
             @click="open(r.id)">
          <div class="flex justify-between gap-2"><b>#{{ r.id }}</b><span class="tag" :class="r.status === 'DONE' || r.status === 'PUBLISHED' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">{{ r.status }}</span></div>
          <div class="text-slate-400 mt-1">{{ r.createTime }} · {{ r.provider }}</div>
        </div>
        <div v-if="!runs.length" class="p-4 text-xs text-slate-400 text-center">暂无运行记录</div>
      </aside>

      <section class="lg:col-span-3 space-y-3" v-if="detail">
        <div class="card p-4 text-xs text-slate-600 grid grid-cols-2 lg:grid-cols-4 gap-2">
          <div>runId：<b>{{ detail.id }}</b></div>
          <div>provider：{{ detail.provider }}</div>
          <div>role_count：{{ detail.roleCount }}</div>
          <div>状态：{{ detail.status }}</div>
          <div>开始：{{ detail.createTime }}</div>
          <div>结束：{{ detail.finishTime }}</div>
          <div>runKey：{{ detail.runKey }}</div>
        </div>

        <div class="card p-4 overflow-x-auto">
          <h2 class="text-sm font-medium text-slate-700 mb-2">步骤</h2>
          <table class="w-full text-xs">
            <thead class="text-slate-400"><tr>
              <th class="text-left py-1">#</th><th class="text-left">角色</th><th class="text-left">工具</th>
              <th class="text-left">门控</th><th class="text-left">耗时</th><th class="text-left">返回摘要</th>
            </tr></thead>
            <tbody>
              <tr v-for="s in detail.steps" :key="s.stepNo + '-' + s.stepRole" class="border-t border-slate-100">
                <td class="py-1">{{ s.stepNo }}</td>
                <td>{{ s.stepRole || s.name }}</td>
                <td>{{ s.toolName || '—' }}</td>
                <td><span :class="s.gateStatus === 'PASS' ? 'text-green-600' : 'text-amber-600'">{{ s.gateStatus }}</span></td>
                <td>{{ s.durationMs }}ms</td>
                <td class="text-slate-400">{{ s.outputDigest }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="card p-4 overflow-x-auto">
          <h2 class="text-sm font-medium text-slate-700 mb-2">证据台账（数字 → 来源步）</h2>
          <table class="w-full text-xs">
            <thead class="text-slate-400"><tr>
              <th class="text-left py-1">步</th><th class="text-left">工具</th><th class="text-left">指标</th>
              <th class="text-left">值</th><th class="text-left">来源</th>
            </tr></thead>
            <tbody>
              <tr v-for="(l, i) in detail.ledger" :key="i" class="border-t border-slate-100">
                <td class="py-1">{{ l.stepNo }}</td>
                <td>{{ l.tool }}</td>
                <td>{{ l.metric }}</td>
                <td>
                  <button v-if="/chunkId/i.test(l.metric)" class="text-brand-600 underline" @click="openChunk(l.valueText)">{{ l.valueText }}</button>
                  <b v-else class="text-brand-600">{{ l.valueText }}</b>
                </td>
                <td class="text-slate-400">{{ l.source }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="!detail.ledger || !detail.ledger.length" class="text-xs text-slate-400">该运行没有台账记录。</p>
          <div v-if="chunk" class="mt-3 rounded-lg bg-slate-50 p-3 text-xs text-slate-600">
            <div class="flex justify-between mb-1">
              <b>{{ chunk.docName }} 第 {{ chunk.sectionNo }} 段</b>
              <button class="text-slate-400" @click="chunk = null">关闭</button>
            </div>
            <p class="whitespace-pre-wrap leading-5">{{ chunk.content }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { agentApi } from '../api/agent'

const runs = ref([])
const detail = ref(null)
const loading = ref(false)
const error = ref('')
const chunk = ref(null)

async function loadRuns() {
  loading.value = true
  error.value = ''
  try {
    runs.value = await agentApi.runs(20)
    const firstDone = runs.value.find((r) => r.status === 'DONE') || runs.value[0]
    if (firstDone) await open(firstDone.id)
  } catch (e) {
    error.value = e.message || String(e)
  } finally {
    loading.value = false
  }
}

async function open(id) {
  error.value = ''
  chunk.value = null
  try {
    detail.value = await agentApi.run(id)
  } catch (e) {
    error.value = e.message || String(e)
  }
}

async function openChunk(id) {
  try {
    chunk.value = await agentApi.chunk(id)
  } catch (e) {
    error.value = e.message || String(e)
  }
}

onMounted(loadRuns)
</script>
