// api/model.js - 模型管理API
import request from '@/utils/request'

/**
 * 获取模型列表
 */
export function getModelList(params) {
    return request({
        url: '/admin/models/list',
        method: 'get',
        params
    })
}

/**
 * 获取活跃模型
 */
export function getActiveModel() {
    return request({
        url: '/admin/models/active',
        method: 'get'
    })
}

/**
 * 获取模型详情
 */
export function getModelById(modelId) {
    return request({
        url: `/admin/models/${modelId}`,
        method: 'get'
    })
}

/**
 * 切换活跃模型
 */
export function switchActiveModel(modelId) {
    return request({
        url: `/admin/models/${modelId}/activate`,
        method: 'put'
    })
}

/**
 * 停用模型
 */
export function disableModel(modelId) {
    return request({
        url: `/admin/models/${modelId}/disable`,
        method: 'put'
    })
}

/**
 * 启用模型
 */
export function enableModel(modelId) {
    return request({
        url: `/admin/models/${modelId}/enable`,
        method: 'put'
    })
}

/**
 * 删除模型
 */
export function deleteModel(modelId) {
    return request({
        url: `/admin/models/${modelId}`,
        method: 'delete'
    })
}

/**
 * 更新模型信息
 */
export function updateModel(modelId, data) {
    return request({
        url: `/admin/models/${modelId}`,
        method: 'put',
        data
    })
}

/**
 * 获取模型版本列表
 */
export function getModelVersions(modelName) {
    return request({
        url: `/admin/models/${modelName}/versions`,
        method: 'get'
    })
}

/**
 * 对比两个模型
 */
export function compareModels(modelId1, modelId2) {
    return request({
        url: '/admin/models/compare',
        method: 'get',
        params: {
            modelId1,
            modelId2
        }
    })
}

/**
 * 获取模型统计信息
 */
export function getModelStatistics() {
    return request({
        url: '/admin/models/statistics',
        method: 'get'
    })
}

/**
 * 批量删除模型
 */
export function batchDeleteModels(modelIds) {
    return request({
        url: '/admin/models/batch',
        method: 'delete',
        data: modelIds
    })
}