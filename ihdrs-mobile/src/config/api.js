// API Configuration
// Update these URLs to match your backend deployment

export const API_CONFIG = {
    // Java Backend URL (Spring Boot - Port 8080)
    BACKEND_URL: 'http://192.168.193.1:8080/api',

    // Python Model Service URL (Flask - Port 5000)
    MODEL_SERVICE_URL: 'http://192.168.193.1:5000',

    // Endpoints
    ENDPOINTS: {
        // ===================== 系统健康 =====================
        HEALTH: '/health',
        HEALTH_PING: '/health/ping',

        // ===================== 识别服务 =====================
        RECOGNIZE: '/recognition/recognize',
        HISTORY: '/recognition/history',
        HISTORY_ADMIN: '/recognition/history_admin',
        HISTORY_DELETE: (recordId) => `/recognition/history/${recordId}`,
        HISTORY_BATCH_DELETE: '/recognition/history/batch',

        // ===================== 用户认证 =====================
        AUTH_REGISTER: '/auth/register',
        AUTH_LOGIN: '/auth/login',
        AUTH_VALIDATE: '/auth/validate',

        // ===================== 用户反馈反馈 =====================
        FEEDBACK_SUBMIT: '/feedback',
        FEEDBACK_LIST: '/feedback/list',
        FEEDBACK_REVIEW: (feedbackId) => `/feedback/${feedbackId}/review`,
        FEEDBACK_BATCH_REVIEW: '/feedback/batch-review',

        // ===================== 模型管理 =====================
        MODEL_LIST: '/models/list',
        MODEL_ACTIVE: '/models/active',
        MODEL_DETAIL: (modelId) => `/models/${modelId}`,
        MODEL_ACTIVATE: (modelId) => `/models/${modelId}/activate`,

        // ===================== 增强模型管理（管理员端） =====================
        ADMIN_MODEL_LIST: '/admin/models/list',
        ADMIN_MODEL_ACTIVE: '/admin/models/active',
        ADMIN_MODEL_DETAIL: (modelId) => `/admin/models/${modelId}`,
        ADMIN_MODEL_ACTIVATE: (modelId) => `/admin/models/${modelId}/activate`,
        ADMIN_MODEL_DISABLE: (modelId) => `/admin/models/${modelId}/disable`,
        ADMIN_MODEL_ENABLE: (modelId) => `/admin/models/${modelId}/enable`,
        ADMIN_MODEL_DELETE: (modelId) => `/admin/models/${modelId}`,
        ADMIN_MODEL_UPDATE: (modelId) => `/admin/models/${modelId}`,
        ADMIN_MODEL_VERSIONS: (modelName) => `/admin/models/${modelName}/versions`,
        ADMIN_MODEL_COMPARE: '/admin/models/compare',
        ADMIN_MODEL_STATISTICS: '/admin/models/statistics',
        ADMIN_MODEL_BATCH_DELETE: '/admin/models/batch',

        // ===================== 用户管理 =====================
        USER_LIST: '/users/list',
        USER_DETAIL: (userId) => `/users/${userId}`,
        USER_UPDATE_STATUS: (userId) => `/users/${userId}/status`,
        USER_ACTIVE_COUNT: '/users/active-count',

        // ===================== 训练管理 =====================
        TRAINING_TASK_CREATE: '/training/tasks',
        TRAINING_TASK_LIST: '/training/tasks',
        TRAINING_TASK_DETAIL: (taskId) => `/training/tasks/${taskId}`,
        TRAINING_TASK_LOGS: (taskId) => `/training/tasks/${taskId}/logs`,
        TRAINING_TASK_CANCEL: (taskId) => `/training/tasks/${taskId}/cancel`,
        TRAINING_TASK_UPDATE_PROGRESS: (taskId) => `/training/tasks/${taskId}/progress`,
        TRAINING_TASK_COMPLETE: (taskId) => `/training/tasks/${taskId}/complete`,
        TRAINING_TASK_FAIL: (taskId) => `/training/tasks/${taskId}/fail`,

        // ===================== 统计 =====================
        STATS_OVERVIEW: '/statistics/overview',
        STATS_TREND: '/statistics/recognition-trend',

        // ===================== 测试接口 =====================
        TEST_HELLO: '/test/hello',
        TEST_DB: '/test/db',
        TEST_CREATE_USER: '/test/create-user',
    },

    // Request timeout in milliseconds
    TIMEOUT: 30000,
};

// For production, you might want to use environment variables:
// export const API_CONFIG = {
//   BACKEND_URL: process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080',
//   MODEL_SERVICE_URL: process.env.REACT_APP_MODEL_SERVICE_URL || 'http://localhost:5000',
// };
