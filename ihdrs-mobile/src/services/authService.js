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
   * @param {string} username - User username
   * @param {string} password - User password
   * @returns {Promise} Login result with token and user data
   */
  async login(username, password) {
    try {
      const response = await this.api.post('/auth/login', {
        username,
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
   * Register new user
   * @param {string} username - User username (3-50 characters, alphanumeric and underscore)
   * @param {string} password - User password (6-20 characters)
   * @param {string} email - User email (optional)
   * @param {string} phone - User phone number (optional, Chinese format)
   * @returns {Promise} Registration result with user data
   */
  async register(username, password, email = null, phone = null) {
    try {
      const requestData = {
        username,
        password,
      };

      // Add optional fields if provided
      if (email) {
        requestData.email = email;
      }
      if (phone) {
        requestData.phone = phone;
      }

      const response = await this.api.post('/auth/register', requestData);

      if (response.data && response.data.code === 200) {
        return {
          success: true,
          data: response.data.data,
          message: response.data.message || 'Registration successful',
        };
      } else {
        return {
          success: false,
          error: response.data?.message || 'Registration failed',
        };
      }
    } catch (error) {
      console.error('Registration error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Registration failed',
      };
    }
  }

  /**
   * Validate token
   * @param {string} token - JWT token to validate
   * @returns {Promise} Validation result with user data
   */
  async validateToken(token) {
    try {
      const response = await this.api.get('/auth/validate', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.data && response.data.code === 200) {
        return {
          success: true,
          data: response.data.data,
        };
      } else {
        return {
          success: false,
          error: response.data?.message || 'Token validation failed',
        };
      }
    } catch (error) {
      console.error('Token validation error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Token validation failed',
      };
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
