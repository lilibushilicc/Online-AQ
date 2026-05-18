<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from './AdminLayout.vue'
import { useExamStore, type Exam, type ExamHistory, type Question } from '@/stores/exam'

const store = useExamStore()
const previewVisible = ref(false)
const previewExam = ref<Exam | null>(null)
const previewQuestions = ref<Question[]>([])
const historyVisible = ref(false)
const currentHistory = ref<ExamHistory[]>([])
const form = reactive({
  examName: '新建基础测试',
  description: '用于课堂快速测验',
  duration: 30,
  startTime: '',
  endTime: '',
  allowRetake: false,
  questionIds: [] as number[],
})

const selectedQuestions = computed(() => store.questions.filter((question) => form.questionIds.includes(question.questionId)))
const selectedScore = computed(() => selectedQuestions.value.reduce((sum, question) => sum + Number(question.score), 0))
const publishedCount = computed(() => store.exams.filter((exam) => exam.status === 'published').length)
const draftCount = computed(() => store.exams.filter((exam) => exam.status === 'draft').length)
const closedCount = computed(() => store.exams.filter((exam) => exam.status === 'closed').length)

onMounted(async () => {
  await Promise.all([store.loadQuestions(), store.loadExams()])
  form.questionIds = store.questions.map((question) => question.questionId)
})

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString() : '未设置'
}

async function createExam() {
  if (!form.examName || form.questionIds.length === 0) {
    ElMessage.warning('请填写考试名称并至少选择一道题')
    return
  }

  await store.createExam({
    examName: form.examName,
    description: form.description,
    duration: form.duration,
    startTime: form.startTime || null,
    endTime: form.endTime || null,
    allowRetake: form.allowRetake,
    questionIds: form.questionIds,
  })
  ElMessage.success('考试已保存为草稿')
}

async function showPreview(row: Exam) {
  const detail = await store.getExamDetail(row.examId)
  previewExam.value = detail.exam
  previewQuestions.value = detail.questions
  previewVisible.value = true
}

async function showHistory(row: Exam) {
  currentHistory.value = await store.loadExamHistory(row.examId)
  previewExam.value = row
  historyVisible.value = true
}
</script>

<template>
  <AdminLayout title="考试管理" subtitle="支持设置考试开放时间、重考策略，并查看每场考试的历史记录。">
    <section class="stat-grid">
      <div class="stat-card"><span>全部考试</span><strong>{{ store.exams.length }}</strong></div>
      <div class="stat-card"><span>已发布</span><strong>{{ publishedCount }}</strong></div>
      <div class="stat-card"><span>草稿</span><strong>{{ draftCount }}</strong></div>
      <div class="stat-card"><span>已关闭</span><strong>{{ closedCount }}</strong></div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>创建考试</h3>
        <span class="muted">已选 {{ form.questionIds.length }} 道题，预计 {{ selectedScore }} 分</span>
      </div>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="考试名称"><el-input v-model="form.examName" /></el-form-item>
          <el-form-item label="考试说明"><el-input v-model="form.description" /></el-form-item>
          <el-form-item label="时长（分钟）"><el-input-number v-model="form.duration" :min="5" :max="180" style="width: 100%" /></el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="重考策略">
            <el-switch v-model="form.allowRetake" active-text="允许重考" inactive-text="仅允许一次提交" />
          </el-form-item>
        </div>

        <el-form-item label="选择题目">
          <el-checkbox-group v-model="form.questionIds">
            <el-checkbox v-for="question in store.questions" :key="question.questionId" :value="question.questionId">
              {{ question.questionContent }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <div class="status-line">
          <div class="status-box"><span class="muted">题目数量</span><b>{{ selectedQuestions.length }} 道</b></div>
          <div class="status-box"><span class="muted">试卷总分</span><b>{{ selectedScore }} 分</b></div>
          <div class="status-box"><span class="muted">重考策略</span><b>{{ form.allowRetake ? '允许重考' : '仅一次提交' }}</b></div>
        </div>
        <el-button type="primary" style="margin-top: 16px" @click="createExam">保存考试</el-button>
      </el-form>
    </section>

    <section class="exam-grid">
      <article v-for="exam in store.exams" :key="exam.examId" class="exam-tile">
        <div class="toolbar" style="margin-bottom: 0">
          <div>
            <h3>{{ exam.examName }}</h3>
            <p class="muted">{{ exam.description || '暂无考试说明' }}</p>
          </div>
          <el-tag :type="exam.status === 'published' ? 'success' : exam.status === 'closed' ? 'info' : 'warning'">
            {{ exam.status }}
          </el-tag>
        </div>
        <div class="status-line">
          <div class="status-box"><span class="muted">时长</span><b>{{ exam.duration }} 分钟</b></div>
          <div class="status-box"><span class="muted">总分</span><b>{{ exam.totalScore }} 分</b></div>
          <div class="status-box"><span class="muted">重考</span><b>{{ exam.allowRetake ? '允许' : '禁止' }}</b></div>
        </div>
        <div class="status-line">
          <div class="status-box"><span class="muted">开始时间</span><b>{{ formatTime(exam.startTime) }}</b></div>
          <div class="status-box"><span class="muted">结束时间</span><b>{{ formatTime(exam.endTime) }}</b></div>
          <div class="status-box"><span class="muted">考试ID</span><b>{{ exam.examId }}</b></div>
        </div>
        <div class="tag-row">
          <el-button type="success" plain @click="showPreview(exam)">预览试卷</el-button>
          <el-button type="info" plain @click="showHistory(exam)">历史记录</el-button>
          <el-button type="primary" plain @click="store.publishExam(exam.examId)">发布</el-button>
          <el-button type="warning" plain @click="store.closeExam(exam.examId)">关闭</el-button>
        </div>
      </article>
      <el-empty v-if="store.exams.length === 0" description="暂无考试，请先创建考试" />
    </section>

    <el-drawer v-model="previewVisible" title="试卷预览" size="42%">
      <template v-if="previewExam">
        <h3>{{ previewExam.examName }}</h3>
        <p class="muted">{{ previewExam.description }}</p>
        <el-divider />
        <div class="question-list">
          <article v-for="(question, index) in previewQuestions" :key="question.questionId" class="question-card">
            <strong>{{ index + 1 }}. {{ question.questionContent }}</strong>
            <div class="option-grid">
              <span>A. {{ question.optionA }}</span>
              <span>B. {{ question.optionB }}</span>
              <span v-if="question.optionC">C. {{ question.optionC }}</span>
              <span v-if="question.optionD">D. {{ question.optionD }}</span>
            </div>
            <p class="muted">答案：{{ question.correctAnswer }}，分值：{{ question.score }}</p>
          </article>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="historyVisible" title="考试历史记录" size="42%">
      <template v-if="previewExam">
        <h3>{{ previewExam.examName }}</h3>
        <p class="muted">查看该考试的创建、发布、关闭和提交历史。</p>
        <el-divider />
        <div class="question-list">
          <article v-for="history in currentHistory" :key="history.historyId" class="question-card">
            <div class="toolbar" style="margin-bottom: 6px">
              <strong>{{ history.actionType }}</strong>
              <span class="muted">{{ history.operatorName || `用户 #${history.operatorId ?? '-'}` }} · {{ new Date(history.createTime).toLocaleString() }}</span>
            </div>
            <p class="muted">{{ history.actionDetail }}</p>
          </article>
          <el-empty v-if="currentHistory.length === 0" description="暂无历史记录" />
        </div>
      </template>
    </el-drawer>
  </AdminLayout>
</template>
