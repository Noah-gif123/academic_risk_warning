<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">成绩构成</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      成绩构成由课程组任课教师共同确定并确认后自动锁定；教务只做审批与「未达成一致」时的裁定锁定，不直接改教师的评分项。
      考勤不计分，只通过缺课硬门槛与违纪扣分影响结果。
    </p>

    <!-- 模板 -->
    <div class="card p-4">
      <div class="flex items-center justify-between mb-2">
        <h2 class="font-medium text-slate-700">构成模板</h2>
        <span class="text-xs text-slate-400">教师新建成绩构成时可直接套用</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">编码</th><th class="text-left py-2">名称</th>
            <th class="text-left py-2">课程类型</th><th class="text-left py-2">层级</th>
            <th class="text-left py-2">默认</th><th class="text-left py-2">说明</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in templates" :key="t.id" class="border-b border-slate-100">
            <td class="py-2">{{ t.templateCode }}</td>
            <td class="py-2">{{ t.templateName }}</td>
            <td class="py-2 text-slate-500">{{ courseTypeLabel(t.courseType) }}</td>
            <td class="py-2 text-slate-500">{{ t.level }}</td>
            <td class="py-2">{{ t.isDefault === 1 ? '默认' : '—' }}</td>
            <td class="py-2 text-slate-500">{{ t.description || '—' }}</td>
          </tr>
          <tr v-if="!templates.length"><td colspan="6" class="py-6 text-center text-slate-400">暂无模板</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 构成列表 -->
    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">各课程成绩构成</h2>
        <label class="text-sm">学期
          <select v-model="termId" class="input mt-1" @change="loadSchemes">
            <option :value="''">全部学期</option>
            <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
          </select>
        </label>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">学期</th>
            <th class="text-right py-2">版本</th><th class="text-right py-2">一级权重合计</th>
            <th class="text-left py-2">确认进度</th><th class="text-left py-2">状态</th>
            <th class="text-left py-2">锁定方式</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in schemes" :key="s.id" class="border-b border-slate-100"
              :class="active && active.id === s.id ? 'bg-brand-50/40' : ''">
            <td class="py-2">{{ s.courseName }} <span class="tag bg-slate-100 text-slate-500 ml-1">{{ courseTypeLabel(s.courseType) }}</span></td>
            <td class="py-2 text-slate-500">{{ s.termName }}</td>
            <td class="py-2 text-right">v{{ s.version }}</td>
            <td class="py-2 text-right" :class="Number(s.totalWeight) === 100 ? 'text-emerald-600' : 'text-red-600'">{{ s.totalWeight }}</td>
            <td class="py-2">{{ s.confirmDone }} / {{ s.teacherCount }}</td>
            <td class="py-2">
              <span class="tag" :class="s.locked ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'">
                {{ statusText(s) }}
              </span>
            </td>
            <td class="py-2 text-slate-500">{{ s.lockType || '—' }}<span v-if="s.lockedByName"> · {{ s.lockedByName }}</span></td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline mr-3" @click="showItems(s)">查看构成</button>
              <button v-if="!s.locked" class="text-amber-600 hover:underline" @click="doLock(s)">裁定锁定</button>
            </td>
          </tr>
          <tr v-if="!schemes.length"><td colspan="8" class="py-6 text-center text-slate-400">暂无成绩构成（可由任课教师在课程内创建）</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 构成明细 -->
    <div class="card p-4" v-if="active">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">{{ active.courseName }} · v{{ active.version }} 的评分项</h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="active = null">收起</button>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">评分项</th><th class="text-left py-2">分类</th>
            <th class="text-right py-2">权重(%)</th><th class="text-left py-2">数据来源</th>
            <th class="text-right py-2">预计次数</th><th class="text-left py-2">标记</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="r in active.items" :key="r.id">
            <tr class="border-b border-slate-100 bg-slate-50/60">
              <td class="py-2 font-medium">{{ r.itemName }}</td>
              <td class="py-2 text-slate-500">{{ r.category }}</td>
              <td class="py-2 text-right font-medium">{{ r.weight }}</td>
              <td class="py-2 text-slate-500">{{ r.dataSource === 'AUTO' ? '自动汇总' : '手工录入' }}</td>
              <td class="py-2 text-right text-slate-400">—</td>
              <td class="py-2 text-slate-500">{{ flag(r) }}</td>
            </tr>
            <tr v-for="c in (r.children || [])" :key="c.id" class="border-b border-slate-100">
              <td class="py-1.5 pl-6 text-slate-600">↳ {{ c.itemName }}</td>
              <td class="py-1.5 text-slate-500">{{ c.category }}</td>
              <td class="py-1.5 text-right">{{ c.weight }}</td>
              <td class="py-1.5 text-slate-500">{{ c.dataSource === 'AUTO' ? '自动汇总' : '手工录入' }}</td>
              <td class="py-1.5 text-right text-slate-500">{{ c.expectedTimes || '—' }}</td>
              <td class="py-1.5 text-slate-500">{{ flag(c) }}</td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/admin'
import { courseTypeLabel } from '../../labels'
import { adminScoreApi } from '../../api/score'

const message = ref('')
const ok = ref(true)
const terms = ref([])
const templates = ref([])
const schemes = ref([])
const active = ref(null)
const termId = ref('')

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function statusText(s) {
  const m = { DRAFT: '草稿', CONFIRMING: '确认中', CONFIRMED: '已确认', LOCKED: '已锁定' }
  return m[s.confirmStatus] || s.confirmStatus
}
function flag(i) {
  const out = []
  if (i.isAnchor === 1) out.push('锚点' + (i.anchorLayer ? '（' + i.anchorLayer + '）' : ''))
  if (i.isSubjective === 1) out.push('主观')
  return out.join(' · ') || '—'
}

async function loadSchemes() {
  schemes.value = await adminScoreApi.schemes(termId.value || undefined)
  if (active.value) {
    active.value = schemes.value.find((s) => s.id === active.value.id) || null
  }
}
function showItems(s) { active.value = s }
async function doLock(s) {
  try {
    await adminScoreApi.lock(s.id)
    toast('已裁定锁定：' + s.courseName + ' v' + s.version)
    await loadSchemes()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    terms.value = await adminApi.listTerms()
    const cur = terms.value.find((t) => t.current)
    if (cur) termId.value = cur.id
    templates.value = await adminScoreApi.templates()
    await loadSchemes()
  } catch (e) { toast(e.message, false) }
})
</script>
