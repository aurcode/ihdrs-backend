import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * Feedback Service
 * Handles user feedback management
 */
class FeedbackService {
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
     * Get user's feedback list
     * @param {string} token - JWT token
     * @param {number} current - Current page (1-based)
     * @param {number} size - Page size
     * @param {string} status - Filter by status (optional)
     * @param {string} feedbackType - Filter by type (optional)
     * @returns {Promise} Feedback list data
     */
    async getUserFeedbackList(token, current = 1, size = 10, status = null, feedbackType = null) {
        try {
            this.setAuthToken(token);

            const params = {
                current,
                size,
            };

            if (status) {
                params.status = status;
            }

            if (feedbackType) {
                params.feedbackType = feedbackType;
            }

            const response = await this.api.get('/feedback/my-feedback', { params });

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    data: response.data.data,
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to fetch feedback list',
                };
            }
        } catch (error) {
            console.error('Get feedback list error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to fetch feedback list',
            };
        }
    }

    /**
     * Get feedback detail by ID
     * @param {string} token - JWT token
     * @param {number} feedbackId - Feedback ID
     * @returns {Promise} Feedback detail data
     */
    async getFeedbackById(token, feedbackId) {
        try {
            this.setAuthToken(token);
            const response = await this.api.get(`/feedback/${feedbackId}`);

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    data: response.data.data,
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to fetch feedback detail',
                };
            }
        } catch (error) {
            console.error('Get feedback detail error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to fetch feedback detail',
            };
        }
    }

    /**
     * Delete a feedback
     * @param {string} token - JWT token
     * @param {number} feedbackId - Feedback ID to delete
     * @returns {Promise} Delete result
     */
    async deleteFeedback(token, feedbackId) {
        try {
            this.setAuthToken(token);
            const response = await this.api.delete(`/feedback/${feedbackId}`);

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    message: response.data.message || 'Feedback deleted successfully',
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to delete feedback',
                };
            }
        } catch (error) {
            console.error('Delete feedback error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to delete feedback',
            };
        }
    }

    /**
     * Get feedback statistics
     * @param {string} token - JWT token
     * @returns {Promise} Statistics data
     */
    async getFeedbackStatistics(token) {
        try {
            this.setAuthToken(token);
            const response = await this.api.get('/feedback/statistics');

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    data: response.data.data,
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to fetch statistics',
                };
            }
        } catch (error) {
            console.error('Get feedback statistics error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to fetch statistics',
            };
        }
    }
}

export default new FeedbackService();