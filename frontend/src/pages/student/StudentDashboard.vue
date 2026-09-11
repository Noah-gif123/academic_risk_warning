<script setup>
import { ref, computed, watch, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../../composables/useAuth.js'
import { getJson, postJson } from '../../api/http.js'
import {
  getStudentAlerts, acknowledgeAlert, studentRespond,
  getMyRecommends, submitAnswer, getSubQuestions,
  submitAnswerSub, saveDraft, getDraft, deleteDraft,
  studentAgentChat, studentAgentCourses,
  getStudentRadar, getStudentTrend, getMyProfile
} from '../../api/http.js'
import RiskRadarChart from '../../components/RiskRadarChart.vue'
import RiskTrendChart from '../../components/RiskTrendChart.vue'
import ThreeDimProfile from '../../components/ThreeDimProfile.vue'
import { riskLevelFromScore, riskLevelLabel } from '../../utils/riskLevel.js'
// PDF 渲染按需初始化，避免 ESM 导入时崩溃导致整个组件白屏
let pdfjsLib = null
async function ensurePdfJs() {
  if (pdfjsLib) return pdfjsLib
  try {
    pdfjsLib = await import('pdfjs-dist')
    pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://unpkg.com/pdfjs-dist@3.11.174/build/pdf.worker.min.js'
    return pdfjsLib
  } catch (e) {
    console.warn('pdfjs-dist 加载失败，PDF 预览不可用:', e)
    return null
  }
}

const router = useRouter()
const route = useRoute()
const { studentToken, studentInfo, isStudentLoggedIn, studentLogout } = useAuth()

// ============ Tab 管理 ============
const activeTab = ref('info') // info | warnings | exercises | goals

// 根据路由同步 activeTab，避免组件复用时 Tab 状态错乱导致白屏
watch(() => route.path, () => {
  const tab = route.meta?.tab
  if (tab) {
    activeTab.value = tab
  } else if (route.path === '/student/info') {
    activeTab.value = 'info'
  }
  // 切换 tab 时按需加载对应数据（nextTick 确保 watch 触发时其它 ref 已初始化）
  nextTick(() => {
    if (activeTab.value === 'warnings' && !studentAlerts.value.length) {
      loadStudentAlerts()
    } else if (activeTab.value === 'exercises' && !myRecommends.value.length) {
      loadMyRecommends()
    } else if (activeTab.value === 'goals') {
      loadStudentGoals()
    }
  })
}, { immediate: true })
const message = ref('')
const messageType = ref('info')

function setMessage(text, type = 'info') { message.value = text; messageType.value = type; setTimeout(() => { if (message.value === text) message.value = '' }, 3000) }

// ============ 学情数据 ============
const studentData = ref(null)
const loading = ref(false)

// 选中的课程 ID（用于详细学情）
const selectedCourseId = ref(null)
// 切换课程时重新计算各模块
const currentCourseScore = computed(() =>
  (studentData.value?.scores || []).find(s => s.courseId === selectedCourseId.value))
const currentCourseHomework = computed(() =>
  (studentData.value?.homeworkList || []).find(h => h.courseId === selectedCourseId.value))
const currentCoursePerformance = computed(() =>
  (studentData.value?.classPerformances || []).find(p => p.courseId === selectedCourseId.value))
const currentCourseHistoryRisk = computed(() =>
  (studentData.value?.historyRisks || []).find(r => r.courseId === selectedCourseId.value))
const currentCourseKnowledge = computed(() =>
  (studentData.value?.knowledgeMasteries || []).find(k => k.courseId === selectedCourseId.value))
const currentCourseWeakPoints = computed(() => {
  const list = studentData.value?.weakPoints || []
  if (!selectedCourseId.value) return list.slice(0, 10)
  return list.filter(wp => Number(wp.courseId) === Number(selectedCourseId.value)).slice(0, 10)
})

// 风险等级（用于风险画像标签）：阈值口径统一收敛到 utils/riskLevel.js
function getAlertLevel() {
  return riskLevelFromScore(currentCourseScore.value?.calculatedScore)
}

function emptyText(text) {
  return text && String(text).trim() ? String(text).trim() : '暂无记录'
}

// 数据加载完后默认选中第一门课程
watch(studentData, (val) => {
  if (val && !selectedCourseId.value) {
    const firstCourse = (val.scores || [])[0] || (val.homeworkList || [])[0] ||
      (val.classPerformances || [])[0] || (val.knowledgeMasteries || [])[0]
    if (firstCourse) selectedCourseId.value = firstCourse.courseId
  }
})

onMounted(() => {
  if (!isStudentLoggedIn.value) { router.replace('/student/login'); return }
  loadStudentData()
  loadNotifications()
})

async function loadStudentData() {
  loading.value = true
  studentData.value = null
  try {
    const res = await getJson('/student/my-info', studentToken.value)
    console.log('[StudentDashboard] my-info response:', res)
    if (res.success) {
      if (res.data) {
        studentData.value = res.data
      } else {
        setMessage('暂无学情数据，请稍后重试', 'warning')
        console.warn('[StudentDashboard] my-info 返回成功但 data 为空')
      }
    } else {
      setMessage(res.message || '查询失败', 'error')
      console.warn('[StudentDashboard] my-info 返回失败:', res.message)
    }
  } catch (e) {
    setMessage('查询失败，请检查网络或后端服务', 'error')
    console.error('[StudentDashboard] my-info 请求异常:', e)
  }
  finally { loading.value = false }
}

// ============ 通知 ============
const notifications = ref([])
const unreadCount = ref(0)

async function loadNotifications() {
  try {
    const [listRes, countRes] = await Promise.all([
      postJson('/notification/list', { userType: 'STUDENT' }, studentToken.value),
      postJson('/notification/unread-count', { userType: 'STUDENT' }, studentToken.value)
    ])
    if (listRes.success) notifications.value = listRes.data || []
    if (countRes.success) unreadCount.value = countRes.data || 0
  } catch {}
}

// ============ 预警 ============
const studentAlerts = ref([])
const alertLoading = ref(false)
const showAlertModal = ref(false)
const selectedAlert = ref(null)

async function loadStudentAlerts() {
  if (!studentToken.value) return
  alertLoading.value = true
  try {
    const res = await getStudentAlerts(studentToken.value, null)
    if (res.success) studentAlerts.value = res.data || []
  } catch {}
  finally { alertLoading.value = false }
}

function openAlertDetail(alert) { selectedAlert.value = alert; showAlertModal.value = true }

async function confirmStudentAck(alert) {
  const confirmed = confirm(`确认收到预警：${alert.alertTypeName}？`)
  if (!confirmed) return
  try {
    const res = await acknowledgeAlert(studentToken.value, alert.id)
    if (res.success) { setMessage('已确认预警', 'success'); await loadStudentAlerts() }
    else setMessage(res.message || '确认失败', 'error')
  } catch { setMessage('确认失败', 'error') }
}

async function studentRespondToAlert(alert) {
  const response = prompt('请填写你的改进情况：', '正在按老师要求积极改进中')
  if (!response?.trim()) return
  try {
    const res = await studentRespond(studentToken.value, alert.id, response.trim())
    if (res.success) { setMessage('回应成功', 'success'); await loadStudentAlerts() }
    else setMessage(res.message || '回应失败', 'error')
  } catch { setMessage('回应失败', 'error') }
}

// ============ 练习 ============
const myRecommends = ref([])
const recLoading = ref(false)
const filterStatus = ref('ALL')
const filteredRecommends = computed(() => {
  if (filterStatus.value === 'ALL') return myRecommends.value
  if (filterStatus.value === 'COMPLETED') return myRecommends.value.filter(r => r.status === 'COMPLETED' || r.status === 'GRADED')
  return myRecommends.value.filter(r => r.status === filterStatus.value)
})

const showAnswerModal = ref(false)
const currentRecommend = ref(null)
const studentAnswer = ref('')
const subQuestions = ref([])
const subAnswers = ref({})
const currentQIndex = ref(0)
const pdfZoom = ref(1.0)
const pdfCurrentPage = ref(1)
const pdfTotalPages = ref(0)
const pdfLoadingDoc = ref(false)
let pdfDocInstance = null
const pdfCanvasWrap = ref(null)

async function loadMyRecommends() {
  recLoading.value = true
  try {
    const res = await getMyRecommends(studentToken.value)
    if (res.success) myRecommends.value = res.data?.records || []
  } catch {} finally { recLoading.value = false }
}

async function openStudentAnswer(rec) {
  currentRecommend.value = rec
  subAnswers.value = {}
  subQuestions.value = []
  currentQIndex.value = 0
  studentAnswer.value = ''
  pdfZoom.value = 1.4   /* 默认放大到 140%，更清晰 */
  pdfCurrentPage.value = 1
  pdfTotalPages.value = 0
  showAnswerModal.value = true

  /* 先等模版渲染完成，再渲染 PDF */
  if (rec.fileUrl) { await nextTick(); loadAndRenderPdf(rec.fileUrl) }

  try {
    const sqRes = await getSubQuestions(studentToken.value, rec.exerciseId)
    if (sqRes.success && sqRes.data?.length) {
      subQuestions.value = sqRes.data
      const newAnswers = {}; sqRes.data.forEach(sq => newAnswers[sq.id] = ''); subAnswers.value = newAnswers
    }
  } catch {}


  // 草稿恢复
  try {
    const backendRes = await getDraft(studentToken.value, rec.id)
    if (backendRes.success && backendRes.data?.answer) {
      studentAnswer.value = backendRes.data.answer
      if (backendRes.data.subAnswers) subAnswers.value = JSON.parse(backendRes.data.subAnswers)
      return
    }
  } catch {}
  const draftKey = `draft_${rec.id}`
  const saved = localStorage.getItem(draftKey)
  if (saved) {
    try {
      const d = JSON.parse(saved)
      if (d.text) studentAnswer.value = d.text
      if (d.subAnswers) subAnswers.value = d.subAnswers
    } catch { localStorage.removeItem(draftKey) }
  }
}

// 子题导航
function goPrevQuestion() {
  if (currentQIndex.value > 0) currentQIndex.value--
}
function goNextQuestion() {
  if (currentQIndex.value < subQuestions.value.length - 1) currentQIndex.value++
}

async function handleSubmitAnswers() {
  if (!currentRecommend.value) return
  try {
    let res
    if (subQuestions.value.length) {
      res = await submitAnswerSub(studentToken.value, currentRecommend.value.id, { ...subAnswers.value })
    } else {
      res = await submitAnswer(studentToken.value, currentRecommend.value.id, studentAnswer.value.trim())
    }
    if (res.success) {
      showAnswerModal.value = false
      localStorage.removeItem(`draft_${currentRecommend.value.id}`)
      studentAnswer.value = ''; subAnswers.value = {}
      setMessage('提交成功', 'success'); await loadMyRecommends()
    } else setMessage(res.message || '提交失败', 'error')
  } catch { setMessage('提交失败', 'error') }
}

// ============ 学习目标 ============
const studentGoal = ref(null)
const goalLoading = ref(false)
const studentCourses = ref([])
const goalCourseId = ref(null)
const goalLevel = ref('冲刺')
const goalScore = ref(null)
const goalMilestones = ref('')
const goalNote = ref('')
const goalSaving = ref(false)

async function loadStudentGoals() {
  goalLoading.value = true
  try {
    const coursesRes = await studentAgentCourses(studentToken.value)
    if (coursesRes.success) studentCourses.value = coursesRes.data || []
    if (!studentCourses.value.length) { studentGoal.value = null; return }
    const courseId = goalCourseId.value || studentCourses.value[0].courseId
    goalCourseId.value = courseId
    const res = await getJson(`/student/goal?courseId=${courseId}`, studentToken.value)
    if (res.success) {
      studentGoal.value = res.data
      if (res.data) {
        goalLevel.value = res.data.goalLevel || '冲刺'
        goalScore.value = res.data.targetScore || null
        goalMilestones.value = res.data.milestones || ''
        goalNote.value = res.data.note || ''
      }
    }
  } catch {} finally { goalLoading.value = false }
}

async function saveStudentGoal() {
  if (!goalLevel.value) { setMessage('请选择目标等级', 'error'); return }
  goalSaving.value = true
  try {
    const res = await postJson('/student/goal', {
      courseId: goalCourseId.value, goalLevel: goalLevel.value,
      targetScore: goalScore.value ? parseInt(goalScore.value) : null,
      milestones: goalMilestones.value || null, note: goalNote.value || null
    }, studentToken.value)
    if (res.success) { studentGoal.value = res.data; setMessage('目标设置成功', 'success') }
    else setMessage(res.message || '设置失败', 'error')
  } catch { setMessage('网络错误', 'error') }
  finally { goalSaving.value = false }
}

// ============ PDF 渲染（按需加载 pdfjs-dist） ============
async function loadAndRenderPdf(url) {
  if (!url) return
  const lib = await ensurePdfJs()
  if (!lib) return
  pdfLoadingDoc.value = true; pdfTotalPages.value = 0; pdfCurrentPage.value = 1
  try {
    const loadingTask = lib.getDocument(url)
    pdfDocInstance = await loadingTask.promise
    pdfTotalPages.value = pdfDocInstance.numPages
    await renderCurrentPage()
  } catch {} finally { pdfLoadingDoc.value = false }
}

async function renderCurrentPage() {
  if (!pdfDocInstance || !pdfCanvasWrap.value) return
  pdfCanvasWrap.value.innerHTML = ''
  try {
    const page = await pdfDocInstance.getPage(pdfCurrentPage.value)
    const baseViewport = page.getViewport({ scale: 1.0 })
    const containerW = pdfCanvasWrap.value.clientWidth - 12
    const baseScale = Math.max(0.3, containerW / baseViewport.width)
    const scale = baseScale * pdfZoom.value
    const viewport = page.getViewport({ scale })
    const canvas = document.createElement('canvas')
    canvas.width = viewport.width; canvas.height = viewport.height
    canvas.style.display = 'block'
    const ctx = canvas.getContext('2d')
    await page.render({ canvasContext: ctx, viewport }).promise
    pdfCanvasWrap.value.appendChild(canvas)
  } catch {}
}

function scoreColor(score) {
  if (score >= 90) return 'var(--semantic-green)'; if (score >= 80) return '#7c3aed'
  if (score >= 70) return '#0284c7'; if (score >= 60) return '#d97706'
  return 'var(--semantic-red-dark)'
}

function alertLevelColor(l) {
  if (l === 'RED') return 'var(--semantic-red-dark)'; if (l === 'ORANGE') return 'var(--semantic-orange-dark)'
  return 'var(--semantic-yellow-dark)'
}
function alertLevelBg(l) {
  if (l === 'RED') return 'var(--semantic-red-bg)'; if (l === 'ORANGE') return '#fff7ed'
  return '#fefce8'
}

// 监听数据变化
watch(studentData, async (val) => { if (val) await loadStudentAlerts() })

// ============ 风险画像（雷达图 + 趋势图） ============
const showRiskProfile = ref(false)
const profileLoading = ref(false)
const profileError = ref('')
const profileRadar = ref(null)
const profileTrend = ref([])
const profileTrendSource = ref('')
const profileTrendSourceText = ref('')
const profile3d = ref(null)

async function openRiskProfile() {
  const studentId = studentData.value?.basicInfo?.id
  const courseId = selectedCourseId.value
  if (!studentId || !courseId) { setMessage('暂无画像数据，请先选择课程', 'warning'); return }
  showRiskProfile.value = true
  profileLoading.value = true
  profileError.value = ''
  profileRadar.value = null
  profileTrend.value = []
  profileTrendSource.value = ''
  profileTrendSourceText.value = ''
  profile3d.value = null
  try {
    const [radarRes, trendRes, profileRes] = await Promise.all([
      getStudentRadar(studentToken.value, studentId, courseId),
      getStudentTrend(studentToken.value, studentId, courseId),
      getMyProfile(studentToken.value, courseId)
    ])
    if (radarRes?.success && radarRes.data) profileRadar.value = radarRes.data
    if (trendRes?.success) {
      // 新接口返回 {points, source, sourceText}，同时兼容旧的数组返回
      const trendData = trendRes.data || {}
      profileTrend.value = Array.isArray(trendData) ? trendData : (trendData.points || [])
      profileTrendSource.value = Array.isArray(trendData) ? '' : (trendData.source || '')
      profileTrendSourceText.value = Array.isArray(trendData) ? '' : (trendData.sourceText || '')
    }
    // 三维画像可能尚未生成（exists=false），不算加载失败
    profile3d.value = profileRes?.success ? profileRes.data : null
    if (!radarRes?.success || !trendRes?.success) {
      profileError.value = (radarRes?.message || trendRes?.message || '画像数据加载失败') + '（请联系老师或稍后重试）'
    }
  } catch (e) {
    profileError.value = '画像数据加载失败，请检查网络或后端服务'
  } finally { profileLoading.value = false }
}

function closeRiskProfile() { showRiskProfile.value = false }
</script>

<template>
  <div class="page" v-if="isStudentLoggedIn">
    <!-- 学生信息栏 -->
    <section class="toolbar card">
      <div class="user-info">
        <div class="avatar">{{ studentInfo?.studentName?.[0] || 'S' }}</div>
        <div>
          <strong>{{ studentInfo?.studentName || studentInfo?.studentNo }}</strong>
          <span>学号：{{ studentInfo?.studentNo }}</span>
          <span v-if="studentInfo?.grade">年级：{{ studentInfo.grade }}</span>
        </div>
      </div>
      <div class="actions">
        <button class="btn-ghost" @click="studentLogout(); router.push('/')">🚪 退出</button>
      </div>
    </section>

    <p v-if="message" class="msg" :class="messageType">{{ message }}</p>

    <!-- Tab 切换已由 StudentLayout 底部 Tab 栏提供，此处不再重复渲染 -->

    <!-- ====== 学情透视 ====== -->
    <div v-if="activeTab === 'info'" class="student-info-page">
      <!-- 加载中 -->
      <div v-if="loading" class="empty">⏳ 加载学情数据中...</div>

      <!-- 加载失败 -->
      <div v-else-if="!studentData && message" class="empty">
        <p style="color:var(--semantic-red-dark);margin-bottom:12px;">{{ message }}</p>
        <button class="btn-primary" @click="loadStudentData()">🔄 重新加载</button>
      </div>

      <!-- 详情区 -->
      <div v-else-if="studentData" class="detail-body">
        <!-- 学生头部 -->
        <div class="detail-header">
          <div class="student-avatar">{{ studentData?.basicInfo?.studentName?.[0] || '学' }}</div>
          <div class="student-meta">
            <div class="student-name-row">
              <span class="student-name">{{ studentData?.basicInfo?.studentName }}</span>
              <button class="risk-badge profile-entry" :class="getAlertLevel().toLowerCase()"
                      @click="openRiskProfile" title="点击查看我的风险画像图表">📊 风险画像</button>
            </div>
            <div class="student-tags">
              <span class="info-tag">{{ studentData?.basicInfo?.studentNo }}</span>
              <span class="info-tag">{{ studentData?.basicInfo?.grade }}</span>
              <span class="info-tag">{{ studentData?.basicInfo?.className }}</span>
            </div>
          </div>
        </div>

        <!-- 课程选择器 -->
        <div v-if="(studentData?.scores || []).length > 1" class="course-picker">
          <span class="picker-label">📚 切换课程：</span>
          <button v-for="s in studentData.scores" :key="s.courseId"
                  class="course-chip"
                  :class="{ active: selectedCourseId === s.courseId }"
                  @click="selectedCourseId = s.courseId">
            {{ s.courseName }}
          </button>
        </div>

        <!-- 学业成绩 -->
        <div class="detail-card">
          <div class="card-title">学业成绩</div>
          <div v-if="currentCourseScore" class="course-section">
            <div class="course-name">{{ currentCourseScore.courseName }}</div>
            <div class="score-grid">
              <div class="score-block"><span class="label">平时</span><span class="val" :style="{color: scoreColor(currentCourseScore.usualScore)}">{{ currentCourseScore.usualScore ?? '-' }}</span></div>
              <div class="score-block"><span class="label">期中</span><span class="val" :style="{color: scoreColor(currentCourseScore.midScore)}">{{ currentCourseScore.midScore ?? '-' }}</span></div>
              <div class="score-block"><span class="label">期末</span><span class="val" :style="{color: scoreColor(currentCourseScore.finalScore)}">{{ currentCourseScore.finalScore ?? '-' }}</span></div>
              <div class="score-block highlight"><span class="label">综合</span><span class="val" :style="{color: scoreColor(currentCourseScore.calculatedScore)}">{{ currentCourseScore.calculatedScore ?? '-' }}</span></div>
            </div>
          </div>
          <div v-else class="empty-text">暂无成绩数据</div>
        </div>

        <!-- 作业完成 -->
        <div class="detail-card">
          <div class="card-title">作业完成</div>
          <div v-if="currentCourseHomework" class="course-section">
            <div class="course-name">{{ currentCourseHomework.courseName }}</div>
            <div class="homework-row">
              <span class="hw-tag submitted">✓ 已交 {{ currentCourseHomework.submitCount ?? 0 }}</span>
              <span class="hw-tag missing">✗ 未交 {{ currentCourseHomework.notSubmitCount ?? 0 }}</span>
              <span class="hw-tag avg">均分 {{ currentCourseHomework.avgScore ?? '-' }}</span>
            </div>
          </div>
          <div v-else class="empty-text">暂无作业记录</div>
        </div>

        <!-- 课堂表现 -->
        <div class="detail-card">
          <div class="card-title">课堂表现</div>
          <div v-if="currentCoursePerformance" class="course-section">
            <div class="course-name">{{ currentCoursePerformance.courseName }}</div>
            <div class="performance-row">
              <div class="perf-item">
                <span class="perf-label">缺勤</span>
                <span class="perf-val">{{ currentCoursePerformance.absentCount ?? 0 }}/{{ currentCoursePerformance.totalClassTimes ?? '-' }}</span>
              </div>
              <div class="perf-item">
                <span class="perf-label">迟到</span>
                <span class="perf-val">{{ currentCoursePerformance.lateCount ?? 0 }}</span>
              </div>
              <div class="perf-item">
                <span class="perf-label">小测</span>
                <span class="perf-val">{{ currentCoursePerformance.quizScore ?? '-' }}</span>
              </div>
              <div class="perf-item">
                <span class="perf-label">问答</span>
                <span class="perf-val">{{ emptyText(currentCoursePerformance.qaEvaluate) }}</span>
              </div>
              <div class="perf-item">
                <span class="perf-label">态度</span>
                <span class="perf-val">{{ emptyText(currentCoursePerformance.attitudeEvaluate) }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-text">暂无课堂表现记录</div>
        </div>

        <!-- 历史风险 -->
        <div class="detail-card">
          <div class="card-title">历史风险</div>
          <div v-if="currentCourseHistoryRisk" class="history-content">
            <div v-if="currentCourseHistoryRisk.summary" class="history-summary">{{ currentCourseHistoryRisk.summary }}</div>
            <div v-else class="empty-text">暂无历史风险记录</div>
          </div>
          <div v-else class="empty-text">暂无（大一新生无历史记录）</div>
        </div>

        <!-- 知识点掌握 -->
        <div class="detail-card">
          <div class="card-title">知识点掌握</div>
          <div v-if="currentCourseKnowledge" class="course-section">
            <div class="course-name">{{ currentCourseKnowledge.courseName }}</div>
            <div class="knowledge-row">
              <div class="knowledge-item">
                <span class="kn-label">总知识点</span>
                <span class="kn-val">{{ currentCourseKnowledge.totalKnowledge ?? '-' }}</span>
              </div>
              <div class="knowledge-item">
                <span class="kn-label">错题数</span>
                <span class="kn-val">{{ currentCourseKnowledge.errorCount ?? '-' }}</span>
              </div>
              <div class="knowledge-item">
                <span class="kn-label">总题数</span>
                <span class="kn-val">{{ currentCourseKnowledge.totalQuestion ?? '-' }}</span>
              </div>
              <div class="knowledge-item">
                <span class="kn-label">薄弱知识点</span>
                <span class="kn-val">{{ currentCourseKnowledge.weakKnowledgeCount ?? '-' }}</span>
              </div>
              <div class="knowledge-item">
                <span class="kn-label">基础题</span>
                <span class="kn-val">{{ currentCourseKnowledge.basicCorrect ?? '-' }}/{{ currentCourseKnowledge.basicTotal ?? '-' }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-text">暂无知识点掌握数据</div>
        </div>

        <!-- 薄弱知识点 -->
        <div class="detail-card">
          <div class="card-title">薄弱知识点</div>
          <div v-if="currentCourseWeakPoints.length" class="weak-points">
            <span v-for="(wp, i) in currentCourseWeakPoints" :key="wp.id || wp.knowledgePointId || i"
                  class="weak-tag"
                  :title="'错误率: ' + (wp.errorRate ?? wp.avgErrorRate ?? '?') + '%'">
              {{ wp.knowledgePointName || wp.name || wp.kpName || ('知识点 #' + (i+1)) }}
            </span>
          </div>
          <div v-else class="empty-text">暂未检测到薄弱知识点</div>
        </div>
      </div>

      <!-- 其他异常 -->
      <div v-else class="empty">
        <p style="margin-bottom:12px;">⚠️ 无法加载学情数据</p>
        <button class="btn-primary" @click="loadStudentData()">🔄 重试</button>
      </div>
    </div>

    <!-- ====== 预警列表 ====== -->
    <div v-if="activeTab === 'warnings'" class="warning-section">
      <h3>🚨 我的预警</h3>
      <div v-if="alertLoading" class="empty">加载中...</div>
      <div v-else-if="studentAlerts.length" class="alert-list">
        <div v-for="a in studentAlerts" :key="a.id" class="alert-card" :style="{ borderLeftColor: alertLevelColor(a.alertLevel) }">
          <div class="alert-header">
            <span class="alert-level-badge" :style="{ background: alertLevelBg(a.alertLevel), color: alertLevelColor(a.alertLevel) }">{{ a.alertLevelName }}</span>
            <span class="alert-type">{{ a.alertTypeName }}</span>
            <span class="alert-status" :class="a.status">{{ a.statusName }}</span>
          </div>
          <div class="alert-body">
            <div class="alert-reason">{{ a.alertReason }}</div>
            <div class="alert-scores"><span class="score-tag">综合风险 {{ a.riskScore }}分</span></div>
          </div>
          <div class="alert-footer" v-if="a.status === 'ACTIVE'">
            <button class="btn-handle" @click="confirmStudentAck(a)">确认预警</button>
            <button class="btn-ghost" @click="studentRespondToAlert(a)">回应老师</button>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无预警</div>
    </div>

    <!-- ====== 练习列表 ====== -->
    <div v-if="activeTab === 'exercises'" class="exercise-page">
      <h3>📝 我的练习</h3>
      <div class="filter-row">
        <select v-model="filterStatus"><option value="ALL">全部</option><option value="PENDING">待完成</option><option value="ANSWERED">待批改</option><option value="COMPLETED">已完成</option></select>
      </div>
      <div v-if="recLoading" class="empty">加载中...</div>
      <div v-else-if="filteredRecommends.length" class="exercise-list">
        <div v-for="rec in filteredRecommends" :key="rec.id" class="exercise-card">
          <div class="exercise-info">
            <strong>{{ rec.exerciseTitle }}</strong>
            <span class="tag" :style="{ background: rec.status==='PENDING'?'#fef3c7':rec.status==='ANSWERED'?'#dbeafe':rec.status==='GRADED'?'#d4edda':'#ddd', color: rec.status==='PENDING'?'#92400e':rec.status==='ANSWERED'?'#1e40af':rec.status==='GRADED'?'#166534':'#666' }">{{ rec.status==='PENDING'?'待完成':rec.status==='ANSWERED'?'待批改':rec.status==='GRADED'?'已评分':rec.status }}</span>
            <span v-if="rec.score != null" style="color:var(--semantic-green);">得分：{{ rec.score }}</span>
          </div>
          <button v-if="rec.status === 'PENDING'" class="btn-primary btn-sm" @click="openStudentAnswer(rec)">开始作答</button>
        </div>
      </div>
      <div v-else class="empty">暂无练习</div>
    </div>

    <!-- ====== 分栏作答面板（左 PDF + 右答题） ====== -->
    <div v-if="showAnswerModal" class="answer-overlay" @click.self="showAnswerModal = false">
      <div class="answer-split">
        <!-- === 顶部栏 === -->
        <div class="answer-topbar">
          <span class="answer-title">📝 {{ currentRecommend?.exerciseTitle }}</span>
          <div class="answer-topbar-right">
            <span class="answer-exercise-desc" v-if="currentRecommend?.exerciseDescription">{{ currentRecommend.exerciseDescription }}</span>
            <button class="btn-ghost btn-sm" @click="showAnswerModal = false">✕ 关闭</button>
          </div>
        </div>

        <!-- === 主体分栏 === -->
        <div class="answer-body">
          <!-- === 左栏：PDF 预览 === -->
          <div class="answer-pdf-panel">
            <div class="pdf-toolbar">
              <button class="btn-ghost btn-xs" @click="pdfZoom = Math.max(0.3, pdfZoom - 0.2); renderCurrentPage()" title="缩小">🔍−</button>
              <span class="pdf-zoom-label">{{ Math.round(pdfZoom * 100) }}%</span>
              <button class="btn-ghost btn-xs" @click="pdfZoom = Math.min(3, pdfZoom + 0.2); renderCurrentPage()" title="放大">🔍+</button>
              <span class="pdf-sep">|</span>
              <button class="btn-ghost btn-xs" :disabled="pdfCurrentPage <= 1" @click="pdfCurrentPage--; renderCurrentPage()">‹ 上一页</button>
              <span class="pdf-page-label">{{ pdfCurrentPage }} / {{ pdfTotalPages || '?' }}</span>
              <button class="btn-ghost btn-xs" :disabled="pdfCurrentPage >= pdfTotalPages" @click="pdfCurrentPage++; renderCurrentPage()">下一页 ›</button>
              <span class="pdf-sep">|</span>
              <button class="btn-ghost btn-xs" @click="pdfZoom = 1.0; renderCurrentPage()" title="重置大小">⊡ 实际大小</button>
            </div>
            <div class="pdf-canvas-wrap">
              <!-- 独立 canvas 容器（始终在 DOM 中，避免与 Vue v-if 冲突） -->
              <div v-show="currentRecommend?.fileUrl && !pdfLoadingDoc"
                   class="pdf-canvas-target" ref="pdfCanvasWrap"></div>
              <div v-if="!currentRecommend?.fileUrl" class="pdf-empty">该练习无附件 PDF</div>
              <div v-else-if="pdfLoadingDoc" class="pdf-empty">⏳ 加载 PDF 中...</div>
            </div>
          </div>

          <!-- === 右栏：答题区 === -->
          <div class="answer-question-panel">
            <div v-if="!subQuestions.length" class="answer-question-scroll">
              <!-- 无子题：简单题 -->
              <div class="answer-question-card">
                <div class="aq-label">作答</div>
                <textarea v-model="studentAnswer" rows="10" class="aq-textarea" placeholder="输入你的答案..."></textarea>
              </div>
            </div>
            <div v-else class="answer-question-scroll">
              <!-- 有子题：当前题展示 -->
              <div class="answer-question-card aq-current">
                <div class="aq-q-header">
                  <span class="aq-q-num">第 {{ currentQIndex + 1 }} / {{ subQuestions.length }} 题</span>
                  <span class="aq-q-status" v-if="subAnswers[subQuestions[currentQIndex]?.id]?.trim()">✓ 已答</span>
                  <span class="aq-q-status answering">作答中</span>
                </div>
                <div class="aq-q-title">{{ subQuestions[currentQIndex]?.title }}</div>
                <textarea
                  :value="subAnswers[subQuestions[currentQIndex]?.id] || ''"
                  @input="e => { subAnswers = { ...subAnswers, [subQuestions[currentQIndex]?.id]: e.target.value } }"
                  rows="8" class="aq-textarea" placeholder="输入你的答案..."></textarea>
              </div>

              <!-- 导航按钮 -->
              <div class="aq-nav-row">
                <button class="btn-ghost btn-sm" :disabled="currentQIndex <= 0"
                        @click="goPrevQuestion">← 上一题</button>
                <div class="aq-nav-dots">
                  <span v-for="(sq, sqi) in subQuestions" :key="sq.id"
                        class="aq-nav-dot"
                        :class="{ active: sqi === currentQIndex, done: subAnswers[sq.id]?.trim() }"
                        :title="'第 ' + (sqi + 1) + ' 题'"
                        @click="currentQIndex = sqi"></span>
                </div>
                <button class="btn-ghost btn-sm" :disabled="currentQIndex >= subQuestions.length - 1"
                        @click="goNextQuestion">下一题 →</button>
              </div>
            </div>

            <!-- 底部操作 -->
            <div class="answer-footer">
              <div class="answer-progress" v-if="subQuestions.length">
                已完成 {{ Object.values(subAnswers).filter(v => v?.trim()).length }} / {{ subQuestions.length }}
              </div>
              <div class="answer-actions">
                <button class="btn-ghost" @click="showAnswerModal = false">取消</button>
                <button class="btn-primary" @click="handleSubmitAnswers">提交答案</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== 学习目标 ====== -->
    <div v-if="activeTab === 'goals'" class="exercise-page">
      <h3>🎯 我的学习目标</h3>
      <div v-if="goalLoading" class="empty">加载中...</div>
      <div v-else class="card" style="max-width:560px;">
        <div class="input-group">
          <label>选择课程</label>
          <select v-model="goalCourseId" @change="loadStudentGoals">
            <option v-for="c in studentCourses" :key="c.courseId" :value="c.courseId">{{ c.courseName }}</option>
          </select>
        </div>
        <div class="input-group">
          <label>目标等级</label>
          <div class="goal-levels">
            <button v-for="l in ['冲刺','稳定','追赶','保底']" :key="l" :class="{ active: goalLevel === l }" @click="goalLevel = l">{{ l }}</button>
          </div>
        </div>
        <div class="input-group"><label>目标分数（可选）</label><input v-model="goalScore" type="number" min="0" max="100" placeholder="0-100" /></div>
        <div class="input-group"><label>里程碑（可选）</label><textarea v-model="goalMilestones" rows="2" placeholder="如：期中达到70分，作业全交..." style="width:100%;padding:8px;border:1px solid #ddd;border-radius:4px;"></textarea></div>
        <div class="input-group"><label>个人备注（可选）</label><textarea v-model="goalNote" rows="2" placeholder="其他想告诉老师的..." style="width:100%;padding:8px;border:1px solid #ddd;border-radius:4px;"></textarea></div>
        <button class="btn-primary" :disabled="goalSaving" @click="saveStudentGoal" style="width:100%;">{{ goalSaving ? '保存中...' : '保存目标' }}</button>
      </div>
    </div>

    <!-- ====== 风险画像弹窗 ====== -->
    <div v-if="showRiskProfile" class="profile-overlay" @click.self="closeRiskProfile">
      <div class="profile-modal">
        <div class="profile-modal-header">
          <div class="profile-modal-title">
            <strong>学情画像</strong>
            <span>{{ studentData?.basicInfo?.studentName }} · {{ currentCourseScore?.courseName || '当前课程' }}</span>
          </div>
          <button class="btn-ghost" @click="closeRiskProfile">✕ 关闭</button>
        </div>

        <div v-if="profileLoading" class="profile-state">⏳ 正在加载画像数据...</div>
        <div v-else-if="profileError" class="profile-state profile-error">{{ profileError }}</div>
        <div v-else class="profile-modal-body">
          <div class="profile-block">
            <div class="profile-block-title">① 五维风险雷达</div>
            <RiskRadarChart
              :student="profileRadar?.student"
              :class-avg="profileRadar?.classAvg"
              :has-data="profileRadar?.student?.hasData !== false"
              :scope="profileRadar?.scope || ''"
              :scope-text="profileRadar?.scopeText || ''"
              :missing-dimensions="profileRadar?.missingDimensions || []" />
          </div>
          <div class="profile-block">
            <div class="profile-block-title">② 综合风险分趋势</div>
            <RiskTrendChart
              :points="profileTrend"
              :source="profileTrendSource"
              :source-text="profileTrendSourceText" />
          </div>
          <div class="profile-block">
            <div class="profile-block-title">③ 三维学情画像（知识掌握 / 学习习惯 / 学习目标）</div>
            <ThreeDimProfile
              :profile="profile3d"
              empty-text="你还没有三维学情画像，可联系老师发起一次学习画像分析" />
          </div>
        </div>
      </div>
    </div>

  </div>
  <div v-else style="padding:40px;text-align:center;">
    <p>请先登录</p>
    <button class="btn-primary" @click="router.push('/student/login')">前往登录</button>
  </div>
</template>

<style scoped>
/* ===== 学情页面（与教师端详情模态框一致） ===== */
.student-info-page { padding: 0; }
.student-info-page h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 4px 0 12px;
  color: var(--text-primary);
}

/* 详情头 */
.detail-header { display:flex; align-items:flex-start; gap:16px; margin-bottom:20px; }
.student-avatar { width:52px; height:52px; border-radius:10px; background:var(--bg-hover); display:flex; align-items:center; justify-content:center; font-size:22px; font-weight:700; color:var(--text-secondary); flex-shrink:0; }
.student-meta { flex:1; min-width:0; }
.student-name-row { display:flex; align-items:center; gap:10px; flex-wrap:wrap; margin-bottom:6px; }
.student-name { font-size:20px; font-weight:700; color:var(--text-primary); }
.risk-badge { display:inline-block; padding:2px 10px; border-radius:12px; font-size:12px; font-weight:600; }
.risk-badge.green { background:var(--semantic-green-bg); color:var(--semantic-green-text); }
.risk-badge.yellow { background:var(--semantic-yellow-bg); color:var(--semantic-yellow-text); }
.risk-badge.orange { background:var(--semantic-orange-bg); color:var(--semantic-orange-text); }
.risk-badge.red { background:var(--semantic-red-bg); color:var(--semantic-red-text); }
.student-tags { display:flex; gap:8px; flex-wrap:wrap; }
.info-tag { display:inline-block; padding:4px 12px; border-radius:12px; background:var(--bg-hover); color:var(--text-secondary); font-size:13px; }

/* 课程选择器 */
.course-picker { display:flex; align-items:center; flex-wrap:wrap; gap:8px; margin-bottom:16px; padding:10px 12px; background:var(--bg-card-subtle); border:1px solid var(--border-light); border-radius:10px; }
.picker-label { font-size:13px; color:var(--text-secondary); margin-right:4px; }
.course-chip { padding:6px 14px; border:1px solid var(--border-light); background:#fff; border-radius:16px; font-size:13px; color:var(--text-secondary); cursor:pointer; transition:all 0.2s; }
.course-chip:hover { border-color:var(--accent-primary); color:var(--accent-primary); }
.course-chip.active { background:var(--accent-primary); border-color:var(--accent-primary); color:#fff; }

/* 卡片 */
.detail-body { display:flex; flex-direction:column; gap:16px; }
.detail-card { background:var(--bg-card-subtle); border:1px solid var(--border-light); border-radius:12px; padding:16px; }
.card-title { font-size:16px; font-weight:700; color:var(--text-primary); margin-bottom:12px; padding-left:8px; border-left:4px solid var(--accent-primary); }
.empty-text { color:var(--text-tertiary); font-size:14px; padding:8px 0; }

/* 课程区块 */
.course-section { display:flex; flex-direction:column; gap:10px; }
.course-name { font-size:15px; font-weight:600; color:var(--text-primary); }

/* 成绩 */
.score-grid { display:grid; grid-template-columns:repeat(4, 1fr); gap:10px; }
.score-block { display:flex; flex-direction:column; align-items:center; gap:4px; padding:10px; border-radius:8px; background:#fff; border:1px solid var(--border-light); }
.score-block.highlight { background:var(--accent-light); border-color:var(--accent-primary); }
.score-block .label { font-size:12px; color:var(--text-tertiary); }
.score-block .val { font-size:20px; font-weight:700; }

/* 作业 */
.homework-row { display:flex; gap:12px; flex-wrap:wrap; }
.hw-tag { display:inline-flex; align-items:center; gap:4px; padding:6px 12px; border-radius:8px; font-size:14px; font-weight:600; }
.hw-tag.submitted { background:#dcfce7; color:#166534; }
.hw-tag.missing { background:#fee2e2; color:#991b1b; }
.hw-tag.avg { background:#e0e7ff; color:#3730a3; }

/* 课堂表现 & 知识点 */
.performance-row, .knowledge-row { display:grid; grid-template-columns:repeat(5, 1fr); gap:10px; }
.perf-item, .knowledge-item { display:flex; flex-direction:column; gap:4px; padding:10px; border-radius:8px; background:#fff; border:1px solid var(--border-light); text-align:center; }
.perf-label, .kn-label { font-size:12px; color:var(--text-tertiary); }
.perf-val, .kn-val { font-size:15px; font-weight:600; color:var(--text-primary); }

/* 历史风险 */
.history-content { padding:4px 0; }
.history-summary { font-size:14px; color:var(--text-primary); line-height:1.6; }

/* 薄弱知识点 */
.weak-points { display:flex; flex-wrap:wrap; gap:8px; }
.weak-tag { display:inline-block; padding:4px 10px; border-radius:6px; background:var(--kp-tag-bg, #fef3c7); color:var(--kp-tag-text, #92400e); font-size:13px; }

/* ===== 分栏作答面板 ===== */
.answer-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.35);
  display: flex; flex-direction: column;
  animation: fadeIn 0.15s ease;
}
.answer-split {
  flex: 1; display: flex; flex-direction: column;
  margin: 8px; border-radius: 12px; overflow: hidden;
  background: #fff; box-shadow: 0 8px 40px rgba(0,0,0,0.15);
}
.answer-topbar {
  display: flex; align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--bg-card-subtle);
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.answer-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.answer-topbar-right { display: flex; align-items: center; gap: 12px; }
.answer-exercise-desc { font-size: 13px; color: var(--text-tertiary); max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.answer-body {
  flex: 1; display: flex;
  overflow: hidden;
}

/* 左栏：PDF */
.answer-pdf-panel {
  flex: 1; display: flex; flex-direction: column;
  border-right: 1px solid var(--border-light);
  min-width: 0;
  background: #f8f9fb;
}
.pdf-toolbar {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
  flex-wrap: wrap;
}
.pdf-zoom-label { font-size: 12px; color: var(--text-secondary); min-width: 36px; text-align: center; }
.pdf-page-label { font-size: 12px; color: var(--text-secondary); min-width: 50px; text-align: center; }
.pdf-sep { color: var(--border-light); font-size: 14px; }
.pdf-canvas-wrap {
  flex: 1; overflow: auto;
  display: flex; flex-direction: column; align-items: center;
  padding: 12px;
}
.pdf-canvas-target {
  width: 100%; display: flex; justify-content: center;
}
.pdf-empty {
  color: var(--text-tertiary); font-size: 14px;
  padding: 60px 20px; text-align: center;
}

/* 右栏：答题 */
.answer-question-panel {
  width: 380px; display: flex; flex-direction: column;
  flex-shrink: 0;
  background: #fff;
}
.answer-question-scroll {
  flex: 1; overflow-y: auto;
  padding: 12px;
  display: flex; flex-direction: column; gap: 12px;
}
.answer-question-card {
  padding: 14px; border-radius: 10px;
  border: 1px solid var(--border-light);
  background: #fff;
  transition: border-color 0.2s;
}
.answer-question-card.aq-active {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px var(--accent-focus);
}
.answer-question-card.aq-done { border-color: var(--semantic-green-border); }
.aq-q-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 8px; cursor: pointer;
}
.aq-q-num { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.aq-q-status { font-size: 11px; padding: 1px 8px; border-radius: 8px; background: var(--bg-hover); color: var(--text-tertiary); }
.aq-q-title { font-size: 14px; color: var(--text-primary); margin-bottom: 8px; line-height: 1.5; }
.aq-textarea {
  width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 14px; line-height: 1.6; resize: vertical;
  box-sizing: border-box;
}
.aq-textarea:focus { border-color: var(--accent-primary); box-shadow: 0 0 0 2px var(--accent-focus); outline: none; }

/* 子题导航 */
.aq-nav-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 4px; gap: 8px;
}
.aq-nav-dots { display: flex; gap: 6px; flex:1; justify-content:center; flex-wrap:wrap; }
.aq-nav-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--border-light); cursor: pointer;
  transition: all 0.2s; flex-shrink:0;
}
.aq-nav-dot.active { background: var(--accent-primary); transform: scale(1.3); }
.aq-nav-dot.done { background: var(--semantic-green); }
.aq-current { border-color: var(--accent-primary); box-shadow: 0 0 0 2px var(--accent-focus); }

.aq-q-status.answering { background: var(--accent-light); color: var(--accent-primary); }

.answer-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid var(--border-light);
  background: #fff;
  flex-shrink: 0;
}
.answer-progress { font-size: 13px; color: var(--text-secondary); }
.answer-actions { display: flex; gap: 8px; }

/* 移动端适配 */
@media (max-width: 768px) {
  .answer-body { flex-direction: column; }
  .answer-pdf-panel { flex: none; height: 45vh; border-right: none; border-bottom: 1px solid var(--border-light); }
  .answer-question-panel { width: 100%; }
}
@media (max-width: 640px) {
  .score-grid { grid-template-columns: repeat(2, 1fr); }
  .performance-row, .knowledge-row { grid-template-columns: repeat(2, 1fr); }
  .student-name { font-size: 18px; }
}

/* 练习 / 目标页面 */
.filter-row { margin-bottom: 16px; }
.filter-row select { padding: 6px 12px; border: 1px solid #ddd; border-radius: 4px; }
.exercise-list { display: flex; flex-direction: column; gap: 12px; }
.exercise-card { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--bg-card-subtle); border-radius: 8px; border: 1px solid var(--bg-active); }
.goal-levels { display: flex; gap: 8px; }
.goal-levels button { padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; }
.goal-levels button.active { background: var(--text-primary); color: #fff; }

/* ===== 风险画像 ===== */
.profile-entry { cursor: pointer; border: none; transition: filter .15s, transform .15s; }
.profile-entry:hover { filter: brightness(1.08); transform: translateY(-1px); }

.profile-overlay {
  position: fixed; inset: 0; z-index: 1200;
  background: rgba(0, 0, 0, 0.35);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
  animation: fadeIn 0.15s ease;
}
.profile-modal {
  width: min(680px, 100%);
  max-height: 86vh;
  display: flex; flex-direction: column;
  background: var(--bg-card, #fff);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
}
.profile-modal-header {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  background: var(--bg-card-subtle);
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.profile-modal-title { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.profile-modal-title strong { font-size: 17px; color: var(--text-primary); }
.profile-modal-title span { font-size: 13px; color: var(--text-tertiary); }
.profile-modal-body { overflow-y: auto; padding: 14px 18px 18px; display: flex; flex-direction: column; gap: 12px; }
.profile-block { background: var(--bg-card-subtle); border: 1px solid var(--border-light); border-radius: 10px; padding: 12px; }
.profile-block-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 6px; }
.profile-state { padding: 48px 16px; text-align: center; color: var(--text-tertiary); font-size: 14px; }
.profile-error { color: var(--semantic-red-dark); }
</style>
