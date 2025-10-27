import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * Authentication Service
 * Handles user login and authentication
 */

class AuthService {
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
   * Login user
   * @param {string} email - User email
   * @param {string} password - User password
   * @returns {Promise} Login result with token and user data
   */
  async login(email, password) {
    try {
      const response = await this.api.post('/auth/login', {
        email,
        password,
      });

      if (response.data && response.data.code === 200) {
        // Store token for future requests
        const token = response.data.data.token;
        this.setAuthToken(token);
        
        return {
          success: true,
          data: response.data.data,
        };
      } else {
        return {
          success: false,
          error: response.data?.message || 'Login failed',
        };
      }
    } catch (error) {
      console.error('Login error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Login failed',
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

  /**
   * Logout user
   */
  logout() {
    this.setAuthToken(null);
  }
}

export default new AuthService();
