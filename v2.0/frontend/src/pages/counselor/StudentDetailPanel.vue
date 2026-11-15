<template>
  <div class="fixed inset-0 z-40 bg-slate-900/30 flex justify-end" @click.self="$emit('close')">
    <div class="w-full max-w-3xl h-full bg-white shadow-xl overflow-y-auto">
      <div class="sticky top-0 z-10 bg-white border-b border-slate-200 px-5 py-3 flex items-center justify-between gap-3">
        <div class="flex items-center gap-2 flex-wrap min-w-0">
          <h3 class="font-medium text-slate-700 truncate">{{ detail.studentName }}（{{ detail.studentNo }}）</h3>
          <span class="text-xs text-slate-400">{{ detail.majorName }} ｜ {{ detail.adminClassName }} ｜ 入学 {{ detail.enrollYear }}</span>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-primary text-sm" @click="$emit('report', detail)">生成学期汇总报告</button>
          <button class="text-slate-400 hover:text-slate-600 text-lg" title="关闭" @click="$emit('close')">✕</button>
        </div>
      </div>

      <div class="p-5 space-y-4">
        <section class="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3"><p class="text-[10px] text-slate-400">课程</p><b class="text-2xl text-[#17324b]">{{ detail.courses.length }}</b></div>
          <div class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3"><p class="text-[10px] text-slate-400">风险课程</p><b class="text-2xl text-red-600">{{ riskCount }}</b></div>
          <div class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3"><p class="text-[10px] text-slate-400">累计缺课</p><b class="text-2xl text-slate-700">{{ absentTotal }}</b></div>
          <div class="rounded-xl border border-[#e6edf5] bg-[#fbfdff] p-3"><p class="text-[10px] text-slate-400">平均总评</p><b class="text-2xl text-slate-700">{{ avgScore }}</b></div>
        </section>

        <section class="rounded-xl border border-[#e6edf5] p-4">
          <div class="flex items-center justify-between mb-2"><h4 class="text-sm font-bold text-[#17324b]">学期课程概览</h4><span class="text-[10px] text-slate-400">{{ detail.termName }}</span></div>
          <div class="overflow-x-auto">
            <table><thead><tr><th>课程</th><th>教师</th><th class="text-right">缺课/允许</th><th class="text-right">总评</th><th>结论</th></tr></thead><tbody>
              <tr v-for="c in detail.courses" :key="c.teachingClassId"><td>{{ c.courseName }}</td><td class="text-slate-500">{{ c.teacherName }}</td><td class="text-right" :class="c.hardFail ? 'text-red-600 font-medium' : ''">{{ c.absentCount ?? 0 }} / {{ c.allowedAbsent ?? 0 }}</td><td class="text-right" :class="c.fail ? 'text-red-600 font-medium' : ''">{{ c.totalScore ?? '未定稿' }}</td><td><span class="tag" :class="c.fail ? 'bg-red-50 text-red-600' : (c.totalScore != null ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500')">{{ c.failReasonText || (c.totalScore != null ? '通过' : '成绩未齐') }}</span></td></tr>
              <tr v-if="!detail.courses.length"><td colspan="5" class="py-6 text-center text-slate-400">没有选课记录</td></tr>
            </tbody></table>
          </div>
        </section>

        <section class="rounded-xl border border-[#e6edf5] p-4">
          <div class="flex items-center justify-between mb-2"><h4 class="text-sm font-bold text-[#17324b]">处理记录（{{ detail.interventions.length }}）</h4></div>
          <div v-if="detail.interventions.length" class="space-y-2">
            <div v-for="r in detail.interventions" :key="r.id" class="rounded-lg bg-[#f8fbfd] p-2.5">
              <div class="flex justify-between gap-2"><span class="text-[10px] text-slate-400">{{ fmt(r.createTime) }} · {{ r.actionTypeText }}</span><span class="tag" :class="r.effective === 'INEFFECTIVE' ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'">{{ r.effectiveText }}</span></div>
              <p class="text-xs text-slate-600 mt-1">{{ r.content }}</p>
              <div class="flex gap-2 justify-end mt-1" v-if="r.referStatus !== 'REFERRED' && r.effective !== 'TERMINATED'">
                <button class="text-[10px] text-[#0f9d8a]" @click="$emit('refer', r)">上报教务</button>
                <button class="text-[10px] text-slate-400" @click="$emit('terminate', r)">终止</button>
              </div>
            </div>
          </div>
          <p v-else class="text-xs text-slate-400 py-4">还没有处理记录。</p>
        </section>

        <section class="rounded-xl border border-[#e6edf5] p-4">
          <h4 class="text-sm font-bold text-[#17324b] mb-2">录入处理</h4>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-2">
            <label class="text-xs">方式<select v-model="form.actionType" class="input mt-1"><option value="TALK">谈话</option><option value="MEETING">班会</option><option value="PARENT">联系家长</option><option value="OTHER">其他</option></select></label>
            <label class="text-xs">处理内容<input v-model="form.content" class="input mt-1" placeholder="了解学习困难…" /></label>
            <label class="text-xs">结果<input v-model="form.result" class="input mt-1" placeholder="学生承诺…" /></label>
            <label class="text-xs">有效性<select v-model="form.effective" class="input mt-1"><option value="PENDING">待复评</option><option value="EFFECTIVE">有效</option><option value="INEFFECTIVE">无效</option></select></label>
          </div>
          <button class="btn-primary text-sm mt-2" @click="save">保存处理记录</button>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { counselorApi } from '../../api/counselor'

const props = defineProps({ detail: { type: Object, required: true }, embedded: { type: Boolean, default: false } })
const emit = defineEmits(['close', 'refer', 'terminate', 'saved', 'report'])
const form = reactive({ actionType: 'TALK', content: '', result: '', effective: 'PENDING' })
const riskCount = computed(() => props.detail.courses.filter(c => c.fail || ['RED', 'ORANGE', 'YELLOW'].includes(c.riskLevel)).length)
const absentTotal = computed(() => props.detail.courses.reduce((n, c) => n + Number(c.absentCount || 0), 0))
const avgScore = computed(() => {
  const vals = props.detail.courses.map(c => Number(c.totalScore)).filter(Number.isFinite)
  return vals.length ? (vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(1) : '—'
})
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
async function save() {
  if (!form.content) return
  await counselorApi.addIntervention({ studentId: props.detail.studentId, actionType: form.actionType, content: form.content, result: form.result || null, effective: form.effective })
  form.content = ''; form.result = ''
  emit('saved')
}
</script>
