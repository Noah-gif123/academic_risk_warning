<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { getSummaries, generateSummary } from '../../api/http.js'

const { teacherToken } = useAuth()
defineProps({ courseId: { type: Number, required: true } })

const summaries = ref([])
const loading = ref(false)
const period = ref('DAILY')
const generating = ref(false)
const message = ref('')
const messageType = ref('info')

onMounted(() => loadSummaries())

async function loadSummaries() {
  loading.value = true
  try {
    const res = await getSummaries(teacherToken.value, period.value, 20)
    if (res.success) summaries.value = res.data || []
    else setMsg(res.message || '获取失败', 'error')
  } catch { setMsg('获取汇总报告失败', 'error') }
  finally { loading.value = false }
}

async function doGenerate() {
  generating.value = true
  try {
    const res = await generateSummary(teacherToken.value, period.value)
    if (res.success) { setMsg(res.message || '生成成功', 'success'); loadSummaries() }
    else setMsg(res.message || '生成失败', 'error')
  } catch { setMsg('生成失败', 'error') }
  finally { generating.value = false }
}

function setMsg(text, type = 'info') { message.value = text; messageType.value = type }

function changePeriod(p) { period.value = p; loadSummaries() }

function formatTime(t) { return t ? t.replace('T', ' ').substring(0, 16) : '' }
</script>

<template>
  <div class="section">
    <div class="card" style="padding:16px;margin-bottom:16px;display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
      <button class="btn-ghost" :class="{ active: period === 'DAILY' }" @click="changePeriod('DAILY')">每日</button>
      <button class="btn-ghost" :class="{ active: period === 'WEEKLY' }" @click="changePeriod('WEEKLY')">每周</button>
      <span style="flex:1;"></span>
      <button class="btn-primary" @click="doGenerate" :disabled="generating">
        {{ generating ? '生成中...' : '⚡ 生成' + (period === 'DAILY' ? '日报' : '周报') }}
      </button>
    </div>

    <p v-if="message" class="msg" :class="messageType" style="margin-bottom:12px;">{{ message }}</p>

    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载中...
    </div>

    <div v-else-if="summaries.length">
      <div v-for="s in summaries" :key="s.id || s.summaryId" class="card" style="padding:16px;margin-bottom:12px;">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;">
          <h3 style="margin-bottom:8px;">{{ s.title || '汇总报告' }}</h3>
          <span class="period-tag">{{ s.period || period }}</span>
        </div>
        <div style="font-size:12px;color:var(--text-tertiary);margin-bottom:8px;">
          生成时间：{{ formatTime(s.createTime || s.generatedAt) }}
        </div>
        <div style="font-size:14px;line-height:1.7;white-space:pre-wrap;background:var(--bg-card-subtle);padding:12px;border-radius:8px;">
          {{ s.content || s.summary || '暂无内容' }}
        </div>
      </div>
    </div>

    <div v-else class="card" style="text-align:center;padding:40px;color:var(--text-tertiary);">
      暂无汇总报告，点击上方按钮生成
    </div>
  </div>
</template>

<style scoped>
.period-tag {
  display:inline-block; padding:2px 10px; border-radius:12px; font-size:12px;
  background:var(--accent-light); color:var(--accent-primary); font-weight:600;
}
.btn-ghost.active { background:var(--accent-light); color:var(--accent-primary); }
</style>
