import { reactive } from 'vue'

/** 全局轻提示：右下角浮层，成功/失败都用它，避免内联小字看不清 */
let seq = 0
export const toasts = reactive([])

function push(text, type, ms) {
  const id = ++seq
  toasts.push({ id, text: String(text == null ? '' : text), type })
  setTimeout(() => {
    const i = toasts.findIndex((t) => t.id === id)
    if (i >= 0) toasts.splice(i, 1)
  }, ms)
}

export function toast(text, ms = 2600) {
  push(text, 'success', ms)
}

export function toastError(text, ms = 4500) {
  push(text, 'error', ms)
}

/** 统一入口：ok 为真走成功样式，否则失败样式 */
export function notify(text, ok = true) {
  if (ok) toast(text)
  else toastError(text)
}
