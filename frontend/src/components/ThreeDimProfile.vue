<script setup>
import { computed } from 'vue'

/**
 * 学生三维学情画像展示组件（知识掌握 / 学习习惯 / 学习目标）
 *
 * 数据来自 ProfileAgent 生成、由 /api/profile/** 读取；大模型输出字段可能缺失，
 * 因此所有字段都做兜底，缺字段时只隐藏对应小节，不显示 undefined。
 */
const props = defineProps({
  profile: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  generating: { type: Boolean, default: false },
  canGenerate: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无三维学情画像' },
  generateLabel: { type: String, default: '生成三维画像' }
})
const emit = defineEmits(['generate'])

const exists = computed(() => !!props.profile?.exists)
const knowledge = computed(() => props.profile?.knowledgeProfile || null)
const habit = computed(() => props.profile?.habitProfile || null)
const goal = computed(() => props.profile?.goalProfile || null)

const scopeText = computed(() => {
  if (!exists.value) return ''
  if (props.profile?.fallbackToComprehensive) return '本课程暂无画像，展示综合画像（跨课程）'
  return props.profile?.courseScoped ? '课程级画像' : '综合画像（跨课程）'
})

const updatedText = computed(() => {
  const t = props.profile?.updateTime
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
})

const masteryPercent = computed(() => {
  const v = Number(knowledge.value?.masteryPercent)
  if (!Number.isFinite(v)) return null
  return Math.max(0, Math.min(100, Math.round(v)))
})

const feasibilityClass = computed(() => {
  const f = goal.value?.feasibility
  if (f === '高') return 'good'
  if (f === '中') return 'mid'
  if (f === '低') return 'bad'
  return ''
})

function asList(v) {
  if (v == null || v === '') return []
  if (Array.isArray(v)) return v.filter((x) => x != null && String(x).trim() !== '')
  return [String(v)]
}
</script>

<template>
  <div class="tdp-wrap">
    <div v-if="loading" class="tdp-state">⏳ 正在加载三维画像...</div>

    <div v-else-if="!exists" class="tdp-state">
      <p class="tdp-empty-text">{{ profile?.message || emptyText }}</p>
      <p class="tdp-empty-hint">
        三维画像由智能体流水线（监测 → 分析 → 画像 → 推荐）生成，需要先触发一次该生的智能体分析。
      </p>
      <button v-if="canGenerate" class="tdp-btn" :disabled="generating" @click="emit('generate')">
        {{ generating ? '生成中，请稍候…' : generateLabel }}
      </button>
    </div>

    <div v-else class="tdp-body">
      <div class="tdp-meta">
        <span v-if="scopeText" class="tdp-tag">{{ scopeText }}</span>
        <span v-if="updatedText" class="tdp-time">更新于 {{ updatedText }}</span>
        <button v-if="canGenerate" class="tdp-btn tdp-btn-sm" :disabled="generating" @click="emit('generate')">
          {{ generating ? '生成中…' : '重新生成' }}
        </button>
      </div>

      <div class="tdp-grid">
        <!-- 知识掌握画像 -->
        <section class="tdp-card">
          <header class="tdp-card-head">
            <span class="tdp-icon">📘</span>
            <strong>知识掌握画像</strong>
            <span v-if="knowledge?.level" class="tdp-level">{{ knowledge.level }}</span>
          </header>
          <div v-if="masteryPercent !== null" class="tdp-bar-row">
            <div class="tdp-bar"><div class="tdp-bar-fill" :style="{ width: masteryPercent + '%' }"></div></div>
            <span class="tdp-bar-num">{{ masteryPercent }}%</span>
          </div>
          <p v-if="knowledge?.summary" class="tdp-summary">{{ knowledge.summary }}</p>
          <div v-if="asList(knowledge?.strongPoints).length" class="tdp-chips">
            <span class="tdp-chips-title">已掌握</span>
            <span v-for="(s, i) in asList(knowledge.strongPoints)" :key="'sp' + i" class="tdp-chip ok">{{ s }}</span>
          </div>
          <div v-if="asList(knowledge?.weakPoints).length" class="tdp-chips">
            <span class="tdp-chips-title">薄弱点</span>
            <span v-for="(s, i) in asList(knowledge.weakPoints)" :key="'wp' + i" class="tdp-chip warn">{{ s }}</span>
          </div>
          <p v-if="!knowledge" class="tdp-none">该维度暂无内容</p>
        </section>

        <!-- 学习习惯画像 -->
        <section class="tdp-card">
          <header class="tdp-card-head">
            <span class="tdp-icon">⏱️</span>
            <strong>学习习惯画像</strong>
            <span v-if="habit?.level" class="tdp-level">{{ habit.level }}</span>
          </header>
          <p v-if="habit?.summary" class="tdp-summary">{{ habit.summary }}</p>
          <div v-if="asList(habit?.traits).length" class="tdp-chips">
            <span class="tdp-chips-title">行为特征</span>
            <span v-for="(s, i) in asList(habit.traits)" :key="'tr' + i" class="tdp-chip">{{ s }}</span>
          </div>
          <div v-if="asList(habit?.suggestedHabits).length" class="tdp-chips">
            <span class="tdp-chips-title">建议培养</span>
            <span v-for="(s, i) in asList(habit.suggestedHabits)" :key="'sh' + i" class="tdp-chip ok">{{ s }}</span>
          </div>
          <p v-if="!habit" class="tdp-none">该维度暂无内容</p>
        </section>

        <!-- 学习目标画像 -->
        <section class="tdp-card">
          <header class="tdp-card-head">
            <span class="tdp-icon">🎯</span>
            <strong>学习目标画像</strong>
            <span v-if="goal?.level" class="tdp-level">{{ goal.level }}</span>
          </header>
          <div class="tdp-goal-row">
            <div v-if="goal?.targetScore != null" class="tdp-goal-item">
              <span class="tdp-goal-label">目标分数</span>
              <span class="tdp-goal-value">{{ goal.targetScore }}</span>
            </div>
            <div v-if="goal?.feasibility" class="tdp-goal-item">
              <span class="tdp-goal-label">可达性</span>
              <span class="tdp-feasibility" :class="feasibilityClass">{{ goal.feasibility }}</span>
            </div>
          </div>
          <p v-if="goal?.summary" class="tdp-summary">{{ goal.summary }}</p>
          <ol v-if="asList(goal?.milestones).length" class="tdp-milestones">
            <li v-for="(s, i) in asList(goal.milestones)" :key="'ms' + i">{{ s }}</li>
          </ol>
          <p v-if="!goal" class="tdp-none">该维度暂无内容</p>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tdp-wrap { width: 100%; }
.tdp-state {
  padding: 18px 12px;
  text-align: center;
  color: var(--text-tertiary, #94a3b8);
  font-size: 13px;
}
.tdp-empty-text { margin: 0 0 6px; color: var(--text-secondary, #475569); font-size: 14px; }
.tdp-empty-hint { margin: 0 auto 12px; max-width: 520px; font-size: 12px; line-height: 1.7; }
.tdp-body { display: flex; flex-direction: column; gap: 10px; }
.tdp-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
}
.tdp-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-muted, #f1f5f9);
  color: var(--text-secondary, #475569);
}
.tdp-time { color: var(--text-tertiary, #94a3b8); }
.tdp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}
.tdp-card {
  background: var(--bg-card-subtle, #f8fafc);
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tdp-card-head { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-primary, #0f172a); }
.tdp-icon { font-size: 15px; }
.tdp-level {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #7c3aed;
  background: rgba(124, 58, 237, 0.1);
}
.tdp-bar-row { display: flex; align-items: center; gap: 8px; }
.tdp-bar { flex: 1; height: 6px; border-radius: 999px; background: #e2e8f0; overflow: hidden; }
.tdp-bar-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #7c3aed, #0ea5e9); }
.tdp-bar-num { font-size: 11px; color: var(--text-secondary, #475569); }
.tdp-summary { margin: 0; font-size: 12.5px; line-height: 1.75; color: var(--text-secondary, #475569); }
.tdp-chips { display: flex; flex-wrap: wrap; gap: 5px; align-items: center; }
.tdp-chips-title { font-size: 11px; color: var(--text-tertiary, #94a3b8); margin-right: 2px; }
.tdp-chip {
  font-size: 11.5px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef2f7;
  color: var(--text-secondary, #475569);
}
.tdp-chip.ok { background: rgba(34, 197, 94, 0.12); color: #15803d; }
.tdp-chip.warn { background: rgba(249, 115, 22, 0.14); color: #c2410c; }
.tdp-none { margin: 0; font-size: 12px; color: var(--text-tertiary, #94a3b8); }
.tdp-goal-row { display: flex; gap: 16px; }
.tdp-goal-item { display: flex; flex-direction: column; gap: 2px; }
.tdp-goal-label { font-size: 11px; color: var(--text-tertiary, #94a3b8); }
.tdp-goal-value { font-size: 18px; font-weight: 700; color: #7c3aed; }
.tdp-feasibility { font-size: 13px; font-weight: 600; }
.tdp-feasibility.good { color: #15803d; }
.tdp-feasibility.mid { color: #b45309; }
.tdp-feasibility.bad { color: #b91c1c; }
.tdp-milestones { margin: 0; padding-left: 18px; font-size: 12.5px; line-height: 1.8; color: var(--text-secondary, #475569); }
.tdp-btn {
  align-self: center;
  padding: 7px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  color: #fff;
  background: #7c3aed;
  cursor: pointer;
  transition: filter 0.15s;
}
.tdp-btn:hover:not(:disabled) { filter: brightness(1.08); }
.tdp-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.tdp-btn-sm { align-self: flex-end; margin-left: auto; padding: 4px 12px; font-size: 12px; }
</style>
