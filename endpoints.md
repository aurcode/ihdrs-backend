Based on my analysis of the codebase, I've identified all the endpoints for using the model in your IHDRS (Intelligent Handwritten Digit Recognition System). The system has two main components:

## **Python Model Service** (Flask - Port typically 5000)

### Recognition Endpoints:
1. **POST `/api/recognize`** - Main digit recognition endpoint
   - Accepts Base64-encoded image data
   - Returns predicted digit, confidence score, and all probabilities
   - Located in: `ihdrs-model-service/api/recognition.py`

### Training Endpoints:
2. **POST `/api/train`** - Start model training
   - Accepts training configuration (epochs, batch_size, learning_rate, etc.)
   - Runs training in background thread
   - Located in: `ihdrs-model-service/api/training.py`

3. **GET `/api/train/status`** - Get training status
   - Returns current training progress and status
   - Located in: `ihdrs-model-service/api/training.py`

### Health Check Endpoints:
4. **GET `/health`** - Comprehensive health check
   - Checks TensorFlow, models, system resources, filesystem
   - Located in: `ihdrs-model-service/api/health.py`

5. **GET `/ping`** - Simple connectivity test
   - Returns basic "pong" response
   - Located in: `ihdrs-model-service/api/health.py`

## **Java Backend Service** (Spring Boot - Port typically 8080)

### Recognition Endpoints:
6. **POST `/recognition/recognize`** - Recognition proxy endpoint
   - Forwards requests to Python model service
   - Tracks recognition records in database
   - No authentication required
   - Located in: `src/main/java/com/ihdrs/backend/controller/RecognitionController.java`

### Model Management Endpoints:
7. **GET `/models/list`** - Get paginated model list
8. **GET `/models/active`** - Get currently active model
9. **GET `/models/{modelId}`** - Get specific model details
10. **PUT `/models/{modelId}/activate`** - Switch active model
   - All located in: `src/main/java/com/ihdrs/backend/controller/ModelController.java`

## **Architecture Flow:**

```
Frontend → Java Backend (/recognition/recognize) → Python Model Service (/api/recognize) → TensorFlow Model
```

The Java backend acts as a gateway that:
- Handles authentication/authorization
- Stores recognition records in database
- Manages model metadata
- Proxies requests to the Python model service

The Python service handles:
- Actual ML model inference
- Model training
- Image preprocessing
- Direct TensorFlow operations

Would you like me to provide more details about any specific endpoint, such as request/response formats or how to test them?