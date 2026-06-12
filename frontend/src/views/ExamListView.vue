<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ClipboardList, RefreshCw } from 'lucide-vue-next'
import { http } from '../api/http'
import PaginationBar from '../components/PaginationBar.vue'

const papers = ref([])
const loading = ref(false)
const error = ref('')
const now = computed(() => new Date())
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
    const payload = await http.get('/papers', {
      params: { page: pageInfo.page, size: pageInfo.size }
    })
    papers.value = payload.records
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

function statusOf(paper) {
  const start = new Date(paper.startTime)
  const end = new Date(paper.endTime)
  if (now.value < start) return { text: '未开始', className: 'amber', active: false }
  if (now.value > end) return { text: '已结束', className: 'red', active: false }
  return { text: '可答题', className: 'green', active: true }
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>在线答题</h2>
      <button class="btn" @click="load">
        <RefreshCw :size="18" />
        <span>刷新</span>
      </button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载试卷...</div>
      <div v-else-if="papers.length === 0" class="empty">暂无已发布试卷</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>总分</th>
              <th>范围</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="paper in papers" :key="paper.id">
              <td>
                <strong>{{ paper.title }}</strong>
                <div class="muted">{{ paper.description }}</div>
              </td>
              <td>{{ paper.startTime?.replace('T', ' ') }}</td>
              <td>{{ paper.endTime?.replace('T', ' ') }}</td>
              <td>{{ paper.totalScore }}</td>
              <td>
                <span class="badge" :class="paper.commonCourse ? 'green' : ''">
                  {{ paper.commonCourse ? '公共课' : (paper.major || '本专业') }}
                </span>
              </td>
              <td><span class="badge" :class="statusOf(paper).className">{{ statusOf(paper).text }}</span></td>
              <td>
                <RouterLink v-if="statusOf(paper).active" class="btn primary" :to="`/exams/${paper.id}`">
                  <ClipboardList :size="18" />
                  <span>开始答题</span>
                </RouterLink>
                <span v-else class="muted">不可答题</span>
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
