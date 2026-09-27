<template>
  <el-form :inline="true" class="search-form">
    <el-form-item label="用户名">
      <el-input v-model="query.username" placeholder="支持模糊查询" clearable style="width: 180px" />
    </el-form-item>
    <el-form-item label="性别">
      <el-select v-model="query.gender" placeholder="全部" clearable style="width: 120px">
        <el-option label="男" value="1" />
        <el-option label="女" value="0" />
      </el-select>
    </el-form-item>
    <el-form-item label="出生日期">
      <el-date-picker
        v-model="birthRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        unlink-panels
        style="width: 250px"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, ref } from 'vue'

// 子组件通过 defineEmits 声明事件，把查询条件抛给父组件
const emit = defineEmits(['search'])

const query = reactive({ username: '', gender: '' })

// 出生日期区间：选完后拆成 birthdate1 / birthdate2 两个参数传给后端
const birthRange = ref([])

// 区间未选满时后台会收到 null，这里统一兜底成空串，避免把查询条件带成无效值
const rangeValue = index => (birthRange.value && birthRange.value[index] ? birthRange.value[index] : '')

const buildCondition = () => ({
  username: query.username,
  gender: query.gender,
  birthdate1: rangeValue(0),
  birthdate2: rangeValue(1)
})

const search = () => {
  emit('search', buildCondition())
}

const reset = () => {
  query.username = ''
  query.gender = ''
  birthRange.value = []
  emit('search', buildCondition())
}

// 供父组件调用：只清表单本身，不触发查询（新增成功后要先重置条件再跳到最后一页）
const clear = () => {
  query.username = ''
  query.gender = ''
  birthRange.value = []
}

// 通过 defineExpose 暴露给父组件
defineExpose({ clear })
</script>

<style scoped>
.search-form {
  padding-top: 18px;
}
</style>
