<script setup>
import { onMounted, reactive, ref } from 'vue'
import { MessageSquare, Plus, RefreshCw, Search, Send, Trash2, X } from 'lucide-vue-next'
import { http } from '../api/http'
import PaginationBar from '../components/PaginationBar.vue'

const rows = ref([])
const papers = ref([])
const detail = ref(null)
const keyword = ref('')
const filterPaperId = ref('')
const replyContent = ref('')
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
const discussionPageSizes = [3]
const pageInfo = reactive({
  page: 1,
  size: 3,
  total: 0,
  pages: 0
})
const form = reactive({
  title: '',
  content: '',
  paperId: ''
})

const roleNames = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

async function loadPapers() {
  try {
    const payload = await http.get('/papers', { params: { page: 1, size: 100 } })
    papers.value = payload.records
  } catch (err) {
    error.value = err.message
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const payload = await http.get('/discussions', {
      params: {
        keyword: keyword.value,
        paperId: filterPaperId.value || undefined,
        page: pageInfo.page,
        size: pageInfo.size
      }
    })
    rows.value = payload.records
    pageInfo.page = payload.page
    pageInfo.size = payload.size
    pageInfo.total = payload.total
    pageInfo.pages = payload.pages
    if (!detail.value && rows.value.length > 0) {
      await openDetail(rows.value[0])
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function search() {
  pageInfo.page = 1
  detail.value = null
  load()
}

function resetSearch() {
  keyword.value = ''
  filterPaperId.value = ''
  pageInfo.page = 1
  detail.value = null
  load()
}

function changePage(payload) {
  pageInfo.page = payload.page
  pageInfo.size = payload.size
  detail.value = null
  load()
}

async function openDetail(row) {
  error.value = ''
  try {
    detail.value = await http.get(`/discussions/${row.id}`)
    replyContent.value = ''
  } catch (err) {
    error.value = err.message
  }
}

function openCreate() {
  Object.assign(form, { title: '', content: '', paperId: filterPaperId.value || '' })
  modalOpen.value = true
}

async function createPost() {
  error.value = ''
  try {
    detail.value = await http.post('/discussions', {
      title: form.title,
      content: form.content,
      paperId: form.paperId || null
    })
    modalOpen.value = false
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function reply() {
  if (!detail.value || !replyContent.value.trim()) return
  error.value = ''
  try {
    detail.value = await http.post(`/discussions/${detail.value.post.id}/replies`, {
      content: replyContent.value
    })
    replyContent.value = ''
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function deletePost(post) {
  if (!confirm(`确认删除讨论「${post.title}」吗？`)) return
  error.value = ''
  try {
    await http.delete(`/discussions/${post.id}`)
    detail.value = null
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function deleteReply(replyRow) {
  if (!detail.value || !confirm('确认删除这条回复吗？')) return
  error.value = ''
  try {
    detail.value = await http.delete(`/discussions/${detail.value.post.id}/replies/${replyRow.id}`)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

function initialOf(name) {
  return (name || '用户').slice(0, 1)
}

function formatTime(value) {
  return value?.replace('T', ' ') || '-'
}

onMounted(async () => {
  await loadPapers()
  await load()
})
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>讨论区</h2>
      <div class="toolbar-actions">
        <div class="field" style="min-width: 220px">
          <input v-model.trim="keyword" class="input" placeholder="搜索标题或内容" @keyup.enter="search" />
        </div>
        <select v-model="filterPaperId" class="select" style="min-width: 220px" @change="search">
          <option value="">全部试卷</option>
          <option v-for="paper in papers" :key="paper.id" :value="paper.id">{{ paper.title }}</option>
        </select>
        <button class="btn" @click="search">
          <Search :size="18" />
          <span>搜索</span>
        </button>
        <button class="btn" @click="resetSearch">
          <RefreshCw :size="18" />
          <span>重置</span>
        </button>
        <button class="btn primary" @click="openCreate">
          <Plus :size="18" />
          <span>发起讨论</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="discussion-layout">
      <div class="panel discussion-list">
        <div v-if="loading" class="empty">正在加载讨论...</div>
        <div v-else-if="rows.length === 0" class="empty">暂无讨论</div>
        <template v-else>
          <button
            v-for="row in rows"
            :key="row.id"
            class="discussion-row"
            :class="{ active: detail?.post?.id === row.id }"
            @click="openDetail(row)"
          >
            <div class="discussion-row-main">
              <strong>{{ row.title }}</strong>
              <span v-if="row.paperTitle" class="badge">{{ row.paperTitle }}</span>
            </div>
            <p>{{ row.content }}</p>
            <div class="discussion-meta">
              <span>{{ row.authorName }} · {{ roleNames[row.authorRole] || row.authorRole }}</span>
              <span>{{ row.replyCount }} 条回复</span>
              <span>{{ formatTime(row.updatedAt) }}</span>
            </div>
          </button>
        </template>
        <PaginationBar
          v-if="!loading && pageInfo.total > 0"
          :page="pageInfo.page"
          :size="pageInfo.size"
          :total="pageInfo.total"
          :pages="pageInfo.pages"
          :size-options="discussionPageSizes"
          @change="changePage"
        />
      </div>

      <div class="panel pad discussion-detail">
        <div v-if="!detail" class="empty">选择一条讨论查看详情</div>
        <template v-else>
          <div class="discussion-detail-head">
            <div>
              <div class="discussion-author">
                <img v-if="detail.post.authorAvatarUrl" class="avatar" :src="detail.post.authorAvatarUrl" alt="" />
                <span v-else class="avatar fallback">{{ initialOf(detail.post.authorName) }}</span>
                <span>{{ detail.post.authorName }} · {{ roleNames[detail.post.authorRole] || detail.post.authorRole }}</span>
              </div>
              <h2>{{ detail.post.title }}</h2>
              <p class="muted">{{ formatTime(detail.post.createdAt) }}</p>
            </div>
            <button v-if="detail.post.canDelete" class="btn icon danger" title="删除讨论" @click="deletePost(detail.post)">
              <Trash2 :size="17" />
            </button>
          </div>

          <div v-if="detail.post.paperTitle" class="alert">关联试卷：{{ detail.post.paperTitle }}</div>
          <p class="discussion-content">{{ detail.post.content }}</p>

          <div class="reply-list">
            <h2>回复</h2>
            <div v-if="detail.replies.length === 0" class="empty">暂无回复</div>
            <div v-for="replyRow in detail.replies" :key="replyRow.id" class="reply-row">
              <div class="discussion-author">
                <img v-if="replyRow.authorAvatarUrl" class="avatar" :src="replyRow.authorAvatarUrl" alt="" />
                <span v-else class="avatar fallback">{{ initialOf(replyRow.authorName) }}</span>
                <span>{{ replyRow.authorName }} · {{ roleNames[replyRow.authorRole] || replyRow.authorRole }}</span>
                <span class="muted">{{ formatTime(replyRow.createdAt) }}</span>
              </div>
              <p>{{ replyRow.content }}</p>
              <button v-if="replyRow.canDelete" class="btn icon danger" title="删除回复" @click="deleteReply(replyRow)">
                <Trash2 :size="17" />
              </button>
            </div>
          </div>

          <div class="reply-editor">
            <textarea v-model.trim="replyContent" class="textarea" placeholder="输入回复内容"></textarea>
            <button class="btn primary" :disabled="!replyContent.trim()" @click="reply">
              <Send :size="18" />
              <span>发送回复</span>
            </button>
          </div>
        </template>
      </div>
    </section>

    <div v-if="modalOpen" class="modal-mask">
      <form class="modal" @submit.prevent="createPost">
        <div class="modal-head">
          <h2>发起讨论</h2>
          <button type="button" class="btn icon ghost" title="关闭" @click="modalOpen = false">
            <X :size="18" />
          </button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="field">
              <label>标题</label>
              <input v-model.trim="form.title" class="input" required />
            </div>
            <div class="field">
              <label>关联试卷</label>
              <select v-model="form.paperId" class="select">
                <option value="">不关联</option>
                <option v-for="paper in papers" :key="paper.id" :value="paper.id">{{ paper.title }}</option>
              </select>
            </div>
            <div class="field" style="grid-column: 1 / -1">
              <label>内容</label>
              <textarea v-model.trim="form.content" class="textarea" required></textarea>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button type="button" class="btn" @click="modalOpen = false">取消</button>
          <button class="btn primary">
            <MessageSquare :size="18" />
            <span>发布</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
