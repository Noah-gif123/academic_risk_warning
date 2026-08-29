<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { getExerciseList, addExercise, deleteExercise, getKnowledgeTree, addExerciseWithFile,
         addSubQuestions, getSubQuestions } from '../../api/http.js'

const { teacherToken } = useAuth()
const props = defineProps({
  courseId: { type: Number, required: true },
  courseName: { type: String, default: '' }
})

const exercises = ref([])
const loading = ref(false)
const message = ref('')
const messageType = ref('info')
const searchKeyword = ref('')
const filterDifficulty = ref('all')
const filterKpId = ref(null)
const page = ref(1)
const total = ref(0)

// 添加/编辑弹窗
const showEditModal = ref(false)
const editingId = ref(null)
const editTitle = ref('')
const editDescription = ref('')
const editDifficulty = ref('MEDIUM')
const editQuestionType = ref('SUBJECTIVE')
const editAnswer = ref('')
const editKpIds = ref([])
const editFile = ref(null)
const editFileName = ref('')
const editSubmitting = ref(false)

// 知识点树（用于选择关联知识点）
const knowledgeTree = ref([])

// ===== 子题切分 =====
const showSplitModal = ref(false)
const splitExercise = ref(null)          // 当前切分的习题
const splitItems = ref([])               // 子题列表 [{title, score}]
const splitSaving = ref(false)

function openSplit(ex) {
  splitExercise.value = ex
  splitItems.value = []
  showSplitModal.value = true
  getSubQuestions(teacherToken.value, ex.id || ex.exerciseId).then(res => {
    if (res.success && res.data?.length) {
      splitItems.value = res.data.map(sq => ({
        id: sq.id,
        title: sq.title || '',
        score: sq.score || null,
        questionType: sq.questionType || 'SINGLE',
        kpId: sq.kpId || null,
        kpSelections: (sq.kpWeights || []).map(kw => ({
          kpId: kw.kpId,
          weight: kw.weight || 0
        }))
      }))
    } else {
      splitItems.value = [
        { title: '题目1', score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] },
        { title: '题目2', score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] },
        { title: '题目3', score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] }
      ]
    }
  }).catch(() => {
    splitItems.value = [
      { title: '题目1', score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] },
      { title: '题目2', score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] },
      { title: '题目3', score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] }
    ]
  })
}

function closeSplit() {
  showSplitModal.value = false
  splitExercise.value = null
  splitItems.value = []
}

function addSplitRow() {
  const nextNum = splitItems.value.length + 1
  splitItems.value.push({ title: '题目' + nextNum, score: null, questionType: 'SINGLE', kpId: null, kpSelections: [] })
}

function removeSplitRow(index) {
  splitItems.value.splice(index, 1)
}

function toggleKpForSub(item, kpId) {
  const idx = item.kpSelections.findIndex(ks => ks.kpId === kpId)
  if (idx >= 0) {
    item.kpSelections.splice(idx, 1)
  } else {
    item.kpSelections.push({ kpId, weight: 0 })
  }
}

function updateKpWeight(item, kpId, weight) {
  const found = item.kpSelections.find(ks => ks.kpId === kpId)
  if (found) found.weight = weight
}

async function doSaveSplit() {
  const validItems = splitItems.value.filter(item => item.title.trim())
  if (!validItems.length) { setMsg('请至少输入一个子题', 'error'); return }
  splitSaving.value = true
  try {
    const payload = validItems.map((item, i) => {
      const base = {
        seq: i + 1,
        title: item.title.trim(),
        score: item.score || null,
        questionType: item.questionType || 'SINGLE'
      }
      if (item.questionType === 'COMPREHENSIVE') {
        base.kpWeights = (item.kpSelections || []).filter(ks => ks.kpId).map(ks => ({
          kpId: ks.kpId,
          weight: ks.weight || 0
        }))
      } else {
        base.kpId = item.kpId || null
      }
      return base
    })
    const res = await addSubQuestions(teacherToken.value,
      splitExercise.value.id || splitExercise.value.exerciseId, payload)
    if (res.success) {
      setMsg('切分成功', 'success')
      closeSplit()
    } else {
      setMsg(res.message || '保存失败', 'error')
    }
  } catch {
    setMsg('网络异常', 'error')
  } finally { splitSaving.value = false }
}

onMounted(() => {
  loadExercises()
  loadKnowledge()
})

async function loadExercises() {
  loading.value = true
  try {
    const res = await getExerciseList(teacherToken.value, props.courseId)
    if (res.success) {
      const data = res.data || {}
      exercises.value = data.records || data || []
      total.value = data.total || (Array.isArray(exercises.value) ? exercises.value.length : 0)
    } else setMsg(res.message || '获取失败', 'error')
  } catch { setMsg('获取题库失败', 'error') }
  finally { loading.value = false }
}

async function loadKnowledge() {
  try {
    const res = await getKnowledgeTree(teacherToken.value, props.courseId)
    if (res.success) knowledgeTree.value = res.data || []
  } catch { /* ignore */ }
}

function openAdd() {
  editingId.value = null
  editTitle.value = ''
  editDescription.value = ''
  editDifficulty.value = 'MEDIUM'
  editQuestionType.value = 'SUBJECTIVE'
  editAnswer.value = ''
  editKpIds.value = []
  editFile.value = null
  editFileName.value = ''
  showEditModal.value = true
}

function toggleKp(kpId) {
  const idx = editKpIds.value.indexOf(kpId)
  if (idx >= 0) editKpIds.value.splice(idx, 1)
  else editKpIds.value.push(kpId)
}

function handleFileUpload(e) {
  const f = e.target.files?.[0]
  if (f) { editFile.value = f; editFileName.value = f.name }
}

async function doSave() {
  if (!editTitle.value.trim()) { setMsg('请输入题目标题', 'error'); return }
  if (!editKpIds.value.length) { setMsg('请选择至少一个知识点', 'error'); return }
  editSubmitting.value = true
  try {
    let res
    if (editFile.value) {
      res = await addExerciseWithFile(teacherToken.value, props.courseId, editKpIds.value,
        editTitle.value.trim(), editDescription.value, editDifficulty.value,
        editQuestionType.value, editAnswer.value, editFile.value)
    } else {
      res = await addExercise(teacherToken.value, props.courseId, editKpIds.value,
        editTitle.value.trim(), editDescription.value, editDifficulty.value,
        editQuestionType.value, editAnswer.value)
    }
    if (res.success) {
      setMsg('添加成功', 'success')
      showEditModal.value = false
      loadExercises()
    } else setMsg(res.message || '添加失败', 'error')
  } catch { setMsg('添加失败', 'error') }
  finally { editSubmitting.value = false }
}

async function doDelete(id, title) {
  if (!confirm(`确定删除练习题 "${title}" 吗？`)) return
  try {
    const res = await deleteExercise(teacherToken.value, id)
    if (res.success) { setMsg('已删除', 'success'); loadExercises() }
    else setMsg(res.message || '删除失败', 'error')
  } catch { setMsg('删除失败', 'error') }
}

function setMsg(text, type = 'info') { message.value = text; messageType.value = type }

function getDiffLabel(d) {
  const m = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return m[d] || d
}

function getAuditLabel(s) {
  const m = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return m[s] || s || '未知'
}

function getFlatKnowledge(kps, prefix = '') {
  let result = []
  for (const kp of kps) {
    const key = kp.id || kp.knowledgePointId
    const name = prefix + (kp.name || kp.knowledgePointName)
    result.push({ id: key, name })
    if (kp.children) result = result.concat(getFlatKnowledge(kp.children, name + ' > '))
  }
  return result
}

const filteredExercises = computed(() => {
  let list = exercises.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(e => (e.title || '').toLowerCase().includes(kw))
  }
  if (filterDifficulty.value !== 'all') {
    list = list.filter(e => e.difficulty === filterDifficulty.value)
  }
  return list
})

// 知识点扁平列表（供子题切分下拉/勾选用）
const flatKnowledgeList = computed(() => getFlatKnowledge(knowledgeTree.value))

import { computed } from 'vue'
</script>

<template>
  <div class="section">
    <!-- 工具栏 -->
    <div class="card" style="padding:12px 16px;margin-bottom:16px;display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
      <h3 style="margin:0;">题库管理 - {{ courseName }}</h3>
      <span style="flex:1;"></span>
      <input v-model="searchKeyword" placeholder="搜索题目..." style="width:180px;padding:6px 10px;" />
      <select v-model="filterDifficulty" style="padding:6px 10px;">
        <option value="all">全部难度</option>
        <option value="EASY">简单</option>
        <option value="MEDIUM">中等</option>
        <option value="HARD">困难</option>
      </select>
      <button class="btn-primary" @click="openAdd">+ 添加题目</button>
      <button class="btn-ghost" @click="loadExercises">🔄 刷新</button>
    </div>

    <p v-if="message" class="msg" :class="messageType" style="margin-bottom:12px;">{{ message }}</p>

    <!-- 题目列表 -->
    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载中...
    </div>

    <div v-else-if="filteredExercises.length">
      <div v-for="ex in filteredExercises" :key="ex.id || ex.exerciseId" class="card exercise-card" style="padding:16px;margin-bottom:10px;">
        <div style="display:flex;align-items:flex-start;gap:8px;">
          <div style="flex:1;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
              <strong>{{ ex.title }}</strong>
              <span class="diff-tag" :class="(ex.difficulty || 'MEDIUM').toLowerCase()">{{ getDiffLabel(ex.difficulty || 'MEDIUM') }}</span>
              <span v-if="ex.questionType" class="type-tag">{{ ex.questionType === 'OBJECTIVE' ? '客观题' : '主观题' }}</span>
              <span class="audit-tag" :class="'audit-' + (ex.auditStatus || 'APPROVED').toLowerCase()">{{ getAuditLabel(ex.auditStatus) }}</span>
            </div>
            <div v-if="ex.description" style="font-size:13px;color:var(--text-secondary);margin-bottom:6px;line-height:1.5;">{{ ex.description }}</div>
            <div style="display:flex;flex-wrap:wrap;gap:4px;">
              <span v-for="kp in (ex.knowledgePointNames || ex.kps || [])" :key="kp" class="kp-tag">{{ kp }}</span>
            </div>
            <div v-if="ex.fileUrl" style="margin-top:6px;font-size:12px;">
              📎 <a :href="ex.fileUrl" target="_blank">{{ ex.fileUrl.split('/').pop() || '附件' }}</a>
            </div>
            <div style="font-size:11px;color:var(--text-muted);margin-top:6px;">
              创建：{{ (ex.createTime || ex.createdAt || '').substring(0,16) }}
            </div>
          </div>
          <div style="display:flex;flex-direction:column;gap:4px;">
            <button class="btn-ghost btn-sm" @click="openSplit(ex)" title="将本题切分为多个子题">✂ 切分</button>
            <button class="btn-ghost btn-sm btn-danger" @click="doDelete(ex.id || ex.exerciseId, ex.title)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card" style="text-align:center;padding:40px;color:var(--text-tertiary);">
      {{ exercises.length ? '无匹配题目' : '暂无练习题，请添加' }}
    </div>

    <!-- 添加弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-card" style="max-width:600px;max-height:85vh;overflow-y:auto;">
        <h3 style="margin-bottom:16px;">添加练习题</h3>
        <div style="display:flex;flex-direction:column;gap:12px;">
          <label>题目 <input v-model="editTitle" style="width:100%;margin-top:4px;" placeholder="题目内容" /></label>
          <label>描述 <textarea v-model="editDescription" rows="3" style="width:100%;margin-top:4px;" placeholder="可选描述"></textarea></label>
          <div style="display:flex;gap:12px;">
            <label style="flex:1;">难度
              <select v-model="editDifficulty" style="width:100%;margin-top:4px;">
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </label>
            <label style="flex:1;">题目类型
              <select v-model="editQuestionType" style="width:100%;margin-top:4px;">
                <option value="SUBJECTIVE">主观题</option>
                <option value="OBJECTIVE">客观题</option>
              </select>
            </label>
          </div>
          <label>标准答案 <textarea v-model="editAnswer" rows="2" style="width:100%;margin-top:4px;" placeholder="可选，用于客观题自动判断"></textarea></label>
          <label>附件 <input type="file" @change="handleFileUpload" style="margin-top:4px;" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.gif" /></label>
          <p v-if="editFileName" style="font-size:12px;color:var(--text-tertiary);">已选择: {{ editFileName }}</p>

          <!-- 知识点选择 -->
          <div>
            <label style="font-weight:600;">关联知识点 (已选: {{ editKpIds.length }})</label>
            <div style="margin-top:8px;max-height:200px;overflow-y:auto;border:1px solid var(--border-light);border-radius:8px;padding:8px;">
              <div v-for="kp in getFlatKnowledge(knowledgeTree)" :key="kp.id"
                @click="toggleKp(kp.id)"
                class="kp-select-item"
                :class="{ selected: editKpIds.includes(kp.id) }"
                style="padding:6px 8px;cursor:pointer;border-radius:4px;display:flex;align-items:center;gap:6px;font-size:13px;">
                <input type="checkbox" :checked="editKpIds.includes(kp.id)" style="pointer-events:none;" />
                {{ kp.name }}
              </div>
              <div v-if="!getFlatKnowledge(knowledgeTree).length" style="color:var(--text-tertiary);font-size:13px;text-align:center;padding:12px;">
                暂无知识点，请先在知识点管理中创建
              </div>
            </div>
          </div>

          <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:8px;">
            <button class="btn-ghost" @click="showEditModal = false">取消</button>
            <button class="btn-primary" @click="doSave" :disabled="editSubmitting">{{ editSubmitting ? '保存中...' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>
    <!-- ===== 子题切分弹窗 ===== -->
    <div v-if="showSplitModal" class="modal-overlay" @click.self="closeSplit">
      <div class="modal-card split-modal">
        <h3 style="margin-bottom:8px;">✂ 子题切分</h3>
        <p style="font-size:13px;color:var(--text-secondary);margin-bottom:16px;">
          题目：<strong>{{ splitExercise?.title }}</strong>
          <a v-if="splitExercise?.fileUrl" :href="splitExercise.fileUrl" target="_blank" style="margin-left:8px;font-size:12px;">📎 查看附件</a>
        </p>

        <div style="display:flex;flex-direction:column;gap:12px;">
          <div v-for="(item, i) in splitItems" :key="i"
               class="split-card">
            <!-- 题型切换 + 删除 -->
            <div class="split-card-header">
              <span class="split-card-num">第 {{ i + 1 }} 题</span>
              <div class="split-type-toggle">
                <button class="split-type-btn" :class="{ active: item.questionType === 'SINGLE' }"
                        @click="item.questionType = 'SINGLE'; item.kpId = null; item.kpSelections = []">单知识点</button>
                <button class="split-type-btn" :class="{ active: item.questionType === 'COMPREHENSIVE' }"
                        @click="item.questionType = 'COMPREHENSIVE'; item.kpId = null">综合题</button>
              </div>
              <button class="btn-ghost btn-xs btn-danger" @click="removeSplitRow(i)"
                      :disabled="splitItems.length <= 1">✕</button>
            </div>

            <!-- 题目内容 -->
            <input v-model="item.title" placeholder="题目内容" class="split-title-input" />

            <!-- 分值 -->
            <div class="split-score-row">
              <span class="split-label">分值</span>
              <input v-model.number="item.score" type="number" min="0" max="100"
                     placeholder="可选，默认10分" class="split-score-input" />
            </div>

            <!-- 关联知识点 -->
            <div class="split-kp-section">
              <!-- 单知识点：下拉选择 -->
              <div v-if="item.questionType === 'SINGLE'" class="split-kp-row">
                <span class="split-label">知识点</span>
                <select v-model="item.kpId" class="split-kp-select">
                  <option :value="null">不关联知识点</option>
                  <option v-for="kp in flatKnowledgeList" :key="kp.id" :value="kp.id">
                    {{ kp.name }}
                  </option>
                </select>
              </div>

              <!-- 综合题：多选 + 权重 -->
              <div v-else class="split-comp-kps">
                <span class="split-label">关联知识点（权重合计应≤1）</span>
                <div class="split-kp-check-list">
                  <label v-for="kp in flatKnowledgeList" :key="kp.id"
                         class="split-kp-check-item"
                         :class="{ selected: item.kpSelections.some(ks => ks.kpId === kp.id) }">
                    <input type="checkbox"
                           :checked="item.kpSelections.some(ks => ks.kpId === kp.id)"
                           @change="toggleKpForSub(item, kp.id)" />
                    <span class="split-kp-check-name">{{ kp.name }}</span>
                    <input v-if="item.kpSelections.some(ks => ks.kpId === kp.id)"
                           type="number" min="0" max="1" step="0.1"
                           :value="(item.kpSelections.find(ks => ks.kpId === kp.id) || {}).weight || 0"
                           @input="updateKpWeight(item, kp.id, parseFloat($event.target.value) || 0)"
                           class="split-weight-input" placeholder="权重" />
                  </label>
                  <div v-if="!flatKnowledgeList.length" class="split-kp-empty">
                    暂无知识点，请先在知识点管理中创建
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div style="margin-top:14px;display:flex;gap:8px;justify-content:space-between;">
          <button class="btn-ghost" @click="addSplitRow">+ 添加子题</button>
          <div style="display:flex;gap:8px;">
            <button class="btn-ghost" @click="closeSplit">取消</button>
            <button class="btn-primary" @click="doSaveSplit" :disabled="splitSaving">
              {{ splitSaving ? '保存中...' : '保存切分' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.exercise-card { border-left:3px solid var(--accent-primary); }
.exercise-card:hover { box-shadow:var(--shadow-md); }

.diff-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; font-weight:600; }
.diff-tag.easy { background:var(--diff-easy-bg); color:var(--diff-easy-text); }
.diff-tag.medium { background:var(--diff-medium-bg); color:var(--diff-medium-text); }
.diff-tag.hard { background:var(--diff-hard-bg); color:var(--diff-hard-text); }

.type-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; background:var(--accent-light); color:var(--accent-primary); }

.kp-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; background:var(--kp-tag-bg); color:var(--kp-tag-text); }

.kp-select-item:hover { background:var(--bg-hover); }
.kp-select-item.selected { background:var(--accent-light); color:var(--accent-primary); }

.audit-tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; font-weight:600; }
.audit-tag.audit-pending { background:#fff3cd; color:#856404; }
.audit-tag.audit-approved { background:#d4edda; color:#155724; }
.audit-tag.audit-rejected { background:#f8d7da; color:#721c24; }

.btn-sm { font-size:12px; padding:3px 8px; }
.btn-xs { font-size:11px; padding:2px 6px; }
.btn-danger { color:var(--semantic-red); }
.btn-danger:hover { background:var(--semantic-red-bg); }

/* 切分弹窗 */
.split-modal { max-width:720px; max-height:85vh; overflow-y:auto; }
.split-card { border:1px solid var(--border-light); border-radius:10px; padding:12px; background:var(--bg-card-subtle); display:flex; flex-direction:column; gap:8px; }
.split-card-header { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.split-card-num { font-size:13px; font-weight:600; color:var(--text-secondary); white-space:nowrap; }
.split-type-toggle { display:flex; gap:0; border:1px solid var(--border-light); border-radius:6px; overflow:hidden; }
.split-type-btn { padding:4px 10px; font-size:12px; border:none; background:#fff; cursor:pointer; color:var(--text-secondary); }
.split-type-btn.active { background:var(--accent-primary); color:#fff; }
.split-type-btn:not(:last-child) { border-right:1px solid var(--border-light); }
.split-title-input { width:100%; padding:6px 10px; font-size:13px; border:1px solid var(--border-light); border-radius:6px; box-sizing:border-box; }
.split-score-row { display:flex; align-items:center; gap:8px; }
.split-label { font-size:12px; color:var(--text-tertiary); white-space:nowrap; }
.split-score-input { width:100px; padding:4px 8px; font-size:13px; border:1px solid var(--border-light); border-radius:4px; }

/* 知识点选择 */
.split-kp-section { padding:6px 0 0; border-top:1px dashed var(--border-light); }
.split-kp-row { display:flex; align-items:center; gap:8px; }
.split-kp-select { flex:1; padding:4px 8px; font-size:13px; border:1px solid var(--border-light); border-radius:4px; }
.split-comp-kps { display:flex; flex-direction:column; gap:6px; }
.split-kp-check-list { display:flex; flex-wrap:wrap; gap:6px; max-height:160px; overflow-y:auto; padding:6px; border:1px solid var(--border-light); border-radius:6px; background:#fff; }
.split-kp-check-item { display:flex; align-items:center; gap:4px; padding:4px 8px; border-radius:4px; font-size:13px; cursor:pointer; background:var(--bg-card-subtle); border:1px solid var(--border-light); }
.split-kp-check-item.selected { background:var(--accent-light); border-color:var(--accent-primary); }
.split-kp-check-name { font-size:13px; }
.split-weight-input { width:48px; padding:2px 4px; font-size:12px; text-align:center; border:1px solid var(--border-light); border-radius:4px; }
.split-kp-empty { color:var(--text-tertiary); font-size:12px; padding:8px; }
</style>
