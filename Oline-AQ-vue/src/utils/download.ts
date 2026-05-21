import { ElMessage } from 'element-plus'
import request from './request'
import { cleanParams } from './apiHelper'

export function downloadFile(url: string, filename: string, params?: Record<string, string | number | undefined>) {
  return request
    .get(url, { params: cleanParams(params), responseType: 'blob' })
    .then((res) => {
      const blob = new Blob([res as unknown as BlobPart])
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      setTimeout(() => {
        document.body.removeChild(link)
        URL.revokeObjectURL(link.href)
      }, 5000)
    })
    .catch((err) => {
      const msg = err?.response?.data
        ? (err.response.data instanceof Blob
          ? '导出失败：服务器异常'
          : err.response.data?.message)
        : err?.message
      ElMessage.error(msg || '导出失败')
    })
}
