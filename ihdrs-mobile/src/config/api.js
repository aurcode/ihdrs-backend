// API Configuration
// Update these URLs to match your backend deployment

export const API_CONFIG = {
  // Java Backend URL (Spring Boot - Port 8080)
  BACKEND_URL: 'http://10.194.142.184:8080/api',
  
  // Python Model Service URL (Flask - Port 5000)
  MODEL_SERVICE_URL: 'http://10.194.142.184:5000',
  
  // Endpoints
  ENDPOINTS: {
    RECOGNIZE: '/recognition/recognize',
    HEALTH: '/health',
    MODELS: {
      BASE: '/models',
      LIST: '/models/list',
      ACTIVE: '/models/active',
    },
  },
  
  // Request timeout in milliseconds
  TIMEOUT: 30000,
};

// For production, you might want to use environment variables:
// export const API_CONFIG = {
//   BACKEND_URL: process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080',
//   MODEL_SERVICE_URL: process.env.REACT_APP_MODEL_SERVICE_URL || 'http://localhost:5000',
// };
