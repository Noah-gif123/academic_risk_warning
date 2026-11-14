<template>
  <div class="space-y-5">
    <header>
      <h1>学情报告</h1>
      <p class="text-xs text-slate-500 mt-2">一个入口，按页签选择报告范围；生成由 Agent 编排完成，结果读取真实快照。</p>
    </header>

    <div class="flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.key" class="px-3 py-1.5 rounded-lg text-sm"
              :class="tab === t.key ? 'bg-[#0f9d8a] text-white shadow-sm' : 'bg-white text-slate-600 border border-[#e6edf5]'"
              @click="tab = t.key">{{ t.label }}</button>
    </div>

    <p v-if="message" class="text-sm text-red-600">{{ message }}</p>

    <section v-if="tab === 'class'" class="card p-5 space-y-4">
      <div class="flex items-end justify-between flex-wrap gap-3">
        <label class="text-sm">教学班
          <select v-model="classId" class="input mt-1 w-80">
            <option :value="0">选择教学班</option>
            <option v-for="c in classOptions" :key="c.teachingClassId" :value="c.teachingClassId">{{ c.courseName }} · {{ c.className }}</option>
          </select>
        </label>
        <button class="btn-primary text-sm" :disabled="!classId" @click="reportClass()">生成班级报告</button>
      </div>
      <p class="text-xs text-slate-400">范围：一个教学班 × 一门课，报告格式与学生个人报告一致。</p>
    </section>

    <section v-if="tab === 'offering'" class="card p-5 space-y-4">
      <div class="flex items-end justify-between flex-wrap gap-3">
        <label class="text-sm">课程（我教的全部教学班）
          <select v-model="offeringId" class="input mt-1 w-80">
            <option :value="0">选择课程</option>
            <option v-for="c in offeringOptions" :key="c.offeringId" :value="c.offeringId">{{ c.courseName }} · {{ c.termName }}</option>
          </select>
        </label>
        <button class="btn-primary text-sm" :disabled="!offeringId" @click="reportOffering()">生成课程总体报告</button>
      </div>
      <p class="text-xs text-slate-400">范围：一门课下我教的全部教学班汇总。</p>
    </section>

    <section v-if="tab === 'student'" class="card p-5 space-y-4">
      <div class="flex items-end justify-between flex-wrap gap-3">
        <label class="text-sm">教学班
          <select v-model="studentClassId" class="input mt-1 w-72" @change="loadStudents">
            <option :value="0">选择教学班</option>
            <option v-for="c in classOptions" :key="c.teachingClassId" :value="c.teachingClassId">{{ c.courseName }} · {{ c.className }}</option>
          </select>
        </label>
        <label class="text-sm">学生
          <select v-model="studentId" class="input mt-1 w-72">
            <option :value="0">选择学生</option>
            <option v-for="s in students" :key="s.studentId + '-' + s.teachingClassId" :value="s.studentId">{{ s.studentNo }} · {{ s.studentName }}</option>
          </select>
        </label>
        <button class="btn-primary text-sm" :disabled="!studentId" @click="reportStudent()">查看/生成学生报告</button>
      </div>
      <p class="text-xs text-slate-400">范围：所选教学班内某位学生的课程个人报告；已有报告直接查看，没有时可生成。</p>
    </section>

    <GroupReportModal :open="groupOpen" :scope="groupScope" :target-id="groupTarget"
                      :title="groupTitle" @close="groupOpen = false" />
    <StudentReportModal :open="studentOpen" :student="studentMeta" :courses="studentCourses"
                        mode="teacher" @close="studentOpen = false" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { teacherPortalApi } from '../../api/teacherPortal'
import { teacherAlertApi } from '../../api/alert'
import GroupReportModal from '../../components/GroupReportModal.vue'
import StudentReportModal from '../../components/StudentReportModal.vue'

const tabs = [
  { key: 'class', label: '班级报告' },
  { key: 'offering', label: '课程总体报告' },
  { key: 'student', label: '学生个人报告' }
]
const tab = ref('class')
const courses = ref([])
const classId = ref(0)
const offeringId = ref(0)
const studentClassId = ref(0)
const studentId = ref(0)
const students = ref([])
const message = ref('')
const groupOpen = ref(false)
const groupScope = ref('CLASS')
const groupTarget = ref(0)
const groupTitle = ref('')
const studentOpen = ref(false)
const studentMeta = ref({})
const studentCourses = ref([])

const classOptions = computed(() => courses.value.flatMap(c => (c.classes || []).map(x => ({
  teachingClassId: x.teachingClassId, className: x.className || x.classCode || '教学班', courseName: c.courseName
}))))
const offeringOptions = computed(() => courses.value.map(c => ({
  offeringId: c.offeringId, courseName: c.courseName, termName: c.termName
})))

function reportClass() {
  const c = classOptions.value.find(x => x.teachingClassId === classId.value)
  if (!c) return
  groupScope.value = 'CLASS'; groupTarget.value = classId.value; groupTitle.value = `${c.courseName} · ${c.className} · 班级学情报告`; groupOpen.value = true
}
function reportOffering() {
  const c = offeringOptions.value.find(x => x.offeringId === offeringId.value)
  if (!c) return
  groupScope.value = 'OFFERING'; groupTarget.value = offeringId.value; groupTitle.value = `${c.courseName} · 课程总体学情报告`; groupOpen.value = true
}
async function loadStudents() {
  students.value = []
  studentId.value = 0
  if (!studentClassId.value) return
  try {
    const rows = await teacherAlertApi.list({ teachingClassId: studentClassId.value })
    const seen = new Set()
    students.value = (rows || []).filter(r => {
      const k = String(r.studentId)
      if (seen.has(k)) return false
      seen.add(k); return true
    })
  } catch (e) { message.value = e?.message || '学生名单加载失败' }
}
function reportStudent() {
  const s = students.value.find(x => x.studentId === studentId.value)
  if (!s) return
  const c = classOptions.value.find(x => x.teachingClassId === s.teachingClassId) || {}
  studentMeta.value = { studentName: s.studentName, studentNo: s.studentNo, adminClassName: s.classCode, riskScore: s.riskScore }
  studentCourses.value = [{ teachingClassId: s.teachingClassId, courseName: s.courseName || c.courseName, teacherName: s.teacherName || '', riskLevel: s.alertLevel, riskHint: s.alertReason, absentCount: s.absentCount, allowedAbsent: s.allowedAbsent, hardFail: s.hardFail, totalScore: s.totalScore, processScore: s.channelBScore }]
  studentOpen.value = true
}
onMounted(async () => {
  try {
    const h = await teacherPortalApi.home()
    courses.value = h.courses || []
    const first = classOptions.value[0]
    if (first) { classId.value = first.teachingClassId; studentClassId.value = first.teachingClassId }
    const off = offeringOptions.value[0]
    if (off) offeringId.value = off.offeringId
  } catch (e) { message.value = e?.message || '课程数据加载失败' }
})
</script>
