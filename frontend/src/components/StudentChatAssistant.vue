<template>
  <div class="sai-sidebar" :class="{ open: isOpen }" v-if="isLoggedIn">
    <!-- 收起时的触发条 -->
    <div v-if="!isOpen" class="sai-toggle-strip" @click="open" title="打开学习助手">
      <div class="sai-toggle-inner">
        <svg class="sai-toggle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z"/>
          <circle cx="8.5" cy="9.5" r="1.5"/><circle cx="15.5" cy="9.5" r="1.5"/>
          <path d="M8 14c0 0 1.5 2 4 2s4-2 4-2" stroke-linecap="round"/>
        </svg>
        <span class="sai-toggle-label">学习助手</span>
      </div>
    </div>

    <!-- 展开面板 -->
    <div v-if="isOpen" class="sai-panel">
      <!-- 面板头部 -->
      <div class="sai-panel-header">
        <div class="sai-panel-header-left">
          <svg class="sai-panel-logo" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2z"/>
            <circle cx="8.5" cy="9.5" r="1.5"/><circle cx="15.5" cy="9.5" r="1.5"/>
            <path d="M8 14c0 0 1.5 2 4 2s4-2 4-2" stroke-linecap="round"/>
          </svg>
          <span class="sai-panel-title">学习助手</span>
        </div>
        <div class="sai-panel-header-right">
          <button class="sai-header-btn" @click="newChat" title="新对话">＋</button>
          <button class="sai-header-btn sai-close-btn" @click="close" title="关闭面板">✕</button>
        </div>
      </div>

      <!-- 课程选择器 -->
      <div v-if="courses.length > 1" class="sai-course-selector">
        <select v-model="selectedCourseId" @change="onCourseChange">
          <option :value="null">全部课程</option>
          <option v-for="c in courses" :key="c.courseId" :value="c.courseId">{{ c.courseName }}</option>
        </select>
      </div>

      <!-- 聊天消息区 -->
      <div class="sai-chat-area" ref="chatArea">
        <div v-if="messages.length === 0 && !loading" class="sai-empty">
          <div class="sai-empty-icon">🎓</div>
          <div class="sai-empty-text">你好！我是你的学习助手</div>
          <div class="sai-empty-desc">可以问我学习状态、薄弱知识点、学习建议等</div>
        </div>

        <div v-for="(msg, i) in messages" :key="i" class="sai-message" :class="msg.role">
          <div class="sai-msg-avatar">{{ msg.role === 'user' ? (userName?.[0] || '我') : '🤖' }}</div>
          <div class="sai-msg-content">
            <pre class="sai-msg-text">{{ msg.content }}</pre>
          </div>
        </div>

        <div v-if="loading" class="sai-message assistant">
          <div class="sai-msg-avatar">🤖</div>
          <div class="sai-msg-content">
            <div class="sai-typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 推荐问题 -->
      <div v-if="suggestions.length > 0 && messages.length === 0" class="sai-suggestions">
        <div class="sai-suggestions-title">💡 你可能想问：</div>
        <button v-for="(s, i) in suggestions" :key="i" class="sai-suggestion-btn"
                @click="askQuestion(s)" :disabled="loading">
          {{ s }}
        </button>
      </div>

      <!-- 输入区 -->
      <div class="sai-input-area">
        <textarea v-model="inputText" class="sai-input"
                  placeholder="输入你的问题... (Enter 发送，Shift+Enter 换行)"
                  :disabled="loading"
                  @keydown.enter.exact.prevent="sendMessage"
                  rows="1" ref="inputEl"></textarea>
        <button class="sai-send-btn" @click="sendMessage" :disabled="loading || !inputText.trim()">
          <svg viewBox="0 0 16 16" fill="currentColor"><path d="M1 1l14 7-14 7 3-7z"/></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { studentAgentChat, studentAgentCourses, studentAgentSuggestions } from '../api/http.js'

const emit = defineEmits(['close'])

const props = defineProps({
  token: String,
  userName: String,
  studentId: Number
})

const isOpen = ref(false)
const isLoggedIn = ref(false)
const loading = ref(false)
const inputText = ref('')
const messages = ref([])
const suggestions = ref([])
const courses = ref([])
const selectedCourseId = ref(null)
const chatArea = ref(null)
const inputEl = ref(null)

// 监听 token 变化
watch(() => props.token, (val) => {
  isLoggedIn.value = !!val
  if (val) {
    loadCourses()
  } else {
    messages.value = []
    suggestions.value = []
    courses.value = []
  }
}, { immediate: true })

async function loadCourses() {
  try {
    const res = await studentAgentCourses(props.token)
    if (res.success) {
      courses.value = res.courses || []
    }
  } catch (e) {
    console.error('加载课程列表失败', e)
  }
  // 加载推荐问题
  loadSuggestions()
}

async function loadSuggestions() {
  try {
    const res = await studentAgentSuggestions(props.token, selectedCourseId.value)
    if (res.success) {
      suggestions.value = res.suggestions || []
    }
  } catch (e) {
    console.error('加载推荐问题失败', e)
  }
}

function onCourseChange() {
  messages.value = []
  loadSuggestions()
}

function open() {
  isOpen.value = true
  nextTick(() => {
    inputEl.value?.focus()
  })
}

function close() {
  isOpen.value = false
  emit('close')
}

function newChat() {
  messages.value = []
  loadSuggestions()
}

function askQuestion(q) {
  inputText.value = q
  sendMessage()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await studentAgentChat(props.token, text, selectedCourseId.value)
    if (res.success) {
      messages.value.push({ role: 'assistant', content: res.reply })
    } else {
      messages.value.push({ role: 'assistant', content: res.reply || '抱歉，暂时无法回复。' })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '网络异常，请稍后再试。' })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function scrollToBottom() {
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}

// 键盘快捷键
function handleKeydown(e) {
  if (e.ctrlKey && e.key === 'k') {
    e.preventDefault()
    open()
  }
  if (e.key === 'Escape' && isOpen.value) {
    close()
  }
}

// 挂载/卸载键盘事件
import { onMounted, onUnmounted } from 'vue'
onMounted(() => window.addEventListener('keydown', handleKeydown))
onUnmounted(() => window.removeEventListener('keydown', handleKeydown))

defineExpose({ open, close, isOpen })
</script>

<style scoped>
/* 侧边栏容器 — 改为 flex 布局适配全局面板 */
.sai-sidebar {
  position: relative;
  display: flex;
  flex-shrink: 0;
  height: 100%;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 收起触发条 */
.sai-toggle-strip {
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
}
.sai-toggle-strip:hover {
  background: rgba(99, 102, 241, 0.06);
  border-left-color: rgba(99, 102, 241, 0.2);
}
.sai-toggle-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 8px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.08);
  transition: all 0.2s;
}
.sai-toggle-strip:hover .sai-toggle-inner {
  background: rgba(99, 102, 241, 0.16);
  transform: translateX(-2px);
}
.sai-toggle-icon { width: 22px; height: 22px; color: #7c7bef; }
.sai-toggle-label {
  color: #7c7bef;
  font-size: 0.65rem;
  font-weight: 600;
  writing-mode: vertical-rl;
}

/* 面板 */
.sai-panel {
  width: 380px;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e8e8f0;
  animation: saiPanelIn 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}
.sai-panel > * { flex: 0 0 auto; }
.sai-panel > .sai-chat-area { flex: 1 1 0; min-height: 0; }
@keyframes saiPanelIn {
  from { transform: translateX(20px); opacity: 0.8; }
  to { transform: translateX(0); opacity: 1; }
}

/* 头部 */
.sai-panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  flex-shrink: 0;
}
.sai-panel-header-left { display: flex; align-items: center; gap: 8px; }
.sai-panel-logo { width: 22px; height: 22px; color: #fff; }
.sai-panel-title { color: #fff; font-size: 0.95rem; font-weight: 600; }
.sai-panel-header-right { display: flex; gap: 4px; }
.sai-header-btn {
  width: 28px; height: 28px; border: none; border-radius: 6px;
  background: rgba(255,255,255,0.2); color: #fff; cursor: pointer;
  font-size: 0.9rem; display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.sai-header-btn:hover { background: rgba(255,255,255,0.35); }

/* 课程选择器 */
.sai-course-selector {
  padding: 10px 16px; border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.sai-course-selector select {
  width: 100%; padding: 8px 10px; border: 1px solid #e0e0e0; border-radius: 8px;
  font-size: 0.82rem; color: #333; background: #fafafa; cursor: pointer;
}

/* 聊天区 */
.sai-chat-area {
  flex: 1 1 0; min-height: 0;
  overflow-y: auto; padding: 16px;
  padding-bottom: 80px; /* 给绝对定位的输入区留空间 */
  display: flex; flex-direction: column; gap: 12px;
}
.sai-chat-area::-webkit-scrollbar { width: 4px; }
.sai-chat-area::-webkit-scrollbar-thumb { background: #e0e0e0; border-radius: 4px; }

/* 空状态 */
.sai-empty {
  text-align: center; padding: 40px 20px; color: #999;
}
.sai-empty-icon { font-size: 2.5rem; margin-bottom: 12px; }
.sai-empty-text { font-size: 1rem; font-weight: 600; color: #666; margin-bottom: 6px; }
.sai-empty-desc { font-size: 0.82rem; }

/* 消息 */
.sai-message { display: flex; gap: 10px; }
.sai-message.user { flex-direction: row-reverse; }
.sai-msg-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.75rem; font-weight: 600; flex-shrink: 0;
}
.sai-message.user .sai-msg-avatar {
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
}
.sai-message.assistant .sai-msg-avatar {
  background: #f0f0f0; font-size: 1rem;
}
.sai-msg-content { max-width: 78%; }
.sai-message.user .sai-msg-content {
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
  border-radius: 14px 4px 14px 14px; padding: 10px 14px;
}
.sai-message.assistant .sai-msg-content {
  background: #f5f5f5; border-radius: 4px 14px 14px 14px; padding: 10px 14px;
}
.sai-msg-text {
  margin: 0; white-space: pre-wrap; word-break: break-word;
  font-size: 0.85rem; line-height: 1.6; font-family: inherit;
}

/* 输入中动画 */
.sai-typing { display: flex; gap: 4px; padding: 4px 0; }
.sai-typing span {
  width: 6px; height: 6px; border-radius: 50%; background: #bbb;
  animation: sai-typing 1.4s infinite;
}
.sai-typing span:nth-child(2) { animation-delay: 0.2s; }
.sai-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes sai-typing {
  0%,60%,100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* 推荐问题 */
.sai-suggestions {
  padding: 0 16px 10px; border-top: 1px solid #f5f5f5;
  flex: 0 0 auto;
  max-height: 180px;
  overflow-y: auto;
}
.sai-suggestions-title {
  font-size: 0.78rem; color: #999; margin-bottom: 8px; font-weight: 500;
}
.sai-suggestion-btn {
  display: block; width: 100%; text-align: left;
  padding: 8px 12px; margin-bottom: 6px;
  border: 1px solid #e8e0ff; border-radius: 8px;
  background: #faf8ff; color: #6366f1; cursor: pointer;
  font-size: 0.8rem; transition: all 0.15s;
}
.sai-suggestion-btn:hover {
  background: #f0ebff; border-color: #c4b5fd;
}

/* 输入区 - 钉在面板底部 */
.sai-input-area {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex; gap: 8px; padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0; align-items: flex-end;
  z-index: 5;
}
.sai-input {
  flex: 1; border: 1px solid #e0e0e0; border-radius: 10px;
  padding: 10px 14px; font-size: 0.85rem; resize: none;
  outline: none; font-family: inherit; line-height: 1.5;
  max-height: 90px; transition: border-color 0.15s;
}
.sai-input:focus { border-color: #6366f1; }
.sai-send-btn {
  width: 38px; height: 38px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: opacity 0.15s;
}
.sai-send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.sai-send-btn svg { width: 16px; height: 16px; }
</style>
