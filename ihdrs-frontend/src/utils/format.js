// utils/format.js

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的大小
 */
export function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B'

    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    const k = 1024
    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + units[i]
}

/**
 * 格式化日期时间
 * @param {string|Date} datetime - 日期时间
 * @returns {string} 格式化后的日期时间
 */
export function formatDateTime(datetime) {
    if (!datetime) return '-'

    const date = new Date(datetime)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化数据集状态
 * @param {string} status - 状态值
 * @returns {object} 状态对象
 */
export function formatDatasetStatus(status) {
    const statusMap = {
        'UPLOADING': { text: '上传中', type: 'info', color: '#409EFF' },
        'PROCESSING': { text: '处理中', type: 'warning', color: '#E6A23C' },
        'AVAILABLE': { text: '可用', type: 'success', color: '#67C23A' },
        'ERROR': { text: '错误', type: 'danger', color: '#F56C6C' }
    }

    return statusMap[status] || { text: status, type: 'info', color: '#909399' }
}

/**
 * 格式化数据集类型
 * @param {string} type - 类型值
 * @returns {string} 类型文本
 */
export function formatDatasetType(type) {
    const typeMap = {
        'IMAGE_CLASSIFICATION': '图像分类',
        'OBJECT_DETECTION': '目标检测',
        'OTHER': '其他'
    }

    return typeMap[type] || type
}

/**
 * 获取相对时间
 * @param {string|Date} datetime - 日期时间
 * @returns {string} 相对时间文本
 */
export function getRelativeTime(datetime) {
    if (!datetime) return '-'

    const now = new Date()
    const past = new Date(datetime)
    const diff = now - past

    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (days > 7) {
        return formatDateTime(datetime)
    } else if (days > 0) {
        return `${days}天前`
    } else if (hours > 0) {
        return `${hours}小时前`
    } else if (minutes > 0) {
        return `${minutes}分钟前`
    } else {
        return '刚刚'
    }
}