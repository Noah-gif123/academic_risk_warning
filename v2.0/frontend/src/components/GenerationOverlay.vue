<template>
  <Teleport to="body">
    <div v-if="open" class="gen-overlay fixed inset-0 z-[90] flex items-center justify-center p-4">
      <div class="gen-card relative w-full max-w-[760px] rounded-[26px] overflow-hidden text-white">
        <div class="gen-aurora"></div>
        <div class="gen-grid"></div>
        <div class="relative p-7 md:p-9">
          <p class="text-[10px] tracking-[.28em] text-[#7dd3c7]">COURSE INTELLIGENCE · GENERATING</p>
          <h3 class="text-2xl font-semibold mt-3 text-white">{{ title }}</h3>
          <p class="text-sm text-slate-300 mt-2">{{ subtitle || messages[Math.min(step, messages.length - 1)] }}</p>

          <div class="grid grid-cols-4 md:grid-cols-7 gap-2 mt-6">
            <div v-for="(a, i) in agents" :key="a.name"
                 class="gen-node" :class="step > i ? 'done' : (step === i ? 'active' : '')">
              <span class="text-xl">{{ a.icon }}</span>
              <p class="text-[10px] mt-1">{{ a.name }}</p>
              <p class="text-[8px] mt-1" :class="step > i ? 'text-[#7dd3c7]' : 'text-slate-500'">
                {{ step > i ? 'DONE' : (step === i ? 'RUNNING' : 'WAIT') }}
              </p>
            </div>
          </div>

          <div class="mt-7">
            <div class="gen-track"><div class="gen-fill" :style="{ width: Math.min(100, step / Math.max(1, agents.length) * 100) + '%' }"></div></div>
            <div class="flex items-center justify-between mt-2 text-[10px] text-slate-400">
              <span>{{ flow || '数据 → 研判 → 取证 → 生成 → 校验' }}</span>
              <span>{{ Math.min(100, Math.round(step / Math.max(1, agents.length) * 100)) }}%</span>
            </div>
          </div>

          <div class="flex items-center gap-2 mt-6 text-[11px] text-slate-300"><span class="gen-dot"></span> Agent 正在协同工作，请稍候，不要关闭窗口</div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  open: Boolean,
  step: { type: Number, default: 0 },
  title: { type: String, default: '正在生成报告' },
  subtitle: { type: String, default: '' },
  agents: { type: Array, default: () => [] },
  messages: { type: Array, default: () => [] },
  flow: { type: String, default: '' }
})
</script>

<style scoped>
.gen-overlay { background: rgba(3, 12, 22, .72); backdrop-filter: blur(14px); }
.gen-card { background: radial-gradient(circle at 15% 0%, #123b4b 0, #081c2b 45%, #061520 100%); box-shadow: 0 30px 100px rgba(0,0,0,.55), inset 0 1px 0 rgba(255,255,255,.12); }
.gen-aurora { position: absolute; width: 520px; height: 520px; left: -160px; top: -240px; border-radius: 50%; background: conic-gradient(from 0deg, transparent, #20b7a4, #3b82c4, transparent); filter: blur(45px); opacity: .55; animation: gen-spin 7s linear infinite; }
.gen-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(125,211,199,.08) 1px, transparent 1px), linear-gradient(90deg, rgba(125,211,199,.08) 1px, transparent 1px); background-size: 38px 38px; mask-image: linear-gradient(to bottom, black, transparent 80%); }
.gen-node { border: 1px solid rgba(125,211,199,.16); background: rgba(8,34,48,.72); border-radius: 12px; padding: 9px 5px; text-align: center; transition: .35s; opacity: .45; transform: translateY(4px); }
.gen-node.active { opacity: 1; border-color: #20b7a4; background: rgba(15,157,138,.2); transform: translateY(0); animation: gen-pulse 1.15s ease-in-out infinite; }
.gen-node.done { opacity: 1; border-color: rgba(125,211,199,.45); background: rgba(15,157,138,.12); transform: translateY(0); }
.gen-track { height: 5px; border-radius: 999px; background: rgba(255,255,255,.1); overflow: hidden; }
.gen-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #20b7a4, #7dd3c7, #3b82c4); transition: width .6s ease; box-shadow: 0 0 18px rgba(32,183,164,.7); }
.gen-dot { width: 7px; height: 7px; border-radius: 50%; background: #20b7a4; box-shadow: 0 0 0 5px rgba(32,183,164,.14); animation: gen-pulse 1.2s infinite; }
@keyframes gen-spin { to { transform: rotate(360deg); } }
@keyframes gen-pulse { 50% { box-shadow: 0 0 24px rgba(32,183,164,.35); } }
</style>
