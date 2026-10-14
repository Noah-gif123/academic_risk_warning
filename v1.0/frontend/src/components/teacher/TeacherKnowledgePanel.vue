<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { getKnowledgeTree, addKnowledgePoint, updateKnowledgeDesc, deleteKnowledgePoint as delKp } from '../../api/http.js'
import { getJson, postJson, putJson } from '../../api/http.js'

const { teacherToken } = useAuth()
const props = defineProps({
  courseId: { type: Number, required: true },
  courseName: { type: String, default: '' }
})

const knowledgeTree = ref([])
const loading = ref(false)
const message = ref('')
const messageType = ref('info')

// 添加知识点
const showAddModal = ref(false)
const newKpName = ref('')
const parentKpId = ref(null)
const adding = ref(false)

// 编辑描述
const showDescModal = ref(false)
const editKpId = ref(null)
const editKpName = ref('')
const editKpDesc = ref('')
const savingDesc = ref(false)

onMounted(() => loadTree())

async function loadTree() {
  loading.value = true
  try {
    const res = await getKnowledgeTree(teacherToken.value, props.courseId)
    if (res.success) knowledgeTree.value = res.data || []
    else setMsg(res.message || '加载知识点失败', 'error')
  } catch { setMsg('加载知识点失败', 'error') }
  finally { loading.value = false }
}

async function doAdd() {
  if (!newKpName.value.trim()) return
  adding.value = true
  try {
    const res = await addKnowledgePoint(teacherToken.value, props.courseId, parentKpId.value, newKpName.value.trim())
    if (res.success) { setMsg('添加成功', 'success'); showAddModal.value = false; newKpName.value = ''; loadTree() }
    else setMsg(res.message || '添加失败', 'error')
  } catch { setMsg('添加失败', 'error') }
  finally { adding.value = false }
}

async function doDeleteKp(id, name) {
  if (!confirm(`确定删除知识点 "${name}" 吗？子知识点也将被删除。`)) return
  try {
    const res = await delKp(teacherToken.value, id)
    if (res.success) { setMsg('已删除', 'success'); loadTree() }
    else setMsg(res.message || '删除失败', 'error')
  } catch { setMsg('删除失败', 'error') }
}

function openDescEditor(kp) {
  editKpId.value = kp.id || kp.knowledgePointId
  editKpName.value = kp.name || kp.knowledgePointName || ''
  editKpDesc.value = kp.description || ''
  showDescModal.value = true
}

async function doSaveDesc() {
  savingDesc.value = true
  try {
    const res = await updateKnowledgeDesc(teacherToken.value, editKpId.value, editKpDesc.value)
    if (res.success) { setMsg('保存成功', 'success'); showDescModal.value = false; loadTree() }
    else setMsg(res.message || '保存失败', 'error')
  } catch { setMsg('保存失败', 'error') }
  finally { savingDesc.value = false }
}

function setMsg(text, type = 'info') { message.value = text; messageType.value = type }

function openAdd(parentId) {
  parentKpId.value = parentId
  newKpName.value = ''
  showAddModal.value = true
}
</script>

<template>
  <div class="section">
    <div class="card" style="padding:16px;margin-bottom:16px;display:flex;align-items:center;gap:12px;">
      <h3>知识点管理 - {{ courseName }}</h3>
      <span style="flex:1;"></span>
      <button class="btn-primary" @click="openAdd(null)">+ 添加根知识点</button>
    </div>

    <p v-if="message" class="msg" :class="messageType" style="margin-bottom:12px;">{{ message }}</p>

    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <span class="spinner"></span> 加载中...
    </div>

    <div v-else-if="knowledgeTree.length">
      <!-- 递归知识点树 -->
      <div v-for="kp in knowledgeTree" :key="kp.id || kp.knowledgePointId" class="kp-node card" style="margin-bottom:8px;">
        <div class="kp-row" style="display:flex;align-items:center;gap:8px;padding:12px 16px;">
          <span class="kp-icon">📘</span>
          <strong style="flex:1;">{{ kp.name || kp.knowledgePointName }}</strong>
          <button class="btn-ghost btn-sm" @click="openDescEditor(kp)">✏️ 编辑</button>
          <button class="btn-ghost btn-sm" @click="openAdd(kp.id || kp.knowledgePointId)">+ 子节点</button>
          <button class="btn-ghost btn-sm btn-danger" @click="doDeleteKp(kp.id || kp.knowledgePointId, kp.name || kp.knowledgePointName)">删除</button>
        </div>
        <div v-if="kp.description" style="padding:0 16px 12px 40px;font-size:13px;color:var(--text-tertiary);line-height:1.6;">
          {{ kp.description }}
        </div>
        <!-- 递归子节点 -->
        <div v-if="kp.children && kp.children.length" style="padding-left:24px;padding-bottom:8px;">
          <div v-for="child in kp.children" :key="child.id || child.knowledgePointId" class="card" style="margin-bottom:4px;background:var(--bg-card-subtle);">
            <div style="display:flex;align-items:center;gap:8px;padding:8px 12px;">
              <span class="kp-icon">📄</span>
              <strong style="flex:1;font-size:14px;">{{ child.name || child.knowledgePointName }}</strong>
              <button class="btn-ghost btn-sm" @click="openDescEditor(child)">编辑</button>
              <button class="btn-ghost btn-sm" @click="openAdd(child.id || child.knowledgePointId)">+ 子节点</button>
              <button class="btn-ghost btn-sm btn-danger" @click="doDeleteKp(child.id || child.knowledgePointId, child.name || child.knowledgePointName)">删除</button>
            </div>
            <div v-if="child.description" style="padding:0 12px 8px 36px;font-size:12px;color:var(--text-tertiary);line-height:1.5;">
              {{ child.description }}
            </div>
            <!-- 三级 -->
            <div v-if="child.children && child.children.length" style="padding-left:24px;padding-bottom:6px;">
              <div v-for="gchild in child.children" :key="gchild.id || gchild.knowledgePointId" style="padding:6px 10px;font-size:13px;display:flex;align-items:center;gap:6px;border-bottom:1px solid var(--border-light);">
                <span>📌</span><span style="flex:1;">{{ gchild.name || gchild.knowledgePointName }}</span>
                <button class="btn-ghost btn-sm" @click="openDescEditor(gchild)">编辑</button>
                <button class="btn-ghost btn-sm btn-danger" @click="doDeleteKp(gchild.id || gchild.knowledgePointId, gchild.name || gchild.knowledgePointName)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card" style="text-align:center;padding:40px;color:var(--text-tertiary);">
      暂无知识点，请添加
    </div>

    <!-- 添加弹窗 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal-card" style="max-width:400px;">
        <h3 style="margin-bottom:16px;">添加知识点</h3>
        <label>名称 <input v-model="newKpName" style="width:100%;margin-top:4px;" placeholder="请输入知识点名称" /></label>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
          <button class="btn-ghost" @click="showAddModal = false">取消</button>
          <button class="btn-primary" @click="doAdd" :disabled="adding || !newKpName.trim()">{{ adding ? '添加中...' : '确定' }}</button>
        </div>
      </div>
    </div>

    <!-- 编辑描述弹窗 -->
    <div v-if="showDescModal" class="modal-overlay" @click.self="showDescModal = false">
      <div class="modal-card" style="max-width:500px;">
        <h3 style="margin-bottom:16px;">编辑知识点 - {{ editKpName }}</h3>
        <label>知识点介绍 <textarea v-model="editKpDesc" rows="5" style="width:100%;margin-top:4px;" placeholder="请输入知识点详细介绍..."></textarea></label>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
          <button class="btn-ghost" @click="showDescModal = false">取消</button>
          <button class="btn-primary" @click="doSaveDesc" :disabled="savingDesc">{{ savingDesc ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.btn-sm { font-size:12px; padding:3px 8px; }
.btn-danger { color:var(--semantic-red); }
.btn-danger:hover { background:var(--semantic-red-bg); }

.kp-node { border-left:3px solid var(--accent-primary); }
</style>
