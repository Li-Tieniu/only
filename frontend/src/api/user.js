import http from './http'

/** 登录：返回 { token, expireSeconds, user } */
export const login = (data) => http.post('/user/login', data)

/** 注册：返回新用户 ID */
export const register = (data) => http.post('/user/register', data)

/** 查询当前登录用户（用户身份来自后端 Token，不由前端指定 userId） */
export const getCurrentUser = () => http.get('/user/info')
