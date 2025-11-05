// utils/request.js

import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 创建axios实例
const service = axios.create({
    baseURL: 'http://localhost:8080/api' || '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    }
})

// 配置NProgress
NProgress.configure({
    showSpinner: false,
    trickleSpeed: 200,
    minimum: 0.3
})

// 请求拦截器
service.interceptors.request.use(
    (config) => {
        NProgress.start()

        // 添加token到请求头
        const userStore = useUserStore()
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`
        }

        // 打印请求信息
        console.log(`${config.method?.toUpperCase()} ${config.url}`, config.data || config.params)

        return config
    },
    (error) => {
        NProgress.done()
        console.error('Request Error:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    (response) => {
        NProgress.done()

        const { data } = response

        console.log(`${response.config.method?.toUpperCase()} ${response.config.url}`, data)

        // 根据后端返回的统一格式处理
        if (data.code === 200) {
            return data
        } else {
            // 处理业务错误
            ElMessage.error(data.message || '请求失败')
            return Promise.reject(new Error(data.message || '请求失败'))
        }
    },
    (error) => {
        NProgress.done()

        console.error('Response Error:', error)

        let message = '网络错误'

        if (error.response) {
            const { status, data } = error.response

            switch (status) {
                case 400:
                    message = data.message || '请求参数错误'
                    break
                case 401:
                    message = '登录已过期，请重新登录'
                    // 清除token并跳转到登录页
                    const userStore = useUserStore()
                    userStore.logout()
                    router.push('/login')
                    break
                case 403:
                    message = '权限不足'
                    break
                case 404:
                    message = '请求的资源不存在'
                    break
                case 500:
                    message = '服务器内部错误'
                    break
                default:
                    message = data.message || `请求失败 (${status})`
            }
        } else if (error.code === 'ECONNABORTED') {
            message = '请求超时'
        } else if (error.message === 'Network Error') {
            message = '网络连接异常'
        }

        ElMessage.error(message)
        return Promise.reject(error)
    }
)

export default service

/*
便捷的请求方法，方便以下这么调用
import { get, post } from '@/utils/request'

await get('/users', { id: 1 })
await post('/auth/login', { username, password })
*/

export const get = (url, params = {}) => {
    return service({
        method: 'get',
        url,
        params
    })
}

export const post = (url, data = {}) => {
    return service({
        method: 'post',
        url,
        data
    })
}

export const put = (url, data = {}) => {
    return service({
        method: 'put',
        url,
        data
    })
}

export const del = (url, params = {}) => {
    return service({
        method: 'delete',
        url,
        params
    })
}