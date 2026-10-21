<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3">
        <h1 class="text-lg font-semibold text-slate-800">全部课程</h1>
        <span class="tag bg-slate-100 text-slate-500">默认当前学期</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <div class="card p-4">
      <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
        <label class="text-sm md:col-span-2">搜索（课程名称 / 班级）
          <input v-model="keyword" class="input mt-1" placeholder="输入关键词" />
        </label>
        <label class="text-sm">学期
          <select v-model="termId" class="input mt-1" @change="load">
            <option :value="''">全部学期</option>
            <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
          </select>
        </label>
        <label class="text-sm">状态
          <select v-model="status" class="input mt-1">
            <option value="">全部</option>
            <option value="进行中">进行中</option>
            <option value="已结课">已结课</option>
          </select>
        </label>
        <div class="flex items-end">
          <button class="btn-ghost text-sm" @click="reset">重置</button>
        </div>
      </div>
    </div>

    <div v-if="!shown.length" class="card p-10 text-center text-slate-400 text-sm">
      没有符合条件的课程。
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <div v-for="c in shown" :key="c.offeringId" class="card overflow-hidden flex flex-col">
        <div class="h-20 bg-gradient-to-r from-brand-500 to-brand-700 flex items-end justify-end p-2">
          <span class="tag" :class="c.allClosed ? 'bg-white/80 text-slate-600' : 'bg-white/90 text-brand-700'">
            {{ c.allClosed ? '已结课' : '进行中' }}
          </span>
        </div>
        <div class="p-4 flex-1 flex flex-col justify-between">
          <div class="space-y-1">
            <h3 class="font-medium text-slate-800">{{ c.courseName }}</h3>
            <p class="text-xs text-slate-500">{{ c.termName }}</p>
            <p class="text-xs text-slate-400">负责 {{ c.classCount }} 个教学班 ｜ 学生 {{ c.studentCount }} 人</p>
            <p class="text-xs text-slate-400 truncate">
              {{ (c.classes || []).map((x) => x.classCode + ' ' + x.className).join('、') }}
            </p>
          </div>
          <div class="flex items-center justify-between mt-3">
            <button class="text-xs text-brand-600 hover:underline"
                    @click="$router.push({ path: '/teacher/course/' + c.offeringId })">课程详情</button>
            <button class="text-xs text-slate-500 hover:underline"
                    @click="$router.push({ path: '/teacher/teaching', query: { teachingClassId: (c.classes && c.classes[0] || {}).teachingClassId, offeringId: c.offeringId } })">
              教学详情
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { teacherPortalApi } from '../../api/teacherPortal'

const terms = ref([])
const courses = ref([])
const termId = ref('')
const status = ref('')
const keyword = ref('')
const message = ref('')
const ok = ref(true)

const shown = computed(() => {
  const k = keyword.value.trim()
  return courses.value.filter((c) => {
    if (k && !((c.courseName || '').includes(k)
      || (c.classes || []).some((x) => (x.className || '').includes(k) || (x.classCode || '').includes(k)))) {
      return false
    }
    if (status.value === '进行中' && c.allClosed) return false
    if (status.value === '已结课' && !c.allClosed) return false
    return true
  })
})

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function load() {
  const home = await teacherPortalApi.home(termId.value || undefined)
  courses.value = home.courses || []
}
function reset() {
  keyword.value = ''
  status.value = ''
  const cur = terms.value.find((t) => t.current || t.isCurrent === 1)
  termId.value = cur ? cur.id : ''
  load()
}

onMounted(async () => {
  try {
    terms.value = await teacherPortalApi.terms()
    const cur = terms.value.find((t) => t.current || t.isCurrent === 1)
    if (cur) termId.value = cur.id
    await load()
  } catch (e) { toast(e.message, false) }
})
</script>
