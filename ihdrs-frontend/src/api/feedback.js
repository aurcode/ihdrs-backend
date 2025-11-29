// api/feedback.js
import request from '@/utils/request'

/**
 * 获取反馈列表
 */
export function getFeedbackList(params) {
    return request({
        url: '/feedback/list',
        method: 'get',
        params: {
            current: params.current || 1,
            size: params.size || 10,
            status: params.status,
            feedbackType: params.feedbackType
        }
    })
}

/**
 * 审核反馈
 */
export function reviewFeedback(feedbackId, data) {
    return request({
        url: `/feedback/${feedbackId}/review`,
        method: 'put',
        params: {
            status: data.status,
            reviewNote: data.reviewNote
        }
    })
}

/**
 * 批量审核反馈
 */
export function batchReviewFeedback(feedbackIds, status, reviewNote) {
    return request({
        url: '/feedback/batch-review',
        method: 'put',
        data: {
            feedbackIds,
            status,
            reviewNote
        }
    })
}

/**
 * 导出反馈数据
 * @param {Object} params - 筛选参数
 * @param {string} format - 导出格式: 'excel', 'csv', 'pdf'
 */
export function exportFeedback(params, format = 'excel') {
    return request({
        url: '/feedback/export',
        method: 'get',
        params: {
            ...params,
            format
        },
        responseType: 'blob'
    })
}

