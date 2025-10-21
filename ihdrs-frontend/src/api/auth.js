// api/auth.js

import request from '@/utils/request'

/**
 * 用户认证相关API
 */

// 用户登录
export function login(data) {
    return request({
        url: '/auth/login',
        method: 'post',
        data
    })
}

// 用户注册
export function register(data) {
    return request({
        url: '/auth/register',
        method: 'post',
        data
    })
}

// 验证Token
export function validateToken() {
    return request({
        url: '/auth/validate',
        method: 'get'
    })
}

// 获取用户信息
export function getUserInfo() {
    return request({
        url: '/users/info',
        method: 'get'
    })
}