<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import AdminLayout from './AdminLayout.vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)

function beforeUpload(file: File) {
  const valid = file.name.endsWith('.txt') || file.name.endsWith('.docx')
  if (!valid) {
    ElMessage.warning('初版只支持 .txt 和 .docx 文件')
    return false
  }
  selectedFile.value = file
  return false
}

function onRemove() {
  selectedFile.value = null
}

async function parse() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择试题文件')
    return
  }
  uploading.value = true
  try {
    const count = await store.uploadAndParse(selectedFile.value)
    ElMessage.success(`上传并解析成功，共新增 ${count} 道题`)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <AdminLayout title="上传试题" subtitle="上传 TXT 或 DOCX 文件，后端读取文本并解析保存到题库。">
    <section class="panel">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :before-upload="beforeUpload"
        :on-remove="onRemove"
        accept=".txt,.docx"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽试题文件到此处，或点击选择</div>
        <template #tip>
          <div class="el-upload__tip">固定格式示例：题号、A-D 选项、答案：B</div>
        </template>
      </el-upload>
      <div style="margin-top: 18px">
        <el-button type="primary" :loading="uploading" @click="parse">上传并解析</el-button>
        <span class="muted" style="margin-left: 12px">最近解析：{{ store.latestParsedCount }} 道题</span>
      </div>
    </section>
  </AdminLayout>
</template>
