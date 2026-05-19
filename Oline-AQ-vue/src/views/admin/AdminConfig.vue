<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, MagicStick } from '@element-plus/icons-vue'
import { useExamStore } from '@/stores/exam'

const store = useExamStore()
const loading = ref(false)
const testing = ref(false)
const testingAi = ref(false)

const config = ref({
  'storage.type': 'local',
  'r2.endpoint': '',
  'r2.access_key_id': '',
  'r2.secret_access_key': '',
  'r2.bucket_name': '',
  'ai.endpoint': '',
  'ai.api_key': '',
  'ai.model': '',
})

async function load() {
  loading.value = true
  try {
    const data = await store.loadConfig()
    for (const key of Object.keys(config.value)) {
      if (data[key] !== undefined) {
        config.value[key as keyof typeof config.value] = data[key]
      }
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  if (config.value['storage.type'] === 'r2') {
    const missing = []
    if (!config.value['r2.endpoint']) missing.push('Endpoint')
    if (!config.value['r2.access_key_id']) missing.push('Access Key ID')
    if (!config.value['r2.secret_access_key']) missing.push('Secret Access Key')
    if (!config.value['r2.bucket_name']) missing.push('Bucket Name')
    if (missing.length) {
      ElMessage.warning(`请填写完整的 R2 配置，缺少: ${missing.join(', ')}`)
      return
    }
  }
  loading.value = true
  try {
    await store.saveConfig({ ...config.value })
    ElMessage.success('保存成功')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

async function testConnection() {
  const missing = []
  if (!config.value['r2.endpoint']) missing.push('Endpoint')
  if (!config.value['r2.access_key_id']) missing.push('Access Key ID')
  if (!config.value['r2.secret_access_key']) missing.push('Secret Access Key')
  if (!config.value['r2.bucket_name']) missing.push('Bucket Name')
  if (missing.length) {
    ElMessage.warning(`请先填写完整的 R2 配置，缺少: ${missing.join(', ')}`)
    return
  }
  testing.value = true
  try {
    await store.saveConfig({ ...config.value })
    const result = await store.testR2()
    if (result.result === 'ok') {
      ElMessage.success('R2 连接测试成功')
    } else {
      ElMessage.error(result.result)
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '连接测试失败')
  } finally {
    testing.value = false
  }
}

async function testAiConnection() {
  if (!config.value['ai.endpoint'] || !config.value['ai.api_key']) {
    ElMessage.warning('请先填写 AI Endpoint 和 API Key')
    return
  }
  testingAi.value = true
  try {
    await store.saveConfig({ ...config.value })
    const result = await store.testAi()
    ElMessage.success(`AI 连接测试成功：${result.result}`)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : 'AI 连接测试失败')
  } finally {
    testingAi.value = false
  }
}

onMounted(load)
</script>

<template>
    <el-card v-loading="loading" style="max-width: 640px; margin-bottom: 14px">
      <template #header>
        <div style="display: flex; align-items: center; gap: 8px">
          <el-icon><Setting /></el-icon>
          <span>存储设置</span>
        </div>
      </template>

      <el-form label-width="140px" label-position="left">
        <el-form-item label="存储方式">
          <el-radio-group v-model="config['storage.type']">
            <el-radio value="local">本地磁盘</el-radio>
            <el-radio value="r2">Cloudflare R2</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="config['storage.type'] === 'r2'">
          <el-divider content-position="left">R2 连接参数</el-divider>

          <el-form-item label="Endpoint">
            <el-input v-model="config['r2.endpoint']" placeholder="https://<account_id>.r2.cloudflarestorage.com" clearable />
          </el-form-item>
          <el-form-item label="Access Key ID">
            <el-input v-model="config['r2.access_key_id']" placeholder="R2 访问密钥 ID" clearable />
          </el-form-item>
          <el-form-item label="Secret Access Key">
            <el-input v-model="config['r2.secret_access_key']" type="password" placeholder="R2 机密访问密钥" show-password clearable />
          </el-form-item>
          <el-form-item label="Bucket Name">
            <el-input v-model="config['r2.bucket_name']" placeholder="存储桶名称" clearable />
          </el-form-item>

          <el-form-item label=" ">
            <el-button :loading="testing" @click="testConnection">保存并测试连接</el-button>
          </el-form-item>
        </template>

        <el-form-item label=" ">
          <el-button type="primary" :loading="loading" @click="save">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="max-width: 640px">
      <template #header>
        <div style="display: flex; align-items: center; gap: 8px">
          <el-icon><MagicStick /></el-icon>
          <span>AI 智能解析</span>
        </div>
      </template>
      <p class="muted" style="margin-bottom: 16px; font-size: 13px">
        配置 AI 接口后可对上传的试题文件进行智能解析，支持任意排版格式，准确率远高于正则解析。
        兼容 OpenAI / DeepSeek 等兼容接口。
      </p>
      <el-form label-width="140px" label-position="left">
        <el-form-item label="API Endpoint">
          <el-input v-model="config['ai.endpoint']" placeholder="https://api.deepseek.com/v1/chat/completions" clearable />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="config['ai.api_key']" type="password" placeholder="sk-..." show-password clearable />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="config['ai.model']" placeholder="deepseek-chat（留空默认）" clearable />
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="primary" :loading="loading" @click="save">保存设置</el-button>
          <el-button :loading="testingAi" @click="testAiConnection">保存并测试连接</el-button>
        </el-form-item>
      </el-form>
    </el-card>
</template>
