import { createRouter, createWebHistory } from 'vue-router'
import { authState, hasAnyRole } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/register', component: () => import('../views/RegisterView.vue'), meta: { public: true } },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/students', component: () => import('../views/StudentsView.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
  { path: '/student-class-options', component: () => import('../views/StudentClassOptionsView.vue'), meta: { roles: ['ADMIN'] } },
  { path: '/users', component: () => import('../views/UsersView.vue'), meta: { roles: ['ADMIN'] } },
  { path: '/papers', component: () => import('../views/PapersView.vue'), meta: { roles: ['ADMIN', 'TEACHER'] } },
  { path: '/exams', component: () => import('../views/ExamListView.vue'), meta: { roles: ['STUDENT'] } },
  { path: '/exams/:id', component: () => import('../views/ExamTakeView.vue'), meta: { roles: ['STUDENT'] } },
  { path: '/submissions', component: () => import('../views/SubmissionsView.vue') },
  { path: '/discussions', component: () => import('../views/DiscussionsView.vue') },
  { path: '/files', component: () => import('../views/FilesView.vue') },
  { path: '/reminders', component: () => import('../views/RemindersView.vue'), meta: { roles: ['STUDENT'] } },
  { path: '/profile', component: () => import('../views/ProfileView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return authState.token ? '/dashboard' : true
  }
  if (!authState.token) {
    return '/login'
  }
  if (!hasAnyRole(to.meta.roles)) {
    return '/dashboard'
  }
  return true
})

export default router
