import axios from 'axios';
import { API_CONFIG } from '../config/api';

/**
 * Recognition Service
 * 通过 Java 后端进行识别：/api/recognition/recognize
 */

class RecognitionService {
    constructor() {
        this.backendApi = axios.create({
            baseURL: API_CONFIG.BACKEND_URL,
            timeout: API_CONFIG.TIMEOUT,
            headers: {
                'Content-Type': 'application/json',
            },
        });
    }

    /**
     * 识别手写数字
     * @param {string} base64Image - Base64 编码图像（不带 data:image/png;base64, 前缀）
     * @param {string} inputType - 'CANVAS' | 'UPLOAD'
     * @param {string|null} sessionId
     * @param {object|null} clientInfo
     */
    async recognizeDigit(
        base64Image,
        inputType = 'CANVAS',
        sessionId = null,
        clientInfo = null
        , p) {
        try {
            const requestBody = {
                imageData: base64Image,
                inputType: inputType,
                sessionId: sessionId,
                clientInfo: clientInfo ? JSON.stringify(clientInfo) : null,
            };

            // RecognitionController 的路径是 /recognition/recognize
            const response = await this.backendApi.post(
                '/recognition/recognize',
                requestBody
            );

            const resData = response.data;

            // 统一 Result 包装：code / msg / data
            if (resData && resData.code === 200 && resData.data) {
                const recognitionData = resData.data;

                return {
                    success: true,
                    data: {
                        predictedDigit: recognitionData.recognitionResult,
                        confidence: Number(recognitionData.confidence), // BigDecimal -> number
                        processingTime: recognitionData.processingTime,
                        needRewrite: recognitionData.needRewrite,
                        message: recognitionData.message,
                        recordId: recognitionData.recordId,
                        probabilities: recognitionData.probabilities || [],
                        probabilitiesMap: recognitionData.probabilitiesMap || null,
                    },
                    raw: recognitionData,
                };
            } else {
                throw new Error(resData?.msg || '识别接口返回错误');
            }
        } catch (error) {
            console.error('Recognition error (via backend):', error);
            return {
                success: false,
                error:
                    error.response?.data?.msg ||
                    error.response?.data?.message ||
                    error.message ||
                    'Recognition failed',
            };
        }
    }

    async recognizeMulti(base64Image,
                         inputType = 'CANVAS',
                         sessionId = null,
                         clientInfo = null) {
        const body = {
            imageData: base64Image,
            inputType: inputType,
            sessionId: sessionId,
            clientInfo: clientInfo ? JSON.stringify(clientInfo) : null,
        };

        const response = await this.backendApi.post(
            '/recognition/recognize_multi',
            body
        );

        if (response.data.code === 200) {
            return {
                success: true,
                data: response.data.data,
            };
        }
    }


    /**
     * 设置认证 token，给 Java 后端用
     */
    setAuthToken(token) {
        if (token) {
            this.backendApi.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        } else {
            delete this.backendApi.defaults.headers.common['Authorization'];
        }
    }
}

export default new RecognitionService();