import { ref, nextTick } from 'vue'

export function useCaptcha() {
  const captchaCode = ref('')
  const captchaInput = ref('')

  function generateCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    let code = ''
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    captchaCode.value = code
    return code
  }

  function drawCaptcha(canvasEl) {
    if (!canvasEl) return
    const width = 120, height = 40
    canvasEl.width = width
    canvasEl.height = height
    const ctx = canvasEl.getContext('2d')
    if (!ctx) return

    // 背景
    ctx.fillStyle = '#f1f5f9'
    ctx.fillRect(0, 0, width, height)

    // 干扰线
    for (let i = 0; i < 3; i++) {
      ctx.strokeStyle = `rgba(${Math.random() * 100 + 100}, ${Math.random() * 100 + 100}, ${Math.random() * 100 + 100}, 0.5)`
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(Math.random() * width, Math.random() * height)
      ctx.lineTo(Math.random() * width, Math.random() * height)
      ctx.stroke()
    }

    // 文字
    ctx.font = 'bold 24px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    for (let i = 0; i < captchaCode.value.length; i++) {
      const x = 20 + i * 25
      const y = height / 2 + (Math.random() - 0.5) * 10
      const rotation = (Math.random() - 0.5) * 0.4
      ctx.save()
      ctx.translate(x, y)
      ctx.rotate(rotation)
      const hue = Math.random() * 60 + 200
      ctx.fillStyle = `hsl(${hue}, 70%, 40%)`
      ctx.fillText(captchaCode.value[i], 0, 0)
      ctx.restore()
    }

    // 干扰点
    for (let i = 0; i < 30; i++) {
      ctx.fillStyle = `rgba(${Math.random() * 150}, ${Math.random() * 150}, ${Math.random() * 150}, 0.5)`
      ctx.beginPath()
      ctx.arc(Math.random() * width, Math.random() * height, 1, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  function refreshCaptcha(canvasRef) {
    generateCode()
    captchaInput.value = ''
    nextTick(() => {
      if (canvasRef?.value) {
        drawCaptcha(canvasRef.value)
      }
    })
  }

  function verifyCaptcha() {
    if (!captchaInput.value) return '请输入验证码'
    if (captchaInput.value.toUpperCase() !== captchaCode.value.toUpperCase()) return '验证码错误'
    return null
  }

  return {
    captchaCode, captchaInput,
    generateCode, drawCaptcha,
    refreshCaptcha, verifyCaptcha
  }
}
