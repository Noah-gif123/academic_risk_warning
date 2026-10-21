<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">审计日志</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      改分、改考勤、归档与退回、成绩构成锁定、项目评分、导入回滚等关键操作都会留痕；考勤还有行级留痕表。
    </p>

    <div class="card p-4">
      <div class="grid grid-cols-1 md:grid-cols-6 gap-3">
        <label class="text-sm">业务类型
          <select v-model="filters.bizType" class="input mt-1">
            <option value="">全部</option>
            <option v-for="t in bizTypes" :key="t" :value="t">{{ t }}</option>
          </select>
        </label>
        <label class="text-sm">动作
          <select v-model="filters.action" class="input mt-1">
            <option value="">全部</option>
            <option v-for="a in actions" :key="a" :value="a">{{ a }}</option>
          </select>
        </label>
        <label class="text-sm">关键词
          <input v-model="filters.keyword" class="input mt-1" placeholder="明细 / 类型 / 动作" @keyup.enter="search" />
        </label>
        <label class="text-sm">起始日期
          <input v-model="filters.from" type="date" class="input mt-1" />
        </label>
        <label class="text-sm">截止日期
          <input v-model="filters.to" type="date" class="input mt-1" />
        </label>
        <div class="flex items-end gap-2">
          <button class="btn-primary text-sm" @click="search">查询</button>
          <button class="btn-ghost text-sm" @click="reset">重置</button>
        </div>
      </div>
    </div>

    <div class="card p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">记录（{{ total }}）</h2>
        <span class="text-xs text-slate-400">最多显示最近 1000 条</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2 w-16">编号</th>
            <th class="text-left py-2">业务类型</th>
            <th class="text-left py-2 w-24">动作</th>
            <th class="text-left py-2 w-28">操作人</th>
            <th class="text-left py-2 w-20">角色</th>
            <th class="text-left py-2">明细</th>
            <th class="text-left py-2 w-40">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in items" :key="a.id" class="border-b border-slate-100">
            <td class="py-2 text-slate-400">{{ a.id }}</td>
            <td class="py-2">{{ a.bizType }}<span class="text-xs text-slate-400">#{{ a.bizId }}</span></td>
            <td class="py-2">
              <span class="tag bg-slate-100 text-slate-500">{{ a.action }}</span>
            </td>
            <td class="py-2 text-slate-600">{{ a.operatorName || '—' }}</td>
            <td class="py-2 text-slate-500">{{ a.operatorRoleText }}</td>
            <td class="py-2 text-slate-600 max-w-xl truncate" :title="a.detail">{{ a.detail }}</td>
            <td class="py-2 text-slate-500">{{ fmt(a.createTime) }}</td>
          </tr>
          <tr v-if="!items.length"><td colspan="7" class="py-6 text-center text-slate-400">没有匹配的记录</td></tr>
        </tbody>
      </table>

      <div class="flex items-center justify-between text-sm text-slate-500 mt-3" v-if="total > 0">
        <span>共 {{ total }} 条，第 {{ page }} / {{ pages }} 页</span>
        <div class="flex items-center gap-2">
          <button class="btn-ghost text-xs" :disabled="page <= 1" @click="goto(page - 1)">上一页</button>
          <button class="btn-ghost text-xs" :disabled="page >= pages" @click="goto(page + 1)">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminPortalApi } from '../../api/adminPortal'

const items = ref([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const bizTypes = ref([])
const actions = ref([])
const message = ref('')
const ok = ref(true)

const filters = reactive({ bizType: '', action: '', keyword: '', from: '', to: '' })

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function fmt(dt) { return dt ? String(dt).replace('T', ' ').slice(0, 19) : '—' }

async function load() {
  const res = await adminPortalApi.audit({
    bizType: filters.bizType || undefined,
    action: filters.action || undefined,
    keyword: filters.keyword || undefined,
    from: filters.from || undefined,
    to: filters.to || undefined,
    page: page.value,
    size: 20
  })
  items.value = res.items
  total.value = res.total
  pages.value = res.pages
  if (res.bizTypes && res.bizTypes.length > bizTypes.value.length) bizTypes.value = res.bizTypes
  if (res.actions && res.actions.length > actions.value.length) actions.value = res.actions
}
function search() { page.value = 1; load() }
function reset() {
  filters.bizType = ''
  filters.action = ''
  filters.keyword = ''
  filters.from = ''
  filters.to = ''
  page.value = 1
  load()
}
function goto(p) { page.value = p; load() }

onMounted(async () => {
  try { await load() } catch (e) { toast(e.message, false) }
})
</script>
