// ModelServiceClient.java - 专门的模型服务客户端
package com.ihdrs.backend.service;

import com.ihdrs.backend.config.ModelServiceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceClient {

    private final RestTemplate restTemplate;
    private final ModelServiceConfig config;

    /**
     * 调用模型识别服务
     */
    public Map<String, Object> recognize(byte[] imageData, Long modelId) {
        String url = config.getBaseUrl() + "/api/recognize";

        for (int attempt = 1; attempt <= config.getMaxRetries(); attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("image", Base64.getEncoder().encodeToString(imageData));
                requestBody.put("model_id", modelId);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    if ("success".equals(body.get("status"))) {
                        return (Map<String, Object>) body.get("data");
                    }
                }

                log.warn("模型服务返回错误状态: {}", response.getStatusCode());

            } catch (ResourceAccessException e) {
                log.warn("第 {} 次调用模型服务超时，剩余重试次数: {}",
                        attempt, config.getMaxRetries() - attempt);

                if (attempt < config.getMaxRetries()) {
                    try {
                        Thread.sleep(config.getRetryDelay().toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("调用模型服务失败 (第 {} 次尝试)", attempt, e);
                break;
            }
        }

        return null;
    }

    /**
     * 检查模型服务健康状态
     */
    public boolean checkHealth() {
        try {
            String url = config.getBaseUrl() + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("检查模型服务健康状态失败", e);
            return false;
        }
    }

    /**
     * 启动训练任务
     */
    public Map<String, Object> startTraining(Map<String, Object> trainingConfig) {
        try {
            String url = config.getBaseUrl() + "/api/train";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(trainingConfig, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("启动训练任务失败", e);
        }

        return null;
    }
}