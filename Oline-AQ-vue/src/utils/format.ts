export function formatTime(value?: string | null, fallback = '未设置') {
  return value ? new Date(value).toLocaleString() : fallback
}
