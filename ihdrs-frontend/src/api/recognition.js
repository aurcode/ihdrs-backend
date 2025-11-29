// api/recognition.js
import request from '@/utils/request'

/**
 * 获取识别历史列表
 */
export function getRecognitionHistory(params) {
    return request({
        url: '/recognition/history_admin',
        method: 'get',
        params: {
            page: params.page || 0,
            size: params.size || 10,
            result: params.result,
            userId: params.userId || null,
            startTime: params.startTime,
            endTime: params.endTime
        }
    })
}

/**
 * 删除识别记录
 */
export function deleteRecognitionRecord(recordId) {
    return request({
        url: `/recognition/history/${recordId}`,
        method: 'delete'
    })
}

/**
 * 批量删除识别记录
 */
export function batchDeleteRecords(recordIds) {
    return request({
        url: '/recognition/history/batch',
        method: 'delete',
        data: recordIds
    })
}

/**
 * 导出识别历史
 * @param {Object} params - 筛选参数
 * @param {string} format - 导出格式: 'excel', 'csv', 'pdf'
 */
export function exportRecognitionHistory(params, format = 'excel') {
    return request({
        url: '/recognition/history/export',
        method: 'get',
        params: {
            ...params,
            format
        },
        responseType: 'blob'
    })
}

