<template>
  <section class="card p-4 space-y-3">
    <div class="flex items-center justify-between gap-2 flex-wrap">
      <div class="flex items-center gap-2">
        <h2 class="font-medium text-slate-700">干预闭环（预警到底有没有用）</h2>
        <span v-if="data?.termName" class="tag bg-slate-100 text-slate-500">{{ data.termName }}</span>
        <span v-if="data?.scopeText" class="tag bg-slate-100 text-slate-500">{{ data.scopeText }}</span>
      </div>
      <button class="btn-ghost text-xs" :disabled="loading" @click="load">
        {{ loading ? '刷新中…' : '刷新' }}
      </button>
    </div>

    <p v-if="error" class="text-xs text-red-600">{{ error }}</p>

    <div v-if="data" class="grid grid-cols-2 md:grid-cols-5 gap-3">
      <div v-for="t in tiles" :key="t.key" class="rounded-lg bg-slate-50 p-3">
        <p class="text-xs text-slate-400">{{ t.label }}</p>
        <p class="text-lg font-semibold mt-0.5" :class="t.color">{{ t.value }}</p>
        <p class="text-[11px] text-slate-400 mt-0.5 truncate">{{ t.hint }}</p>
      </div>
    </div>

    <div v-if="data" class="rounded-lg bg-brand-50/60 px-3 py-2">
      <p class="text-sm text-slate-700 leading-relaxed">{{ data.conclusion }}</p>
    </div>

    <table v-if="data?.byLevel?.length" class="w-full text-xs">
      <thead class="text-slate-500 border-b border-slate-200">
        <tr>
          <th class="text-left py-1.5">等级</th>
          <th class="text-right py-1.5">预警数</th>
          <th class="text-right py-1.5">已完成回访</th>
          <th class="text-right py-1.5">有效</th>
          <th class="text-right py-1.5">有效率</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in data.byLevel" :key="b.level" class="border-b border-slate-100">
          <td class="py-1.5">
            <span class="tag" :class="b.level === 'RED' ? 'bg-red-50 text-red-600' : (b.level === 'YELLOW' ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600')">
              {{ b.levelText }}
            </span>
          </td>
          <td class="text-right">{{ b.alerts }}</td>
          <td class="text-right">{{ b.followed }}</td>
          <td class="text-right">{{ b.effective }}</td>
          <td class="text-right font-medium">{{ b.effectiveRate }}%</td>
        </tr>
      </tbody>
    </table>

    <p class="text-[11px] text-slate-400">
      口径：覆盖率＝有干预记录的预警 ÷ 预警总数；有效率＝回访结论「有效」÷ 已完成回访；响应时长＝预警创建到首次干预；
      复发＝干预后同一门课又产生新预警。数字均由数据库聚合，可逐项复算。
    </p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { interventionMetricsApi } from '../api/ai'

const props = defineProps({
  role: { type: String, default: 'admin' }
})

const data = ref(null)
const loading = ref(false)
const error = ref('')

const tiles = computed(() => {
  const d = data.value
  if (!d) return []
  return [
    { key: 'coverage', label: '干预覆盖率', value: (d.coverageRate ?? 0) + '%', color: 'text-slate-800', hint: `${d.coveredAlerts}/${d.totalAlerts} 条预警有人介入` },
    { key: 'effective', label: '回访有效率', value: (d.effectiveRate ?? 0) + '%', color: d.effectiveRate >= 60 ? 'text-emerald-600' : 'text-amber-600', hint: `有效 ${d.effectiveCount} / 回访 ${d.followed}` },
    { key: 'response', label: '平均响应', value: d.avgResponseHours == null ? '—' : d.avgResponseHours + ' h', color: 'text-slate-800', hint: '预警创建 → 首次干预' },
    { key: 'pending', label: '待回访', value: d.pendingCount ?? 0, color: d.pendingCount > 0 ? 'text-amber-600' : 'text-slate-800', hint: '督促教务处与辅导员回访' },
    { key: 'recurrence', label: '复发', value: (d.recurrenceRate ?? 0) + '%', color: d.recurrenceCount > 0 ? 'text-red-600' : 'text-slate-800', hint: `干预后又新增 ${d.recurrenceCount} 条` }
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await interventionMetricsApi.get(props.role)
  } catch (e) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
