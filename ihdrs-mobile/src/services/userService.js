import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * User Service
 * Handles user profile management
 */
class UserService {
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
     * Get current user profile
     * @param {string} token - JWT token
     * @returns {Promise} User profile data
     */
    async getCurrentUser(token) {
        try {
            this.setAuthToken(token);
            const response = await this.api.get('/users/me');

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    data: response.data.data,
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to fetch user profile',
                };
            }
        } catch (error) {
            console.error('Get user profile error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to fetch user profile',
            };
        }
    }

    /**
     * Update user profile
     * @param {string} token - JWT token
     * @param {object} profileData - Profile data to update
     * @returns {Promise} Update result
     */
    async updateProfile(token, profileData) {
        try {
            this.setAuthToken(token);
            const response = await this.api.put('/users/me', profileData);

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    data: response.data.data,
                    message: response.data.message || 'Profile updated successfully',
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to update profile',
                };
            }
        } catch (error) {
            console.error('Update profile error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to update profile',
            };
        }
    }

    /**
     * Change password
     * @param {string} token - JWT token
     * @param {string} oldPassword - Current password
     * @param {string} newPassword - New password
     * @returns {Promise} Change password result
     */
    async changePassword(token, oldPassword, newPassword) {
        try {
            this.setAuthToken(token);
            const response = await this.api.put('/users/me/password', {
                oldPassword,
                newPassword,
            });

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    message: response.data.message || 'Password changed successfully',
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to change password',
                };
            }
        } catch (error) {
            console.error('Change password error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to change password',
            };
        }
    }

    /**
     * Check if username exists (excluding current user)
     * @param {string} token - JWT token
     * @param {string} username - Username to check
     * @returns {Promise} Check result
     */
    async checkUsername(token, username) {
        try {
            this.setAuthToken(token);
            const response = await this.api.get('/users/check-username', {
                params: { username },
            });

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    exists: response.data.data,
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to check username',
                };
            }
        } catch (error) {
            console.error('Check username error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to check username',
            };
        }
    }
}

export default new UserService();