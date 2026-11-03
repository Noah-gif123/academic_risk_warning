// 只校验 SFC 能否解析与编译（沙箱里跑不了 vite build 时的替代手段）
import { readFileSync } from 'node:fs'
import { parse, compileTemplate, compileScript } from '@vue/compiler-sfc'

const files = process.argv.slice(2)
let bad = 0
for (const f of files) {
  const src = readFileSync(f, 'utf8')
  const { descriptor, errors } = parse(src, { filename: f })
  if (errors.length) {
    bad++
    console.log(`[parse] ${f}`)
    errors.forEach((e) => console.log('   ' + e.message))
    continue
  }
  const id = 'x' + Math.random().toString(36).slice(2, 8)
  try {
    const script = compileScript(descriptor, { id })
    const tpl = compileTemplate({
      source: descriptor.template ? descriptor.template.content : '',
      filename: f,
      id,
      compilerOptions: { bindingMetadata: script.bindings }
    })
    if (tpl.errors.length) {
      bad++
      console.log(`[template] ${f}`)
      tpl.errors.forEach((e) => console.log('   ' + (e.message || e)))
    } else {
      console.log(`[ok] ${f}`)
    }
  } catch (e) {
    bad++
    console.log(`[script] ${f}`)
    console.log('   ' + e.message)
  }
}
process.exit(bad ? 1 : 0)
