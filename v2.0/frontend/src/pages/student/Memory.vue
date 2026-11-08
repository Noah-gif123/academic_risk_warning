<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-semibold text-slate-800">我的画像</h1>
        <p class="text-xs text-slate-400 mt-1">摘要 ≤300 字；删除后不再注入 Agent。</p>
      </div>
      <button class="btn-secondary" :disabled="loading" @click="load">刷新</button>
    </div>
    <div v-if="error" class="card p-3 text-sm text-red-600">{{ error }}</div>

    <div v-if="data.exists" class="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <section class="card p-4 lg:col-span-2">
        <h2 class="text-sm font-medium text-slate-700 mb-2">画像摘要</h2>
        <p class="text-sm text-slate-700 leading-6">{{ data.memory.summary || '暂无摘要' }}</p>
        <div class="mt-3 flex flex-wrap gap-2 text-xs">
          <span class="rounded bg-slate-100 px-2 py-1">风险等级：{{ data.memory.recentLevels || '—' }}</span>
          <span class="rounded bg-slate-100 px-2 py-1">考勤：{{ data.memory.attendanceTag || '—' }}</span>
          <span class="rounded bg-slate-100 px-2 py-1">干预：{{ data.memory.interventionCount || 0 }} 次</span>
        </div>
        <p class="text-xs text-slate-500 mt-3" v-if="data.memory.lastConclusion">上次干预：{{ data.memory.lastConclusion }}</p>
      </section>
      <section class="card p-4">
        <h2 class="text-sm font-medium text-slate-700 mb-2">合规操作</h2>
        <p class="text-xs text-slate-500 mb-3">删除后画像摘要与事件流都会被清除，Agent 不再使用这些信息。</p>
        <button class="btn-secondary !text-red-600" :disabled="deleting" @click="remove">删除我的画像</button>
      </section>
    </div>
    <div v-else class="card p-4 text-sm text-slate-500">
      暂无画像摘要（管理员可在后台重建，或先产生风险/干预记录）。
    </div>

    <section class="card p-4" v-if="data.exists">
      <h2 class="text-sm font-medium text-slate-700 mb-2">事件流（最近 20 条）</h2>
      <div v-for="e in data.events" :key="e.id" class="flex items-start gap-2 py-1.5 border-b border-slate-100 last:border-0 text-xs">
        <span class="rounded px-1.5 py-0.5" :class="e.weight === 'HIGH' ? 'bg-brand-50 text-brand-600' : 'bg-slate-100 text-slate-500'">{{ e.type }}</span>
        <span class="text-slate-600 flex-1">{{ e.summary }}</span>
        <span class="text-slate-400">{{ e.createdAt }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { agentApi } from '../../api/agent'

const data = ref({ exists: false, memory: null, events: [] })
const loading = ref(false)
const deleting = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await agentApi.memory()
  } catch (e) {
    error.value = e.message || String(e)
  } finally {
    loading.value = false
  }
}

async function remove() {
  if (!confirm('确认删除你的画像摘要与事件流？删除后不可恢复。')) return
  deleting.value = true
  error.value = ''
  try {
    await agentApi.deleteMemory()
    await load()
  } catch (e) {
    error.value = e.message || String(e)
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>
