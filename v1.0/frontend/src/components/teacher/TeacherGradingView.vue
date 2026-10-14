<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import {
  getPendingGrading, gradeFeedback, gradeFeedbackSub,
  getSubQuestions, getSubAnswers
} from '../../api/http.js'

const { teacherToken } = useAuth()
const props = defineProps({
  courseId: { type: Number, required: true }
})
const emit = defineEmits(['message'])

const records = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = 20

// 评分弹窗
const showGradeModal = ref(false)
const gradingRec = ref(null)
const gradeScore = ref(0)
const gradeFeedbackText = ref('')
const gradeSubScores = ref({})
const subQuestions = ref([])
const subAnswers = ref([])
const gradeSubmitting = ref(false)

onMounted(() => { loadRecords() })

async function loadRecords(p = 1) {
  loading.value = true
  page.value = p
  try {
    const res = await getPendingGrading(teacherToken.value, props.courseId, p, pageSize)
    if (res.success) {
      const data = res.data || {}
      records.value = data.records || []
      total.value = data.total || 0
    } else {
      emit('message', res.message || '获取待批改列表失败', 'error')
    }
  } catch { emit('message', '获取待批改列表失败', 'error') }
  finally { loading.value = false }
}

function getPageCount() { return Math.ceil(total.value / pageSize) }

function setMsg(text, type = 'info') { emit('message', text, type) }

// ==================== 评分逻辑 ====================

async function openGrade(rec) {
  gradingRec.value = rec
  gradeScore.value = 0
  gradeFeedbackText.value = ''
  gradeSubScores.value = {}
  subQuestions.value = []
  subAnswers.value = []
  showGradeModal.value = true

  // 并行加载子题和作答
  try {
    const [subQRes, subARes] = await Promise.all([
      getSubQuestions(teacherToken.value, rec.exerciseId),
      getSubAnswers(teacherToken.value, rec.id)
    ])
    if (subQRes.success) subQuestions.value = subQRes.data || []
    if (subARes.success) subAnswers.value = subARes.data || []
  } catch { /* 无子题也正常 */ }
}

function closeGrade() {
  showGradeModal.value = false
  gradingRec.value = null
}

async function doGrade() {
  gradeSubmitting.value = true
  try {
    let res

    // 检查是否有子题需要分题评分
    const hasSub = subQuestions.value.length > 0
    if (hasSub) {
      res = await gradeFeedbackSub(teacherToken.value, gradingRec.value.id,
        gradeSubScores.value, gradeFeedbackText.value)
    } else {
      res = await gradeFeedback(teacherToken.value, gradingRec.value.id,
        gradeScore.value, gradeFeedbackText.value)
    }

    if (res.success) {
      setMsg('评分完成', 'success')
      closeGrade()
      loadRecords(page.value)
    } else {
      setMsg(res.message || '评分失败', 'error')
    }
  } catch { setMsg('评分失败', 'error') }
  finally { gradeSubmitting.value = false }
}

// ==================== 格式化 ====================

function formatTime(t) {
  if (!t) return '-'
  return t.substring(0, 16)
}

function getAnswerText(rec) {
  const answer = rec.studentAnswer
  if (!answer || answer === 'null' || answer === 'undefined') return '(无文本作答)'
  // 如果答案是 JSON 格式的子题作答
  try {
    const parsed = JSON.parse(answer)
    if (typeof parsed === 'object') {
      return Object.entries(parsed)
        .map(([_, v]) => v)
        .filter(Boolean)
        .join(' | ') || '(无文本作答)'
    }
  } catch { /* 不是 JSON，直接返回字符串 */ }
  return String(answer)
}

// ==================== 分页 ====================

function prevPage() { if (page.value > 1) loadRecords(page.value - 1) }
function nextPage() { if (page.value < getPageCount()) loadRecords(page.value + 1) }

// ==================== 子题答案查找 ====================

function getSubAnswer(subQuestionId) {
  const a = subAnswers.value.find(sa => sa.subQuestionId === subQuestionId)
  return a ? (a.answer || '(未作答)') : '(未作答)'
}
</script>

<template>
  <div class="section">
    <!-- 标题栏 -->
    <div class="card" style="padding:12px 16px;margin-bottom:16px;display:flex;align-items:center;gap:12px;">
      <h3 style="margin:0;">📝 待批改练习</h3>
      <span style="color:var(--text-tertiary);font-size:14px;">共 {{ total }} 份待批改</span>
      <span style="flex:1;"></span>
      <button class="btn-ghost" @click="loadRecords(1)">🔄 刷新</button>
    </div>

    <!-- 列表 -->
    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载中...
    </div>

    <div v-else-if="records.length" style="display:flex;flex-direction:column;gap:10px;">
      <div v-for="rec in records" :key="rec.id"
           class="card grading-card" style="padding:16px;">
        <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:12px;">
          <div style="flex:1;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
              <span class="grading-student">👨‍🎓 {{ rec.studentName || '学生' }}</span>
              <span style="font-size:12px;color:var(--text-tertiary);">{{ rec.studentNo }}</span>
              <span class="grading-difficulty" v-if="rec.difficulty"
                    :class="(rec.difficulty || 'MEDIUM').toLowerCase()">
                {{ {EASY:'简单',MEDIUM:'中等',HARD:'困难'}[rec.difficulty] || rec.difficulty }}
              </span>
            </div>

            <div class="grading-title">📄 {{ rec.exerciseTitle || '练习题目' }}</div>

            <div v-if="rec.studentAnswer" class="grading-answer">
              <span style="font-weight:600;color:var(--text-secondary);">学生作答：</span>
              {{ getAnswerText(rec) }}
            </div>

            <div style="font-size:12px;color:var(--text-muted);margin-top:8px;">
              提交时间：{{ formatTime(rec.completeTime) }}
            </div>
          </div>

          <button class="btn-primary" @click="openGrade(rec)">✏️ 评分</button>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="getPageCount() > 1" style="display:flex;justify-content:center;align-items:center;gap:8px;margin-top:8px;">
        <button class="btn-ghost btn-sm" :disabled="page <= 1" @click="prevPage">上一页</button>
        <span style="font-size:13px;color:var(--text-tertiary);">{{ page }} / {{ getPageCount() }}</span>
        <button class="btn-ghost btn-sm" :disabled="page >= getPageCount()" @click="nextPage">下一页</button>
      </div>
    </div>

    <div v-else class="card" style="text-align:center;padding:60px;color:var(--text-tertiary);">
      <div style="font-size:48px;margin-bottom:12px;">✅</div>
      <div style="font-size:16px;">没有待批改的练习</div>
      <div style="font-size:13px;margin-top:6px;">学生提交后会自动出现在这里</div>
    </div>

    <!-- 评分弹窗 -->
    <div v-if="showGradeModal && gradingRec" class="modal-overlay" @click.self="closeGrade">
      <div class="modal-card" style="max-width:650px;max-height:85vh;overflow-y:auto;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3>批改练习</h3>
          <button class="btn-ghost" @click="closeGrade">✕</button>
        </div>

        <!-- 题目信息 -->
        <div class="card" style="padding:12px;margin-bottom:12px;background:var(--bg-card);">
          <div style="font-weight:600;margin-bottom:4px;">{{ gradingRec.exerciseTitle }}</div>
          <div v-if="gradingRec.exerciseDescription"
               style="font-size:13px;color:var(--text-secondary);line-height:1.5;">
            {{ gradingRec.exerciseDescription }}
          </div>
          <div style="font-size:12px;color:var(--text-tertiary);margin-top:4px;">
            学生：{{ gradingRec.studentName }} ({{ gradingRec.studentNo }})
          </div>
        </div>

        <!-- 学生作答 -->
        <div class="card" style="padding:12px;margin-bottom:16px;background:var(--bg-hover);">
          <div style="font-weight:600;font-size:13px;color:var(--text-secondary);margin-bottom:8px;">学生作答内容</div>

          <!-- 有子题 -->
          <div v-if="subQuestions.length" style="display:flex;flex-direction:column;gap:10px;">
            <div v-for="sq in subQuestions" :key="sq.id" style="border-bottom:1px solid var(--border-light);padding-bottom:8px;">
              <div style="font-size:13px;font-weight:600;">
                第{{ sq.seq }}题 <span v-if="sq.score" style="font-weight:400;color:var(--text-tertiary);">(满分{{ sq.score }}分)</span>
              </div>
              <div style="font-size:12px;color:var(--text-secondary);margin-top:2px;">{{ sq.title }}</div>
              <div style="font-size:13px;margin-top:4px;padding:6px 8px;background:var(--bg-card);border-radius:4px;color:var(--accent-primary);">
                {{ getSubAnswer(sq.id) }}
              </div>
              <!-- 分题打分 -->
              <div style="margin-top:6px;display:flex;align-items:center;gap:8px;">
                <span style="font-size:12px;color:var(--text-secondary);">得分：</span>
                <input type="number" v-model.number="gradeSubScores[sq.id]"
                       style="width:80px;padding:4px 8px;text-align:center;"
                       :max="sq.score || 100" min="0" placeholder="0" />
                <span v-if="sq.score" style="font-size:12px;color:var(--text-tertiary);">/ {{ sq.score }}</span>
              </div>
            </div>
          </div>

          <!-- 无子题：直接显示作答 -->
          <div v-else>
            <div style="font-size:14px;padding:8px;background:var(--bg-card);border-radius:4px;color:var(--accent-primary);white-space:pre-wrap;">
              {{ gradingRec.studentAnswer || '(未作答)' }}
            </div>

            <div style="margin-top:12px;display:flex;align-items:center;gap:12px;">
              <label style="font-size:13px;">
                <span style="color:var(--text-secondary);">得分（0-100）：</span>
                <input type="number" v-model.number="gradeScore"
                       style="width:80px;padding:4px 8px;text-align:center;margin-left:8px;"
                       min="0" max="100" placeholder="0" />
              </label>
            </div>
          </div>
        </div>

        <!-- 反馈 -->
        <label style="display:block;margin-bottom:16px;">
          <span style="font-weight:600;font-size:14px;">教师反馈</span>
          <textarea v-model="gradeFeedbackText" rows="3"
                    style="width:100%;margin-top:6px;"
                    placeholder="可填写评语或改进建议..."></textarea>
        </label>

        <!-- 操作按钮 -->
        <div style="display:flex;gap:8px;justify-content:flex-end;">
          <button class="btn-ghost" @click="closeGrade">取消</button>
          <button class="btn-primary" @click="doGrade" :disabled="gradeSubmitting">
            {{ gradeSubmitting ? '提交中...' : '确认评分' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grading-card {
  border-left: 4px solid var(--accent-primary);
  transition: box-shadow 0.2s;
}
.grading-card:hover { box-shadow: var(--shadow-md); }

.grading-student {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.grading-difficulty {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}
.grading-difficulty.easy { background: var(--diff-easy-bg); color: var(--diff-easy-text); }
.grading-difficulty.medium { background: var(--diff-medium-bg); color: var(--diff-medium-text); }
.grading-difficulty.hard { background: var(--diff-hard-bg); color: var(--diff-hard-text); }

.grading-title {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.grading-answer {
  font-size: 13px;
  color: var(--text-secondary);
  padding: 8px 12px;
  background: var(--bg-hover);
  border-radius: 6px;
  line-height: 1.6;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.btn-sm { font-size: 12px; padding: 3px 8px; }
</style>
