import { http } from './http'

/** P1 统计看板接口（/api/stats 七接口） */
function qs(params = {}) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.set(k, v)
  })
  const s = q.toString()
  return s ? '?' + s : ''
}

export const statsApi = {
  overview: (params) => http.get('/stats/overview' + qs(params)),
  trend: (params) => http.get('/stats/trend' + qs(params)),
  distribution: (params) => http.get('/stats/distribution' + qs(params)),
  compare: (params) => http.get('/stats/compare' + qs(params)),
  funnel: (params) => http.get('/stats/funnel' + qs(params)),
  effect: (params) => http.get('/stats/effect' + qs(params)),
  exportCsv: (params) => http.download('/stats/export' + qs(params), 'stats.csv'),
  recompute: (params) => http.post('/stats/recompute' + qs(params), {})
}
