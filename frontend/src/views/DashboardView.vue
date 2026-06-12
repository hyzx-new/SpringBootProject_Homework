<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Bell, BookOpen, ClipboardCheck, GraduationCap, Users } from 'lucide-vue-next'
import { http } from '../api/http'
import { authState } from '../stores/auth'

const loading = ref(true)
const error = ref('')
const stats = ref({
  students: 0,
  users: 0,
  papers: 0,
  submissions: 0,
  reminders: 0
})
const recentPapers = ref([])
const recentSubmissions = ref([])
const reminders = ref([])

const isStudent = computed(() => authState.user?.role === 'STUDENT')
const isAdmin = computed(() => authState.user?.role === 'ADMIN')

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (isStudent.value) {
      const [paperPage, submissionPage, reminderRows] = await Promise.all([
        http.get('/papers', { params: { page: 1, size: 5 } }),
        http.get('/submissions', { params: { page: 1, size: 5 } }),
        http.get('/reminders/my')
      ])
      const papers = paperPage.records
      const submissions = submissionPage.records
      recentPapers.value = papers.slice(0, 5)
      recentSubmissions.value = submissions.slice(0, 5)
      reminders.value = reminderRows.slice(0, 5)
      stats.value = {
        students: 0,
        users: 0,
        papers: paperPage.total,
        submissions: submissionPage.total,
        reminders: reminderRows.filter((item) => !item.readFlag).length
      }
    } else {
      const requests = [
        http.get('/students', { params: { page: 1, size: 1 } }),
        http.get('/papers', { params: { page: 1, size: 5 } }),
        http.get('/submissions', { params: { page: 1, size: 5 } })
      ]
      if (isAdmin.value) requests.push(http.get('/users', { params: { page: 1, size: 1 } }))
      const result = await Promise.all(requests)
      const studentPage = result[0]
      const paperPage = result[1]
      const submissionPage = result[2]
      const userPage = isAdmin.value ? result[3] : { total: 0 }
      const papers = paperPage.records
      const submissions = submissionPage.records
      recentPapers.value = papers.slice(0, 5)
      recentSubmissions.value = submissions.slice(0, 5)
      stats.value = {
        students: studentPage.total,
        users: userPage.total,
        papers: paperPage.total,
        submissions: submissionPage.total,
        reminders: 0
      }
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <div>
        <h2>你好，{{ authState.user?.realName }}</h2>
        <p class="muted">当前身份：{{ authState.user?.role }}</p>
      </div>
      <div class="toolbar-actions">
        <RouterLink v-if="isStudent" to="/exams" class="btn primary">
          <BookOpen :size="18" />
          <span>去答题</span>
        </RouterLink>
        <RouterLink v-else to="/papers" class="btn primary">
          <BookOpen :size="18" />
          <span>管理试卷</span>
        </RouterLink>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading" class="muted">正在加载数据...</p>

    <section v-if="!loading" class="stats">
      <div v-if="!isStudent" class="stat">
        <span><GraduationCap :size="16" /> 学生数</span>
        <strong>{{ stats.students }}</strong>
      </div>
      <div v-if="isAdmin" class="stat">
        <span><Users :size="16" /> 账号数</span>
        <strong>{{ stats.users }}</strong>
      </div>
      <div class="stat">
        <span><BookOpen :size="16" /> 试卷数</span>
        <strong>{{ stats.papers }}</strong>
      </div>
      <div class="stat">
        <span><ClipboardCheck :size="16" /> 成绩记录</span>
        <strong>{{ stats.submissions }}</strong>
      </div>
      <div v-if="isStudent" class="stat">
        <span><Bell :size="16" /> 未读提醒</span>
        <strong>{{ stats.reminders }}</strong>
      </div>
    </section>

    <section class="panel pad">
      <div class="toolbar">
        <h2>{{ isStudent ? '可答试卷' : '最近试卷' }}</h2>
        <RouterLink :to="isStudent ? '/exams' : '/papers'" class="btn">查看全部</RouterLink>
      </div>
      <div v-if="recentPapers.length === 0" class="empty">暂无试卷</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>总分</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="paper in recentPapers" :key="paper.id">
              <td>{{ paper.title }}</td>
              <td>{{ paper.startTime?.replace('T', ' ') }}</td>
              <td>{{ paper.endTime?.replace('T', ' ') }}</td>
              <td>{{ paper.totalScore }}</td>
              <td><span class="badge green">{{ paper.published ? '已发布' : '未发布' }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel pad">
      <div class="toolbar">
        <h2>最近成绩</h2>
        <RouterLink to="/submissions" class="btn">查看全部</RouterLink>
      </div>
      <div v-if="recentSubmissions.length === 0" class="empty">暂无成绩记录</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>学生</th>
              <th>得分</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in recentSubmissions" :key="row.id">
              <td>{{ row.paperTitle }}</td>
              <td>{{ row.studentName }}</td>
              <td><strong>{{ row.score }}</strong> / {{ row.totalScore }}</td>
              <td>{{ row.submittedAt?.replace('T', ' ') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="isStudent" class="panel pad">
      <div class="toolbar">
        <h2>考试提醒</h2>
        <RouterLink to="/reminders" class="btn">查看全部</RouterLink>
      </div>
      <div v-if="reminders.length === 0" class="empty">暂无提醒</div>
      <div v-else class="grid">
        <div v-for="item in reminders" :key="item.id" class="alert">
          {{ item.message }}
        </div>
      </div>
    </section>
  </div>
</template>
