import request from '@/utils/request'
import type { Role } from '@/stores/exam'

export function loginApi(data: { username: string; password: string; role: Role }) {
  return request.post('/auth/login', data)
}
