# IHDRS API Documentation

## Overview
IHDRS (Intelligent Handwritten Digit Recognition System) consists of two main services:
1. **Java Backend Service** (Spring Boot) - Port 8080
2. **Python Model Service** (Flask) - Port 5000

## Java Backend Service APIs (Port 8080)

### Authentication APIs
Base Path: `/api/auth`

#### 1. User Registration
- **Endpoint**: `POST /auth/register`
- **Description**: Register a new user account
- **Request Body**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "confirmPassword": "string"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "string",
    "email": "string",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

#### 2. User Login
- **Endpoint**: `POST /auth/login`
- **Description**: User login to obtain JWT token
- **Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "string",
      "email": "string",
      "role": "USER"
    }
  }
}
```

#### 3. Validate Token
- **Endpoint**: `GET /auth/validate`
- **Description**: Validate JWT token validity
- **Headers**: `Authorization: Bearer <token>`
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "string",
    "email": "string",
    "role": "USER"
  }
}
```

### Recognition APIs
Base Path: `/api/recognition`

#### 4. Single Digit Recognition
- **Endpoint**: `POST /recognition/recognize`
- **Description**: Recognize a single handwritten digit (anonymous or authenticated)
- **Request Body**:
```json
{
  "image": "base64_encoded_image_string",
  "modelId": 1
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "result": 7,
    "confidence": 0.95,
    "processingTime": 150,
    "allProbabilities": [0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.95, 0.08, 0.09],
    "recordId": 123
  }
}
```

#### 5. Multi-Digit Recognition
- **Endpoint**: `POST /recognition/recognize_multi`
- **Description**: Recognize multiple handwritten digits in one image
- **Request Body**:
```json
{
  "image": "base64_encoded_image_string",
  "modelId": 1
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "count": 3,
    "results": [
      {
        "digit": 1,
        "confidence": 0.92,
        "allProbabilities": [0.02, 0.92, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01]
      },
      {
        "digit": 2,
        "confidence": 0.89,
        "allProbabilities": [0.01, 0.02, 0.89, 0.02, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01]
      },
      {
        "digit": 3,
        "confidence": 0.94,
        "allProbabilities": [0.01, 0.01, 0.02, 0.94, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01]
      }
    ],
    "processingTime": 250
  }
}
```

#### 6. Get Recognition History (User)
- **Endpoint**: `GET /recognition/history_user`
- **Description**: Get current user's recognition history
- **Headers**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `page` (default: 0)
  - `size` (default: 10)
  - `result` (optional): Filter by digit result
  - `startTime` (optional): Filter by start time (yyyy-MM-dd or ISO format)
  - `endTime` (optional): Filter by end time (yyyy-MM-dd or ISO format)
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 123,
        "result": 7,
        "confidence": 0.95,
        "imageUrl": "/uploads/recognition/2025/01/01/image_123.png",
        "createdAt": "2025-01-01T12:00:00",
        "modelName": "CNN_v1.0"
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "currentPage": 0,
    "size": 10
  }
}
```

#### 7. Delete Recognition Record
- **Endpoint**: `DELETE /recognition/history/{recordId}`
- **Description**: Delete a specific recognition record
- **Headers**: `Authorization: Bearer <token>`
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### Model Management APIs
Base Path: `/api/models`

#### 8. Get Model List
- **Endpoint**: `GET /models/list`
- **Description**: Get paginated list of available models
- **Query Parameters**:
  - `page` (default: 0)
  - `size` (default: 10)
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "CNN_v1.0",
        "version": "1.0.0",
        "accuracy": 0.98,
        "status": "ACTIVE",
        "createdAt": "2025-01-01T00:00:00",
        "description": "Default CNN model"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "currentPage": 0,
    "size": 10
  }
}
```

#### 9. Get Active Model
- **Endpoint**: `GET /models/active`
- **Description**: Get currently active model
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "CNN_v1.0",
    "version": "1.0.0",
    "accuracy": 0.98,
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

#### 10. Switch Active Model
- **Endpoint**: `PUT /models/{modelId}/activate`
- **Description**: Switch to a different model
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### Dataset APIs
Base Path: `/api/datasets`

#### 11. Upload Dataset
- **Endpoint**: `POST /datasets/upload`
- **Description**: Upload a new training dataset
- **Headers**: `Authorization: Bearer <token>`
- **Request Body** (multipart/form-data):
  - `file`: Dataset file (CSV/JSON)
  - `name`: Dataset name
  - `description`: Dataset description
  - `isPublic`: Whether dataset is public
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "MNIST_Extended",
    "description": "Extended MNIST dataset",
    "fileUrl": "/uploads/datasets/dataset_1.csv",
    "size": 10485760,
    "recordCount": 10000,
    "isPublic": true,
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

### Training Task APIs
Base Path: `/api/training`

#### 12. Create Training Task
- **Endpoint**: `POST /training/tasks`
- **Description**: Create a new model training task
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
```json
{
  "taskName": "Train CNN v2.0",
  "datasetId": 1,
  "modelConfig": {
    "epochs": 50,
    "batchSize": 32,
    "learningRate": 0.001,
    "modelType": "CNN"
  }
}
```
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "taskName": "Train CNN v2.0",
    "status": "PENDING",
    "progress": 0,
    "createdAt": "2025-01-01T00:00:00"
  }
}
```

### Statistics APIs
Base Path: `/api/stats`

#### 13. Get Dashboard Statistics
- **Endpoint**: `GET /stats/dashboard`
- **Description**: Get dashboard statistics
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalRecognitions": 10000,
    "totalUsers": 500,
    "totalModels": 5,
    "totalDatasets": 10,
    "todayRecognitions": 150,
    "activeUsers": 50,
    "averageConfidence": 0.92,
    "accuracyRate": 0.95
  }
}
```

### Health Check APIs
Base Path: `/api`

#### 14. Health Check
- **Endpoint**: `GET /health`
- **Description**: System health check
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "healthy",
    "timestamp": 1640995200,
    "service": "ihdrs-backend",
    "database": "connected",
    "redis": "connected",
    "modelService": "connected"
  }
}
```

#### 15. Ping
- **Endpoint**: `GET /ping`
- **Description**: Simple connectivity test
- **Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": "pong"
}
```

## Python Model Service APIs (Port 5000)

### Recognition APIs
Base Path: `/api`

#### 16. Single Digit Recognition
- **Endpoint**: `POST /api/recognize`
- **Description**: Recognize a single handwritten digit
- **Request Body**:
```json
{
  "image": "base64_encoded_image_string",
  "model_id": 1
}
```
- **Response**:
```json
{
  "status": "success",
  "data": {
    "result": 7,
    "confidence": 0.95,
    "processing_time": 150,
    "all_probabilities": [0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.95, 0.08, 0.09]
  }
}
```

#### 17. Multi-Digit Recognition
- **Endpoint**: `POST /api/recognize_multi`
- **Description**: Recognize multiple digits in one image
- **Request Body**:
```json
{
  "image": "base64_encoded_image_string",
  "model_id": 1
}
```
- **Response**:
```json
{
  "status": "success",
  "data": {
    "count": 3,
    "results": [
      {
        "digit": 1,
        "confidence": 0.92,
        "all_probabilities": [0.02, 0.92, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01]
      }
    ],
    "processing_time": 250
  }
}
```

### Training APIs
Base Path: `/api`

#### 18. Start Training
- **Endpoint**: `POST /api/train`
- **Description**: Start a model training task
- **Request Body**:
```json
{
  "taskId": 1,
  "taskName": "Train CNN v2.0",
  "trainingConfig": {
    "epochs": 50,
    "batch_size": 32,
    "learning_rate": 0.001
  },
  "datasetConfig": {
    "dataset_id": 1,
    "validation_split": 0.2
  }
}
```
- **Response**:
```json
{
  "status": "success",
  "message": "训练任务已启动",
  "data": {
    "taskId": 1,
    "taskName": "Train CNN v2.0"
  }
}
```

#### 19. Cancel Training
- **Endpoint**: `POST /api/train/cancel`
- **Description**: Cancel a training task
- **Request Body**:
```json
{
  "taskId": 1
}
```
- **Response**:
```json
{
  "status": "success",
  "message": "训练任务已取消"
}
```

### Model Management APIs
Base Path: `/api`

#### 20. Activate Model
- **Endpoint**: `POST /api/models/activate`
- **Description**: Activate a specific model
- **Request Body**:
```json
{
  "model_id": 1,
  "model_path": "/app/models/model_v2.0.h5"
}
```
- **Response**:
```json
{
  "status": "success",
  "active_model_id": 1
}
```

### Health Check APIs
Base Path: `/`

#### 21. Health Check
- **Endpoint**: `GET /health`
- **Description**: Comprehensive health check
- **Response**:
```json
{
  "status": "healthy",
  "timestamp": 1640995200,
  "service": "ihdrs-model-service",
  "version": "1.0.0",
  "checks": {
    "tensorflow": {
      "status": "ok",
      "version": "2.13.0",
      "gpu_available": true
    },
    "models": {
      "status": "ok",
      "loaded_models": 3,
      "active_model_id": 1
    },
    "system": {
      "status": "ok",
      "memory_usage": "45%",
      "disk_usage": "30%",
      "cpu_count": 8
    },
    "filesystem": {
      "status": "ok",
      "model_directory": true,
      "log_directory": true
    }
  }
}
```

#### 22. Ping
- **Endpoint**: `GET /ping`
- **Description**: Simple connectivity test
- **Response**:
```json
{
  "status": "ok",
  "message": "pong",
  "timestamp": 1640995200
}
```

## Error Responses

All APIs return consistent error responses:

```json
{
  "code": 400,
  "message": "error",
  "data": "Detailed error message"
}
```

Common HTTP status codes:
- `200`: Success
- `400`: Bad Request
- `401`: Unauthorized
- `403`: Forbidden
- `404`: Not Found
- `500`: Internal Server Error

## Authentication

The Java backend uses JWT (JSON Web Token) authentication:
1. Login via `/auth/login` to obtain a token
2. Include the token in subsequent requests: `Authorization: Bearer <token>`
3. Some endpoints allow anonymous access (marked in documentation)

## Rate Limiting

- Anonymous requests: 100 requests per hour per IP
- Authenticated requests: 1000 requests per hour per user
- Model training: 1 concurrent training per user

## File Upload Limits

- Maximum file size: 5MB
- Supported image formats: PNG, JPEG, JPG
- Dataset files: CSV, JSON format

## Model Information

- Default model: CNN_v1.0 (accuracy: 98%)
- Model switching: Real-time without service restart
- Supported model types: CNN, ResNet, DenseNet

## Performance Metrics

- Average recognition time: 150ms
- Model loading time: 2-5 seconds
- Training time: 30-60 minutes depending on dataset size

## Docker Deployment

The system is designed to run in Docker containers:
- Java Backend: Port 8080
- Python Model Service: Port 5000
- PostgreSQL: Port 5432
- Redis: Port 6379

Health checks are implemented for all services with automatic restart policies.
