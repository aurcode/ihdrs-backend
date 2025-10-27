// API Configuration
// Update these URLs to match your backend deployment

export const API_CONFIG = {
  // Java Backend URL (Spring Boot - Port 8080)
  BACKEND_URL: 'http://localhost:8080',
  
  // Python Model Service URL (Flask - Port 5000)
  MODEL_SERVICE_URL: 'http://localhost:5000',
  
  // Endpoints
  ENDPOINTS: {
    RECOGNIZE: '/recognition/recognize',
    HEALTH: '/health',
  },
  
  // Request timeout in milliseconds
  TIMEOUT: 30000,
};

// For production, you might want to use environment variables:
// export const API_CONFIG = {
//   BACKEND_URL: process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080',
//   MODEL_SERVICE_URL: process.env.REACT_APP_MODEL_SERVICE_URL || 'http://localhost:5000',
// };
