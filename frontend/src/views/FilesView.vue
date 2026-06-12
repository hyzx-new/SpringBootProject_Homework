<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Download, RefreshCw, Trash2, Upload } from 'lucide-vue-next'
import { http } from '../api/http'
import PaginationBar from '../components/PaginationBar.vue'

const rows = ref([])
const fileInput = ref(null)
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
const pageInfo = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 0
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const payload = await http.get('/files', {
      params: { page: pageInfo.page, size: pageInfo.size }
    })
    rows.value = payload.records
    pageInfo.page = payload.page
    pageInfo.size = payload.size
    pageInfo.total = payload.total
    pageInfo.pages = payload.pages
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function changePage(payload) {
  pageInfo.page = payload.page
  pageInfo.size = payload.size
  load()
}

async function uploadFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploading.value = true
  error.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    await http.post('/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    event.target.value = ''
    await load()
  } catch (err) {
    error.value = err.message
  } finally {
    uploading.value = false
  }
}

function download(row) {
  const link = document.createElement('a')
  link.href = row.downloadUrl
  link.download = row.originalName
  link.click()
}

async function remove(row) {
  if (!confirm(`确认删除文件「${row.originalName}」吗？`)) return
  error.value = ''
  try {
    await http.delete(`/files/${row.id}`)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>资料文件</h2>
      <div class="toolbar-actions">
        <input ref="fileInput" type="file" style="display: none" @change="uploadFile" />
        <button class="btn" @click="load">
          <RefreshCw :size="18" />
          <span>刷新</span>
        </button>
        <button class="btn primary" :disabled="uploading" @click="fileInput?.click()">
          <Upload :size="18" />
          <span>{{ uploading ? '上传中' : '上传文件' }}</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载文件...</div>
      <div v-else-if="rows.length === 0" class="empty">暂无上传文件</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文件名</th>
              <th>类型</th>
              <th>大小</th>
              <th>上传人</th>
              <th>上传时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td><strong>{{ row.originalName }}</strong></td>
              <td>{{ row.contentType || '-' }}</td>
              <td>{{ formatSize(row.fileSize) }}</td>
              <td>{{ row.uploaderName }}</td>
              <td>{{ row.createdAt?.replace('T', ' ') }}</td>
              <td>
                <div class="actions">
                  <button class="btn icon" title="下载" @click="download(row)">
                    <Download :size="17" />
                  </button>
                  <button v-if="row.canDelete" class="btn icon danger" title="删除" @click="remove(row)">
                    <Trash2 :size="17" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        v-if="!loading && pageInfo.total > 0"
        :page="pageInfo.page"
        :size="pageInfo.size"
        :total="pageInfo.total"
        :pages="pageInfo.pages"
        @change="changePage"
      />
    </section>
  </div>
</template>
