<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api'
import type { PracticeResult } from '@/types'

const history = ref<PracticeResult[]>([])
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    history.value = await api.getPracticeHistoryApi()
  } catch {
    ElMessage.error('加载练习历史失败')
  } finally {
    loading.value = false
  }
}

function accuracy(item: PracticeResult) {
  if (item.totalQuestions === 0) return 0
  return Math.round((item.correctCount / item.totalQuestions) * 100)
}
</script>

<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <strong>练习历史 ({{ history.length }})</strong>
        <el-button size="small" @click="load" :loading="loading">刷新</el-button>
      </div>
    </template>

    <el-empty v-if="!history.length && !loading" description="暂无练习记录，去在线做题页面开始练习吧" />

    <el-table v-else :data="history" stripe>
      <el-table-column label="练习时间" min-width="180">
        <template #default="{ row }">
          {{ new Date(row.submitTime).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="题目数" prop="totalQuestions" width="80" />
      <el-table-column label="正确数" prop="correctCount" width="80" />
      <el-table-column label="错误数" prop="wrongCount" width="80" />
      <el-table-column label="正确率" width="100">
        <template #default="{ row }">
          <el-tag :type="accuracy(row) >= 80 ? 'success' : accuracy(row) >= 60 ? 'warning' : 'danger'">
            {{ accuracy(row) }}%
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="80">
        <template #default="{ row }">
          <strong>{{ row.totalScore }}</strong>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
