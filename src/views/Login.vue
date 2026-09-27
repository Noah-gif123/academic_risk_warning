<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">用户登录</div>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="page-form">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">登录</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button link type="primary" @click="$router.push('/register')">没有账号？去注册</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const formRef = ref()
const form = ref({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  const data = await request.get('/user/login', { params: form.value })
  if (data.login === 'yes') {
    localStorage.setItem('loginUser', form.value.username)
    ElMessage.success('登录成功')
    // 整页跳转，让 App.vue 重新读取登录用户信息
    window.location.href = '/users'
  } else {
    ElMessage.error('用户名或密码错误')
  }
}

const reset = () => {
  form.value = { username: '', password: '' }
  formRef.value.clearValidate()
}
</script>

<style scoped>
.page-form {
  width: 420px;
}
.card-header {
  font-weight: bold;
}
</style>
