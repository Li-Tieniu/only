import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { authState, clearLogin } from '../stores/auth'

/**
 * 统一封装 axios：
 * 1) 自动带上 Authorization: Bearer <token>
 * 2) 按后端 ApiResult（code/message/data）解包，成功直接返回 data
 * 3) 401 清理登录态并跳登录页，403 提示无权限
 */
function createHttp(baseURL) {
  const instance = axios.create({ baseURL, timeout: 10000 })

  instance.interceptors.request.use((config) => {
    if (authState.token) {
      config.headers.Authorization = `Bearer ${authState.token}`
    }
    return config
  })

  instance.interceptors.response.use(
    (response) => {
      const body = response.data
      if (body && typeof body.code !== 'undefined') {
        if (body.code === 200) {
          return body.data
        }
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body
    },
    (error) => {
      const status = error.response?.status
      const message = error.response?.data?.message
      if (status === 401) {
        clearLogin()
        ElMessage.warning(message || '登录已过期，请重新登录')
        const current = router.currentRoute.value
        if (current.path !== '/login') {
          router.push({ path: '/login', query: { redirect: current.fullPath } })
        }
      } else if (status === 403) {
        ElMessage.error(message || '没有权限执行该操作')
      } else if (status === 404) {
        ElMessage.error(message || '请求的数据不存在')
      } else {
        ElMessage.error(message || '网络异常，请稍后重试')
      }
      return Promise.reject(error)
    }
  )

  return instance
}

/** 用户端接口（app 模块，8080） */
const http = createHttp('/api')

/** 管理端接口（console 模块，8081） */
const adminHttp = createHttp('/admin-api')

export { adminHttp }
export default http
