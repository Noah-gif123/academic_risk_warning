<template>
  <el-card shadow="never" class="gallery">
    <template #header>
      <div class="gallery-header">
        <span class="gallery-title">照片墙</span>
        <el-select v-model="userid" clearable filterable placeholder="全部用户" style="width:220px" @change="load">
          <el-option v-for="u in users" :key="u.userid" :label="label(u)" :value="u.userid" />
        </el-select>
      </div>
    </template>
    <div v-loading="loading" class="wall">
      <el-empty v-if="!images.length && !loading" description="还没有照片" :image-size="80" />
      <div v-for="(img, i) in images" :key="img.imageid" class="cell">
        <el-image :src="img.imageUrl" :preview-src-list="previewList" :initial-index="i" fit="cover" class="img" />
        <div class="bar">
          <span class="who">{{ img.realname || img.username || '未知用户' }}</span>
          <el-tag v-if="img.isPortrait === 1" type="success" size="small">头像</el-tag>
        </div>
      </div>
    </div>
  </el-card>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '../utils/request'

const loading = ref(false)
const userid = ref(null)
const images = ref([])

const users = ref([])
const label = u => (u.realname ? u.realname + '（' + u.username + '）' : u.username)

const previewList = computed(() => images.value.map(i => i.imageUrl))

const loadUsers = async () => {
  const data = await request.get('/image/getAllImages')
  const map = new Map()
  ;(data.imageList || []).forEach(i => map.set(i.userid, { userid: i.userid, username: i.username, realname: i.realname }))
  users.value = Array.from(map.values())
}

const load = async () => {
  loading.value = true
  try {
    const params = userid.value ? { userid: userid.value } : {}
    const data = await request.get('/image/getAllImages', { params })
    images.value = data.imageList || []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadUsers()
  load()
})
</script>
<style scoped>
.gallery-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.gallery-title {
  font-size: 16px;
  font-weight: bold;
}
.wall {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  min-height: 160px;
}
.cell {
  width: 180px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  background-color: #fff;
}
.img {
  width: 180px;
  height: 180px;
  display: block;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  font-size: 12px;
  color: #606266;
}
</style>
