<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api'
import type { ReviewItem } from '@/types'

const items = ref<ReviewItem[]>([])
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    items.value = await api.getPendingReviewsApi()
  } catch {
    ElMessage.error('加载待评分列表失败')
  } finally {
    loading.value = false
  }
}

async function doReview(item: ReviewItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入该题的得分', '评分', {
      inputValue: String(item.score || 0),
      inputPattern: /^\d+(\.\d{1,2})?$/,
      inputErrorMessage: '请输入有效数字',
    })
    await api.reviewAnswerApi(item.answerId, Number(value))
    ElMessage.success('评分成功')
    await load()
  } catch {
    // cancelled
  }
}
</script>

<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <strong>待评分简答题 ({{ items.length }})</strong>
        <el-button size="small" @click="load" :loading="loading">刷新</el-button>
      </div>
    </template>

    <el-empty v-if="!items.length && !loading" description="暂无待评分的简答题" />

    <div v-else class="review-list">
      <el-card v-for="item in items" :key="item.answerId" shadow="hover" style="margin-bottom: 14px">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
          <div>
            <el-tag size="small" type="warning" style="margin-right: 8px">{{ item.examName }}</el-tag>
            <el-tag size="small">{{ item.studentName }}</el-tag>
          </div>
          <span class="muted">{{ new Date(item.submitTime).toLocaleString() }}</span>
        </div>
        <p style="font-weight: 600; margin-bottom: 8px">{{ item.questionContent }}</p>
        <div style="display: flex; flex-wrap: wrap; gap: 8px; align-items: center">
          <el-tag>学生答案：{{ item.studentAnswer || '未作答' }}</el-tag>
          <el-tag type="success">参考答案：{{ item.correctAnswer }}</el-tag>
          <el-button type="primary" size="small" @click="doReview(item)">评分</el-button>
        </div>
      </el-card>
    </div>
  </el-card>
</template>
