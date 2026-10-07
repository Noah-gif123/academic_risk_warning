<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>用户列表</span>
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
          <el-button type="primary" size="small" :icon="Plus" @click="editRef.open()">新增用户</el-button>
        </div>
      </div>
    </template>

    <UserSearch ref="searchRef" @search="handleSearch" />

    <el-table
      ref="tableRef"
      :data="tableData"
      border
      stripe
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="userid" label="编号" width="70" />
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <el-image
            v-if="row.portrait"
            :src="row.portrait"
            :preview-src-list="[row.portrait]"
            fit="cover"
            class="avatar"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="110" />
      <el-table-column prop="realname" label="真实姓名" width="110" />
      <el-table-column prop="birthdate" label="出生日期" width="120" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">{{ row.gender === '1' ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column prop="interest" label="兴趣爱好" min-width="140" />
      <el-table-column label="学历" width="90">
        <template #default="{ row }">{{ degreeMap[row.degree] || '-' }}</template>
      </el-table-column>
      <el-table-column prop="intro" label="个人简介" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" :icon="Edit" circle title="修改" @click="editRef.open(row)" />
          <el-button size="small" type="success" :icon="Picture" circle title="照片墙" @click="photoRef.open(row)" />
          <el-popconfirm title="确定删除该用户吗？" @confirm="remove(row)">
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

    <UserEdit ref="editRef" @refresh="handleRefresh" />
    <PhotoWall ref="photoRef" @refresh="load" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Plus, Picture } from '@element-plus/icons-vue'
import request from '../utils/request'
import UserSearch from '../components/UserSearch.vue'
import UserEdit from '../components/UserEdit.vue'
import PhotoWall from '../components/PhotoWall.vue'

const degreeMap = { 1: '大专', 2: '本科', 3: '硕士', 4: '博士' }

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const editRef = ref()
const tableRef = ref()
const photoRef = ref()
const searchRef = ref()

// 表格多选结果，用于批量删除
const selectedRows = ref([])

// 查询条件与分页参数一起提交给后端
const query = reactive({ username: '', gender: '', birthdate1: '', birthdate2: '', pageNo: 1, pageSize: 5 })

const load = async () => {
  loading.value = true
  try {
    const data = await request.get('/user/getUserList', { params: query })
    tableData.value = data.userList || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = condition => {
  query.username = condition.username
  query.gender = condition.gender
  query.birthdate1 = condition.birthdate1
  query.birthdate2 = condition.birthdate2
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
  query.username = ''
  query.gender = ''
  query.birthdate1 = ''
  query.birthdate2 = ''
  query.pageNo = 1
  await load()
  query.pageNo = Math.max(1, Math.ceil(total.value / query.pageSize))
  await load()
}

const remove = async row => {
  const data = await request.post('/user/deleteUser', { userid: row.userid })
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

// 批量删除：勾选多行后把 userid 拼成逗号分隔的 userids 提交
const batchRemove = async () => {
  const ids = selectedRows.value.map(row => row.userid)
  if (!ids.length) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个用户吗？`, '提示', { type: 'warning' })
  } catch (e) {
    // 用户取消删除
    return
  }
  const data = await request.post('/user/deleteUser', { userids: ids.join(',') })
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
.avatar {
  width: 44px;
  height: 44px;
  border-radius: 4px;
  display: block;
}
</style>
