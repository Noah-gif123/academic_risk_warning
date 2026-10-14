<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../../composables/useAuth.js'
import { useCaptcha } from '../../composables/useCaptcha.js'
import { adminLogin, getAdminTeachers, getAdminTeacherCourses, getAdminTeacherStudents,
  getAdminTeacherAlerts, adminMarkTeacher, adminNotifyTeacher, getAdminTeacherLogs,
  getAllAdminLogs, getAlertConfigs, createAlertConfig, updateAlertConfig, deleteAlertConfig,
  getPendingExercises, approveExercise, rejectExercise, adminAddRemark } from '../../api/http.js'

const router = useRouter()
const route = useRoute()
const { adminToken, adminInfo, isAdminLoggedIn, setAdminLogin, adminLogout } = useAuth()
const { captchaCode, captchaInput, generateCode, drawCaptcha, refreshCaptcha, verifyCaptcha } = useCaptcha()

// ============ 登录 ============
const adminNo = ref('')
const adminPassword = ref('')
const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const captchaCanvas = ref(null)

onMounted(() => {
  if (isAdminLoggedIn.value) {
    // 根据当前路由自动加载对应数据
    initAdminPage()
    return
  }
  generateCode()
  nextTick(() => drawCaptcha(captchaCanvas.value))
})

// 监听路由变化，按页面加载数据（侧边栏点击后可自动刷新）
watch(() => route.path, () => {
  if (isAdminLoggedIn.value) initAdminPage()
})

function initAdminPage() {
  const path = route.path
  if (path === '/admin/dashboard') {
    // 回到总览时重置选中状态
    selectedTeacher.value = null; selectedCourse.value = null
    teacherStudents.value = []; teacherAlerts.value = []
    loadTeachers()
  } else if (path === '/admin/config') {
    loadAlertConfigs()
  } else if (path === '/admin/audit') {
    loadPendingAudits()
  } else if (path === '/admin/logs') {
    loadAllLogs()
  }
}

function doRefresh() { refreshCaptcha(captchaCanvas) }
function setMsg(t, type = 'info') { message.value = t; messageType.value = type }

async function handleLogin() {
  const err = verifyCaptcha()
  if (err) { setMsg(err, 'error'); doRefresh(); return }
  loading.value = true; setMsg('')
  try {
    const res = await adminLogin(adminNo.value, adminPassword.value)
    if (!res.success) { setMsg(res.message || '登录失败', 'error'); doRefresh(); return }
    setAdminLogin(res.token, res.data)
    setMsg(`欢迎，管理员 ${res.data?.adminName}`, 'success')
    router.push('/admin/dashboard')
  } catch { setMsg('无法连接后端', 'error') }
  finally { loading.value = false }
}

// ============ 教师总览 ============
const teachers = ref([])
const selectedTeacher = ref(null)
const selectedCourse = ref(null)
const teacherStudents = ref([])
const teacherAlerts = ref([])
// 按风险等级降序排序（红色 → 橙色 → 黄色 → 正常）
const sortedStudents = computed(() => {
  const list = [...teacherStudents.value]
  const riskOrder = { RED: 0, ORANGE: 1, YELLOW: 2, GREEN: 3 }
  list.sort((a, b) => {
    const getLevel = (s) => {
      const score = (s.scores || []).find(sc => sc.courseId === selectedCourse.value?.courseId)?.calculatedScore
      if (score == null) return 'GREEN'
      if (score < 60) return 'RED'
      if (score < 70) return 'ORANGE'
      if (score < 80) return 'YELLOW'
      return 'GREEN'
    }
    return (riskOrder[getLevel(a)] ?? 3) - (riskOrder[getLevel(b)] ?? 3)
  })
  return list
})
const showManagePanel = ref(false)
const manageRemark = ref('')
const notifyTitle = ref('')
const notifyContent = ref('')
const manageSubmitting = ref(false)
const teacherLogs = ref([])
const allLogs = ref([])

async function loadTeachers() {
  loading.value = true
  try {
    const res = await getAdminTeachers(adminToken.value)
    if (res.success) teachers.value = res.data || []
  } catch { setMsg('获取失败', 'error') }
  finally { loading.value = false }
}

async function selectTeacher(t) {
  selectedTeacher.value = t; selectedCourse.value = null
  teacherStudents.value = []; teacherAlerts.value = []
  router.push(`/admin/teacher/${t.teacherId}`)
  loading.value = true
  try {
    const res = await getAdminTeacherCourses(adminToken.value, t.teacherId)
    if (res.success) selectedTeacher.value._courses = res.data || []
  } catch {}
  finally { loading.value = false }
}

async function selectCourse(c) {
  selectedCourse.value = c
  loading.value = true
  router.push(`/admin/teacher/${selectedTeacher.value.teacherId}/course/${c.courseId}`)
  try {
    const [sRes, aRes] = await Promise.all([
      getAdminTeacherStudents(adminToken.value, selectedTeacher.value.teacherId),
      getAdminTeacherAlerts(adminToken.value, selectedTeacher.value.teacherId)
    ])
    if (sRes.success) teacherStudents.value = sRes.data || []
    if (aRes.success) teacherAlerts.value = aRes.data || []
  } catch {}
  finally { loading.value = false }
}

function backToDashboard() {
  selectedTeacher.value = null; selectedCourse.value = null
  teacherStudents.value = []; teacherAlerts.value = []
  router.push('/admin/dashboard')
}

function backToCourses() { selectedCourse.value = null; teacherStudents.value = []; teacherAlerts.value = [] }

async function markTeacher(status) {
  manageSubmitting.value = true
  try {
    const res = await adminMarkTeacher(adminToken.value, selectedTeacher.value.teacherId, status, manageRemark.value)
    if (res.success) { setMsg('操作成功', 'success'); manageRemark.value = ''; await loadTeachers() }
    else setMsg(res.message || '失败', 'error')
  } catch { setMsg('操作失败', 'error') }
  finally { manageSubmitting.value = false }
}

async function sendNotice() {
  if (!notifyTitle.value.trim()) { setMsg('请输入标题', 'error'); return }
  if (!notifyContent.value.trim()) { setMsg('请输入内容', 'error'); return }
  manageSubmitting.value = true
  try {
    const res = await adminNotifyTeacher(adminToken.value, selectedTeacher.value.teacherId, notifyTitle.value, notifyContent.value)
    if (res.success) { setMsg('已发送', 'success'); notifyTitle.value = ''; notifyContent.value = '' }
    else setMsg(res.message || '失败', 'error')
  } catch { setMsg('失败', 'error') }
  finally { manageSubmitting.value = false }
}

async function loadTeacherLogs() {
  try {
    const res = await getAdminTeacherLogs(adminToken.value, selectedTeacher.value.teacherId)
    if (res.success) teacherLogs.value = res.data || []
  } catch {}
}

async function addTeacherRemark() {
  if (!manageRemark.value.trim()) { setMsg('请输入备注内容', 'error'); return }
  manageSubmitting.value = true
  try {
    const res = await adminAddRemark(adminToken.value, selectedTeacher.value.teacherId, manageRemark.value.trim())
    if (res.success) {
      setMsg('备注已添加', 'success')
      manageRemark.value = ''
      loadTeacherLogs()
    } else setMsg(res.message || '失败', 'error')
  } catch { setMsg('操作失败', 'error') }
  finally { manageSubmitting.value = false }
}

// 管理状态中文标签
function mgmtLabel(status) {
  const map = { NORMAL: '正常', ATTENTION: '关注', WARNING: '警告', RECTIFY: '整改', RESOLVED: '已解决' }
  return map[status] || status || ''
}
// 管理状态对应的卡片边框色
function teacherCardStyle(status) {
  const map = {
    WARNING: 'border-left: 4px solid var(--semantic-red); background: #fef2f2;',
    ATTENTION: 'border-left: 4px solid #f97316; background: #fff7ed;',
    RECTIFY: 'border-left: 4px solid #eab308; background: #fefce8;',
    RESOLVED: 'border-left: 4px solid #22c55e; background: #f0fdf4;'
  }
  return map[status] ? map[status] : ''
}

// 分数颜色
function scoreColor(score) {
  if (score == null) return 'var(--text-tertiary)'
  if (score >= 90) return 'var(--semantic-green)'
  if (score >= 80) return '#7c3aed'
  if (score >= 70) return '#0284c7'
  if (score >= 60) return '#d97706'
  return 'var(--semantic-red)'
}
// 风险等级标签
function alertLevelForScore(score) {
  if (score == null) return { label: '未知', cls: 'tag-green' }
  if (score < 60) return { label: '红色预警', cls: 'tag-red' }
  if (score < 70) return { label: '橙色预警', cls: 'tag-orange' }
  if (score < 80) return { label: '黄色预警', cls: 'tag-yellow' }
  return { label: '正常', cls: 'tag-green' }
}

// 操作类型中文标签
function getActionLabel(log) {
  const map = { MARK_WARNING: '标记警告', RESOLVE: '解除警告', SEND_NOTICE: '发送通知', ADD_REMARK: '添加备注' }
  return map[log.actionType] || log.actionType || '操作'
}

async function loadAllLogs() {
  loading.value = true
  try {
    const res = await getAllAdminLogs(adminToken.value)
    if (res.success) allLogs.value = res.data || []
  } catch {}
  finally { loading.value = false }
}

// ============ 规则配置 ============
const alertConfigs = ref([])
const configLoading = ref(false)
const configEditorOpen = ref(false)
const configEditing = ref(null)

const configEditForm = ref({
  systemType: 'SENIOR', thresholdRed: '40', thresholdOrange: '30', thresholdYellow: '15',
  weightAcademic: '0.300', weightHomework: '0.200', weightKnowledge: '0.150',
  weightAttendance: '0.100', weightHistory: '0.250',
  homeworkSubmitRateThreshold: '70', knowledgeCorrectRateThreshold: '50',
  attendanceRateThreshold: '80', scoreDropThreshold: '15',
  defaultAcademicRisk: '50', defaultHomeworkRisk: '30', defaultAttendanceRisk: '20',
  defaultKnowledgeRisk: '25', defaultHistoryRisk: '15', defaultHistoryFreshmanRisk: '0',
  homeworkSubmitWeight: '40', homeworkOntimeWeight: '30', homeworkAvgScoreWeight: '30',
  attendanceAbsentWeight: '50', attendanceLateWeight: '30', attendanceQuizWeight: '20',
  knowledgeCorrectWeight: '50', knowledgeWeakRateWeight: '30', knowledgeBasicRateWeight: '20',
  historyFailedBonus: '15', historyUnstableBonus: '20', historyNormalBonus: '10',
  studyLookbackWeeks: '4', studyDeclineRatio: '0.50', studyDeclineBonus: '10', studyDefaultRisk: '10',
  predictedPassScore: '60', predictedSubmitThreshold: '70', predictedAttendanceThreshold: '80'
})

async function loadAlertConfigs() {
  configLoading.value = true
  try {
    const res = await getAlertConfigs(adminToken.value)
    if (res.success) alertConfigs.value = res.data || []
  } catch {} finally { configLoading.value = false }
}

function openConfigEditor(config) {
  if (config) {
    configEditing.value = config
    configEditForm.value = { ...configEditForm.value, ...Object.fromEntries(
      Object.entries(config).map(([k,v]) => [k, v != null ? String(v) : configEditForm.value[k] || ''])) }
  } else {
    configEditing.value = null
    const d = {
      systemType: 'SENIOR', thresholdRed: '40', thresholdOrange: '30', thresholdYellow: '15',
      weightAcademic: '0.300', weightHomework: '0.200', weightKnowledge: '0.150',
      weightAttendance: '0.100', weightHistory: '0.250',
      homeworkSubmitRateThreshold: '70', knowledgeCorrectRateThreshold: '50',
      attendanceRateThreshold: '80', scoreDropThreshold: '15',
      defaultAcademicRisk: '50', defaultHomeworkRisk: '30', defaultAttendanceRisk: '20',
      defaultKnowledgeRisk: '25', defaultHistoryRisk: '15', defaultHistoryFreshmanRisk: '0',
      homeworkSubmitWeight: '40', homeworkOntimeWeight: '30', homeworkAvgScoreWeight: '30',
      attendanceAbsentWeight: '50', attendanceLateWeight: '30', attendanceQuizWeight: '20',
      knowledgeCorrectWeight: '50', knowledgeWeakRateWeight: '30', knowledgeBasicRateWeight: '20',
      historyFailedBonus: '15', historyUnstableBonus: '20', historyNormalBonus: '10',
      studyLookbackWeeks: '4', studyDeclineRatio: '0.50', studyDeclineBonus: '10', studyDefaultRisk: '10',
      predictedPassScore: '60', predictedSubmitThreshold: '70', predictedAttendanceThreshold: '80'
    }
    configEditForm.value = d
  }
  configEditorOpen.value = true
}

async function saveAlertConfig() {
  const f = configEditForm.value
  const data = {
    systemType: f.systemType,
    weightAcademic: parseFloat(f.weightAcademic), weightHomework: parseFloat(f.weightHomework),
    weightKnowledge: parseFloat(f.weightKnowledge), weightAttendance: parseFloat(f.weightAttendance),
    weightHistory: parseFloat(f.weightHistory),
    thresholdRed: parseFloat(f.thresholdRed), thresholdOrange: parseFloat(f.thresholdOrange),
    thresholdYellow: parseFloat(f.thresholdYellow),
    homeworkSubmitRateThreshold: parseFloat(f.homeworkSubmitRateThreshold),
    knowledgeCorrectRateThreshold: parseFloat(f.knowledgeCorrectRateThreshold),
    attendanceRateThreshold: parseFloat(f.attendanceRateThreshold),
    scoreDropThreshold: parseFloat(f.scoreDropThreshold),
    defaultAcademicRisk: parseFloat(f.defaultAcademicRisk), defaultHomeworkRisk: parseFloat(f.defaultHomeworkRisk),
    defaultAttendanceRisk: parseFloat(f.defaultAttendanceRisk), defaultKnowledgeRisk: parseFloat(f.defaultKnowledgeRisk),
    defaultHistoryRisk: parseFloat(f.defaultHistoryRisk), defaultHistoryFreshmanRisk: parseFloat(f.defaultHistoryFreshmanRisk),
    homeworkSubmitWeight: parseFloat(f.homeworkSubmitWeight), homeworkOntimeWeight: parseFloat(f.homeworkOntimeWeight),
    homeworkAvgScoreWeight: parseFloat(f.homeworkAvgScoreWeight),
    attendanceAbsentWeight: parseFloat(f.attendanceAbsentWeight), attendanceLateWeight: parseFloat(f.attendanceLateWeight),
    attendanceQuizWeight: parseFloat(f.attendanceQuizWeight),
    knowledgeCorrectWeight: parseFloat(f.knowledgeCorrectWeight), knowledgeWeakRateWeight: parseFloat(f.knowledgeWeakRateWeight),
    knowledgeBasicRateWeight: parseFloat(f.knowledgeBasicRateWeight),
    historyFailedBonus: parseFloat(f.historyFailedBonus), historyUnstableBonus: parseFloat(f.historyUnstableBonus),
    historyNormalBonus: parseFloat(f.historyNormalBonus),
    studyLookbackWeeks: parseInt(f.studyLookbackWeeks), studyDeclineRatio: parseFloat(f.studyDeclineRatio),
    studyDeclineBonus: parseFloat(f.studyDeclineBonus), studyDefaultRisk: parseFloat(f.studyDefaultRisk),
    predictedPassScore: parseFloat(f.predictedPassScore), predictedSubmitThreshold: parseFloat(f.predictedSubmitThreshold),
    predictedAttendanceThreshold: parseFloat(f.predictedAttendanceThreshold),
    isActive: true
  }
  let res
  if (configEditing.value?.id) {
    res = await updateAlertConfig(adminToken.value, configEditing.value.id, data)
  } else {
    res = await createAlertConfig(adminToken.value, data)
  }
  if (res.success) { configEditorOpen.value = false; await loadAlertConfigs() }
  else setMsg(res.message || '保存失败', 'error')
}

async function removeAlertConfig(id) {
  if (!confirm('确定删除？')) return
  try {
    const res = await deleteAlertConfig(adminToken.value, id)
    if (res.success) await loadAlertConfigs()
    else setMsg(res.message || '删除失败', 'error')
  } catch (e) { setMsg(e?.message || '删除失败', 'error') }
}

// ============ 题库审核 ============
const pendingAudits = ref([])
const auditLoading = ref(false)

async function loadPendingAudits() {
  auditLoading.value = true
  try {
    const res = await getPendingExercises(adminToken.value, 1, 50)
    if (res.success && res.data) pendingAudits.value = res.data.records || []
  } catch {} finally { auditLoading.value = false }
}

async function auditApprove(id) {
  if (!confirm('确定通过？')) return
  try {
    const res = await approveExercise(adminToken.value, id)
    if (res.success) { setMsg('审核通过', 'success'); await loadPendingAudits() }
    else setMsg(res.message || '操作失败', 'error')
  } catch (e) { setMsg(e?.message || '操作失败', 'error') }
}

async function auditReject(id) {
  const remark = prompt('驳回理由：')
  if (!remark) return
  try {
    const res = await rejectExercise(adminToken.value, id, remark)
    if (res.success) { setMsg('已驳回', 'info'); await loadPendingAudits() }
    else setMsg(res.message || '操作失败', 'error')
  } catch (e) { setMsg(e?.message || '操作失败', 'error') }
}

// 初始加载
if (isAdminLoggedIn.value) loadTeachers()
</script>

<template>
  <!-- 登录 -->
  <section v-if="!isAdminLoggedIn" class="login-split">
    <div class="login-split-left">
      <div class="login-panel-top">
        <div class="login-brand-icon">🎓</div>
        <h1>学情预警系统</h1>
        <p class="login-brand-sub">Academic Risk Warning System</p>
      </div>
      <div class="login-panel-mid">
        <p class="login-welcome">🛡️ 管理后台</p>
        <p class="login-desc">全局掌控教学风险，统一管理教师与学生数据</p>
        <div class="login-features">
          <div class="login-feat"><span>👥</span> 教师总览看板</div>
          <div class="login-feat"><span>⚙️</span> 预警规则配置</div>
          <div class="login-feat"><span>✅</span> 题库审核管理</div>
          <div class="login-feat"><span>📋</span> 操作日志追溯</div>
        </div>
      </div>
      <div class="login-panel-bot"><span>v2.0 · 管理后台</span></div>
    </div>
    <div class="login-split-right">
      <div class="login-header">
        <div class="login-icon">🛡️</div>
        <h2>管理员登录</h2>
      </div>
      <form @submit.prevent="handleLogin">
        <div class="input-group"><label>管理员账号</label><input v-model="adminNo" placeholder="请输入管理员账号" required /></div>
        <div class="input-group"><label>密码</label><input v-model="adminPassword" type="password" placeholder="请输入密码" required /></div>
        <div class="input-group captcha-group">
          <label>验证码</label>
          <div class="captcha-wrapper">
            <input v-model="captchaInput" type="text" class="captcha-input" placeholder="请输入验证码" maxlength="4" required />
            <canvas ref="captchaCanvas" class="captcha-canvas" @click="doRefresh"></canvas>
          </div>
        </div>
        <button type="submit" class="btn-primary" :disabled="loading" style="width:100%;">{{ loading ? '登录中…' : '登 录' }}</button>
      </form>
      <button class="btn-back-landing" @click="router.push('/')">← 返回首页</button>
    </div>
  </section>

  <!-- 已登录 - 教师总览 -->
  <div v-else class="page admin-page">
    <div class="toolbar card">
      <div class="user-info"><strong>{{ adminInfo?.adminName || '管理员' }}</strong></div>
      <div class="actions">
        <button class="btn-ghost" @click="backToDashboard(); loadTeachers()">👥 教师总览</button>
        <button class="btn-ghost" @click="router.push('/admin/config')">⚙ 规则配置</button>
        <button class="btn-ghost" @click="router.push('/admin/audit')">✅ 题库审核</button>
        <button class="btn-ghost" @click="router.push('/admin/logs')">📋 操作日志</button>
        <button class="btn-ghost" @click="adminLogout(); router.push('/admin/login')">🚪 退出</button>
      </div>
    </div>

    <p v-if="message" class="msg" :class="messageType">{{ message }}</p>

    <!-- 规则配置页 -->
    <div v-if="$route.path === '/admin/config'" class="warning-section">
      <h3>⚙ 预警规则配置</h3>
      <button class="btn-primary" @click="openConfigEditor(null)" style="margin-bottom:12px;">+ 新增配置</button>
      <div v-if="configLoading" class="empty">加载中...</div>
      <div v-else-if="alertConfigs.length" class="alert-list">
        <div v-for="cfg in alertConfigs" :key="cfg.id" class="alert-card">
          <div class="alert-header">
            <span class="alert-type">{{ cfg.systemType === 'SENIOR' ? '高中' : cfg.systemType === 'JUNIOR' ? '初中' : cfg.systemType }}</span>
            <span style="font-size:12px;color:var(--text-secondary);">课程ID: {{ cfg.courseId || '全局' }}</span>
          </div>
          <div class="alert-body">
            <div>阈值：<span style="color:var(--semantic-red-dark)">🔴{{ cfg.thresholdRed }}</span> / <span style="color:var(--semantic-orange-dark)">🟠{{ cfg.thresholdOrange }}</span> / <span style="color:var(--semantic-yellow-dark)">🟡{{ cfg.thresholdYellow }}</span></div>
          </div>
          <div class="alert-footer">
            <button class="btn-ghost" @click="openConfigEditor(cfg)">编辑</button>
            <button class="btn-ghost" style="color:var(--semantic-red);" @click="removeAlertConfig(cfg.id)">删除</button>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无配置</div>
    </div>

    <!-- 配置编辑弹窗 -->
    <div v-if="configEditorOpen" class="modal-overlay" @click.self="configEditorOpen = false">
      <div class="modal-card modal-wide">
        <div class="modal-header"><h2>{{ configEditing ? '编辑规则' : '新增规则' }}</h2><button class="modal-close" @click="configEditorOpen = false">✕</button></div>
        <div class="modal-body" style="max-height:70vh;overflow-y:auto;">
          <div class="detail-section"><h4>基础设置</h4>
            <div class="input-group"><label>体系类型</label><select v-model="configEditForm.systemType"><option value="SENIOR">高中</option><option value="JUNIOR">初中</option></select></div>
            <div class="input-group"><label>🔴 红色阈值</label><input v-model="configEditForm.thresholdRed" type="number" /></div>
            <div class="input-group"><label>🟠 橙色阈值</label><input v-model="configEditForm.thresholdOrange" type="number" /></div>
            <div class="input-group"><label>🟡 黄色阈值</label><input v-model="configEditForm.thresholdYellow" type="number" /></div>
          </div>
          <div class="detail-section"><h4>维度权重</h4>
            <div class="input-group"><label>学业权重</label><input v-model="configEditForm.weightAcademic" /></div>
            <div class="input-group"><label>作业权重</label><input v-model="configEditForm.weightHomework" /></div>
            <div class="input-group"><label>知识点权重</label><input v-model="configEditForm.weightKnowledge" /></div>
            <div class="input-group"><label>出勤权重</label><input v-model="configEditForm.weightAttendance" /></div>
            <div class="input-group"><label>历史风险权重</label><input v-model="configEditForm.weightHistory" /></div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-ghost" @click="configEditorOpen = false">取消</button>
          <button class="btn-primary" @click="saveAlertConfig">保存</button>
        </div>
      </div>
    </div>

    <!-- 题库审核 -->
    <div v-if="$route.path === '/admin/audit'" class="warning-section">
      <h3>✅ 题库审核</h3>
      <button class="btn-primary" @click="loadPendingAudits" style="margin-bottom:12px;">刷新列表</button>
      <div v-if="auditLoading" class="empty">加载中...</div>
      <div v-else-if="pendingAudits.length" class="alert-list">
        <div v-for="ex in pendingAudits" :key="ex.id" class="alert-card">
          <div class="alert-header"><strong>{{ ex.title }}</strong></div>
          <div class="alert-body">
            <div>知识点：{{ ex.knowledgePointName || '未关联' }}</div>
            <div>难度：{{ ex.difficulty }} | 提交时间：{{ ex.createTime }}</div>
          </div>
          <div class="alert-footer">
            <button class="btn-handle" @click="auditApprove(ex.id)">通过</button>
            <button class="btn-dismiss" @click="auditReject(ex.id)">驳回</button>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无待审核习题</div>
    </div>

    <!-- 操作日志 -->
    <div v-if="$route.path === '/admin/logs'" class="warning-section">
      <h3>📋 操作日志</h3>
      <button class="btn-primary" @click="loadAllLogs" style="margin-bottom:12px;">加载日志</button>
      <div v-if="allLogs.length" class="alert-list">
        <div v-for="log in allLogs" :key="log.id" class="alert-card">
          <div class="alert-header"><span class="alert-type">{{ log.actionType }}</span></div>
          <div class="alert-body"><div>{{ log.description }}</div><div style="font-size:12px;color:var(--text-tertiary);">{{ log.createTime }}</div></div>
        </div>
      </div>
      <div v-else class="empty">暂无日志</div>
    </div>

    <!-- 教师列表 -->
    <div v-if="$route.path === '/admin/dashboard' && !selectedTeacher">
      <h3>👥 教师总览</h3>
      <div v-if="teachers.length" class="course-cards">
        <button v-for="t in teachers" :key="t.teacherId" class="course-card-btn"
                :style="teacherCardStyle(t.managementStatus)"
                @click="selectTeacher(t)">
          <div class="course-card-icon">👨‍🏫</div>
          <div class="course-card-name">{{ t.teacherName }}</div>
          <div class="course-card-stats">
            <span class="course-stat">工号：{{ t.teacherNo }}</span>
          </div>
          <div class="course-card-tags">
            <span v-if="t.managementStatus && t.managementStatus !== 'NORMAL'"
                  class="mgmt-tag" :class="'mgmt-' + t.managementStatus.toLowerCase()">
              {{ mgmtLabel(t.managementStatus) }}
            </span>
            <span v-if="t.redAlertCount > 0" class="alert-badge red">{{ t.redAlertCount }}🔴</span>
            <span v-else-if="t.totalAlertCount > 0" class="alert-badge orange">{{ t.totalAlertCount }}⚠️</span>
          </div>
          <div v-if="t.studentCount > 0" class="course-card-sub">
            {{ t.studentCount }} 名学生 · {{ t.totalAlertCount || 0 }} 条预警
          </div>
        </button>
      </div>
      <div v-else class="empty">暂无教师数据</div>
    </div>

    <!-- 教师课程列表 -->
    <div v-if="selectedTeacher && !selectedCourse">
      <h3>{{ selectedTeacher.teacherName }} - 课程列表</h3>
      <button class="btn-ghost" @click="backToDashboard" style="margin-bottom:12px;">← 返回教师列表</button>
      <div v-if="selectedTeacher._courses?.length" class="course-cards">
        <button v-for="c in selectedTeacher._courses" :key="c.courseId" class="course-card-btn" @click="selectCourse(c)">
          <div class="course-card-icon">📖</div>
          <div class="course-card-name">{{ c.courseName }}</div>
        </button>
      </div>
      <div v-else class="empty">暂无课程</div>

      <!-- 教师管理操作面板 -->
      <div style="margin-top:16px;">
        <button class="btn-ghost" @click="showManagePanel = !showManagePanel;
          if(showManagePanel && selectedTeacher) loadTeacherLogs()">⚙ 管理操作</button>
        <div v-if="showManagePanel" class="card" style="margin-top:8px;padding:16px;">
          <h4>管理 {{ selectedTeacher?.teacherName }}</h4>

          <div class="input-group"><label>操作备注</label><input v-model="manageRemark" placeholder="操作备注" /></div>
          <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;">
            <button class="btn-handle" @click="markTeacher('NORMAL')" :disabled="manageSubmitting">✅ 标记正常</button>
            <button class="btn-dismiss" @click="markTeacher('WARNING')" :disabled="manageSubmitting">⚠️ 标记警告</button>
            <button class="btn-ghost" @click="addTeacherRemark" :disabled="manageSubmitting">📝 单独添加备注</button>
          </div>

          <div style="border-top:1px solid var(--border-light);padding-top:12px;margin-top:8px;">
            <div class="input-group"><label>通知标题</label><input v-model="notifyTitle" placeholder="通知标题" /></div>
            <div class="input-group"><label>通知内容</label><textarea v-model="notifyContent" rows="2" placeholder="通知内容" style="width:100%;"></textarea></div>
            <button class="btn-primary" @click="sendNotice" :disabled="manageSubmitting">发送通知</button>
          </div>

          <div style="border-top:1px solid var(--border-light);padding-top:12px;margin-top:12px;">
            <h4 style="font-size:14px;margin-bottom:8px;">操作日志</h4>
            <div v-if="teacherLogs.length" style="max-height:200px;overflow-y:auto;font-size:13px;">
              <div v-for="log in teacherLogs" :key="log.id" style="padding:6px 0;border-bottom:1px solid var(--border-light);display:flex;gap:6px;">
                <span style="color:var(--text-tertiary);white-space:nowrap;">{{ (log.createTime || '').substring(0, 16) }}</span>
                <span :style="{ color: log.actionType === 'MARK_WARNING' ? 'var(--semantic-red)' : 'var(--text-secondary)' }">
                  {{ log.remark || getActionLabel(log) }}
                </span>
              </div>
            </div>
            <div v-else style="font-size:13px;color:var(--text-tertiary);padding:8px 0;">暂无操作日志</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 教师课程详情 -->
    <div v-if="selectedTeacher && selectedCourse">
      <h3>{{ selectedTeacher.teacherName }} - {{ selectedCourse.courseName }}</h3>
      <button class="btn-ghost" @click="backToCourses" style="margin-bottom:12px;">← 返回课程列表</button>

      <div v-if="sortedStudents.length" class="student-list">
        <h4>学生列表 ({{ sortedStudents.length }}人) 按风险↓排序</h4>
        <div v-for="s in sortedStudents" :key="s.basicInfo?.id || s.basicInfo?.studentNo" class="card admin-student-card">
          <!-- 头部：姓名 + 学号 + 风险等级 -->
          <div class="asc-header">
            <div class="asc-user">
              <strong>{{ s.basicInfo?.studentName || '-' }}</strong>
              <span class="asc-sub">{{ s.basicInfo?.studentNo }} · {{ s.basicInfo?.className || '-' }}</span>
            </div>
            <span v-if="(s.scores || []).find(sc => sc.courseId === selectedCourse?.courseId)" class="risk-tag"
                  :class="alertLevelForScore((s.scores || []).find(sc => sc.courseId === selectedCourse?.courseId)?.calculatedScore).cls">
              {{ alertLevelForScore((s.scores || []).find(sc => sc.courseId === selectedCourse?.courseId)?.calculatedScore).label }}
            </span>
            <span v-else class="risk-tag tag-gray">无成绩</span>
          </div>

          <!-- 成绩四列 -->
          <div v-if="(s.scores || []).find(sc => sc.courseId === selectedCourse?.courseId)" class="asc-scores">
            <div v-for="(label, key) in { usualScore: '平时', midScore: '期中', finalScore: '期末', calculatedScore: '综合' }"
                 :key="key" class="asc-score-block"
                 :class="{ highlight: key === 'calculatedScore' }">
              <span class="asc-s-label">{{ label }}</span>
              <span class="asc-s-val"
                    :style="{ color: scoreColor((s.scores || []).find(sc => sc.courseId === selectedCourse?.courseId)?.[key]) }">
                {{ (s.scores || []).find(sc => sc.courseId === selectedCourse?.courseId)?.[key] ?? '-' }}
              </span>
            </div>
          </div>

          <!-- 作业 + 课堂 + 薄弱点 -->
          <div class="asc-meta">
            <span v-if="(s.homeworkList || []).find(h => h.courseId === selectedCourse?.courseId)" class="asc-meta-item">
              📝 作业 {{ (s.homeworkList || []).find(h => h.courseId === selectedCourse?.courseId)?.submitCount ?? 0 }}/{{ 
                ((s.homeworkList || []).find(h => h.courseId === selectedCourse?.courseId)?.submitCount || 0) + 
                ((s.homeworkList || []).find(h => h.courseId === selectedCourse?.courseId)?.notSubmitCount || 0) }} ·
              均分 {{ (s.homeworkList || []).find(h => h.courseId === selectedCourse?.courseId)?.avgScore ?? '-' }}
            </span>
            <span v-if="(s.classPerformances || []).find(p => p.courseId === selectedCourse?.courseId)" class="asc-meta-item">
              🏫 缺勤 {{ (s.classPerformances || []).find(p => p.courseId === selectedCourse?.courseId)?.absentCount ?? 0 }} ·
              迟到 {{ (s.classPerformances || []).find(p => p.courseId === selectedCourse?.courseId)?.lateCount ?? 0 }}
            </span>
            <span class="asc-meta-item">
              📊 薄弱知识点 {{ (s.weakPoints || []).filter(wp => Number(wp.courseId) === Number(selectedCourse?.courseId)).length || 0 }} 个
            </span>
          </div>
        </div>
      </div>
      <div v-else class="empty">暂无学生数据</div>

      <!-- 管理操作面板 -->
      <div style="margin-top:16px;">
        <button class="btn-ghost" @click="showManagePanel = !showManagePanel; if(showManagePanel) loadTeacherLogs()">⚙ 管理操作</button>
        <div v-if="showManagePanel" class="card" style="margin-top:8px;padding:16px;">
          <h4>管理操作</h4>

          <!-- 标记状态 -->
          <div class="input-group"><label>操作备注</label><input v-model="manageRemark" placeholder="操作备注" /></div>
          <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;">
            <button class="btn-handle" @click="markTeacher('NORMAL')" :disabled="manageSubmitting">✅ 标记正常</button>
            <button class="btn-dismiss" @click="markTeacher('WARNING')" :disabled="manageSubmitting">⚠️ 标记警告</button>
            <button class="btn-ghost" @click="addTeacherRemark" :disabled="manageSubmitting">📝 单独添加备注</button>
          </div>

          <!-- 发送通知 -->
          <div style="border-top:1px solid var(--border-light);padding-top:12px;margin-top:8px;">
            <div class="input-group"><label>通知标题</label><input v-model="notifyTitle" placeholder="通知标题" /></div>
            <div class="input-group"><label>通知内容</label><textarea v-model="notifyContent" rows="2" placeholder="通知内容" style="width:100%;"></textarea></div>
            <button class="btn-primary" @click="sendNotice" :disabled="manageSubmitting">发送通知</button>
          </div>

          <!-- 管理日志 -->
          <div style="border-top:1px solid var(--border-light);padding-top:12px;margin-top:12px;">
            <h4 style="font-size:14px;margin-bottom:8px;">操作日志</h4>
            <div v-if="teacherLogs.length" style="max-height:200px;overflow-y:auto;font-size:13px;">
              <div v-for="log in teacherLogs" :key="log.id" style="padding:6px 0;border-bottom:1px solid var(--border-light);display:flex;gap:6px;">
                <span style="color:var(--text-tertiary);white-space:nowrap;">{{ (log.createTime || '').substring(0, 16) }}</span>
                <span :style="{ color: log.actionType === 'MARK_WARNING' ? 'var(--semantic-red)' : 'var(--text-secondary)' }">
                  {{ log.remark || getActionLabel(log) }}
                </span>
              </div>
            </div>
            <div v-else style="font-size:13px;color:var(--text-tertiary);padding:8px 0;">暂无操作日志</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* AdminDashboard — 继承全局 .login-split / .page / .toolbar / .warning-section 等样式 */
.admin-page { padding-bottom: 60px; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

/* 教师卡片 */
.course-card-btn { position: relative; }
.course-card-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 4px; }
.course-card-sub { font-size: 11px; color: var(--text-tertiary); margin-top: 4px; }
.mgmt-tag { display: inline-block; padding: 1px 8px; border-radius: 8px; font-size: 11px; font-weight: 600; }
.mgmt-tag.mgmt-warning { background: #fee2e2; color: #991b1b; }
.mgmt-tag.mgmt-attention { background: #ffedd5; color: #9a3412; }
.mgmt-tag.mgmt-rectify { background: #fef9c3; color: #854d0e; }
.mgmt-tag.mgmt-resolved { background: #dcfce7; color: #166534; }
.alert-badge { display: inline-block; padding: 1px 6px; border-radius: 8px; font-size: 11px; font-weight: 600; }
.alert-badge.red { background: var(--semantic-red-bg); color: var(--semantic-red-text); }
.alert-badge.orange { background: var(--semantic-orange-bg); color: var(--semantic-orange-text); }

/* 课程详情 - 学生卡片 */
.admin-student-card { padding: 14px 16px; margin-bottom: 10px; display: flex; flex-direction: column; gap: 10px; }
.admin-student-card:hover { box-shadow: var(--shadow-md); }
.asc-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.asc-user { display: flex; flex-direction: column; gap: 2px; }
.asc-sub { font-size: 12px; color: var(--text-tertiary); }
.risk-tag { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.risk-tag.tag-red { background: var(--semantic-red-bg); color: var(--semantic-red-text); }
.risk-tag.tag-orange { background: var(--semantic-orange-bg); color: var(--semantic-orange-text); }
.risk-tag.tag-yellow { background: var(--semantic-yellow-bg); color: var(--semantic-yellow-text); }
.risk-tag.tag-green { background: var(--semantic-green-bg); color: var(--semantic-green-text); }
.risk-tag.tag-gray { background: var(--bg-hover); color: var(--text-tertiary); }
.asc-scores { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.asc-score-block { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 6px; border-radius: 6px; background: var(--bg-card-subtle); border: 1px solid var(--border-light); }
.asc-score-block.highlight { background: var(--accent-light); border-color: var(--accent-primary); }
.asc-s-label { font-size: 11px; color: var(--text-tertiary); }
.asc-s-val { font-size: 18px; font-weight: 700; }
.asc-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 13px; color: var(--text-secondary); }
.asc-meta-item { display: inline-flex; align-items: center; gap: 4px; }
</style>
