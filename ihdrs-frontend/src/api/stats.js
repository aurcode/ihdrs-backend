// api/stats.js
import request from '@/utils/request'

export function getDashboardStats() {
    return request({
        url: '/stats/dashboard',
        method: 'get'
    })
}

export function getRecentRecognitions(limit = 10) {
    return request({
        url: '/stats/recognitions',
        method: 'get',
        params: { limit }
    })
}

export function getPerformanceMetrics() {
    return request({
        url: '/stats/performance',
        method: 'get'
    })
}

export function getErrorAnalysis() {
    return request({
        url: '/stats/analysis',
        method: 'get'
    })
}

export function getStatsByTimeRange(startTime, endTime) {
    return request({
        url: '/stats/time-range',
        method: 'get',
        params: { startTime, endTime }
    })
}
