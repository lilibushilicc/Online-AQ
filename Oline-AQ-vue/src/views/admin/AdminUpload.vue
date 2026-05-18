<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import AdminLayout from './AdminLayout.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()
const selectedFile = ref<File | null>(null)
const category = ref('')
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const isDragOver = ref(false)

function validate(file: File | null): boolean {
  if (!file) return false
  const valid = file.name.endsWith('.txt') || file.name.endsWith('.docx')
  if (!valid) ElMessage.warning('只支持 .txt 和 .docx 文件')
  return valid
}

function onDrop(e: DragEvent) {
  isDragOver.value = false
  const dt = e.dataTransfer
  const file = dt?.files[0]
  if (file && validate(file)) selectedFile.value = file
}

function onInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file && validate(file)) selectedFile.value = file
}

async function parse() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择试题文件')
    return
  }
  uploading.value = true
  try {
    const count = await store.uploadAndParse(selectedFile.value, category.value || undefined)
    selectedFile.value = null
    ElMessage.success(`上传并解析成功，共新增 ${count} 道题`)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <AdminLayout title="上传试题" subtitle="上传 TXT 或 DOCX 文件，可指定分类，后端自动解析并保存到题库。">
    <el-card>
      <div
        class="drop-zone"
        :class="{ 'drop-zone--over': isDragOver }"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="onDrop"
        @click="fileInput?.click()"
      >
        <el-icon class="drop-icon"><UploadFilled /></el-icon>
        <div class="drop-text">{{ selectedFile ? selectedFile.name : '拖拽试题文件到此处，或点击选择' }}</div>
        <div class="drop-hint">支持 .txt 和 .docx 格式，文件包含题号、A-D 选项和答案</div>
        <input
          ref="fileInput"
          type="file"
          accept=".txt,.docx"
          class="file-input"
          @change="onInputChange"
        />
      </div>
      <div style="margin-top: 16px; display: flex; align-items: center; gap: 12px">
        <el-input
          v-model="category"
          placeholder="题目分类（可选，如：Java / 数学）"
          clearable
          style="max-width: 300px"
        />
        <el-button type="primary" :loading="uploading" @click="parse">上传并解析</el-button>
        <span class="muted" style="font-size: 13px">最近解析：{{ store.latestParsedCount }} 道题</span>
      </div>
    </el-card>
  </AdminLayout>
</template>

<style scoped>
.drop-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 20px;
  border: 2px dashed var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--el-fill-color-lighter);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.drop-zone:hover,
.drop-zone--over {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.drop-icon {
  font-size: 40px;
  color: var(--el-text-color-placeholder);
}
.drop-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.drop-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.file-input {
  display: none;
}
</style>
