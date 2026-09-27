<template>
  <el-dialog v-model="visible" :title="title" width="500px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="!isNew" placeholder="请输入用户名" clearable @blur="remoteCheck" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          :placeholder="isNew ? '请输入密码' : '不修改请留空'"
          show-password
        />
      </el-form-item>
      <el-form-item v-if="isNew" label="确认密码" prop="password2">
        <el-input v-model="form.password2" type="password" placeholder="请再次输入密码" show-password />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realname">
        <el-input v-model="form.realname" placeholder="请输入真实姓名" clearable />
      </el-form-item>
      <el-form-item label="出生日期">
        <el-date-picker
          v-model="form.birthdate"
          type="date"
          placeholder="请选择出生日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio label="1">男</el-radio>
          <el-radio label="0">女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="兴趣爱好">
        <el-checkbox-group v-model="interestList">
          <el-checkbox label="篮球">篮球</el-checkbox>
          <el-checkbox label="足球">足球</el-checkbox>
          <el-checkbox label="音乐">音乐</el-checkbox>
          <el-checkbox label="阅读">阅读</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="学历">
        <el-select v-model="form.degree" placeholder="请选择学历" clearable>
          <el-option label="大专" value="1" />
          <el-option label="本科" value="2" />
          <el-option label="硕士" value="3" />
          <el-option label="博士" value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="个人简介">
        <el-input v-model="form.intro" type="textarea" :rows="3" />
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
const interestList = ref([])

// 没有 userid 表示新增，有 userid 表示修改
const isNew = computed(() => form.value.userid == null)
const title = computed(() => (isNew.value ? '新增用户' : '修改用户'))
const submitText = computed(() => (isNew.value ? '确定新增' : '保存修改'))

// 修改时密码与用户名不必填，新增时必须填写
const rules = computed(() => ({
  username: isNew.value
    ? [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
      ]
    : [],
  password: isNew.value
    ? [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
      ]
    : [],
  password2: isNew.value
    ? [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== form.value.password) {
              callback(new Error('两次输入的密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    : [],
  realname: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}))

// 父组件通过 ref 调用该方法打开弹窗：不传 row 为新增，传 row 为修改（先按 id 查询回显）
const open = async row => {
  if (!row) {
    form.value = { gender: '1', degree: '', password: '', password2: '' }
    interestList.value = []
    visible.value = true
    return
  }
  const data = await request.get('/user/getUser', { params: { userid: row.userid } })
  form.value = { ...data.user, password: '', password2: '' }
  interestList.value = data.user && data.user.interest ? data.user.interest.split(',') : []
  visible.value = true
}

// 新增时用户名失焦调用后端接口校验是否已存在
const remoteCheck = async () => {
  if (!isNew.value || !form.value.username) {
    return
  }
  const data = await request.get('/user/checkUsername', { params: { username: form.value.username } })
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
  const payload = { ...form.value, interest: interestList.value.join(',') }
  delete payload.password2
  const data = await request.post(isNew.value ? '/user/addUser' : '/user/updateUser', payload)
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
