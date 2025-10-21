// stores/user.js

import { defineStore } from 'pinia'
import { login, register, validateToken } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: localStorage.getItem('ihdrs_token') || '',
        userInfo: JSON.parse(localStorage.getItem('ihdrs_user_info') || '{}'),
        permissions: [],
        roles: []
    }),

    getters: {
        // 是否已登录
        isLoggedIn: (state) => !!state.token,

        // 是否是管理员
        isAdmin: (state) => state.userInfo.role === 'ADMIN',

        // 用户名
        username: (state) => state.userInfo.username || '',

        // 用户ID
        userId: (state) => state.userInfo.userId || null,

        // 用户角色
        userRole: (state) => state.userInfo.role || 'ADMIN'
    },

    actions: {
        // 登录
        async login(loginForm) {
            try {
                const response = await login(loginForm)

                if (response.code === 200) {
                    const { token, tokenType, userInfo } = response.data

                    // 保存token和用户信息
                    this.token = token
                    this.userInfo = userInfo

                    // 持久化存储
                    localStorage.setItem('ihdrs_token', token)
                    localStorage.setItem('ihdrs_user_info', JSON.stringify(userInfo))

                    ElMessage.success('登录成功')

                    // 跳转到首页或之前访问的页面
                    const redirect = router.currentRoute.value.query.redirect || '/'
                    router.push(redirect)

                    return response
                }
            } catch (error) {
                console.error('登录失败:', error)
                throw error
            }
        },

        // 注册
        async register(registerForm) {
            try {
                const response = await register(registerForm)

                if (response.code === 200) {
                    ElMessage.success('注册成功，请登录')
                    router.push('/login')
                    return response
                }
            } catch (error) {
                console.error('注册失败:', error)
                throw error
            }
        },

        // 登出
        logout() {
            this.token = ''
            this.userInfo = {}
            this.permissions = []
            this.roles = []

            // 清除本地存储
            localStorage.removeItem('ihdrs_token')
            localStorage.removeItem('ihdrs_user_info')

            ElMessage.success('已退出登录')
            router.push('/login')
        },

        // 验证token有效性
        async validateToken() {
            if (!this.token) {
                return false
            }

            try {
                const response = await validateToken()
                if (response.code === 200) {
                    this.userInfo = response.data
                    localStorage.setItem('ihdrs_user_info', JSON.stringify(response.data))
                    return true
                }
            } catch (error) {
                console.error('Token验证失败:', error)
                this.logout()
                return false
            }

            return false
        },

        // 更新用户信息
        updateUserInfo(userInfo) {
            this.userInfo = { ...this.userInfo, ...userInfo }
            localStorage.setItem('ihdrs_user_info', JSON.stringify(this.userInfo))
        },

        // 检查权限
        hasPermission(permission) {
            return this.permissions.includes(permission) || this.isAdmin
        },

        // 检查角色
        hasRole(role) {
            return this.roles.includes(role) || this.userRole === role
        }
    }
})