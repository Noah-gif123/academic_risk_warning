<template>
  <div class="space-y-5">
    <header class="flex items-start justify-between gap-4 flex-wrap">
      <div>
        <div class="flex items-center gap-3 flex-wrap">
          <button class="text-sm text-slate-400 hover:text-slate-600" @click="$router.push('/teacher/home')">← 返回课程</button>
          <h1>入班设置</h1>
          <span class="tag bg-white text-slate-500 border border-[#e6edf5]">教学班 #{{ teachingClassId }}</span>
        </div>
        <p class="text-xs text-slate-500 mt-2">学生输入 6 位邀请码申请入班；开启审核时由你确认，同意后系统给教务记一条知会。</p>
      </div>
      <button class="btn-ghost text-sm" @click="load">刷新申请</button>
    </header>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-amber-300"></span><p class="text-[11px] text-slate-400">待审核</p><p class="text-[28px] leading-none font-semibold text-amber-600 mt-3">{{ pendingCount }}</p><p class="text-[11px] text-slate-400 mt-2">需要你处理</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-emerald-300"></span><p class="text-[11px] text-slate-400">已同意</p><p class="text-[28px] leading-none font-semibold text-emerald-600 mt-3">{{ approvedCount }}</p><p class="text-[11px] text-slate-400 mt-2">已入班</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#7aa7c7]"></span><p class="text-[11px] text-slate-400">申请总数</p><p class="text-[28px] leading-none font-semibold text-[#17324b] mt-3">{{ requests.length }}</p><p class="text-[11px] text-slate-400 mt-2">当前筛选范围</p></div>
      <div class="card p-4 relative overflow-hidden"><span class="absolute left-0 top-0 bottom-0 w-1 bg-[#20b7a4]"></span><p class="text-[11px] text-slate-400">入班方式</p><p class="text-lg leading-none font-semibold text-[#17324b] mt-3">{{ reviewEnabled ? '需审核' : '免审入班' }}</p><p class="text-[11px] text-slate-400 mt-2">邀请码有效</p></div>
    </section>

    <section class="grid lg:grid-cols-[1.1fr_.9fr] gap-4">
      <div class="card p-5">
        <div class="flex items-start justify-between gap-3">
          <div><h2>邀请码</h2><p class="text-[11px] text-slate-400 mt-1">学生输入 6 位邀请码申请加入本教学班</p></div>
          <span class="tag" :class="reviewEnabled ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'">{{ reviewEnabled ? '入班审核开启' : '输码即入班' }}</span>
        </div>
        <div class="mt-4 flex items-center gap-3 flex-wrap">
          <input v-model="code" class="input !w-40 text-2xl tracking-[.25em] font-semibold uppercase" maxlength="6" placeholder="自动生成" />
          <button class="btn-ghost text-sm" @click="copyCode">{{ copied ? '已复制' : '复制邀请码' }}</button>
        </div>
        <div class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
          <label class="text-sm">入班审核
            <select v-model.number="reviewEnabled" class="input mt-1">
              <option :value="0">关闭（输码即入班）</option>
              <option :value="1">开启（需我同意）</option>
            </select>
          </label>
          <div class="flex items-end"><button class="btn-primary text-sm w-full" @click="saveInvite">保存设置</button></div>
        </div>
        <p class="text-[10px] text-slate-400 mt-3">邀请码留空保存时会自动生成。审核通过后学生进入教学班，系统给教务记一条知会，不需要教务审批。</p>
      </div>

      <div class="card p-5">
        <h2>入班说明</h2>
        <div class="mt-3 space-y-2 text-xs text-slate-600 leading-6">
          <p><b class="text-slate-700">免审入班：</b>适合公开课或补选阶段，学生输入正确邀请码即入班。</p>
          <p><b class="text-slate-700">审核入班：</b>适合有人数上限或需要确认身份的课程，拒绝原因会显示给学生。</p>
          <p><b class="text-slate-700">数据口径：</b>入班申请只记录加入教学班的请求，不改选课/成绩数据；退课仍走教务流程。</p>
        </div>
      </div>
    </section>

    <section class="card p-4">
      <div class="flex items-center justify-between flex-wrap gap-3 mb-3">
        <div><h2>入班申请</h2><p class="text-[11px] text-slate-400 mt-1">待审 {{ pendingCount }} 条 · 当前显示 {{ requests.length }} 条</p></div>
        <div class="flex items-center gap-2">
          <button v-for="s in statusTabs" :key="s.value" class="px-3 py-1.5 rounded-lg text-xs"
                  :class="status === s.value ? 'bg-[#0f9d8a] text-white' : 'bg-white text-slate-600 border border-[#e6edf5]'"
                  @click="status = s.value; load()">{{ s.label }}</button>
        </div>
      </div>
      <div class="overflow-x-auto border border-[#e6edf5] rounded-xl">
        <table>
          <thead><tr><th>学生</th><th>申请时间</th><th>状态</th><th>知会教务</th><th class="text-right">操作</th></tr></thead>
          <tbody>
            <tr v-for="r in requests" :key="r.id">
              <td><div class="flex items-center gap-2"><span class="w-7 h-7 rounded-full bg-[#e9f6f2] text-[#0b7d6e] text-xs flex items-center justify-center">{{ (r.studentName || '?').slice(0,1) }}</span><div><b class="text-xs text-slate-700">{{ r.studentName }}</b><p class="text-[10px] text-slate-400">{{ r.studentNo }}</p></div></div></td>
              <td class="text-xs text-slate-500">{{ fmt(r.applyTime) }}</td>
              <td><span class="tag" :class="statusClass(r.status)">{{ r.statusText }}</span><p v-if="r.reviewNote" class="text-[10px] text-slate-400 mt-1">{{ r.reviewNote }}</p></td>
              <td class="text-xs text-slate-500">{{ r.notifyAdmin ? '已记知会' : '—' }}</td>
              <td class="text-right whitespace-nowrap">
                <template v-if="r.status === 'APPLIED'">
                  <button class="px-2 py-1 rounded-md bg-[#e9f6f2] text-[#0b7d6e] text-xs" @click="review(r, true)">同意入班</button>
                  <button class="px-2 py-1 rounded-md bg-red-50 text-red-600 text-xs ml-1" @click="review(r, false)">拒绝</button>
                </template>
                <span v-else class="text-[10px] text-slate-400">{{ fmt(r.reviewTime) }}</span>
              </td>
            </tr>
            <tr v-if="!requests.length"><td colspan="5" class="py-10 text-center text-slate-400">当前筛选下没有入班申请</td></tr>
          </tbody>
        </table>
      </div>
    </section>
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
const copied = ref(false)

const statusTabs = [
  { value: '', label: '全部' }, { value: 'APPLIED', label: '待审' },
  { value: 'APPROVED', label: '已同意' }, { value: 'REJECTED', label: '已拒绝' }, { value: 'AUTO', label: '免审' }
]
const pendingCount = computed(() => requests.value.filter(r => r.status === 'APPLIED').length)
const approvedCount = computed(() => requests.value.filter(r => r.status === 'APPROVED').length)

function toast(msg, isOk = true) {
  message.value = msg; ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 16) : '—' }
function statusClass(s) {
  return { APPLIED: 'bg-amber-50 text-amber-600', APPROVED: 'bg-emerald-50 text-emerald-600', REJECTED: 'bg-red-50 text-red-600', AUTO: 'bg-[#e9f6f2] text-[#0b7d6e]' }[s]
}
async function copyCode() {
  if (!code.value) return
  try { await navigator.clipboard.writeText(code.value); copied.value = true; setTimeout(() => copied.value = false, 1500) }
  catch (e) { toast('复制失败，请手动选择邀请码', false) }
}
async function load() {
  try { requests.value = await joinApi.requests(teachingClassId, status.value || undefined) }
  catch (e) { toast(e.message, false) }
}
async function saveInvite() {
  try {
    const saved = await joinApi.setInvite(teachingClassId, { inviteCode: code.value || null, joinReviewEnabled: reviewEnabled.value })
    code.value = saved
    toast('已保存，邀请码 ' + saved)
  } catch (e) { toast(e.message, false) }
}
async function review(r, approve) {
  const note = approve ? null : window.prompt('拒绝原因（学生可见）：', '名额已满')
  if (!approve && note === null) return
  try { await joinApi.review(r.id, approve, note); toast(approve ? '已同意入班' : '已拒绝'); await load() }
  catch (e) { toast(e.message, false) }
}
onMounted(load)
</script>
