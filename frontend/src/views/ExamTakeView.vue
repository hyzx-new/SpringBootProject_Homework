<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle2, Send, XCircle } from 'lucide-vue-next'
import { http } from '../api/http'

const route = useRoute()
const router = useRouter()
const paper = ref(null)
const questions = ref([])
const answers = reactive({})
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const result = ref(null)

const typeNames = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = route.params.id
    const [paperPayload, questionPayload] = await Promise.all([
      http.get(`/papers/${id}`),
      http.get(`/papers/${id}/questions`)
    ])
    paper.value = paperPayload
    questions.value = questionPayload
    questionPayload.forEach((question) => {
      answers[question.id] = question.type === 'MULTIPLE_CHOICE' ? [] : ''
    })
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function formatAnswer(value) {
  return Array.isArray(value) ? value.join(',') : value || ''
}

function resultStatus(answer) {
  if (answer.questionType === 'SHORT_ANSWER' && Number(answer.score) === 0 && !answer.correct) {
    return { text: '待批改', className: 'amber' }
  }
  return answer.correct ? { text: '正确', className: 'green' } : { text: '错误', className: 'red' }
}

function parseOptions(optionsText) {
  if (!optionsText) return []
  return optionsText.split(/\r?\n/).filter(Boolean).map((line) => {
    const trimmed = line.trim()
    const match = trimmed.match(/^([A-Za-z]+|true|false)[.、\s]+(.+)$/i)
    return {
      label: trimmed,
      value: match ? match[1] : trimmed
    }
  })
}

async function submit() {
  if (!confirm('确认提交当前答案吗？')) return
  submitting.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await http.post(`/submissions/papers/${paper.value.id}`, {
      answers: questions.value.map((question) => ({
        questionId: question.id,
        answer: formatAnswer(answers[question.id])
      }))
    })
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <div>
        <h2>{{ paper?.title || '答题' }}</h2>
        <p v-if="paper" class="muted">
          {{ paper.startTime?.replace('T', ' ') }} 至 {{ paper.endTime?.replace('T', ' ') }} · 总分 {{ paper.totalScore }}
        </p>
      </div>
      <button class="btn" @click="router.push('/exams')">返回列表</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="empty">正在加载试卷...</div>

    <section v-if="result" class="panel pad">
      <div class="toolbar">
        <div>
          <h2>提交完成</h2>
          <p class="muted">提交时间：{{ result.submittedAt?.replace('T', ' ') }}</p>
        </div>
        <div class="badge green">得分 {{ result.score }} / {{ result.totalScore }}</div>
      </div>
      <div class="question-list">
        <div v-for="answer in result.answers" :key="answer.questionId" class="question-item">
          <div class="question-title">
            <strong>{{ answer.title }}</strong>
            <span class="badge" :class="resultStatus(answer).className">
              <CheckCircle2 v-if="answer.correct" :size="15" />
              <XCircle v-else-if="resultStatus(answer).text === '错误'" :size="15" />
              {{ resultStatus(answer).text }}
            </span>
          </div>
          <p>你的答案：{{ answer.answer || '未作答' }}</p>
          <p class="muted">标准答案：{{ answer.correctAnswer }} · 本题得分：{{ answer.score }}</p>
        </div>
      </div>
    </section>

    <form v-if="!loading && !result" class="panel pad" @submit.prevent="submit">
      <div v-if="questions.length === 0" class="empty">该试卷暂无题目</div>
      <div v-else class="question-list">
        <div v-for="(question, index) in questions" :key="question.id" class="question-item">
          <div class="question-title">
            <div>
              <strong>{{ index + 1 }}. {{ question.title }}</strong>
              <p class="muted">{{ typeNames[question.type] }} · {{ question.score }} 分</p>
            </div>
          </div>

          <div v-if="['SINGLE_CHOICE', 'TRUE_FALSE'].includes(question.type)" class="option-list">
            <label v-for="option in parseOptions(question.optionsText)" :key="option.value" class="option-row">
              <input v-model="answers[question.id]" type="radio" :name="`q_${question.id}`" :value="option.value" />
              <span>{{ option.label }}</span>
            </label>
          </div>

          <div v-else-if="question.type === 'MULTIPLE_CHOICE'" class="option-list">
            <label v-for="option in parseOptions(question.optionsText)" :key="option.value" class="option-row">
              <input v-model="answers[question.id]" type="checkbox" :value="option.value" />
              <span>{{ option.label }}</span>
            </label>
          </div>

          <div v-else-if="question.type === 'FILL_BLANK'" class="field">
            <input v-model.trim="answers[question.id]" class="input" placeholder="请输入答案" />
          </div>

          <div v-else class="field">
            <textarea v-model.trim="answers[question.id]" class="textarea" placeholder="请输入简答内容"></textarea>
          </div>
        </div>
      </div>

      <div class="modal-foot" style="padding-left: 0; padding-right: 0; padding-bottom: 0">
        <button class="btn primary" :disabled="submitting || questions.length === 0">
          <Send :size="18" />
          <span>{{ submitting ? '提交中' : '提交答案' }}</span>
        </button>
      </div>
    </form>
  </div>
</template>
