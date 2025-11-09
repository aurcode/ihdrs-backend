import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * Recognition Service
 * Handles communication with the Python Model Service for digit recognition
 */

class RecognitionService {
  constructor() {
    // Axios instance for the Python Model Service (for recognition)
    this.modelApi = axios.create({
      baseURL: API_CONFIG.MODEL_SERVICE_URL,
      timeout: API_CONFIG.TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  /**
   * Recognize a digit from a base64 encoded image
   * This method now calls the Python Model Service directly.
   * @param {string} base64Image - Base64 encoded image string
   * @param {string} inputType - (No longer used directly in this call, but kept for signature compatibility)
   * @param {string} sessionId - (No longer used directly in this call, but kept for signature compatibility)
   * @param {object} clientInfo - (No longer used directly in this call, but kept for signature compatibility)
   * @returns {Promise} Recognition result with predicted digit and confidence
   */
  async recognizeDigit(base64Image, inputType = 'CANVAS', sessionId = null, clientInfo = null) {
    try {
      // Hardcoding model_id to 1 as per Postman example,
      // since the Java backend call was removed.
      const modelId = 1;

      // 1. Prepare the request body for the Python Model Service
      // This matches the format from your Postman example
      const requestBody = {
        model_id: modelId,
        image: base64Image,
      };

      // 2. Call the Python Model Service endpoint
      // We use 'this.modelApi' and the '/api/recognize' endpoint
      const response = await this.modelApi.post('/api/recognize', requestBody);

      // 3. Parse the response data
      if (response.data && response.data.status === 'success' && response.data.data) {
        const predictionData = response.data.data;

        // Create a more structured probabilities object (digit: probability)
        const probabilitiesMap = {};
        if (predictionData.all_probabilities && Array.isArray(predictionData.all_probabilities)) {
          predictionData.all_probabilities.forEach((probability, index) => {
            probabilitiesMap[index] = probability;
          });
        }

        // Return a structured response that matches what MainScreen.js expects
        return {
          success: true,
          data: {
            // Add the extra 'data' nesting that MainScreen.js expects
            data: {
              predictedDigit: predictionData.result,
              confidence: predictionData.confidence,
              processingTime: predictionData.processing_time,
              // Rename 'all_probabilities' to 'probabilities' as expected by MainScreen.js
              probabilities: predictionData.all_probabilities,
              probabilitiesMap: probabilitiesMap, // Still include this, it might be useful later
            },
          },
        };
      } else {
        // Handle unexpected success response format
        throw new Error(response.data?.message || 'Received an unexpected response format from the model service.');
      }
    } catch (error) {
      console.error('Recognition error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Recognition failed',
      };
    }
  }

  /**
   * Set authentication token for API requests
   * @param {string} token - JWT token
   */
  setAuthToken(token) {
    if (token) {
      this.modelApi.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      delete this.modelApi.defaults.headers.common['Authorization'];
    }
  }
}

export default new RecognitionService();