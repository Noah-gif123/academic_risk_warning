<template>
  <div v-if="error" class="min-h-[60vh] flex items-center justify-center p-6">
    <div class="card p-6 max-w-lg w-full text-center">
      <div class="w-12 h-12 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center mx-auto text-xl">!</div>
      <h2 class="mt-3 text-base font-semibold text-slate-800">页面加载失败</h2>
      <p class="text-xs text-slate-500 mt-2 leading-6">当前页面遇到异常，其他页面不受影响。可以重试，或返回首页继续操作。</p>
      <div class="flex justify-center gap-2 mt-4">
        <button class="btn-primary text-sm" @click="retry">重新加载</button>
        <button class="btn-ghost text-sm" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { onErrorCaptured, ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const error = ref(null)
onErrorCaptured((err) => {
  error.value = err
  console.error('[ErrorBoundary]', err)
  return false
})
function retry() {
  error.value = null
  router.go(0)
}
function goHome() {
  error.value = null
  router.push('/login')
}
</script>
