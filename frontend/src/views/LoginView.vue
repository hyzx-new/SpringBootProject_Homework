<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { LogIn } from 'lucide-vue-next'
import { http } from '../api/http'
import { setSession } from '../stores/auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: 'admin',
  password: '123456'
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const payload = await http.post('/auth/login', form)
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
        <h1>学生在线答题系统</h1>
        <p>面向管理员、教师和学生的在线考试平台，支持学生档案维护、试卷题库、自动判分、成绩导出和考前提醒。</p>
      </div>
      <p>示例账号：admin / teacher / student，密码均为 123456。</p>
    </section>

    <section class="auth-panel">
      <form class="auth-box" @submit.prevent="submit">
        <h2>登录</h2>
        <p class="sub">输入账号进入对应角色端。</p>

        <div class="grid">
          <div class="field">
            <label>用户名</label>
            <input v-model.trim="form.username" class="input" autocomplete="username" required />
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="form.password" class="input" type="password" autocomplete="current-password" required />
          </div>
          <button class="btn primary" :disabled="loading">
            <LogIn :size="18" />
            <span>{{ loading ? '登录中' : '登录' }}</span>
          </button>
          <RouterLink class="btn" to="/register">教师注册</RouterLink>
          <p v-if="error" class="error">{{ error }}</p>
        </div>
      </form>
    </section>
  </div>
</template>
