<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-[70] bg-slate-900/35 backdrop-blur-sm flex items-center justify-center p-4" @click.self="$emit('close')">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden">
        <div class="px-5 py-3 border-b border-[#e6edf5] flex items-center justify-between">
          <div><h2>记录处理 · {{ student?.studentName }}</h2><p class="text-[11px] text-slate-400 mt-1">{{ student?.studentNo }} · {{ student?.adminClassName }}</p></div>
          <button class="text-slate-400" @click="$emit('close')">✕</button>
        </div>
        <div class="p-5 space-y-3">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label class="text-sm">方式<select v-model="form.actionType" class="input mt-1"><option value="TALK">谈话</option><option value="MEETING">班会</option><option value="PARENT">联系家长</option><option value="SUPPLEMENT">补课补习</option><option value="OTHER">其他</option></select></label>
            <label class="text-sm">有效性<select v-model="form.effective" class="input mt-1"><option value="PENDING">待复评</option><option value="EFFECTIVE">有效</option><option value="INEFFECTIVE">无效</option></select></label>
          </div>
          <label class="text-sm block">处理内容<textarea v-model="form.content" rows="3" class="input mt-1" placeholder="了解学习困难，约定本周补齐作业…"></textarea></label>
          <label class="text-sm block">处理结果<input v-model="form.result" class="input mt-1" placeholder="学生承诺本周五前完成" /></label>
          <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
        </div>
        <div class="px-5 py-3 border-t border-[#e6edf5] flex justify-end gap-2">
          <button class="btn-ghost text-sm" @click="$emit('close')">取消</button>
          <button class="btn-primary text-sm" :disabled="saving || !form.content" @click="save">{{ saving ? '保存中…' : '保存处理记录' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { counselorApi } from '../api/counselor'
const props = defineProps({ open: Boolean, student: { type: Object, default: () => ({}) } })
const emit = defineEmits(['close', 'saved'])
const form = reactive({ actionType: 'TALK', content: '', result: '', effective: 'PENDING' })
const saving = ref(false)
const error = ref('')
watch(() => props.open, (v) => { if (v) { form.content = ''; form.result = ''; form.effective = 'PENDING'; error.value = '' } })
async function save() {
  if (!props.student?.studentId) { error.value = '缺少学生信息'; return }
  saving.value = true
  try {
    await counselorApi.addIntervention({ studentId: props.student.studentId, actionType: form.actionType, content: form.content, result: form.result || null, effective: form.effective })
    emit('saved'); emit('close')
  } catch (e) { error.value = e?.message || '保存失败' } finally { saving.value = false }
}
</script>
