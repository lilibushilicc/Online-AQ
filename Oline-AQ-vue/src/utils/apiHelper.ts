import request from './request'

export function cleanParams<T extends Record<string, any>>(params?: T) {
  return params
    ? (Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== '')) as Partial<T>)
    : undefined
}

export function apiGet<T>(url: string, params?: Record<string, string | number | undefined>) {
  return request.get<unknown, { code: number; message: string; data: T }>(url, { params: cleanParams(params) }).then((res) => res.data)
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
