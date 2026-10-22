<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">干预记录</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      干预流程：录入处理 → 14 天或下次评估复评 → 两次仍无改善可上报教务 → 教务处理后回填结果；始终无改善可终止并留痕。
    </p>

    <div class="card p-4 flex flex-wrap items-end gap-3">
      <label class="text-sm">行政班
        <select v-model="adminClassId" class="input mt-1 w-56" @change="load">
          <option :value="0">全部班级</option>
          <option v-for="c in classes" :key="c.adminClassId" :value="c.adminClassId">{{ c.className }}（{{ c.gradeYear }}级）</option>
        </select>
      </label>
      <label class="text-xs mb-2 text-slate-500">共 {{ rows.length }} 条记录</label>
    </div>

    <div class="card p-4">
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">学生</th><th class="text-left py-2 w-20">方式</th>
            <th class="text-left py-2">内容</th><th class="text-left py-2 w-32">结果</th>
            <th class="text-left py-2 w-24">有效性</th><th class="text-left py-2 w-28">上报</th>
            <th class="text-left py-2 w-36">时间</th><th class="text-right py-2 w-32">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id" class="border-b border-slate-100">
            <td class="py-2">{{ r.studentNo }} · {{ r.studentName }}</td>
            <td class="py-2">{{ r.actionTypeText }}</td>
            <td class="py-2 text-slate-600 max-w-xs truncate" :title="r.content">{{ r.content }}</td>
            <td class="py-2 text-slate-500">{{ r.result || '—' }}</td>
            <td class="py-2" :class="r.effective === 'INEFFECTIVE' ? 'text-red-600' : (r.effective === 'TERMINATED' ? 'text-slate-400' : 'text-slate-600')">
              {{ r.effectiveText }}
            </td>
            <td class="py-2 text-xs">
              <span v-if="r.referStatus === 'REFERRED'" class="tag bg-brand-50 text-brand-600">已上报</span>
              <span v-else class="text-slate-400">—</span>
            </td>
            <td class="py-2 text-slate-500 text-xs">{{ fmt(r.createTime) }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <template v-if="r.referStatus !== 'REFERRED' && r.effective !== 'TERMINATED'">
                <button class="text-xs text-brand-600 hover:underline mr-2" @click="refer(r)">上报</button>
                <button class="text-xs text-slate-400 hover:text-red-500" @click="terminate(r)">终止</button>
              </template>
              <span v-else class="text-xs text-slate-400">已归档</span>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="8" class="py-6 text-center text-slate-400">还没有干预记录</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { counselorApi } from '../../api/counselor'

const classes = ref([])
const rows = ref([])
const adminClassId = ref(0)
const message = ref('')
const ok = ref(true)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }

async function load() {
  rows.value = await counselorApi.interventionRecords(adminClassId.value || undefined)
}
async function refer(r) {
  const note = window.prompt('上报说明（教务可见）：', '两次处理均无改善，请教务协助介入')
  if (note === null) return
  try { await counselorApi.refer(r.id, note); toast('已上报教务'); await load() }
  catch (e) { toast(e.message, false) }
}
async function terminate(r) {
  const reason = window.prompt('终止原因：', '学生已转专业，停止跟踪')
  if (reason === null) return
  try { await counselorApi.terminate(r.id, reason); toast('已终止干预'); await load() }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    const o = await counselorApi.overview()
    classes.value = o.classes || []
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
