<template>
  <el-form :inline="true" class="search-form">
    <el-form-item label="课程名称">
      <el-input v-model="query.coursename" placeholder="支持模糊查询" clearable style="width: 180px" />
    </el-form-item>
    <el-form-item label="授课教师">
      <el-input v-model="query.teacher" placeholder="支持模糊查询" clearable style="width: 150px" />
    </el-form-item>
    <el-form-item label="学分">
      <el-input-number v-model="query.credit" :min="0" :max="10" :step="0.5" :precision="1" controls-position="right"
        style="width: 130px" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { reactive } from 'vue'

// 子组件通过 defineEmits 声明事件，把查询条件抛给父组件
const emit = defineEmits(['search'])

const query = reactive({ coursename: '', teacher: '', credit: null })

const buildCondition = () => ({
  coursename: query.coursename,
  teacher: query.teacher,
  // 数字选择器清空后是 null，统一转成空串，避免把 null 传给后端
  credit: query.credit == null ? '' : query.credit
})

const search = () => {
  emit('search', buildCondition())
}

const reset = () => {
  query.coursename = ''
  query.teacher = ''
  query.credit = null
  emit('search', buildCondition())
}

// 供父组件调用：只清表单本身，不触发查询（新增成功后要先重置条件再跳到最后一页）
const clear = () => {
  query.coursename = ''
  query.teacher = ''
  query.credit = null
}

// 通过 defineExpose 暴露给父组件
defineExpose({ clear })
</script>

<style scoped>
.search-form {
  padding-top: 18px;
}
</style>
