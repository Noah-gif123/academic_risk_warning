<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/student/home')">← 返回首页</button>
        <h1 class="text-lg font-semibold text-slate-800">我的成绩</h1>
      </div>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
    </div>

    <p class="text-xs text-slate-400">
      总评 = 加权分 − 考勤扣分；缺课超过有效课次的 1/3 直接不能参加期末考核。
      这里只展示你本人的分项与总评。
    </p>

    <div v-if="!items.length" class="card p-10 text-center text-slate-400 text-sm">还没有成绩记录。</div>

    <div v-for="c in items" :key="c.teachingClassId" class="card p-4 space-y-3">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 class="font-medium text-slate-800">{{ c.courseName }}</h2>
          <p class="text-xs text-slate-500">{{ c.termName }} ｜ {{ c.teacherName }}</p>
        </div>
        <div class="flex flex-wrap items-center gap-4 text-sm">
          <span class="text-slate-500">加权分 {{ c.weightedScore }}</span>
          <span class="text-red-600">扣分 {{ c.deductionPoints }}</span>
          <span class="font-medium" :class="c.fail ? 'text-red-600' : 'text-emerald-600'">总评 {{ c.totalScore }}</span>
          <span class="tag" :class="c.scoreStatusText === '已定稿' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
            {{ c.scoreStatusText }}
          </span>
        </div>
      </div>

      <div v-if="c.hardFail" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2">
        缺课 {{ c.absentCount }} 次，已超过允许的 {{ c.allowedAbsent }} 次，按学籍规定不能参加期末考核。
      </div>
      <div v-else-if="c.fail" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-2">
        {{ c.failReasonText }}（总评 {{ c.totalScore }}，及格线 {{ c.passScore }}）
      </div>

      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">评分项</th><th class="text-right py-2">权重</th>
            <th class="text-right py-2">得分（百分制）</th><th class="text-right py-2">计入总评</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(it, i) in c.items" :key="i" class="border-b border-slate-100">
            <td class="py-1.5">{{ it.path }}</td>
            <td class="py-1.5 text-right text-slate-500">{{ it.weight }}%</td>
            <td class="py-1.5 text-right">
              <span :class="it.missing ? 'text-red-500' : ''">{{ it.missing || it.score == null ? '未录入' : Number(it.score).toFixed(1) }}</span>
            </td>
            <td class="py-1.5 text-right font-medium">{{ it.weightPercent == null ? '—' : it.weightPercent }}</td>
          </tr>
        </tbody>
      </table>

      <p class="text-xs text-slate-400">
        出勤情况：缺课 {{ c.absentCount }} 次、迟到 {{ c.lateCount }} 次、请假 {{ c.leaveCount }} 次
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myScoreApi } from '../../api/scoreMatrix'

const items = ref([])
const error = ref('')

onMounted(async () => {
  try { items.value = await myScoreApi.mine() }
  catch (e) { error.value = e.message }
})
</script>
