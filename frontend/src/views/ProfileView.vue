<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Save, ShieldCheck, Upload } from 'lucide-vue-next'
import { http } from '../api/http'
import { authState, updateCurrentUser } from '../stores/auth'

const loading = ref(false)
const saving = ref(false)
const changing = ref(false)
const uploading = ref(false)
const error = ref('')
const success = ref('')
const profile = ref(null)
const avatarInput = ref(null)
const form = reactive({
  realName: '',
  avatarUrl: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleNames = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const avatarPresets = [
  makeAvatar('#2563eb', '#ffffff', '学'),
  makeAvatar('#0f9f6e', '#ffffff', '答'),
  makeAvatar('#f59e0b', '#111827', '测'),
  makeAvatar('#ef4444', '#ffffff', '卷'),
  makeAvatar('#111827', '#ffffff', '知')
]

function makeAvatar(bg, fg, text) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"><rect width="160" height="160" rx="40" fill="${bg}"/><text x="50%" y="54%" text-anchor="middle" dominant-baseline="middle" font-family="Arial, sans-serif" font-size="72" font-weight="700" fill="${fg}">${text}</text></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await http.get('/profile/me')
    form.realName = profile.value.realName
    form.avatarUrl = profile.value.avatarUrl || ''
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    profile.value = await http.put('/profile/me', form)
    updateCurrentUser({
      realName: profile.value.realName,
      avatarUrl: profile.value.avatarUrl
    })
    success.value = '个人信息已保存'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type?.startsWith('image/')) {
    error.value = '头像文件必须是图片'
    event.target.value = ''
    return
  }
  uploading.value = true
  error.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploaded = await http.post('/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.avatarUrl = uploaded.downloadUrl
    event.target.value = ''
  } catch (err) {
    error.value = err.message
  } finally {
    uploading.value = false
  }
}

async function changePassword() {
  error.value = ''
  success.value = ''
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  changing.value = true
  try {
    await http.put('/profile/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    success.value = '密码已修改'
  } catch (err) {
    error.value = err.message
  } finally {
    changing.value = false
  }
}

function initialOf(name) {
  return (name || '用户').slice(0, 1)
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <div>
        <h2>个人中心</h2>
        <p class="muted">{{ authState.user?.username }} · {{ roleNames[authState.user?.role] }}</p>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="success" class="alert">{{ success }}</div>
    <div v-if="loading" class="empty">正在加载个人信息...</div>

    <section v-if="!loading" class="profile-grid">
      <form class="panel pad" @submit.prevent="saveProfile">
        <div class="profile-head">
          <img v-if="form.avatarUrl" class="profile-avatar" :src="form.avatarUrl" alt="" />
          <div v-else class="profile-avatar fallback">{{ initialOf(form.realName) }}</div>
          <div>
            <h2>{{ form.realName || '未命名用户' }}</h2>
            <p class="muted">{{ roleNames[authState.user?.role] }}端账号</p>
          </div>
        </div>

        <div class="form-grid one">
          <div class="field">
            <label>姓名</label>
            <input v-model.trim="form.realName" class="input" required />
          </div>
          <div class="field">
            <label>头像地址</label>
            <input v-model.trim="form.avatarUrl" class="input" placeholder="可粘贴图片地址，或上传/选择头像" />
          </div>
          <div class="field">
            <label>头像选择</label>
            <div class="avatar-options">
              <button
                v-for="preset in avatarPresets"
                :key="preset"
                type="button"
                class="avatar-option"
                :class="{ active: form.avatarUrl === preset }"
                @click="form.avatarUrl = preset"
              >
                <img :src="preset" alt="" />
              </button>
              <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="uploadAvatar" />
              <button type="button" class="btn" :disabled="uploading" @click="avatarInput?.click()">
                <Upload :size="18" />
                <span>{{ uploading ? '上传中' : '上传图片' }}</span>
              </button>
            </div>
          </div>
        </div>

        <div v-if="profile?.studentId" class="student-profile">
          <div>
            <span>学号</span>
            <strong>{{ profile.studentNo }}</strong>
          </div>
          <div>
            <span>班级</span>
            <strong>{{ profile.className }}</strong>
          </div>
          <div>
            <span>年级</span>
            <strong>{{ profile.grade }}</strong>
          </div>
          <div>
            <span>学院</span>
            <strong>{{ profile.college }}</strong>
          </div>
          <div>
            <span>专业</span>
            <strong>{{ profile.major }}</strong>
          </div>
        </div>

        <div v-else-if="profile?.major" class="student-profile">
          <div>
            <span>专业</span>
            <strong>{{ profile.major }}</strong>
          </div>
        </div>

        <div class="modal-foot" style="padding-left: 0; padding-right: 0; padding-bottom: 0">
          <button class="btn primary" :disabled="saving">
            <Save :size="18" />
            <span>{{ saving ? '保存中' : '保存资料' }}</span>
          </button>
        </div>
      </form>

      <form class="panel pad" @submit.prevent="changePassword">
        <div class="toolbar">
          <h2>修改密码</h2>
          <ShieldCheck :size="22" />
        </div>
        <div class="form-grid one">
          <div class="field">
            <label>原密码</label>
            <input v-model="passwordForm.oldPassword" class="input" type="password" autocomplete="current-password" required />
          </div>
          <div class="field">
            <label>新密码</label>
            <input v-model="passwordForm.newPassword" class="input" type="password" autocomplete="new-password" minlength="6" required />
          </div>
          <div class="field">
            <label>确认新密码</label>
            <input v-model="passwordForm.confirmPassword" class="input" type="password" autocomplete="new-password" minlength="6" required />
          </div>
        </div>
        <div class="modal-foot" style="padding-left: 0; padding-right: 0; padding-bottom: 0">
          <button class="btn primary" :disabled="changing">
            <Save :size="18" />
            <span>{{ changing ? '修改中' : '修改密码' }}</span>
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
