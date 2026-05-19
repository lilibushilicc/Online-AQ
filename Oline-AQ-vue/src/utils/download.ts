import request from './request'

export function downloadFile(url: string, filename: string, params?: Record<string, string | number | undefined>) {
  const clean = params
    ? Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== ''))
    : undefined
  return request
    .get(url, { params: clean, responseType: 'blob' })
    .then((res) => {
      const blob = new Blob([res as unknown as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename
      link.click()
      URL.revokeObjectURL(link.href)
    })
}
