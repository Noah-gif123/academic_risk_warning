<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">覆盖率督办</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      点名覆盖率 = 已点名课次 / 已过日期课次。覆盖率低于 <b>1/3</b> 的教学班判为低置信度，需要督办；
      督办会记入审计，并同时催办该班未提交的归档批次。
    </p>

    <div class="card p-4">
      <div class="flex items-center justify-between flex-wrap gap-2 mb-3">
        <h2 class="font-medium text-slate-700">教学班体检（{{ rows.length }}）</h2>
        <div class="flex items-center gap-2">
          <label class="text-sm">学期
            <select v-model="termId" class="input mt-1" @change="load">
              <option :value="''">全部学期</option>
              <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
            </select>
          </label>
          <label class="flex items-center gap-1 text-sm text-slate-600">
            <input type="checkbox" v-model="onlyLow" /> 只看不足的
          </label>
        </div>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">教学班</th>
            <th class="text-left py-2">教师</th><th class="text-right py-2">有效课次</th>
            <th class="text-right py-2">已过课次</th><th class="text-right py-2">已点名</th>
            <th class="text-right py-2">覆盖率</th><th class="text-left py-2">结论</th>
            <th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in shown" :key="c.teachingClassId" class="border-b border-slate-100">
            <td class="py-2">{{ c.courseName }}</td>
            <td class="py-2 text-slate-500">{{ c.className }}</td>
            <td class="py-2 text-slate-500">{{ c.teacherName }}</td>
            <td class="py-2 text-right">{{ c.effectiveSessions ?? '—' }}</td>
            <td class="py-2 text-right">{{ c.passedSessions }}</td>
            <td class="py-2 text-right">{{ c.calledSessions }}</td>
            <td class="py-2 text-right font-medium"
                :class="c.lowCoverage ? 'text-red-600' : 'text-slate-600'">
              {{ c.coverageRate == null ? '—' : c.coverageRate + '%' }}
            </td>
            <td class="py-2">
              <span v-if="c.lowCoverage" class="tag bg-red-50 text-red-600">覆盖率不足，需督办</span>
              <span v-else class="tag bg-emerald-50 text-emerald-600">正常</span>
              <span v-if="c.archivePending" class="tag bg-amber-50 text-amber-600 ml-1">待归档 {{ c.archivePending }}</span>
            </td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline" @click="urge(c)">督办</button>
              <button class="text-slate-500 hover:underline ml-3" @click="$router.push('/admin/attendance')">归档工作台</button>
            </td>
          </tr>
          <tr v-if="!shown.length"><td colspan="9" class="py-6 text-center text-slate-400">没有需要督办的教学班</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminAttendanceApi } from '../../api/attendance'
import { adminPortalApi } from '../../api/adminPortal'
import { adminApi } from '../../api/admin'

const terms = ref([])
const rows = ref([])
const termId = ref('')
const onlyLow = ref(false)
const message = ref('')
const ok = ref(true)

const shown = computed(() => (onlyLow.value ? rows.value.filter((c) => c.lowCoverage) : rows.value))

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function load() {
  rows.value = await adminAttendanceApi.coverage(termId.value || undefined)
}
async function urge(c) {
  if (!window.confirm('对该教学班发起督办？会记入审计，并同时催办未提交的归档。')) return
  try {
    await adminPortalApi.urge(c.teachingClassId)
    toast('已督办：' + c.className)
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    terms.value = await adminApi.listTerms()
    const cur = terms.value.find((t) => t.current)
    if (cur) termId.value = cur.id
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
