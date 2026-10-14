<script setup>
import { ref } from 'vue'
import { useAuth } from '../../composables/useAuth.js'
import { importExcelData, downloadImportTemplate } from '../../api/http.js'

const { teacherToken } = useAuth()
const props = defineProps({ courseId: { type: Number, required: true } })
const emit = defineEmits(['message'])

const file = ref(null)
const fileName = ref('')
const uploading = ref(false)
const result = ref(null)
const message = ref('')
const messageType = ref('info')

function handleFile(e) {
  const f = e.target.files?.[0]
  if (!f) return
  if (!f.name.endsWith('.xlsx') && !f.name.endsWith('.xls')) {
    setMsg('仅支持 .xlsx 或 .xls 格式', 'error')
    return
  }
  file.value = f
  fileName.value = f.name
  result.value = null
}

async function doUpload() {
  if (!file.value) { setMsg('请选择文件', 'error'); return }
  uploading.value = true
  result.value = null
  try {
    const res = await importExcelData(teacherToken.value, file.value)
    if (res.success) {
      result.value = res.data || {}
      setMsg(res.message || '导入成功', 'success')
      emit('message', res.message || '导入成功', 'success')
    } else setMsg(res.message || '导入失败', 'error')
  } catch { setMsg('导入失败，请检查网络', 'error') }
  finally { uploading.value = false }
}

async function doDownloadTemplate() {
  try {
    await downloadImportTemplate(teacherToken.value)
    setMsg('模板下载开始', 'success')
  } catch { setMsg('模板下载失败', 'error') }
}

function setMsg(text, type = 'info') { message.value = text; messageType.value = type }

function formatNum(n) { return n ?? 0 }
</script>

<template>
  <div class="section">
    <div class="card" style="padding:24px;max-width:600px;margin:0 auto;">
      <div style="display:flex;align-items:center;gap:8px;margin-bottom:20px;">
        <div class="upload-icon">📊</div>
        <div>
          <h3 style="margin:0;">批量数据导入</h3>
          <p style="margin:0;font-size:13px;color:var(--text-tertiary);">支持成绩、作业、出勤等 Excel 数据批量导入</p>
        </div>
      </div>

      <!-- 模板下载 -->
      <div class="template-box" style="padding:16px;background:var(--bg-card-subtle);border-radius:12px;margin-bottom:20px;">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
          <span>📋</span>
          <strong style="font-size:14px;">第一步：下载导入模板</strong>
        </div>
        <p style="font-size:13px;color:var(--text-tertiary);margin-bottom:8px;">请先下载标准模板，按格式填写数据后再上传</p>
        <button class="btn-primary" @click="doDownloadTemplate">📥 下载模板</button>
      </div>

      <!-- 文件上传 -->
      <div class="upload-box" style="border:2px dashed var(--border-default);border-radius:12px;padding:32px;text-align:center;margin-bottom:20px;"
        :style="{ borderColor: fileName ? 'var(--accent-primary)' : 'var(--border-default)' }">
        <div v-if="!fileName">
          <div style="font-size:2rem;margin-bottom:8px;">📁</div>
          <p style="margin-bottom:12px;color:var(--text-secondary);">拖拽或点击选择 Excel 文件</p>
          <input type="file" @change="handleFile" accept=".xlsx,.xls"
            style="width:100%;" />
        </div>
        <div v-else>
          <div style="font-size:2rem;margin-bottom:8px;">📄</div>
          <p style="font-weight:600;color:var(--accent-primary);margin-bottom:4px;">{{ fileName }}</p>
          <p style="font-size:12px;color:var(--text-tertiary);margin-bottom:12px;">文件已就绪，点击上传</p>
          <div style="display:flex;gap:8px;justify-content:center;">
            <button class="btn-primary" @click="doUpload" :disabled="uploading">
              {{ uploading ? '上传中...' : '🚀 开始导入' }}
            </button>
            <button class="btn-ghost" @click="file = null; fileName = ''; result = null">更换文件</button>
          </div>
        </div>
      </div>

      <!-- 提示信息和结果 -->
      <p v-if="message" class="msg" :class="messageType" style="margin-bottom:12px;">{{ message }}</p>

      <!-- 导入结果 -->
      <div v-if="result" class="result-box" style="padding:16px;border-radius:12px;background:var(--semantic-green-bg);border:1px solid var(--semantic-green-border);">
        <h4 style="margin-bottom:8px;color:var(--semantic-green-text);">导入成功</h4>
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;font-size:14px;">
          <div style="text-align:center;">
            <div style="font-size:1.5rem;font-weight:700;color:var(--semantic-green-text);">{{ formatNum(result.scoreCount) }}</div>
            <div style="color:var(--text-tertiary);font-size:12px;">成绩数据</div>
          </div>
          <div style="text-align:center;">
            <div style="font-size:1.5rem;font-weight:700;color:var(--semantic-green-text);">{{ formatNum(result.homeworkCount) }}</div>
            <div style="color:var(--text-tertiary);font-size:12px;">作业数据</div>
          </div>
          <div style="text-align:center;">
            <div style="font-size:1.5rem;font-weight:700;color:var(--semantic-green-text);">{{ formatNum(result.attendanceCount) }}</div>
            <div style="color:var(--text-tertiary);font-size:12px;">出勤数据</div>
          </div>
        </div>
      </div>

      <!-- 使用说明 -->
      <div class="help-box" style="padding:16px;background:var(--semantic-blue-bg);border-radius:12px;margin-top:16px;">
        <h4 style="margin-bottom:8px;font-size:14px;">💡 使用说明</h4>
        <ul style="margin:0;padding-left:20px;font-size:13px;color:var(--text-secondary);line-height:1.8;">
          <li>请先下载标准模板，按格式填写数据</li>
          <li>支持 .xlsx 和 .xls 格式</li>
          <li>导入数据将自动关联到当前课程</li>
          <li>如果已有数据，将进行更新而非重复插入</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-icon { font-size:2rem; }
.upload-box { transition:border-color var(--transition-base); }
.upload-box:hover { border-color:var(--accent-primary); background:var(--accent-light); }
</style>
