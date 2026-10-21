<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">班级管理</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">按行政班查看挂科率与预警分布；退课与休学学生不计入比例。</p>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div v-for="c in classes" :key="c.adminClassId" class="card p-4 space-y-2 cursor-pointer"
           :class="adminClassId === c.adminClassId ? 'ring-2 ring-brand-200' : ''"
           @click="pick(c.adminClassId)">
        <div class="flex items-center justify-between">
          <h2 class="font-medium text-slate-800">{{ classLabel(c) }}</h2>
          <span class="tag" :class="alertClass(c.classAlertLevel)">
            {{ c.classAlertLevel === 'NONE' ? '正常' : (c.classAlertLevel === 'SUGGEST' ? '建议班会' : '重点班级') }}
          </span>
        </div>
        <p class="text-xs text-slate-500">{{ c.majorName }} ｜ {{ c.gradeYear }} 级 ｜ {{ c.studentCount }} 人</p>
        <p class="text-xs text-slate-400">
          已出分 {{ c.scoredCount }} ｜ 挂科 {{ c.failCount }} 人 ｜ 挂科率 {{ c.failRate == null ? '—' : c.failRate + '%' }}
          ｜ 硬门槛 {{ c.hardFailCount }}
        </p>
        <p class="text-xs" :class="c.classAlertLevel === 'NONE' ? 'text-slate-400' : 'text-amber-600'">{{ c.classAlertText }}</p>
      </div>
    </div>

    <div class="card p-4" v-if="students.length">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">学生名单（{{ students.length }}）</h2>
        <label class="flex items-center gap-1 text-sm text-slate-600">
          <input type="checkbox" v-model="onlyRisky" /> 只看有风险的
        </label>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
            <th class="text-right py-2">风险课程</th><th class="text-right py-2">挂科</th>
            <th class="text-right py-2">缺课</th><th class="text-right py-2">平均总评</th>
            <th class="text-left py-2">最近处理</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in shownStudents" :key="s.studentId" class="border-b border-slate-100">
            <td class="py-2 text-slate-500">{{ s.studentNo }}</td>
            <td class="py-2">{{ s.studentName }}</td>
            <td class="py-2 text-right" :class="s.riskCourseCount > 0 ? 'text-amber-600' : ''">{{ s.riskCourseCount }}</td>
            <td class="py-2 text-right" :class="s.failCourseCount > 0 ? 'text-red-600' : ''">{{ s.failCourseCount }}</td>
            <td class="py-2 text-right">{{ s.absentTotal }}</td>
            <td class="py-2 text-right">{{ s.avgTotalScore ?? '—' }}</td>
            <td class="py-2 text-xs text-slate-500">{{ s.latestInterventionText || '—' }}</td>
            <td class="py-2 text-right">
              <button class="text-brand-600 hover:underline" @click="open(s)">详情</button>
            </td>
          </tr>
          <tr v-if="!shownStudents.length"><td colspan="8" class="py-6 text-center text-slate-400">没有学生</td></tr>
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
import { useRoute } from 'vue-router'
import { counselorApi } from '../../api/counselor'
import StudentDetailPanel from './StudentDetailPanel.vue'

const route = useRoute()
const classes = ref([])
const students = ref([])
const adminClassId = ref(Number(route.query.adminClassId || 0))
const onlyRisky = ref(false)
const detail = ref(null)
const message = ref('')
const ok = ref(true)

const shownStudents = computed(() => (onlyRisky.value
  ? students.value.filter((s) => s.riskCourseCount > 0)
  : students.value))

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function classLabel(c) {
  if (!c) return ''
  return c.gradeYear ? c.className + '（' + c.gradeYear + '级）' : c.className
}
function alertClass(level) {
  return {
    NONE: 'bg-emerald-50 text-emerald-600',
    SUGGEST: 'bg-amber-50 text-amber-600',
    FOCUS: 'bg-red-50 text-red-600'
  }[level]
}

async function load() {
  const o = await counselorApi.overview()
  classes.value = o.classes || []
  if (!adminClassId.value && classes.value.length) {
    adminClassId.value = 0
  }
  students.value = await counselorApi.students({ adminClassId: adminClassId.value || undefined })
}
async function pick(id) {
  adminClassId.value = id
  await load()
}
async function open(s) {
  try {
    detail.value = await counselorApi.studentDetail(s.studentId)
  } catch (e) {
    toast('打开学生详情失败：' + e.message, false)
  }
}
async function reloadDetail() {
  if (!detail.value) return
  detail.value = await counselorApi.studentDetail(detail.value.studentId)
  await load()
}
async function refer(r) {
  const note = window.prompt('上报说明（教务可见）：', '两次处理均无改善，请教务协助介入')
  if (note === null) return
  try { await counselorApi.refer(r.id, note); toast('已上报教务'); await reloadDetail() }
  catch (e) { toast(e.message, false) }
}
async function terminate(r) {
  const reason = window.prompt('终止原因：', '学生已转专业，停止跟踪')
  if (reason === null) return
  try { await counselorApi.terminate(r.id, reason); toast('已终止干预'); await reloadDetail() }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
