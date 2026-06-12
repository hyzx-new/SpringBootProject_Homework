<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Edit, ListPlus, Plus, RefreshCw, Trash2, X } from 'lucide-vue-next'
import { http } from '../api/http'
import PaginationBar from '../components/PaginationBar.vue'

const papers = ref([])
const questions = ref([])
const loading = ref(false)
const error = ref('')
const paperModal = ref(false)
const questionModal = ref(false)
const editingPaperId = ref(null)
const editingQuestionId = ref(null)
const selectedPaper = ref(null)
const pageInfo = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 0
})

const paperForm = reactive(emptyPaper())
const questionForm = reactive(emptyQuestion())

const typeNames = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

function emptyPaper() {
  return {
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    major: '',
    commonCourse: false,
    published: true
  }
}

function emptyQuestion() {
  return {
    title: '',
    type: 'SINGLE_CHOICE',
    optionsText: '',
    correctAnswer: '',
    score: 10,
    orderNo: ''
  }
}

function toInputDateTime(value) {
  return value ? value.slice(0, 16) : ''
}

async function loadPapers() {
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
  loadPapers()
}

function openPaperCreate() {
  editingPaperId.value = null
  Object.assign(paperForm, emptyPaper())
  paperModal.value = true
}

function openPaperEdit(row) {
  editingPaperId.value = row.id
  Object.assign(paperForm, {
    title: row.title,
    description: row.description || '',
    startTime: toInputDateTime(row.startTime),
    endTime: toInputDateTime(row.endTime),
    major: row.major || '',
    commonCourse: Boolean(row.commonCourse),
    published: row.published
  })
  paperModal.value = true
}

async function savePaper() {
  error.value = ''
  const payload = { ...paperForm }
  try {
    if (editingPaperId.value) {
      await http.put(`/papers/${editingPaperId.value}`, payload)
    } else {
      await http.post('/papers', payload)
    }
    paperModal.value = false
    await loadPapers()
  } catch (err) {
    error.value = err.message
  }
}

async function removePaper(row) {
  if (!confirm(`确认删除试卷《${row.title}》吗？`)) return
  error.value = ''
  try {
    await http.delete(`/papers/${row.id}`)
    await loadPapers()
  } catch (err) {
    error.value = err.message
  }
}

async function openQuestions(row) {
  selectedPaper.value = row
  questionModal.value = true
  editingQuestionId.value = null
  Object.assign(questionForm, emptyQuestion())
  await loadQuestions()
}

async function loadQuestions() {
  if (!selectedPaper.value) return
  error.value = ''
  try {
    questions.value = await http.get(`/papers/${selectedPaper.value.id}/questions`)
  } catch (err) {
    error.value = err.message
  }
}

function openQuestionCreate() {
  editingQuestionId.value = null
  Object.assign(questionForm, emptyQuestion())
}

function openQuestionEdit(row) {
  editingQuestionId.value = row.id
  Object.assign(questionForm, {
    title: row.title,
    type: row.type,
    optionsText: row.optionsText || '',
    correctAnswer: row.correctAnswer || '',
    score: Number(row.score),
    orderNo: row.orderNo
  })
}

async function saveQuestion() {
  if (!selectedPaper.value) return
  error.value = ''
  const payload = {
    ...questionForm,
    orderNo: questionForm.orderNo === '' ? null : Number(questionForm.orderNo)
  }
  try {
    if (editingQuestionId.value) {
      await http.put(`/papers/${selectedPaper.value.id}/questions/${editingQuestionId.value}`, payload)
    } else {
      await http.post(`/papers/${selectedPaper.value.id}/questions`, payload)
    }
    openQuestionCreate()
    await loadQuestions()
    await loadPapers()
  } catch (err) {
    error.value = err.message
  }
}

async function removeQuestion(row) {
  if (!confirm(`确认删除题目「${row.title}」吗？`)) return
  error.value = ''
  try {
    await http.delete(`/papers/${selectedPaper.value.id}/questions/${row.id}`)
    await loadQuestions()
    await loadPapers()
  } catch (err) {
    error.value = err.message
  }
}

onMounted(loadPapers)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>试卷管理</h2>
      <div class="toolbar-actions">
        <button class="btn" @click="loadPapers">
          <RefreshCw :size="18" />
          <span>刷新</span>
        </button>
        <button class="btn primary" @click="openPaperCreate">
          <Plus :size="18" />
          <span>新增试卷</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载试卷...</div>
      <div v-else-if="papers.length === 0" class="empty">暂无试卷</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>总分</th>
              <th>范围</th>
              <th>发布</th>
              <th>创建人</th>
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
                  {{ paper.commonCourse ? '公共课' : (paper.major || '未设置') }}
                </span>
              </td>
              <td><span class="badge" :class="paper.published ? 'green' : 'amber'">{{ paper.published ? '已发布' : '未发布' }}</span></td>
              <td>{{ paper.createdBy || '-' }}</td>
              <td>
                <div class="actions">
                  <button class="btn icon" title="题目" @click="openQuestions(paper)">
                    <ListPlus :size="17" />
                  </button>
                  <button class="btn icon" title="编辑" @click="openPaperEdit(paper)">
                    <Edit :size="17" />
                  </button>
                  <button class="btn icon danger" title="删除" @click="removePaper(paper)">
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

    <div v-if="paperModal" class="modal-mask">
      <form class="modal" @submit.prevent="savePaper">
        <div class="modal-head">
          <h2>{{ editingPaperId ? '编辑试卷' : '新增试卷' }}</h2>
          <button type="button" class="btn icon ghost" title="关闭" @click="paperModal = false">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-body">
          <div class="form-grid">
            <div class="field">
              <label>标题</label>
              <input v-model.trim="paperForm.title" class="input" required />
            </div>
            <div class="field">
              <label>状态</label>
              <select v-model="paperForm.published" class="select">
                <option :value="true">发布</option>
                <option :value="false">暂不发布</option>
              </select>
            </div>
            <div class="field">
              <label>试卷类型</label>
              <select v-model="paperForm.commonCourse" class="select">
                <option :value="false">专业课试卷</option>
                <option :value="true">公共课试卷</option>
              </select>
            </div>
            <div v-if="!paperForm.commonCourse" class="field">
              <label>适用专业</label>
              <input v-model.trim="paperForm.major" class="input" required />
            </div>
            <div class="field">
              <label>开始时间</label>
              <input v-model="paperForm.startTime" class="input" type="datetime-local" required />
            </div>
            <div class="field">
              <label>结束时间</label>
              <input v-model="paperForm.endTime" class="input" type="datetime-local" required />
            </div>
            <div class="field" style="grid-column: 1 / -1">
              <label>说明</label>
              <textarea v-model="paperForm.description" class="textarea"></textarea>
            </div>
          </div>
        </div>

        <div class="modal-foot">
          <button type="button" class="btn" @click="paperModal = false">取消</button>
          <button class="btn primary">保存</button>
        </div>
      </form>
    </div>

    <div v-if="questionModal" class="modal-mask">
      <div class="modal">
        <div class="modal-head">
          <h2>题目管理：{{ selectedPaper?.title }}</h2>
          <button type="button" class="btn icon ghost" title="关闭" @click="questionModal = false">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-body grid">
          <form class="panel pad" @submit.prevent="saveQuestion">
            <div class="toolbar">
              <h2>{{ editingQuestionId ? '编辑题目' : '新增题目' }}</h2>
              <button type="button" class="btn" @click="openQuestionCreate">
                <Plus :size="18" />
                <span>清空</span>
              </button>
            </div>
            <div class="form-grid">
              <div class="field" style="grid-column: 1 / -1">
                <label>题干</label>
                <textarea v-model.trim="questionForm.title" class="textarea" required></textarea>
              </div>
              <div class="field">
                <label>类型</label>
                <select v-model="questionForm.type" class="select">
                  <option value="SINGLE_CHOICE">单选题</option>
                  <option value="MULTIPLE_CHOICE">多选题</option>
                  <option value="TRUE_FALSE">判断题</option>
                  <option value="FILL_BLANK">填空题</option>
                  <option value="SHORT_ANSWER">简答题</option>
                </select>
              </div>
              <div class="field">
                <label>分值</label>
                <input v-model.number="questionForm.score" class="input" type="number" min="1" step="0.5" required />
              </div>
              <div class="field">
                <label>排序</label>
                <input v-model="questionForm.orderNo" class="input" type="number" min="1" placeholder="不填自动排序" />
              </div>
              <div class="field">
                <label>标准答案</label>
                <textarea
                  v-if="questionForm.type === 'SHORT_ANSWER'"
                  v-model.trim="questionForm.correctAnswer"
                  class="textarea"
                  placeholder="填写参考答案，提交后可在成绩详情中人工批改"
                  required
                ></textarea>
                <input v-else v-model.trim="questionForm.correctAnswer" class="input" placeholder="多选题示例：A,C" required />
              </div>
              <div v-if="!['FILL_BLANK', 'SHORT_ANSWER'].includes(questionForm.type)" class="field" style="grid-column: 1 / -1">
                <label>选项</label>
                <textarea v-model="questionForm.optionsText" class="textarea" placeholder="每行一个选项，例如 A. 正确答案"></textarea>
              </div>
            </div>
            <div class="modal-foot" style="padding-left: 0; padding-right: 0; padding-bottom: 0">
              <button class="btn primary">保存题目</button>
            </div>
          </form>

          <section class="panel">
            <div v-if="questions.length === 0" class="empty">暂无题目</div>
            <div v-else class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>序号</th>
                    <th>题目</th>
                    <th>类型</th>
                    <th>答案</th>
                    <th>分值</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="question in questions" :key="question.id">
                    <td>{{ question.orderNo }}</td>
                    <td>
                      <strong>{{ question.title }}</strong>
                      <pre v-if="question.optionsText" class="muted" style="white-space: pre-wrap; margin: 8px 0 0">{{ question.optionsText }}</pre>
                    </td>
                    <td><span class="badge">{{ typeNames[question.type] || question.type }}</span></td>
                    <td>{{ question.correctAnswer }}</td>
                    <td>{{ question.score }}</td>
                    <td>
                      <div class="actions">
                        <button class="btn icon" title="编辑" @click="openQuestionEdit(question)">
                          <Edit :size="17" />
                        </button>
                        <button class="btn icon danger" title="删除" @click="removeQuestion(question)">
                          <Trash2 :size="17" />
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>
