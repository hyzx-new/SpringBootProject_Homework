<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, Eye, RefreshCw, Save, X } from 'lucide-vue-next'
import { http } from '../api/http'
import { authState } from '../stores/auth'
import PaginationBar from '../components/PaginationBar.vue'

const rows = ref([])
const papers = ref([])
const classOptions = ref([])
const detail = ref(null)
const filterPaperId = ref('')
const exportModalOpen = ref(false)
const exportLoading = ref(false)
const loading = ref(false)
const error = ref('')
const pageInfo = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 0
})
const grading = reactive({})

const isManager = computed(() => ['ADMIN', 'TEACHER'].includes(authState.user?.role))
const exportClassNames = computed(() => {
  const names = new Set(classOptions.value.map((option) => option.className).filter(Boolean))
  rows.value.forEach((row) => {
    if (row.className) names.add(row.className)
  })
  return Array.from(names).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})
const exportForm = reactive({
  className: '',
  paperId: ''
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const payload = await http.get('/submissions', {
      params: {
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
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function loadPapers() {
  if (!isManager.value) return
  try {
    const payload = await http.get('/papers', { params: { page: 1, size: 100 } })
    papers.value = payload.records
  } catch (err) {
    error.value = err.message
  }
}

async function loadClassOptions() {
  if (!isManager.value) return
  try {
    classOptions.value = await http.get('/student-class-options')
  } catch (err) {
    error.value = err.message
  }
}

function filterChanged() {
  pageInfo.page = 1
  load()
}

function changePage(payload) {
  pageInfo.page = payload.page
  pageInfo.size = payload.size
  load()
}

async function openDetail(row) {
  error.value = ''
  try {
    detail.value = await http.get(`/submissions/${row.id}`)
    resetGrading(detail.value.answers)
  } catch (err) {
    error.value = err.message
  }
}

function resetGrading(answers) {
  Object.keys(grading).forEach((key) => delete grading[key])
  answers.forEach((answer) => {
    grading[answer.answerId] = {
      score: Number(answer.score),
      correct: answer.correct
    }
  })
}

function answerStatus(answer) {
  if (answer.questionType === 'SHORT_ANSWER' && Number(answer.score) === 0 && !answer.correct) {
    return { text: '待批改', className: 'amber' }
  }
  return answer.correct ? { text: '正确', className: 'green' } : { text: '错误', className: 'red' }
}

async function gradeAnswer(answer) {
  const payload = grading[answer.answerId]
  if (!payload) return
  error.value = ''
  try {
    detail.value = await http.put(`/submissions/${detail.value.id}/answers/${answer.answerId}/score`, payload)
    resetGrading(detail.value.answers)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function openExportModal() {
  exportForm.paperId = filterPaperId.value || ''
  exportForm.className = ''
  exportModalOpen.value = true
  await Promise.all([loadPapers(), loadClassOptions()])
}

async function exportScores() {
  error.value = ''
  exportLoading.value = true
  try {
    const response = await http.get('/submissions/export', {
      params: {
        paperId: exportForm.paperId || undefined,
        className: exportForm.className || undefined
      },
      responseType: 'blob'
    })
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '成绩表.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    exportModalOpen.value = false
  } catch (err) {
    error.value = err.message
  } finally {
    exportLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadPapers(), loadClassOptions()])
  await load()
})
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>成绩记录</h2>
      <div class="toolbar-actions">
        <select v-if="isManager" v-model="filterPaperId" class="select" style="min-width: 240px" @change="filterChanged">
          <option value="">全部试卷</option>
          <option v-for="paper in papers" :key="paper.id" :value="paper.id">{{ paper.title }}</option>
        </select>
        <button class="btn" @click="load">
          <RefreshCw :size="18" />
          <span>刷新</span>
        </button>
        <button v-if="isManager" class="btn primary" @click="openExportModal">
          <Download :size="18" />
          <span>导出成绩表</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载成绩...</div>
      <div v-else-if="rows.length === 0" class="empty">暂无成绩记录</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>学生</th>
              <th>学号</th>
              <th>班级</th>
              <th>学院</th>
              <th>得分</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.paperTitle }}</td>
              <td>{{ row.studentName }}</td>
              <td>{{ row.studentNo }}</td>
              <td>{{ row.className }}</td>
              <td>{{ row.college }}</td>
              <td><strong>{{ row.score }}</strong> / {{ row.totalScore }}</td>
              <td>{{ row.submittedAt?.replace('T', ' ') }}</td>
              <td>
                <button class="btn icon" title="详情" @click="openDetail(row)">
                  <Eye :size="17" />
                </button>
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

    <div v-if="exportModalOpen" class="modal-mask">
      <div class="modal export-modal">
        <div class="modal-head">
          <h2>导出成绩表</h2>
          <button type="button" class="btn icon ghost" title="关闭" @click="exportModalOpen = false">
            <X :size="18" />
          </button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="field">
              <label>班级</label>
              <select v-model="exportForm.className" class="select">
                <option value="">全部班级</option>
                <option v-for="className in exportClassNames" :key="className" :value="className">{{ className }}</option>
              </select>
            </div>
            <div class="field">
              <label>试卷</label>
              <select v-model="exportForm.paperId" class="select">
                <option value="">全部试卷</option>
                <option v-for="paper in papers" :key="paper.id" :value="paper.id">{{ paper.title }}</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button type="button" class="btn" @click="exportModalOpen = false">取消</button>
          <button type="button" class="btn primary" :disabled="exportLoading" @click="exportScores">
            <Download :size="18" />
            <span>{{ exportLoading ? '导出中' : '确认导出' }}</span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="detail" class="modal-mask">
      <div class="modal">
        <div class="modal-head">
          <h2>成绩详情</h2>
          <button class="btn icon ghost" title="关闭" @click="detail = null">
            <X :size="18" />
          </button>
        </div>
        <div class="modal-body grid">
          <div class="alert">
            {{ detail.paperTitle }} · {{ detail.studentName }} · 得分 {{ detail.score }} / {{ detail.totalScore }}
          </div>
          <div class="question-list">
            <div v-for="answer in detail.answers" :key="answer.questionId" class="question-item">
              <div class="question-title">
                <strong>{{ answer.title }}</strong>
                <span class="badge" :class="answerStatus(answer).className">{{ answerStatus(answer).text }}</span>
              </div>
              <p>学生答案：{{ answer.answer || '未作答' }}</p>
              <p class="muted">标准答案：{{ answer.correctAnswer }} · 本题得分：{{ answer.score }} / {{ answer.fullScore }}</p>
              <div v-if="isManager && grading[answer.answerId]" class="grading-row">
                <input
                  v-model.number="grading[answer.answerId].score"
                  class="input"
                  type="number"
                  min="0"
                  :max="Number(answer.fullScore)"
                  step="0.5"
                />
                <select v-model="grading[answer.answerId].correct" class="select">
                  <option :value="true">判定正确</option>
                  <option :value="false">判定错误</option>
                </select>
                <button class="btn" @click="gradeAnswer(answer)">
                  <Save :size="17" />
                  <span>保存分数</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
