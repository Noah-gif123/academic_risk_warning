<template>
  <el-dialog v-model="visible" :title="`照片墙 · ${user.realname || user.username}`" width="820px">
    <div v-loading="loading" class="photo-wrap">
      <el-upload
        v-model:file-list="fileList"
        :action="uploadAction"
        :data="{ userid: user.userid }"
        name="file"
        multiple
        accept="image/jpeg,image/png,image/gif"
        list-type="picture-card"
        :before-upload="beforeUpload"
        :on-success="onSuccess"
        :on-error="onError"
      >
        <el-icon class="photo-add-icon"><Plus /></el-icon>

        <template #tip>
          <div class="upload-tip">支持 jpg / png / gif，单张不超过 10MB，可一次选择多张；鼠标移到照片上可预览 / 设为头像 / 删除</div>
        </template>

        <template #file="{ file }">
          <img class="el-upload-list__item-thumbnail" :src="file.url" alt="" />
          <span class="el-upload-list__item-actions">
            <span class="el-upload-list__item-preview" title="预览大图" @click="onPreview(file)">
              <el-icon><ZoomIn /></el-icon>
            </span>
            <span
              v-if="!isHead(file)"
              class="el-upload-list__item-preview"
              title="设为头像"
              @click="setPortrait(file)"
            >
              <el-icon><Check /></el-icon>
            </span>
            <span class="el-upload-list__item-delete" title="删除照片" @click="onRemove(file)">
              <el-icon><Delete /></el-icon>
            </span>
          </span>
          <span v-if="isHead(file)" class="el-upload-list__item-status-label" title="当前头像">
            <el-icon class="el-icon--check"><Check /></el-icon>
          </span>
        </template>
      </el-upload>

      <el-empty v-if="!fileList.length && !loading" description="还没有照片，先上传一张吧" :image-size="80" />
    </div>

    <el-image-viewer
      v-if="viewerVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      :hide-on-click-modal="true"
      teleported
      @close="viewerVisible = false"
    />
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ZoomIn, Check, Delete } from '@element-plus/icons-vue'
import request from '../utils/request'

// 上传地址直连后端，走 devServer 代理，与 axios 的 baseURL 保持一致
const uploadAction = '/springbootdemo/image/upload'

const emit = defineEmits(['refresh'])

const visible = ref(false)
const loading = ref(false)
const user = ref({})
const images = ref([])
const fileList = ref([])

// 大图预览相关
const viewerVisible = ref(false)
const previewIndex = ref(0)
const previewList = computed(() => images.value.map(img => img.imageUrl))

// file-list 里的 uid 就是 imageid，据此回到原始记录上取最新状态
const isHead = file => {
  const img = images.value.find(item => item.imageid === file.imageid)
  return !!(img && img.isPortrait === 1)
}

const buildFileList = () => {
  fileList.value = images.value.map(img => ({
    uid: img.imageid,
    name: String(img.imageUrl).split('/').pop(),
    url: img.imageUrl,
    imageid: img.imageid,
    isPortrait: img.isPortrait,
    status: 'success'
  }))
}

const loadImages = async () => {
  loading.value = true
  try {
    const data = await request.get('/image/getUserImages', { params: { userid: user.value.userid } })
    images.value = data.imageList || []
    buildFileList()
  } finally {
    loading.value = false
  }
}

const open = row => {
  user.value = row
  visible.value = true
  loadImages()
}

defineExpose({ open })

const beforeUpload = file => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.error('只能上传 jpg / png / gif 格式的图片')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('单张照片不能超过 10MB')
    return false
  }
  return true
}

const onSuccess = res => {
  if (res && res.result === 'yes') {
    ElMessage.success(res.msg)
    loadImages()
    emit('refresh')
  } else {
    ElMessage.error((res && res.msg) || '上传失败')
    loadImages()
  }
}

const onError = () => {
  ElMessage.error('上传失败，请检查是否已登录')
  loadImages()
}

const onPreview = file => {
  const index = images.value.findIndex(item => item.imageid === file.imageid)
  previewIndex.value = index < 0 ? 0 : index
  viewerVisible.value = true
}

const setPortrait = async file => {
  const imageid = file.imageid
  const data = await request.post('/image/setPortrait', { userid: user.value.userid, imageid })
  if (data.result === 'yes') {
    ElMessage.success(data.msg)
    await loadImages()
    emit('refresh')
  } else {
    ElMessage.error(data.msg)
  }
}

const onRemove = async file => {
  try {
    await ElMessageBox.confirm('确定删除这张照片吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  const imageid = file.imageid
  const data = await request.post('/image/deleteImage', { imageid })
  if (data.result === 'yes') {
    ElMessage.success(data.msg)
    await loadImages()
    emit('refresh')
  } else {
    ElMessage.error(data.msg)
  }
}
</script>

<style scoped>
.photo-wrap {
  min-height: 160px;
}
.photo-add-icon {
  font-size: 24px;
  color: #8c939d;
}
.upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
/* 灰色遮罩里三个按钮横向排开，图标居中 */
:deep(.el-upload-list__item-actions) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
:deep(.el-upload-list__item-actions > span) {
  cursor: pointer;
}
:deep(.el-upload-list__item-thumbnail) {
  object-fit: cover;
}
</style>
