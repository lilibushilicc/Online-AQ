<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const testing = ref(false)
const testingAi = ref(false)
const testingEmailId = ref<number | null>(null)
const testingInline = ref(false)
const accounts = ref<api.SmtpAccountData[]>([])
const dialogVisible = ref(false)
const editingAccount = ref<Partial<api.SmtpAccountData>>({ host: '', port: 587, username: '', password: '', fromAddress: '', hourlyLimit: 100, dailyLimit: 1000 })
const isEditing = ref(false)

function progressColor(sent: number, limit: number) {
  const pct = limit > 0 ? sent / limit : 0
  if (pct >= 1) return ['#c0392b', '#e74c3c']
  if (pct >= 0.8) return ['#e67e22', '#f39c12']
  return ['var(--ink-green-light)', 'var(--ink-green)']
}

const emailStats = ref<{ todayCount: number; weekCount: number; totalCount: number; lastSendTime: string | null }>({
  todayCount: 0,
  weekCount: 0,
  totalCount: 0,
  lastSendTime: null,
})

async function loadEmailStats() {
  try {
    emailStats.value = await api.loadEmailStatsApi()
  } catch { }
}

async function loadAccounts() {
  try {
    accounts.value = await api.loadSmtpAccountsApi()
  } catch { }
}

function openAddDialog() {
  isEditing.value = false
  editingAccount.value = { host: '', port: 587, username: '', password: '', fromAddress: '', hourlyLimit: 100, dailyLimit: 1000 }
  dialogVisible.value = true
}

function openEditDialog(account: api.SmtpAccountData) {
  isEditing.value = true
  editingAccount.value = { ...account }
  dialogVisible.value = true
}

async function testInlineConnection() {
  const { host, port, username, password, fromAddress } = editingAccount.value
  if (!host || !username || !password || !fromAddress || !port) {
    ElMessage.warning('请先填写完整信息（服务器、端口、账号、密码、发件人）')
    return
  }
  testingInline.value = true
  try {
    await api.testSmtpInlineApi({ host, port, username, password, fromAddress })
    ElMessage.success('SMTP 连接测试成功')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '连接测试失败')
  } finally {
    testingInline.value = false
  }
}

async function saveAccount() {
  if (!editingAccount.value.host || !editingAccount.value.username || !editingAccount.value.fromAddress) {
    ElMessage.warning('请填写完整信息（密码可留空不修改）')
    return
  }
  try {
    if (isEditing.value && editingAccount.value.id) {
      const payload: Partial<api.SmtpAccountData> = { host: editingAccount.value.host, port: editingAccount.value.port, username: editingAccount.value.username, fromAddress: editingAccount.value.fromAddress }
      if (editingAccount.value.password) payload.password = editingAccount.value.password
      await api.updateSmtpAccountApi(editingAccount.value.id, payload)
    } else {
      await api.createSmtpAccountApi(editingAccount.value as api.SmtpAccountData)
    }
    ElMessage.success(isEditing.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    await loadAccounts()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function deleteAccount(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此 SMTP 账号？', '确认')
    await api.deleteSmtpAccountApi(id)
    ElMessage.success('删除成功')
    await loadAccounts()
  } catch { }
}

async function activateAccount(id: number) {
  try {
    await api.activateSmtpAccountApi(id)
    ElMessage.success('切换成功')
    await loadAccounts()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '切换失败')
  }
}

async function toggleEnabled(id: number) {
  try {
    await api.toggleSmtpEnabledApi(id)
    ElMessage.success('操作成功')
    await loadAccounts()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function testAccountConnection(id?: number) {
  testingEmailId.value = id ?? null
  try {
    const result = await api.testEmailApi(id ? { accountId: id } : undefined)
    ElMessage.success('SMTP 连接测试成功')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : 'SMTP 连接测试失败')
  } finally {
    testingEmailId.value = null
  }
}

const config = ref<Record<string, string>>({
  'storage.type': 'local',
  'r2.endpoint': '',
  'r2.access_key_id': '',
  'r2.secret_access_key': '',
  'r2.bucket_name': '',
  'ai.endpoint': '',
  'ai.api_key': '',
  'ai.model': '',
  'register.email.enabled': 'false',
  'smtp.subject': '',
  'smtp.system_name': '',
  'smtp.email_template': '',
  'login.logo.click.count': '3',
  'login.admin.method': 'both',
})

const emailEnabled = computed(() => config.value['register.email.enabled'] === 'true')
const hasActiveSmtp = computed(() => accounts.value.some(a => a.active && a.enabled !== false))
const smtpWarning = computed(() => emailEnabled.value && !hasActiveSmtp.value)

const activeTab = computed(() => route.path)

const tabs = [
  { label: '存储设置', name: '/admin/config/storage' },
  { label: 'AI 智能解析', name: '/admin/config/ai' },
  { label: '登录设置', name: '/admin/config/login' },
  { label: '邮箱注册设置', name: '/admin/config/email' },
]

async function load() {
  loading.value = true
  try {
    const data = await api.loadConfigApi()
    for (const key of Object.keys(config.value)) {
      if (data[key] !== undefined) {
        config.value[key] = data[key]
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
    await api.saveConfigApi({ ...config.value })
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
    await api.saveConfigApi({ ...config.value })
    const result = await api.testR2Api()
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
    await api.saveConfigApi({ ...config.value })
    const result = await api.testAiApi()
    ElMessage.success(`AI 连接测试成功：${result.result}`)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : 'AI 连接测试失败')
  } finally {
    testingAi.value = false
  }
}

function handleTabClick(tab: { paneName: string }) {
  router.push(tab.paneName)
}

onMounted(() => {
  load()
  loadEmailStats()
  loadAccounts()
})
</script>

<template>
  <el-tabs :model-value="activeTab" @tab-click="handleTabClick" style="max-width: 640px">
    <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name" />

    <template #extra>
      <el-button type="primary" size="small" :loading="loading" @click="save">保存设置</el-button>
    </template>
  </el-tabs>

  <el-card v-if="activeTab === '/admin/config/storage'" v-loading="loading" style="max-width: 640px">
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
          <el-input v-model="config['r2.endpoint']" placeholder="https://&lt;account_id&gt;.r2.cloudflarestorage.com" clearable />
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
    </el-form>
  </el-card>

  <el-card v-if="activeTab === '/admin/config/ai'" v-loading="loading" style="max-width: 640px">
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
        <el-button :loading="testingAi" @click="testAiConnection">保存并测试连接</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card v-if="activeTab === '/admin/config/login'" v-loading="loading" style="max-width: 640px">
    <el-form label-width="160px" label-position="left">
      <el-form-item label="管理员登录方式">
        <el-radio-group v-model="config['login.admin.method']">
          <el-radio value="direct">
            <span style="font-weight: 600;">首页直接登录</span>
            <span class="muted" style="display: block; font-size: 12px; margin-top: 2px;">登录页底部显示"管理员登录"按钮，隐藏 Logo 点击入口</span>
          </el-radio>
          <el-radio value="click_logo" style="margin-top: 12px;">
            <span style="font-weight: 600;">点击 Logo 登录</span>
            <span class="muted" style="display: block; font-size: 12px; margin-top: 2px;">隐藏"管理员登录"按钮，需连续点击左上角 Logo 进入</span>
          </el-radio>
          <el-radio value="both" style="margin-top: 12px;">
            <span style="font-weight: 600;">两种方式都支持</span>
            <span class="muted" style="display: block; font-size: 12px; margin-top: 2px;">按钮和 Logo 点击均可用（默认）</span>
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="config['login.admin.method'] !== 'direct'" label="Logo 点击触发次数">
        <div style="display: flex; align-items: center; gap: 12px;">
          <el-input-number v-model="config['login.logo.click.count']" :min="1" :max="20" />
          <span class="muted" style="font-size: 13px;">次连续点击（2 秒内）</span>
        </div>
        <div class="muted" style="font-size: 12px; margin-top: 4px;">
          设置连续点击左上角 Logo 多少次后弹出管理员登录弹窗，默认 3 次。
        </div>
      </el-form-item>
      <div v-if="config['login.admin.method'] === 'direct'" class="muted" style="font-size: 13px; padding: 12px 16px; background: var(--paper-warm); border-radius: var(--radius);">
        当前为"首页直接登录"模式，左上角 Logo 点击不会触发管理员登录弹窗。
      </div>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="save">保存登录设置</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card v-if="activeTab === '/admin/config/email'" v-loading="loading" style="max-width: 960px">
    <p class="muted" style="margin-bottom: 16px; font-size: 13px">
      开启后学生可在登录页使用邮箱注册账号。需先配置 SMTP 服务用于发送验证码邮件。
    </p>

    <div class="email-stats-bar">
      <div class="stat-item">
        <span class="stat-label">今日发送</span>
        <span class="stat-value">{{ emailStats.todayCount }}</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-label">本周发送</span>
        <span class="stat-value">{{ emailStats.weekCount }}</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-label">累计发送</span>
        <span class="stat-value">{{ emailStats.totalCount }}</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item" style="flex: 1;">
        <span class="stat-label">最近一次</span>
        <span class="stat-time">{{ emailStats.lastSendTime ? new Date(emailStats.lastSendTime).toLocaleString('zh-CN') : '暂无' }}</span>
      </div>
    </div>

    <el-alert v-if="smtpWarning" type="error" title="邮箱注册已开启，但没有激活的 SMTP 账号" :closable="false" show-icon style="margin-bottom: 16px;">
      <template #default>
        请先添加 SMTP 账号并点击<strong>「切换为此账号」</strong>激活，否则注册页面发送验证码将失败。
      </template>
    </el-alert>

    <el-form label-width="120px" label-position="left">
      <el-form-item label="开启邮箱注册">
        <el-switch v-model="config['register.email.enabled']" active-value="true" inactive-value="false" />
      </el-form-item>

      <el-divider content-position="left">SMTP 账号</el-divider>

      <div v-if="!accounts.length" class="smtp-empty fade-in">
        <p>暂无 SMTP 账号，添加一个以启用邮箱注册功能</p>
      </div>

      <div v-else class="smtp-grid">
        <div
          v-for="(row, idx) in accounts"
          :key="row.id"
          class="smtp-card"
          :class="{ 'smtp-card--disabled': row.enabled === false, 'fade-in': true }"
          :style="{ animationDelay: `${idx * 0.04}s` }"
        >
          <!-- 头部：发件人 + 状态 -->
          <div class="smtp-card-head">
            <div class="smtp-card-from">{{ row.fromAddress }}</div>
            <div class="smtp-card-status">
              <el-tag v-if="row.active && row.enabled !== false" type="success" size="small" effect="dark">当前</el-tag>
              <el-tag v-else-if="row.enabled === false" type="danger" size="small" effect="plain">已禁用</el-tag>
              <el-tag v-else type="info" size="small" effect="plain">备用</el-tag>
            </div>
          </div>

          <!-- 服务器 -->
          <div class="smtp-card-server">{{ row.host }}:{{ row.port }}</div>

          <!-- 发送量进度条 -->
          <div class="smtp-card-limits">
            <div class="smtp-card-limit-row">
              <el-progress
                :percentage="Math.min(100, Math.round(((row.hourlySent ?? 0) / (row.hourlyLimit || 1)) * 100))"
                :stroke-width="6"
                :color="progressColor((row.hourlySent ?? 0), row.hourlyLimit || 0)"
                :format="() => ''"
              />
              <span class="smtp-card-limit-label">每小时 {{ row.hourlyLimit ? `${row.hourlySent ?? 0}/${row.hourlyLimit}` : '不限' }}</span>
            </div>
            <div class="smtp-card-limit-row">
              <el-progress
                :percentage="Math.min(100, Math.round(((row.dailySent ?? 0) / (row.dailyLimit || 1)) * 100))"
                :stroke-width="6"
                :color="progressColor((row.dailySent ?? 0), row.dailyLimit || 0)"
                :format="() => ''"
              />
              <span class="smtp-card-limit-label">每日 {{ row.dailyLimit ? `${row.dailySent ?? 0}/${row.dailyLimit}` : '不限' }}</span>
            </div>
          </div>

          <!-- 操作栏 -->
          <div class="smtp-card-actions">
            <div class="smtp-card-toggle">
              <el-switch :model-value="row.enabled !== false" size="small" @click="toggleEnabled(row.id!)" />
              <span class="smtp-toggle-label">{{ row.enabled !== false ? '已启用' : '已禁用' }}</span>
            </div>
            <div class="smtp-card-btns">
              <el-button text size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button text size="small" :loading="testingEmailId === row.id" @click="testAccountConnection(row.id)">测试</el-button>
              <el-button v-if="!row.active && row.enabled !== false" text size="small" type="primary" @click="activateAccount(row.id!)">切换为此账号</el-button>
              <el-button v-if="row.active && row.enabled === false" text size="small" type="info" disabled>已禁用</el-button>
              <el-button text size="small" type="danger" @click="deleteAccount(row.id!)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <div style="margin-top: 16px;">
        <el-button size="small" @click="openAddDialog">+ 添加 SMTP 账号</el-button>
      </div>

      <el-divider content-position="left">邮件内容自定义</el-divider>

      <el-form-item label="系统名称">
        <div style="display: flex; gap: 8px; width: 100%; align-items: center;">
          <el-input v-model="config['smtp.system_name']" placeholder="Online-AQ 智能在线答题系统" style="flex: 1;" clearable />
          <span style="color: var(--muted); flex-shrink: 0;">标题</span>
          <el-input v-model="config['smtp.subject']" placeholder="${systemName} - 邮箱验证" style="flex: 1;" clearable />
        </div>
        <span class="muted" style="font-size: 12px; margin-top: 4px; display: block;">可用占位符：${systemName}（系统名称）${code}（验证码）</span>
      </el-form-item>
      <el-form-item label="邮件模板">
        <el-input v-model="config['smtp.email_template']" type="textarea" :rows="10" placeholder="留空使用默认模板" style="font-family: monospace; font-size: 12px;" />
        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 4px;">
          <span class="muted" style="font-size: 12px;">可用占位符：${code} ${systemName}</span>
          <el-button link size="small" @click="config['smtp.email_template'] = ''">恢复默认模板</el-button>
        </div>
      </el-form-item>
    </el-form>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑 SMTP 账号' : '添加 SMTP 账号'" width="480px">
    <el-form label-width="110px" label-position="left">
      <el-form-item label="SMTP 服务器">
        <div style="display: flex; gap: 8px; width: 100%; align-items: center;">
          <el-input v-model="editingAccount.host" placeholder="smtp.qq.com" style="flex: 1;" clearable />
          <span style="color: var(--muted); flex-shrink: 0;">端口</span>
          <el-input-number v-model="editingAccount.port" :min="1" :max="65535" style="width: 130px;" />
        </div>
      </el-form-item>
      <el-form-item label="账号">
        <el-input v-model="editingAccount.username" placeholder="SMTP 登录账号" clearable />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="editingAccount.password" type="password" placeholder="留空则不修改密码" show-password clearable />
      </el-form-item>
      <el-form-item label="发件人地址">
        <el-input v-model="editingAccount.fromAddress" placeholder="noreply@example.com" clearable />
      </el-form-item>
      <el-divider content-position="left">发送限制（0 或留空表示不限制）</el-divider>
      <el-form-item label="每小时上限">
        <el-input-number v-model="editingAccount.hourlyLimit" :min="0" style="width: 180px;" />
      </el-form-item>
      <el-form-item label="每日上限">
        <el-input-number v-model="editingAccount.dailyLimit" :min="0" style="width: 180px;" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :loading="testingInline" @click="testInlineConnection">测试连接</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="saveAccount">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
/* 禁用行 */
:deep(.disabled-row) {
  opacity: 0.45;
  background: var(--paper-dark);
}

/* 邮箱统计栏 */
.email-stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 16px 20px;
  background: linear-gradient(180deg, rgba(255,255,255,0.92), var(--paper-warm));
  border-radius: var(--radius-lg);
  margin-bottom: 18px;
  border: 1px solid var(--line-light);
  box-shadow: var(--shadow-sm);
}

.stat-item {
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-item:first-child { padding-left: 0; }

.stat-label {
  font-size: 11px;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
  font-weight: 600;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-green);
  font-family: var(--font-serif);
  line-height: 1.2;
}

.stat-time {
  font-size: 13px;
  color: var(--ink-secondary);
  line-height: 1.2;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: var(--line-light);
  flex-shrink: 0;
}

/* SMTP 卡片网格 */
.smtp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 12px;
}

.smtp-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.95), var(--paper-warm));
  border: 1px solid var(--line-light);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s, border-color 0.2s;
}

.smtp-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--line-medium);
}

.smtp-card--disabled {
  opacity: 0.5;
}

.smtp-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.smtp-card-from {
  font-weight: 700;
  font-size: 15px;
  color: var(--ink-primary);
  font-family: var(--font-serif);
  letter-spacing: -0.3px;
}

.smtp-card-status {
  flex-shrink: 0;
}

.smtp-card-server {
  font-size: 12px;
  color: var(--muted);
  font-family: var(--font-mono);
  margin-bottom: 14px;
}

.smtp-card-limits {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
  padding: 12px;
  background: rgba(0,0,0,0.02);
  border-radius: var(--radius);
}

.smtp-card-limit-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.smtp-card-limit-label {
  font-size: 11px;
  color: var(--ink-tertiary);
  font-weight: 500;
}

.smtp-card-limit-row :deep(.el-progress) {
  margin: 0 !important;
}
.smtp-card-limit-row :deep(.el-progress__text) {
  display: none !important;
}
.smtp-card-limit-row :deep(.el-progress-bar__outer) {
  border-radius: 99px !important;
  background: var(--paper-dark) !important;
}

.smtp-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.smtp-card-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
}

.smtp-toggle-label {
  font-size: 11px;
  color: var(--muted);
  font-weight: 500;
}

.smtp-card-btns {
  display: flex;
  gap: 2px;
}

.smtp-empty {
  padding: 32px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  background: var(--paper-warm);
  border: 1px dashed var(--line-medium);
  border-radius: var(--radius-lg);
}

.fade-in {
  animation: fadeSlideIn 0.35s ease both;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* 深色模式覆写（邮箱设置页特有样式） */
html.dark .email-stats-bar {
  background: linear-gradient(180deg, rgba(34,32,30,0.96), rgba(26,25,23,0.98));
}
html.dark .smtp-card {
  background: linear-gradient(180deg, rgba(34,32,30,0.96), rgba(26,25,23,0.98));
}
html.dark .smtp-card-limits {
  background: rgba(255,255,255,0.03);
}
html.dark .smtp-empty {
  border-color: var(--line);
}
</style>
