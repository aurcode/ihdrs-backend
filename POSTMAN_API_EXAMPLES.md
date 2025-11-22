# Postman API Examples for Java Backend Prediction Endpoints

## Base Information
- **Base URL**: `http://localhost:8080` (adjust based on your Java backend port)
- **Authentication**: Bearer Token (optional for some endpoints)
- **Content-Type**: `application/json`

## 1. Single Digit Recognition

### Endpoint
```
POST /recognition/recognize
```

### Request Body
```json
{
    "imageData": "base64_encoded_image_string_here",
    "inputType": "CANVAS",
    "sessionId": "optional_session_id",
    "clientInfo": "{\"device\": \"web\", \"browser\": \"chrome\"}"
}
```

### Postman Configuration
```json
{
    "name": "Single Digit Recognition",
    "request": {
        "method": "POST",
        "header": [
            {
                "key": "Content-Type",
                "value": "application/json"
            },
            {
                "key": "Authorization",
                "value": "Bearer {{auth_token}}",
                "description": "Optional - include if you want to save to user history"
            }
        ],
        "body": {
            "mode": "raw",
            "raw": "{\n    \"imageData\": \"{{base64_image}}\",\n    \"inputType\": \"CANVAS\",\n    \"sessionId\": \"test_session_123\",\n    \"clientInfo\": \"{\\\"device\\\": \\\"web\\\", \\\"browser\\\": \\\"chrome\\\"}\"\n}"
        },
        "url": {
            "raw": "http://localhost:8080/recognition/recognize",
            "protocol": "http",
            "host": ["localhost"],
            "port": "8080",
            "path": ["recognition", "recognize"]
        }
    }
}
```

### Example with Base64 Image
```json
{
    "imageData": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
    "inputType": "CANVAS",
    "sessionId": "test_session_123",
    "clientInfo": "{\"device\": \"web\", \"browser\": \"chrome\"}"
}
```

### Expected Response
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "userId": 1,
        "recordId": 123,
        "recognitionResult": 7,
        "sequenceResult": "7",
        "confidence": 0.95,
        "processingTime": 45,
        "message": "Recognition successful",
        "needRewrite": false,
        "createTime": "2025-11-22T08:30:00",
        "imagePath": "/uploads/recognitions/20251122/image_123.png",
        "inputType": "CANVAS",
        "isCorrect": null,
        "probabilities": [0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.95, 0.08, 0.09],
        "probabilitiesMap": {
            "0": 0.01,
            "1": 0.02,
            "2": 0.03,
            "3": 0.04,
            "4": 0.05,
            "5": 0.06,
            "6": 0.07,
            "7": 0.95,
            "8": 0.08,
            "9": 0.09
        },
        "modelId": 1,
        "modelName": "CNN_MNIST_v1",
        "modelVersion": "1.0.0"
    }
}
```

## 2. Multi-Digit Recognition (Continuous Digits)

### Endpoint
```
POST /recognition/recognize_multi
```

### Request Body
```json
{
    "imageData": "base64_encoded_image_string_here",
    "inputType": "CANVAS",
    "sessionId": "optional_session_id",
    "clientInfo": "{\"device\": \"web\", \"browser\": \"chrome\"}"
}
```

### Postman Configuration
```json
{
    "name": "Multi-Digit Recognition",
    "request": {
        "method": "POST",
        "header": [
            {
                "key": "Content-Type",
                "value": "application/json"
            },
            {
                "key": "Authorization",
                "value": "Bearer {{auth_token}}",
                "description": "Optional - include if you want to save to user history"
            }
        ],
        "body": {
            "mode": "raw",
            "raw": "{\n    \"imageData\": \"{{base64_image_multi}}\",\n    \"inputType\": \"CANVAS\",\n    \"sessionId\": \"test_session_multi_123\",\n    \"clientInfo\": \"{\\\"device\\\": \\\"web\\\", \\\"browser\\\": \\\"chrome\\\"}\"\n}"
        },
        "url": {
            "raw": "http://localhost:8080/recognition/recognize_multi",
            "protocol": "http",
            "host": ["localhost"],
            "port": "8080",
            "path": ["recognition", "recognize_multi"]
        }
    }
}
```

### Expected Response
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "recordId": 124,
        "sequence": "123",
        "count": 3,
        "processingTime": 85,
        "results": [
            {
                "digit": 1,
                "confidence": 0.92,
                "position": 0,
                "probabilities": [0.05, 0.92, 0.01, 0.01, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0]
            },
            {
                "digit": 2,
                "confidence": 0.89,
                "position": 1,
                "probabilities": [0.02, 0.03, 0.89, 0.02, 0.01, 0.01, 0.01, 0.01, 0.0, 0.0]
            },
            {
                "digit": 3,
                "confidence": 0.94,
                "position": 2,
                "probabilities": [0.01, 0.01, 0.02, 0.94, 0.01, 0.01, 0.0, 0.0, 0.0, 0.0]
            }
        ],
        "message": "Multi-digit recognition successful",
        "needRewrite": false
    }
}
```

## 3. Get Recognition History

### Endpoint
```
GET /recognition/history_user
```

### Postman Configuration
```json
{
    "name": "Get User Recognition History",
    "request": {
        "method": "GET",
        "header": [
            {
                "key": "Authorization",
                "value": "Bearer {{auth_token}}",
                "description": "Required - user authentication"
            }
        ],
        "url": {
            "raw": "http://localhost:8080/recognition/history_user?page=0&size=10&result=7&startTime=2025-11-01&endTime=2025-11-22",
            "protocol": "http",
            "host": ["localhost"],
            "port": "8080",
            "path": ["recognition", "history_user"],
            "query": [
                {
                    "key": "page",
                    "value": "0",
                    "description": "Page number (0-based)"
                },
                {
                    "key": "size",
                    "value": "10",
                    "description": "Page size"
                },
                {
                    "key": "result",
                    "value": "7",
                    "description": "Filter by recognition result (optional)"
                },
                {
                    "key": "startTime",
                    "value": "2025-11-01",
                    "description": "Start date (yyyy-MM-dd or ISO format)"
                },
                {
                    "key": "endTime",
                    "value": "2025-11-22",
                    "description": "End date (yyyy-MM-dd or ISO format)"
                }
            ]
        }
    }
}
```

## 4. Delete Recognition Record

### Endpoint
```
DELETE /recognition/history/{recordId}
```

### Postman Configuration
```json
{
    "name": "Delete Recognition Record",
    "request": {
        "method": "DELETE",
        "header": [
            {
                "key": "Authorization",
                "value": "Bearer {{auth_token}}",
                "description": "Required - user authentication"
            }
        ],
        "url": {
            "raw": "http://localhost:8080/recognition/history/123",
            "protocol": "http",
            "host": ["localhost"],
            "port": "8080",
            "path": ["recognition", "history", "123"]
        }
    }
}
```

## Postman Environment Variables

Create a Postman environment with these variables:

```json
{
    "name": "IHDRS Java Backend",
    "values": [
        {
            "key": "base_url",
            "value": "http://localhost:8080",
            "enabled": true
        },
        {
            "key": "auth_token",
            "value": "your_jwt_token_here",
            "enabled": true
        },
        {
            "key": "base64_image",
            "value": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
            "enabled": true
        },
        {
            "key": "base64_image_multi",
            "value": "your_multi_digit_base64_image_here",
            "enabled": true
        }
    ]
}
```

## Testing Instructions

### 1. Authentication Flow
1. First, authenticate via the auth endpoints to get a JWT token
2. Use the token in the `Authorization` header as `Bearer {{auth_token}}`
3. Some endpoints work without authentication (anonymous recognition)

### 2. Image Preparation
- Convert your image to Base64 format
- For single digit: use a clear image of one handwritten digit
- For multi-digit: use an image with multiple handwritten digits in sequence

### 3. Testing Steps
1. **Test Single Recognition**: Use a single digit image
2. **Test Multi Recognition**: Use a multi-digit image  
3. **Test History**: After making some recognitions, check history
4. **Test Delete**: Delete specific records from history

### 4. Common Response Codes
- `200`: Success
- `400`: Bad Request (validation error)
- `401`: Unauthorized (missing or invalid token)
- `500`: Internal Server Error

### 5. Error Response Format
```json
{
    "code": 400,
    "message": "图像数据不能为空",
    "data": null
}
```

## Notes
- The Java backend uses Spring Boot with JWT authentication
- Image data must be Base64 encoded
- Processing time is returned in milliseconds
- Confidence scores range from 0.0 to 1.0
- The system supports both single and multi-digit recognition
- History records are optionally saved when authenticated
