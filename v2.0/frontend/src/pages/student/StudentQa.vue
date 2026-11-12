<template>
  <div class="h-[calc(100vh-8.5rem)] min-h-[560px] grid grid-cols-1 xl:grid-cols-[230px_1fr_300px] gap-3">
    <!-- 课程 -->
    <aside class="card p-3 overflow-y-auto">
      <div class="flex items-center justify-between px-1 mb-2">
        <h2 class="text-sm font-medium text-slate-700">课程答疑</h2>
        <span class="text-[10px] text-slate-400">{{ courses.length }} 门</span>
      </div>
      <button v-for="c in courses" :key="c.teachingClassId"
              class="w-full text-left px-3 py-2 rounded-lg mb-1 border transition"
              :class="selected?.teachingClassId === c.teachingClassId ? 'border-brand-400 bg-brand-50' : 'border-transparent hover:bg-slate-50'"
              @click="switchCourse(c)">
        <p class="text-sm font-medium text-slate-700 truncate">{{ c.courseName }}</p>
        <p class="text-[10px] text-slate-400 truncate">{{ c.classCode || '' }} {{ c.teacherName || '' }}</p>
      </button>
      <p v-if="!courses.length" class="text-xs text-slate-400 px-2 py-6">暂时没有可选课程</p>
    </aside>

    <!-- 对话 -->
    <section class="card flex flex-col min-h-0">
      <div class="px-4 py-3 border-b border-slate-200 flex items-center justify-between gap-2">
        <div>
          <h2 class="text-sm font-medium text-slate-800">{{ selected?.courseName || '选择一门课程' }}</h2>
          <p class="text-[10px] text-slate-400">仅基于本课程知识库回答 · 引用可查 · 无依据拒答</p>
        </div>
        <span class="tag bg-brand-50 text-brand-700">RAG</span>
      </div>

      <div ref="scrollRef" class="flex-1 overflow-y-auto px-4 py-4 space-y-3">
        <div v-if="!messages.length" class="h-full flex flex-col items-center justify-center text-center text-slate-400">
          <p class="text-sm">选一门课，开始提问</p>
          <p class="text-xs mt-1">例如：这门课的期末成绩怎么构成？</p>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="flex" :class="m.role === 'user' ? 'justify-end' : 'justify-start'">
          <div class="max-w-[82%] rounded-2xl px-3.5 py-2.5 text-sm leading-6"
               :class="m.role === 'user' ? 'bg-brand-600 text-white rounded-br-md' : 'bg-slate-50 text-slate-700 border border-slate-200 rounded-bl-md'">
            <p class="whitespace-pre-line">{{ m.content }}</p>
            <div v-if="m.role === 'assistant' && m.citations?.length" class="mt-2 flex flex-wrap gap-1.5">
              <button v-for="(c, j) in m.citations" :key="j"
                      class="text-[10px] px-2 py-0.5 rounded-full bg-white border border-slate-200 text-slate-500 hover:border-brand-300"
                      @click="showSource(m, c)">
                依据：{{ c.docName }} 第{{ c.sectionNo }}段
              </button>
            </div>
            <div v-if="m.refused" class="text-[10px] mt-2 text-amber-600">未检索到可靠依据，已拒答</div>
          </div>
        </div>
        <div v-if="busy" class="text-xs text-slate-400">正在检索课程资料…</div>
      </div>

      <div class="border-t border-slate-200 p-3">
        <p v-if="error" class="text-xs text-red-500 mb-2">{{ error }}</p>
        <div class="flex gap-2">
          <textarea v-model="question" rows="2" class="input flex-1 resize-none text-sm" :disabled="!selected || busy"
                    placeholder="问这门课的知识点、作业、考试要求…" @keydown.enter.exact.prevent="ask" />
          <button class="btn-primary text-sm px-4 self-stretch" :disabled="!selected || !question.trim() || busy" @click="ask">
            {{ busy ? '回答中…' : '发送' }}
          </button>
        </div>
        <p class="text-[10px] text-slate-400 mt-1">Enter 发送；回答只引用本课程资料，不跨课程。</p>
      </div>
    </section>

    <!-- 来源 -->
    <aside class="card p-3 overflow-y-auto">
      <h2 class="text-sm font-medium text-slate-700 mb-2">引用来源</h2>
      <p v-if="!activeSource" class="text-xs text-slate-400 leading-6">点击回答里的“依据”查看原文片段。</p>
      <div v-else class="space-y-3">
        <div class="rounded-lg bg-slate-50 p-3">
          <p class="text-xs font-medium text-slate-700">{{ activeSource.docName }}</p>
          <p class="text-[10px] text-slate-400 mt-0.5">第 {{ activeSource.sectionNo }} 段
            <span v-if="activeSource.pageNo"> · 第 {{ activeSource.pageNo }} 页</span>
          </p>
        </div>
        <p class="text-xs text-slate-600 leading-6 whitespace-pre-line">{{ activeSource.content }}</p>
        <p class="text-[10px] text-slate-400">来源：{{ activeSource.path || '课程知识库' }}</p>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { portalApi } from '../../api/portal'
import { qaApi } from '../../api/ai'

const courses = ref([])
const selected = ref(null)
const sessions = ref({})
const question = ref('')
const busy = ref(false)
const error = ref('')
const activeSource = ref(null)
const scrollRef = ref(null)

const messages = computed(() => selected.value ? (sessions.value[selected.value.teachingClassId] || []) : [])

function switchCourse(c) {
  selected.value = c
  if (!sessions.value[c.teachingClassId]) sessions.value[c.teachingClassId] = []
  activeSource.value = null
  error.value = ''
  nextTick(() => scrollRef.value && (scrollRef.value.scrollTop = scrollRef.value.scrollHeight))
}

function historyOf(c) {
  return (sessions.value[c.teachingClassId] || []).slice(-8).map((m) => ({ role: m.role, content: m.content }))
}

async function ask() {
  const q = question.value.trim()
  if (!q || !selected.value || busy.value) return
  const c = selected.value
  const list = sessions.value[c.teachingClassId] || (sessions.value[c.teachingClassId] = [])
  list.push({ role: 'user', content: q })
  question.value = ''
  busy.value = true
  error.value = ''
  try {
    const r = await qaApi.ask({
      courseId: c.courseId,
      teachingClassId: c.teachingClassId,
      question: q,
      history: historyOf(c)
    })
    list.push({
      role: 'assistant',
      content: r.answer || '',
      citations: r.citations || [],
      hits: r.hits || [],
      refused: !!r.refused,
      source: r.source
    })
    if (r.citations?.length) showSource(list[list.length - 1], r.citations[0])
  } catch (e) {
    error.value = e?.message || '回答失败'
  } finally {
    busy.value = false
    nextTick(() => scrollRef.value && (scrollRef.value.scrollTop = scrollRef.value.scrollHeight))
  }
}

function showSource(message, citation) {
  activeSource.value = (message.hits || []).find((h) =>
    h.docName === citation.docName && Number(h.sectionNo) === Number(citation.sectionNo)
  ) || { ...citation, content: '未找到完整片段' }
}

onMounted(async () => {
  try {
    const r = await portalApi.cards({ size: 50 })
    courses.value = r?.items || r || []
    if (courses.value.length) switchCourse(courses.value[0])
  } catch (e) {
    error.value = e?.message || '课程加载失败'
  }
})
</script>
