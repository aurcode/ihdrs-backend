// src/api/admin.js
import service, { get } from '@/utils/request'

/**
 * 获取用户列表
 */
export const getUserList = (params) => {
    return get('/users/list', params)
}

/**
 * 更新用户角色
 */
export const updateUserRole = (userId, role) => {
    return service({
        method: 'put',
        url: `/users/${userId}/role`,
        params: { role }
    })
}

/**
 * 更新用户状态
 */
export const updateUserStatus = (userId, status) => {
    return service({
        method: 'put',
        url: `/users/${userId}/status`,
        params: { status }
    })
}

/**
 * 获取用户行为日志
 */
export const getUserLogs = (params) => {
    const { userId, page, size } = params
    return get(`/users/${userId}/logs`, { page, size })
}