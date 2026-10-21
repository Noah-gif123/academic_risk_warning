<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-lg font-semibold text-slate-800">基础数据</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <!-- 页签 -->
    <div class="flex gap-1 border-b border-slate-200">
      <button v-for="t in tabs" :key="t.key"
        class="px-4 py-2 text-sm -mb-px border-b-2"
        :class="tab === t.key ? 'border-brand-500 text-brand-600 font-medium' : 'border-transparent text-slate-500 hover:text-slate-700'"
        @click="tab = t.key">{{ t.text }}</button>
    </div>

    <!-- ============================ 学期与周历 ============================ -->
    <div v-if="tab === 'term'" class="space-y-4">
      <div class="card p-4">
        <div class="flex items-center justify-between mb-3">
          <h2 class="font-medium text-slate-700">学期</h2>
          <button class="btn-ghost text-xs" @click="resetTermForm">新增学期</button>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-slate-500 border-b border-slate-200">
              <tr>
                <th class="text-left py-2">编码</th><th class="text-left py-2">学年</th>
                <th class="text-left py-2">学期名称</th><th class="text-left py-2">起止日期</th>
                <th class="text-right py-2">周数</th><th class="text-right py-2">周历</th>
                <th class="text-center py-2">当前</th><th class="text-right py-2">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in terms" :key="t.id" class="border-b border-slate-100 hover:bg-slate-50">
                <td class="py-2">{{ t.termCode }}</td>
                <td class="py-2">{{ t.schoolYear }}</td>
                <td class="py-2">{{ t.termName }}</td>
                <td class="py-2 text-slate-500">{{ t.startDate }} ~ {{ t.endDate }}</td>
                <td class="py-2 text-right">{{ t.totalWeeks }}</td>
                <td class="py-2 text-right">{{ t.calendarWeeks }} 周</td>
                <td class="py-2 text-center">
                  <span v-if="t.current" class="tag bg-emerald-50 text-emerald-600">当前</span>
                  <span v-else class="text-slate-300">—</span>
                </td>
                <td class="py-2 text-right whitespace-nowrap">
                  <button class="text-brand-600 hover:underline mr-3" @click="setCurrent(t)">设为当前</button>
                  <button class="text-brand-600 hover:underline mr-3" @click="pickTerm(t)">查看周历</button>
                  <button class="text-slate-500 hover:underline" @click="editTerm(t)">编辑</button>
                </td>
              </tr>
              <tr v-if="!terms.length"><td colspan="8" class="py-6 text-center text-slate-400">暂无学期</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 学期表单 -->
      <div class="card p-4">
        <h2 class="font-medium text-slate-700 mb-3">{{ termForm.id ? '编辑学期' : '新增学期' }}</h2>
        <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
          <label class="text-sm">编码<input v-model="termForm.termCode" class="input mt-1" placeholder="2026-2027-1" /></label>
          <label class="text-sm">学年<input v-model="termForm.schoolYear" class="input mt-1" placeholder="2026-2027" /></label>
          <label class="text-sm">学期序号<input v-model.number="termForm.termNo" type="number" class="input mt-1" placeholder="1" /></label>
          <label class="text-sm">学期名称<input v-model="termForm.termName" class="input mt-1" placeholder="2026-2027学年-第1学期" /></label>
          <label class="text-sm">开始日期<input v-model="termForm.startDate" type="date" class="input mt-1" /></label>
          <label class="text-sm">结束日期<input v-model="termForm.endDate" type="date" class="input mt-1" /></label>
          <label class="text-sm">总周数<input v-model.number="termForm.totalWeeks" type="number" class="input mt-1" placeholder="16" /></label>
          <div class="flex items-end gap-2">
            <button class="btn-primary text-sm" @click="saveTerm">保存</button>
            <button class="btn-ghost text-sm" @click="resetTermForm">清空</button>
          </div>
        </div>
      </div>

      <!-- 周历 -->
      <div class="card p-4" v-if="activeTerm">
        <div class="flex items-center justify-between mb-3">
          <h2 class="font-medium text-slate-700">
            教学周历 · {{ activeTerm.termName }}
            <span class="text-xs text-slate-400 ml-2">应完成课次与覆盖率的分母以此为准</span>
          </h2>
          <button class="btn-primary text-xs" @click="genCalendar(false)">生成周历</button>
        </div>
        <div class="overflow-x-auto max-h-[420px] overflow-y-auto">
          <table class="w-full text-sm">
            <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
              <tr>
                <th class="text-left py-2">周次</th><th class="text-left py-2">起止</th>
                <th class="text-left py-2">类型</th><th class="text-right py-2">假日天数</th>
                <th class="text-left py-2">备注</th><th class="text-right py-2">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="w in calendar" :key="w.id" class="border-b border-slate-100">
                <td class="py-1.5">第 {{ w.weekNo }} 周</td>
                <td class="py-1.5 text-slate-500">{{ w.startDate }} ~ {{ w.endDate }}</td>
                <td class="py-1.5">
                  <select v-model="w.weekType" class="border border-slate-300 rounded px-2 py-1 text-xs">
                    <option value="TEACHING">教学周</option>
                    <option value="HOLIDAY">假期</option>
                    <option value="EXAM">考试周</option>
                  </select>
                </td>
                <td class="py-1.5 text-right">
                  <input v-model.number="w.holidayDays" type="number" min="0" max="7"
                    class="w-16 border border-slate-300 rounded px-2 py-1 text-xs text-right" />
                </td>
                <td class="py-1.5">
                  <input v-model="w.note" class="w-full border border-slate-300 rounded px-2 py-1 text-xs" placeholder="可选" />
                </td>
                <td class="py-1.5 text-right">
                  <button class="text-brand-600 hover:underline text-xs" @click="saveWeek(w)">保存</button>
                </td>
              </tr>
              <tr v-if="!calendar.length">
                <td colspan="6" class="py-6 text-center text-slate-400">尚未生成周历，点右上角「生成周历」</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- ============================ 专业 ============================ -->
    <div v-if="tab === 'major'" class="space-y-4">
      <div class="card p-4">
        <h2 class="font-medium text-slate-700 mb-3">专业</h2>
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200">
            <tr><th class="text-left py-2">编码</th><th class="text-left py-2">专业名称</th>
              <th class="text-left py-2">学院</th><th class="text-right py-2">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="m in majors" :key="m.id" class="border-b border-slate-100">
              <td class="py-2">{{ m.majorCode }}</td>
              <td class="py-2">{{ m.majorName }}</td>
              <td class="py-2 text-slate-500">{{ m.collegeName || '—' }}</td>
              <td class="py-2 text-right"><button class="text-brand-600 hover:underline" @click="editMajor(m)">编辑</button></td>
            </tr>
            <tr v-if="!majors.length"><td colspan="4" class="py-6 text-center text-slate-400">暂无专业</td></tr>
          </tbody>
        </table>
      </div>
      <div class="card p-4">
        <h2 class="font-medium text-slate-700 mb-3">{{ majorForm.id ? '编辑专业' : '新增专业' }}</h2>
        <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
          <label class="text-sm">编码<input v-model="majorForm.majorCode" class="input mt-1" /></label>
          <label class="text-sm">专业名称<input v-model="majorForm.majorName" class="input mt-1" /></label>
          <label class="text-sm">学院编码<input v-model="majorForm.collegeCode" class="input mt-1" /></label>
          <label class="text-sm">学院名称<input v-model="majorForm.collegeName" class="input mt-1" /></label>
          <div class="flex items-end gap-2">
            <button class="btn-primary text-sm" @click="saveMajor">保存</button>
            <button class="btn-ghost text-sm" @click="majorForm = { majorCode: '', majorName: '', collegeCode: '', collegeName: '' }">清空</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============================ 行政班 ============================ -->
    <div v-if="tab === 'class'" class="space-y-4">
      <div class="card p-4">
        <div class="flex items-center justify-between mb-3">
          <h2 class="font-medium text-slate-700">行政班</h2>
          <div class="flex items-center gap-2 text-sm">
            <select v-model="classFilter.majorId" class="border border-slate-300 rounded px-2 py-1" @change="loadClasses">
              <option :value="''">全部专业</option>
              <option v-for="m in majors" :key="m.id" :value="m.id">{{ m.majorName }}</option>
            </select>
            <input v-model.number="classFilter.gradeYear" type="number" placeholder="年级" class="w-24 border border-slate-300 rounded px-2 py-1" @change="loadClasses" />
          </div>
        </div>
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200">
            <tr><th class="text-left py-2">班级编码</th><th class="text-left py-2">班级名称</th>
              <th class="text-left py-2">专业</th><th class="text-right py-2">年级</th>
              <th class="text-right py-2">学生数</th><th class="text-right py-2">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in classes" :key="c.id" class="border-b border-slate-100">
              <td class="py-2">{{ c.classCode }}</td>
              <td class="py-2">{{ c.className }}</td>
              <td class="py-2 text-slate-500">{{ c.majorName || '—' }}</td>
              <td class="py-2 text-right">{{ c.gradeYear }}</td>
              <td class="py-2 text-right">{{ c.studentCount }}</td>
              <td class="py-2 text-right"><button class="text-brand-600 hover:underline" @click="editClass(c)">编辑</button></td>
            </tr>
            <tr v-if="!classes.length"><td colspan="6" class="py-6 text-center text-slate-400">暂无行政班</td></tr>
          </tbody>
        </table>
      </div>
      <div class="card p-4">
        <h2 class="font-medium text-slate-700 mb-3">{{ classForm.id ? '编辑行政班' : '新增行政班' }}</h2>
        <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
          <label class="text-sm">班级编码<input v-model="classForm.classCode" class="input mt-1" placeholder="CS-2026-1" /></label>
          <label class="text-sm">班级名称<input v-model="classForm.className" class="input mt-1" placeholder="计算机1班" /></label>
          <label class="text-sm">所属专业
            <select v-model="classForm.majorId" class="input mt-1">
              <option :value="''">请选择</option>
              <option v-for="m in majors" :key="m.id" :value="m.id">{{ m.majorName }}</option>
            </select>
          </label>
          <label class="text-sm">年级<input v-model.number="classForm.gradeYear" type="number" class="input mt-1" placeholder="2026" /></label>
          <div class="flex items-end gap-2">
            <button class="btn-primary text-sm" @click="saveClass">保存</button>
            <button class="btn-ghost text-sm" @click="resetClassForm">清空</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============================ 辅导员分配 ============================ -->
    <div v-if="tab === 'counselor'" class="space-y-4">
      <div class="card p-4">
        <h2 class="font-medium text-slate-700 mb-3">分配行政班给辅导员</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <label class="text-sm">学期
            <select v-model="assignForm.termId" class="input mt-1" @change="loadAssignments">
              <option :value="''">请选择</option>
              <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
            </select>
          </label>
          <label class="text-sm">辅导员
            <select v-model="assignForm.counselorId" class="input mt-1">
              <option :value="''">请选择</option>
              <option v-for="c in counselors" :key="c.id" :value="c.id">{{ c.counselorNo }} · {{ c.counselorName }}</option>
            </select>
          </label>
          <div class="flex items-end">
            <button class="btn-primary text-sm" @click="doAssign">分配所选班级</button>
          </div>
        </div>
        <div class="mt-3">
          <p class="text-xs text-slate-500 mb-2">勾选要分配给该辅导员的行政班（同班同教师只允许一条，重复选择会自动跳过）</p>
          <div class="flex flex-wrap gap-x-4 gap-y-2 max-h-40 overflow-y-auto border border-slate-200 rounded-lg p-3">
            <label v-for="c in classes" :key="c.id" class="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" :value="c.id" v-model="assignForm.adminClassIds" class="accent-brand-500" />
              {{ c.className }}（{{ c.gradeYear }}，{{ c.studentCount }} 人）
            </label>
          </div>
        </div>
      </div>

      <div class="card p-4">
        <h2 class="font-medium text-slate-700 mb-3">当前带班关系</h2>
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200">
            <tr><th class="text-left py-2">辅导员</th><th class="text-left py-2">工号</th>
              <th class="text-left py-2">行政班</th><th class="text-right py-2">年级</th>
              <th class="text-right py-2">学生数</th><th class="text-right py-2">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in assignments" :key="a.id" class="border-b border-slate-100">
              <td class="py-2">{{ a.counselorName }}</td>
              <td class="py-2 text-slate-500">{{ a.counselorNo }}</td>
              <td class="py-2">{{ a.className }}</td>
              <td class="py-2 text-right">{{ a.gradeYear }}</td>
              <td class="py-2 text-right">{{ a.studentCount }}</td>
              <td class="py-2 text-right">
                <button class="text-red-600 hover:underline" @click="doUnassign(a)">解除</button>
              </td>
            </tr>
            <tr v-if="!assignments.length"><td colspan="6" class="py-6 text-center text-slate-400">当前学期暂无带班关系</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api/admin'

const tabs = [
  { key: 'term', text: '学期与教学周历' },
  { key: 'major', text: '专业' },
  { key: 'class', text: '行政班' },
  { key: 'counselor', text: '辅导员分配' }
]
const tab = ref('term')
const message = ref('')
const ok = ref(true)

const terms = ref([])
const activeTerm = ref(null)
const calendar = ref([])
const majors = ref([])
const classes = ref([])
const counselors = ref([])
const assignments = ref([])

const termForm = reactive({ id: null, termCode: '', schoolYear: '', termNo: 1, termName: '', startDate: '', endDate: '', totalWeeks: 16 })
const majorForm = reactive({ id: null, majorCode: '', majorName: '', collegeCode: '', collegeName: '' })
const classForm = reactive({ id: null, classCode: '', className: '', majorId: '', gradeYear: new Date().getFullYear() })
const classFilter = reactive({ majorId: '', gradeYear: '' })
const assignForm = reactive({ termId: '', counselorId: '', adminClassIds: [] })

const activeTermName = computed(() => (activeTerm.value ? activeTerm.value.termName : ''))

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3000)
}

async function loadTerms() {
  terms.value = await adminApi.listTerms()
  const cur = terms.value.find(t => t.current)
  if (!activeTerm.value && cur) activeTerm.value = cur
  if (!activeTerm.value && terms.value.length) activeTerm.value = terms.value[0]
  if (activeTerm.value) await loadCalendar(activeTerm.value.id)
  if (!assignForm.termId && cur) assignForm.termId = cur.id
}

async function loadCalendar(termId) {
  calendar.value = await adminApi.listCalendar(termId)
}

async function loadMajors() { majors.value = await adminApi.listMajors() }
async function loadClasses() {
  classes.value = await adminApi.listClasses({
    majorId: classFilter.majorId || undefined,
    gradeYear: classFilter.gradeYear || undefined
  })
}
async function loadCounselors() { counselors.value = await adminApi.listCounselors() }
async function loadAssignments() {
  if (!assignForm.termId) { assignments.value = []; return }
  assignments.value = await adminApi.listAssignments({ termId: assignForm.termId })
}

function resetTermForm() {
  Object.assign(termForm, { id: null, termCode: '', schoolYear: '', termNo: 1, termName: '', startDate: '', endDate: '', totalWeeks: 16 })
}
function editTerm(t) {
  Object.assign(termForm, { ...t, termNo: t.termNo, totalWeeks: t.totalWeeks })
}
async function saveTerm() {
  try {
    await adminApi.saveTerm({ ...termForm, id: termForm.id || undefined })
    toast('学期已保存')
    resetTermForm()
    await loadTerms()
  } catch (e) { toast(e.message, false) }
}
async function setCurrent(t) {
  try { await adminApi.setCurrentTerm(t.id); toast('已设为当前学期'); await loadTerms() }
  catch (e) { toast(e.message, false) }
}
async function pickTerm(t) {
  activeTerm.value = t
  await loadCalendar(t.id)
}
async function genCalendar(force) {
  try {
    const n = await adminApi.generateCalendar(activeTerm.value.id, force)
    toast('已生成 ' + n + ' 周周历')
    await loadCalendar(activeTerm.value.id)
    await loadTerms()
  } catch (e) { toast(e.message, false) }
}
async function saveWeek(w) {
  try {
    await adminApi.updateWeek(w.id, { weekType: w.weekType, holidayDays: w.holidayDays, note: w.note })
    toast('第 ' + w.weekNo + ' 周已保存')
  } catch (e) { toast(e.message, false) }
}

function editMajor(m) { Object.assign(majorForm, m) }
async function saveMajor() {
  try {
    await adminApi.saveMajor({ ...majorForm, id: majorForm.id || undefined })
    toast('专业已保存')
    Object.assign(majorForm, { id: null, majorCode: '', majorName: '', collegeCode: '', collegeName: '' })
    await loadMajors()
  } catch (e) { toast(e.message, false) }
}

function resetClassForm() {
  Object.assign(classForm, { id: null, classCode: '', className: '', majorId: '', gradeYear: new Date().getFullYear() })
}
function editClass(c) {
  Object.assign(classForm, { id: c.id, classCode: c.classCode, className: c.className, majorId: c.majorId, gradeYear: c.gradeYear })
}
async function saveClass() {
  try {
    await adminApi.saveClass({ ...classForm, id: classForm.id || undefined, majorId: Number(classForm.majorId) })
    toast('行政班已保存')
    resetClassForm()
    await loadClasses()
  } catch (e) { toast(e.message, false) }
}

async function doAssign() {
  if (!assignForm.termId || !assignForm.counselorId || !assignForm.adminClassIds.length) {
    toast('请选择学期、辅导员与至少一个行政班', false)
    return
  }
  try {
    const n = await adminApi.assignCounselor({
      termId: Number(assignForm.termId),
      counselorId: Number(assignForm.counselorId),
      adminClassIds: assignForm.adminClassIds.map(Number)
    })
    toast('新增带班关系 ' + n + ' 条（重复的已自动跳过）')
    assignForm.adminClassIds = []
    await loadAssignments()
  } catch (e) { toast(e.message, false) }
}
async function doUnassign(a) {
  if (!confirm('确定解除 ' + a.counselorName + ' 对 ' + a.className + ' 的带班关系？')) return
  try { await adminApi.unassign(a.id); toast('已解除'); await loadAssignments() }
  catch (e) { toast(e.message, false) }
}

onMounted(async () => {
  try {
    await loadTerms()
    await loadMajors()
    await loadClasses()
    await loadCounselors()
    await loadAssignments()
  } catch (e) { toast(e.message, false) }
})
</script>
