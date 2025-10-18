-- init.sql - 数据库初始化脚本
-- 创建数据库
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

-- 创建识别记录表
CREATE TABLE IF NOT EXISTS `recognition_records` (
                                                     `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
                                                     `user_id` bigint DEFAULT NULL COMMENT '用户ID（可为空，支持匿名识别）',
                                                     `model_id` bigint NOT NULL COMMENT '使用的模型ID',
                                                     `recognition_result` int NOT NULL COMMENT '识别结果（0-9）',
                                                     `confidence` decimal(5,4) NOT NULL COMMENT '置信度',
                                                     `image_data` longblob COMMENT '原始图像数据',
                                                     `image_path` varchar(500) COMMENT '图像文件路径',
                                                     `image_hash` varchar(64) COMMENT '图像MD5哈希',
                                                     `input_type` enum('CANVAS','UPLOAD','CAMERA') DEFAULT 'CANVAS' COMMENT '输入类型',
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
                                               `status` enum('PENDING','REVIEWED','ACCEPTED','REJECTED') DEFAULT 'PENDING' COMMENT '反馈状态',
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
ALTER TABLE `feedback_data` ADD CONSTRAINT `fk_feedback_record` FOREIGN KEY (`record_id`) REFERENCES `recognition_records` (`record_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `feedback_data` ADD CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE `feedback_data` ADD CONSTRAINT `fk_feedback_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE `training_logs` ADD CONSTRAINT `fk_logs_task` FOREIGN KEY (`task_id`) REFERENCES `training_tasks` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `operation_logs` ADD CONSTRAINT `fk_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- 插入初始数据
-- 插入默认管理员用户 (密码: admin123)
INSERT INTO `users` (`username`, `password_hash`, `salt`, `role`, `email`, `status`) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi7X7L4NXMHpnsKp7x6GzwxK', 'default_salt', 'ADMIN', 'admin@ihdrs.com', 1)
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`);

-- 插入默认测试用户 (密码: test123)
INSERT INTO `users` (`username`, `password_hash`, `salt`, `role`, `email`, `status`) VALUES
    ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKi7X7L4NXMHpnsKp7x6GzwxK', 'default_salt', 'USER', 'test@ihdrs.com', 1)
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`);

-- 插入系统配置
INSERT INTO `system_configs` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
                                                                                                           ('min_confidence_threshold', '0.8', 'NUMBER', '最小置信度阈值', 1),
                                                                                                           ('max_file_size', '5242880', 'NUMBER', '最大文件大小（字节）', 1),
                                                                                                           ('recognition_timeout', '10000', 'NUMBER', '识别超时时间（毫秒）', 0),
                                                                                                           ('default_model_config', '{"learning_rate": 0.001, "batch_size": 32, "epochs": 10}', 'JSON', '默认模型配置', 0),
                                                                                                           ('system_name', 'IHDRS', 'STRING', '系统名称', 1),
                                                                                                           ('system_version', '1.0.0', 'STRING', '系统版本', 1)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- 创建默认模型记录（示例）
INSERT INTO `models` (`model_name`, `model_version`, `model_path`, `model_type`, `accuracy`, `training_samples`, `test_samples`, `status`, `description`, `creator_id`) VALUES
    ('DefaultCNN', 'v1.0.0', 'models/default_cnn_v1.0.0.h5', 'CNN', 0.9200, 60000, 10000, 'ACTIVE', '默认卷积神经网络模型', 1)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);