<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api'
import type { Announcement } from '@/types'

const announcements = ref<Announcement[]>([])
const dialogVisible = ref(false)
const editing = ref<Announcement | null>(null)
const form = ref({ title: '', content: '', active: true })
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    announcements.value = await api.loadAnnouncementsApi()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { title: '', content: '', active: true }
  dialogVisible.value = true
}

function openEdit(ann: Announcement) {
  editing.value = ann
  form.value = { title: ann.title, content: ann.content, active: ann.active }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  try {
    if (editing.value) {
      await api.updateAnnouncementApi(editing.value.announcementId, form.value)
      ElMessage.success('更新成功')
    } else {
      await api.createAnnouncementApi(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function remove(ann: Announcement) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${ann.title}」？`, '确认删除')
    await api.deleteAnnouncementApi(ann.announcementId)
    ElMessage.success('删除成功')
    await load()
  } catch { /* cancelled */ }
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: flex-end; margin-bottom: 14px">
      <el-button type="primary" @click="openCreate">发布公告</el-button>
    </div>

    <el-table :data="announcements" v-loading="loading" stripe>
      <el-table-column prop="announcementId" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="content" label="内容" min-width="300">
        <template #default="{ row }">
          <span style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">{{ row.content }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'" size="small">{{ row.active ? '启用' : '关闭' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑公告' : '发布公告'" width="600px">
      <el-form label-position="top">
        <el-form-item label="公告标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.active" active-text="启用" inactive-text="关闭" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
