<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { getJson } from '../../api/http.js'
import {
  getStudentTrend, getStudentRadar, getStudentWeakPoints,
  getStudentProfile, agentFullAssessment
} from '../../api/http.js'
import RiskRadarChart from '../RiskRadarChart.vue'
import RiskTrendChart from '../RiskTrendChart.vue'
import ThreeDimProfile from '../ThreeDimProfile.vue'
import AgentRunHistory from './AgentRunHistory.vue'
import AgentToolAsk from './AgentToolAsk.vue'
import { riskLevelFromScore, riskLevelLabel } from '../../utils/riskLevel.js'

const { teacherToken } = useAuth()
const props = defineProps({ courseId: { type: Number, required: true } })
const emit = defineEmits(['message'])

const students = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const filterLevel = ref('all')
const showDetailModal = ref(false)
const selectedStudent = ref(null)
const studentDetail = ref({ weakPoints: [], radar: null, trend: null, profile: null })
const detailLoading = ref(false)
const profileGenerating = ref(false)

onMounted(() => loadStudents())

async function loadStudents() {
  loading.value = true
  try {
    const res = await getJson('/teacher/my-students', teacherToken.value)
    if (res.success) {
      // 后端返回 StudentFullVO[]，保留完整嵌套对象，只过滤出选了当前课程的学生
      students.value = (res.data || [])
        .filter(full => (full.scores || []).some(s => s.courseId === props.courseId))
    } else {
      emit('message', res.message || '获取学生列表失败', 'error')
    }
  } catch { emit('message', '获取学生列表失败', 'error') }
  finally { loading.value = false }
}

function getCourseScore(student) {
  return (student?.scores || []).find(s => s.courseId === props.courseId)
}

function getAlertLevel(student) {
  // 阈值口径统一在 utils/riskLevel.js，避免多份魔法数漂移
  return riskLevelFromScore(getCourseScore(student)?.calculatedScore)
}

function getRiskScore(student) {
  const raw = getCourseScore(student)?.calculatedScore
  const cs = Number(raw)
  if (raw == null || Number.isNaN(cs)) return null
  return Math.round(Math.max(0, 100 - cs) * 10) / 10
}

const filteredStudents = computed(() => {
  let list = students.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(s => (s.basicInfo?.studentName || '').toLowerCase().includes(kw) ||
      (s.basicInfo?.studentNo || '').toLowerCase().includes(kw))
  }
  if (filterLevel.value !== 'all') {
    list = list.filter(s => getAlertLevel(s) === filterLevel.value)
  }
  return list
})

function getStatusLabel(s) {
  return riskLevelLabel(getAlertLevel(s))
}

function getStatusClass(s) {
  const lv = getAlertLevel(s)
  return 'status-tag ' + lv.toLowerCase()
}

async function openStudentDetail(student) {
  selectedStudent.value = student
  showDetailModal.value = true
  detailLoading.value = true
  studentDetail.value = { weakPoints: [], radar: null, trend: [], trendSource: '', trendSourceText: '', profile: null }
  try {
    const [wpRes, trendRes, radarRes, profileRes] = await Promise.all([
      getStudentWeakPoints(teacherToken.value, student.basicInfo?.id, props.courseId),
      getStudentTrend(teacherToken.value, student.basicInfo?.id, props.courseId),
      getStudentRadar(teacherToken.value, student.basicInfo?.id, props.courseId),
      getStudentProfile(teacherToken.value, student.basicInfo?.id, props.courseId)
    ])
    studentDetail.value.weakPoints = (wpRes.data || wpRes || []).slice(0, 10)
    // 新接口返回 {points, source, sourceText}，同时兼容旧的数组返回
    const trendData = trendRes.data || trendRes || {}
    studentDetail.value.trend = Array.isArray(trendData) ? trendData : (trendData.points || [])
    studentDetail.value.trendSource = Array.isArray(trendData) ? '' : (trendData.source || '')
    studentDetail.value.trendSourceText = Array.isArray(trendData) ? '' : (trendData.sourceText || '')
    studentDetail.value.radar = radarRes.data || radarRes || null
    studentDetail.value.profile = profileRes?.data || null
  } catch { /* ignore */ }
  finally { detailLoading.value = false }
}

/**
 * 生成/刷新该生的三维画像：调用智能体完整评估流水线（监测→分析→画像→推荐），会调用大模型
 */
async function generateProfile() {
  const studentId = selectedStudent.value?.basicInfo?.id
  if (!studentId) return
  if (!window.confirm('将调用大模型执行「监测 → 分析 → 画像 → 推荐」完整评估，可能耗时数十秒，是否继续？')) return
  profileGenerating.value = true
  try {
    const res = await agentFullAssessment(teacherToken.value, studentId, props.courseId)
    if (res?.success === false) {
      emit('message', res?.message || '画像生成失败', 'error')
      return
    }
    emit('message', '三维画像生成完成', 'success')
    const refreshed = await getStudentProfile(teacherToken.value, studentId, props.courseId)
    studentDetail.value.profile = refreshed?.data || studentDetail.value.profile
  } catch (e) {
    emit('message', '画像生成失败：' + (e?.message || e), 'error')
  } finally {
    profileGenerating.value = false
  }
}

function closeDetail() {
  showDetailModal.value = false
  selectedStudent.value = null
}

// 点击头部"风险画像"徽章 -> 平滑滚动到画像图表卡片
function scrollToProfile() {
  const id = 'profile-card-' + (selectedStudent.value?.basicInfo?.id ?? '')
  const el = document.getElementById(id)
  if (el) {
    const modal = el.closest('.detail-modal')
    if (modal) modal.scrollTo({ top: el.offsetTop - modal.offsetTop - 12, behavior: 'smooth' })
    else el.scrollIntoView({ behavior: 'smooth' })
  }
}

function scoreColor(score) {
  if (score == null) return 'var(--text-tertiary)'
  if (score >= 90) return 'var(--semantic-green)'
  if (score >= 80) return '#7c3aed'
  if (score >= 70) return '#0284c7'
  if (score >= 60) return '#d97706'
  return 'var(--semantic-red)'
}

function emptyText(text) {
  return text && text.trim() ? text.trim() : '暂无记录'
}

const currentCourseScore = computed(() => getCourseScore(selectedStudent.value))
const currentCourseHomework = computed(() =>
  (selectedStudent.value?.homeworkList || []).find(h => h.courseId === props.courseId))
const currentCoursePerformance = computed(() =>
  (selectedStudent.value?.classPerformances || []).find(p => p.courseId === props.courseId))
const currentCourseHistoryRisk = computed(() =>
  (selectedStudent.value?.historyRisks || []).find(r => r.courseId === props.courseId))
const currentCourseKnowledge = computed(() =>
  (selectedStudent.value?.knowledgeMasteries || []).find(k => k.courseId === props.courseId))

</script>

<template>
  <div class="section">
    <!-- 工具栏 -->
    <div class="card" style="padding:16px 20px;margin-bottom:16px;display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
      <input v-model="searchKeyword" placeholder="搜索学生姓名/学号..." style="flex:1;min-width:180px;max-width:280px;padding:7px 12px;" />
      <select v-model="filterLevel" style="padding:7px 12px;">
        <option value="all">全部等级</option>
        <option value="RED">🔴 红色预警</option>
        <option value="ORANGE">🟠 橙色预警</option>
        <option value="YELLOW">🟡 黄色预警</option>
        <option value="GREEN">🟢 正常</option>
      </select>
      <button class="btn-ghost" @click="loadStudents">🔄 刷新</button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载中...
    </div>

    <!-- 学生表格 -->
    <div v-else class="card" style="overflow-x:auto;">
      <table class="data-table">
        <thead>
          <tr>
            <th>学号</th>
            <th>姓名</th>
            <th>班级</th>
            <th>状态</th>
            <th>风险分</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in filteredStudents" :key="s.basicInfo?.id">
            <td>{{ s.basicInfo?.studentNo }}</td>
            <td><strong>{{ s.basicInfo?.studentName }}</strong></td>
            <td>{{ s.basicInfo?.className || '-' }}</td>
            <td><span :class="getStatusClass(s)">{{ getStatusLabel(s) }}</span></td>
            <td>{{ getRiskScore(s) ?? '-' }}</td>
            <td>
              <button class="btn-ghost" @click="openStudentDetail(s)">📋 详情</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filteredStudents.length" style="text-align:center;padding:40px;color:var(--text-tertiary);">
        {{ students.length ? '无匹配学生' : '暂无学生数据' }}
      </div>
    </div>

    <!-- 学生详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-card detail-modal">
        <div class="detail-header">
          <div class="student-avatar">学</div>
          <div class="student-meta">
            <div class="student-name-row">
              <span class="student-name">{{ selectedStudent?.basicInfo?.studentName }}</span>
              <button class="risk-badge profile-entry" :class="getAlertLevel(selectedStudent).toLowerCase()"
                      @click="scrollToProfile" title="点击查看风险画像图表">📊 风险画像</button>
            </div>
            <div class="student-tags">
              <span class="info-tag">{{ selectedStudent?.basicInfo?.studentNo }}</span>
              <span class="info-tag">{{ selectedStudent?.basicInfo?.grade }}</span>
              <span class="info-tag">{{ selectedStudent?.basicInfo?.className }}</span>
            </div>
          </div>
          <button class="btn-ghost" @click="closeDetail">✕ 关闭</button>
        </div>

        <div v-if="detailLoading" style="text-align:center;padding:40px;">
          <span class="spinner"></span> 加载详情...
        </div>

        <div v-else class="detail-body">
          <!-- 风险画像（雷达图 + 趋势图） -->
          <div class="detail-card" :id="'profile-card-' + (selectedStudent?.basicInfo?.id ?? '')">
            <div class="card-title">风险画像</div>
            <div class="profile-section">
              <div class="profile-sub-title">风险维度雷达（该生 vs 班级均值）</div>
              <RiskRadarChart
                :student="studentDetail.radar?.student"
                :class-avg="studentDetail.radar?.classAvg"
                :has-data="studentDetail.radar?.student?.hasData !== false"
                :scope="studentDetail.radar?.scope || ''"
                :scope-text="studentDetail.radar?.scopeText || ''"
                :missing-dimensions="studentDetail.radar?.missingDimensions || []"
                empty-text="暂未生成该生的风险维度数据" />
            </div>
            <div class="profile-section">
              <div class="profile-sub-title">综合风险分趋势</div>
              <RiskTrendChart :points="studentDetail.trend || []"
                              :source="studentDetail.trendSource || ''"
                              :source-text="studentDetail.trendSourceText || ''"
                              empty-text="暂无风险趋势快照数据" />
            </div>
          </div>

          <!-- 三维学情画像（知识掌握 / 学习习惯 / 学习目标） -->
          <div class="detail-card">
            <div class="card-title">三维学情画像</div>
            <ThreeDimProfile
              :profile="studentDetail.profile"
              :loading="detailLoading"
              :generating="profileGenerating"
              :can-generate="true"
              empty-text="该生暂无三维学情画像"
              @generate="generateProfile" />
          </div>

          <!-- 智能体工具问答（W3：自然语言查学情，带工具调用轨迹） -->
          <div class="detail-card">
            <div class="card-title">智能体工具问答</div>
            <AgentToolAsk
              :student-id="selectedStudent?.basicInfo?.id ?? null"
              :course-id="props.courseId ?? null" />
          </div>

          <!-- 智能体运行历史（W1：可观测） -->
          <div class="detail-card">
            <div class="card-title">智能体运行历史</div>
            <AgentRunHistory :student-id="selectedStudent?.basicInfo?.id ?? null" :limit="5" />
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
            <div v-if="studentDetail.weakPoints.length" class="weak-points">
              <span v-for="wp in studentDetail.weakPoints" :key="wp.id || wp.knowledgePointId"
                class="weak-tag"
                :title="'错误率: ' + (wp.errorRate ?? wp.avgErrorRate ?? '?') + '%'">
                {{ wp.knowledgePointName || wp.name }}
              </span>
            </div>
            <div v-else class="empty-text">暂未检测到薄弱知识点</div>
          </div>


        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.data-table { width:100%; border-collapse:collapse; font-size:14px; }
.data-table th { text-align:left; padding:12px 16px; border-bottom:2px solid var(--border-light); color:var(--text-secondary); font-weight:600; white-space:nowrap; }
.data-table td { padding:12px 16px; border-bottom:1px solid var(--border-light); }
.data-table tbody tr:hover { background:var(--bg-hover); }

.status-tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:12px; font-weight:600; }
.status-tag.red { background:var(--semantic-red-bg); color:var(--semantic-red-text); }
.status-tag.orange { background:var(--semantic-orange-bg); color:var(--semantic-orange-text); }
.status-tag.yellow { background:var(--semantic-yellow-bg); color:var(--semantic-yellow-text); }
.status-tag.green { background:var(--semantic-green-bg); color:var(--semantic-green-text); }

.detail-modal { max-width:720px; max-height:85vh; overflow-y:auto; padding:24px; }
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
.profile-entry { cursor:pointer; border:none; transition:filter .15s, transform .15s; }
.profile-entry:hover { filter:brightness(1.08); transform:translateY(-1px); }
.student-tags { display:flex; gap:8px; flex-wrap:wrap; }
.info-tag { display:inline-block; padding:4px 12px; border-radius:12px; background:var(--bg-hover); color:var(--text-secondary); font-size:13px; }

.detail-body { display:flex; flex-direction:column; gap:16px; }
.detail-card { background:var(--bg-card-subtle); border:1px solid var(--border-light); border-radius:12px; padding:16px; }
.card-title { font-size:16px; font-weight:700; color:var(--text-primary); margin-bottom:12px; padding-left:8px; border-left:4px solid var(--accent-primary); }
.empty-text { color:var(--text-tertiary); font-size:14px; padding:8px 0; }

.course-section { display:flex; flex-direction:column; gap:10px; }
.course-name { font-size:15px; font-weight:600; color:var(--text-primary); }
.score-grid { display:grid; grid-template-columns:repeat(4, 1fr); gap:10px; }
.score-block { display:flex; flex-direction:column; align-items:center; gap:4px; padding:10px; border-radius:8px; background:#fff; border:1px solid var(--border-light); }
.score-block.highlight { background:var(--accent-light); border-color:var(--accent-primary); }
.score-block .label { font-size:12px; color:var(--text-tertiary); }
.score-block .val { font-size:20px; font-weight:700; }

.homework-row { display:flex; gap:12px; flex-wrap:wrap; }
.hw-tag { display:inline-flex; align-items:center; gap:4px; padding:6px 12px; border-radius:8px; font-size:14px; font-weight:600; }
.hw-tag.submitted { background:#dcfce7; color:#166534; }
.hw-tag.missing { background:#fee2e2; color:#991b1b; }
.hw-tag.avg { background:#e0e7ff; color:#3730a3; }

.performance-row { display:grid; grid-template-columns:repeat(5, 1fr); gap:10px; }
.perf-item, .knowledge-item { display:flex; flex-direction:column; gap:4px; padding:10px; border-radius:8px; background:#fff; border:1px solid var(--border-light); text-align:center; }
.perf-label, .kn-label { font-size:12px; color:var(--text-tertiary); }
.perf-val, .kn-val { font-size:15px; font-weight:600; color:var(--text-primary); }

.knowledge-row { display:grid; grid-template-columns:repeat(5, 1fr); gap:10px; }

.history-content { padding:4px 0; }
.history-summary { font-size:14px; color:var(--text-primary); line-height:1.6; }

.profile-section { padding:12px 0 4px; }
.profile-section + .profile-section { border-top:1px dashed var(--border-light); }
.profile-sub-title { font-size:13px; font-weight:600; color:var(--text-secondary); margin-bottom:6px; }

.weak-points { display:flex; flex-wrap:wrap; gap:8px; }
.weak-tag { display:inline-block; padding:4px 10px; border-radius:6px; background:var(--kp-tag-bg); color:var(--kp-tag-text); font-size:13px; }
</style>
