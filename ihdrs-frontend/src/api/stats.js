// api/stats.js
import request from '@/utils/request'

/**
 * 获取仪表盘统计数据
 */
export function getDashboardStats() {
    return request({
        url: '/stats/dashboard',
        method: 'get'
    })
}

/**
 * 获取最近的识别记录
 */
export function getRecentRecognitions(limit = 10) {
    return request({
        url: '/stats/recognitions',
        method: 'get',
        params: { limit }
    })
}

/**
 * 获取性能指标数据
 */
export function getPerformanceMetrics() {
    return request({
        url: '/stats/performance',
        method: 'get'
    })
}
