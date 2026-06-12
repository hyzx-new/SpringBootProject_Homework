<script setup>
import { onMounted, ref } from 'vue'
import { Check, RefreshCw } from 'lucide-vue-next'
import { http } from '../api/http'

const rows = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await http.get('/reminders/my')
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function markRead(row) {
  error.value = ''
  try {
    await http.put(`/reminders/${row.id}/read`)
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
      <h2>考试提醒</h2>
      <button class="btn" @click="load">
        <RefreshCw :size="18" />
        <span>刷新</span>
      </button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <section class="panel">
      <div v-if="loading" class="empty">正在加载提醒...</div>
      <div v-else-if="rows.length === 0" class="empty">暂无考试提醒</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>提醒内容</th>
              <th>提醒时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.paperTitle }}</td>
              <td>{{ row.message }}</td>
              <td>{{ row.remindAt?.replace('T', ' ') }}</td>
              <td><span class="badge" :class="row.readFlag ? 'green' : 'amber'">{{ row.readFlag ? '已读' : '未读' }}</span></td>
              <td>
                <button v-if="!row.readFlag" class="btn" @click="markRead(row)">
                  <Check :size="18" />
                  <span>标记已读</span>
                </button>
                <span v-else class="muted">已处理</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
