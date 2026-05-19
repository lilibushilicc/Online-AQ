<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, MagicStick, InfoFilled, Delete, FolderOpened, DocumentChecked } from '@element-plus/icons-vue'
import { useExamStore, type UploadFileItem } from '@/stores/exam'

type ParseMode = 'regex' | 'ai'

interface FormatGuide {
  title: string
  description: string
  sample: string
  tip: string
}

const store = useExamStore()
const selectedFile = ref<File | null>(null)
const category = ref('')
const parseMode = ref<ParseMode>('regex')
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadFiles = ref<UploadFileItem[]>([])
const loadingFiles = ref(false)
const isDragOver = ref(false)

const parseModeOptions = [
  { label: '正则解析', value: 'regex' },
  { label: 'AI 智能解析', value: 'ai' },
]

const formatGuides: FormatGuide[] = [
  {
    title: '选择题',
    description: '适合单选题与多选式判断题，支持章节标题和无数字编号题干。',
    sample: `Spring 框架的核心两大特性是（）\nA. 反射和注解\nB. IOC 控制反转、AOP 面向切面编程\nC. MVC 和 ORM\nD. 依赖注入和序列化\n答案：B`,
    tip: '每道题保持“题干 + 选项 + 答案”结构，答案单独一行。',
  },
  {
    title: '简答题',
    description: '支持“答案：”后换行，多段答案会合并为同一道题的参考答案。',
    sample: `简述什么是 IOC（控制反转），有什么好处？\n答案：\nIOC 即控制反转，是把对象的创建、管理、依赖关系交给 Spring 容器控制。\n好处：解耦代码、便于测试。`,
    tip: '答案正文可以换行，不需要强行压成一行。',
  },
  {
    title: '章节式题库',
    description: '可保留“一、选择题”“二、简答题”这类章节标题，解析时会自动跳过。',
    sample: `一、选择题（5题）\nSpring AOP 中，用来定义切面逻辑的注解是（）\nA. @Controller\nB. @Aspect\nC. @Service\nD. @Repository\n答案：B`,
    tip: '推荐同一份文件内不同题型之间保留空行，便于人工检查。',
  },
]

const useAi = computed(() => parseMode.value === 'ai')
const parseModeTitle = computed(() => useAi.value ? 'AI 智能解析' : '正则解析')
const parseModeHint = computed(() => useAi.value
  ? '适合格式混杂、章节式、答案跨行的题库文本。若 AI 返回漏题，后端会自动尝试本地结构化解析兜底。'
  : '适合结构清晰的 TXT / DOCX 题库。题干、选项、答案越规范，解析越稳定。')
const latestParsedText = computed(() => store.latestParsedCount > 0 ? `${store.latestParsedCount} 道题` : '暂无记录')
const selectedFileName = computed(() => selectedFile.value?.name || '未选择文件')
const selectedFileSize = computed(() => selectedFile.value ? formatFileSize(selectedFile.value.size) : '等待上传')
const totalImportedQuestions = computed(() => uploadFiles.value.reduce((sum, item) => sum + item.questionCount, 0))

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

function validate(file: File | null): boolean {
  if (!file) return false
  const valid = file.name.endsWith('.txt') || file.name.endsWith('.docx')
  if (!valid) ElMessage.warning('仅支持 .txt 和 .docx 文件')
  return valid
}

function setSelectedFile(file: File | null) {
  if (!file || !validate(file)) return
  selectedFile.value = file
}

function resetFilePicker() {
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function openFilePicker() {
  fileInput.value?.click()
}

function clearSelectedFile() {
  selectedFile.value = null
  resetFilePicker()
}

function onDrop(event: DragEvent) {
  isDragOver.value = false
  setSelectedFile(event.dataTransfer?.files[0] ?? null)
}

function onInputChange(event: Event) {
  const input = event.target as HTMLInputElement
  setSelectedFile(input.files?.[0] ?? null)
}

async function parse() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择试题文件')
    return
  }
  uploading.value = true
  try {
    const count = await store.uploadAndParse(selectedFile.value, category.value.trim() || undefined, useAi.value)
    const mode = parseModeTitle.value
    clearSelectedFile()
    ElMessage.success(`[${mode}] 上传并解析成功，共新增 ${count} 道题`)
    await loadFiles()
  } finally {
    uploading.value = false
  }
}

async function loadFiles() {
  loadingFiles.value = true
  try {
    uploadFiles.value = await store.loadUploadFiles()
  } finally {
    loadingFiles.value = false
  }
}

async function handleDeleteFile(fileId: number, fileName: string) {
  try {
    await ElMessageBox.confirm(`确认删除“${fileName}”及其关联的所有题目？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await store.deleteFile(fileId)
    uploadFiles.value = uploadFiles.value.filter((item) => item.fileId !== fileId)
    ElMessage.success('文件已删除')
  } catch {
    // cancelled
  }
}

onMounted(loadFiles)
</script>

<template>
    <section class="upload-grid">
      <el-card class="upload-card">
        <template #header>
          <div class="section-head">
            <div>
              <div class="section-title">导入文件</div>
              <div class="section-subtitle">选择分类、确认解析模式后执行上传</div>
            </div>
            <el-tag type="info" effect="plain">{{ parseModeTitle }}</el-tag>
          </div>
        </template>

        <div
          class="drop-zone"
          :class="{ 'drop-zone--over': isDragOver, 'drop-zone--ready': !!selectedFile }"
          @dragover.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @drop.prevent="onDrop"
          @click="openFilePicker"
        >
          <el-icon class="drop-icon"><UploadFilled /></el-icon>
          <div class="drop-text">{{ selectedFile ? '文件已就绪，点击可重新选择' : '拖拽试题文件到此处，或点击选择文件' }}</div>
          <div class="drop-hint">支持 .txt 与 .docx，章节标题与多行答案可直接保留</div>
          <input
            ref="fileInput"
            type="file"
            accept=".txt,.docx"
            class="file-input"
            @change="onInputChange"
          />
        </div>

        <div class="file-preview">
          <div class="file-preview__main">
            <span class="file-preview__icon">
              <el-icon><DocumentChecked /></el-icon>
            </span>
            <div>
              <div class="file-preview__name">{{ selectedFileName }}</div>
              <div class="file-preview__meta">{{ selectedFileSize }}</div>
            </div>
          </div>
          <el-button link type="danger" :disabled="!selectedFile" @click="clearSelectedFile">清空</el-button>
        </div>

        <div class="form-grid">
          <el-input
            v-model="category"
            placeholder="题目分类，可选，如：Java / 数学 / 期中复习"
            clearable
          />
          <el-segmented v-model="parseMode" :options="parseModeOptions" class="mode-segmented" />
        </div>

        <div class="action-row">
          <el-button type="primary" :loading="uploading" @click="parse">
            <el-icon v-if="useAi"><MagicStick /></el-icon>
            <el-icon v-else><UploadFilled /></el-icon>
            <span>上传并解析</span>
          </el-button>
          <div class="action-meta">
            <span>最近解析：{{ latestParsedText }}</span>
            <span>当前模式：{{ parseModeTitle }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="status-card">
        <template #header>
          <div class="section-head">
            <div>
              <div class="section-title">解析策略</div>
              <div class="section-subtitle">根据题库质量选择更合适的解析路径</div>
            </div>
            <el-icon><InfoFilled /></el-icon>
          </div>
        </template>

        <div class="status-panel">
          <div class="status-badge" :class="{ 'status-badge--ai': useAi }">
            {{ parseModeTitle }}
          </div>
          <p class="status-copy">{{ parseModeHint }}</p>

          <div class="stats-grid">
            <div class="stat-box">
              <span class="stat-label">已上传文件</span>
              <strong>{{ uploadFiles.length }}</strong>
            </div>
            <div class="stat-box">
              <span class="stat-label">累计题目</span>
              <strong>{{ totalImportedQuestions }}</strong>
            </div>
            <div class="stat-box">
              <span class="stat-label">支持格式</span>
              <strong>TXT / DOCX</strong>
            </div>
            <div class="stat-box">
              <span class="stat-label">推荐结构</span>
              <strong>题干 + 答案</strong>
            </div>
          </div>

          <div class="checklist">
            <div class="checklist-item">章节标题可保留，解析时会自动跳过。</div>
            <div class="checklist-item">简答题答案支持“答案：”后换行。</div>
            <div class="checklist-item">选择题推荐保持 A/B/C/D 单独成行。</div>
          </div>
        </div>
      </el-card>
    </section>

    <el-card>
      <template #header>
        <div class="section-head">
          <div>
            <div class="section-title">最近导入记录</div>
            <div class="section-subtitle">可查看已解析文件，并删除对应题目来源</div>
          </div>
          <el-tag type="info" effect="plain">{{ uploadFiles.length }} 个文件</el-tag>
        </div>
      </template>

      <div v-if="loadingFiles" class="table-placeholder">正在加载上传记录...</div>
      <div v-else-if="uploadFiles.length === 0" class="table-placeholder">暂无已上传文件</div>
      <el-table v-else :data="uploadFiles" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column prop="fileType" label="格式" width="90">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.fileType.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="190">
          <template #default="{ row }">
            {{ new Date(row.createTime).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDeleteFile(row.fileId, row.fileName)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <template #header>
        <div class="section-head">
          <div>
            <div class="section-title">格式参考</div>
            <div class="section-subtitle">上传前先对照模板，能明显减少错题和漏题</div>
          </div>
          <el-icon><FolderOpened /></el-icon>
        </div>
      </template>

      <div class="guide-grid">
        <article v-for="guide in formatGuides" :key="guide.title" class="guide-card">
          <div class="guide-card__title">{{ guide.title }}</div>
          <p class="guide-card__desc">{{ guide.description }}</p>
          <pre class="guide-card__sample">{{ guide.sample }}</pre>
          <div class="guide-card__tip">{{ guide.tip }}</div>
        </article>
      </div>
    </el-card>
</template>

<style scoped>
.upload-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.9fr);
  gap: 20px;
}

.upload-card,
.status-card {
  height: 100%;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.section-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--muted);
  line-height: 1.5;
}

.drop-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 220px;
  padding: 28px 24px;
  border: 1.5px dashed var(--line);
  border-radius: var(--radius);
  background:
    linear-gradient(180deg, rgba(255,255,255,0.72), rgba(245,243,238,0.96)),
    repeating-linear-gradient(135deg, rgba(45,90,71,0.03), rgba(45,90,71,0.03) 12px, transparent 12px, transparent 24px);
  cursor: pointer;
  transition: border-color var(--duration), transform var(--duration), box-shadow var(--duration), background var(--duration);
}

.drop-zone:hover,
.drop-zone--over {
  border-color: var(--ink-green);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.drop-zone--ready {
  background:
    linear-gradient(180deg, rgba(232,240,236,0.95), rgba(250,249,246,0.98)),
    repeating-linear-gradient(135deg, rgba(45,90,71,0.04), rgba(45,90,71,0.04) 12px, transparent 12px, transparent 24px);
}

.drop-icon {
  font-size: 42px;
  color: var(--ink-green);
}

.drop-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  text-align: center;
}

.drop-hint {
  max-width: 440px;
  font-size: 12px;
  color: var(--muted);
  text-align: center;
  line-height: 1.7;
}

.file-input {
  display: none;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  background: var(--paper-warm);
}

.file-preview__main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.file-preview__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: var(--accent-light);
  color: var(--ink-green);
  flex-shrink: 0;
}

.file-preview__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  word-break: break-all;
}

.file-preview__meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--muted);
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  margin-top: 16px;
}

.mode-segmented {
  min-width: 240px;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.action-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  font-size: 12px;
  color: var(--muted);
}

.status-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--paper-dark);
  color: var(--ink-secondary);
  font-size: 12px;
  font-weight: 600;
}

.status-badge--ai {
  background: var(--accent-light);
  color: var(--ink-green);
}

.status-copy {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-secondary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-box {
  padding: 14px;
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, var(--paper) 0%, var(--paper-warm) 100%);
}

.stat-box strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.2;
  word-break: break-word;
}

.stat-label {
  font-size: 11px;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.checklist {
  display: grid;
  gap: 10px;
}

.checklist-item {
  position: relative;
  padding-left: 14px;
  font-size: 13px;
  color: var(--ink-secondary);
  line-height: 1.6;
}

.checklist-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--ink-green);
}

.table-placeholder {
  padding: 28px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.guide-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--line-light);
  border-radius: var(--radius);
  background: linear-gradient(180deg, var(--paper) 0%, var(--paper-warm) 100%);
}

.guide-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.guide-card__desc {
  margin: 0;
  font-size: 12px;
  color: var(--ink-secondary);
  line-height: 1.7;
}

.guide-card__sample {
  margin: 0;
  padding: 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  background: rgba(255,255,255,0.7);
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
}

.guide-card__tip {
  padding-left: 10px;
  border-left: 3px solid var(--ink-green);
  font-size: 12px;
  line-height: 1.7;
  color: var(--muted);
}

@media (max-width: 1080px) {
  .upload-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .drop-zone {
    min-height: 180px;
    padding: 24px 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .mode-segmented {
    min-width: 100%;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .file-preview {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
