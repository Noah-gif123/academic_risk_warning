<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div>
        <h1 class="text-xl font-semibold text-slate-800">运行轨迹</h1>
        <p class="text-xs text-slate-400 mt-1">每步角色/工具/入参/返回/门控；数字可回溯到台账来源步。</p>
      </div>
      <button class="btn-secondary" :disabled="loading" @click="loadRuns">刷新</button>
    </div>
    <div v-if="error" class="card p-3 text-sm text-red-600">{{ error }}</div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-3">
      <aside class="card p-2 lg:col-span-1 max-h-[70vh] overflow-y-auto">
        <div v-for="r in runs" :key="r.id" class="p-2 rounded cursor-pointer text-xs"
             :class="detail && detail.id === r.id ? 'bg-brand-50 text-brand-700' : 'hover:bg-slate-50'"
             @click="open(r.id)">
          <div class="flex justify-between"><b>#{{ r.id }}</b><span>{{ r.status }}</span></div>
          <div class="text-slate-400 mt-1">{{ r.createTime }} · {{ r.provider }}</div>
        </div>
        <div v-if="!runs.length" class="p-2 text-xs text-slate-400">暂无运行记录</div>
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
                <td><b class="text-brand-600">{{ l.valueText }}</b></td>
                <td class="text-slate-400">{{ l.source }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="!detail.ledger || !detail.ledger.length" class="text-xs text-slate-400">该运行没有台账记录。</p>
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
  try {
    detail.value = await agentApi.run(id)
  } catch (e) {
    error.value = e.message || String(e)
  }
}

onMounted(loadRuns)
</script>
