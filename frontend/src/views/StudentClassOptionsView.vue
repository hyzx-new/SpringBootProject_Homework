<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, RefreshCw, Trash2 } from 'lucide-vue-next'
import { http } from '../api/http'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const keyword = ref('')
const rows = ref([])
const deleting = reactive({})

const form = reactive({
  college: '',
  major: '',
  className: ''
})

const filteredRows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return rows.value
  return rows.value.filter((row) => {
    return [row.college, row.major, row.className].some((value) => (value || '').toLowerCase().includes(text))
  })
})

const collegeCount = computed(() => new Set(rows.value.map((row) => row.college).filter(Boolean)).size)
const majorCount = computed(() => {
  const names = new Set(
    rows.value
      .filter((row) => row.college && row.major)
      .map((row) => `${row.college}\n${row.major}`)
  )
  return names.size
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const payload = await http.get('/student-class-options')
    rows.value = payload
      .map((option) => ({
        ...option,
        college: option.college || '',
        major: option.major || '',
        className: option.className || ''
      }))
      .filter((option) => option.college && option.major && option.className)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function save() {
  error.value = ''
  if (!form.college.trim() || !form.major.trim() || !form.className.trim()) {
    error.value = '请填写学院、专业和班级'
    return
  }
  saving.value = true
  try {
    await http.post('/student-class-options', {
      college: form.college.trim(),
      major: form.major.trim(),
      className: form.className.trim()
    })
    form.className = ''
    await load()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  if (!row.id) return
  if (!confirm(`确认删除 ${row.college} / ${row.major} / ${row.className} 吗？`)) return
  error.value = ''
  deleting[row.id] = true
  try {
    await http.delete(`/student-class-options/${row.id}`)
    await load()
  } catch (err) {
    error.value = err.message
  } finally {
    deleting[row.id] = false
  }
}

function rowKey(row) {
  return `${row.college}-${row.major}-${row.className}-${row.id || 'profile'}`
}

onMounted(load)
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>学院专业班级</h2>
      <div class="toolbar-actions">
        <div class="field" style="min-width: 260px">
          <input v-model.trim="keyword" class="input" placeholder="搜索学院、专业或班级" />
        </div>
        <button class="btn" @click="load">
          <RefreshCw :size="18" />
          <span>刷新</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel pad">
      <form class="option-editor-form" @submit.prevent="save">
        <div class="field">
          <label>学院名称</label>
          <input v-model.trim="form.college" class="input" required placeholder="如：信息工程学院" />
        </div>
        <div class="field">
          <label>专业名称</label>
          <input v-model.trim="form.major" class="input" required placeholder="如：软件工程" />
        </div>
        <div class="field">
          <label>班级名称</label>
          <input v-model.trim="form.className" class="input" required placeholder="如：软件工程 1 班" />
        </div>
        <button class="btn primary" :disabled="saving">
          <Plus :size="18" />
          <span>{{ saving ? '添加中' : '新增' }}</span>
        </button>
      </form>
    </section>

    <section class="panel">
      <div class="table-summary">
        <span>共 {{ rows.length }} 个班级，{{ collegeCount }} 个学院，{{ majorCount }} 个专业</span>
        <span v-if="keyword">筛选结果 {{ filteredRows.length }} 条</span>
      </div>
      <div v-if="loading" class="empty">正在加载学院、专业和班级...</div>
      <div v-else-if="filteredRows.length === 0" class="empty">暂无学院、专业和班级</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>学院</th>
              <th>专业</th>
              <th>班级</th>
              <th>来源</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="rowKey(row)">
              <td>{{ row.college }}</td>
              <td>{{ row.major }}</td>
              <td>{{ row.className }}</td>
              <td>
                <span class="badge" :class="row.id ? 'green' : ''">{{ row.id ? '维护项' : '学生档案' }}</span>
              </td>
              <td>
                <button
                  v-if="row.id"
                  class="btn icon danger"
                  title="删除"
                  :disabled="deleting[row.id]"
                  @click="remove(row)"
                >
                  <Trash2 :size="17" />
                </button>
                <span v-else class="muted">来自学生档案</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
