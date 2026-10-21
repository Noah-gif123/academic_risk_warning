<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/home')">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">入班设置</h1>
        <span class="tag bg-slate-100 text-slate-500">教学班 #{{ teachingClassId }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      学生输入 6 位邀请码申请入班。开启「入班审核」时，需要你同意后才入班；
      同意后系统给教务记一条**知会**（教务不需要审批）。
    </p>

    <div class="card p-4 space-y-3">
      <h2 class="font-medium text-slate-700">邀请码</h2>
      <div class="flex flex-wrap items-end gap-3">
        <label class="text-sm">当前邀请码
          <input v-model="code" class="input mt-1 w-40 uppercase" maxlength="6" placeholder="留空自动生成" />
        </label>
        <label class="text-sm">入班审核
          <select v-model.number="reviewEnabled" class="input mt-1 w-40">
            <option :value="0">关闭（输码即入班）</option>
            <option :value="1">开启（需我同意）</option>
          </select>
        </label>
        <button class="btn-primary text-sm" @click="saveInvite">保存</button>
      </div>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">入班申请（待审 {{ pendingCount }}）</h2>
        <label class="text-sm">状态
          <select v-model="status" class="input mt-1" @change="load">
            <option value="">全部</option>
            <option value="APPLIED">待审</option>
            <option value="APPROVED">已同意</option>
            <option value="REJECTED">已拒绝</option>
            <option value="AUTO">免审入班</option>
          </select>
        </label>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">学号</th><th class="text-left py-2">姓名</th>
            <th class="text-left py-2">申请时间</th><th class="text-left py-2">状态</th>
            <th class="text-left py-2">知会教务</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in requests" :key="r.id" class="border-b border-slate-100">
            <td class="py-2 text-slate-500">{{ r.studentNo }}</td>
            <td class="py-2">{{ r.studentName }}</td>
            <td class="py-2 text-slate-500">{{ fmt(r.applyTime) }}</td>
            <td class="py-2">
              <span class="tag" :class="statusClass(r.status)">{{ r.statusText }}</span>
              <span v-if="r.reviewNote" class="text-xs text-slate-400 ml-1">（{{ r.reviewNote }}）</span>
            </td>
            <td class="py-2 text-xs text-slate-500">{{ r.notifyAdmin ? '已记知会' : '—' }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <template v-if="r.status === 'APPLIED'">
                <button class="text-emerald-600 hover:underline mr-2" @click="review(r, true)">同意入班</button>
                <button class="text-red-600 hover:underline" @click="review(r, false)">拒绝</button>
              </template>
              <span v-else class="text-xs text-slate-400">{{ fmt(r.reviewTime) }}</span>
            </td>
          </tr>
          <tr v-if="!requests.length"><td colspan="6" class="py-6 text-center text-slate-400">没有申请</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { joinApi } from '../../api/portal'

const route = useRoute()
const teachingClassId = Number(route.query.teachingClassId || 1)

const code = ref('')
const reviewEnabled = ref(0)
const requests = ref([])
const status = ref('')
const message = ref('')
const ok = ref(true)

const pendingCount = computed(() => requests.value.filter((r) => r.status === 'APPLIED').length)

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function statusClass(s) {
  return {
    APPLIED: 'bg-amber-50 text-amber-600',
    APPROVED: 'bg-emerald-50 text-emerald-600',
    REJECTED: 'bg-red-50 text-red-600',
    AUTO: 'bg-brand-50 text-brand-600'
  }[s]
}

async function load() {
  requests.value = await joinApi.requests(teachingClassId, status.value || undefined)
}
async function saveInvite() {
  try {
    const saved = await joinApi.setInvite(teachingClassId, {
      inviteCode: code.value || null,
      joinReviewEnabled: reviewEnabled.value
    })
    code.value = saved
    toast('已保存，邀请码 ' + saved)
  } catch (e) { toast(e.message, false) }
}
async function review(r, approve) {
  const note = approve ? null : window.prompt('拒绝原因（学生可见）：', '名额已满')
  if (!approve && note === null) return
  try {
    await joinApi.review(r.id, approve, note)
    toast(approve ? '已同意入班' : '已拒绝')
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
