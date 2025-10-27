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
   * @returns {Promise} Recognition result with predicted digit and confidence
   */
  async recognizeDigit(base64Image) {
    try {
      const response = await this.api.post(API_CONFIG.ENDPOINTS.RECOGNIZE, {
        imageData: base64Image,
      });

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
}

export default new RecognitionService();
