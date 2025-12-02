-- init.sql - 数据库初始化脚本
-- 创建数据库W
DROP DATABASE ihdrs;
CREATE DATABASE IF NOT EXISTS ihdrs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ihdrs;

-- 删除外键约束，重新创建表
SET FOREIGN_KEY_CHECKS = 0;

-- 删除已存在的表
DROP TABLE IF EXISTS training_logs;
DROP TABLE IF EXISTS operation_logs;
DROP TABLE IF EXISTS feedback_data;
DROP TABLE IF EXISTS recognition_records;
DROP TABLE IF EXISTS training_tasks;
DROP TABLE IF EXISTS models;
DROP TABLE IF EXISTS system_configs;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS datasets;
DROP TABLE IF EXISTS user_log;

-- 创建数据集表
CREATE TABLE `datasets` (
                            `dataset_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据集ID',
                            `dataset_name` varchar(100) NOT NULL COMMENT '数据集名称',
                            `dataset_type` enum('IMAGE_CLASSIFICATION', 'OBJECT_DETECTION', 'OTHER') NOT NULL DEFAULT 'IMAGE_CLASSIFICATION' COMMENT '数据集类型',
                            `description` text COMMENT '数据集描述',
                            `file_path` varchar(500) NOT NULL COMMENT '数据集文件路径',
                            `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
                            `num_classes` int DEFAULT NULL COMMENT '类别数量',
                            `num_samples` int DEFAULT NULL COMMENT '样本总数',
                            `train_samples` int DEFAULT NULL COMMENT '训练集样本数',
                            `test_samples` int DEFAULT NULL COMMENT '测试集样本数',
                            `image_width` int DEFAULT NULL COMMENT '图像宽度',
                            `image_height` int DEFAULT NULL COMMENT '图像高度',
                            `class_names` json COMMENT '类别名称列表',
                            `status` enum('UPLOADING', 'PROCESSING', 'AVAILABLE', 'ERROR') NOT NULL DEFAULT 'UPLOADING' COMMENT '数据集状态',
                            `error_message` text COMMENT '错误信息',
                            `is_public` tinyint DEFAULT '0' COMMENT '是否公开：1-公开，0-私有',
                            `creator_id` bigint NOT NULL COMMENT '创建者ID',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`dataset_id`),
                            KEY `idx_creator_id` (`creator_id`),
                            KEY `idx_status` (`status`),
                            KEY `idx_type` (`dataset_type`),
                            KEY `idx_is_public` (`is_public`),
                            KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集表';

-- 创建用户表
CREATE TABLE IF NOT EXISTS `users` (
                                       `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                       `username` varchar(50) NOT NULL COMMENT '用户名',
                                       `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
                                       `salt` varchar(32) NOT NULL COMMENT '密码盐值',
                                       `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色',
                                       `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                                       `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                                       `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                                       `login_count` int DEFAULT '0' COMMENT '登录次数',
                                       `status` tinyint DEFAULT '1' COMMENT '状态：1-正常，0-禁用',
                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`user_id`),
                                       UNIQUE KEY `uk_username` (`username`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_role` (`role`),
                                       KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入初始数据
INSERT INTO `users` (`username`, `password_hash`, `salt`, `role`, `email`, `status`) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi7X7L4NXMHpnsKp7x6GzwxK', 'default_salt', 'ADMIN', 'admin@ihdrs.com', 1)
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`);

INSERT INTO `users` (`username`, `password_hash`, `salt`, `role`, `email`, `status`) VALUES
    ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi7X7L4NXMHpnsKp7x6GzwxK', 'default_salt', 'USER', 'test@ihdrs.com', 1)
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`);

CREATE TABLE user_log (
                          log_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
                          user_id BIGINT NOT NULL COMMENT '用户ID',
                          action VARCHAR(50) NOT NULL COMMENT '用户行为动作',
                          ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
                          user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
                          create_time DATETIME NOT NULL COMMENT '创建时间',

                          INDEX idx_user_id (user_id),
                          INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为日志表';

INSERT INTO user_log (user_id, action, ip_address, user_agent, create_time)
VALUES
    (1, 'LOGIN', '192.168.1.10', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', '2025-11-14 10:12:30'),

    (1, 'UPDATE_PROFILE', '192.168.1.10', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', '2025-11-14 10:20:15'),

    (2, 'LOGIN', '10.0.0.5', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', '2025-11-14 09:00:01'),

    (1, 'START_TRAINING', '192.168.1.10', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', '2025-11-14 11:05:44'),

    (2, 'DELETE_DATASET', '10.0.0.5', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', '2025-11-13 23:45:10');

-- 创建模型表
CREATE TABLE IF NOT EXISTS `models` (
                                        `model_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模型ID',
                                        `model_name` varchar(100) NOT NULL COMMENT '模型名称',
                                        `model_version` varchar(20) NOT NULL COMMENT '模型版本',
                                        `model_path` varchar(500) NOT NULL COMMENT '模型文件路径',
                                        `model_type` varchar(50) DEFAULT 'CNN' COMMENT '模型类型',
                                        `accuracy` decimal(5,4) DEFAULT NULL COMMENT '模型准确率',
                                        `loss` decimal(10,6) DEFAULT NULL COMMENT '损失值',
                                        `training_samples` int DEFAULT NULL COMMENT '训练样本数',
                                        `test_samples` int DEFAULT NULL COMMENT '测试样本数',
                                        `model_size` bigint DEFAULT NULL COMMENT '模型文件大小（字节）',
                                        `status` enum('TRAINING','COMPLETED','ACTIVE','DISABLED') DEFAULT 'TRAINING' COMMENT '模型状态',
                                        `description` text COMMENT '模型描述',
                                        `hyperparameters` json COMMENT '超参数配置',
                                        `creator_id` bigint NOT NULL COMMENT '创建者ID',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`model_id`),
                                        UNIQUE KEY `uk_model_version` (`model_name`,`model_version`),
                                        KEY `idx_creator_id` (`creator_id`),
                                        KEY `idx_status` (`status`),
                                        KEY `idx_accuracy` (`accuracy` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型表';

INSERT INTO `models` (`model_name`, `model_version`, `model_path`, `model_type`, `accuracy`, `training_samples`, `test_samples`, `status`, `description`, `creator_id`, `loss`, `model_size`) VALUES
    ('DefaultCNN', 'v1.0.0', 'models/default_cnn_v2.0.0.h5', 'CNN', 0.9200, 60000, 10000, 'ACTIVE', '默认卷积神经网络模型', 1, 0.03, 1000000)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

INSERT INTO `models` (`model_name`, `model_version`, `model_path`, `model_type`, `accuracy`, `training_samples`, `test_samples`, `status`, `description`, `creator_id`, `loss`, `model_size`) VALUES
                                                                                                                                                                                                  ('ImageClassifier', 'v1.0.0', 'models/best_model_checkpoint.h5', 'CNN', 0.8500, 50000, 8000, 'COMPLETED', '图像分类模型', 2, 0.03, 1100000),
                                                                                                                                                                                                  ('TextAnalyzer', 'v1.2.3', 'models/text_analyzer_v1.2.3.h5', 'RNN', 0.9100, 45000, 9000, 'COMPLETED', '文本分析模型', 2, 0.02, 1200000),
                                                                                                                                                                                                  ('FaceDetector', 'v1.1.0', 'models/face_detector_v1.1.0.h5', 'CNN', 0.9500, 30000, 5000, 'DISABLED', '人脸检测模型', 1, 0.01, 1100000),
                                                                                                                                                                                                  ('SentimentModel', 'v2.0.0', 'models/sentiment_model_v2.0.0.h5', 'LSTM', 0.8800, 40000, 10000, 'COMPLETED', '情感分析模型', 1, 0.05, 1000000),
                                                                                                                                                                                                  ('SpeechRecognizer', 'v3.5.0', 'models/speech_recognizer_v3.5.0.h5', 'DNN', 0.9300, 70000, 15000, 'DISABLED', '语音识别模型', 2, 0.04, 900000)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

-- 创建识别记录表
CREATE TABLE IF NOT EXISTS `recognition_records` (
                                                     `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                                     `user_id` bigint DEFAULT NULL COMMENT '用户ID（可为空，支持匿名识别）',
                                                     `model_id` bigint NOT NULL COMMENT '使用的模型ID',
                                                     `recognition_result` int COMMENT '识别结果（0-9）',
                                                     `confidence` decimal(5,4) NOT NULL COMMENT '置信度',
                                                     `image_data` longblob COMMENT '原始图像数据',
                                                     `image_path` varchar(500) COMMENT '图像文件路径',
                                                     `sequence_result` varchar(500) COMMENT '图像文件路径',
                                                     `image_hash` varchar(64) COMMENT '图像MD5哈希',
                                                     `input_type` enum('CANVAS','UPLOAD','MULTI','CAMERA') DEFAULT 'CANVAS' COMMENT '输入类型',
                                                     `processing_time` int DEFAULT NULL COMMENT '处理时间（毫秒）',
                                                     `client_info` json COMMENT '客户端信息',
                                                     `is_correct` tinyint DEFAULT NULL COMMENT '是否正确：1-正确，0-错误，NULL-未知',
                                                     `session_id` varchar(64) COMMENT '会话ID',
                                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     PRIMARY KEY (`record_id`),
                                                     KEY `idx_user_id` (`user_id`),
                                                     KEY `idx_model_id` (`model_id`),
                                                     KEY `idx_create_time` (`create_time`),
                                                     KEY `idx_result_confidence` (`recognition_result`,`confidence`),
                                                     KEY `idx_session_id` (`session_id`),
                                                     KEY `idx_image_hash` (`image_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='识别记录表';

INSERT INTO recognition_records (
    user_id, model_id, recognition_result, confidence,
    image_data, image_path, image_hash, input_type,
    processing_time, client_info, is_correct, session_id
) VALUES
      (3, 1, 7, 0.9845, NULL, '/api/uploads/digit7.png', 'a7b9c8d5e0f11223344556677889900a', 'UPLOAD', 132,
       JSON_OBJECT('device', 'Windows 10', 'browser', 'Edge', 'version', '142.0'), 1, 'session_001'),

      (2, 2, 2, 0.8794, NULL, '/api/uploads/digit2.png', 'b8f5c9e3a1d04567bb123abc9d0e1f2a', 'CANVAS', 95,
       JSON_OBJECT('device', 'Android', 'app_version', '1.2.3'), 1, 'session_002'),

      (1, 3, 9, 0.7563, NULL, '/api/uploads/digit9.png', 'd4a7b9f3e8c01234aabbccddeeff0011', 'CAMERA', 188,
       JSON_OBJECT('device', 'iPhone 14', 'os', 'iOS 18'), 0, 'session_003'),

      (3, 4, 0, 0.9931, NULL, '/api/uploads/digit0.png', 'aabbccddeeff00112233445566778899', 'CANVAS', 81,
       JSON_OBJECT('device', 'Windows 11', 'browser', 'Chrome', 'version', '142.0.0'), 1, 'session_004'),

      (3, 5, 5, 0.6238, NULL, '/api/uploads/digit5.png', '1234567890abcdef1234567890abcdef', 'UPLOAD', 142,
       JSON_OBJECT('device', 'MacBook Pro', 'os', 'macOS 15'), 1, 'session_005');

-- 创建训练任务表
CREATE TABLE IF NOT EXISTS `training_tasks` (
                                                `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                                                `task_name` varchar(100) NOT NULL COMMENT '任务名称',
                                                `creator_id` bigint NOT NULL COMMENT '创建者ID',
                                                `model_id` bigint DEFAULT NULL COMMENT '生成的模型ID',
                                                `dataset_config` json NOT NULL COMMENT '数据集配置',
                                                `training_config` json NOT NULL COMMENT '训练配置',
                                                `status` enum('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED') DEFAULT 'PENDING' COMMENT '任务状态',
                                                `progress` decimal(5,2) DEFAULT '0.00' COMMENT '进度百分比',
                                                `current_epoch` int DEFAULT '0' COMMENT '当前训练轮数',
                                                `total_epochs` int NOT NULL COMMENT '总训练轮数',
                                                `best_accuracy` decimal(5,4) DEFAULT NULL COMMENT '最佳准确率',
                                                `final_accuracy` decimal(5,4) DEFAULT NULL COMMENT '最终准确率',
                                                `final_loss` decimal(10,6) DEFAULT NULL COMMENT '最终损失值',
                                                `error_message` text COMMENT '错误信息',
                                                `start_time` datetime DEFAULT NULL COMMENT '开始时间',
                                                `end_time` datetime DEFAULT NULL COMMENT '结束时间',
                                                `estimated_time` int DEFAULT NULL COMMENT '预估时间（分钟）',
                                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                PRIMARY KEY (`task_id`),
                                                KEY `idx_creator_id` (`creator_id`),
                                                KEY `idx_status` (`status`),
                                                KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练任务表';

ALTER TABLE training_tasks
    ADD COLUMN confusion_matrix TEXT NULL,
    ADD COLUMN class_names TEXT NULL;

-- 创建反馈数据表
CREATE TABLE IF NOT EXISTS `feedback_data` (
                                               `feedback_id` bigint NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
                                               `record_id` bigint NOT NULL COMMENT '关联的识别记录ID',
                                               `user_id` bigint NOT NULL COMMENT '反馈用户ID',
                                               `original_result` int NOT NULL COMMENT '原始识别结果',
                                               `correct_result` int NOT NULL COMMENT '正确结果',
                                               `feedback_type` enum('WRONG_RESULT','LOW_CONFIDENCE','OTHER') DEFAULT 'WRONG_RESULT' COMMENT '反馈类型',
                                               `feedback_reason` varchar(500) COMMENT '反馈原因',
                                               `quality_score` int DEFAULT NULL COMMENT '图像质量评分（1-5）',
                                               `status` enum('PENDING','ACCEPTED','REJECTED') DEFAULT 'PENDING' COMMENT '反馈状态',
                                               `reviewer_id` bigint DEFAULT NULL COMMENT '审核者ID',
                                               `review_time` datetime DEFAULT NULL COMMENT '审核时间',
                                               `review_note` varchar(500) COMMENT '审核备注',
                                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               PRIMARY KEY (`feedback_id`),
                                               KEY `idx_record_id` (`record_id`),
                                               KEY `idx_user_id` (`user_id`),
                                               KEY `idx_status` (`status`),
                                               KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈数据表';

INSERT INTO `feedback_data`
(`record_id`, `user_id`, `original_result`, `correct_result`, `feedback_type`, `feedback_reason`, `quality_score`, `status`)
VALUES
    (1, 1, 3, 5, 'WRONG_RESULT', '识别结果偏差较大', 4, 'PENDING');

INSERT INTO `feedback_data`
(`record_id`, `user_id`, `original_result`, `correct_result`, `feedback_type`, `feedback_reason`, `quality_score`, `status`)
VALUES
    (2, 2, 7, 7, 'LOW_CONFIDENCE', '模型置信度很低，建议二次校验', 3, 'PENDING');

INSERT INTO `feedback_data`
(`record_id`, `user_id`, `original_result`, `correct_result`, `feedback_type`, `feedback_reason`, `quality_score`, `status`)
VALUES
    (3, 2, 9, 9, 'OTHER', '图像边缘模糊，可能需要增强处理', 2, 'PENDING');

INSERT INTO `feedback_data`
(`record_id`, `user_id`, `original_result`, `correct_result`, `feedback_type`, `feedback_reason`, `quality_score`, `status`)
VALUES
    (4, 1, 4, 1, 'WRONG_RESULT', '系统误识别为4', 5, 'PENDING');

INSERT INTO `feedback_data`
(`record_id`, `user_id`, `original_result`, `correct_result`, `feedback_type`, `feedback_reason`, `quality_score`, `status`)
VALUES
    (5, 2, 6, 6, 'LOW_CONFIDENCE', '识别置信度低于设定阈值', 3, 'PENDING');

-- 创建训练日志表
CREATE TABLE IF NOT EXISTS `training_logs` (
                                               `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                               `task_id` bigint NOT NULL COMMENT '训练任务ID',
                                               `epoch` int NOT NULL COMMENT '训练轮数',
                                               `step` int DEFAULT NULL COMMENT '步骤',
                                               `loss` decimal(10,6) NOT NULL COMMENT '损失值',
                                               `accuracy` decimal(5,4) DEFAULT NULL COMMENT '准确率',
                                               `val_loss` decimal(10,6) DEFAULT NULL COMMENT '验证损失',
                                               `val_accuracy` decimal(5,4) DEFAULT NULL COMMENT '验证准确率',
                                               `learning_rate` decimal(10,8) DEFAULT NULL COMMENT '学习率',
                                               `batch_size` int DEFAULT NULL COMMENT '批次大小',
                                               `message` text DEFAULT NULL COMMENT '终端信息',
                                               `timestamp` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
                                               PRIMARY KEY (`log_id`),
                                               KEY `idx_task_epoch` (`task_id`,`epoch`),
                                               KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='训练日志表';

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS `system_configs` (
                                                `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
                                                `config_key` varchar(100) NOT NULL COMMENT '配置键',
                                                `config_value` text NOT NULL COMMENT '配置值',
                                                `config_type` enum('STRING','NUMBER','BOOLEAN','JSON') DEFAULT 'STRING' COMMENT '配置类型',
                                                `description` varchar(500) COMMENT '配置描述',
                                                `is_public` tinyint DEFAULT '0' COMMENT '是否公开：1-是，0-否',
                                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                PRIMARY KEY (`config_id`),
                                                UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS `operation_logs` (
                                                `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                                `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
                                                `operation_type` varchar(50) NOT NULL COMMENT '操作类型',
                                                `operation_object` varchar(100) COMMENT '操作对象',
                                                `operation_detail` json COMMENT '操作详情',
                                                `ip_address` varchar(45) COMMENT 'IP地址',
                                                `user_agent` varchar(500) COMMENT '用户代理',
                                                `execution_time` int DEFAULT NULL COMMENT '执行时间（毫秒）',
                                                `result` enum('SUCCESS','FAILURE') DEFAULT 'SUCCESS' COMMENT '操作结果',
                                                `error_message` text COMMENT '错误信息',
                                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                PRIMARY KEY (`log_id`),
                                                KEY `idx_user_id` (`user_id`),
                                                KEY `idx_operation_type` (`operation_type`),
                                                KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 添加外键约束
ALTER TABLE `models` ADD CONSTRAINT `fk_models_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `recognition_records` ADD CONSTRAINT `fk_records_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `recognition_records` ADD CONSTRAINT `fk_records_model` FOREIGN KEY (`model_id`) REFERENCES `models` (`model_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `training_tasks` ADD CONSTRAINT `fk_tasks_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `training_tasks` ADD CONSTRAINT `fk_tasks_model` FOREIGN KEY (`model_id`) REFERENCES `models` (`model_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `feedback_data` ADD CONSTRAINT `fk_feedback_record` FOREIGN KEY (`record_id`) REFERENCES `recognition_records` (`record_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `feedback_data` ADD CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `feedback_data` ADD CONSTRAINT `fk_feedback_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `training_logs` ADD CONSTRAINT `fk_logs_task` FOREIGN KEY (`task_id`) REFERENCES `training_tasks` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `operation_logs` ADD CONSTRAINT `fk_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- 插入系统配置
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
                                                                                                           ('min_confidence_threshold', '0.8', 'NUMBER', '最小置信度阈值', 1),
                                                                                                           ('max_file_size', '5242880', 'NUMBER', '最大文件大小（字节）', 1),
                                                                                                           ('recognition_timeout', '10000', 'NUMBER', '识别超时时间（毫秒）', 0),
                                                                                                           ('default_model_config', '{"learning_rate": 0.001, "batch_size": 32, "epochs": 10}', 'JSON', '默认模型配置', 0),
                                                                                                           ('system_name', 'IHDRS', 'STRING', '系统名称', 1),
                                                                                                           ('system_version', '1.0.0', 'STRING', '系统版本', 1)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);


-- 插入一个示例MNIST数据集
INSERT INTO `datasets` (
    `dataset_name`,
    `dataset_type`,
    `description`,
    `file_path`,
    `file_size`,
    `num_classes`,
    `num_samples`,
    `train_samples`,
    `test_samples`,
    `image_width`,
    `image_height`,
    `class_names`,
    `status`,
    `is_public`,
    `creator_id`
) VALUES (
             'MNIST手写数字数据集',
             'IMAGE_CLASSIFICATION',
             '经典的手写数字识别数据集，包含0-9共10个类别',
             './datasets/mnist/dataset.zip',
             11594722,  -- 约11MB
             10,
             70000,
             60000,
             10000,
             28,
             28,
             '["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]',
             'AVAILABLE',
             1,  -- 设为公开
             1   -- 假设用户ID为1
         ) ON DUPLICATE KEY UPDATE dataset_id=dataset_id;  -- 避免重复插入