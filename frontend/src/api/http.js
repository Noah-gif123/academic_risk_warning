const BASE = '/api'

// ==================== Token 自动刷新机制 ====================
let _activeToken = null
let _refreshPromise = null    // 防止并发刷新
let _onTokenExpired = null    // 刷新失败后的回调

export function setActiveToken(token) { _activeToken = token }
export function getActiveToken() { return _activeToken }
export function onTokenExpired(callback) { _onTokenExpired = callback }

/** 根据当前登录身份判断刷新接口和 localStorage key */
function getRefreshConfig() {
  if (localStorage.getItem('adminToken')) {
    return { url: `${BASE}/admin/refresh`, key: 'adminToken', infoKey: 'adminInfo' }
  } else if (localStorage.getItem('studentToken')) {
    return { url: `${BASE}/student/refresh`, key: 'studentToken', infoKey: 'studentInfo' }
  } else {
    // 教师或默认
    return { url: `${BASE}/teacher/refresh`, key: 'token', infoKey: 'teacher' }
  }
}

async function tryRefreshToken() {
  if (_refreshPromise) return _refreshPromise  // 已有刷新进行中
  if (!_activeToken) return null
  _refreshPromise = (async () => {
    try {
      const config = getRefreshConfig()
      const res = await fetch(config.url, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${_activeToken}` }
      })
      const data = await res.json()
      if (data.success && data.token) {
        _activeToken = data.token
        localStorage.setItem(config.key, data.token) // 持久化到正确的 key
        return data.token
      }
      return null
    } catch { return null }
    finally { _refreshPromise = null }
  })()
  return _refreshPromise
}

async function handle401() {
  const newToken = await tryRefreshToken()
  if (!newToken && _onTokenExpired) {
    _onTokenExpired()
  }
  return newToken
}

// ==================== 基础 HTTP 函数 ====================

export async function postJson(path, body, token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) { headers['Authorization'] = `Bearer ${token}` }
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST', headers, body: JSON.stringify(body)
  })
  if (res.status === 401) {
    const newToken = await handle401()
    if (newToken) return postJson(path, body, newToken)  // 重试
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '网络错误')
    throw new Error(`请求失败 (${res.status}): ${text}`)
  }
  return res.json()
}

export async function getJson(path, token) {
  const headers = {}
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, { headers })
  if (res.status === 401) {
    const newToken = await handle401()
    if (newToken) return getJson(path, newToken)  // 重试
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '网络错误')
    throw new Error(`请求失败 (${res.status}): ${text}`)
  }
  return res.json()
}

export async function putJson(path, body, token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) { headers['Authorization'] = `Bearer ${token}` }
  const res = await fetch(`${BASE}${path}`, {
    method: 'PUT', headers, body: JSON.stringify(body)
  })
  if (res.status === 401) {
    const newToken = await handle401()
    if (newToken) return putJson(path, body, newToken)  // 重试
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '网络错误')
    throw new Error(`请求失败 (${res.status}): ${text}`)
  }
  return res.json()
}

export async function delJson(path, token) {
  const headers = {}
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, { method: 'DELETE', headers })
  if (res.status === 401) {
    const newToken = await handle401()
    if (newToken) return delJson(path, newToken)
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '网络错误')
    throw new Error(`请求失败 (${res.status}): ${text}`)
  }
  return res.json()
}

// ========== 预警相关API ==========

/**
 * 获取教师端预警列表
 */
export async function getTeacherAlerts(token, status, courseId) {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  if (courseId) params.set('courseId', courseId)
  const qs = params.toString()
  return getJson(`/alert/teacher/list${qs ? '?' + qs : ''}`, token)
}

/**
 * 获取教师端预警统计
 */
export async function getTeacherStats(token, courseId) {
  const params = courseId ? `?courseId=${courseId}` : ''
  return getJson(`/alert/teacher/stats${params}`, token)
}

/**
 * 教师手动触发预警生成
 */
export async function generateWarnings(token) {
  return postJson('/alert/teacher/generate', {}, token)
}

/**
 * 教师处理预警(含干预类型)
 */
export async function handleAlert(token, alertId, note, interventionType, interventionDesc, exerciseIds) {
  const body = { alertId, note, interventionType, interventionDesc }
  if (exerciseIds && exerciseIds.length) body.exerciseIds = exerciseIds
  return postJson('/alert/teacher/handle', body, token)
}

/**
 * 教师撤销预警
 */
export async function dismissAlert(token, alertId, reason) {
  return postJson('/alert/teacher/dismiss', { alertId, reason }, token)
}

/**
 * 获取学生端预警列表
 */
export async function getStudentAlerts(token, status) {
  const params = status ? `?status=${status}` : ''
  return getJson(`/alert/student/list${params}`, token)
}

/**
 * 学生确认预警
 */
export async function acknowledgeAlert(token, alertId) {
  return postJson('/alert/student/acknowledge', { alertId }, token)
}

/**
 * 学生回应教师处理意见
 */
/**
 * 批量处理
 */
export async function batchHandle(token, alertIds, note, interventionType, interventionDesc) {
  return postJson('/alert/teacher/batch-handle', { alertIds, note, interventionType, interventionDesc }, token)
}

/**
 * 批量撤销
 */
export async function batchDismiss(token, alertIds, note) {
  return postJson('/alert/teacher/batch-dismiss', { alertIds, note }, token)
}

/**
 * 批量关闭
 */
export async function batchClose(token, alertIds, note) {
  return postJson('/alert/teacher/batch-close', { alertIds, note }, token)
}

export async function studentRespond(token, alertId, response) {
  return postJson('/alert/student/respond', { alertId, response }, token)
}

/**
 * 教师仪表盘摘要
 */
export async function getDashboard(token, courseId) {
  const params = courseId ? `?courseId=${courseId}` : ''
  return getJson(`/alert/teacher/dashboard${params}`, token)
}

/**
 * Top N 风险学生
 */
export async function getTopRisk(token, limit = 10, courseId) {
  let url = `/alert/teacher/top-risk?limit=${limit}`
  if (courseId) url += `&courseId=${courseId}`
  return getJson(url, token)
}

/**
 * 班级热力图
 */
export async function getClassHeatmap(token) {
  return getJson('/alert/teacher/class-heatmap', token)
}

/**
 * 获取汇总报告列表
 */
export async function getSummaries(token, period = 'DAILY', limit = 10) {
  return getJson('/alert/teacher/summaries?period=' + period + '&limit=' + limit, token)
}

/**
 * 手动生成汇总报告
 */
export async function generateSummary(token, period = 'DAILY') {
  return postJson('/alert/teacher/generate-summary?period=' + period, {}, token)
}

/**
 * 手动触发全量预警分析
 */
export async function generateAllWarnings(token) {
  return postJson('/alert/teacher/generate-all', {}, token)
}

/**
 * 学生风险趋势
 */
export async function getStudentTrend(token, studentId, courseId) {
  return getJson('/alert/student/' + studentId + '/trend?courseId=' + courseId, token)
}

/**
 * 学生风险雷达图
 */
export async function getStudentRadar(token, studentId, courseId) {
  return getJson('/alert/student/' + studentId + '/radar?courseId=' + courseId, token)
}

/**
 * 获取通知列表
 */
export async function getNotifications(token, type) {
  return getJson('/alert/notifications?type=' + (type || 'TEACHER'), token)
}

/**
 * 获取未读通知数
 */
export async function getUnreadCount(token, type) {
  return getJson('/alert/notifications/unread-count?type=' + (type || 'TEACHER'), token)
}

/**
 * 标记通知已读
 */
export async function markNotificationRead(token, id, type) {
  return putJson('/alert/notifications/' + id + '/read?type=' + (type || 'TEACHER'), {}, token)
}

/**
 * 全部已读
 */
export async function markAllNotificationsRead(token, type) {
  return putJson('/alert/notifications/read-all?type=' + (type || 'TEACHER'), {}, token)
}

// ========== 管理员端 API ==========

/**
 * 管理员登录
 */
export async function adminLogin(adminNo, password) {
  return postJson('/admin/login', { adminNo, password })
}

/**
 * 获取所有教师列表（含风险统计）
 */
export async function getAdminTeachers(token) {
  return getJson('/admin/teachers', token)
}

/**
 * 获取指定教师的课程列表
 */
export async function getAdminTeacherCourses(token, teacherId) {
  return getJson('/admin/teachers/' + teacherId + '/courses', token)
}

/**
 * 获取指定教师的学生列表
 */
export async function getAdminTeacherStudents(token, teacherId) {
  return getJson('/admin/teachers/' + teacherId + '/students', token)
}

/**
 * 获取指定教师的预警列表
 */
export async function getAdminTeacherAlerts(token, teacherId, status) {
  const params = status ? '?status=' + status : ''
  return getJson('/admin/teachers/' + teacherId + '/alerts' + params, token)
}

/**
 * 获取指定教师的仪表盘数据
 */
export async function getAdminTeacherDashboard(token, teacherId) {
  return getJson('/admin/teachers/' + teacherId + '/dashboard', token)
}

/**
 * 标记教师管理状态
 */
export async function adminMarkTeacher(token, teacherId, status, remark) {
  return postJson('/admin/teachers/' + teacherId + '/mark', { status, remark: remark || '' }, token)
}

/**
 * 向教师发送通知
 */
export async function adminNotifyTeacher(token, teacherId, title, content) {
  return postJson('/admin/teachers/' + teacherId + '/notify', { title, content }, token)
}

/**
 * 获取教师管理日志
 */
export async function getAdminTeacherLogs(token, teacherId) {
  return getJson('/admin/teachers/' + teacherId + '/logs', token)
}

/**
 * 获取全部管理操作日志
 */
export async function getAllAdminLogs(token) {
  return getJson('/admin/logs', token)
}

/**
 * 添加管理备注
 */
export async function adminAddRemark(token, teacherId, remark) {
  return postJson('/admin/teachers/' + teacherId + '/remark', { remark }, token)
}

// ========== 练习题与知识点 API ==========

/**
 * 获取课程知识点树
 */
export async function getKnowledgeTree(token, courseId) {
  return getJson('/exercise/knowledge/tree?courseId=' + courseId, token)
}

/**
 * 添加知识点
 */
export async function addKnowledgePoint(token, courseId, parentId, name) {
  return postJson('/exercise/knowledge/add', { courseId, parentId, name }, token)
}

/**
 * 更新知识点介绍
 */
export async function updateKnowledgeDesc(token, id, description) {
  return putJson('/exercise/knowledge/' + id + '/desc', { description }, token)
}

/**
 * 删除知识点
 */
export async function deleteKnowledgePoint(token, id) {
  return delJson('/exercise/knowledge/' + id, token)
}

/**
 * 教师获取题库
 */
export async function getExerciseList(token, courseId) {
  const params = courseId ? '?courseId=' + courseId : ''
  return getJson('/exercise/list' + params, token)
}

/**
 * 添加练习题（支持多知识点 + 题目类型 + 标准答案）
 */
export async function addExercise(token, courseId, kpIds, title, description, difficulty, questionType, answer) {
  const fd = new FormData()
  fd.append('courseId', courseId)
  kpIds.forEach(id => fd.append('kpIds', id))
  fd.append('title', title)
  fd.append('description', description || '')
  fd.append('difficulty', difficulty)
  if (questionType) fd.append('questionType', questionType)
  if (answer) fd.append('answer', answer)
  const res = await fetch('/api/exercise/add', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + token },
    body: fd
  })
  return res.json()
}

/**
 * 添加练习题 — 带文件附件（FormData 提交）
 */
export async function addExerciseWithFile(token, courseId, kpIds, title, description, difficulty, questionType, answer, file) {
  const fd = new FormData()
  fd.append('courseId', courseId)
  kpIds.forEach(id => fd.append('kpIds', id))
  fd.append('title', title)
  fd.append('description', description || '')
  fd.append('difficulty', difficulty)
  if (questionType) fd.append('questionType', questionType)
  if (answer) fd.append('answer', answer)
  fd.append('file', file)
  const res = await fetch('/api/exercise/add', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + token },
    body: fd
  })
  return res.json()
}

/**
 * 删除练习题
 */
export async function deleteExercise(token, id) {
  return fetch('/api/exercise/' + id, {
    method: 'DELETE',
    headers: { Authorization: 'Bearer ' + token }
  }).then(r => r.json())
}

/**
 * 获取学生薄弱点
 */
export async function getStudentWeakPoints(token, studentId, courseId) {
  return getJson('/exercise/student-weak-points?studentId=' + studentId + '&courseId=' + courseId, token)
}

/**
 * 匹配练习题（按知识点）
 */
export async function matchExercises(token, courseId, kpIds) {
  const ids = kpIds.join(',')
  return getJson('/exercise/match?courseId=' + courseId + '&kpIds=' + ids, token)
}

/**
 * 推荐练习给学生
 */
export async function recommendExercise(token, exerciseId, studentId, alertId) {
  return postJson('/exercise/recommend', { exerciseId, studentId, alertId }, token)
}

/**
 * 获取某预警的推荐记录
 */
export async function getRecommendList(token, alertId) {
  return getJson('/exercise/recommend/list?alertId=' + alertId, token)
}

/**
 * 学生获取推荐列表
 */
export async function getMyRecommends(token) {
  return getJson('/exercise/my-recommends', token)
}

/**
 * 学生提交作答
 */
export async function submitAnswer(token, recommendId, answer) {
  return postJson('/exercise/submit', { recommendId, answer }, token)
}

/**
 * 教师打分反馈
 */
export async function gradeFeedback(token, recommendId, score, feedback) {
  return postJson('/exercise/grade', { recommendId, score, feedback }, token)
}

/**
 * 教师打分反馈（支持子题分题评分）
 */
export async function gradeFeedbackSub(token, recommendId, subScores, feedback) {
  return postJson('/exercise/grade', { recommendId, subScores, feedback }, token)
}

/**
 * 添加子题（拆分PDF习题）
 */
export async function addSubQuestions(token, exerciseId, subQuestions) {
  return postJson('/exercise/' + exerciseId + '/sub-questions', subQuestions, token)
}

/**
 * 获取习题的子题目列表
 */
export async function getSubQuestions(token, exerciseId) {
  return getJson('/exercise/' + exerciseId + '/sub-questions', token)
}

/**
 * 获取推荐的子题作答记录（教师评分时查看）
 */
export async function getSubAnswers(token, recommendId) {
  return getJson('/exercise/recommend/' + recommendId + '/sub-answers', token)
}

/**
 * 学生提交作答（支持子题分题作答）
 */
export async function submitAnswerSub(token, recommendId, subAnswers) {
  return postJson('/exercise/submit', { recommendId, subAnswers }, token)
}

// ========== 作答草稿API ==========

/**
 * 保存作答草稿到后端
 */
export async function saveDraft(token, recommendId, answer, subAnswers) {
  return postJson('/exercise/draft/save', {
    recommendId,
    answer: answer || '',
    subAnswers: subAnswers ? JSON.stringify(subAnswers) : null
  }, token)
}

/**
 * 从后端获取作答草稿
 */
export async function getDraft(token, recommendId) {
  return getJson('/exercise/draft/' + recommendId, token)
}

/**
 * 删除作答草稿
 */
export async function deleteDraft(token, recommendId) {
  const res = await fetch('/api/exercise/draft/' + recommendId, {
    method: 'DELETE',
    headers: { Authorization: 'Bearer ' + token }
  })
  return res.json()
}

// ========== 预警规则配置API (管理员端) ==========

/** 获取规则配置列表 */
export async function getAlertConfigs(token, systemType) {
  const q = systemType ? `?systemType=${systemType}` : ''
  return getJson('/admin/config/list' + q, token)
}

/** 创建规则配置 */
export async function createAlertConfig(token, config) {
  return postJson('/admin/config', config, token)
}

/** 更新规则配置 */
export async function updateAlertConfig(token, id, config) {
  return putJson('/admin/config/' + id, config, token)
}

/** 删除规则配置 */
export async function deleteAlertConfig(token, id) {
  return (await fetch('/api/admin/config/' + id, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  })).json()
}

// ========== 题库审核API (管理员端) ==========

/** 获取待审核练习题列表 */
export async function getPendingExercises(token, page = 1, size = 20) {
  return getJson(`/admin/exercises/pending?page=${page}&size=${size}`, token)
}

/** 审核通过 */
export async function approveExercise(token, id) {
  return (await fetch('/api/admin/exercises/' + id + '/approve', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  })).json()
}

/** 审核驳回 */
export async function rejectExercise(token, id, remark) {
  return postJson('/admin/exercises/' + id + '/reject', { remark }, token)
}

// ========== 多智能体API (Agent) ==========

/**
 * 完整评估：执行 Monitor→Analysis→Profile→Recommend 流水线
 */
export async function agentFullAssessment(token, studentId, courseId) {
  return postJson(`/agent/full-assessment/${studentId}`, { courseId }, token)
}

/**
 * 提交学生反馈，触发 Feedback→Strategy→Recommend 流水线
 */
export async function agentFeedback(token, studentId, courseId, message) {
  return postJson('/agent/feedback', { studentId, courseId, message }, token)
}

/**
 * 端到端闭环：完整评估 + 反馈 + 策略调整 + 重推荐
 */
export async function agentCloseLoop(token, studentId, courseId, feedback) {
  return postJson(`/agent/close-loop/${studentId}`, { courseId, feedback }, token)
}

/**
 * 效果驱动自动调整：基于练习数据自动调整策略（无需人工反馈）
 */
export async function agentEffectCheck(token, studentId, courseId) {
  return postJson(`/agent/effect-check/${studentId}`, { courseId }, token)
}

/**
 * RAG 知识库问答（百炼知识库）
 */
export async function agentQA(token, question) {
  return postJson('/agent/qa', { question }, token)
}

/**
 * 批量效果检查
 */
export async function agentBatchEffectCheck(token, studentIds, courseId) {
  return postJson('/agent/batch-effect-check', { studentIds, courseId }, token)
}

/**
 * 学习计划反馈快捷入口
 */
export async function agentPlanFeedback(token, studentId, planId, feedbackType, comment) {
  return postJson('/agent/plan-feedback', { studentId, planId, feedbackType, comment }, token)
}

/**
 * 获取流水线统计信息
 */
export async function agentStats(token) {
  return getJson('/agent/stats', token)
}

// ========== 教师端智能体 API (新) ==========

/**
 * 获取教师授课课程列表
 */
export async function teacherAgentCourses(token, teacherId) {
  return getJson(`/agent/teacher/courses?teacherId=${teacherId}`, token)
}

/**
 * 获取数据驱动的推荐问题
 */
export async function teacherAgentSuggestions(token, teacherId, courseId) {
  const params = courseId ? `teacherId=${teacherId}&courseId=${courseId}` : `teacherId=${teacherId}`
  return getJson(`/agent/teacher/suggestions?${params}`, token)
}

/**
 * 课程风险概览（真实数据 + AI 解读）
 */
export async function teacherAgentRiskOverview(token, teacherId, courseId) {
  return getJson(`/agent/teacher/risk-overview?teacherId=${teacherId}&courseId=${courseId}`, token)
}

/**
 * 课程知识点薄弱项
 */
export async function teacherAgentWeakPoints(token, teacherId, courseId) {
  return getJson(`/agent/teacher/weak-points?teacherId=${teacherId}&courseId=${courseId}`, token)
}

/**
 * 预警规则配置
 */
export async function teacherAgentAlertRules(token, courseId) {
  return getJson(`/agent/teacher/alert-rules?courseId=${courseId}`, token)
}

/**
 * 生成干预方案（POST）
 */
export async function teacherAgentInterventionPlan(token, teacherId, courseId) {
  return postJson('/agent/teacher/intervention-plan', { teacherId, courseId }, token)
}

/**
 * 生成教学建议（POST）
 */
export async function teacherAgentTeachingAdvice(token, teacherId, courseId) {
  return postJson('/agent/teacher/teaching-advice', { teacherId, courseId }, token)
}

/**
 * 教师端通用 RAG 问答（POST）
 */
export async function teacherAgentQA(token, question) {
  return postJson('/agent/teacher/qa', { question }, token)
}

// ==================== 学生端 AI 助手 ====================

/**
 * 学生端智能对话
 */
export async function studentAgentChat(token, question, courseId) {
  return postJson('/agent/student/chat', { question, courseId }, token)
}

/**
 * 获取学生课程列表（用于课程选择器）
 */
export async function studentAgentCourses(token) {
  return getJson('/agent/student/courses', token)
}

/**
 * 获取学生端动态推荐问题
 */
export async function studentAgentSuggestions(token, courseId) {
  const params = courseId ? `courseId=${courseId}` : ''
  return getJson(`/agent/student/suggestions?${params}`, token)
}

// ==================== 数据导入 API ====================

/**
 * 批量数据导入（Excel 文件上传）
 */
export async function importExcelData(token, file) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch('/api/import/upload', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + token },
    body: fd
  })
  return res.json()
}

/**
 * 下载导入模板
 */
export async function downloadImportTemplate(token) {
  const res = await fetch('/api/import/template', {
    headers: { Authorization: 'Bearer ' + token }
  })
  if (!res.ok) throw new Error('下载失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '数据导入模板.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

// ==================== 待批改练习 (教师独立入口) ====================

/**
 * 获取待批改练习列表（ANSWERED 状态）
 */
export async function getPendingGrading(token, courseId, page = 1, size = 20) {
  return getJson(`/exercise/pending-grading?courseId=${courseId}&page=${page}&size=${size}`, token)
}

// ==================== 学生学习目标 API ====================

/** 查询学习目标 */
export async function getStudentGoal(token, courseId) {
  return getJson(`/student/goal?courseId=${courseId}`, token)
}

/** 设置学习目标 */
export async function saveStudentGoal(token, data) {
  return postJson('/student/goal', data, token)
}
