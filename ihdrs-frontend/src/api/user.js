// api/user.js
import request from '@/utils/request'

/**
 * 获取当前登录用户信息
 */
export function getMe() {
    return request({
        url: '/users/me',
        method: 'get'
    })
}

/**
 * 更新当前用户信息
 */
export function updateMe(payload) {
    return request({
        url: '/users/me',
        method: 'put',
        data: payload
    })
}

/**
 * 修改当前用户密码
 */
export function changeMyPwd(payload) {
    return request({
        url: '/users/me/password',
        method: 'put',
        data: payload
    })
}

/**
 * 检查用户名是否存在
 */
export function checkUsernameExists(username) {
    return request({
        url: '/users/check-username',
        method: 'get',
        params: { username }
    })
}
