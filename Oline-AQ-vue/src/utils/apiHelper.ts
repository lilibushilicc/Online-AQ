import request from './request'

export function apiGet<T>(url: string, params?: Record<string, string | number | undefined>) {
  const clean = params ? Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined)) : params
  return request.get<unknown, { code: number; message: string; data: T }>(url, { params: clean }).then((res) => res.data)
}

export function apiPost<T>(url: string, data?: unknown) {
  return request.post<unknown, { code: number; message: string; data: T }>(url, data).then((res) => res.data)
}

export function apiPut<T = void>(url: string, data?: unknown) {
  return request.put<unknown, { code: number; message: string; data: T }>(url, data).then((res) => res.data)
}

export function apiDelete<T = void>(url: string) {
  return request.delete<unknown, { code: number; message: string; data: T }>(url).then((res) => res.data)
}
