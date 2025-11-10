// src/api/user.js
import axios from 'axios'

const TOKEN_KEY = 'ihdrs_token'

// 统一 axios 实例
export const http = axios.create({
    baseURL: '/api',
    timeout: 15000,
})

// 请求拦截器
http.interceptors.request.use((config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 响应拦截器
http.interceptors.response.use(
    (response) => {
        return response.data
    },
    (error) => {
        console.error('API Error:', error)
        return Promise.reject(error)
    }
)

/**
 * 获取当前登录用户信息
 */
export function getMe() {
    return http.get('/users/me')
}

/**
 * 更新当前用户资料
 */
export function updateMe(payload) {
    return http.put('/users/me', payload)
}

/**
 * 修改当前用户密码
 */
export function changeMyPwd(payload) {
    return http.put('/users/me/password', payload)
}

/**
 * 检查用户名是否已存在
 */
export function checkUsernameExists(username) {
    return http.get('/users/check-username', {
        params: { username }
    })
}

export default {
    getMe,
    updateMe,
    changeMyPwd,
    checkUsernameExists
}