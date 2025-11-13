// api/dataset.js
import request from '@/utils/request'

/**
 * 数据集管理API
 */

// 上传数据集
export function uploadDataset(formData) {
    return request({
        url: '/datasets/upload',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        timeout: 300000 // 5分钟超时（大文件上传）
    })
}

// 获取我的数据集列表
export function getMyDatasets(params) {
    return request({
        url: '/datasets/my',
        method: 'get',
        params
    })
}

// 获取数据集详情
export function getDatasetDetail(datasetId) {
    return request({
        url: `/datasets/${datasetId}`,
        method: 'get'
    })
}

// 获取可用数据集列表（用于训练时选择）
export function getAvailableDatasets() {
    return request({
        url: '/datasets/available',
        method: 'get'
    })
}

// 获取公开数据集列表
export function getPublicDatasets(params) {
    return request({
        url: '/datasets/public',
        method: 'get',
        params
    })
}

// 更新数据集信息
export function updateDataset(datasetId, data) {
    return request({
        url: `/datasets/${datasetId}`,
        method: 'put',
        data
    })
}

// 设置数据集公开状态
export function setDatasetPublic(datasetId, isPublic) {
    return request({
        url: `/datasets/${datasetId}/public`,
        method: 'put',
        params: { isPublic }
    })
}

// 删除数据集
export function deleteDataset(datasetId) {
    return request({
        url: `/datasets/${datasetId}`,
        method: 'delete'
    })
}