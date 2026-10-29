<template>
  <div class="space-y-6">
    <section class="card p-6 flex items-start justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Hello，{{ name }}</h1>
        <p class="text-slate-500 mt-1">欢迎来到学情预警系统 · 这里是本人名下行政班的看板</p>
      </div>
      <label class="text-sm">学期
        <select v-model="termId" class="input mt-1 w-56" @change="load">
          <option :value="''">当前学期</option>
          <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
        </select>
      </label>
    </section>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 md:grid-cols-5 gap-4">
      <div class="card p-4">
        <p class="text-xs text-slate-400">教学周</p>
        <p class="text-lg font-medium text-slate-800">第 {{ o.weekNo || '—' }} 周</p>
        <p class="text-xs text-slate-400">{{ o.termName || '—' }}</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">名下行政班 / 学生</p>
        <p class="text-lg font-medium text-slate-800">{{ o.classCount }} / {{ o.studentCount }}</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">有风险学生</p>
        <p class="text-lg font-medium text-amber-600">{{ o.riskStudentCount }}</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">挂科 / 缺课超门槛</p>
        <p class="text-lg font-medium text-red-600">{{ o.failStudentCount }} / {{ o.hardFailStudentCount }}</p>
      </div>
      <div class="card p-4">
        <p class="text-xs text-slate-400">平均挂科率</p>
        <p class="text-lg font-medium text-slate-800">{{ o.avgFailRate == null ? '—' : o.avgFailRate + '%' }}</p>
        <p class="text-xs text-slate-400">退课不计入比例</p>
      </div>
    </section>

    <section class="card p-4">
      <h2 class="font-medium text-slate-700 mb-2">汇总提醒（一次一条，覆盖名下全部行政班）</h2>
      <ul class="list-disc pl-6 text-sm text-slate-600 space-y-1">
        <li v-for="(r, i) in (o.reminders || [])" :key="i">{{ r }}</li>
      </ul>
      <p class="text-xs text-slate-400 mt-2">
        说明：班集体提醒从第 5 周起生效；预警等级由第二阶段引擎产出，本阶段按「挂科 / 缺课超 1/3」等确定性指标估算。
      </p>
    </section>
    <!-- 干预闭环度量（M10） -->
    <InterventionMetricsCard role="counselor" />

    <section>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-base font-semibold text-slate-700">名下行政班（{{ (o.classes || []).length }}）</h2>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/counselor/classes')">班级管理</button>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="c in (o.classes || [])" :key="c.adminClassId" class="card p-4 space-y-2">
          <div class="flex items-center justify-between">
            <h3 class="font-medium text-slate-800">{{ classLabel(c) }}</h3>
            <span class="tag" :class="alertClass(c.classAlertLevel)">
              {{ c.classAlertLevel === 'NONE' ? '正常' : (c.classAlertLevel === 'SUGGEST' ? '建议班会' : '重点班级') }}
            </span>
          </div>
          <p class="text-xs text-slate-500">{{ c.majorName }} ｜ {{ c.gradeYear }} 级 ｜ {{ c.studentCount }} 人</p>
          <p class="text-xs text-slate-400">
            已出分 {{ c.scoredCount }} ｜ 挂科 {{ c.failCount }} ｜ 挂科率 {{ c.failRate == null ? '—' : c.failRate + '%' }}
            ｜ 缺课超门槛 {{ c.absentOverCount }}
          </p>
          <p class="text-xs" :class="c.classAlertLevel === 'NONE' ? 'text-slate-400' : 'text-amber-600'">{{ c.classAlertText }}</p>
          <button class="text-xs text-brand-600 hover:underline"
                  @click="$router.push({ path: '/counselor/classes', query: { adminClassId: c.adminClassId } })">查看名单</button>
        </div>
        <p v-if="!(o.classes || []).length" class="text-sm text-slate-400">
          还没有分配行政班，请联系教务在「基础数据」里分配导员。
        </p>
      </div>
    </section>

    <section>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-base font-semibold text-slate-700">
          重点关注名单（{{ (o.focusStudents || []).length }}）
          <span class="text-xs font-normal text-slate-400 ml-2">
            同一学生有 3 门及以上课程处于风险状态才进这张表；有 1~2 门风险的共 {{ riskyCount }} 人，可在「重点关注」页查看
          </span>
        </h2>
        <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/counselor/focus')">全部名单</button>
      </div>
      <div class="card p-4">
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200">
            <tr>
              <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
              <th class="text-left py-2">班级</th><th class="text-right py-2">风险课程</th>
              <th class="text-right py-2">挂科</th><th class="text-right py-2">最低总评</th>
              <th class="text-left py-2">提示</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in (o.focusStudents || [])" :key="s.studentId" class="border-b border-slate-100">
              <td class="py-2 text-slate-500">{{ s.studentNo }}</td>
              <td class="py-2">{{ s.studentName }}</td>
              <td class="py-2 text-slate-500">{{ s.adminClassName }}</td>
              <td class="py-2 text-right">{{ s.riskCourseCount }}</td>
              <td class="py-2 text-right text-red-600">{{ s.failCourseCount }}</td>
              <td class="py-2 text-right">{{ s.minTotalScore ?? '—' }}</td>
              <td class="py-2 text-xs text-slate-500">{{ s.riskHint }}</td>
            </tr>
            <tr v-if="!(o.focusStudents || []).length">
              <td colspan="7" class="py-6 text-center text-slate-400">没有达到重点关注标准的学生</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import InterventionMetricsCard from '../../components/InterventionMetricsCard.vue'
import { computed, onMounted, ref } from 'vue'
import { counselorApi } from '../../api/counselor'
import { session } from '../../session'

const name = computed(() => session.realName || '老师')
const terms = ref([])
const termId = ref('')
const o = ref({ classes: [], focusStudents: [], reminders: [] })
const message = ref('')
const ok = ref(true)

/** 有风险学生（1 门及以上风险课程）总数，与「重点关注（≥3 门）」区分 */
const riskyCount = computed(() => o.value.riskStudentCount || 0)

/** 行政班显示名：带上年级，避免「计算机1班」重名 */
function classLabel(c) {
  if (!c) return ''
  return c.gradeYear ? c.className + '（' + c.gradeYear + '级）' : c.className
}

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function alertClass(level) {
  return {
    NONE: 'bg-emerald-50 text-emerald-600',
    SUGGEST: 'bg-amber-50 text-amber-600',
    FOCUS: 'bg-red-50 text-red-600'
  }[level]
}

async function load() {
  try { o.value = await counselorApi.overview(termId.value || undefined) }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    terms.value = await counselorApi.terms()
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
