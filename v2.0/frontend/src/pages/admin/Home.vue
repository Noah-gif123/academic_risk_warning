<template>
  <div class="space-y-4">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <h1>教务工作台</h1>
          <span class="tag bg-white text-slate-500 border border-[#e6edf5]">学期运行</span>
          <span class="tag bg-[#e9f6f2] text-[#0b7d6e]">{{ d.currentTermName || '当前学期' }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2">排课、考勤归档、覆盖督办和流程留痕，从这里统一处理。</p>
      </div>
      <label class="text-sm">学期
        <select v-model="termId" class="input mt-1 w-56" @change="load">
          <option :value="''">当前学期</option>
          <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
        </select>
      </label>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 md:grid-cols-6 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span><p class="text-[11px] text-slate-400">开课 / 教学班</p><p class="text-[26px] leading-none font-semibold text-[#17324b] mt-3">{{ d.offeringCount }}<span class="text-sm font-normal text-slate-400"> / {{ d.teachingClassCount }}</span></p><p class="text-[11px] text-slate-400 mt-2">本学期运行中</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">在册学生</p><p class="text-[26px] leading-none font-semibold text-[#17324b] mt-3">{{ d.studentCount }}</p><p class="text-[11px] text-slate-400 mt-2">课程 {{ d.courseCount }} 门</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-indigo-300"></span><p class="text-[11px] text-slate-400">教师</p><p class="text-[26px] leading-none font-semibold text-[#17324b] mt-3">{{ d.teacherCount }}</p><p class="text-[11px] text-slate-400 mt-2">在任任课教师</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-300"></span><p class="text-[11px] text-slate-400">待审归档</p><p class="text-[26px] leading-none font-semibold text-amber-600 mt-3">{{ d.archivePending || 0 }}</p><p class="text-[11px] text-slate-400 mt-2">考勤归档待处理</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-red-300"></span><p class="text-[11px] text-slate-400">低覆盖教学班</p><p class="text-[26px] leading-none font-semibold text-red-600 mt-3">{{ d.lowCoverageCount || 0 }}</p><p class="text-[11px] text-slate-400 mt-2">需要督办</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-slate-300"></span><p class="text-[11px] text-slate-400">今日审计</p><p class="text-[26px] leading-none font-semibold text-[#17324b] mt-3">{{ d.auditTodayCount || 0 }}</p><p class="text-[11px] text-slate-400 mt-2">关键操作留痕</p></div>
    </section>

    <section class="card p-5">
      <div class="flex items-center justify-between gap-3 flex-wrap">
        <div><h2 class="text-sm font-semibold text-[#17324b]">常用工作入口</h2><p class="text-[11px] text-slate-400 mt-1">按学期运行顺序排列，避免在多个页面之间来回找。</p></div>
        <span class="text-[11px] text-slate-400">教务权限</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-2 mt-3">
        <button v-for="e in entries" :key="e.path" class="text-left rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3 hover:border-[#9fe3d8] hover:bg-[#f5fcf9]" @click="$router.push(e.path)">
          <span class="text-lg">{{ e.icon }}</span>
          <p class="text-xs font-medium text-slate-700 mt-1">{{ e.label }}</p>
          <p class="text-[10px] text-slate-400 mt-1">{{ e.desc }}</p>
        </button>
      </div>
    </section>

    <div class="grid grid-cols-1 xl:grid-cols-3 gap-3">
      <section class="xl:col-span-2 card p-5">
        <div class="flex items-center justify-between"><h2 class="text-sm font-semibold text-[#17324b]">流程提醒</h2><button class="text-[11px] text-[#0f9d8a]" @click="$router.push('/admin/workflow')">查看全部</button></div>
        <div class="grid grid-cols-2 gap-3 mt-3">
          <button class="rounded-xl bg-[#fbfdff] border border-[#e6edf5] p-3 text-left" @click="$router.push('/admin/workflow')"><p class="text-[10px] text-slate-400">入课知会</p><p class="text-xl font-semibold text-slate-700">{{ d.joinNoticeCount || 0 }}</p><p class="text-[10px] text-slate-400">教师同意入课后自动产生</p></button>
          <button class="rounded-xl bg-[#fbfdff] border border-[#e6edf5] p-3 text-left" @click="$router.push('/admin/workflow')"><p class="text-[10px] text-slate-400">导入批次</p><p class="text-xl font-semibold text-slate-700">{{ d.importBatchCount || 0 }}</p><p class="text-[10px] text-slate-400">回滚与异常留痕</p></button>
          <button class="rounded-xl bg-[#fbfdff] border border-[#e6edf5] p-3 text-left" @click="$router.push('/admin/attendance')"><p class="text-[10px] text-slate-400">待审归档</p><p class="text-xl font-semibold text-amber-600">{{ d.archivePending || 0 }}</p><p class="text-[10px] text-slate-400">确认后冻结考勤</p></button>
          <button class="rounded-xl bg-[#fbfdff] border border-[#e6edf5] p-3 text-left" @click="$router.push('/admin/coverage')"><p class="text-[10px] text-slate-400">覆盖率督办</p><p class="text-xl font-semibold text-red-600">{{ d.lowCoverageCount || 0 }}</p><p class="text-[10px] text-slate-400">低于 1/3 需催办</p></button>
        </div>
      </section>
      <section class="card p-5">
        <h2 class="text-sm font-semibold text-[#17324b]">当前学期</h2>
        <p class="text-[11px] text-slate-400 mt-2">{{ d.currentTermName || '—' }}</p>
        <div class="mt-4 space-y-2 text-xs text-slate-600">
          <p class="flex justify-between"><span class="text-slate-400">课程</span><b>{{ d.courseCount || 0 }}</b></p>
          <p class="flex justify-between"><span class="text-slate-400">教学班</span><b>{{ d.teachingClassCount || 0 }}</b></p>
          <p class="flex justify-between"><span class="text-slate-400">学生</span><b>{{ d.studentCount || 0 }}</b></p>
          <p class="flex justify-between"><span class="text-slate-400">教师</span><b>{{ d.teacherCount || 0 }}</b></p>
        </div>
      </section>
    </div>

    <InterventionMetricsCard role="admin" />
    <AiRuntimeCard />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminPortalApi } from '../../api/adminPortal'
import { adminApi } from '../../api/admin'
import InterventionMetricsCard from '../../components/InterventionMetricsCard.vue'
import AiRuntimeCard from '../../components/AiRuntimeCard.vue'

const terms = ref([])
const termId = ref('')
const d = ref({})
const message = ref('')
const ok = ref(true)
const entries = [
  { icon: '🗂️', label: '基础数据', desc: '学期 / 课程 / 师生', path: '/admin/base' },
  { icon: '🗓️', label: '排课与教学班', desc: '开课与教学班', path: '/admin/offerings' },
  { icon: '🧮', label: '成绩构成', desc: '模板与审批', path: '/admin/schemes' },
  { icon: '📋', label: '考勤归档', desc: '审核与冻结', path: '/admin/attendance' },
  { icon: '📡', label: '规则配置', desc: '判定口径版本', path: '/admin/rules' },
  { icon: '🔁', label: '流程与问卷', desc: '知会 / 导入 / 审计', path: '/admin/workflow' }
]

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
async function load() {
  try { d.value = await adminPortalApi.dashboard(termId.value || undefined) }
  catch (e) { toast(e.message, false) }
}
onMounted(async () => {
  try {
    terms.value = await adminApi.listTerms()
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
