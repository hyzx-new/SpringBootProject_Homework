import { reactive, computed } from 'vue'

function readSavedUser() {
  const savedUser = localStorage.getItem('sas_user')
  if (!savedUser || savedUser === 'undefined' || savedUser === 'null') {
    localStorage.removeItem('sas_user')
    return null
  }
  try {
    return JSON.parse(savedUser)
  } catch {
    localStorage.removeItem('sas_user')
    localStorage.removeItem('sas_token')
    return null
  }
}

const savedUser = readSavedUser()

export const authState = reactive({
  token: savedUser ? localStorage.getItem('sas_token') || '' : '',
  user: savedUser
})

export const isLoggedIn = computed(() => Boolean(authState.token && authState.user))

export function setSession(payload) {
  authState.token = payload.token
  authState.user = payload.user
  localStorage.setItem('sas_token', payload.token)
  localStorage.setItem('sas_user', JSON.stringify(payload.user))
}

export function updateCurrentUser(payload) {
  authState.user = { ...authState.user, ...payload }
  localStorage.setItem('sas_user', JSON.stringify(authState.user))
}

export function clearSession() {
  authState.token = ''
  authState.user = null
  localStorage.removeItem('sas_token')
  localStorage.removeItem('sas_user')
}

export function hasAnyRole(roles) {
  if (!roles || roles.length === 0) return true
  return Boolean(authState.user && roles.includes(authState.user.role))
}
