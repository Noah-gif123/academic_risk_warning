<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { getDashboard, getTopRisk } from '../../api/http.js'

const { teacherToken } = useAuth()
const props = defineProps({ courseId: { type: Number, required: true } })

const loading = ref(false)
const dashboard = ref(null)
const topRisks = ref([])

onMounted(() => {
  loadDashboard()
  loadTopRisks()
})

async function loadDashboard() {
  loading.value = true
  try {
    const res = await getDashboard(teacherToken.value, props.courseId)
    if (res.success) dashboard.value = res.data || {}
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function loadTopRisks() {
  try {
    const res = await getTopRisk(teacherToken.value, 10, props.courseId)
    if (res.success) topRisks.value = res.data || []
  } catch { /* ignore */ }
}

function getLevelClass(lv) {
  const m = { RED: 'level-red', ORANGE: 'level-orange', YELLOW: 'level-yellow', GREEN: 'level-green' }
  return m[lv || 'GREEN'] || 'level-green'
}
</script>

<template>
  <div class="section">
    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载仪表盘...
    </div>

    <template v-else>
      <div v-if="!dashboard" class="card" style="text-align:center;padding:40px;color:var(--text-tertiary);">
        暂无课程数据，请确认已选择课程
      </div>
      <!-- 概览卡片 -->
      <div v-if="dashboard" class="stats-grid" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin-bottom:16px;">
        <div class="card" style="padding:20px;text-align:center;">
          <div class="stat-value" style="font-size:2rem;font-weight:700;color:var(--semantic-blue);">{{ dashboard.studentsAtRisk ?? 0 }}</div>
          <div class="stat-label" style="font-size:13px;color:var(--text-tertiary);">风险学生数</div>
        </div>
        <div class="card" style="padding:20px;text-align:center;">
          <div class="stat-value" style="font-size:2rem;font-weight:700;color:var(--semantic-red);">{{ dashboard.total ?? 0 }}</div>
          <div class="stat-label" style="font-size:13px;color:var(--text-tertiary);">预警总数</div>
        </div>
        <div class="card" style="padding:20px;text-align:center;">
          <div class="stat-value" style="font-size:2rem;font-weight:700;color:var(--semantic-green);">{{ dashboard.closedCount ?? 0 }}</div>
          <div class="stat-label" style="font-size:13px;color:var(--text-tertiary);">已闭环</div>
        </div>
        <div class="card" style="padding:20px;text-align:center;">
          <div class="stat-value" style="font-size:2rem;font-weight:700;color:var(--semantic-orange);">{{ dashboard.activeCount ?? 0 }}</div>
          <div class="stat-label" style="font-size:13px;color:var(--text-tertiary);">待处理</div>
        </div>
      </div>

      <!-- 预警分布条 -->
      <div class="card" style="padding:16px;margin-bottom:16px;" v-if="dashboard">
        <h4 style="margin-bottom:12px;">预警分布</h4>
        <div class="bar-chart" style="display:flex;height:28px;border-radius:8px;overflow:hidden;gap:3px;">
          <div v-for="lv in ['RED','ORANGE','YELLOW','GREEN']" :key="lv"
            :class="'bar-' + lv.toLowerCase()"
            :style="{ flex: Math.max(dashboard.levelDistribution?.[lv] || 0, 0.1) }"
            style="display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:600;min-width:0;">
            {{ dashboard.levelDistribution?.[lv] || 0 }}
          </div>
        </div>
        <div style="display:flex;justify-content:space-between;margin-top:6px;font-size:12px;color:var(--text-tertiary);">
          <span>🔴 红色 {{ dashboard.redCount || 0 }}</span>
          <span>🟠 橙色 {{ dashboard.orangeCount || 0 }}</span>
          <span>🟡 黄色 {{ dashboard.yellowCount || 0 }}</span>
          <span>🟢 正常</span>
        </div>
      </div>

      <!-- Top风险学生 -->
      <div class="card" style="padding:16px;" v-if="topRisks.length">
        <h4 style="margin-bottom:12px;">TOP 风险学生</h4>
        <div style="overflow-x:auto;">
          <table class="mini-table">
            <thead>
              <tr><th>#</th><th>姓名</th><th>学号</th><th>等级</th><th>风险分</th><th>原因</th></tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in topRisks" :key="s.studentId || s.id">
                <td>{{ i + 1 }}</td>
                <td><strong>{{ s.studentName || s.name }}</strong></td>
                <td>{{ s.studentNo || '-' }}</td>
                <td><span :class="getLevelClass(s.alertLevel || s.riskLevel)">{{ s.alertLevel || s.riskLevel || 'GREEN' }}</span></td>
                <td>{{ s.riskScore ?? '-' }}</td>
                <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="s.alertReason">{{ s.alertReason || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.bar-red { background:var(--semantic-red); color:#fff; }
.bar-orange { background:var(--semantic-orange); color:#fff; }
.bar-yellow { background:var(--semantic-yellow); color:#333; }
.bar-green { background:var(--semantic-green); color:#fff; }

.level-red { color:var(--semantic-red); font-weight:600; }
.level-orange { color:var(--semantic-orange); font-weight:600; }
.level-yellow { color:var(--semantic-yellow); font-weight:600; }
.level-green { color:var(--semantic-green); font-weight:600; }

.mini-table { width:100%; border-collapse:collapse; font-size:13px; }
.mini-table th { text-align:left; padding:8px 10px; border-bottom:2px solid var(--border-light); color:var(--text-secondary); white-space:nowrap; }
.mini-table td { padding:8px 10px; border-bottom:1px solid var(--border-light); }
.mini-table tbody tr:hover { background:var(--bg-hover); }
</style>
