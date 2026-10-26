<template>
  <div class="rte">
    <div class="rte-toolbar">
      <button type="button" class="rte-btn" title="加粗" @click="cmd('bold')"><b>B</b></button>
      <button type="button" class="rte-btn" title="斜体" @click="cmd('italic')"><i>I</i></button>
      <button type="button" class="rte-btn" title="下划线" @click="cmd('underline')"><u>U</u></button>
      <span class="rte-sep"></span>
      <button type="button" class="rte-btn" title="正文" @click="format('p')">正文</button>
      <button type="button" class="rte-btn" title="小标题" @click="format('h3')">H3</button>
      <button type="button" class="rte-btn" title="大标题" @click="format('h2')">H2</button>
      <span class="rte-sep"></span>
      <button type="button" class="rte-btn" title="小号字" @click="size('2')">A-</button>
      <button type="button" class="rte-btn" title="正常字号" @click="size('3')">A</button>
      <button type="button" class="rte-btn" title="大号字" @click="size('5')">A+</button>
      <span class="rte-sep"></span>
      <button type="button" class="rte-btn" title="无序列表" @click="cmd('insertUnorderedList')">• 列表</button>
      <button type="button" class="rte-btn" title="有序列表" @click="cmd('insertOrderedList')">1. 列表</button>
      <span class="rte-sep"></span>
      <button type="button" class="rte-btn" title="插入链接" @click="addLink">链接</button>
      <button type="button" class="rte-btn" title="清除格式" @click="cmd('removeFormat')">清格式</button>
      <button type="button" class="rte-btn" title="撤销" @click="cmd('undo')">↶</button>
      <button type="button" class="rte-btn" title="重做" @click="cmd('redo')">↷</button>
    </div>
    <div
      ref="editor"
      class="rte-body"
      :style="{ minHeight: minHeight + 'px' }"
      contenteditable="true"
      :data-placeholder="placeholder"
      @input="onInput"
      @blur="onBlur"
      @keydown.ctrl.b.prevent="cmd('bold')"
      @keydown.meta.b.prevent="cmd('bold')"
    ></div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '在这里写内容…' },
  minHeight: { type: Number, default: 240 }
})
const emit = defineEmits(['update:modelValue'])

const editor = ref(null)
const focused = ref(false)

function paint(html) {
  if (!editor.value) return
  if (editor.value.innerHTML !== (html || '')) {
    editor.value.innerHTML = html || ''
  }
}

function sync() {
  if (!editor.value) return
  emit('update:modelValue', editor.value.innerHTML)
}

function onInput() {
  sync()
}

function onBlur() {
  focused.value = false
  sync()
}

function cmd(name, value = null) {
  if (!editor.value) return
  editor.value.focus()
  document.execCommand('styleWithCSS', false, true)
  document.execCommand(name, false, value)
  sync()
}

function format(tag) {
  cmd('formatBlock', '<' + tag + '>')
}

function size(level) {
  cmd('fontSize', level)
}

function addLink() {
  const url = window.prompt('链接地址（https://…）')
  if (!url) return
  cmd('createLink', url)
}

onMounted(() => {
  paint(props.modelValue)
  if (editor.value) {
    editor.value.addEventListener('focus', () => { focused.value = true })
  }
})

watch(() => props.modelValue, (v) => {
  if (!focused.value) paint(v)
})

defineExpose({ focus: () => editor.value && editor.value.focus() })
</script>

<style scoped>
.rte { border: 1px solid #e2e8f0; border-radius: 0.5rem; overflow: hidden; background: #fff; }
.rte:focus-within { border-color: #94a3b8; box-shadow: 0 0 0 2px rgba(148, 163, 184, 0.15); }
.rte-toolbar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 2px;
  padding: 4px 6px; background: #f8fafc; border-bottom: 1px solid #e2e8f0;
}
.rte-btn {
  min-width: 28px; height: 26px; padding: 0 6px; border-radius: 4px;
  font-size: 12px; color: #475569; background: transparent; border: 1px solid transparent;
}
.rte-btn:hover { background: #fff; border-color: #cbd5e1; }
.rte-sep { width: 1px; height: 16px; background: #e2e8f0; margin: 0 4px; }
.rte-body {
  padding: 10px 12px; font-size: 14px; line-height: 1.7; color: #334155; outline: none;
  overflow-y: auto; max-height: 520px;
}
.rte-body:empty:before {
  content: attr(data-placeholder);
  color: #94a3b8;
}
.rte-body :deep(h2) { font-size: 18px; font-weight: 600; margin: 8px 0 4px; }
.rte-body :deep(h3) { font-size: 16px; font-weight: 600; margin: 6px 0 4px; }
.rte-body :deep(p) { margin: 4px 0; }
.rte-body :deep(ul) { list-style: disc; padding-left: 20px; margin: 4px 0; }
.rte-body :deep(ol) { list-style: decimal; padding-left: 20px; margin: 4px 0; }
.rte-body :deep(a) { color: #2563eb; text-decoration: underline; }
.rte-body :deep(table) { width: 100%; border-collapse: collapse; }
.rte-body :deep(th),
.rte-body :deep(td) { border: 1px solid #e2e8f0; padding: 4px 8px; font-size: 13px; }
.rte-body :deep(th) { background: #f8fafc; }
</style>
