<template>
  <div class="space-y-4">
    <div v-if="!embedded" class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="back">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">课程内容</h1>
        <span class="tag bg-slate-100 text-slate-500">教学班 #{{ teachingClassId }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <!-- 页签（嵌入到「教学详情」时由外层页签控制，这里不再重复一排） -->
    <div class="flex flex-wrap gap-2" v-if="!embedded">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-brand-500 text-white' : 'bg-white text-slate-600 border border-slate-200'"
              @click="tab = t.key">{{ t.label }}</button>
    </div>

    <!-- 文件选择：目录页与资料库页共用（支持多选） -->
    <input ref="fileInput" type="file" class="hidden" multiple @change="onPick" />

    <!-- ============ 导学 ============ -->
    <div class="card p-4 space-y-3" v-if="tab === 'guide'">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">课程导学</h2>
        <span class="text-xs text-slate-400">
          版本 v{{ guide.version }}
          <span v-if="guide.isPublished === 1" class="text-emerald-600 ml-2">已发布 · {{ fmt(guide.publishTime) }}</span>
          <span v-else class="text-amber-600 ml-2">未发布（学生看不到）</span>
        </span>
      </div>
      <p class="text-xs text-slate-400">
        导学是教师写给学生的课程说明（学什么、怎么学、怎么考）。直接用富文本编辑：加粗、字号、标题、列表都能用；
        保存后自动记录版本，点「保存并发布」学生端才可见。
      </p>
      <RichTextEditor v-model="guide.content" :min-height="300"
                      placeholder="例：本课程总评 = 平时成绩 40%（课堂测试 7×5 + 三级项目 20 + 课堂表现 5 + 课后作业 4×10）+ 期末考试 60%……" />
      <div class="flex items-center gap-3 flex-wrap">
        <button class="btn-primary text-sm" @click="saveGuide(false)">保存草稿</button>
        <button class="btn-ghost text-sm" @click="saveGuide(true)">保存并发布</button>
        <span class="text-xs text-slate-400">
          正文 {{ guide.content ? guide.content.replace(/<[^>]+>/g, '').trim().length : 0 }} 字（不含标签）
        </span>
      </div>
    </div>

    <!-- ============ 教学目标 ============ -->
    <div class="card p-4 space-y-3" v-if="tab === 'objectives'">
      <div class="flex items-center justify-between">
        <h2 class="font-medium text-slate-700">教学目标</h2>
        <span class="text-xs text-slate-400">表格展示、内容过长时截断显示</span>
      </div>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2 w-28">编号</th>
            <th class="text-left py-2">目标内容</th>
            <th class="text-left py-2 w-36">类型</th>
            <th class="text-right py-2 w-16">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(o, i) in objectives" :key="i" class="border-b border-slate-100">
            <td class="py-1.5 pr-2"><input v-model="o.objCode" class="input" placeholder="OBJ-1" /></td>
            <td class="py-1.5 pr-2"><input v-model="o.objContent" class="input" placeholder="能够……" /></td>
            <td class="py-1.5 pr-2">
              <select v-model="o.objType" class="input">
                <option value="">未分类</option>
                <option value="知识目标">知识目标</option>
                <option value="能力目标">能力目标</option>
                <option value="素质目标">素质目标</option>
              </select>
            </td>
            <td class="py-1.5 text-right">
              <button class="text-xs text-slate-400 hover:text-red-500" @click="objectives.splice(i, 1)">删除</button>
            </td>
          </tr>
          <tr v-if="!objectives.length"><td colspan="4" class="py-6 text-center text-slate-400">还没有教学目标</td></tr>
        </tbody>
      </table>
      <div class="flex items-center gap-2">
        <button class="btn-ghost text-sm" @click="addObjective">+ 添加一行</button>
        <button class="btn-primary text-sm" @click="saveObjectives">保存教学目标</button>
        <span class="text-xs text-slate-400">保存以当前清单为准，删掉的行会被停用（不物理删除）。</span>
      </div>
    </div>

    <!-- ============ 目录 ============ -->
    <div class="card p-4 space-y-3" v-if="tab === 'lessons'">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <h2 class="font-medium text-slate-700">课程目录（一行 = 一次课）</h2>
        <div class="flex items-center gap-2">
          <span class="text-xs text-slate-400">共 {{ lessons.length }} 次课</span>
          <button class="btn-ghost text-sm" @click="sync">按课次生成目录</button>
        </div>
      </div>
      <p class="text-xs text-slate-400">
        目录由课次自动生成并与点名、覆盖率对齐；每次课展开为「课前 / 课中 / 课后」三段，
        可挂资料、外链、富文本，以及后续发布的测验 / 作业 / 项目。
      </p>

      <div class="space-y-2">
        <div v-for="l in lessons" :key="l.id" class="border border-slate-200 rounded-lg">
          <div class="flex flex-wrap items-center gap-3 px-3 py-2 cursor-pointer hover:bg-slate-50"
               @click="toggle(l.id)">
            <span class="text-xs text-slate-400 w-24">第 {{ l.weekNo }} 周 · 第 {{ l.sessionNo }} 次</span>
            <span class="text-xs text-slate-500 w-24">{{ l.sessionDate || '—' }}</span>
            <input v-model="l.title" class="input flex-1 min-w-48" placeholder="本次课内容标题（可含多条，用分号）"
                   @click.stop />
            <button class="text-xs text-brand-600 hover:underline" @click.stop="saveLesson(l)">保存标题</button>
            <span class="text-xs text-slate-400">
              课前 {{ l.before.length }} ｜ 课中 {{ l.during.length }} ｜ 课后 {{ l.after.length }}
            </span>
            <span class="text-xs text-slate-400">{{ expanded === l.id ? '收起' : '展开' }}</span>
          </div>

          <div v-if="expanded === l.id" class="border-t border-slate-100 p-3 space-y-3 bg-slate-50/50">
            <div v-for="ph in phases" :key="ph.value" class="space-y-1">
              <p class="text-xs font-medium text-slate-600">{{ ph.label }}</p>
              <div v-for="r in l[ph.key]" :key="r.id"
                   class="flex flex-wrap items-center gap-2 text-sm bg-white border border-slate-200 rounded-lg px-3 py-1.5">
                <span class="tag bg-slate-100 text-slate-500">{{ typeLabel(r.resType) }}</span>
                <span class="flex-1 min-w-32 truncate">{{ r.title }}</span>
                <span class="text-xs" :class="r.publishStatus === 'PUBLISHED' ? 'text-emerald-600' : 'text-amber-600'">
                  {{ r.publishStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
                </span>
                <button class="text-xs text-brand-600 hover:underline"
                        @click="togglePublish(r)">{{ r.publishStatus === 'PUBLISHED' ? '撤回' : '发布' }}</button>
                <button v-if="r.resType === 'FILE'" class="text-xs text-slate-500 hover:underline"
                        @click="contentApi.downloadFile(r.refId, r.displayName)">下载</button>
                <a v-if="r.resType === 'LINK'" :href="r.extUrl" target="_blank" class="text-xs text-slate-500 hover:underline">打开</a>
                <button class="text-xs text-slate-400 hover:text-red-500" @click="removeResource(r)">删除</button>
              </div>
              <p v-if="!l[ph.key].length" class="text-xs text-slate-300">暂无</p>
            </div>

            <!-- 新增资源 -->
            <div class="flex flex-wrap items-end gap-2 pt-2 border-t border-slate-200">
              <label class="text-xs">阶段
                <select v-model="resForm.phase" class="input mt-1 w-24">
                  <option v-for="p in phases" :key="p.value" :value="p.value">{{ p.label }}</option>
                </select>
              </label>
              <label class="text-xs">类型
                <select v-model="resForm.resType" class="input mt-1 w-32">
                  <option value="FILE">文件</option>
                  <option value="LINK">外链</option>
                  <option value="TEXT">富文本</option>
                  <option value="PAPER">测验 / 作业</option>
                </select>
              </label>
              <div class="text-xs w-full" v-if="resForm.resType === 'FILE'">
                <div class="flex flex-wrap items-center gap-2 mt-1">
                  <button class="btn-ghost text-sm" type="button" @click="openPicker">从资料库选择</button>
                  <button class="btn-ghost text-sm" type="button" :disabled="uploading" @click="fileInput.click()">
                    {{ uploading ? '上传中…' : '从电脑上传' }}
                  </button>
                  <span class="text-slate-400">也可以把文件直接拖到下面的虚线框</span>
                </div>
                <div class="mt-2 flex flex-wrap gap-1.5" v-if="resForm.refIds.length">
                  <span v-for="id in resForm.refIds" :key="id"
                        class="inline-flex items-center gap-1 bg-brand-50 text-brand-700 border border-brand-200 rounded-full pl-2 pr-1 py-0.5">
                    {{ fileNameOf(id) }}
                    <button type="button" class="text-brand-500 hover:text-red-500 px-1" @click="removePicked(id)">✕</button>
                  </span>
                </div>
                <div class="mt-2 border border-dashed border-slate-300 rounded-lg px-3 py-3 text-center text-slate-400"
                     :class="dragging ? 'bg-brand-50 border-brand-300' : ''"
                     @dragover.prevent="dragging = true"
                     @dragleave.prevent="dragging = false"
                     @drop.prevent="onDrop">
                  拖拽文件到这里上传（支持多选）
                </div>
              </div>
              <label class="text-xs" v-if="resForm.resType === 'PAPER'">测验 / 作业
                <select v-model="resForm.refId" class="input mt-1 w-56">
                  <option :value="null">暂无（1-7 后可发布）</option>
                  <option v-for="p in papers" :key="p.id" :value="p.id">{{ p.paperName }}</option>
                </select>
              </label>
              <label class="text-xs" v-if="resForm.resType === 'LINK'">地址
                <input v-model="resForm.extUrl" class="input mt-1 w-64" placeholder="https://" />
              </label>
              <label class="text-xs">标题
                <input v-model="resForm.title" class="input mt-1 w-56" placeholder="留空自动取名" />
              </label>
              <label class="flex items-center gap-1 text-xs text-slate-500 mb-2">
                <input type="checkbox" v-model="resForm.publish" /> 立即发布给学生
              </label>
              <button class="btn-primary text-sm" @click="addResource(l)">添加到本次课</button>
            </div>
          </div>
        </div>
        <p v-if="!lessons.length" class="py-6 text-center text-slate-400 text-sm">
          还没有目录。先由教务在「排课与教学班」生成课次，然后点上面的「按课次生成目录」。
        </p>
      </div>
    </div>

    <!-- ============ 资料库选择器（弹窗） ============ -->
    <div v-if="pickerOpen" class="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-4"
         @click.self="pickerOpen = false">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[80vh] flex flex-col">
        <div class="flex items-center justify-between px-4 py-3 border-b border-slate-200">
          <div class="flex items-center gap-2">
            <h3 class="font-medium text-slate-800">从资料库选择</h3>
            <span class="tag bg-brand-50 text-brand-700">课程组共享 {{ resources.length }} 份</span>
          </div>
          <button class="text-slate-400 hover:text-slate-600" @click="pickerOpen = false">✕</button>
        </div>
        <div class="px-4 py-3 border-b border-slate-100 flex flex-wrap items-center gap-2">
          <input v-model="pickerSearch" class="input w-64" placeholder="搜索文件名 / 上传人" />
          <label class="flex items-center gap-1 text-xs text-slate-500">
            <input type="checkbox" v-model="pickerMineOnly" /> 只看我上传
          </label>
          <span class="ml-auto text-xs text-slate-400">已选 {{ pickerSelected.length }} 份</span>
        </div>
        <div class="flex-1 overflow-y-auto px-4">
          <table class="w-full text-sm">
            <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
              <tr>
                <th class="py-2 w-8">
                  <input type="checkbox" :checked="pageAllChecked" @change="togglePage($event.target.checked)" />
                </th>
                <th class="text-left py-2">文件名</th>
                <th class="text-left py-2 w-24">上传人</th>
                <th class="text-left py-2 w-24">教学班</th>
                <th class="text-left py-2 w-32">上传时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in pickerPageRows" :key="f.id" class="border-b border-slate-100 hover:bg-slate-50"
                  @click="togglePick(f.id)">
                <td class="py-2"><input type="checkbox" :value="f.id" v-model="pickerSelected" @click.stop /></td>
                <td class="py-2 truncate max-w-72">{{ f.fileName }}</td>
                <td class="py-2 text-slate-500">{{ f.uploaderName || '—' }}</td>
                <td class="py-2 text-slate-500">{{ f.classCode || '课程组' }}</td>
                <td class="py-2 text-slate-500">{{ fmt(f.createTime) }}</td>
              </tr>
              <tr v-if="!pickerRows.length">
                <td colspan="5" class="py-10 text-center text-slate-400">
                  {{ resources.length ? '没有匹配的资料' : '资料库还是空的，先点「从电脑上传」' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="px-4 py-3 border-t border-slate-200 flex items-center justify-between">
          <div class="flex items-center gap-2 text-sm">
            <button class="btn-ghost text-sm" :disabled="pickerPage <= 1" @click="pickerPage--">上一页</button>
            <span class="text-slate-500">{{ pickerPage }} / {{ pickerPageCount }}</span>
            <button class="btn-ghost text-sm" :disabled="pickerPage >= pickerPageCount" @click="pickerPage++">下一页</button>
            <select v-model.number="pickerPageSize" class="input w-20 ml-2">
              <option :value="10">10 条</option>
              <option :value="20">20 条</option>
              <option :value="50">50 条</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <button class="btn-ghost text-sm" @click="pickerOpen = false">取消</button>
            <button class="btn-primary text-sm" @click="confirmPick">确定（{{ pickerSelected.length }}）</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 资料库（课程组共享） ============ -->
    <div class="card p-4 space-y-3" v-if="tab === 'files'">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-3 flex-wrap">
          <h2 class="font-medium text-slate-700">课程资料库</h2>
          <span class="tag bg-brand-50 text-brand-700">课程组共享</span>
        </div>
        <div class="flex items-center gap-2">
          <label class="flex items-center gap-1 text-xs text-slate-500">
            <input type="checkbox" v-model="mineOnly" @change="loadResources" /> 只看我上传
          </label>
          <button class="btn-primary text-sm" :disabled="uploading" @click="fileInput && fileInput.click()">
            {{ uploading ? '上传中…' : '上传文件' }}
          </button>
        </div>
      </div>
      <p class="text-xs text-slate-400">
        同一门课（同一个开课实例）下所有教学班、所有老师上传的资料都在这里，互相可见、可下载；
        上传时记录到当前教学班。文件只存元信息，本体落服务器目录，作废为逻辑删除（不破坏已引用它的课时）。
      </p>
      <p class="text-xs text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-lg px-3 py-2">
        上传即入库：课件（ppt/md/txt/docx/pptx）上传后自动进本课程知识库，学生在「课程答疑」里问课件内容时，
        AI 只依据这些资料作答，并在答案后标注「文档名 + 第几段（第几页）」，可以翻到对应页核对。
      </p>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">文件名</th><th class="text-left py-2 w-16">类型</th>
            <th class="text-right py-2 w-24">大小</th>
            <th class="text-left py-2 w-32">上传人</th>
            <th class="text-left py-2 w-24">教学班</th>
            <th class="text-left py-2 w-40">上传时间</th>
            <th class="text-right py-2 w-32">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in resources" :key="f.id" class="border-b border-slate-100">
            <td class="py-2 truncate max-w-80">{{ f.fileName }}</td>
            <td class="py-2 text-slate-500">{{ f.fileExt || '—' }}</td>
            <td class="py-2 text-right text-slate-500">{{ sizeText(f.fileSize) }}</td>
            <td class="py-2">
              <span :class="f.isMine ? 'text-slate-700' : 'text-slate-500'">{{ f.uploaderName || '—' }}</span>
              <span v-if="f.isMine" class="tag bg-slate-100 text-slate-500 ml-1">我</span>
            </td>
            <td class="py-2 text-slate-500">{{ f.classCode || '课程组' }}</td>
            <td class="py-2 text-slate-500">{{ fmt(f.createTime) }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline mr-3" @click="contentApi.downloadFile(f.id, f.fileName)">下载</button>
              <button v-if="f.isMine" class="text-slate-400 hover:text-red-500" @click="voidFile(f)">作废</button>
              <span v-else class="text-xs text-slate-300">他人上传</span>
            </td>
          </tr>
          <tr v-if="!resources.length"><td colspan="7" class="py-6 text-center text-slate-400">
            {{ mineOnly ? '你还没有上传过资料' : '本课程组还没有资料，点右上角「上传文件」' }}
          </td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { contentApi } from '../../api/content'
import RichTextEditor from '../../components/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()
const props = defineProps({
  teachingClassId: { type: Number, default: 0 },
  offeringId: { type: Number, default: 0 },
  embedded: { type: Boolean, default: false },
  initialTab: { type: String, default: 'guide' }
})
const teachingClassId = props.teachingClassId || Number(route.query.teachingClassId || 0)
const offeringId = props.offeringId || Number(route.query.offeringId || 0)

const tabs = [
  { key: 'guide', label: '导学' },
  { key: 'objectives', label: '教学目标' },
  { key: 'lessons', label: '目录' },
  { key: 'files', label: '资料库' }
]
const phases = [
  { value: 'BEFORE', key: 'before', label: '课前预习' },
  { value: 'DURING', key: 'during', label: '课中随堂' },
  { value: 'AFTER', key: 'after', label: '课后作业' }
]

const tab = ref(props.initialTab)
const message = ref('')
const ok = ref(true)
const guide = reactive({ content: '', contentFormat: 'HTML', version: 0, isPublished: 0, publishTime: '' })
const objectives = ref([])
const lessons = ref([])
const resources = ref([])
const papers = ref([])
const expanded = ref(null)
const uploading = ref(false)
const mineOnly = ref(false)
const fileInput = ref(null)
const resForm = reactive({ phase: 'BEFORE', resType: 'FILE', refId: null, refIds: [], extUrl: '', title: '', publish: true })

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}
function typeLabel(t) {
  return { FILE: '文件', LINK: '外链', TEXT: '富文本', PAPER: '测验/作业', PROJECT: '项目' }[t] || t
}
function fmt(dt) {
  if (!dt) return '—'
  return String(dt).replace('T', ' ').slice(0, 16)
}
function sizeText(n) {
  if (!n) return '0 B'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}
function back() { router.push('/teacher/home') }

// ---------------- 导学
async function loadGuide() {
  const g = await contentApi.guide(teachingClassId)
  Object.assign(guide, g || {})
  if (!guide.contentFormat) guide.contentFormat = 'HTML'
}
async function saveGuide(publish) {
  try {
    const g = await contentApi.saveGuide({
      teachingClassId, content: guide.content, contentFormat: guide.contentFormat, publish
    })
    Object.assign(guide, g)
    toast(publish ? '已发布给学生' : '已保存（未发布）')
  } catch (e) { toast(e.message, false) }
}

// ---------------- 教学目标
async function loadObjectives() {
  objectives.value = await contentApi.objectives(teachingClassId)
}
function addObjective() {
  objectives.value.push({ objCode: '', objContent: '', objType: '', sortNo: objectives.value.length + 1 })
}
async function saveObjectives() {
  try {
    objectives.value = await contentApi.saveObjectives({
      teachingClassId,
      items: objectives.value.map((o, i) => ({
        id: o.id || null, objCode: o.objCode, objContent: o.objContent, objType: o.objType, sortNo: i + 1
      }))
    })
    toast('教学目标已保存')
  } catch (e) { toast(e.message, false) }
}

// ---------------- 目录
async function loadLessons() {
  lessons.value = await contentApi.lessons(teachingClassId)
}
async function sync() {
  try {
    lessons.value = await contentApi.syncLessons(teachingClassId)
    toast('目录已按课次生成')
  } catch (e) { toast(e.message, false) }
}
function toggle(id) { expanded.value = expanded.value === id ? null : id }
async function saveLesson(l) {
  try {
    await contentApi.updateLesson(l.id, { title: l.title })
    toast('标题已保存')
  } catch (e) { toast(e.message, false) }
}
async function addResource(l) {
  // 文件类型支持一次挂多份（从资料库多选 / 拖拽或多选上传）
  if (resForm.resType === 'FILE') {
    if (!resForm.refIds.length) { toast('请先从资料库选择，或从电脑上传文件', false); return }
    try {
      for (const id of resForm.refIds) {
        await contentApi.addResource({
          lessonId: l.id, phase: resForm.phase, resType: 'FILE', refId: id,
          extUrl: null, title: resForm.title || null, publish: resForm.publish
        })
      }
      toast(`已添加 ${resForm.refIds.length} 份资料`)
      resForm.title = ''
      resForm.refIds = []
      await loadLessons()
      expanded.value = l.id
    } catch (e) { toast(e.message, false) }
    return
  }
  const body = {
    lessonId: l.id, phase: resForm.phase, resType: resForm.resType,
    refId: resForm.resType === 'LINK' || resForm.resType === 'TEXT' ? null : resForm.refId,
    extUrl: resForm.resType === 'LINK' ? resForm.extUrl : null,
    title: resForm.title || null, publish: resForm.publish
  }
  try {
    await contentApi.addResource(body)
    resForm.title = ''
    await loadLessons()
    expanded.value = l.id
    toast('已添加')
  } catch (e) { toast(e.message, false) }
}
async function togglePublish(r) {
  try {
    await contentApi.publishResource(r.id, r.publishStatus !== 'PUBLISHED')
    await loadLessons()
    expanded.value = r.lessonId
  } catch (e) { toast(e.message, false) }
}
async function removeResource(r) {
  try {
    await contentApi.deleteResource(r.id)
    await loadLessons()
    expanded.value = r.lessonId
    toast('已删除')
  } catch (e) { toast(e.message, false) }
}

// ---------------- 资料库（课程组共享：按开课实例取，含其他老师上传的）
async function loadResources() {
  resources.value = await contentApi.resources({
    teachingClassId,
    offeringId: offeringId || undefined,
    mineOnly: mineOnly.value ? true : undefined
  })
  papers.value = await contentApi.papers(teachingClassId)
}

// ---------------- 资料库选择器（弹窗：搜索 + 只看我上传 + 分页 + 多选）
const pickerOpen = ref(false)
const pickerSearch = ref('')
const pickerMineOnly = ref(false)
const pickerPage = ref(1)
const pickerPageSize = ref(10)
const pickerSelected = ref([])
const dragging = ref(false)

const pickerRows = computed(() => {
  const kw = pickerSearch.value.trim()
  return resources.value.filter((f) => {
    if (pickerMineOnly.value && !f.isMine) return false
    if (!kw) return true
    return String(f.fileName || '').includes(kw) || String(f.uploaderName || '').includes(kw)
  })
})
const pickerPageCount = computed(() => Math.max(1, Math.ceil(pickerRows.value.length / pickerPageSize.value)))
const pickerPageRows = computed(() => {
  const start = (pickerPage.value - 1) * pickerPageSize.value
  return pickerRows.value.slice(start, start + pickerPageSize.value)
})
const pageAllChecked = computed(() =>
  pickerPageRows.value.length > 0 && pickerPageRows.value.every((f) => pickerSelected.value.includes(f.id))
)

function openPicker() {
  pickerSearch.value = ''
  pickerMineOnly.value = mineOnly.value
  pickerPage.value = 1
  pickerSelected.value = []
  pickerOpen.value = true
}
function togglePick(id) {
  if (pickerSelected.value.includes(id)) {
    pickerSelected.value = pickerSelected.value.filter((x) => x !== id)
  } else {
    pickerSelected.value = [...pickerSelected.value, id]
  }
}
function togglePage(checked) {
  const pageIds = pickerPageRows.value.map((f) => f.id)
  if (checked) {
    const set = new Set([...pickerSelected.value, ...pageIds])
    pickerSelected.value = Array.from(set)
  } else {
    pickerSelected.value = pickerSelected.value.filter((id) => !pageIds.includes(id))
  }
}
function confirmPick() {
  resForm.refIds = [...new Set([...resForm.refIds, ...pickerSelected.value])]
  pickerOpen.value = false
  toast(`已选 ${pickerSelected.value.length} 份资料`)
}
function removePicked(id) {
  resForm.refIds = resForm.refIds.filter((x) => x !== id)
}
function fileNameOf(id) {
  const f = resources.value.find((x) => x.id === id)
  return f ? f.fileName : '#' + id
}

async function onPick(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  uploading.value = true
  try {
    for (const file of files) {
      const saved = await contentApi.upload(teachingClassId, file)
      if (saved && saved.id) resForm.refIds = [...resForm.refIds, saved.id]
    }
    await loadResources()
    toast(`已上传 ${files.length} 个文件，可直接「添加到本次课」`)
  } catch (err) { toast(err.message, false) }
  uploading.value = false
  e.target.value = ''
}

async function onDrop(e) {
  dragging.value = false
  const files = Array.from((e.dataTransfer && e.dataTransfer.files) || [])
  if (!files.length) return
  uploading.value = true
  try {
    for (const file of files) {
      const saved = await contentApi.upload(teachingClassId, file)
      if (saved && saved.id) resForm.refIds = [...resForm.refIds, saved.id]
    }
    await loadResources()
    toast(`已上传 ${files.length} 个文件`)
  } catch (err) { toast(err.message, false) }
  uploading.value = false
}
async function voidFile(f) {
  try {
    await contentApi.voidFile(f.id, '教师手工作废')
    await loadResources()
    toast('已作废')
  } catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    await Promise.all([loadGuide(), loadObjectives(), loadLessons(), loadResources()])
  } catch (e) { toast(e.message, false) }
})
</script>
