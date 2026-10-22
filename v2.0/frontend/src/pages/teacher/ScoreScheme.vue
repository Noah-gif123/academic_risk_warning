<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between flex-wrap gap-2">
      <div class="flex items-center gap-3 flex-wrap">
        <button class="text-sm text-slate-400 hover:text-slate-600" @click="back">← 返回课程</button>
        <h1 class="text-lg font-semibold text-slate-800">成绩构成</h1>
        <span v-if="scheme" class="tag bg-brand-50 text-brand-600">{{ scheme.courseName }}</span>
        <span v-if="scheme" class="tag bg-slate-100 text-slate-500">{{ scheme.termName }}</span>
        <span v-if="scheme" class="tag bg-slate-100 text-slate-500">{{ courseTypeLabel(scheme.courseType) }} · {{ scheme.level }}</span>
      </div>
      <p v-if="message" class="text-sm" :class="ok ? 'text-emerald-600' : 'text-red-600'">{{ message }}</p>
    </div>

    <p class="text-xs text-slate-400">
      成绩构成是课程的「总开关」：这里定义的评分项决定后续可以发布哪些活动、学生端显示哪些页签，也决定成绩册的列。
      同一层级权重之和必须为 100%；考勤（出勤）不作为评分项，只通过缺课硬门槛与违纪扣分影响结果。
    </p>

    <!-- 状态条 -->
    <div class="card p-4" v-if="scheme">
      <div class="grid grid-cols-2 md:grid-cols-5 gap-4 text-sm">
        <div><p class="text-xs text-slate-400">版本</p><p class="font-medium">v{{ scheme.version }}</p></div>
        <div><p class="text-xs text-slate-400">根层权重合计</p>
          <p class="font-medium" :class="rootSum === 100 ? 'text-emerald-600' : 'text-red-600'">{{ rootSum }}%</p></div>
        <div><p class="text-xs text-slate-400">课程组确认</p><p class="font-medium">{{ scheme.confirmDone }} / {{ scheme.teacherCount }}</p></div>
        <div><p class="text-xs text-slate-400">状态</p>
          <p class="font-medium" :class="scheme.locked ? 'text-emerald-600' : 'text-amber-600'">{{ statusText }}</p></div>
        <div><p class="text-xs text-slate-400">我的确认</p>
          <p class="font-medium">{{ scheme.myConfirmed ? '已确认' : '未确认' }}</p></div>
      </div>
      <p class="text-xs text-slate-400 mt-3">
        锁定方式：{{ lockText }}<span v-if="scheme.lockedByName"> · {{ scheme.lockedByName }}</span>
        <span v-if="scheme.lockedAt"> · {{ scheme.lockedAt }}</span>
        <span v-if="scheme.remark"> ｜ 备注：{{ scheme.remark }}</span>
      </p>
    </div>

    <!-- 还没有构成：套用模板 -->
    <div class="card p-4" v-if="!scheme && !loading">
      <h2 class="font-medium text-slate-700 mb-2">本课程还没有成绩构成</h2>
      <p class="text-xs text-slate-500 mb-3">可以套用模板后微调，也可以从空白开始自定义（推荐先套模板）。</p>
      <div class="flex flex-wrap items-end gap-3">
        <label class="text-sm">模板
          <select v-model="tplId" class="input mt-1 w-64">
            <option :value="''">请选择模板</option>
            <option v-for="t in templates" :key="t.id" :value="t.id">
              {{ t.templateName }}（{{ courseTypeLabel(t.courseType) }} · {{ t.level }}）
            </option>
          </select>
        </label>
        <button class="btn-primary text-sm" @click="applyTemplate">套用模板</button>
        <button class="btn-ghost text-sm" @click="blankScheme">从空白开始</button>
      </div>
    </div>

    <!-- 编辑 / 只读 -->
    <div class="card p-4" v-if="items.length">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-medium text-slate-700">{{ scheme && scheme.locked ? '成绩构成（已锁定，只读）' : '成绩构成（编辑中）' }}</h2>
        <span class="text-xs" :class="rootSum === 100 ? 'text-emerald-600' : 'text-red-600'">
          一级项权重合计 {{ rootSum }} / 100
        </span>
      </div>

      <div class="space-y-3">
        <div v-for="(it, i) in items" :key="i" class="border border-slate-200 rounded-lg p-3">
          <div class="flex flex-wrap items-center gap-2">
            <span class="text-xs text-slate-400 w-8">{{ i + 1 }}</span>
            <input v-model="it.itemName" class="input w-40" placeholder="评分项名称" :disabled="readonly" />
            <select v-model="it.category" class="input w-32" :disabled="readonly">
              <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
            <input v-model.number="it.weight" type="number" min="0" max="100" class="input w-24" :disabled="readonly" />
            <span class="text-sm text-slate-500">%</span>
            <select v-model="it.dataSource" class="input w-32" :disabled="readonly">
              <option value="AUTO">自动汇总</option>
              <option value="MANUAL">手工录入</option>
            </select>
            <label class="flex items-center gap-1 text-xs text-slate-500">
              <input type="checkbox" :checked="it.isAnchor === 1" :disabled="readonly"
                     @change="it.isAnchor = $event.target.checked ? 1 : 0" /> 锚点项
            </label>
            <span class="text-xs" :class="childSum(it) === 100 || !it.children.length ? 'text-emerald-600' : 'text-red-600'">
              {{ it.children.length ? ('二级项合计 ' + childSum(it) + ' / 100') : '（叶子项）' }}
            </span>
            <button v-if="!readonly" class="text-xs text-slate-400 hover:text-red-500 ml-auto"
                    @click="removeItem(i)">删除</button>
          </div>

          <!-- 二级项 -->
          <div v-if="it.children.length" class="mt-2 ml-10 space-y-2">
            <div v-for="(c, j) in it.children" :key="j" class="flex flex-wrap items-center gap-2">
              <span class="text-xs text-slate-400 w-6">↳</span>
              <input v-model="c.itemName" class="input w-40" placeholder="子项名称" :disabled="readonly" />
              <select v-model="c.category" class="input w-32" :disabled="readonly">
                <option v-for="cat in categories" :key="cat.value" :value="cat.value">{{ cat.label }}</option>
              </select>
              <input v-model.number="c.weight" type="number" min="0" max="100" class="input w-24" :disabled="readonly" />
              <span class="text-sm text-slate-500">%</span>
              <select v-model="c.dataSource" class="input w-32" :disabled="readonly">
                <option value="AUTO">自动汇总</option>
                <option value="MANUAL">手工录入</option>
              </select>
              <input v-model.number="c.expectedTimes" type="number" min="1" class="input w-24" :disabled="readonly"
                     :title="'预计次数（如作业 9 次）'" />
              <label class="flex items-center gap-1 text-xs text-slate-500">
                <input type="checkbox" :checked="c.isSubjective === 1" :disabled="readonly"
                       @change="c.isSubjective = $event.target.checked ? 1 : 0" /> 主观题
              </label>
              <button v-if="!readonly" class="text-xs text-slate-400 hover:text-red-500"
                      @click="removeChild(i, j)">删除</button>
            </div>
          </div>

          <button v-if="!readonly" class="text-xs text-brand-600 hover:underline mt-2 ml-10"
                  @click="addChild(it)">+ 添加二级项</button>
        </div>
      </div>

      <button v-if="!readonly" class="text-xs text-brand-600 hover:underline mt-3" @click="addItem">+ 添加一级评分项</button>

      <!-- 操作 -->
      <div class="flex flex-wrap items-center gap-2 mt-4 pt-3 border-t border-slate-100" v-if="!readonly">
        <button class="btn-primary text-sm" @click="save">保存草稿</button>
        <button class="btn-ghost text-sm" :disabled="!scheme || scheme.locked"
                @click="doConfirm">我确认此构成</button>
        <button class="btn-ghost text-sm" :disabled="!scheme || scheme.locked"
                @click="doLock">锁定（全体确认后）</button>
        <span class="text-xs text-slate-400">
          任一教师都可发起；课程组 {{ scheme ? scheme.teacherCount : '—' }} 人全部确认后可锁定，若无法达成一致请联系教务裁定。
        </span>
      </div>
      <p v-else class="text-xs text-slate-400 mt-4 pt-3 border-t border-slate-100">
        成绩构成已锁定，本学期内不可修改；如需调整请联系教务。
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { scoreApi } from '../../api/score'
import { courseTypeLabel } from '../../labels'

const route = useRoute()
const router = useRouter()
const offeringId = Number(route.query.offeringId || 0)

const scheme = ref(null)
const templates = ref([])
const items = ref([])
const tplId = ref('')
const loading = ref(true)
const message = ref('')
const ok = ref(true)

const categories = [
  { value: 'USUAL', label: '平时' },
  { value: 'HOMEWORK', label: '作业' },
  { value: 'QUIZ', label: '测验' },
  { value: 'PERFORMANCE', label: '课堂表现' },
  { value: 'EXPERIMENT', label: '实验' },
  { value: 'PROJECT', label: '项目' },
  { value: 'MIDTERM', label: '期中' },
  { value: 'FINAL', label: '期末' },
  { value: 'OTHER', label: '其他' }
]

const readonly = computed(() => !!(scheme.value && scheme.value.locked))
const rootSum = computed(() => round(items.value.reduce((s, it) => s + Number(it.weight || 0), 0)))
const statusText = computed(() => {
  const m = { DRAFT: '草稿', CONFIRMING: '确认中', CONFIRMED: '已确认', LOCKED: '已锁定' }
  return m[scheme.value?.confirmStatus] || scheme.value?.confirmStatus || '—'
})
const lockText = computed(() => {
  const m = { TEACHER_COLLECTIVE: '课程组全体确认后锁定', ADMIN_RULED: '教务裁定锁定', TEMPLATE_AUTO: '模板默认' }
  return m[scheme.value?.lockType] || '未锁定'
})

function round(n) { return Math.round(n * 100) / 100 }
function childSum(it) {
  return round((it.children || []).reduce((s, c) => s + Number(c.weight || 0), 0))
}
function toast(msg, isOk = true) {
  message.value = msg
  ok.value = isOk
  setTimeout(() => { if (message.value === msg) message.value = '' }, 3500)
}

function fromTree(list) {
  return (list || []).map((r) => ({
    itemCode: r.itemCode, itemName: r.itemName, category: r.category || 'OTHER',
    weight: Number(r.weight || 0), dataSource: r.dataSource || 'MANUAL',
    scoreType: r.scoreType || 'NUMERIC', isAnchor: r.isAnchor || 0, anchorLayer: r.anchorLayer || null,
    children: (r.children || []).map((c) => ({
      itemCode: c.itemCode, itemName: c.itemName, category: c.category || 'OTHER',
      weight: Number(c.weight || 0), dataSource: c.dataSource || 'MANUAL',
      scoreType: c.scoreType || 'NUMERIC', aggregateMode: c.aggregateMode || null,
      expectedTimes: c.expectedTimes || null, isSubjective: c.isSubjective || 0,
      scorerRole: c.scorerRole || 'TEACHER'
    }))
  }))
}

/** templates.items_json 用 code/name 作键，其余字段与后端 DTO 同名 */
const CODE_CATEGORY = {
  USUAL: 'USUAL', HW: 'HOMEWORK', QUIZ: 'QUIZ', PERF: 'PERFORMANCE', EXP: 'EXPERIMENT',
  PROJ: 'PROJECT', MID: 'MIDTERM', MID_EXAM: 'MIDTERM', FINAL: 'FINAL', FINAL_EXAM: 'FINAL'
}
function fromTemplateJson(json) {
  const list = JSON.parse(json || '[]')
  const map = (o) => ({
    itemCode: o.code || o.itemCode,
    itemName: o.name || o.itemName,
    category: o.category || CODE_CATEGORY[o.code] || 'OTHER',
    weight: Number(o.weight || 0),
    dataSource: o.dataSource || 'MANUAL',
    scoreType: o.scoreType || 'NUMERIC',
    aggregateMode: o.aggregateMode || null,
    expectedTimes: o.expectedTimes || null,
    isAnchor: o.isAnchor ? 1 : 0,
    anchorLayer: o.anchorLayer || null,
    isSubjective: o.isSubjective ? 1 : 0,
    scorerRole: o.scorerRole || 'TEACHER',
    children: (o.children || []).map(map)
  })
  return list.map(map)
}

async function load() {
  loading.value = true
  try {
    scheme.value = await scoreApi.scheme(offeringId)
    const type = scheme.value?.courseType || ''
    templates.value = await scoreApi.templates(type || undefined)
    if (!tplId.value && templates.value.length) {
      const dft = templates.value.find((t) => t.is_default === 1 || t.isDefault === 1)
      tplId.value = (dft || templates.value[0]).id
    }
    items.value = scheme.value ? fromTree(scheme.value.items) : []
  } catch (e) { toast(e.message, false) }
  loading.value = false
}

function applyTemplate() {
  const t = templates.value.find((x) => x.id === Number(tplId.value))
  if (!t) { toast('请先选择模板', false); return }
  items.value = fromTemplateJson(t.itemsJson)
  toast('已套用「' + t.templateName + '」，可继续微调后保存')
}
function blankScheme() {
  items.value = [
    { itemName: '平时', category: 'USUAL', weight: 60, dataSource: 'AUTO', isAnchor: 0, children: [] },
    { itemName: '期末', category: 'FINAL', weight: 40, dataSource: 'MANUAL', isAnchor: 0, children: [] }
  ]
  toast('已放入默认两行，请补充二级项与权重')
}
function addItem() {
  items.value.push({ itemName: '', category: 'OTHER', weight: 0, dataSource: 'MANUAL', isAnchor: 0, children: [] })
}
function removeItem(i) { items.value.splice(i, 1) }
function addChild(it) {
  it.children.push({ itemName: '', category: 'OTHER', weight: 0, dataSource: 'MANUAL', expectedTimes: 1, isSubjective: 0, scorerRole: 'TEACHER' })
}
function removeChild(i, j) { items.value[i].children.splice(j, 1) }

function payload() {
  return {
    offeringId,
    level: scheme.value?.level || 'L2',
    sourceTemplateId: tplId.value ? Number(tplId.value) : null,
    remark: scheme.value?.remark || null,
    items: items.value.map((it) => ({
      itemCode: it.itemCode || null,
      itemName: it.itemName,
      category: it.category,
      weight: Number(it.weight || 0),
      fullScore: 100,
      dataSource: it.dataSource,
      scoreType: it.scoreType || 'NUMERIC',
      isAnchor: it.isAnchor || 0,
      anchorLayer: it.anchorLayer || null,
      children: (it.children || []).map((c) => ({
        itemCode: c.itemCode || null,
        itemName: c.itemName,
        category: c.category,
        weight: Number(c.weight || 0),
        fullScore: 100,
        dataSource: c.dataSource,
        scoreType: c.scoreType || 'NUMERIC',
        aggregateMode: c.aggregateMode || null,
        expectedTimes: c.expectedTimes || null,
        isSubjective: c.isSubjective || 0,
        scorerRole: c.scorerRole || 'TEACHER'
      }))
    }))
  }
}

async function save() {
  if (!items.value.length) { toast('至少需要一个评分项', false); return }
  try {
    await scoreApi.saveScheme(payload())
    toast('已保存草稿')
    await load()
  } catch (e) { toast(e.message, false) }
}
async function doConfirm() {
  if (!scheme.value) { toast('请先保存草稿后再确认', false); return }
  try {
    scheme.value = await scoreApi.confirm(scheme.value.id)
    toast('已记录你的确认')
  } catch (e) { toast(e.message, false) }
}
async function doLock() {
  if (!scheme.value) { toast('请先保存草稿', false); return }
  try {
    scheme.value = await scoreApi.lock(scheme.value.id)
    items.value = fromTree(scheme.value.items)
    toast('成绩构成已锁定')
  } catch (e) { toast(e.message, false) }
}
function back() { router.push('/teacher/home') }

onMounted(load)
</script>
