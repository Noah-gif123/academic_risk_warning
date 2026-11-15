<template>
  <div class="space-y-4">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap"><h1>问卷发布</h1><span class="tag bg-white text-slate-500 border border-[#e6edf5]">匿名作答</span></div>
        <p class="text-xs text-slate-500 mt-2">按行政班发布评教、满意度、心理健康等问卷；结果由教务汇总分发。</p>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </header>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span><p class="text-[11px] text-slate-400">已发布</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ list.length }}</p><p class="text-[11px] text-slate-400 mt-2">我的问卷</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">已答</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ totalAnswers }}</p><p class="text-[11px] text-slate-400 mt-2">全部问卷</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-300"></span><p class="text-[11px] text-slate-400">进行中</p><p class="text-[28px] leading-none font-semibold text-amber-600 mt-3">{{ activeCount }}</p><p class="text-[11px] text-slate-400 mt-2">可继续作答</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-indigo-300"></span><p class="text-[11px] text-slate-400">匿名问卷</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ anonymousCount }}</p><p class="text-[11px] text-slate-400 mt-2">保护学生隐私</p></div>
    </section>

    <div class="card p-5 space-y-3">
      <h2 class="font-medium text-slate-700">发布新问卷</h2>
      <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
        <label class="text-sm">行政班
          <select v-model="form.adminClassId" class="input mt-1">
            <option :value="''">请选择</option>
            <option v-for="c in classes" :key="c.adminClassId" :value="c.adminClassId">{{ c.className }}（{{ c.gradeYear }}级）</option>
          </select>
        </label>
        <label class="text-sm">关联课程（开课）
          <select v-model="form.offeringId" class="input mt-1">
            <option :value="''">请选择</option>
            <option v-for="o in offerings" :key="o.offeringId" :value="o.offeringId">
              {{ o.courseName }}（{{ o.teacherName }}）
            </option>
          </select>
        </label>
        <label class="text-sm">问卷类型
          <select v-model="form.questionnaireType" class="input mt-1">
            <option value="TEACHER_STYLE">教师风格判定（评教）</option>
            <option value="SATISFACTION">学生满意度调查</option>
            <option value="MENTAL_HEALTH">心理健康普查</option>
            <option value="GRADUATE_SURVEY">毕业生去向调研</option>
            <option value="NATIONAL_SURVEY">全国本科生学情调查</option>
          </select>
        </label>
        <label class="text-sm md:col-span-2">标题（可留空按类型自动取名）
          <input v-model="form.title" class="input mt-1" placeholder="如 2026-2027-1 教师风格判定" />
        </label>
      </div>
      <button class="btn-primary text-sm" @click="publish">发布（匿名）</button>
    </div>

    <div class="card p-5">
      <h2 class="font-medium text-slate-700 mb-3">我的问卷（{{ list.length }}）</h2>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">标题</th><th class="text-left py-2 w-28">类型</th>
            <th class="text-left py-2 w-32">课程</th><th class="text-left py-2 w-32">行政班</th>
            <th class="text-left py-2 w-20">匿名</th><th class="text-left py-2 w-24">状态</th>
            <th class="text-right py-2 w-20">已答</th><th class="text-left py-2 w-36">发布时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in list" :key="q.id" class="border-b border-slate-100">
            <td class="py-2">{{ q.title }}</td>
            <td class="py-2 text-slate-500">{{ q.questionnaireTypeText }}</td>
            <td class="py-2 text-slate-500">{{ q.courseName || '—' }}</td>
            <td class="py-2 text-slate-500">{{ q.adminClassName || '—' }}</td>
            <td class="py-2">
              <span class="tag" :class="q.anonymous ? 'bg-brand-50 text-brand-600' : 'bg-slate-100 text-slate-500'">
                {{ q.anonymous ? '匿名' : '实名' }}
              </span>
            </td>
            <td class="py-2">
              <span class="tag bg-amber-50 text-amber-600">{{ q.statusText }}</span>
            </td>
            <td class="py-2 text-right">{{ q.answerCount }}</td>
            <td class="py-2 text-slate-500 text-xs">{{ fmt(q.createTime) }}</td>
          </tr>
          <tr v-if="!list.length"><td colspan="8" class="py-6 text-center text-slate-400">
            还没有发布问卷。作答与汇总属第二阶段能力，本阶段先完成发布与流转留痕。
          </td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { counselorApi } from '../../api/counselor'

const classes = ref([])
const offerings = ref([])
const list = ref([])
const message = ref('')
const ok = ref(true)
const totalAnswers = computed(() => list.value.reduce((n, q) => n + Number(q.answerCount || 0), 0))
const activeCount = computed(() => list.value.filter(q => q.statusText === '进行中' || q.status === 'ACTIVE').length)
const anonymousCount = computed(() => list.value.filter(q => q.anonymous).length)

const form = reactive({ adminClassId: '', offeringId: '', title: '', questionnaireType: 'TEACHER_STYLE' })

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }

async function load() {
  list.value = await counselorApi.questionnaires()
}
async function publish() {
  if (!form.adminClassId) { toast('请选择行政班', false); return }
  if (!form.offeringId) { toast('请选择评价的课程（开课）', false); return }
  try {
    await counselorApi.publishQuestionnaire({
      adminClassId: Number(form.adminClassId),
      offeringId: Number(form.offeringId),
      title: form.title || null,
      questionnaireType: form.questionnaireType
    })
    toast('问卷已发布（匿名）')
    form.title = ''
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    const o = await counselorApi.overview()
    classes.value = o.classes || []
    offerings.value = await counselorApi.offeringOptions()
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
