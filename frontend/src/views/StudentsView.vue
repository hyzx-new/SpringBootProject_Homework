<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Download, Edit, Plus, RefreshCw, Search, Trash2, X } from 'lucide-vue-next'
import { http } from '../api/http'
import { authState } from '../stores/auth'
import PaginationBar from '../components/PaginationBar.vue'

const loading = ref(false)
const error = ref('')
const keyword = ref('')
const rows = ref([])
const papers = ref([])
const modalOpen = ref(false)
const classPickerOpen = ref(false)
const exportModalOpen = ref(false)
const exportLoading = ref(false)
const classOptionLoading = ref(false)
const classOptionError = ref('')
const classOptions = ref([])
const editingId = ref(null)
const pageInfo = reactive({
  page: 1,
  size: 10,
  total: 0,
  pages: 0
})
const isAdmin = computed(() => authState.user?.role === 'ADMIN')

const form = reactive(emptyForm())
const pickerSelection = reactive({
  college: '',
  major: '',
  className: ''
})
const exportForm = reactive({
  className: '',
  paperId: ''
})

const collegeNames = computed(() => {
  const names = new Set(classOptions.value.map((option) => option.college).filter(Boolean))
  if (form.college) names.add(form.college)
  if (pickerSelection.college) names.add(pickerSelection.college)
  return Array.from(names).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const filteredClassOptions = computed(() => {
  return classOptions.value
    .filter((option) => option.college === pickerSelection.college)
    .filter((option) => option.major === pickerSelection.major)
    .sort((a, b) => a.className.localeCompare(b.className, 'zh-CN'))
})

const majorNames = computed(() => {
  const names = new Set(
    classOptions.value
      .filter((option) => option.college === pickerSelection.college)
      .map((option) => option.major)
      .filter(Boolean)
  )
  if (form.college === pickerSelection.college && form.major) names.add(form.major)
  if (pickerSelection.major) names.add(pickerSelection.major)
  return Array.from(names).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const exportClassNames = computed(() => {
  const names = new Set(classOptions.value.map((option) => option.className).filter(Boolean))
  rows.value.forEach((row) => {
    if (row.className) names.add(row.className)
  })
  return Array.from(names).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

function emptyForm() {
  return {
    username: '',
    password: '',
    realName: '',
    studentNo: '',
    className: '',
    grade: '',
    college: '',
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
    const payload = await http.get('/students', {
      params: { keyword: keyword.value, page: pageInfo.page, size: pageInfo.size }
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

function search() {
  pageInfo.page = 1
  load()
}

function resetSearch() {
  keyword.value = ''
  pageInfo.page = 1
  load()
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
      await http.put(`/students/${editingId.value}`, form)
    } else {
      await http.post('/students', form)
    }
    modalOpen.value = false
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function remove(row) {
  if (!confirm(`确认删除学生 ${row.realName} 吗？`)) return
  error.value = ''
  try {
    await http.delete(`/students/${row.id}`)
    await load()
  } catch (err) {
    error.value = err.message
  }
}

async function loadClassOptions() {
  classOptionLoading.value = true
  classOptionError.value = ''
  try {
    const payload = await http.get('/student-class-options')
    classOptions.value = payload
      .map((option) => ({
        ...option,
        college: option.college || '',
        major: option.major || '',
        className: option.className || ''
      }))
      .filter((option) => option.college && option.major && option.className)
  } catch (err) {
    classOptionError.value = err.message
  } finally {
    classOptionLoading.value = false
  }
}

async function loadPapers() {
  try {
    const payload = await http.get('/papers', { params: { page: 1, size: 100 } })
    papers.value = payload.records
  } catch (err) {
    error.value = err.message
  }
}

async function openClassPicker() {
  pickerSelection.college = form.college
  pickerSelection.major = form.major
  pickerSelection.className = form.className
  classPickerOpen.value = true
  await loadClassOptions()
  ensureCurrentClassOption()
}

function ensureCurrentClassOption() {
  if (!form.college || !form.major || !form.className) return
  const exists = classOptions.value.some((option) => {
    return option.college === form.college && option.major === form.major && option.className === form.className
  })
  if (!exists) {
    classOptions.value = [
      ...classOptions.value,
      { id: null, college: form.college, major: form.major, className: form.className }
    ]
  }
}

function selectCollege(college) {
  pickerSelection.college = college
  const hasSelectedMajor = classOptions.value.some((option) => option.college === college && option.major === pickerSelection.major)
  if (!hasSelectedMajor) {
    pickerSelection.major = ''
    pickerSelection.className = ''
  }
}

function selectMajor(major) {
  pickerSelection.major = major
  const hasSelectedClass = classOptions.value.some((option) => {
    return option.college === pickerSelection.college && option.major === major && option.className === pickerSelection.className
  })
  if (!hasSelectedClass) {
    pickerSelection.className = ''
  }
}

function selectClass(option) {
  pickerSelection.college = option.college
  pickerSelection.major = option.major
  pickerSelection.className = option.className
}

function applyClassSelection() {
  classOptionError.value = ''
  if (!pickerSelection.college || !pickerSelection.major || !pickerSelection.className) {
    classOptionError.value = '请选择学院、专业和班级'
    return
  }
  form.college = pickerSelection.college
  form.major = pickerSelection.major
  form.className = pickerSelection.className
  classPickerOpen.value = false
}

function optionKey(option) {
  return `${option.college}-${option.major}-${option.className}-${option.id || 'profile'}`
}

async function openExportModal() {
  exportForm.className = ''
  exportForm.paperId = ''
  exportModalOpen.value = true
  await Promise.all([loadClassOptions(), loadPapers()])
}

async function exportStudents() {
  error.value = ''
  exportLoading.value = true
  try {
    const response = await http.get('/students/export', {
      params: {
        keyword: keyword.value || undefined,
        className: exportForm.className || undefined,
        paperId: exportForm.paperId || undefined
      },
      responseType: 'blob'
    })
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '学生信息.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    exportModalOpen.value = false
  } catch (err) {
    error.value = err.message
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  load()
  loadClassOptions()
  loadPapers()
})
</script>

<template>
  <div class="grid">
    <div class="toolbar">
      <h2>学生管理</h2>
      <div class="toolbar-actions">
        <div class="field" style="min-width: 260px">
          <input v-model.trim="keyword" class="input" placeholder="搜索学号、姓名、班级、年级、学院、专业" @keyup.enter="search" />
        </div>
        <button class="btn" @click="search">
          <Search :size="18" />
          <span>搜索</span>
        </button>
        <button class="btn" @click="resetSearch">
          <RefreshCw :size="18" />
          <span>重置</span>
        </button>
        <button class="btn" @click="openExportModal">
          <Download :size="18" />
          <span>导出学生信息</span>
        </button>
        <button v-if="isAdmin" class="btn primary" @click="openCreate">
          <Plus :size="18" />
          <span>新增学生</span>
        </button>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载学生...</div>
      <div v-else-if="rows.length === 0" class="empty">暂无学生</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>学号</th>
              <th>姓名</th>
              <th>用户名</th>
              <th>班级</th>
              <th>年级</th>
              <th>学院</th>
              <th>专业</th>
              <th>状态</th>
              <th v-if="isAdmin">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.studentNo }}</td>
              <td>{{ row.realName }}</td>
              <td>{{ row.username }}</td>
              <td>{{ row.className }}</td>
              <td>{{ row.grade }}</td>
              <td>{{ row.college }}</td>
              <td>{{ row.major }}</td>
              <td>
                <span class="badge" :class="row.enabled ? 'green' : 'red'">{{ row.enabled ? '启用' : '禁用' }}</span>
              </td>
              <td v-if="isAdmin">
                <div class="actions">
                  <button class="btn icon" title="编辑" @click="openEdit(row)">
                    <Edit :size="17" />
                  </button>
                  <button class="btn icon danger" title="删除" @click="remove(row)">
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

    <div v-if="exportModalOpen" class="modal-mask">
      <div class="modal export-modal">
        <div class="modal-head">
          <h2>导出学生信息</h2>
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
          <button type="button" class="btn primary" :disabled="exportLoading" @click="exportStudents">
            <Download :size="18" />
            <span>{{ exportLoading ? '导出中' : '确认导出' }}</span>
          </button>
        </div>
      </div>
    </div>

    <div v-if="modalOpen" class="modal-mask">
      <form class="modal" @submit.prevent="save">
        <div class="modal-head">
          <h2>{{ editingId ? '编辑学生' : '新增学生' }}</h2>
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
              <label>学号</label>
              <input v-model.trim="form.studentNo" class="input" placeholder="留空自动生成，如 2052443001" />
            </div>
            <div class="field">
              <label>年级</label>
              <input v-model.trim="form.grade" class="input" required />
            </div>
            <div class="field">
              <label>学院</label>
              <div class="input-action">
                <input
                  v-model.trim="form.college"
                  class="input"
                  readonly
                  required
                  placeholder="请选择学院、专业和班级"
                  @click="openClassPicker"
                />
                <button type="button" class="btn" @click="openClassPicker">选择</button>
              </div>
            </div>
            <div class="field">
              <label>专业</label>
              <div class="input-action">
                <input
                  v-model.trim="form.major"
                  class="input"
                  readonly
                  required
                  placeholder="请选择学院、专业和班级"
                  @click="openClassPicker"
                />
                <button type="button" class="btn" @click="openClassPicker">选择</button>
              </div>
            </div>
            <div class="field">
              <label>班级</label>
              <div class="input-action">
                <input
                  v-model.trim="form.className"
                  class="input"
                  readonly
                  required
                  placeholder="请选择学院、专业和班级"
                  @click="openClassPicker"
                />
                <button type="button" class="btn" @click="openClassPicker">选择</button>
              </div>
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

    <div v-if="classPickerOpen" class="modal-mask">
      <div class="modal class-picker-modal">
        <div class="modal-head">
          <h2>选择学院、专业和班级</h2>
          <button type="button" class="btn icon ghost" title="关闭" @click="classPickerOpen = false">
            <X :size="18" />
          </button>
        </div>

        <div class="modal-body grid">
          <p v-if="classOptionError" class="error">{{ classOptionError }}</p>
          <div v-if="classOptionLoading" class="empty compact">正在加载学院、专业和班级...</div>
          <div v-else class="class-picker">
            <div class="picker-column">
              <h3>学院</h3>
              <button
                v-for="college in collegeNames"
                :key="college"
                type="button"
                class="picker-option"
                :class="{ active: college === pickerSelection.college }"
                @click="selectCollege(college)"
              >
                {{ college }}
              </button>
              <div v-if="collegeNames.length === 0" class="empty compact">暂无学院</div>
            </div>

            <div class="picker-column">
              <h3>专业</h3>
              <button
                v-for="major in majorNames"
                :key="major"
                type="button"
                class="picker-option"
                :class="{ active: major === pickerSelection.major }"
                @click="selectMajor(major)"
              >
                {{ major }}
              </button>
              <div v-if="!pickerSelection.college" class="empty compact">请先选择学院</div>
              <div v-else-if="majorNames.length === 0" class="empty compact">该学院暂无专业</div>
            </div>

            <div class="picker-column">
              <h3>班级</h3>
              <button
                v-for="option in filteredClassOptions"
                :key="optionKey(option)"
                type="button"
                class="picker-option"
                :class="{ active: option.className === pickerSelection.className }"
                @click="selectClass(option)"
              >
                {{ option.className }}
              </button>
              <div v-if="!pickerSelection.college" class="empty compact">请先选择学院</div>
              <div v-else-if="!pickerSelection.major" class="empty compact">请先选择专业</div>
              <div v-else-if="filteredClassOptions.length === 0" class="empty compact">该专业暂无班级</div>
            </div>
          </div>

        </div>

        <div class="modal-foot">
          <button type="button" class="btn" @click="classPickerOpen = false">取消</button>
          <button type="button" class="btn primary" @click="applyClassSelection">确定选择</button>
        </div>
      </div>
    </div>
  </div>
</template>
