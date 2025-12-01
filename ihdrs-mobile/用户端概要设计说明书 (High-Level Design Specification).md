# 概要设计说明书 (High-Level Design Specification)

------

## **手写数字识别系统（用户端）概要设计说明书**

**Intelligent Handwritten Digit Recognition System - User Client**
**High-Level Design Specification**

**版本:** 1.4.0
**日期:** 2025-12-01
**项目分支:** IHDRS-Mobile
**编写人:** 刘家乐

------

## **目录**

[TOC]



------

## **1.引言**

### **1.1 编写目的**

本文档旨在描述手写数字识别系统用户端（移动端）的概要设计，为详细设计、开发实施和系统测试提供基础。主要读者包括：

- 系统架构师
- 开发工程师
- 测试工程师
- 项目管理人员

### **1.2 背景**

**项目名称:** 智能手写数字识别系统（用户端）
**开发单位:** 留学生第一组
**用户:** 普通用户、系统管理员
**主要功能:**

- 手写数字识别（单数字/多数字）
- 用户认证与授权
- 识别历史管理
- 用户反馈管理
- 个人信息管理

### **1.3 定义与缩略语**

| 缩略语 | 英文全称                                                | 中文含义                         |
| ------ | ------------------------------------------------------- | -------------------------------- |
| IHDRS  | Intelligent Handwritten Digit Recognition System        | 智能手写数字识别系统             |
| API    | Application Programming Interface                       | 应用程序编程接口                 |
| JWT    | JSON Web Token                                          | JSON网络令牌                     |
| CNN    | Convolutional Neural Network                            | 卷积神经网络                     |
| REST   | Representational State Transfer                         | 表现层状态转换                   |
| MNIST  | Modified National Institute of Standards and Technology | 修改后的国家标准技术研究所数据库 |

### **1.4 参考资料**

1. IEEE Std 1016-2009 软件设计说明推荐实践
2. React Native 官方文档
3. Spring Boot 参考手册
4. MySQL 8.0 参考手册

------

## **2.系统概述**

### **2.1 系统目标**

开发一款基于React Native的跨平台移动应用，提供高精度手写数字识别服务，支持用户交互、历史管理和反馈机制。

### **2.2 系统特点**

1. **跨平台支持**: 基于React Native，同时支持iOS和Android
2. **实时识别**: 毫秒级识别响应
3. **多输入方式**: 支持手写画布、相册上传、相机拍摄
4. **用户友好**: 现代化UI设计，流畅的动画效果
5. **安全可靠**: JWT认证、数据加密传输

### **2.3 运行环境**

**客户端:**

- iOS 13.0 及以上
- Android 8.0 (API Level 26) 及以上
- React Native 0.72+

**服务端:**

- Java 17+
- Spring Boot 3.2.0
- Python 3.8+ (模型服务)
- MySQL 8.0+

------

## **3.系统架构设计**

### **3.1 总体架构**

系统采用**三层架构 + 微服务**设计模式：

```mermaid
flowchart TB
    %% ==== Layers ====

    %% Mobile Client Layer
    subgraph mobileLayer["移动客户端层 (Mobile Client)"]
        subgraph rnApp["React Native Application"]
            direction TB
            ui["UI Components"]
            service["Service Layer"]
            state["State Management"]
        end
    end

    %% Backend Services Layer
    subgraph backendLayer["应用服务层 (Backend Services)"]
        java["Java Backend\n(Spring Boot)\nPort: 8080"]
        python["Python Model Service\n(Flask)\nPort: 5000"]
    end

    %% Data Layer
    subgraph dataLayer["数据持久层 (Data Layer)"]
        mysql["MySQL 8.0\n(Primary DB)"]
        fileStore["File Storage\n(Images/Models)"]
    end

    %% ==== Connections ====
    rnApp -->|"HTTPS/REST API"| java
    rnApp -->|"HTTPS/REST API"| python

    java -->|"JDBC"| mysql
    java -->|"HTTP"| fileStore
    python -->|"HTTP"| fileStore
```

### **3.2 架构模式**

#### **3.2.1 客户端架构**

采用 **组件化 + 服务化** 模式：

```
src/
├── components/          # 可复用组件
│   ├── DrawingCanvas.js
│   ├── ImagePickerComponent.js
│   └── RecognitionHistory.js
├── screens/             # 页面级组件
│   ├── MainScreen.js
│   ├── LoginScreen.js
│   ├── RegisterScreen.js
│   ├── ProfileScreen.js
│   ├── HistoryScreen.js
│   └── FeedbackScreen.js
├── services/            # 业务服务层
│   ├── authService.js
│   ├── recognitionService.js
│   ├── historyService.js
│   ├── feedbackService.js
│   └── userService.js
└── config/              # 配置文件
    └── api.js
```

#### **3.2.2 服务端架构**

采用 **MVC + 微服务** 模式：

```
Java Backend (Spring Boot):
├── controller/          # 控制层
├── service/             # 业务逻辑层
├── dao/                 # 数据访问层
├── entity/              # 实体类
├── dto/                 # 数据传输对象
└── util/                # 工具类

Python Model Service (Flask):
├── app.py               # 主应用
├── models/              # 模型文件
└── utils/               # 工具函数
```

### **3.3 通信机制**

1. **客户端 ↔ Java后端**: RESTful API (JSON over HTTPS)
2. **Java后端 ↔ Python服务**: HTTP/JSON
3. **认证方式**: JWT (JSON Web Token)
4. **数据格式**: 统一JSON响应格式

------

## **4.数据库总体设计**

### **4.1 数据库架构**

**数据库管理系统:** MySQL 8.0
**字符集:** UTF8MB4
**排序规则:** utf8mb4_unicode_ci
**存储引擎:** InnoDB

### **4.2 数据库主要实体关系图 (ER图)**

```mermaid
erDiagram

    %% =========================
    %%        Tables
    %% =========================

    users {
        int user_id PK
        string username
        string password
        string role
        string email
    }

    recognition_records {
        int record_id PK
        int user_id FK
        int model_id FK
        string result
        float confidence
        string image_path
    }

    models {
        int model_id PK
        string model_name
        string version
        float accuracy
        string status
    }

    feedback_data {
        int feedback_id PK
        int record_id FK
        int user_id FK
        string original_result
        string correct_result
        string status
    }

    training_tasks {
        int task_id PK
        int creator_id FK
        int model_id FK
        string status
        int progress
    }

    %% =========================
    %%     Relationships
    %% =========================

    users ||--o{ recognition_records : "1 to many"
    models ||--o{ recognition_records : "1 to many"

    users ||--o{ feedback_data : "1 to many"
    recognition_records ||--|| feedback_data : "1 to 1"

    users ||--o{ training_tasks : "1 to many"
    models ||--o{ training_tasks : "1 to many"
```

### **4.3 核心数据表概览**

| 表名                    | 用途         | 主要字段                                                     |
| ----------------------- | ------------ | ------------------------------------------------------------ |
| **users**               | 用户信息     | user_id, username, password_hash, role, email                |
| **models**              | 模型信息     | model_id, model_name, version, accuracy, status              |
| **recognition_records** | 识别记录     | record_id, user_id, model_id, result, confidence, image_path |
| **feedback_data**       | 用户反馈     | feedback_id, record_id, user_id, original_result, correct_result, status |
| **training_tasks**      | 训练任务     | task_id, creator_id, model_id, status, progress              |
| **training_logs**       | 训练日志     | log_id, task_id, epoch, loss, accuracy                       |
| **datasets**            | 数据集       | dataset_id, dataset_name, file_path, num_samples             |
| **system_configs**      | 系统配置     | config_id, config_key, config_value                          |
| **operation_logs**      | 操作日志     | log_id, user_id, operation_type, result                      |
| **user_log**            | 用户行为日志 | log_id, user_id, action, ip_address                          |

### **4.4 数据完整性约束**

#### **4.4.1 实体完整性**

- 所有表均有主键约束 (PRIMARY KEY)
- 关键字段设置 NOT NULL

#### **4.4.2 参照完整性**

SQL

```
-- 识别记录关联用户和模型
FOREIGN KEY (user_id) REFERENCES users(user_id) 
  ON DELETE SET NULL ON UPDATE CASCADE
FOREIGN KEY (model_id) REFERENCES models(model_id) 
  ON DELETE RESTRICT ON UPDATE CASCADE

-- 反馈数据关联识别记录和用户
FOREIGN KEY (record_id) REFERENCES recognition_records(record_id) 
  ON DELETE CASCADE
FOREIGN KEY (user_id) REFERENCES users(user_id) 
  ON DELETE CASCADE
```

#### **4.4.3 域完整性**

- 使用 ENUM 类型限定状态字段值
- 使用 CHECK 约束验证数值范围
- 设置字段长度限制

### **4.5 索引策略**

| 表名                | 索引类型 | 索引字段                       | 用途             |
| ------------------- | -------- | ------------------------------ | ---------------- |
| users               | UNIQUE   | username                       | 用户名唯一性约束 |
| users               | INDEX    | email, status                  | 快速查询         |
| recognition_records | INDEX    | user_id, model_id, create_time | 历史记录查询     |
| feedback_data       | INDEX    | record_id, user_id, status     | 反馈管理         |
| training_tasks      | INDEX    | creator_id, status             | 任务查询         |

------

## **5.功能模块设计**

### **5.1 功能结构图**

```mermaid
flowchart TB

    subgraph system["手写数字识别系统（用户端）"]

        %% 1. 用户认证模块
        subgraph auth["1. 用户认证模块"]
            auth_reg["- 用户注册"]
            auth_login["- 用户登录"]
            auth_token["- Token验证"]
        end

        %% 2. 数字识别模块
        subgraph recog["2. 数字识别模块"]
            recog_canvas["- 画布手写识别"]
            recog_upload["- 图片上传识别"]
            recog_camera["- 相机拍照识别"]
            recog_seq["- 多数字序列识别"]
        end

        %% 3. 历史管理模块
        subgraph history["3. 历史管理模块"]
            hist_query["- 识别历史查询"]
            hist_detail["- 记录详情查看"]
            hist_delete["- 记录删除（单个/批量）"]
        end

        %% 4. 反馈管理模块
        subgraph feedback["4. 反馈管理模块"]
            fb_submit["- 提交识别反馈"]
            fb_list["- 查看反馈列表"]
            fb_detail["- 反馈详情查看"]
        end

        %% 5. 个人信息管理模块
        subgraph usermgmt["5. 个人信息管理模块"]
            user_info["- 个人信息查看"]
            user_edit["- 个人信息修改"]
            user_pwd["- 密码修改"]
        end
    end
```

### **5.2 模块功能描述**

#### **5.2.1 用户认证模块**

**功能描述:**
提供用户注册、登录、Token验证功能，确保系统安全访问。

**主要功能:**

1. **用户注册**: 验证用户名唯一性、密码强度，创建用户账户
2. **用户登录**: 验证凭据，生成JWT令牌
3. **Token验证**: 验证令牌有效性，获取用户信息

**相关组件:**

- `LoginScreen.js`: 登录界面
- `RegisterScreen.js`: 注册界面
- `authService.js`: 认证服务

#### **5.2.2 数字识别模块**

**功能描述:**
核心业务模块，提供多种方式的手写数字识别功能。

**主要功能:**

1. **画布手写识别**: 在画布上绘制数字，实时识别
2. **图片上传识别**: 从相册选择图片进行识别
3. **相机拍照识别**: 使用相机拍摄数字图片识别
4. **多数字序列识别**: 识别包含多个数字的图片

**相关组件:**

- `DrawingCanvas.js`: 绘图画布组件
- `ImagePickerComponent.js`: 图片选择组件
- `MainScreen.js`: 主界面
- `recognitionService.js`: 识别服务

#### **5.2.3 历史管理模块**

**功能描述:**
管理用户的识别历史记录，支持查询、查看、删除操作。

**主要功能:**

1. **历史记录查询**: 分页查询用户的识别历史
2. **记录详情查看**: 查看识别记录的详细信息（图片、置信度、概率分布）
3. **记录删除**: 支持单个或批量删除记录

**相关组件:**

- `HistoryScreen.js`: 历史记录界面
- `RecognitionHistory.js`: 历史记录列表组件
- `historyService.js`: 历史记录服务

#### **5.2.4 反馈管理模块**

**功能描述:**
允许用户对识别结果提交反馈，帮助改进模型。

**主要功能:**

1. **提交识别反馈**: 标记错误结果，提供正确答案
2. **查看反馈列表**: 查询用户提交的反馈记录
3. **反馈详情查看**: 查看反馈的详细信息和处理状态

**相关组件:**

- `FeedbackScreen.js`: 反馈界面
- `feedbackService.js`: 反馈服务

#### **5.2.5 个人信息管理模块**

**功能描述:**
管理用户个人信息和账户设置。

**主要功能:**

1. **个人信息查看**: 显示用户账户信息
2. **个人信息修改**: 修改邮箱、手机号等信息
3. **密码修改**: 修改登录密码

**相关组件:**

- `ProfileScreen.js`: 个人中心界面
- `userService.js`: 用户服务

------

## **6.接口设计概要**

### **6.1 API接口规范**

**基础URL:**

- Java后端: `http://192.168.193.1:8080/api`
- Python服务: `http://192.168.193.1:5000`

**请求格式:** JSON
**响应格式:** 统一JSON结构

JSON

```
{
  "code": 200,
  "msg": "操作成功",
  "data": { }
}
```

### **6.2 核心接口分类**

| 分类         | 接口数量 | 主要接口                                                     |
| ------------ | -------- | ------------------------------------------------------------ |
| **认证接口** | 3        | `/auth/register`, `/auth/login`, `/auth/validate`            |
| **识别接口** | 2        | `/recognition/recognize`, `/recognition/recognize_multi`     |
| **历史接口** | 3        | `/recognition/history_user`, `/recognition/history/{id}`, `/recognition/history/batch` |
| **反馈接口** | 4        | `/feedback`, `/feedback/my-feedback`, `/feedback/{id}`, `/feedback/statistics` |
| **用户接口** | 3        | `/users/me`, `/users/me/password`, `/users/check-username`   |

### **6.3 认证机制**

**认证方式:** Bearer Token (JWT)

**请求头示例:**

HTTP

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

------

## **7.安全性设计**

### **7.1 认证与授权**

1. **密码加密**: BCrypt加密算法，加盐处理
2. **Token机制**: JWT令牌，设置过期时间
3. **权限控制**: 基于角色的访问控制 (RBAC)

### **7.2 数据安全**

1. **传输加密**: HTTPS协议
2. **敏感数据**: 密码、Token等敏感信息加密存储
3. **SQL注入防护**: 使用预编译语句 (PreparedStatement)

### **7.3 输入验证**

1. **客户端验证**: 表单字段格式、长度验证
2. **服务端验证**: 双重验证，防止绕过
3. **XSS防护**: 过滤特殊字符，转义输出

------

## **8.性能设计**

### **8.1 性能指标**

| 指标         | 目标值   |
| ------------ | -------- |
| 识别响应时间 | < 1000ms |
| 接口响应时间 | < 500ms  |
| 并发用户数   | 500+     |

### **8.2 优化策略**

1. **前端优化**:
   - 懒加载组件
   - 图片压缩
   - 缓存机制
2. **后端优化**:
   - 数据库索引
   - 查询优化
   - 连接池管理
3. **网络优化**:
   - CDN加速
   - Gzip压缩
   - 请求合并

------

## **9.附录**

### **9.1 开发技术栈**

**前端:**

- React Native 0.72
- React Native Svg
- Expo Image Picker
- Axios

**后端:**

- Spring Boot 3.2.0
- Spring Security
- MyBatis Plus
- Flask 2.3.0

**数据库:**

- MySQL 8.0

**工具:**

- Maven 3.8+
- npm/yarn
- Docker

### **9.2 术语表**

| 术语     | 解释                               |
| -------- | ---------------------------------- |
| 置信度   | 模型对识别结果的可信程度，范围 0-1 |
| Base64   | 一种图像编码方式                   |
| 会话ID   | 用于追踪用户会话的唯一标识         |
| 混淆矩阵 | 用于评估分类模型性能的矩阵         |