import { reactive } from 'vue'

const TOKEN_KEY = 'only.token'
const USER_KEY = 'only.user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch (e) {
    // 本地缓存被改坏时不影响启动
    return null
  }
}

/**
 * 登录状态：token + 当前用户。
 * 只存前端本地，真正的身份校验还是后端 JWT + Spring Security。
 */
export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser()
})

export function setLogin(loginData) {
  authState.token = loginData.token
  authState.user = loginData.user
  localStorage.setItem(TOKEN_KEY, loginData.token)
  localStorage.setItem(USER_KEY, JSON.stringify(loginData.user))
}

export function clearLogin() {
  authState.token = ''
  authState.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function isLoggedIn() {
  return Boolean(authState.token)
}

export function isAdmin() {
  return authState.user?.role === 'ADMIN'
}
