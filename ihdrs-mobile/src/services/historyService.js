import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * History Service
 * Handles recognition history management
 */
class HistoryService {
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
     * Get recognition history
     * @param {string} token - JWT token
     * @param {number} page - Page number (0-based)
     * @param {number} size - Page size
     * @param {object} filters - Filter options (result, startTime, endTime)
     * @returns {Promise} History data
     */
    async getHistory(token, page = 0, size = 10, filters = {}) {
        try {
            this.setAuthToken(token);

            const params = {
                page,
                size,
                ...filters,
            };

            const response = await this.api.get('/recognition/history_user', { params });

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    data: response.data.data,
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to fetch history',
                };
            }
        } catch (error) {
            console.error('Get history error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to fetch history',
            };
        }
    }

    /**
     * Delete a recognition record
     * @param {string} token - JWT token
     * @param {number} recordId - Record ID to delete
     * @returns {Promise} Delete result
     */
    async deleteRecord(token, recordId) {
        try {
            this.setAuthToken(token);
            const response = await this.api.delete(`/recognition/history/${recordId}`);

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    message: response.data.message || 'Record deleted successfully',
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to delete record',
                };
            }
        } catch (error) {
            console.error('Delete record error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to delete record',
            };
        }
    }

    /**
     * Batch delete recognition records
     * @param {string} token - JWT token
     * @param {Array<number>} recordIds - Array of record IDs to delete
     * @returns {Promise} Batch delete result
     */
    async batchDeleteRecords(token, recordIds) {
        try {
            this.setAuthToken(token);
            const response = await this.api.delete('/recognition/history/batch', {
                data: recordIds,
            });

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    message: response.data.message || 'Records deleted successfully',
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to delete records',
                };
            }
        } catch (error) {
            console.error('Batch delete records error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to delete records',
            };
        }
    }

    /**
     * Submit feedback for a recognition record
     * @param {string} token - JWT token
     * @param {object} feedbackData - Feedback data
     * @returns {Promise} Submit result
     */
    async submitFeedback(token, feedbackData) {
        try {
            this.setAuthToken(token);
            const response = await this.api.post('/feedback', feedbackData);

            if (response.data && response.data.code === 200) {
                return {
                    success: true,
                    message: response.data.message || 'Feedback submitted successfully',
                };
            } else {
                return {
                    success: false,
                    error: response.data?.message || 'Failed to submit feedback',
                };
            }
        } catch (error) {
            console.error('Submit feedback error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to submit feedback',
            };
        }
    }

    /**
     * Get user statistics
     * @param {string} token - JWT token
     * @returns {Promise} Statistics data
     */
    async getStatistics(token) {
        try {
            this.setAuthToken(token);
            const response = await this.api.get('/recognition/statistics');

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
            console.error('Get statistics error:', error);
            return {
                success: false,
                error: error.response?.data?.message || error.message || 'Failed to fetch statistics',
            };
        }
    }
}

export default new HistoryService();