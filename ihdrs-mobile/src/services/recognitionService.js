import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * Recognition Service
 * Handles communication with the backend API for digit recognition
 */

class RecognitionService {
  constructor() {
    this.api = axios.create({
      baseURL: API_CONFIG.BACKEND_URL,
      timeout: API_CONFIG.TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  /**
   * Recognize a digit from a base64 encoded image
   * @param {string} base64Image - Base64 encoded image string
   * @param {string} inputType - Type of input (CANVAS, UPLOAD, CAMERA)
   * @param {string} sessionId - Session identifier
   * @param {object} clientInfo - Client information
   * @returns {Promise} Recognition result with predicted digit and confidence
   */
  async recognizeDigit(base64Image, inputType = 'CANVAS', sessionId = null, clientInfo = null) {
    try {
      const requestBody = {
        imageData: base64Image,
        inputType: inputType,
      };

      // Add optional parameters if provided
      if (sessionId) {
        requestBody.sessionId = sessionId;
      }
      if (clientInfo) {
        requestBody.clientInfo = JSON.stringify(clientInfo);
      }

      const response = await this.api.post(API_CONFIG.ENDPOINTS.RECOGNIZE, requestBody);

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Recognition error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Recognition failed',
      };
    }
  }

  /**
   * Get list of available models
   * @param {number} current - Current page number
   * @param {number} size - Page size
   * @returns {Promise} List of models
   */
  async getModelList(current = 1, size = 10) {
    try {
      const response = await this.api.get(API_CONFIG.ENDPOINTS.MODELS.LIST, {
        params: { current, size },
      });
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Get model list error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to get model list',
      };
    }
  }

  /**
   * Get active model information
   * @returns {Promise} Active model details
   */
  async getActiveModel() {
    try {
      const response = await this.api.get(API_CONFIG.ENDPOINTS.MODELS.ACTIVE);
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Get active model error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to get active model',
      };
    }
  }

  /**
   * Get model details by ID
   * @param {number} modelId - Model ID
   * @returns {Promise} Model details
   */
  async getModelById(modelId) {
    try {
      const response = await this.api.get(`${API_CONFIG.ENDPOINTS.MODELS.BASE}/${modelId}`);
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Get model by ID error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to get model details',
      };
    }
  }

  /**
   * Switch active model
   * @param {number} modelId - Model ID to activate
   * @returns {Promise} Switch result
   */
  async switchActiveModel(modelId) {
    try {
      const response = await this.api.put(`${API_CONFIG.ENDPOINTS.MODELS.BASE}/${modelId}/activate`);
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Switch active model error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Failed to switch model',
      };
    }
  }

  /**
   * Check backend health status
   * @returns {Promise} Health status
   */
  async checkHealth() {
    try {
      const response = await this.api.get(API_CONFIG.ENDPOINTS.HEALTH);
      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error('Health check error:', error);
      return {
        success: false,
        error: error.message || 'Health check failed',
      };
    }
  }

  /**
   * Set authentication token for API requests
   * @param {string} token - JWT token
   */
  setAuthToken(token) {
    if (token) {
      this.api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      delete this.api.defaults.headers.common['Authorization'];
    }
  }
}

export default new RecognitionService();
