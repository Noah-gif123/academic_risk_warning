<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <h1 class="text-lg font-semibold text-slate-800">课程学习</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <!-- 筛选 -->
    <div class="card p-4">
      <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
        <label class="text-sm md:col-span-2">搜索（课程名称 / 教师姓名）
          <input v-model="filters.keyword" class="input mt-1" placeholder="输入关键词" @keyup.enter="search" />
        </label>
        <label class="text-sm">课程状态
          <select v-model="filters.status" class="input mt-1">
            <option value="">全部</option>
            <option value="进行中">进行中</option>
            <option value="未开课">未开课</option>
          </select>
        </label>
        <label class="text-sm">学期
          <select v-model="filters.termId" class="input mt-1">
            <option :value="''">全部学期</option>
            <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
          </select>
        </label>
        <div class="flex items-end gap-2">
          <button class="btn-primary text-sm" @click="search">筛选</button>
          <button class="btn-ghost text-sm" @click="reset">重置</button>
          <button class="btn-ghost text-sm" @click="showJoin = true">加入课程</button>
        </div>
      </div>
    </div>

    <!-- 加入课程 -->
    <div class="card p-4 space-y-2" v-if="showJoin">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">加入课程</h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="showJoin = false">关闭</button>
      </div>
      <p class="text-xs text-slate-400">输入任课教师给你的 6 位邀请码。若该教学班开启了入班审核，需要等教师同意。</p>
      <div class="flex items-center gap-2">
        <input v-model="inviteCode" class="input w-40 uppercase" maxlength="6" placeholder="如 A3K9ZP" />
        <button class="btn-primary text-sm" @click="join">提交申请</button>
      </div>
      <div v-if="requests.length" class="text-xs text-slate-500 mt-2">
        <p class="mb-1">我的申请</p>
        <p v-for="r in requests" :key="r.id">
          {{ r.courseName }} · {{ r.className }} —— {{ r.statusText }}
          <span v-if="r.reviewNote" class="text-slate-400">（{{ r.reviewNote }}）</span>
        </p>
      </div>
    </div>

    <!-- 列表 -->
    <div v-if="!items.length" class="card p-10 text-center text-slate-400 text-sm">
      暂无课程{{ filters.keyword || filters.status ? '（试试重置筛选）' : '，用邀请码加入一门课吧' }}。
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div v-for="c in items" :key="c.teachingClassId" class="card overflow-hidden">
        <div class="h-20 bg-gradient-to-r from-brand-500 to-brand-700 flex items-end justify-end p-2">
          <span class="tag" :class="c.status === '进行中' ? 'bg-white/90 text-brand-700' : 'bg-white/70 text-slate-600'">
            {{ c.status }}
          </span>
        </div>
        <div class="p-4 space-y-1">
          <h3 class="font-medium text-slate-800">{{ c.courseName }}</h3>
          <p class="text-xs text-slate-500">{{ c.termName }}</p>
          <p class="text-xs text-slate-400">{{ c.teacherName }} ｜ {{ c.className }}</p>
          <button class="text-xs text-brand-600 hover:underline mt-2"
                  @click="$router.push({ path: '/student/course/' + c.teachingClassId })">开始学习</button>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-between text-sm text-slate-500" v-if="total > 0">
      <span>共 {{ total }} 门，第 {{ page }} / {{ pages }} 页</span>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-xs" :disabled="page <= 1" @click="goto(page - 1)">上一页</button>
        <button class="btn-ghost text-xs" :disabled="page >= pages" @click="goto(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { portalApi } from '../../api/portal'

const terms = ref([])
const items = ref([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const showJoin = ref(false)
const inviteCode = ref('')
const requests = ref([])
const message = ref('')
const ok = ref(true)

const filters = reactive({ keyword: '', status: '', termId: '' })

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function load() {
  const res = await portalApi.cards({
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
    termId: filters.termId || undefined,
    page: page.value,
    size: 9
  })
  items.value = res.items
  total.value = res.total
  pages.value = res.pages
}
function search() { page.value = 1; load() }
function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.termId = ''
  page.value = 1
  load()
}
function goto(p) { page.value = p; load() }

async function join() {
  if (!inviteCode.value || inviteCode.value.length !== 6) {
    toast('请输入 6 位邀请码', false)
    return
  }
  try {
    const r = await portalApi.apply(inviteCode.value.toUpperCase())
    toast('已提交：' + r.statusText)
    inviteCode.value = ''
    requests.value = await portalApi.myRequests()
    await load()
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    terms.value = await portalApi.terms()
    // 进页面默认检索「当前学期」的全部课程（之前默认是全部学期，看起来像把所有学期的课都堆在一起）
    const cur = terms.value.find((t) => t.current || t.isCurrent === 1)
    if (cur) filters.termId = cur.id
    requests.value = await portalApi.myRequests()
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
