<template>
  <el-dialog v-model="visible" :title="title" width="500px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="课程名称" prop="coursename">
        <el-input v-model="form.coursename" :disabled="!isNew" placeholder="请输入课程名称" clearable @blur="remoteCheck" />
      </el-form-item>
      <el-form-item label="学分" prop="credit">
        <el-input-number v-model="form.credit" :min="0" :max="10" :step="0.5" :precision="1" controls-position="right"
          style="width: 160px" />
      </el-form-item>
      <el-form-item label="授课教师" prop="teacher">
        <el-input v-model="form.teacher" placeholder="请输入授课教师" clearable />
      </el-form-item>
      <el-form-item label="上课时间">
        <el-input v-model="form.coursetime" placeholder="如：周一第1-2节" clearable />
      </el-form-item>
      <el-form-item label="上课地点">
        <el-input v-model="form.classroom" placeholder="如：信息楼A301" clearable />
      </el-form-item>
      <el-form-item label="课程简介">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">{{ submitText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const emit = defineEmits(['refresh'])

const visible = ref(false)
const formRef = ref()
const form = ref({})

// 没有 courseid 表示新增，有 courseid 表示修改
const isNew = computed(() => form.value.courseid == null)
const title = computed(() => (isNew.value ? '新增课程' : '修改课程'))
const submitText = computed(() => (isNew.value ? '确定新增' : '保存修改'))

const rules = {
  coursename: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'blur' }
  ],
  credit: [{ required: true, message: '请输入学分', trigger: 'change' }],
  teacher: [{ required: true, message: '请输入授课教师', trigger: 'blur' }]
}

// 父组件通过 ref 调用该方法打开弹窗：不传 row 为新增，传 row 为修改（先按 id 查询回显）
const open = async row => {
  if (!row) {
    form.value = { credit: 3 }
    visible.value = true
    return
  }
  const data = await request.get('/course/getCourse', { params: { courseid: row.courseid } })
  form.value = { ...data.course }
  visible.value = true
}

// 新增时课程名失焦调用后端接口校验是否已存在
const remoteCheck = async () => {
  if (!isNew.value || !form.value.coursename) {
    return
  }
  const data = await request.get('/course/checkCoursename', { params: { coursename: form.value.coursename } })
  if (data.result === 'no') {
    ElMessage.warning(data.msg)
  }
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  const data = await request.post(isNew.value ? '/course/addCourse' : '/course/updateCourse', form.value)
  if (data.result === 'yes') {
    ElMessage.success(data.msg)
    visible.value = false
    // 告诉父组件本次是新增还是修改：新增的记录要跳到最后一页才看得见
    emit('refresh', isNew.value ? 'add' : 'update')
  } else {
    ElMessage.error(data.msg)
  }
}

// 通过 defineExpose 把方法暴露给父组件
defineExpose({ open })
</script>
