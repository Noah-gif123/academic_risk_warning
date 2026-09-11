/**
 * 学情画像 / 风险等级的统一口径
 *
 * 综合成绩（calculatedScore）越高越好，因此按分数分段映射到预警等级：
 *   分数 < 60  → RED
 *   60 ~ 70    → ORANGE
 *   70 ~ 80    → YELLOW
 *   否则        → GREEN
 *
 * 注意：后端"风险分"方向相反（越高越危险），不要把风险分直接传进来。
 */
export const SCORE_LEVEL_THRESHOLDS = Object.freeze({ red: 60, orange: 70, yellow: 80 })

export const LEVEL_LABELS = Object.freeze({
  RED: '红色预警',
  ORANGE: '橙色预警',
  YELLOW: '黄色预警',
  GREEN: '正常'
})

/** 由综合成绩推导风险等级，缺数据时返回 fallback */
export function riskLevelFromScore(calculatedScore, fallback = 'GREEN') {
  const n = Number(calculatedScore)
  if (calculatedScore == null || Number.isNaN(n)) return fallback
  if (n < SCORE_LEVEL_THRESHOLDS.red) return 'RED'
  if (n < SCORE_LEVEL_THRESHOLDS.orange) return 'ORANGE'
  if (n < SCORE_LEVEL_THRESHOLDS.yellow) return 'YELLOW'
  return 'GREEN'
}

/** 等级中文名 */
export function riskLevelLabel(level) {
  return LEVEL_LABELS[level] || LEVEL_LABELS.GREEN
}
