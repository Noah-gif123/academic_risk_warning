<template>
  <div class="space-y-6">
    <section class="card p-6 flex items-start justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-semibold">Hello，{{ name }}</h1>
        <p class="text-slate-500 mt-1">欢迎来到学情预警系统 · 这里显示本学期排课、归档与督办概况</p>
      </div>
      <label class="text-sm">学期
        <select v-model="termId" class="input mt-1 w-56" @change="load">
          <option :value="''">当前学期</option>
          <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
        </select>
      </label>
    </section>

    <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>

    <section>
      <h2 class="text-base font-semibold text-slate-700 mb-3">教务工作台</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="card p-5">
          <p class="text-xs text-slate-400">学期</p>
          <p class="text-lg font-medium text-slate-800">{{ d.currentTermName || '未设置' }}</p>
          <p class="text-xs text-slate-400 mt-1">开课 {{ d.offeringCount }} · 教学班 {{ d.teachingClassCount }}</p>
        </div>
        <div class="card p-5">
          <p class="text-xs text-slate-400">在册规模</p>
          <p class="text-lg font-medium text-slate-800">{{ d.studentCount }} 学生 / {{ d.teacherCount }} 教师</p>
          <p class="text-xs text-slate-400 mt-1">课程 {{ d.courseCount }} 门</p>
        </div>
        <div class="card p-5">
          <p class="text-xs text-slate-400">考勤归档待审</p>
          <p class="text-lg font-medium" :class="d.archivePending ? 'text-amber-600' : 'text-slate-800'">{{ d.archivePending }}</p>
          <button class="text-xs text-brand-600 hover:underline mt-1" @click="$router.push('/admin/attendance')">去归档工作台</button>
        </div>
        <div class="card p-5">
          <p class="text-xs text-slate-400">覆盖率不足的教学班</p>
          <p class="text-lg font-medium" :class="d.lowCoverageCount ? 'text-red-600' : 'text-slate-800'">{{ d.lowCoverageCount }}</p>
          <button class="text-xs text-brand-600 hover:underline mt-1" @click="$router.push('/admin/coverage')">去督办</button>
        </div>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
        <div class="card p-5">
          <p class="text-xs text-slate-400">入课知会</p>
          <p class="text-lg font-medium text-slate-800">{{ d.joinNoticeCount }}</p>
          <button class="text-xs text-brand-600 hover:underline mt-1" @click="$router.push('/admin/workflow')">查看知会</button>
        </div>
        <div class="card p-5">
          <p class="text-xs text-slate-400">导入批次</p>
          <p class="text-lg font-medium text-slate-800">{{ d.importBatchCount }}</p>
          <button class="text-xs text-brand-600 hover:underline mt-1" @click="$router.push('/admin/workflow')">批次管理</button>
        </div>
        <div class="card p-5">
          <p class="text-xs text-slate-400">今日审计</p>
          <p class="text-lg font-medium text-slate-800">{{ d.auditTodayCount }}</p>
          <button class="text-xs text-brand-600 hover:underline mt-1" @click="$router.push('/admin/audit')">查看审计</button>
        </div>
        <div class="card p-5">
          <p class="text-xs text-slate-400">常用入口</p>
          <div class="flex flex-wrap gap-2 mt-1">
            <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/admin/base')">基础数据</button>
            <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/admin/offerings')">排课</button>
            <button class="text-xs text-brand-600 hover:underline" @click="$router.push('/admin/schemes')">成绩构成</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminPortalApi } from '../../api/adminPortal'
import { adminApi } from '../../api/admin'
import { session } from '../../session'

const name = computed(() => session.realName || '老师')
const terms = ref([])
const termId = ref('')
const d = ref({})
const message = ref('')
const ok = ref(true)

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
