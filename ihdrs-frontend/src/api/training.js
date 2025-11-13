// api/training.js
import request from '@/utils/request'

// 获取训练任务列表
export function getTrainingTaskList(params) {
    return request({
        url: '/training/tasks',
        method: 'get',
        params
    })
}

// 创建训练任务
export function createTrainingTask(data) {
    return request({
        url: '/training/tasks',
        method: 'post',
        data
    })
}

// 获取训练任务详情
export function getTrainingTaskDetail(taskId) {
    return request({
        url: `/training/tasks/${taskId}`,
        method: 'get'
    })
}

// 获取训练日志
export function getTrainingLogs(taskId) {
    return request({
        url: `/training/tasks/${taskId}/logs`,
        method: 'get'
    })
}

// 取消训练任务
export function cancelTrainingTask(taskId) {
    return request({
        url: `/training/tasks/${taskId}/cancel`,
        method: 'put'
    })
}

// 获取训练统计
export function getTrainingStatistics() {
    return request({
        url: '/training/statistics',
        method: 'get'
    })
}
