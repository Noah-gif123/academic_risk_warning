<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-lg font-semibold text-slate-800">排课与教学班</h1>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <!-- 学期 + 新增开课 -->
    <div class="card p-4">
      <div class="grid grid-cols-1 md:grid-cols-6 gap-3">
        <label class="text-sm">学期
          <select v-model="termId" class="input mt-1" @change="loadOfferings">
            <option :value="''">全部学期</option>
            <option v-for="t in terms" :key="t.id" :value="t.id">{{ t.termName }}</option>
          </select>
        </label>
        <label class="text-sm">课程
          <select v-model="form.courseId" class="input mt-1">
            <option :value="''">请选择</option>
            <option v-for="c in courses" :key="c.id" :value="c.id">{{ c.courseName }}（{{ courseTypeLabel(c.courseType) }}）</option>
          </select>
        </label>
        <label class="text-sm">计划周数
          <input v-model.number="form.plannedWeeks" type="number" class="input mt-1" />
        </label>
        <label class="text-sm">每周课次
          <input v-model.number="form.plannedTimesPerWeek" type="number" class="input mt-1" />
        </label>
        <label class="text-sm">及格线
          <input v-model.number="form.passScore" type="number" class="input mt-1" />
        </label>
        <div class="flex items-end gap-2">
          <button class="btn-primary text-sm" @click="createOffering">新增开课</button>
        </div>
      </div>
      <p class="text-xs text-slate-400 mt-2">计划课次 = 计划周数 × 每周课次；确认后的「有效课次」是缺课门槛（1/3）与覆盖率的分母。</p>
    </div>

    <!-- 开课实例列表 -->
    <div class="card p-4">
      <h2 class="font-medium text-slate-700 mb-3">开课实例</h2>
      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">课程</th><th class="text-left py-2">学期</th>
            <th class="text-right py-2">计划课次</th><th class="text-right py-2">有效课次</th>
            <th class="text-right py-2">教学班</th><th class="text-right py-2">选课人数</th>
            <th class="text-right py-2">已生成课次</th><th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in offerings" :key="o.id" class="border-b border-slate-100"
              :class="active && active.id === o.id ? 'bg-brand-50/40' : ''">
            <td class="py-2">{{ o.courseName }} <span class="tag bg-slate-100 text-slate-500 ml-1">{{ courseTypeLabel(o.courseType) }}</span></td>
            <td class="py-2 text-slate-500">{{ o.termName }}</td>
            <td class="py-2 text-right">{{ o.plannedSessions }}</td>
            <td class="py-2 text-right">
              <span :class="o.effectiveSessions ? 'text-emerald-600 font-medium' : 'text-slate-400'">
                {{ o.effectiveSessions || '未确认' }}
              </span>
            </td>
            <td class="py-2 text-right">{{ o.classCount }}</td>
            <td class="py-2 text-right">{{ o.enrollCount }}</td>
            <td class="py-2 text-right">{{ o.sessionCount }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline mr-3" @click="selectOffering(o)">教学班 / 课次</button>
              <span v-if="o.actualSubmitStatus === 'CONFIRMED'" class="text-slate-400" title="有效课次已锁定">
                已确认并锁定
              </span>
              <button v-else class="text-emerald-600 hover:underline" @click="doConfirm(o)">确认有效课次</button>
            </td>
          </tr>
          <tr v-if="!offerings.length"><td colspan="8" class="py-6 text-center text-slate-400">暂无开课实例</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 选中开课：教学班 -->
    <div class="card p-4" v-if="active">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">教学班 · {{ active.courseName }}</h2>
        <span class="text-xs text-slate-400">一个教学班只有一位任课教师</span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-6 gap-3 mb-4">
        <label class="text-sm">任课教师
          <select v-model="classForm.teacherId" class="input mt-1">
            <option :value="''">请选择</option>
            <option v-for="t in teachers" :key="t.id" :value="t.id">{{ t.teacherNo }} · {{ t.teacherName }}</option>
          </select>
        </label>
        <label class="text-sm">班级编码<input v-model="classForm.classCode" class="input mt-1" placeholder="CS-01" /></label>
        <label class="text-sm">班级名称<input v-model="classForm.className" class="input mt-1" placeholder="计算机1班" /></label>
        <label class="text-sm">容量<input v-model.number="classForm.capacity" type="number" class="input mt-1" /></label>
        <label class="text-sm">重修班
          <select v-model.number="classForm.isRetakeClass" class="input mt-1">
            <option :value="0">否</option><option :value="1">是</option>
          </select>
        </label>
        <div class="flex items-end"><button class="btn-primary text-sm" @click="createClass">新增教学班</button></div>
      </div>

      <div class="mb-3">
        <p class="text-xs text-slate-500 mb-1">关联行政班（新增教学班时一并提交；用于「按行政班一键选课」）</p>
        <div class="flex flex-wrap gap-x-4 gap-y-1 border border-slate-200 rounded-lg p-2 max-h-28 overflow-y-auto">
          <label v-for="c in adminClasses" :key="c.id" class="flex items-center gap-2 text-sm">
            <input type="checkbox" :value="c.id" v-model="classForm.adminClassIds" class="accent-brand-500" />
            {{ c.className }}（{{ c.gradeYear }}，{{ c.studentCount }} 人）
          </label>
        </div>
      </div>

      <table class="w-full text-sm">
        <thead class="text-slate-500 border-b border-slate-200">
          <tr>
            <th class="text-left py-2">班级编码</th><th class="text-left py-2">班级名称</th>
            <th class="text-left py-2">教师</th><th class="text-left py-2">关联行政班</th>
            <th class="text-right py-2">学生数</th><th class="text-right py-2">课次数</th>
            <th class="text-right py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in teachingClasses" :key="c.id" class="border-b border-slate-100">
            <td class="py-2">{{ c.classCode }}</td>
            <td class="py-2">{{ c.className }}</td>
            <td class="py-2 text-slate-500">{{ c.teacherNo }} · {{ c.teacherName }}</td>
            <td class="py-2 text-slate-500">{{ c.adminClasses || '—' }}</td>
            <td class="py-2 text-right">{{ c.studentCount }}</td>
            <td class="py-2 text-right">{{ c.sessionCount }}</td>
            <td class="py-2 text-right whitespace-nowrap">
              <button class="text-brand-600 hover:underline mr-3" @click="doEnroll(c)">按行政班选课</button>
              <button class="text-brand-600 hover:underline mr-3" @click="doGenerate(c)">生成课次</button>
              <button class="text-slate-500 hover:underline mr-3" @click="showStudents(c)">学生</button>
              <button class="text-slate-500 hover:underline" @click="showSessions(c)">课次</button>
            </td>
          </tr>
          <tr v-if="!teachingClasses.length"><td colspan="7" class="py-6 text-center text-slate-400">暂无教学班</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 学生名单 / 课次 -->
    <div class="card p-4" v-if="panelTitle">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">{{ panelTitle }}</h2>
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="panelTitle = ''; panelRows = []">收起</button>
      </div>
      <div class="max-h-72 overflow-y-auto">
        <table class="w-full text-sm">
          <thead class="text-slate-500 border-b border-slate-200 sticky top-0 bg-white">
            <tr><th v-for="h in panelHead" :key="h" class="text-left py-2">{{ h }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="(r, i) in panelRows" :key="i" class="border-b border-slate-100">
              <td v-for="(v, j) in r" :key="j" class="py-1.5">{{ v }}</td>
            </tr>
            <tr v-if="!panelRows.length"><td :colspan="panelHead.length" class="py-6 text-center text-slate-400">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api/admin'
import { courseTypeLabel } from '../../labels'

const message = ref('')
const ok = ref(true)
const terms = ref([])
const courses = ref([])
const teachers = ref([])
const adminClasses = ref([])
const offerings = ref([])
const teachingClasses = ref([])
const active = ref(null)
const termId = ref('')
const panelTitle = ref('')
const panelHead = ref([])
const panelRows = ref([])

const form = reactive({ courseId: '', plannedWeeks: 16, plannedTimesPerWeek: 2, passScore: 60 })
const classForm = reactive({ teacherId: '', classCode: '', className: '', capacity: 60, isRetakeClass: 0, adminClassIds: [] })

function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

async function loadOfferings() {
  offerings.value = await adminApi.listOfferings(termId.value || undefined)
}
async function selectOffering(o) {
  active.value = o
  teachingClasses.value = await adminApi.listTeachingClasses(o.id)
  panelTitle.value = ''
}
async function createOffering() {
  if (!form.courseId) { toast('请选择课程', false); return }
  if (!termId.value) { toast('请先在上方选择学期（新增开课必须指定学期）', false); return }
  try {
    await adminApi.saveOffering({
      courseId: Number(form.courseId),
      termId: Number(termId.value),
      plannedWeeks: form.plannedWeeks,
      plannedTimesPerWeek: form.plannedTimesPerWeek,
      passScore: form.passScore
    })
    toast('开课实例已创建')
    await loadOfferings()
  } catch (e) { toast(e.message, false) }
}
async function doConfirm(o) {
  const ok = window.confirm(
    '确认「' + o.courseName + '」的有效课次为 ' + (o.actualSessions || o.plannedSessions) + '？\n\n'
    + '确认后该开课实例的有效课次将锁定，作为 1/3 缺课门槛与点名覆盖率的分母，不能再重复确认。'
  )
  if (!ok) return
  try { await adminApi.confirmActual(o.id); toast('已确认有效课次并锁定'); await loadOfferings() }
  catch (e) { toast(e.message, false) }
}
async function createClass() {
  if (!classForm.teacherId) { toast('请选择任课教师', false); return }
  try {
    await adminApi.saveTeachingClass({
      offeringId: active.value.id,
      teacherId: Number(classForm.teacherId),
      classCode: classForm.classCode,
      className: classForm.className,
      capacity: classForm.capacity,
      isRetakeClass: classForm.isRetakeClass,
      adminClassIds: classForm.adminClassIds.map(Number)
    })
    toast('教学班已创建')
    classForm.classCode = ''
    classForm.className = ''
    classForm.adminClassIds = []
    await selectOffering(active.value)
    await loadOfferings()
  } catch (e) { toast(e.message, false) }
}
async function doEnroll(c) {
  try {
    const n = await adminApi.enrollByAdminClasses(c.id)
    toast('按行政班选课完成，新增 ' + n + ' 人')
    await selectOffering(active.value)
    await loadOfferings()
  } catch (e) { toast(e.message, false) }
}
async function doGenerate(c) {
  try {
    const n = await adminApi.generateSessions(active.value.id, c.id)
    toast('已生成 ' + n + ' 次课（跳过假期与考试周）')
    await selectOffering(active.value)
    await loadOfferings()
  } catch (e) { toast(e.message, false) }
}
async function showStudents(c) {
  const rows = await adminApi.listClassStudents(c.id)
  panelTitle.value = '学生名单 · ' + c.classCode + '（' + rows.length + ' 人）'
  panelHead.value = ['学号', '姓名', '状态']
  panelRows.value = rows.map(r => [r.studentNo, r.studentName, r.status])
}
async function showSessions(c) {
  const rows = await adminApi.listSessions(active.value.id, c.id)
  panelTitle.value = '课次 · ' + c.classCode + '（' + rows.length + ' 次）'
  panelHead.value = ['次序', '周次', '日期', '点名状态']
  panelRows.value = rows.map(r => [r.sessionNo, '第 ' + r.weekNo + ' 周', r.sessionDate, r.rollCallStatus])
}

onMounted(async () => {
  try {
    terms.value = await adminApi.listTerms()
    const cur = terms.value.find(t => t.current)
    if (cur) termId.value = cur.id
    courses.value = await adminApi.listCourses()
    teachers.value = await adminApi.listTeachers()
    adminClasses.value = await adminApi.listClasses()
    await loadOfferings()
  } catch (e) { toast(e.message, false) }
})
</script>
