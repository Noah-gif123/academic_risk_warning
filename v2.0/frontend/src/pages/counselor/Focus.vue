<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-2">
        <h1 class="text-lg font-semibold text-slate-800">重点关注与班级名单</h1>
        <span class="tag bg-amber-50 text-amber-600">有风险 {{ riskyCount }} 人</span>
        <span class="tag bg-red-50 text-red-600">重点关注（≥3 门）{{ focusCount }} 人</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      本页是「本人名下行政班里有风险的学生」全量名单（有 1 门及以上风险课程即列出，按危险度排序）；
      **重点关注**只统计其中「3 门及以上风险课程」的学生，所以人数会比本页少，两者口径不同但数据同源。
    </p>

    <div class="card p-4 flex flex-wrap items-end gap-3">
      <label class="text-sm">行政班
        <select v-model="adminClassId" class="input mt-1 w-56" @change="load">
          <option :value="0">全部班级</option>
          <option v-for="c in classes" :key="c.adminClassId" :value="c.adminClassId">{{ c.className }}（{{ c.gradeYear }}级）</option>
        </select>
      </label>
      <label class="flex items-center gap-1 text-sm text-slate-600 mb-2">
        <input type="checkbox" v-model="onlyRisky" /> 只看有风险的
      </label>
      <span class="text-xs text-slate-400 mb-2">共 {{ shown.length }} 人</span>
    </div>

    <div class="card p-4">
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
            <th class="text-left py-2">班级</th><th class="text-right py-2">风险课程</th>
            <th class="text-right py-2">挂科</th><th class="text-right py-2">缺课</th>
            <th class="text-right py-2">最低总评</th><th class="text-right py-2">危险度</th>
            <th class="text-left py-2">提示</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in shown" :key="s.studentId" class="border-b border-slate-100"
              :class="active && active.studentId === s.studentId ? 'bg-brand-50/40' : ''">
            <td class="py-2 text-slate-500">{{ s.studentNo }}</td>
            <td class="py-2">{{ s.studentName }}</td>
            <td class="py-2 text-slate-500">{{ s.adminClassName }}</td>
            <td class="py-2 text-right" :class="s.riskCourseCount > 0 ? 'text-amber-600' : ''">{{ s.riskCourseCount }}</td>
            <td class="py-2 text-right" :class="s.failCourseCount > 0 ? 'text-red-600' : ''">{{ s.failCourseCount }}</td>
            <td class="py-2 text-right">{{ s.absentTotal }}</td>
            <td class="py-2 text-right">{{ s.minTotalScore ?? '—' }}</td>
            <td class="py-2 text-right font-medium">{{ s.riskScore }}</td>
            <td class="py-2 text-xs text-slate-500">{{ s.riskHint }}</td>
            <td class="py-2 text-right">
              <button class="text-brand-600 hover:underline" @click="open(s)">详情</button>
            </td>
          </tr>
          <tr v-if="!shown.length"><td colspan="10" class="py-6 text-center text-slate-400">没有学生</td></tr>
        </tbody>
      </table>
    </div>

    <StudentDetailPanel v-if="detail" :detail="detail"
                        @close="detail = null"
                        @refer="refer"
                        @terminate="terminate"
                        @saved="reloadDetail" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { counselorApi } from '../../api/counselor'
import StudentDetailPanel from './StudentDetailPanel.vue'

const classes = ref([])
const students = ref([])
const adminClassId = ref(0)
const onlyRisky = ref(true)
const detail = ref(null)
const message = ref('')
const ok = ref(true)

const shown = computed(() => (onlyRisky.value
  ? students.value.filter((s) => s.riskCourseCount > 0)
  : students.value))
/** 有风险（≥1 门）与重点关注（≥3 门）两个口径，页面顶部同时展示，避免和首页数量对不上 */
const riskyCount = computed(() => students.value.filter((s) => s.riskCourseCount > 0).length)
const focusCount = computed(() => students.value.filter((s) => s.riskCourseCount >= 3).length)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function load() {
  const o = await counselorApi.overview()
  classes.value = o.classes || []
  students.value = await counselorApi.students({ adminClassId: adminClassId.value || undefined })
}
async function open(s) { detail.value = await counselorApi.studentDetail(s.studentId) }
async function reloadDetail() {
  if (!detail.value) return
  detail.value = await counselorApi.studentDetail(detail.value.studentId)
  await load()
}
async function refer(r) {
  const note = window.prompt('上报说明（教务可见）：', '两次处理均无改善，请教务协助介入')
  if (note === null) return
  try {
    await counselorApi.refer(r.id, note)
    toast('已上报教务')
    await reloadDetail()
  } catch (e) { toast(e.message, false) }
}
async function terminate(r) {
  const reason = window.prompt('终止原因：', '学生已转专业，停止跟踪')
  if (reason === null) return
  try {
    await counselorApi.terminate(r.id, reason)
    toast('已终止干预')
    await reloadDetail()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
