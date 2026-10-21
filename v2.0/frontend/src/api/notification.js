import { http } from './http'

/** 四端共用 · 消息中心（数据来自 notification + notification_receipt） */
export const notificationApi = {
  /** 我的消息（分页）；unreadOnly=true 只看未读 */
  list: ({ unreadOnly = false, page = 1, size = 20 } = {}) => {
    const q = new URLSearchParams()
    if (unreadOnly) q.set('unreadOnly', 'true')
    q.set('page', String(page))
    q.set('size', String(size))
    return http.get('/notifications?' + q.toString())
  },
  /** 未读数（顶栏红点，轮询用） */
  unreadCount: () => http.get('/notifications/unread-count'),
  /** 标记单条已读 */
  markRead: (id) => http.post(`/notifications/${id}/read`),
  /** 全部标为已读 */
  markAllRead: () => http.post('/notifications/read-all')
}

export default notificationApi
