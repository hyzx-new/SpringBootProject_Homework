<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Edit, Plus, Trash2, X } from 'lucide-vue-next'
import { http } from '../api/http'
import PaginationBar from '../components/PaginationBar.vue'

const rows = ref([])
const error = ref('')
const loading = ref(false)
const modalOpen = ref(false)
const editingId = ref(null)
const pageInfo = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 0
})

const roleNames = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const form = reactive(emptyForm())

function emptyForm() {
  return {
    username: '',
    password: '',
    realName: '',
    role: 'TEACHER',
    major: '',
    enabled: true
  }
}

function assignForm(data) {
  Object.assign(form, emptyForm(), data)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const payload = await http.get('/users', {
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

function openCreate() {
  editingId.value = null
  assignForm(emptyForm())
  modalOpen.value = true
}

function openEdit(row) {
  editingId.value = row.id
  assignForm({ ...row, password: '' })
  modalOpen.value = true
}

async function save() {
  error.value = ''
  try {
    if (editingId.value) {
      await http.put(`/users/${editingId.value}`, form)
    } else {
      await http.post('/users', form)
    }
    modalOpen.value = false
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function remove(row) {
  if (!confirm(`确认删除账号 ${row.username} 吗？`)) return
  error.value = ''
  try {
    await http.delete(`/users/${row.id}`)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>账号管理</h2>
      <button class="btn primary" @click="openCreate">
        <Plus :size="18" />
        <span>新增账号</span>
      </button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载账号...</div>
      <div v-else-if="rows.length === 0" class="empty">暂无账号</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户名</th>
              <th>姓名</th>
              <th>角色</th>
              <th>专业</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.username }}</td>
              <td>{{ row.realName }}</td>
              <td><span class="badge">{{ roleNames[row.role] || row.role }}</span></td>
              <td>{{ ['TEACHER', 'STUDENT'].includes(row.role) ? (row.major || '未设置') : '-' }}</td>
              <td><span class="badge" :class="row.enabled ? 'green' : 'red'">{{ row.enabled ? '启用' : '禁用' }}</span></td>
              <td>
                <div class="actions">
                  <button v-if="row.role !== 'STUDENT'" class="btn icon" title="编辑" @click="openEdit(row)">
                    <Edit :size="17" />
                  </button>
                  <button v-if="row.role !== 'STUDENT'" class="btn icon danger" title="删除" @click="remove(row)">
                    <Trash2 :size="17" />
                  </button>
                  <span v-else class="muted">学生账号在学生管理维护</span>
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

    <div v-if="modalOpen" class="modal-mask">
      <form class="modal" @submit.prevent="save">
        <div class="modal-head">
          <h2>{{ editingId ? '编辑账号' : '新增账号' }}</h2>
          <button type="button" class="btn icon ghost" title="关闭" @click="modalOpen = false">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-body">
          <div class="form-grid">
            <div class="field">
              <label>用户名</label>
              <input v-model.trim="form.username" class="input" required />
            </div>
            <div class="field">
              <label>{{ editingId ? '新密码' : '密码' }}</label>
              <input v-model="form.password" class="input" type="password" :placeholder="editingId ? '不填则不修改' : '不填默认为 123456'" />
            </div>
            <div class="field">
              <label>姓名</label>
              <input v-model.trim="form.realName" class="input" required />
            </div>
            <div class="field">
              <label>角色</label>
              <select v-model="form.role" class="select">
                <option value="TEACHER">教师</option>
                <option value="ADMIN">管理员</option>
              </select>
            </div>
            <div class="field">
              <label>专业</label>
              <input v-model.trim="form.major" class="input" :required="form.role === 'TEACHER'" placeholder="教师必填，管理员可留空" />
            </div>
            <div class="field">
              <label>状态</label>
              <select v-model="form.enabled" class="select">
                <option :value="true">启用</option>
                <option :value="false">禁用</option>
              </select>
            </div>
          </div>
        </div>

        <div class="modal-foot">
          <button type="button" class="btn" @click="modalOpen = false">取消</button>
          <button class="btn primary">保存</button>
        </div>
      </form>
    </div>
  </div>
</template>
