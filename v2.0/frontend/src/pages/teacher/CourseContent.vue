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
              <label class="text-xs" v-if="resForm.resType === 'FILE'">选择文件
                <select v-model="resForm.refId" class="input mt-1 w-56">
                  <option :value="null">请先到资料库上传</option>
                  <option v-for="f in resources" :key="f.id" :value="f.id">{{ f.fileName }}</option>
                </select>
              </label>
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
          <input ref="fileInput" type="file" class="hidden" @change="onPick" />
          <button class="btn-primary text-sm" :disabled="uploading" @click="$refs.fileInput.click()">
            {{ uploading ? '上传中…' : '上传文件' }}
          </button>
        </div>
      </div>
      <p class="text-xs text-slate-400">
        同一门课（同一个开课实例）下所有教学班、所有老师上传的资料都在这里，互相可见、可下载；
        上传时记录到当前教学班。文件只存元信息，本体落服务器目录，作废为逻辑删除（不破坏已引用它的课时）。
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
import { onMounted, reactive, ref } from 'vue'
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
const resForm = reactive({ phase: 'BEFORE', resType: 'FILE', refId: null, extUrl: '', title: '', publish: true })

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
async function onPick(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    await contentApi.upload(teachingClassId, file)
    await loadResources()
    toast('上传成功：' + file.name)
  } catch (err) { toast(err.message, false) }
  uploading.value = false
  e.target.value = ''
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
