<template>
  <div class="ai-sidebar" :class="{ open: isOpen }" v-if="isLoggedIn">
    <!-- 收起时的触发条 -->
    <div v-if="!isOpen" class="ai-toggle-strip" @click="open" title="打开 AI 助手">
      <div class="ai-toggle-inner">
        <svg class="ai-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z"/>
          <circle cx="8.5" cy="9.5" r="1.5"/><circle cx="15.5" cy="9.5" r="1.5"/>
          <path d="M8 14c0 0 1.5 2 4 2s4-2 4-2" stroke-linecap="round"/>
        </svg>
        <span class="ai-toggle-label">AI</span>
      </div>
    </div>

    <!-- 展开面板 -->
    <div v-if="isOpen" class="ai-panel" :class="{ fullscreen: isFullscreen }">
      <!-- 面板头部 -->
      <div class="ai-panel-header">
        <div class="ai-panel-header-left">
          <svg class="ai-panel-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z"/>
            <circle cx="8.5" cy="9.5" r="1.5"/><circle cx="15.5" cy="9.5" r="1.5"/>
            <path d="M8 14c0 0 1.5 2 4 2s4-2 4-2" stroke-linecap="round"/>
          </svg>
          <span class="ai-panel-title">AI 助手</span>
          <div class="ai-tabs">
            <button class="ai-tab" :class="{ active: activeTab === 'qa' }" @click="switchTab('qa')" title="知识库问答">
              <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"><rect x="1" y="3" width="14" height="10" rx="1"/><path d="M5 7h6M5 10h4"/></svg>
              <span>问答</span>
            </button>
            <button class="ai-tab" :class="{ active: activeTab === 'tools' }" @click="switchTab('tools')" title="数据分析工具">
              <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"><circle cx="8" cy="8" r="6"/><path d="M8 5v3l2 2"/></svg>
              <span>工具</span>
            </button>
          </div>
        </div>
        <div class="ai-panel-header-right">
          <button class="ai-header-btn" @click="newChat" title="新对话">＋</button>
          <button class="ai-header-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
            <svg v-if="!isFullscreen" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M2 6V2h4M10 2h4v4M14 10v4h-4M6 14H2v-4"/></svg>
            <svg v-else viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M4 4v3H1M12 4v3h3M12 12v-3h3M4 12v-3H1"/></svg>
          </button>
          <button class="ai-header-btn ai-close-btn" @click="close" title="关闭面板">✕</button>
        </div>
      </div>

      <!-- ================================================================ -->
      <!-- 快捷工具面板 -->
      <!-- ================================================================ -->
      <div v-if="activeTab === 'tools'" class="ai-tools-panel">
        <!-- 当前课程信息 -->
        <div v-if="actualCourseId" class="ai-current-course">
          📘 {{ actualCourseName || '当前课程' }}
        </div>
        <div v-else class="ai-current-course ai-no-course">
          ⚠️ 请先选择一门课程以启用数据分析工具
        </div>

        <div class="ai-tools-intro">
          <p class="ai-tools-title">⚡ 教师工具</p>
          <p class="ai-tools-desc">基于真实数据的智能分析，点击即可查看结果</p>
        </div>

        <button class="ai-tool-card" :class="{ disabled: !actualCourseId || toolRunning }" @click="runTool('riskOverview')" :disabled="!actualCourseId || toolRunning">
          <span class="ai-tool-icon">📊</span>
          <span class="ai-tool-label">课程风险概览</span>
          <span class="ai-tool-status">{{ toolResults['riskOverview'] ? '✓' : '' }}</span>
          <span class="ai-tool-arrow">→</span>
        </button>

        <button class="ai-tool-card" :class="{ disabled: !actualCourseId || toolRunning }" @click="runTool('weakPoints')" :disabled="!actualCourseId || toolRunning">
          <span class="ai-tool-icon">🎯</span>
          <span class="ai-tool-label">知识点薄弱项</span>
          <span class="ai-tool-status">{{ toolResults['weakPoints'] ? '✓' : '' }}</span>
          <span class="ai-tool-arrow">→</span>
        </button>

        <button class="ai-tool-card" :class="{ disabled: !actualCourseId || toolRunning }" @click="runTool('alertRules')" :disabled="!actualCourseId || toolRunning">
          <span class="ai-tool-icon">⚙️</span>
          <span class="ai-tool-label">预警规则说明</span>
          <span class="ai-tool-status">{{ toolResults['alertRules'] ? '✓' : '' }}</span>
          <span class="ai-tool-arrow">→</span>
        </button>

        <button class="ai-tool-card" :class="{ disabled: !actualCourseId || toolRunning }" @click="runTool('interventionPlan')" :disabled="!actualCourseId || toolRunning">
          <span class="ai-tool-icon">📋</span>
          <span class="ai-tool-label">生成干预方案</span>
          <span class="ai-tool-status">{{ toolResults['interventionPlan'] ? '✓' : '' }}</span>
          <span class="ai-tool-arrow">→</span>
        </button>

        <button class="ai-tool-card" :class="{ disabled: !actualCourseId || toolRunning }" @click="runTool('teachingAdvice')" :disabled="!actualCourseId || toolRunning">
          <span class="ai-tool-icon">💡</span>
          <span class="ai-tool-label">获取教学建议</span>
          <span class="ai-tool-status">{{ toolResults['teachingAdvice'] ? '✓' : '' }}</span>
          <span class="ai-tool-arrow">→</span>
        </button>

        <p class="ai-tools-footer">工具调用基于当前课程的真实数据</p>
      </div>

      <!-- ================================================================ -->
      <!-- 工具结果展示区（tools tab 内） -->
      <!-- ================================================================ -->
      <div v-if="activeTab === 'tools' && activeToolResult" class="ai-tool-result">
        <div class="ai-tool-result-header">
          <span class="ai-tool-result-title">{{ activeToolTitle }}</span>
          <button class="ai-tool-result-close" @click="closeToolResult">✕</button>
        </div>

        <!-- 风险概览 -->
        <div v-if="activeToolResult === 'riskOverview' && toolResults.riskOverview" class="ai-tool-result-body">
          <div v-if="toolResults.riskOverview.error" class="ai-tool-error">{{ toolResults.riskOverview.error }}</div>
          <template v-else>
          <div class="ai-risk-distribution">
            <div class="ai-risk-bar red" :style="{ width: riskPercent('RED') + '%' }">
              <span class="ai-risk-bar-label">🔴 {{ riskCount('RED') }}</span>
            </div>
            <div class="ai-risk-bar orange" :style="{ width: riskPercent('ORANGE') + '%' }">
              <span class="ai-risk-bar-label">🟠 {{ riskCount('ORANGE') }}</span>
            </div>
            <div class="ai-risk-bar yellow" :style="{ width: riskPercent('YELLOW') + '%' }">
              <span class="ai-risk-bar-label">🟡 {{ riskCount('YELLOW') }}</span>
            </div>
            <div class="ai-risk-bar green" :style="{ width: riskPercent('GREEN') + '%' }">
              <span class="ai-risk-bar-label">🟢 {{ riskCount('GREEN') }}</span>
            </div>
          </div>
          <p class="ai-total-students">学生总数：{{ toolResults.riskOverview.totalStudents }}人</p>
          <div v-if="toolResults.riskOverview.topRiskStudents?.length" class="ai-risk-list">
            <h4>⚠️ 高风险学生</h4>
            <div v-for="s in toolResults.riskOverview.topRiskStudents.slice(0,5)" :key="s.studentId" class="ai-risk-item">
              <span :class="'ai-risk-dot ai-dot-' + (s.alertLevel || '').toLowerCase()"></span>
              <strong>{{ s.studentName }}</strong>
              <span class="ai-risk-type">{{ s.alertReason }}</span>
              <span class="ai-risk-score">{{ s.riskScore }}分</span>
            </div>
          </div>
          <div v-if="toolResults.riskOverview.aiAnalysis" class="ai-analysis-text">
            <h4>🤖 AI 分析</h4>
            <p>{{ toolResults.riskOverview.aiAnalysis }}</p>
          </div>
          </template>
        </div>

        <!-- 薄弱知识点 -->
        <div v-if="activeToolResult === 'weakPoints' && toolResults.weakPoints" class="ai-tool-result-body">
          <div v-if="toolResults.weakPoints.error" class="ai-tool-error">{{ toolResults.weakPoints.error }}</div>
          <template v-else>
          <p class="ai-total-students">共 {{ toolResults.weakPoints.totalCount }} 个薄弱知识点</p>
          <div class="ai-weak-list">
            <div v-for="(wp, i) in toolResults.weakPoints.weakPoints" :key="wp.knowledgePointId" class="ai-weak-item">
              <span class="ai-weak-rank">{{ i + 1 }}</span>
              <div class="ai-weak-info">
                <strong>{{ wp.knowledgePointName }}</strong>
                <span class="ai-weak-meta">{{ wp.weakStudentCount }}人薄弱 · 错误率 {{ wp.avgErrorRate }}%</span>
              </div>
            </div>
          </div>
          </template>
        </div>

        <!-- 预警规则 -->
        <div v-if="activeToolResult === 'alertRules' && toolResults.alertRules" class="ai-tool-result-body">
          <div v-if="toolResults.alertRules.error" class="ai-tool-error">{{ toolResults.alertRules.error }}</div>
          <div v-else class="ai-rule-explanation">
            <pre>{{ toolResults.alertRules.explanation }}</pre>
          </div>
        </div>

        <!-- 干预方案 -->
        <div v-if="activeToolResult === 'interventionPlan' && toolResults.interventionPlan" class="ai-tool-result-body">
          <div v-if="toolResults.interventionPlan.error" class="ai-tool-error">{{ toolResults.interventionPlan.error }}</div>
          <div v-else class="ai-analysis-text">
            <pre>{{ toolResults.interventionPlan.interventionPlan }}</pre>
          </div>
        </div>

        <!-- 教学建议 -->
        <div v-if="activeToolResult === 'teachingAdvice' && toolResults.teachingAdvice" class="ai-tool-result-body">
          <div v-if="toolResults.teachingAdvice.error" class="ai-tool-error">{{ toolResults.teachingAdvice.error }}</div>
          <div v-else class="ai-analysis-text">
            <pre>{{ toolResults.teachingAdvice.teachingAdvice }}</pre>
          </div>
        </div>

        <div v-if="toolRunning" class="ai-thinking-bar">
          <span class="ai-dot"></span><span class="ai-dot"></span><span class="ai-dot"></span>
        </div>
      </div>

      <!-- ================================================================ -->
      <!-- 知识问答对话区 -->
      <!-- ================================================================ -->
      <div v-if="activeTab === 'qa'" class="ai-panel-body" ref="chatBody">
        <div v-if="messages.length === 0" class="ai-welcome">
          <div class="ai-welcome-icon">
            <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="32" cy="32" r="28"/>
              <circle cx="24" cy="26" r="3"/><circle cx="40" cy="26" r="3"/>
              <path d="M22 40c0 0 4 5 10 5s10-5 10-5" stroke-linecap="round"/>
            </svg>
          </div>
          <h3 class="ai-welcome-title">你好，我是学业预警 AI 助手</h3>
          <p class="ai-welcome-desc">基于课程知识库与学情数据，为教师提供智能辅助</p>
          <div class="ai-suggestions">
            <p class="ai-suggestions-hint">{{ suggestLoading ? '正在分析课程数据...' : '试试问我：' }}</p>
            <button v-for="q in currentSuggests" :key="q" class="ai-suggest-chip" @click="sendSuggestion(q)" :disabled="suggestLoading">
              {{ q }}
            </button>
            <div v-if="suggestLoading" class="ai-suggest-loading">
              <span class="ai-dot"></span><span class="ai-dot"></span><span class="ai-dot"></span>
            </div>
            <p v-if="!suggestLoading && currentSuggests.length === 0" class="ai-suggestions-hint">请先选择课程以获取个性化建议</p>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, i) in messages" :key="i" class="ai-message" :class="msg.role">
          <div class="ai-msg-avatar">
            <svg v-if="msg.role === 'assistant'" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.4 0-8-3.6-8-8s3.6-8 8-8 8 3.6 8 8-3.6 8-8 8zm-3.5-6.5c.8 0 1.5-.7 1.5-1.5s-.7-1.5-1.5-1.5-1.5.7-1.5 1.5.7 1.5 1.5 1.5zm7 0c.8 0 1.5-.7 1.5-1.5s-.7-1.5-1.5-1.5-1.5.7-1.5 1.5.7 1.5 1.5 1.5zM12 17c-2.3 0-3.5-1.5-3.5-1.5h7s-1.2 1.5-3.5 1.5z"/></svg>
            <div v-else class="ai-msg-avatar-user">{{ getUserInitial() }}</div>
          </div>
          <div class="ai-msg-content">
            <div class="ai-msg-bubble" v-text="msg.content"></div>
            <div v-if="msg.sources && msg.sources.length" class="ai-msg-sources">
              <span class="ai-source-label">参考来源：</span>
              <span v-for="(s, si) in msg.sources" :key="si" class="ai-source-tag">{{ s }}</span>
            </div>
            <div v-else-if="msg.role === 'assistant' && msg.grounded === false" class="ai-msg-nocite">
              ⚠️ {{ msg.retrievalNote || '本条回答未返回知识库引用依据' }}
            </div>
          </div>
        </div>

        <!-- 加载动画 -->
        <div v-if="loading" class="ai-message assistant">
          <div class="ai-msg-avatar">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.4 0-8-3.6-8-8s3.6-8 8-8 8 3.6 8 8-3.6 8-8 8zm-3.5-6.5c.8 0 1.5-.7 1.5-1.5s-.7-1.5-1.5-1.5-1.5.7-1.5 1.5.7 1.5 1.5 1.5zm7 0c.8 0 1.5-.7 1.5-1.5s-.7-1.5-1.5-1.5-1.5.7-1.5 1.5.7 1.5 1.5 1.5zM12 17c-2.3 0-3.5-1.5-3.5-1.5h7s-1.2 1.5-3.5 1.5z"/></svg>
          </div>
          <div class="ai-msg-bubble ai-thinking">
            <span class="ai-dot"></span><span class="ai-dot"></span><span class="ai-dot"></span>
          </div>
        </div>
      </div>

      <!-- 输入区（仅问答tab） -->
      <div v-if="activeTab === 'qa'" class="ai-panel-footer">
        <div class="ai-input-row">
          <button class="ai-input-btn" @click="clearChat" title="清空对话" v-if="messages.length">
            <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M2 4h12M5 4V3a1 1 0 011-1h4a1 1 0 011 1v1M13 4v9a1 1 0 01-1 1H4a1 1 0 01-1-1V4"/></svg>
          </button>
          <div class="ai-input-wrapper">
            <textarea
              v-model="input"
              class="ai-input"
              placeholder="输入问题，按 Enter 发送（Shift+Enter 换行）"
              :disabled="loading"
              @keydown="handleKeydown"
              rows="1"
              ref="inputEl"
            ></textarea>
          </div>
          <button
            class="ai-send-btn"
            :disabled="!input.trim() || loading"
            @click="send"
            :class="{ active: input.trim() && !loading }"
          >
            <svg v-if="!loading" viewBox="0 0 16 16" fill="currentColor"><path d="M1.5 2L14 8l-12.5 6 2.5-6-2.5-6z"/></svg>
            <span v-else class="ai-send-spinner"></span>
          </button>
        </div>
        <p class="ai-footer-hint">AI 助手的回答仅供参考，请结合实际情况判断</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import {
  teacherAgentQA, teacherAgentSuggestions, teacherAgentRiskOverview,
  teacherAgentWeakPoints, teacherAgentAlertRules,
  teacherAgentInterventionPlan, teacherAgentTeachingAdvice
} from '../api/http.js'

const emit = defineEmits(['close'])

const props = defineProps({
  token: { type: String, default: '' },
  userName: { type: String, default: '' },
  teacherId: { type: Number, default: null },
  courseId: { type: Number, default: null },
  courseName: { type: String, default: '' }
})

const isOpen = ref(false)
const isFullscreen = ref(false)
const isLoggedIn = ref(false)
const input = ref('')
const loading = ref(false)
const messages = ref([])
const chatBody = ref(null)
const inputEl = ref(null)
const activeTab = ref('qa')

// 推荐问题（动态获取）
const currentSuggests = ref([])
const suggestLoading = ref(false)

// 工具状态
const toolRunning = ref(false)
const toolResults = ref({})
const activeToolResult = ref(null)

// 当前课程上下文
const actualTeacherId = computed(() => props.teacherId)
const actualCourseId = computed(() => props.courseId)
const actualCourseName = computed(() => props.courseName || '')

watch(() => props.token, (val) => { isLoggedIn.value = !!val }, { immediate: true })

// 当打开面板或切换tab时，加载推荐问题
watch(isOpen, (val) => {
  if (val && activeTab.value === 'qa') loadSuggestions()
})
watch(activeTab, (val) => {
  if (val === 'qa' && isOpen.value) loadSuggestions()
})
// 课程变化时重新加载
watch(() => props.courseId, () => {
  if (isOpen.value && activeTab.value === 'qa') loadSuggestions()
  // 清空工具结果
  toolResults.value = {}
  closeToolResult()
})

onMounted(() => {
  document.addEventListener('keydown', handleGlobalShortcut)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalShortcut)
})

function handleGlobalShortcut(e) {
  if (e.ctrlKey && e.key === 'k' && isLoggedIn.value) {
    e.preventDefault()
    if (!isOpen.value) { open(); return }
    inputEl.value?.focus()
  }
  if (e.key === 'Escape' && isOpen.value && !isFullscreen.value) {
    if (activeToolResult.value) { closeToolResult(); return }
    close()
  }
}

function getUserInitial() {
  return props.userName ? props.userName.charAt(0).toUpperCase() : '👤'
}

function open() {
  isOpen.value = true
  nextTick(() => {
    if (activeTab.value === 'qa') inputEl.value?.focus()
    else loadSuggestions()
  })
}
function close() { isOpen.value = false; closeToolResult(); emit('close') }
function toggleFullscreen() { isFullscreen.value = !isFullscreen.value }
function clearChat() { messages.value = [] }
function newChat() {
  messages.value = []
  activeTab.value = 'qa'
  input.value = ''
  toolResults.value = {}
  closeToolResult()
  loadSuggestions()
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'qa') {
    closeToolResult()
  }
}

function closeToolResult() {
  activeToolResult.value = null
  toolRunning.value = false
}

// ===== 推荐问题：动态获取 =====
async function loadSuggestions() {
  if (!props.token || !actualTeacherId.value) {
    currentSuggests.value = [
      '如何解读学生的风险等级？',
      '学业预警系统的红橙黄三级分别代表什么？',
      '常用预警干预措施有哪些？',
      '教师收到预警后应采取哪些行动？'
    ]
    return
  }

  suggestLoading.value = true
  try {
    const json = await teacherAgentSuggestions(props.token, actualTeacherId.value, actualCourseId.value)
    if (json.success && json.suggestions?.length) {
      currentSuggests.value = json.suggestions
    } else {
      // 兜底
      currentSuggests.value = [
        '如何解读学生的风险等级？',
        '学业预警系统的红橙黄三级分别代表什么？',
        '教师收到预警后应采取哪些行动？',
        '如何根据学生薄弱点推荐练习题？'
      ]
    }
  } catch {
    currentSuggests.value = [
      '如何解读学生的风险等级？',
      '学业预警系统的红橙黄三级分别代表什么？',
      '教师收到预警后应采取哪些行动？'
    ]
  } finally {
    suggestLoading.value = false
  }
}

function sendSuggestion(q) {
  input.value = q
  nextTick(() => send())
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

// ===== 知识问答（保留原 RAG 功能） =====
async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const json = await teacherAgentQA(props.token, text)
    if (json.success) {
      const reply = json.reply || json.message || '(未获取到回答)'
      const cites = json.citations || []
      messages.value.push({
        role: 'assistant',
        content: reply,
        // W3 引用溯源：把 citations 映射为参考来源标签
        sources: cites.map(c => (c.index ? `[${c.index}] ` : '') + (c.title || '依据')),
        grounded: json.grounded,
        retrievalNote: json.retrievalNote
      })
    } else {
      messages.value.push({ role: 'assistant', content: '抱歉：' + (json.message || '未知错误') })
    }
  } catch {
    messages.value.push({ role: 'assistant', content: '网络连接异常，请确认后端服务已启动后重试。' })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

// ===== 工具调用：真实 API =====
async function runTool(toolKey) {
  if (!actualCourseId.value || toolRunning.value) return

  // 如果已有缓存结果，直接展示（不设 toolRunning，不走请求）
  if (toolResults.value[toolKey]) {
    activeToolResult.value = toolKey
    return
  }

  toolRunning.value = true
  activeToolResult.value = toolKey

  try {
    let json
    switch (toolKey) {
      case 'riskOverview':
        json = await teacherAgentRiskOverview(props.token, actualTeacherId.value, actualCourseId.value)
        break
      case 'weakPoints':
        json = await teacherAgentWeakPoints(props.token, actualTeacherId.value, actualCourseId.value)
        break
      case 'alertRules':
        json = await teacherAgentAlertRules(props.token, actualCourseId.value)
        break
      case 'interventionPlan':
        json = await teacherAgentInterventionPlan(props.token, actualTeacherId.value, actualCourseId.value)
        break
      case 'teachingAdvice':
        json = await teacherAgentTeachingAdvice(props.token, actualTeacherId.value, actualCourseId.value)
        break
    }

    if (json?.success) {
      toolResults.value = { ...toolResults.value, [toolKey]: json }
    } else {
      toolResults.value = { ...toolResults.value, [toolKey]: { error: json?.message || '工具调用失败' } }
    }
  } catch (e) {
    toolResults.value = { ...toolResults.value, [toolKey]: { error: '网络异常：' + e.message } }
  } finally {
    toolRunning.value = false
  }
}

// 工具结果计算
const activeToolTitle = computed(() => {
  const titles = {
    riskOverview: '课程风险概览',
    weakPoints: '知识点薄弱项',
    alertRules: '预警规则说明',
    interventionPlan: '干预方案',
    teachingAdvice: '教学建议'
  }
  return titles[activeToolResult.value] || ''
})

function riskCount(level) {
  const d = toolResults.value.riskOverview?.alertDistribution?.[level]
  return d?.count ?? 0
}
function riskPercent(level) {
  const total = toolResults.value.riskOverview?.totalStudents || 1
  const count = riskCount(level)
  return Math.max(2, Math.round((count / total) * 100))
}

async function scrollToBottom() {
  await nextTick()
  if (chatBody.value) {
    chatBody.value.scrollTop = chatBody.value.scrollHeight
  }
}

defineExpose({ open, close, isOpen })
</script>

<style scoped>
/* ============================================================
   AI 侧边面板 - 参考 Linear / Salesforce Agentforce 设计
   ============================================================ */

.ai-sidebar {
  position: relative;
  display: flex;
  flex-shrink: 0;
  height: 100%;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 收起时：窄触发条 */
.ai-toggle-strip {
  width: 44px;
  height: 100%;
  background: transparent;
  border-left: 1px solid transparent;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 16px;
  transition: all 0.2s;
  position: relative;
}
.ai-toggle-strip:hover {
  background: rgba(148, 106, 66, 0.04);
  border-left-color: rgba(148, 106, 66, 0.15);
}
.ai-toggle-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 8px;
  border-radius: 8px;
  background: rgba(148, 106, 66, 0.06);
  transition: all 0.2s;
}
.ai-toggle-strip:hover .ai-toggle-inner {
  background: rgba(148, 106, 66, 0.12);
  transform: translateX(-2px);
}
.ai-toggle-icon { width: 22px; height: 22px; color: #8b7a65; }
.ai-toggle-label { font-size: 0.65rem; font-weight: 600; color: #8b7a65; letter-spacing: 1px; writing-mode: vertical-rl; }

/* 展开面板 */
.ai-panel {
  width: 380px;
  display: flex;
  flex-direction: column;
  background: #fdfcfa;
  border-left: 1px solid #e8e0d0;
  box-shadow: -4px 0 24px rgba(0,0,0,0.04);
  animation: panelSlideIn 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}
.ai-panel.fullscreen {
  position: fixed; top: 0; right: 0; bottom: 0;
  z-index: 1000; width: 50vw; min-width: 500px;
  border-left: 1px solid #e0d5c0;
  box-shadow: -8px 0 40px rgba(0,0,0,0.08);
}
@keyframes panelSlideIn {
  from { transform: translateX(20px); opacity: 0.8; }
  to { transform: translateX(0); opacity: 1; }
}

/* 面板头部 */
.ai-panel-header {
  padding: 14px 16px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #f0ebe0;
  background: linear-gradient(180deg, #fdfcfa 0%, #f8f4ec 100%);
  flex-shrink: 0;
}
.ai-panel-header-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.ai-panel-logo { width: 22px; height: 22px; color: #956a42; flex-shrink: 0; }
.ai-panel-title { font-size: 0.95rem; font-weight: 700; color: #3a3028; white-space: nowrap; }

/* Tab 切换 */
.ai-tabs { display: flex; gap: 2px; margin-left: 12px; background: #f0ebe0; border-radius: 7px; padding: 2px; }
.ai-tab {
  display: flex; align-items: center; gap: 4px; padding: 5px 10px;
  border: none; border-radius: 5px; background: transparent;
  color: #8b7a65; font-size: 0.75rem; font-weight: 500; cursor: pointer;
  transition: all 0.15s; white-space: nowrap;
}
.ai-tab svg { width: 13px; height: 13px; }
.ai-tab.active { background: #fff; color: #3a3028; box-shadow: 0 1px 2px rgba(0,0,0,0.06); }
.ai-tab:hover:not(.active) { color: #5c4a3a; }

.ai-panel-header-right { display: flex; gap: 2px; flex-shrink: 0; }
.ai-header-btn {
  width: 30px; height: 30px; border: none; border-radius: 6px;
  background: transparent; color: #8b7a65; cursor: pointer;
  font-size: 0.95rem; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.ai-header-btn svg { width: 14px; height: 14px; }
.ai-header-btn:hover { background: #f0ebe0; color: #5c4a3a; }
.ai-close-btn:hover { background: #fce4e4; color: #c42e2e; }

/* ============================================================
   快捷工具面板
   ============================================================ */

.ai-current-course {
  padding: 8px 16px; border-radius: 8px; background: #eff6ff; color: #1e40af;
  font-size: 0.82rem; font-weight: 600; margin-bottom: 8px; text-align: center;
}
.ai-current-course.ai-no-course { background: #fefce8; color: #854d0e; }

.ai-tools-panel {
  flex: 1; overflow-y: auto; padding: 16px 20px;
  display: flex; flex-direction: column; gap: 8px;
}
.ai-tools-intro { margin-bottom: 4px; }
.ai-tools-title { font-size: 0.95rem; font-weight: 700; color: #3a3028; margin: 0 0 2px; }
.ai-tools-desc { font-size: 0.78rem; color: #8b7a65; margin: 0; }

.ai-tool-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border: 1px solid #e8e0d0; border-radius: 10px;
  background: #fff; cursor: pointer; transition: all 0.15s; text-align: left;
}
.ai-tool-card:hover:not(.disabled) {
  border-color: #956a42; background: #fdf9f4;
  box-shadow: 0 2px 8px rgba(148, 106, 66, 0.08); transform: translateX(3px);
}
.ai-tool-card.disabled { opacity: 0.45; cursor: not-allowed; }
.ai-tool-icon { font-size: 1.2rem; flex-shrink: 0; }
.ai-tool-label { flex: 1; font-size: 0.83rem; font-weight: 500; color: #3a3028; }
.ai-tool-status { font-size: 0.75rem; color: #22c55e; font-weight: 700; }
.ai-tool-arrow { color: #c0b090; font-size: 0.85rem; opacity: 0; transition: opacity 0.15s; }
.ai-tool-card:hover:not(.disabled) .ai-tool-arrow { opacity: 1; color: #956a42; }
.ai-tools-footer { margin-top: 4px; font-size: 0.68rem; color: #b0a590; text-align: center; }

/* ============================================================
   工具结果展示
   ============================================================ */
.ai-tool-result {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
}
.ai-tool-result-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 8px; border-bottom: 1px solid #f0ebe0;
}
.ai-tool-result-title { font-size: 0.92rem; font-weight: 700; color: #3a3028; }
.ai-tool-result-close {
  border: none; background: none; font-size: 1rem; color: #b0a590; cursor: pointer;
}
.ai-tool-result-close:hover { color: #c42e2e; }
.ai-tool-result-body { display: flex; flex-direction: column; gap: 12px; }

.ai-tool-error {
  padding: 14px 16px; background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 10px; color: #dc2626; font-size: 0.82rem; line-height: 1.6;
}

/* 风险分布条 */
.ai-risk-distribution { display: flex; gap: 4px; height: 32px; border-radius: 8px; overflow: hidden; }
.ai-risk-bar {
  display: flex; align-items: center; justify-content: center;
  transition: width 0.4s ease; min-width: 0; font-size: 0.7rem; font-weight: 600;
}
.ai-risk-bar.red { background: #fef2f2; color: #dc2626; }
.ai-risk-bar.orange { background: #fff7ed; color: #ea580c; }
.ai-risk-bar.yellow { background: #fefce8; color: #ca8a04; }
.ai-risk-bar.green { background: #f0fdf4; color: #16a34a; }
.ai-risk-bar-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding: 0 4px; }
.ai-total-students { font-size: 0.8rem; color: #8b7a65; text-align: center; margin: 4px 0; }

/* 风险学生列表 */
.ai-risk-list { display: flex; flex-direction: column; gap: 6px; }
.ai-risk-list h4, .ai-analysis-text h4 {
  font-size: 0.82rem; font-weight: 700; color: #3a3028; margin: 4px 0 6px;
}
.ai-risk-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #faf8f5; border-radius: 8px; font-size: 0.8rem;
}
.ai-risk-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ai-dot-red { background: #ef4444; }
.ai-dot-orange { background: #f97316; }
.ai-dot-yellow { background: #eab308; }
.ai-dot-green { background: #22c55e; }
.ai-risk-type { color: #8b7a65; flex: 1; }
.ai-risk-score { font-weight: 700; color: #dc2626; }

/* AI 分析文字 */
.ai-analysis-text { background: #f8f4ec; border-radius: 10px; padding: 12px 14px; font-size: 0.82rem; line-height: 1.7; color: #3a3028; }
.ai-analysis-text pre { white-space: pre-wrap; font-family: inherit; margin: 0; font-size: 0.82rem; line-height: 1.7; }

/* 薄弱知识点列表 */
.ai-weak-list { display: flex; flex-direction: column; gap: 6px; }
.ai-weak-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: #faf8f5; border-radius: 8px;
}
.ai-weak-rank {
  width: 24px; height: 24px; border-radius: 50%;
  background: #956a42; color: #fff; display: flex; align-items: center;
  justify-content: center; font-size: 0.72rem; font-weight: 700; flex-shrink: 0;
}
.ai-weak-info { display: flex; flex-direction: column; gap: 2px; }
.ai-weak-info strong { font-size: 0.84rem; color: #3a3028; }
.ai-weak-meta { font-size: 0.72rem; color: #8b7a65; }

/* 规则说明 */
.ai-rule-explanation { background: #f8f4ec; border-radius: 10px; padding: 12px 14px; font-size: 0.82rem; line-height: 1.7; color: #3a3028; }
.ai-rule-explanation pre { white-space: pre-wrap; font-family: inherit; margin: 0; font-size: 0.8rem; line-height: 1.7; }

.ai-thinking-bar { display: flex; gap: 4px; padding: 12px; justify-content: center; }
.ai-thinking-bar .ai-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #c0b090;
  animation: dotBounce 1.4s infinite ease-in-out both;
}
.ai-thinking-bar .ai-dot:nth-child(1) { animation-delay: -0.32s; }
.ai-thinking-bar .ai-dot:nth-child(2) { animation-delay: -0.16s; }

/* ============================================================
   对话区（复用原样式）
   ============================================================ */
.ai-panel-body {
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 16px; scroll-behavior: smooth;
}
.ai-panel-body::-webkit-scrollbar { width: 5px; }
.ai-panel-body::-webkit-scrollbar-thumb { background: #e0d5c0; border-radius: 3px; }
.ai-panel-body::-webkit-scrollbar-track { background: transparent; }

.ai-welcome { text-align: center; padding: 24px 12px 8px; }
.ai-welcome-icon { width: 56px; height: 56px; margin: 0 auto 12px; color: #956a42; }
.ai-welcome-icon svg { width: 100%; height: 100%; }
.ai-welcome-title { font-size: 1rem; font-weight: 700; color: #3a3028; margin: 0 0 4px; }
.ai-welcome-desc { font-size: 0.78rem; color: #8b7a65; margin: 0 0 16px; }

.ai-suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 360px; margin: 0 auto; }
.ai-suggestions-hint { width: 100%; font-size: 0.72rem; color: #b0a590; margin: 0 0 4px; text-align: center; }
.ai-suggest-chip {
  font-size: 0.76rem; padding: 7px 13px; border-radius: 20px;
  border: 1px solid #e8e0d0; background: #fff; color: #5c4a3a;
  cursor: pointer; transition: all 0.15s; line-height: 1.4;
}
.ai-suggest-chip:hover:not(:disabled) {
  border-color: #956a42; color: #956a42; background: #fdf9f4;
  box-shadow: 0 1px 4px rgba(148, 106, 66, 0.08);
}
.ai-suggest-chip:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-suggest-loading { padding: 8px 0; }

/* 消息样式 */
.ai-message { display: flex; gap: 10px; animation: msgFadeIn 0.25s ease; }
.ai-message.user { flex-direction: row-reverse; }
@keyframes msgFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.ai-msg-avatar {
  width: 30px; height: 30px; border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.ai-message.assistant .ai-msg-avatar { background: #f5ede0; color: #956a42; padding: 4px; }
.ai-message.user .ai-msg-avatar { background: linear-gradient(135deg, #956a42, #7d5635); }
.ai-msg-avatar-user {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%; color: #fff; font-size: 0.75rem; font-weight: 700;
}
.ai-msg-content { max-width: 82%; min-width: 0; }
.ai-msg-bubble {
  padding: 10px 14px; border-radius: 14px; font-size: 0.85rem;
  line-height: 1.6; word-break: break-word; white-space: pre-wrap;
}
.ai-message.assistant .ai-msg-bubble {
  background: #f8f4ec; color: #3a3028; border: 1px solid #f0ebe0; border-top-left-radius: 4px;
}
.ai-message.user .ai-msg-bubble {
  background: linear-gradient(135deg, #956a42, #7d5635); color: #fff; border-top-right-radius: 4px;
}

.ai-msg-sources { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.ai-source-label { font-size: 0.68rem; color: #b0a590; }
.ai-source-tag { font-size: 0.66rem; padding: 2px 8px; border-radius: 4px; background: #fdf9f4; border: 1px solid #f0ebe0; color: #8b7a65; }
.ai-msg-nocite { margin-top: 6px; font-size: 0.68rem; color: #b45309; background: #fffbeb; border: 1px solid #fde68a; border-radius: 6px; padding: 3px 8px; }

.ai-thinking { display: flex; gap: 4px; align-items: center; padding: 12px 16px; }
.ai-thinking .ai-dot, .ai-suggest-loading .ai-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #c0b090;
  animation: dotBounce 1.4s infinite ease-in-out both;
  display: inline-block;
}
.ai-thinking .ai-dot:nth-child(1), .ai-suggest-loading .ai-dot:nth-child(1) { animation-delay: -0.32s; }
.ai-thinking .ai-dot:nth-child(2), .ai-suggest-loading .ai-dot:nth-child(2) { animation-delay: -0.16s; }
.ai-suggest-loading { display: flex; gap: 4px; justify-content: center; width: 100%; }

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.6); }
  40% { transform: scale(1); }
}

/* 输入区 */
.ai-panel-footer {
  padding: 12px 16px; border-top: 1px solid #f0ebe0;
  background: #fdfcfa; flex-shrink: 0;
}
.ai-input-row { display: flex; gap: 8px; align-items: flex-end; }
.ai-input-btn {
  width: 34px; height: 34px; border: none; border-radius: 8px;
  background: transparent; color: #b0a590; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.ai-input-btn svg { width: 15px; height: 15px; }
.ai-input-btn:hover { background: #f0ebe0; color: #6b5e4f; }
.ai-input-wrapper { flex: 1; min-width: 0; }
.ai-input {
  width: 100%; padding: 10px 14px; border: 1px solid #e8e0d0;
  border-radius: 10px; font-size: 0.85rem; line-height: 1.5;
  color: #3a3028; background: #fff; outline: none; resize: none;
  font-family: inherit; transition: border-color 0.15s, box-shadow 0.15s;
  max-height: 120px; box-sizing: border-box;
}
.ai-input:focus { border-color: #956a42; box-shadow: 0 0 0 3px rgba(148,106,66,0.08); }
.ai-input::placeholder { color: #c0b090; }
.ai-input:disabled { background: #faf8f5; color: #b0a590; }
.ai-send-btn {
  width: 36px; height: 36px; border: none; border-radius: 10px;
  background: #e8e0d0; color: #b0a590; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.ai-send-btn svg { width: 14px; height: 14px; }
.ai-send-btn.active { background: #956a42; color: #fff; }
.ai-send-btn.active:hover { background: #7d5635; }
.ai-send-btn:disabled { cursor: not-allowed; }
.ai-send-spinner {
  width: 14px; height: 14px; border: 2px solid transparent;
  border-top-color: #b0a590; border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.ai-footer-hint { margin: 8px 0 0; font-size: 0.65rem; color: #c0b090; text-align: center; }
</style>
