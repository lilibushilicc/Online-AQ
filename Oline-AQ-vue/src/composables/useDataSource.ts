import { ref, type Ref } from 'vue'
import { ElMessage } from 'element-plus'

export function useDataSource<T>(fetcher: () => Promise<T>, errorMessage = '加载数据失败，请刷新重试') {
  const data: Ref<T | null> = ref(null) as Ref<T | null>
  const loading = ref(true)

  async function load() {
    loading.value = true
    try {
      data.value = await fetcher()
    } catch {
      ElMessage.error(errorMessage)
    } finally {
      loading.value = false
    }
  }

  return { data, loading, load }
}
