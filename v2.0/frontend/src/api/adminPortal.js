import { http } from './http'

/** 教务端 · 工作台 / 导入批次 / 入课知会 / 审计 / 覆盖率督办 */
export const adminPortalApi = {
  dashboard: (termId) => http.get('/admin/dashboard' + (termId ? `?termId=${termId}` : '')),
  batches: (termId, status) => {
    const q = new URLSearchParams()
    if (termId) q.set('termId', termId)
    if (status) q.set('status', status)
    const s = q.toString()
    return http.get('/admin/import-batches' + (s ? '?' + s : ''))
  },
  rollback: (id, reason) => http.post(`/admin/import-batches/${id}/rollback`, { reason }),
  notices: (termId) => http.get('/admin/join-notices' + (termId ? `?termId=${termId}` : '')),
  audit: (params = {}) => {
    const q = new URLSearchParams()
    if (params.bizType) q.set('bizType', params.bizType)
    if (params.action) q.set('action', params.action)
    if (params.keyword) q.set('keyword', params.keyword)
    if (params.from) q.set('from', params.from)
    if (params.to) q.set('to', params.to)
    if (params.page) q.set('page', params.page)
    if (params.size) q.set('size', params.size)
    const s = q.toString()
    return http.get('/admin/audit' + (s ? '?' + s : ''))
  },
  urge: (teachingClassId) => http.post(`/admin/coverage/${teachingClassId}/urge`)
}
