// API Configuration
// Update these URLs to match your backend deployment

export const API_CONFIG = {
    // Java Backend URL (Spring Boot - Port 8080)
    BACKEND_URL: 'http://169.254.65.12:8080/api',

    // Python Model Service URL (Flask - Port 5000)
    MODEL_SERVICE_URL: 'http://169.254.65.12:5000',

    // Endpoints
    ENDPOINTS: {
        // ===================== 系统健康 =====================
        HEALTH: '/health',
        HEALTH_PING: '/health/ping',

        // ===================== 识别服务 =====================
        RECOGNIZE: '/recognition/recognize',
        HISTORY: '/recognition/history_user',
        HISTORY_ADMIN: '/recognition/history',
        HISTORY_BATCH_DELETE: '/recognition/history/batch',

        // ===================== 用户认证 =====================
        AUTH_REGISTER: '/auth/register',
        AUTH_LOGIN: '/auth/login',
        AUTH_VALIDATE: '/auth/validate',

        // ===================== 用户反馈 =====================
        FEEDBACK_SUBMIT: '/feedback',
        FEEDBACK_LIST: '/feedback/list',
        FEEDBACK_BATCH_REVIEW: '/feedback/batch-review',

        // ===================== 模型管理 =====================
        MODEL_LIST: '/models/list',
        MODEL_ACTIVE: '/models/active',

        // ===================== 增强模型管理（管理员端） =====================
        ADMIN_MODEL_LIST: '/admin/models/list',
        ADMIN_MODEL_ACTIVE: '/admin/models/active',
        ADMIN_MODEL_COMPARE: '/admin/models/compare',
        ADMIN_MODEL_STATISTICS: '/admin/models/statistics',
        ADMIN_MODEL_BATCH_DELETE: '/admin/models/batch',

        // ===================== 用户管理 =====================
        USER_LIST: '/users/list',
        USER_ACTIVE_COUNT: '/users/active-count',

        // ===================== 训练管理 =====================
        TRAINING_TASK_CREATE: '/training/tasks',
        TRAINING_TASK_LIST: '/training/tasks',

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
