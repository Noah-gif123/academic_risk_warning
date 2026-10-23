import { http } from './http'

/** 教务端 · 判定规则配置（第二阶段第 5 步） */
export const adminRuleApi = {
  /** 当前生效版本 + 历史版本 */
  list: () => http.get('/admin/rules'),
  /** 新增版本并立即生效（未传字段沿用当前版本） */
  create: (body) => http.post('/admin/rules', body),
  /** 切换生效版本（含回切历史版本） */
  activate: (id) => http.post(`/admin/rules/${id}/activate`)
}
