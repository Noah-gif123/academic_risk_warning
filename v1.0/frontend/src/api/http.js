const BASE = '/api'

// ==================== Token 自动刷新机制 ====================
let _activeToken = null
let _refreshPromise = null    // 防止并发刷新
let _onTokenExpired = null    // 刷新失败后的回调
let _onTokenRefreshed = null  // 刷新成功后的回调（用于同步 useAuth 里的 ref）

export function setActiveToken(token) { _activeToken = token || '' }
export function getActiveToken() { return _activeToken }
export function onTokenExpired(callback) { _onTokenExpired = callback }
export function onTokenRefreshed(callback) { _onTokenRefreshed = callback }

/**
 * 判断当前 token 属于哪个身份：直接和 localStorage 里的三种 token 比对，
 * 而不是"看哪个 key 存在/按顺序猜"——否则教师 token 过期时可能打到
 * /student/refresh（学生与教师 token 共用同一存储，容易串）。
 */
function getRefreshConfig() {
  const teacher = localStorage.getItem('token')
  const student = localStorage.getItem('studentToken')
  const admin = localStorage.getItem('adminToken')
  if (_activeToken && admin && _activeToken === admin) {
    return { url: `${BASE}/admin/refresh`, key: 'adminToken' }
  }
  if (_activeToken && student && _activeToken === student) {
    return { url: `${BASE}/student/refresh`, key: 'studentToken' }
  }
  return { url: `${BASE}/teacher/refresh`, key: 'token' }
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
      if (!res.ok) return null
      const data = await res.json()
      if (data && data.success && data.token) {
        _activeToken = data.token
        localStorage.setItem(config.key, data.token) // 持久化到正确的 key
        // 通知 useAuth 同步自己的 ref，否则组件还会继续用旧 token 发请求
        if (_onTokenRefreshed) {
          try { _onTokenRefreshed({ token: data.token, key: config.key }) } catch { /* ignore */ }
        }
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
    try { _onTokenExpired() } catch { /* ignore */ }
  }
  return newToken
}

// ==================== 统一请求封装 ====================

/**
 * 统一请求入口：401 自动续期（只重试一次）、错误信息提取、响应格式解析。
 *
 * @param {string} method  HTTP 方法
 * @param {string} path    以 / 开头的接口路径（不含 /api 前缀）
 * @param {object} options token / body(JSON) / formData(FormData) / raw('blob'|'text')
 */
async function request(method, path, { token, body, formData, raw } = {}) {
  const finalToken = token || _activeToken
  const headers = {}
  if (finalToken) headers['Authorization'] = `Bearer ${finalToken}`

  let payload
  if (formData) {
    payload = formData // Content-Type 交给浏览器自动带 boundary
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  const res = await fetch(`${BASE}${path}`, { method, headers, body: payload })

  if (res.status === 401) {
    const newToken = await handle401()
    if (!newToken) {
      const err = new Error('登录已过期，请重新登录')
      err.status = 401
      throw err
    }
    const retryHeaders = { ...headers, 'Authorization': `Bearer ${newToken}` }
    const retry = await fetch(`${BASE}${path}`, { method, headers: retryHeaders, body: payload })
    if (!retry.ok) throw await buildError(retry)
    return parseResponse(retry, raw)
  }
  if (!res.ok) throw await buildError(res)
  return parseResponse(res, raw)
}

/** 把后端返回的错误整理成可读文案（优先取 body.message，不再是整段 HTML） */
async function buildError(res) {
  let message = ''
  try {
    const text = await res.text()
    try {
      const data = JSON.parse(text)
      message = (data && (data.message || data.error)) || text
    } catch {
      message = text
    }
  } catch {
    message = '网络错误'
  }
  const err = new Error(`请求失败 (${res.status})：${message || '未知错误'}`)
  err.status = res.status
  return err
}

/** 解析响应：默认 JSON，空响应体返回 null，避免 JSON.parse 抛错 */
function parseResponse(res, raw) {
  if (raw === 'blob') return res.blob()
  if (raw === 'text') return res.text()
  return res.text().then((text) => (text ? JSON.parse(text) : null))
}

// ==================== 基础 HTTP 函数 ====================

export function postJson(path, body, token) {
  return request('POST', path, { token, body: body ?? {} })
}

export function getJson(path, token) {
  return request('GET', path, { token })
}

export function putJson(path, body, token) {
  return request('PUT', path, { token, body: body ?? {} })
}

export function delJson(path, token) {
  return request('DELETE', path, { token })
}

/** FormData 上传（文件/多字段），与 JSON 请求共用 401 续期与错误处理 */
export function postForm(path, formData, token) {
  return request('POST', path, { token, formData })
}

/** 下载文件：返回 Blob，同样支持 401 续期 */
export function getBlob(path, token) {
  return request('GET', path, { token, raw: 'blob' })
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
  return postForm('/exercise/add', fd, token)
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
  return postForm('/exercise/add', fd, token)
}

/**
 * 删除练习题
 */
export async function deleteExercise(token, id) {
  return delJson('/exercise/' + id, token)
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
  return delJson('/exercise/draft/' + recommendId, token)
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
  return delJson('/admin/config/' + id, token)
}

// ========== 题库审核API (管理员端) ==========

/** 获取待审核练习题列表 */
export async function getPendingExercises(token, page = 1, size = 20) {
  return getJson(`/admin/exercises/pending?page=${page}&size=${size}`, token)
}

/** 审核通过 */
export async function approveExercise(token, id) {
  return postJson('/admin/exercises/' + id + '/approve', {}, token)
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
 * 工具调用问答（W3）：智能体自主调用学情查询工具后作答，返回 answer + toolTrace
 */
export async function agentAsk(token, studentId, courseId, question) {
  return postJson('/agent/ask', { studentId, courseId, question }, token)
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
  return postForm('/import/upload', fd, token)
}

/**
 * 下载导入模板
 */
export async function downloadImportTemplate(token) {
  const blob = await getBlob('/import/template', token)
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

// ==================== 三维学情画像 API ====================

/**
 * 学生查看自己的三维学情画像（知识掌握 / 学习习惯 / 学习目标）
 */
export async function getMyProfile(token, courseId) {
  const q = courseId ? `?courseId=${courseId}` : ''
  return getJson('/profile/my' + q, token)
}

/**
 * 教师查看指定学生的三维学情画像（学生账号只能查本人）
 */
export async function getStudentProfile(token, studentId, courseId) {
  const q = courseId ? `?courseId=${courseId}` : ''
  return getJson(`/profile/student/${studentId}${q}`, token)
}

// ==================== 每日全量快照 API（方案A） ====================

/**
 * 为当前教师的学生生成"当日全量快照"（含未触发预警的学生）
 */
export async function generateSnapshots(token) {
  return postJson('/alert/teacher/generate-snapshots', {}, token)
}

/**
 * 为全体学生生成"当日全量快照"（演示/管理用）
 */
export async function generateAllSnapshots(token) {
  return postJson('/alert/teacher/generate-snapshots-all', {}, token)
}

// ==================== 智能体运行记录 API（W1：可观测） ====================

/** 某学生（不传则全体）的智能体运行历史，含各步明细 */
export async function getAgentRuns(token, studentId, limit = 5) {
  const q = studentId ? `?studentId=${studentId}&limit=${limit}` : `?limit=${limit}`
  return getJson('/agent/runs' + q, token)
}

/** 单次运行明细 */
export async function getAgentRunDetail(token, runId) {
  return getJson('/agent/runs/' + runId, token)
}

/** 智能体真实运行统计（成功率、各智能体失败率与平均耗时、P95、最近运行） */
export async function getAgentStats(token) {
  return getJson('/agent/stats', token)
}

/** Golden set 评测（live=false 用已落库运行结果评测） */
export async function getAgentEval(token, live = false, limit = 20) {
  return getJson(`/agent/eval?live=${live}&limit=${limit}`, token)
}
