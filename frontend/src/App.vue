<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  Bell,
  BookOpen,
  ClipboardList,
  FileDown,
  FolderOpen,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  MessageSquare,
  School,
  Settings,
  Shield,
  Users
} from 'lucide-vue-next'
import { http } from './api/http'
import { authState, clearSession } from './stores/auth'

const route = useRoute()
const router = useRouter()

const roleNames = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const nav = [
  { path: '/dashboard', label: '工作台', icon: LayoutDashboard, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/students', label: '学生管理', icon: GraduationCap, roles: ['ADMIN', 'TEACHER'] },
  { path: '/student-class-options', label: '学院专业班级', icon: School, roles: ['ADMIN'] },
  { path: '/users', label: '账号管理', icon: Users, roles: ['ADMIN'] },
  { path: '/papers', label: '试卷管理', icon: BookOpen, roles: ['ADMIN', 'TEACHER'] },
  { path: '/exams', label: '在线答题', icon: ClipboardList, roles: ['STUDENT'] },
  { path: '/submissions', label: '成绩记录', icon: FileDown, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/discussions', label: '讨论区', icon: MessageSquare, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/files', label: '资料文件', icon: FolderOpen, roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/reminders', label: '考试提醒', icon: Bell, roles: ['STUDENT'] },
  { path: '/profile', label: '个人中心', icon: Settings, roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
]

const isPublic = computed(() => route.meta.public)
const navItems = computed(() => nav.filter((item) => item.roles.includes(authState.user?.role)))
const title = computed(() => nav.find((item) => route.path.startsWith(item.path))?.label || '学生在线答题系统')

async function logout() {
  try {
    await http.post('/auth/logout')
  } finally {
    clearSession()
    router.push('/login')
  }
}

function initialOf(name) {
  return (name || '用户').slice(0, 1)
}
</script>

<template>
  <RouterView v-if="isPublic" />

  <div v-else class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark"><Shield :size="21" /></div>
        <div>
          <strong>在线答题系统</strong>
          <span>{{ roleNames[authState.user?.role] || '用户' }}端</span>
        </div>
      </div>

      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          <component :is="item.icon" :size="18" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="main">
      <header class="topbar">
        <h1>{{ title }}</h1>
        <div class="user-chip">
          <img v-if="authState.user?.avatarUrl" class="chip-avatar" :src="authState.user.avatarUrl" alt="" />
          <span v-else class="chip-avatar fallback">{{ initialOf(authState.user?.realName) }}</span>
          <span>{{ authState.user?.realName }} · {{ roleNames[authState.user?.role] }}</span>
          <button class="btn ghost icon" title="退出登录" @click="logout">
            <LogOut :size="18" />
          </button>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
