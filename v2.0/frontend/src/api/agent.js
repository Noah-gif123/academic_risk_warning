import { http } from './http'

/** P2-11/12：记忆与运行轨迹 */
export const agentApi = {
  memory: () => http.get('/student/memory'),
  deleteMemory: () => http.delete('/student/memory'),
  runs: (limit = 20) => http.get('/agent/runs?limit=' + limit),
  run: (id) => http.get('/agent/runs/' + id),
  chunk: (id) => http.get('/agent/kb/chunk/' + id)
}
