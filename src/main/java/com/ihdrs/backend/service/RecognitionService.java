// RecognitionService.java - 识别服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.common.constants.Constants;
import com.ihdrs.backend.common.utils.FileUtil;
import com.ihdrs.backend.common.utils.ImageUtil;
import com.ihdrs.backend.config.ModelServiceConfig;
import com.ihdrs.backend.dto.request.RecognitionRequest;
import com.ihdrs.backend.dto.response.RecognitionResponse;
import com.ihdrs.backend.entity.Model;
import com.ihdrs.backend.entity.RecognitionRecord;
import com.ihdrs.backend.repository.ModelRepository;
import com.ihdrs.backend.repository.RecognitionRecordRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecognitionService {

    private final ModelRepository modelRepository;
    private final RecognitionRecordRepository recordRepository;
    private final ModelServiceConfig modelServiceConfig;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FileUtil fileUtil;
    private final ImageUtil imageUtil;

    /**
     * 执行数字识别
     */
    @Transactional
    public Result<RecognitionResponse> recognize(RecognitionRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取当前活跃模型
            Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE)
                    .orElse(null);

            if (activeModel == null) {
                return Result.error(500, "没有可用的识别模型");
            }

            // 2. 解码Base64图像数据
            byte[] imageData = Base64.getDecoder().decode(request.getImageData());

            // 3. 计算图像哈希（用于缓存）
            String imageHash = fileUtil.calculateFileHash(imageData);

            // 4. 检查Redis缓存
            String cacheKey = Constants.REDIS_KEY_RECOGNITION_RESULT + imageHash;
            RecognitionResponse cachedResult = (RecognitionResponse) redisTemplate.opsForValue().get(cacheKey);

            if (cachedResult != null) {
                log.info("从缓存获取识别结果: {}", imageHash);

                // 保存识别记录（即使是缓存结果）
                saveRecognitionRecord(userId, activeModel.getModelId(), cachedResult,
                        imageData, imageHash, request, (int)(System.currentTimeMillis() - startTime));

                return Result.success(cachedResult);
            }

            // 5. 调用Flask模型服务进行识别
            Map<String, Object> recognitionResult = callModelService(imageData, activeModel);

            if (recognitionResult == null) {
                return Result.error(500, "模型服务调用失败");
            }

            // 6. 构建识别响应
            Integer result = (Integer) recognitionResult.get("result");
            Double confidenceValue = (Double) recognitionResult.get("confidence");
            BigDecimal confidence = BigDecimal.valueOf(confidenceValue);

            int processingTime = (int) (System.currentTimeMillis() - startTime);

            // 判断是否需要重写（置信度低）
            boolean needRewrite = confidence.compareTo(
                    BigDecimal.valueOf(Constants.MIN_CONFIDENCE_THRESHOLD)) < 0;

            String message = needRewrite ?
                    "识别置信度较低，建议重新书写更清晰的数字" : "识别成功";

            RecognitionResponse response = RecognitionResponse.builder()
                    .recognitionResult(result)
                    .confidence(confidence)
                    .processingTime(processingTime)
                    .message(message)
                    .needRewrite(needRewrite)
                    .build();

            // 7. 保存识别记录
            RecognitionRecord record = saveRecognitionRecord(userId, activeModel.getModelId(),
                    response, imageData, imageHash, request, processingTime);

            response.setRecordId(record.getRecordId());

            // 8. 缓存识别结果（1天）
            redisTemplate.opsForValue().set(cacheKey, response,
                    Constants.CACHE_EXPIRE_RECOGNITION, TimeUnit.SECONDS);

            log.info("识别完成 - 结果: {}, 置信度: {}, 耗时: {}ms",
                    result, confidence, processingTime);

            return Result.success(response);

        } catch (Exception e) {
            log.error("识别失败", e);
            return Result.error(500, "识别服务异常: " + e.getMessage());
        }
    }

    /**
     * 调用Flask模型服务
     */
    private Map<String, Object> callModelService(byte[] imageData, Model model) {
        try {
            String url = modelServiceConfig.getBaseUrl() + "/api/recognize";

            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", Base64.getEncoder().encodeToString(imageData));
            requestBody.put("model_id", model.getModelId());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if ("success".equals(body.get("status"))) {
                    return (Map<String, Object>) body.get("data");
                }
            }

            return null;
        } catch (Exception e) {
            log.error("调用模型服务失败", e);
            return null;
        }
    }

    /**
     * 保存识别记录
     */
    private RecognitionRecord saveRecognitionRecord(Long userId, Long modelId,
                                                    RecognitionResponse response, byte[] imageData, String imageHash,
                                                    RecognitionRequest request, int processingTime) {

        RecognitionRecord record = new RecognitionRecord();
        record.setUserId(userId);
        record.setModelId(modelId);
        record.setRecognitionResult(response.getRecognitionResult());
        record.setConfidence(response.getConfidence());
        record.setImageData(imageData);
        record.setImageHash(imageHash);
        record.setInputType(RecognitionRecord.InputType.valueOf(request.getInputType()));
        record.setProcessingTime(processingTime);
        record.setSessionId(request.getSessionId());
        record.setClientInfo(request.getClientInfo());

        return recordRepository.save(record);
    }

    /**
     * 获得识别记录
     */
    @Transactional(readOnly = true)
    public Result<?> getAllHistory(int page, int size,
                                   Integer result,
                                   Long userId, LocalDateTime startTime, LocalDateTime endTime) {

        Pageable pageable = PageRequest.of(page, size);

        Page<RecognitionRecord> recordPage =
                recordRepository.findAllWithFiltersAndUser(result, userId, startTime, endTime, pageable);

        List<RecognitionResponse> records = recordPage.getContent().stream()
                .map(record -> {
                    RecognitionResponse.RecognitionResponseBuilder builder = RecognitionResponse.builder()
                            .recordId(record.getRecordId())
                            .recognitionResult(record.getRecognitionResult())
                            .confidence(record.getConfidence())
                            .processingTime(record.getProcessingTime())
                            .message("历史记录")
                            .needRewrite(false)
                            .createTime(record.getCreateTime())
                            .imagePath(record.getImagePath())
                            .inputType(record.getInputType() != null ? record.getInputType().name() : null)
                            .isCorrect(record.getIsCorrect())
                            .userId(record.getUserId())
                            .modelId(record.getModelId());

                    // 新增:查询模型信息
                    if (record.getModelId() != null) {
                        modelRepository.findById(record.getModelId())
                                .ifPresent(model -> {
                                    builder.modelName(model.getModelName());
                                    builder.modelVersion(model.getModelVersion());
                                });
                    }

                    return builder.build();
                })
                .toList();

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("total", recordPage.getTotalElements());
        resultData.put("pages", recordPage.getTotalPages());
        resultData.put("records", records);

        return Result.success(resultData);
    }

    @Transactional
    public Result<Void> deleteRecord(Long recordId, Long requestUserId) {
        try {
            RecognitionRecord record = recordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return Result.error(404, "识别记录不存在");
            }

            // 如果是记录所有者，允许删除，管理员需要另行判断
            Long ownerId = record.getUserId();
            if (ownerId == null) {
                // 匿名记录，仅管理员允许删除
                return Result.error(403, "无法删除匿名记录（仅管理员可操作）");
            }

            // 如果不是同一用户，需要判断是否是管理员
            // TODO
            recordRepository.deleteById(recordId);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除识别记录失败", e);
            return Result.error(500, "删除识别记录失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除识别记录
     */
    @Transactional
    public Result<Void> batchDeleteRecords(List<Long> recordIds, Long userId) {
        try {
            if (recordIds == null || recordIds.isEmpty()) {
                return Result.error(400, "记录ID列表不能为空");
            }

            recordRepository.deleteAllById(recordIds);

            log.info("批量删除识别记录成功 - 用户ID: {}, 删除数量: {}", userId, recordIds.size());
            return Result.success(null);
        } catch (Exception e) {
            log.error("批量删除识别记录失败", e);
            return Result.error(500, "批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取识别统计信息
     */
    public Result<Object> getStatistics(Long userId) {
        try {
            Long total = recordRepository.countByUserId(userId);
            Long correct = recordRepository.countCorrectByUserId(userId);
            Double avgTime = recordRepository.avgProcessingTimeByUserId(userId);
            Long today = recordRepository.countByUserIdAndCreateTimeAfter(
                    userId, LocalDateTime.now().toLocalDate().atStartOfDay());

            double accuracy = total > 0 ? (correct.doubleValue() / total.doubleValue() * 100) : 0;

            var statistics = new java.util.HashMap<String, Object>();
            statistics.put("total", total);
            statistics.put("accuracy", String.format("%.1f", accuracy));
            statistics.put("avgTime", avgTime != null ? avgTime.intValue() : 0);
            statistics.put("today", today);

            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return Result.error(500, "获取统计信息失败");
        }
    }
}