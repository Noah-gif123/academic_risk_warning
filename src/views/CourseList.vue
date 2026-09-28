<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>课程列表</span>
        <div>
          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            :disabled="selectedRows.length === 0"
            @click="batchRemove"
          >
            批量删除<span v-if="selectedRows.length">（{{ selectedRows.length }}）</span>
          </el-button>
          <el-button type="primary" size="small" :icon="Plus" @click="editRef.open()">新增课程</el-button>
        </div>
      </div>
    </template>

    <CourseSearch ref="searchRef" @search="handleSearch" />

    <el-table
      ref="tableRef"
      :data="tableData"
      border
      stripe
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="courseid" label="编号" width="70" />
      <el-table-column prop="coursename" label="课程名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="teacher" label="授课教师" width="110" />
      <el-table-column prop="coursetime" label="上课时间" width="130" />
      <el-table-column prop="classroom" label="上课地点" width="130" />
      <el-table-column prop="description" label="课程简介" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" :icon="Edit" circle title="修改" @click="editRef.open(row)" />
          <el-popconfirm title="确定删除该课程吗？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger" :icon="Delete" circle title="删除" />
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :current-page="query.pageNo"
      :page-size="query.pageSize"
      :page-sizes="[5, 10, 20]"
      :total="total"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <CourseEdit ref="editRef" @refresh="handleRefresh" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Plus } from '@element-plus/icons-vue'
import request from '../utils/request'
import CourseSearch from '../components/CourseSearch.vue'
import CourseEdit from '../components/CourseEdit.vue'

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const editRef = ref()
const tableRef = ref()
const searchRef = ref()

// 表格多选结果，用于批量删除
const selectedRows = ref([])

// 查询条件与分页参数一起提交给后端
const query = reactive({ coursename: '', teacher: '', credit: '', pageNo: 1, pageSize: 5 })

const load = async () => {
  loading.value = true
  try {
    const data = await request.get('/course/getCourseList', { params: query })
    tableData.value = data.courseList || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = condition => {
  query.coursename = condition.coursename
  query.teacher = condition.teacher
  query.credit = condition.credit
  query.pageNo = 1
  load()
}

const handleSelectionChange = rows => {
  selectedRows.value = rows
}

const handlePageChange = pageNo => {
  query.pageNo = pageNo
  load()
}

const handleSizeChange = pageSize => {
  query.pageSize = pageSize
  query.pageNo = 1
  load()
}

// 新增/修改成功后的刷新：新增的记录按编号升序排在最后一页，
// 所以先清掉可能存在的查询条件，再直接跳到最后一页，用户才能立刻看到它
const handleRefresh = async type => {
  if (type !== 'add') {
    load()
    return
  }
  if (searchRef.value) {
    searchRef.value.clear()
  }
  query.coursename = ''
  query.teacher = ''
  query.credit = ''
  query.pageNo = 1
  await load()
  query.pageNo = Math.max(1, Math.ceil(total.value / query.pageSize))
  await load()
}

const remove = async row => {
  const data = await request.post('/course/deleteCourse', { courseid: row.courseid })
  if (data.result === 'yes') {
    ElMessage.success(data.msg)
    // 删掉当前页最后一条时回退一页，避免出现空白页
    if (tableData.value.length === 1 && query.pageNo > 1) {
      query.pageNo -= 1
    }
    load()
  } else {
    ElMessage.error(data.msg)
  }
}

// 批量删除：勾选多行后把 courseid 拼成逗号分隔的 courseids 提交
const batchRemove = async () => {
  const ids = selectedRows.value.map(row => row.courseid)
  if (!ids.length) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 门课程吗？`, '提示', { type: 'warning' })
  } catch (e) {
    // 用户取消删除
    return
  }
  const data = await request.post('/course/deleteCourse', { courseids: ids.join(',') })
  if (data.result === 'yes') {
    ElMessage.success(data.msg)
    selectedRows.value = []
    tableRef.value && tableRef.value.clearSelection()
    // 当前页被整页删完时回退一页，避免出现空白页
    if (ids.length >= tableData.value.length && query.pageNo > 1) {
      query.pageNo -= 1
    }
    load()
  } else {
    ElMessage.error(data.msg)
  }
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: bold;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
