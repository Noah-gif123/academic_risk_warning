<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <span class="app-title">智慧巡课录课平台 · 用户管理模块</span>
      <span class="app-user">
        当前用户：{{ loginUser || '未登录' }}
        <el-button v-if="loginUser" size="small" type="primary" plain @click="logout">退出登录</el-button>
      </span>
    </el-header>
    <el-container>
      <el-aside width="200px">
        <el-menu :default-active="$route.path" :default-openeds="['user-manage', 'course-manage']"
          @select="onMenuSelect">
          <el-menu-item index="/login">用户登录</el-menu-item>
          <el-menu-item index="/register">用户注册</el-menu-item>
          <el-sub-menu index="user-manage">
            <template #title>用户管理</template>
            <el-menu-item index="/users">用户列表</el-menu-item>
            <el-menu-item index="/photos">照片墙</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="course-manage">
            <template #title>课程管理</template>
            <el-menu-item index="/courses">课程列表</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from './utils/request'

const router = useRouter()

// 登录用户名保存在 localStorage，登录后整页跳转，组件重新挂载时读取
const loginUser = ref(localStorage.getItem('loginUser') || '')

// 需要登录才能访问的菜单
const needLoginPaths = ['/users', '/photos', '/courses']

// 菜单点击：未登录时提示并停留在当前页，不做跳转
const onMenuSelect = index => {
  if (needLoginPaths.includes(index) && !loginUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  router.push(index)
}

// 退出时先让服务端 session 失效，再清理本地登录态
const logout = async () => {
  try {
    await request.get('/user/logout')
  } catch (e) {
    // 接口异常时也要保证本地登录态被清掉
  }
  localStorage.removeItem('loginUser')
  window.location.href = '/login'
}
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
}
.app-layout {
  height: 100%;
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #409eff;
  color: #fff;
}
.app-title {
  font-size: 18px;
  font-weight: bold;
}
.app-user {
  font-size: 14px;
}
.el-main {
  background-color: #f5f7fa;
}
</style>
