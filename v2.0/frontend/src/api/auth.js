import { http } from './http'

export const authApi = {
  captcha: () => http.get('/auth/captcha'),
  login: (loginNo, password, captchaId, captchaCode) =>
    http.post('/auth/login', { loginNo, password, captchaId, captchaCode }),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout'),
  changePassword: (oldPassword, newPassword) => http.post('/auth/change-password', { oldPassword, newPassword })
}
