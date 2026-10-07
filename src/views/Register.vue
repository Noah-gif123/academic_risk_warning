<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">用户注册</div>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="page-form">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" clearable @blur="checkUsername" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="password2">
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
        <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="请输入个人简介" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">注册</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const formRef = ref()

const form = ref({
  username: '',
  password: '',
  password2: '',
  realname: '',
  birthdate: '',
  gender: '1',
  degree: '',
  intro: ''
})

// 多选的兴趣爱好，提交时拼接成逗号分隔的字符串
const interestList = ref([])

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password2: [
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
  ],
  realname: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

// 用户名失焦时调用后端接口校验是否已存在
const checkUsername = async () => {
  if (!form.value.username) {
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
  const data = await request.post('/user/addUser', payload)
  if (data.result === 'yes') {
    ElMessage.success(data.msg)
    router.push('/login')
  } else {
    ElMessage.error(data.msg)
  }
}

const reset = () => {
  form.value = {
    username: '',
    password: '',
    password2: '',
    realname: '',
    birthdate: '',
    gender: '1',
    degree: '',
    intro: ''
  }
  interestList.value = []
  formRef.value.clearValidate()
}
</script>

<style scoped>
.page-form {
  width: 460px;
}
.card-header {
  font-weight: bold;
}
</style>
