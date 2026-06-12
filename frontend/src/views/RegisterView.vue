<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { UserPlus } from 'lucide-vue-next'
import { http } from '../api/http'
import { setSession } from '../stores/auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  password: '',
  realName: '',
  major: '',
  role: 'TEACHER'
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const payload = await http.post('/auth/register', form)
    setSession(payload)
    router.push('/dashboard')
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-copy">
      <div>
        <h1>教师注册</h1>
        <p>公开注册仅用于教师账号。学生账号由管理员在学生管理中添加，初始密码默认为 123456。</p>
      </div>
      <p>管理员账号由系统初始化或管理员端创建。</p>
    </section>

    <section class="auth-panel">
      <form class="auth-box" @submit.prevent="submit">
        <h2>创建教师账号</h2>
        <p class="sub">注册后进入教师端，可维护试卷、查看成绩和参与讨论。</p>

        <div class="form-grid">
          <div class="field">
            <label>用户名</label>
            <input v-model.trim="form.username" class="input" required />
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="form.password" class="input" type="password" required />
          </div>
          <div class="field">
            <label>姓名</label>
            <input v-model.trim="form.realName" class="input" required />
          </div>
          <div class="field">
            <label>专业</label>
            <input v-model.trim="form.major" class="input" required />
          </div>
        </div>

        <div class="grid" style="margin-top: 16px">
          <button class="btn primary" :disabled="loading">
            <UserPlus :size="18" />
            <span>{{ loading ? '注册中' : '注册并登录' }}</span>
          </button>
          <RouterLink class="btn" to="/login">返回登录</RouterLink>
          <p v-if="error" class="error">{{ error }}</p>
        </div>
      </form>
    </section>
  </div>
</template>
