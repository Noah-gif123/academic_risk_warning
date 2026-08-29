<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import {
  getTeacherAlerts, getTeacherStats, handleAlert, dismissAlert,
  batchHandle, batchDismiss, batchClose, getRecommendList,
  generateWarnings, getExerciseList
} from '../../api/http.js'

const { teacherToken } = useAuth()
const props = defineProps({ courseId: { type: Number, required: true } })
const emit = defineEmits(['message'])

const alerts = ref([])
const stats = ref({})
const loading = ref(false)
const filterStatus = ref('all')
const selectedAlertIds = ref([])
const page = ref(1)
const total = ref(0)
const pageSize = 20

// 处理弹窗
const showHandleModal = ref(false)
const handleAlertId = ref(null)
const handleNote = ref('')
const handleInterventionType = ref('COUNSEL')
const handleInterventionDesc = ref('')
const handleSubmitting = ref(false)

// 布置练习（干预类型为EXERCISE时显示）
const handleExercises = ref([])
const handleExerciseLoading = ref(false)
const selectedExerciseIds = ref([])
const exerciseSearchKeyword = ref('')
const exerciseFilterDifficulty = ref('all')

// 撤销弹窗
const showDismissModal = ref(false)
const dismissAlertId = ref(null)
const dismissReason = ref('')

// 推荐记录
const showRecommendModal = ref(false)
const recommendAlertId = ref(null)
const recommends = ref([])
const recommendLoading = ref(false)

onMounted(() => {
  loadAlerts()
  loadStats()
})

async function loadStats() {
  try {
    const res = await getTeacherStats(teacherToken.value, props.courseId)
    if (res.success) stats.value = res.data || {}
  } catch { /* ignore */ }
}

async function loadAlerts() {
  loading.value = true
  try {
    const res = await getTeacherAlerts(teacherToken.value,
      filterStatus.value === 'all' ? null : filterStatus.value, props.courseId)
    if (res.success) {
      const data = res.data || []
      alerts.value = data
      total.value = res.total || data.length
    } else { emit('message', res.message || '获取预警失败', 'error') }
  } catch { emit('message', '获取预警失败', 'error') }
  finally { loading.value = false }
}

function changeFilter(status) {
  filterStatus.value = status
  selectedAlertIds.value = []
  loadAlerts()
}

function toggleSelectAll() {
  if (selectedAlertIds.value.length === alerts.value.length) {
    selectedAlertIds.value = []
  } else {
    selectedAlertIds.value = alerts.value.map(a => a.id || a.alertId)
  }
}

function toggleSelect(id) {
  const idx = selectedAlertIds.value.indexOf(id)
  if (idx >= 0) selectedAlertIds.value.splice(idx, 1)
  else selectedAlertIds.value.push(id)
}

function getStatusLabel(a) {
  // 优先使用后端已映射好的中文状态名
  if (a.statusName) return a.statusName
  const status = a.status || a.alertStatus || 'ACTIVE'
  const map = { ACTIVE: '待处理', HANDLED: '已处理', ACKNOWLEDGED: '已确认', CLOSED: '已闭环', DISMISSED: '已撤销' }
  return map[status] || status
}

function getLevelLabel(a) {
  const lv = a.alertLevel || a.level || 'GREEN'
  const map = { RED: '🔴 红色', ORANGE: '🟠 橙色', YELLOW: '🟡 黄色', GREEN: '🟢 正常' }
  return map[lv] || '🟢 正常'
}

function getLevelClass(a) {
  const lv = (a.alertLevel || a.level || 'GREEN').toLowerCase()
  return 'level-tag ' + lv
}

function getDiffLabel(d) {
  const m = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return m[d] || d
}

// 切换干预类型时清空已选题目
watch(handleInterventionType, (newVal) => {
  if (newVal !== 'EXERCISE') selectedExerciseIds.value = []
})

// 处理
async function openHandle(id) {
  handleAlertId.value = id; handleNote.value = ''; handleInterventionType.value = 'COUNSEL'; handleInterventionDesc.value = '';
  selectedExerciseIds.value = []; exerciseSearchKeyword.value = ''; exerciseFilterDifficulty.value = 'all';
  showHandleModal.value = true
  // 提前加载题库，切换类型时即用即显
  loadHandleExercises()
}
async function loadHandleExercises() {
  handleExerciseLoading.value = true
  try {
    const res = await getExerciseList(teacherToken.value, props.courseId)
    if (res.success) {
      handleExercises.value = (res.data && res.data.records) ? res.data.records : (res.data || [])
    } else {
      handleExercises.value = []
    }
  } catch { handleExercises.value = [] }
  finally { handleExerciseLoading.value = false }
}
function toggleExerciseSelect(id) {
  const idx = selectedExerciseIds.value.indexOf(id)
  if (idx >= 0) selectedExerciseIds.value.splice(idx, 1)
  else selectedExerciseIds.value.push(id)
}
const filteredHandleExercises = computed(() => {
  let list = handleExercises.value
  if (exerciseSearchKeyword.value) {
    const kw = exerciseSearchKeyword.value.toLowerCase()
    list = list.filter(e => (e.title || '').toLowerCase().includes(kw))
  }
  if (exerciseFilterDifficulty.value !== 'all') {
    list = list.filter(e => e.difficulty === exerciseFilterDifficulty.value)
  }
  return list
})

async function doHandle() {
  handleSubmitting.value = true
  try {
    const exIds = handleInterventionType.value === 'EXERCISE' ? selectedExerciseIds.value : undefined
    const res = await handleAlert(teacherToken.value, handleAlertId.value, handleNote.value,
      handleInterventionType.value, handleInterventionDesc.value, exIds)
    if (res.success) { emit('message', '处理成功', 'success'); showHandleModal.value = false; loadAlerts(); loadStats() }
    else emit('message', res.message || '处理失败', 'error')
  } catch { emit('message', '处理失败', 'error') }
  finally { handleSubmitting.value = false }
}

// 撤销
function openDismiss(id) { dismissAlertId.value = id; dismissReason.value = ''; showDismissModal.value = true }
async function doDismiss() {
  try {
    const res = await dismissAlert(teacherToken.value, dismissAlertId.value, dismissReason.value)
    if (res.success) { emit('message', '已撤销', 'success'); showDismissModal.value = false; loadAlerts(); loadStats() }
    else emit('message', res.message || '撤销失败', 'error')
  } catch { emit('message', '撤销失败', 'error') }
}

// 批量操作
async function doBatchHandle() {
  if (!selectedAlertIds.value.length) return
  loading.value = true
  try {
    const res = await batchHandle(teacherToken.value, selectedAlertIds.value, '批量处理', 'COUNSEL', '')
    emit('message', res.message || `已批量处理 ${selectedAlertIds.value.length} 条`, 'success')
    selectedAlertIds.value = []
    loadAlerts()
  } catch { emit('message', '批量处理失败', 'error') }
  finally { loading.value = false }
}

async function doBatchDismiss() {
  if (!selectedAlertIds.value.length) return
  loading.value = true
  try {
    const res = await batchDismiss(teacherToken.value, selectedAlertIds.value, '批量撤销')
    emit('message', res.message || '已批量撤销', 'success')
    selectedAlertIds.value = []
    loadAlerts()
  } catch { emit('message', '批量撤销失败', 'error') }
  finally { loading.value = false }
}

async function doBatchClose() {
  if (!selectedAlertIds.value.length) return
  loading.value = true
  try {
    const res = await batchClose(teacherToken.value, selectedAlertIds.value, '')
    emit('message', res.message || '已批量闭环', 'success')
    selectedAlertIds.value = []
    loadAlerts()
  } catch { emit('message', '批量闭环失败', 'error') }
  finally { loading.value = false }
}

// 推荐记录
async function openRecommends(alertId) {
  recommendAlertId.value = alertId
  showRecommendModal.value = true
  recommendLoading.value = true
  try {
    const res = await getRecommendList(teacherToken.value, alertId)
    recommends.value = (res.data || res || [])
  } catch { emit('message', '获取推荐记录失败', 'error') }
  finally { recommendLoading.value = false }
}

async function doGenerateWarnings() {
  loading.value = true
  try {
    const res = await generateWarnings(teacherToken.value)
    emit('message', res.message || '预警生成完成', 'success')
    loadAlerts(); loadStats()
  } catch { emit('message', '预警生成失败', 'error') }
  finally { loading.value = false }
}
</script>

<template>
  <div class="section">
    <!-- 统计卡片 -->
    <div class="stat-cards" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin-bottom:16px;">
      <div class="card stat-card" style="padding:16px;text-align:center;">
        <div class="stat-num red">{{ stats.redCount ?? stats.RED ?? 0 }}</div>
        <div class="stat-label">🔴 红色预警</div>
      </div>
      <div class="card stat-card" style="padding:16px;text-align:center;">
        <div class="stat-num orange">{{ stats.orangeCount ?? stats.ORANGE ?? 0 }}</div>
        <div class="stat-label">🟠 橙色预警</div>
      </div>
      <div class="card stat-card" style="padding:16px;text-align:center;">
        <div class="stat-num yellow">{{ stats.yellowCount ?? stats.YELLOW ?? 0 }}</div>
        <div class="stat-label">🟡 黄色预警</div>
      </div>
      <div class="card stat-card" style="padding:16px;text-align:center;">
        <div class="stat-num green">{{ stats.totalStudents ?? 0 }}</div>
        <div class="stat-label">👨‍🎓 学生总数</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="card" style="padding:12px 16px;margin-bottom:12px;display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
      <button class="btn-ghost" :class="{ active: filterStatus === 'all' }" @click="changeFilter('all')">全部</button>
      <button class="btn-ghost" :class="{ active: filterStatus === 'ACTIVE' }" @click="changeFilter('ACTIVE')">待处理</button>
      <button class="btn-ghost" :class="{ active: filterStatus === 'HANDLED' }" @click="changeFilter('HANDLED')">已处理</button>
      <button class="btn-ghost" :class="{ active: filterStatus === 'ACKNOWLEDGED' }" @click="changeFilter('ACKNOWLEDGED')">已确认</button>
      <button class="btn-ghost" :class="{ active: filterStatus === 'CLOSED' }" @click="changeFilter('CLOSED')">已闭环</button>
      <span style="flex:1;"></span>
      <button class="btn-primary" @click="doGenerateWarnings" :disabled="loading">⚡ 生成预警</button>
      <button v-if="selectedAlertIds.length" class="btn-ghost" @click="doBatchHandle">批量处理({{ selectedAlertIds.length }})</button>
      <button v-if="selectedAlertIds.length" class="btn-ghost" @click="doBatchDismiss">批量撤销</button>
      <button v-if="selectedAlertIds.length" class="btn-ghost" @click="doBatchClose">批量闭环</button>
    </div>

    <!-- 预警列表 -->
    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载中...
    </div>

    <div v-else class="card" style="overflow-x:auto;">
      <table class="data-table" v-if="alerts.length">
        <thead>
          <tr>
            <th style="width:36px;"><input type="checkbox" @change="toggleSelectAll" :checked="selectedAlertIds.length === alerts.length && alerts.length > 0" /></th>
            <th>学生</th>
            <th>等级</th>
            <th>原因</th>
            <th>风险分</th>
            <th>状态</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in alerts" :key="a.id || a.alertId">
            <td><input type="checkbox" :checked="selectedAlertIds.includes(a.id || a.alertId)" @change="toggleSelect(a.id || a.alertId)" /></td>
            <td><strong>{{ a.studentName || a.name }}</strong><br/><small style="color:var(--text-tertiary);">{{ a.studentNo }}</small></td>
            <td><span :class="getLevelClass(a)">{{ getLevelLabel(a) }}</span></td>
            <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="a.alertReason || a.reason">{{ a.alertReason || a.reason || '-' }}</td>
            <td>{{ a.riskScore ?? a.score ?? '-' }}</td>
            <td><span class="status-tag">{{ getStatusLabel(a) }}</span></td>
            <td style="font-size:12px;color:var(--text-tertiary);white-space:nowrap;">{{ (a.createTime || a.createdAt || '').substring(0,16) }}</td>
            <td>
              <div style="display:flex;gap:4px;">
                <button v-if="(a.status || a.alertStatus) === 'ACTIVE'" class="btn-ghost btn-sm" @click="openHandle(a.id || a.alertId)">处理</button>
                <button v-if="(a.status || a.alertStatus) !== 'CLOSED' && (a.status || a.alertStatus) !== 'DISMISSED'" class="btn-ghost btn-sm" @click="openDismiss(a.id || a.alertId)">撤销</button>
                <button class="btn-ghost btn-sm" @click="openRecommends(a.id || a.alertId)">推荐</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else style="text-align:center;padding:40px;color:var(--text-tertiary);">
        暂无预警记录
      </div>
    </div>

    <!-- 处理弹窗 -->
    <div v-if="showHandleModal" class="modal-overlay" @click.self="showHandleModal = false">
      <div class="modal-card" style="max-width:560px;">
        <h3 style="margin-bottom:16px;">处理预警</h3>
        <div style="display:flex;flex-direction:column;gap:12px;">
          <label>干预类型
            <select v-model="handleInterventionType" style="width:100%;margin-top:4px;">
              <option value="COUNSEL">谈心谈话</option>
              <option value="WARNING">书面警示</option>
              <option value="PARENT">联系家长</option>
              <option value="EXERCISE">布置练习</option>
              <option value="OTHER">其他</option>
            </select>
          </label>
          <label>干预描述 <textarea v-model="handleInterventionDesc" rows="2" style="width:100%;margin-top:4px;" placeholder="选填"></textarea></label>

          <!-- 布置练习：选择题目 -->
          <div v-if="handleInterventionType === 'EXERCISE'" style="border:1px solid var(--border-light);border-radius:8px;padding:12px;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:10px;flex-wrap:wrap;">
              <strong style="font-size:14px;">选择练习题目</strong>
              <span style="font-size:12px;color:var(--text-tertiary);">已选 {{ selectedExerciseIds.length }} 题</span>
              <button class="btn-ghost btn-sm" @click="loadHandleExercises" :disabled="handleExerciseLoading" style="margin-left:auto;">{{ handleExerciseLoading ? '加载中...' : '刷新题库' }}</button>
            </div>
            <!-- 搜索和过滤 -->
            <div style="display:flex;gap:6px;margin-bottom:8px;">
              <input v-model="exerciseSearchKeyword" placeholder="搜索题目..." style="flex:1;padding:5px 8px;font-size:13px;" />
              <select v-model="exerciseFilterDifficulty" style="padding:5px 8px;font-size:13px;">
                <option value="all">全部难度</option>
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </div>
            <!-- 题目列表 -->
            <div style="max-height:200px;overflow-y:auto;" v-if="filteredHandleExercises.length">
              <label v-for="ex in filteredHandleExercises" :key="ex.id || ex.exerciseId"
                style="display:flex;align-items:flex-start;gap:8px;padding:6px 4px;cursor:pointer;border-radius:4px;font-size:13px;"
                :style="{ background: selectedExerciseIds.includes(ex.id || ex.exerciseId) ? 'var(--accent-light)' : 'transparent' }">
                <input type="checkbox" :checked="selectedExerciseIds.includes(ex.id || ex.exerciseId)"
                  @change="toggleExerciseSelect(ex.id || ex.exerciseId)" style="margin-top:2px;" />
                <div style="flex:1;">
                  <div>{{ ex.title }}</div>
                  <div style="font-size:11px;color:var(--text-tertiary);margin-top:2px;">
                    {{ getDiffLabel(ex.difficulty) }} | {{ ex.description ? ex.description.substring(0, 40) : '无描述' }}
                  </div>
                </div>
              </label>
            </div>
            <div v-else-if="handleExerciseLoading" style="text-align:center;padding:16px;color:var(--text-tertiary);">加载题库中...</div>
            <div v-else style="text-align:center;padding:16px;color:var(--text-tertiary);">
              题库中没有题目，
              <span style="color:var(--accent-primary);text-decoration:underline;cursor:pointer;"
                @click="emit('switch-tab', 'exercises')">去题库管理上传</span>
            </div>
          </div>

          <label>处理备注 <textarea v-model="handleNote" rows="2" style="width:100%;margin-top:4px;" placeholder="选填"></textarea></label>
          <div style="display:flex;gap:8px;justify-content:flex-end;">
            <button class="btn-ghost" @click="showHandleModal = false">取消</button>
            <button class="btn-primary" @click="doHandle" :disabled="handleSubmitting">{{ handleSubmitting ? '提交中...' : '确认处理' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 撤销弹窗 -->
    <div v-if="showDismissModal" class="modal-overlay" @click.self="showDismissModal = false">
      <div class="modal-card" style="max-width:400px;">
        <h3 style="margin-bottom:16px;">撤销预警</h3>
        <label>撤销原因 <textarea v-model="dismissReason" rows="3" style="width:100%;margin-top:4px;"></textarea></label>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
          <button class="btn-ghost" @click="showDismissModal = false">取消</button>
          <button class="btn-primary" @click="doDismiss" :disabled="!dismissReason.trim()">确认撤销</button>
        </div>
      </div>
    </div>

    <!-- 推荐记录弹窗 -->
    <div v-if="showRecommendModal" class="modal-overlay" @click.self="showRecommendModal = false">
      <div class="modal-card" style="max-width:600px;max-height:70vh;overflow-y:auto;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3>推荐练习记录</h3>
          <button class="btn-ghost" @click="showRecommendModal = false">✕</button>
        </div>
        <div v-if="recommendLoading" style="text-align:center;padding:30px;"><span class="spinner"></span></div>
        <div v-else-if="recommends.length">
          <div v-for="r in recommends" :key="r.id" class="card" style="padding:12px;margin-bottom:8px;">
            <div style="font-weight:600;">{{ r.exerciseTitle || r.title }}</div>
            <div style="font-size:13px;color:var(--text-tertiary);margin-top:4px;">
              状态: {{ r.status || '-' }} | 得分: {{ r.score ?? '-' }}
            </div>
          </div>
        </div>
        <div v-else style="text-align:center;padding:30px;color:var(--text-tertiary);">暂无推荐记录</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.data-table { width:100%; border-collapse:collapse; font-size:14px; }
.data-table th { text-align:left; padding:10px 12px; border-bottom:2px solid var(--border-light); color:var(--text-secondary); font-weight:600; white-space:nowrap; }
.data-table td { padding:10px 12px; border-bottom:1px solid var(--border-light); }
.data-table tbody tr:hover { background:var(--bg-hover); }

.level-tag { display:inline-block; padding:2px 10px; border-radius:12px; font-size:12px; font-weight:600; }
.level-tag.red { background:var(--semantic-red-bg); color:var(--semantic-red-text); }
.level-tag.orange { background:var(--semantic-orange-bg); color:var(--semantic-orange-text); }
.level-tag.yellow { background:var(--semantic-yellow-bg); color:var(--semantic-yellow-text); }
.level-tag.green { background:var(--semantic-green-bg); color:var(--semantic-green-text); }

.status-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; background:var(--tag-bg-default); color:var(--tag-text-default); }

.stat-num { font-size:2rem; font-weight:700; }
.stat-num.red { color:var(--semantic-red); }
.stat-num.orange { color:var(--semantic-orange); }
.stat-num.yellow { color:var(--semantic-yellow); }
.stat-num.green { color:var(--semantic-green); }
.stat-label { font-size:12px; color:var(--text-tertiary); margin-top:4px; }

.btn-sm { font-size:12px; padding:3px 8px; }

.btn-ghost.active { background:var(--accent-light); color:var(--accent-primary); }
</style>
