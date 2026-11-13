<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-lg font-semibold text-slate-800">RAG 检索评测</h1>
        <p class="text-xs text-slate-400 mt-1">全文 / 向量 / 混合三组对比；指标来自检索评测，不消耗生成 token。</p>
      </div>
      <button class="btn-primary text-sm" :disabled="loading" @click="run">
        {{ loading ? '评测中…' : '一键重跑' }}
      </button>
    </div>

    <p v-if="error" class="text-sm text-red-500">{{ error }}</p>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div v-for="m in modes" :key="m.key" class="card p-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">{{ m.label }}</h2>
          <span class="tag" :class="m.key === 'HYBRID' ? 'bg-brand-50 text-brand-700' : 'bg-slate-100 text-slate-500'">
            {{ m.key }}
          </span>
        </div>
        <div v-if="data[m.key]" class="text-[10px] mt-1" :class="data[m.key].effective && data[m.key].effective !== m.key ? 'text-amber-600' : 'text-slate-400'">
          effective={{ data[m.key].effective || '—' }}
        </div>
        <div v-if="data[m.key]" class="grid grid-cols-2 gap-2 mt-3 text-xs">
          <div><p class="text-slate-400">Recall@K</p><b>{{ pct(data[m.key].recallAtK) }}</b></div>
          <div><p class="text-slate-400">MRR</p><b>{{ pct(data[m.key].mrr) }}</b></div>
          <div><p class="text-slate-400">NDCG@K</p><b>{{ pct(data[m.key].ndcgAtK) }}</b></div>
          <div><p class="text-slate-400">正确拒答</p><b>{{ pct(data[m.key].refuseAccuracy) }}</b></div>
          <div><p class="text-slate-400">误拒</p><b>{{ pct(data[m.key].falseRefuse) }}</b></div>
          <div><p class="text-slate-400">平均耗时</p><b>{{ data[m.key].avgMs }} ms</b></div>
        </div>
        <p v-else class="text-xs text-slate-400 mt-4">尚未运行</p>
      </div>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-2">
        <h2 class="text-sm font-medium text-slate-700">误拒 / 漏拒清单</h2>
        <div class="flex gap-2">
          <button v-for="m in modes" :key="m.key" class="text-[10px] px-2 py-0.5 rounded border border-slate-200"
                  :class="detailMode === m.key ? 'bg-brand-50 text-brand-700' : 'text-slate-500'" @click="loadDetail(m.key)">
            {{ m.label }}
          </button>
        </div>
      </div>
      <p v-if="!detail.length" class="text-xs text-slate-400">选择一种模式查看误拒/漏拒案例。</p>
      <table v-else class="w-full text-[11px]">
        <thead><tr class="text-slate-400"><th class="text-left py-1">case</th><th class="text-left">问题</th><th>可答</th><th>拒答</th><th>topScore</th><th>lex</th></tr></thead>
        <tbody>
          <tr v-for="c in detail" :key="c.caseId" class="border-t border-slate-100">
            <td class="py-1 text-slate-400">{{ c.caseId }}</td>
            <td class="py-1 text-slate-600 max-w-[360px] truncate">{{ c.question }}</td>
            <td class="text-center">{{ c.answerable ? '是' : '否' }}</td>
            <td class="text-center" :class="c.refused ? 'text-amber-600' : 'text-slate-500'">{{ c.refused ? '拒' : '答' }}</td>
            <td class="text-center">{{ c.topScore }}</td>
            <td class="text-center">{{ c.lexSupport }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card p-4">
      <h2 class="text-sm font-medium text-slate-700 mb-2">评测说明</h2>
      <p class="text-xs text-slate-500 leading-6">
        对比全文、向量、混合三种检索；样本数、可答/不可答数量随结果返回。
        “误拒”越低越好；若 VECTOR 行出现降级提示，说明向量通道不可用，该行不代表向量能力。
      </p>
      <div v-if="data.HYBRID" class="mt-2 text-[11px] text-slate-400">
        HYBRID：cases={{ data.HYBRID.cases }}，answerable={{ data.HYBRID.answerable }}，unanswerable={{ data.HYBRID.unanswerable }}
        <span v-if="data.HYBRID.note" class="text-amber-600"> · {{ data.HYBRID.note }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ragEvalApi } from '../../api/ai'

const modes = [
  { key: 'FULLTEXT', label: '全文检索' },
  { key: 'COSINE', label: '本地 TF-IDF 余弦' },
  { key: 'EMBED_COSINE', label: '本地 Embedding 余弦' },
  { key: 'VECTOR', label: 'Dify 向量' },
  { key: 'HYBRID', label: '混合检索' }
]
const data = ref({})
const loading = ref(false)
const error = ref('')
const detail = ref([])
const detailMode = ref('')

async function loadDetail(mode) {
  detailMode.value = mode
  try {
    const r = await ragEvalApi.run(mode, 4)
    detail.value = (r.detail || []).filter((c) =>
      (c.answerable && c.refused) || (!c.answerable && !c.refused)
    )
  } catch (e) {
    error.value = e?.message || '案例加载失败'
  }
}

function pct(v) {
  return v == null ? '—' : (Number(v) * 100).toFixed(1) + '%'
}

async function run() {
  loading.value = true
  error.value = ''
  try {
    data.value = await ragEvalApi.compare(4)
  } catch (e) {
    error.value = e?.message || '评测失败'
  } finally {
    loading.value = false
  }
}

onMounted(run)
</script>
