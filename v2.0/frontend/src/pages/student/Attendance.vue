<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/student/home')">← 返回首页</button>
        <h1 class="text-lg font-semibold text-slate-800">我的考勤</h1>
      </div>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
    </div>

    <p class="text-xs text-slate-400">
      考勤由任课教师每次课点名产生，只读。缺课超过课程有效课次的 1/3 按学籍规定不能参加期末考核；
      请假每门课每学期前 3 次不扣分。
    </p>

    <div v-if="!items.length" class="card p-10 text-center text-slate-400 text-sm">还没有考勤记录。</div>

    <div v-for="c in items" :key="c.teachingClassId" class="card p-4 space-y-3">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 class="font-medium text-slate-800">{{ c.courseName }}</h2>
          <p class="text-xs text-slate-500">{{ c.teacherName }} ｜ 共 {{ c.totalSessions }} 次课，已点名 {{ c.calledSessions }} 次</p>
        </div>
        <div class="flex flex-wrap gap-4 text-sm">
          <span class="text-emerald-600">出勤 {{ c.presentCount }}</span>
          <span class="text-amber-600">迟到 {{ c.lateCount }}</span>
          <span class="text-red-600">缺课 {{ c.absentCount }}</span>
          <span class="text-brand-600">请假 {{ c.leaveCount }}</span>
          <span class="text-slate-500">扣分 {{ c.deductPoints }}</span>
        </div>
      </div>

      <div v-if="c.thresholdHint" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2">
        {{ c.thresholdHint }}
      </div>

      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课次</th><th class="text-left py-2">日期</th>
            <th class="text-left py-2">状态</th><th class="text-left py-2">请假类型</th>
            <th class="text-left py-2">口径</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in c.records" :key="r.id" class="border-b border-slate-100">
            <td class="py-1.5">第 {{ r.weekNo }} 周 · 第 {{ r.sessionNo }} 次</td>
            <td class="py-1.5 text-slate-500">{{ r.sessionDate }}</td>
            <td class="py-1.5" :class="statusClass(r.status)">{{ statusLabel(r.status) }}</td>
            <td class="py-1.5 text-slate-500">{{ leaveLabel(r.leaveType) }}</td>
            <td class="py-1.5 text-xs text-slate-400">{{ r.phase === 'ARCHIVE' ? '归档' : '平时' }}</td>
          </tr>
          <tr v-if="!c.records.length"><td colspan="5" class="py-4 text-center text-slate-400">还没有点名记录</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myAttendanceApi, statusLabel, statusClass } from '../../api/attendance'

const items = ref([])
const error = ref('')

function leaveLabel(t) {
  return { PERSONAL: '事假', SICK: '病假', OFFICIAL: '公假' }[t] || '—'
}

onMounted(async () => {
  try { items.value = await myAttendanceApi.mine() }
  catch (e) { error.value = e.message }
})
</script>
