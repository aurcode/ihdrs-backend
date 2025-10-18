// Constants.java - 常量类
package com.ihdrs.backend.common.constants;

public class Constants {

    // Redis Key 前缀
    public static final String REDIS_KEY_USER_INFO = "user:info:";
    public static final String REDIS_KEY_MODEL_ACTIVE = "model:active";
    public static final String REDIS_KEY_RECOGNITION_RESULT = "recognition:result:";
    public static final String REDIS_KEY_SYSTEM_CONFIG = "system:config:";

    // 缓存过期时间（秒）
    public static final long CACHE_EXPIRE_USER_INFO = 3600; // 1小时
    public static final long CACHE_EXPIRE_MODEL_INFO = 1800; // 30分钟
    public static final long CACHE_EXPIRE_RECOGNITION = 86400; // 1天
    public static final long CACHE_EXPIRE_CONFIG = 3600; // 1小时

    // 文件相关常量
    public static final String UPLOAD_PATH = "uploads/";
    public static final String MODEL_PATH = "models/";
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final String[] ALLOWED_IMAGE_TYPES = {"image/png", "image/jpeg", "image/jpg"};

    // 识别相关常量
    public static final double MIN_CONFIDENCE_THRESHOLD = 0.8;
    public static final int RECOGNITION_TIMEOUT = 10000; // 10秒
    public static final int MAX_RECOGNITION_RETRIES = 3;

    // 训练相关常量
    public static final int DEFAULT_EPOCHS = 10;
    public static final int DEFAULT_BATCH_SIZE = 32;
    public static final double DEFAULT_LEARNING_RATE = 0.001;
}