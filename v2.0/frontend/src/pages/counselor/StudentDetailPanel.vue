<template>
  <div class="card p-4 space-y-3">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h3 class="font-medium text-slate-700">{{ detail.studentName }}（{{ detail.studentNo }}）</h3>
      <div class="flex items-center gap-2">
        <span class="text-xs text-slate-400">{{ detail.majorName }} ｜ {{ detail.adminClassName }} ｜ 入学 {{ detail.enrollYear }}</span>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$emit('close')">收起</button>
      </div>
    </div>

    <div>
      <h4 class="text-sm font-medium text-slate-600 mb-2">各科情况</h4>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">教师</th>
            <th class="text-right py-2">缺课/允许</th><th class="text-right py-2">扣分</th>
            <th class="text-right py-2">加权</th><th class="text-right py-2">总评</th>
            <th class="text-left py-2">结论</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in detail.courses" :key="c.teachingClassId" class="border-b border-slate-100">
            <td class="py-2">{{ c.courseName }}</td>
            <td class="py-2 text-slate-500">{{ c.teacherName }}</td>
            <td class="py-2 text-right" :class="c.hardFail ? 'text-red-600 font-medium' : ''">
              {{ c.absentCount ?? '—' }} / {{ c.allowedAbsent ?? '—' }}
            </td>
            <td class="py-2 text-right">{{ c.deductPoints ?? '—' }}</td>
            <td class="py-2 text-right">{{ c.weightedScore ?? '—' }}</td>
            <td class="py-2 text-right" :class="c.fail ? 'text-red-600 font-medium' : ''">{{ c.totalScore ?? '未定稿' }}</td>
            <td class="py-2 text-xs">
              <span v-if="c.hardFail" class="tag bg-red-50 text-red-600">缺课超 1/3</span>
              <span v-else-if="c.fail" class="tag bg-red-50 text-red-600">分数不足</span>
              <span v-else-if="c.totalScore != null" class="tag bg-emerald-50 text-emerald-600">通过</span>
              <span v-else class="tag bg-slate-100 text-slate-500">成绩未齐</span>
              <span v-if="c.riskHint" class="text-slate-400 ml-1">{{ c.riskHint }}</span>
            </td>
          </tr>
          <tr v-if="!detail.courses.length"><td colspan="7" class="py-4 text-center text-slate-400">没有选课记录</td></tr>
        </tbody>
      </table>
    </div>

    <div>
      <h4 class="text-sm font-medium text-slate-600 mb-2">处理记录（{{ detail.interventions.length }}）</h4>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2 w-20">方式</th><th class="text-left py-2">内容</th>
            <th class="text-left py-2 w-24">结果</th><th class="text-left py-2 w-24">有效性</th>
            <th class="text-left py-2 w-40">时间</th><th class="text-right py-2 w-32">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in detail.interventions" :key="r.id" class="border-b border-slate-100">
            <td class="py-2">{{ r.actionTypeText }}</td>
            <td class="py-2 text-slate-600">{{ r.content }}</td>
            <td class="py-2 text-slate-500">{{ r.result || '—' }}</td>
            <td class="py-2" :class="r.effective === 'INEFFECTIVE' ? 'text-red-600' : 'text-slate-500'">{{ r.effectiveText }}</td>
            <td class="py-2 text-slate-500 text-xs">{{ fmt(r.createTime) }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <span v-if="r.referStatus === 'REFERRED'" class="text-xs text-brand-600">已上报教务</span>
              <template v-else-if="r.effective !== 'TERMINATED'">
                <button class="text-xs text-brand-600 hover:underline mr-2" @click="$emit('refer', r)">上报教务</button>
                <button class="text-xs text-slate-400 hover:text-red-500" @click="$emit('terminate', r)">终止</button>
              </template>
              <span v-else class="text-xs text-slate-400">已终止</span>
            </td>
          </tr>
          <tr v-if="!detail.interventions.length"><td colspan="6" class="py-4 text-center text-slate-400">还没有处理记录</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="!embedded">
      <h4 class="text-sm font-medium text-slate-600 mb-2">录入处理</h4>
      <div class="grid grid-cols-1 md:grid-cols-4 gap-2">
        <label class="text-xs">方式
          <select v-model="form.actionType" class="input mt-1">
            <option value="TALK">谈话</option>
            <option value="MEETING">班会</option>
            <option value="PARENT">联系家长</option>
            <option value="OTHER">其他</option>
          </select>
        </label>
        <label class="text-xs">处理内容
          <input v-model="form.content" class="input mt-1" placeholder="如：了解学习困难，约定每周反馈" />
        </label>
        <label class="text-xs">结果
          <input v-model="form.result" class="input mt-1" placeholder="如：学生承诺按时交作业" />
        </label>
        <label class="text-xs">有效性
          <select v-model="form.effective" class="input mt-1">
            <option value="PENDING">待复评</option>
            <option value="EFFECTIVE">有效</option>
            <option value="INEFFECTIVE">无效</option>
          </select>
        </label>
      </div>
      <button class="btn-primary text-sm mt-2" @click="save">保存处理记录</button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { counselorApi } from '../../api/counselor'

const props = defineProps({
  detail: { type: Object, required: true },
  embedded: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'refer', 'terminate', 'saved'])

const form = reactive({ actionType: 'TALK', content: '', result: '', effective: 'PENDING' })

function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }

async function save() {
  if (!form.content) return
  await counselorApi.addIntervention({
    studentId: props.detail.studentId,
    actionType: form.actionType,
    content: form.content,
    result: form.result || null,
    effective: form.effective
  })
  form.content = ''
  form.result = ''
  emit('saved')
}
</script>
