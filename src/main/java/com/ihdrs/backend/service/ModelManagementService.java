// ModelManagementService.java - 模型管理服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.UpdateModelRequest;
import com.ihdrs.backend.dto.response.*;
import com.ihdrs.backend.entity.Model;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.repository.ModelRepository;
import com.ihdrs.backend.repository.RecognitionRecordRepository;
import com.ihdrs.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.ihdrs.backend.config.ModelServiceConfig;

import jakarta.persistence.criteria.Predicate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManagementService {

    private final ModelRepository modelRepository;
    private final RecognitionRecordRepository recognitionRecordRepository;
    private final UserRepository userRepository;
    private final ModelServiceConfig modelServiceConfig;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 分页查询模型列表（支持过滤）
     */
    public Result<PageResult<ModelResponse>> getModelList(
            PageRequest pageRequest, String status, String modelType, String keyword) {

        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        // 构建查询条件
        Specification<Model> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleted"), false));

            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), Model.ModelStatus.valueOf(status)));
            }

            if (StringUtils.hasText(modelType)) {
                predicates.add(cb.equal(root.get("modelType"), modelType));
            }

            if (StringUtils.hasText(keyword)) {
                Predicate namePredicate = cb.like(root.get("modelName"), "%" + keyword + "%");
                Predicate versionPredicate = cb.like(root.get("modelVersion"), "%" + keyword + "%");
                Predicate descPredicate = cb.like(root.get("description"), "%" + keyword + "%");
                predicates.add(cb.or(namePredicate, versionPredicate, descPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Model> modelPage = modelRepository.findAll(spec, springPageRequest);

        // 获取当前活跃模型ID
        Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE).orElse(null);
        Long activeModelId = activeModel != null ? activeModel.getModelId() : null;

        List<ModelResponse> modelList = modelPage.getContent().stream()
                .map(model -> convertToModelResponse(model, activeModelId))
                .collect(Collectors.toList());

        PageResult<ModelResponse> result = PageResult.of(
                modelList,
                modelPage.getTotalElements(),
                pageRequest.getSize(),
                pageRequest.getCurrent()
        );

        return Result.success(result);
    }

    /**
     * 获取当前活跃模型
     */
    public Result<ModelResponse> getActiveModel() {
        Model model = modelRepository.findByStatus(Model.ModelStatus.ACTIVE).orElse(null);

        if (model == null) {
            return Result.error(404, "没有活跃的模型");
        }

        return Result.success(convertToModelResponse(model, model.getModelId()));
    }

    /**
     * 根据ID获取模型信息
     */
    public Result<ModelResponse> getModelById(Long modelId) {
        Model model = modelRepository.findById(modelId).orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE).orElse(null);
        Long activeModelId = activeModel != null ? activeModel.getModelId() : null;

        return Result.success(convertToModelResponse(model, activeModelId));
    }

    /**
     * 切换活跃模型
     */
    @Transactional
    public Result<Void> switchActiveModel(Long modelId, Long userId) {
        Model model = modelRepository.findById(modelId).orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        if (model.getStatus() == Model.ModelStatus.DISABLED) {
            return Result.error(400, "已停用的模型不能设置为活跃状态");
        }

        if (model.getStatus() != Model.ModelStatus.COMPLETED &&
                model.getStatus() != Model.ModelStatus.ACTIVE) {
            return Result.error(400, "只有已完成的模型才能设置为活跃状态");
        }

        // 将所有模型设置为COMPLETED状态
        modelRepository.deactivateAllModels();

        // 设置当前模型为ACTIVE
        model.setStatus(Model.ModelStatus.ACTIVE);
        modelRepository.save(model);

        try {
            String url = modelServiceConfig.getBaseUrl() + "/api/models/activate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model_id", modelId);
            body.put("model_path", model.getModelPath());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            log.info("Flask 模型切换成功: modelId={}, path={}", modelId, model.getModelPath());

        } catch (Exception e) {
            log.error("通知Flask切换模型失败: {}", e.getMessage());
            return Result.error(500, "数据库更新成功，但模型服务未同步，请检查模型服务是否在线");
        }

        log.info("用户{}激活模型成功: {}", userId, model.getModelName());
        return Result.success("模型切换成功", null);
    }

    /**
     * 停用模型
     */
    @Transactional
    public Result<Void> disableModel(Long modelId, Long userId) {
        Model model = modelRepository.findById(modelId).orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        if (model.getStatus() == Model.ModelStatus.ACTIVE) {
            return Result.error(400, "当前活跃模型不能被停用，请先切换到其他模型");
        }

        model.setStatus(Model.ModelStatus.DISABLED);
        modelRepository.save(model);

        log.info("用户 {} 停用模型: modelId={}", userId, modelId);
        return Result.success("模型已停用", null);
    }

    /**
     * 启用模型
     */
    @Transactional
    public Result<Void> enableModel(Long modelId, Long userId) {
        Model model = modelRepository.findById(modelId).orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        if (model.getStatus() != Model.ModelStatus.DISABLED) {
            return Result.error(400, "只有已停用的模型才能被启用");
        }

        model.setStatus(Model.ModelStatus.COMPLETED);
        modelRepository.save(model);

        log.info("用户 {} 启用模型: modelId={}", userId, modelId);
        return Result.success("模型已启用", null);
    }

    /**
     * 删除模型（逻辑删除或物理删除）
     */
    @Transactional
    public Result<Void> deleteModel(Long modelId, Long userId) {
        Model model = modelRepository.findById(modelId).orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        // 检查模型是否已被删除
        if (Boolean.TRUE.equals(model.getDeleted())) {
            return Result.error(400, "模型已被删除");
        }

        if (model.getStatus() == Model.ModelStatus.ACTIVE) {
            return Result.error(400, "当前活跃模型不能被删除");
        }

        // 执行逻辑删除：设置 deleted = true
        model.setDeleted(true);
        // 同时将状态改为 DISABLED
        model.setStatus(Model.ModelStatus.DISABLED);
        modelRepository.save(model);

        log.info("用户 {} 逻辑删除模型: modelId={}", userId, modelId);

        return Result.success("模型已删除", null);
    }

    /**
     * 更新模型信息
     */
    @Transactional
    public Result<ModelResponse> updateModel(Long modelId, UpdateModelRequest request, Long userId) {
        Model model = modelRepository.findById(modelId).orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        // 更新模型信息
        if (StringUtils.hasText(request.getModelName())) {
            model.setModelName(request.getModelName());
        }
        if (StringUtils.hasText(request.getModelVersion())) {
            model.setModelVersion(request.getModelVersion());
        }
        if (StringUtils.hasText(request.getDescription())) {
            model.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getModelType())) {
            model.setModelType(request.getModelType());
        }

        model = modelRepository.save(model);

        log.info("用户 {} 更新模型信息: modelId={}", userId, modelId);

        Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE).orElse(null);
        Long activeModelId = activeModel != null ? activeModel.getModelId() : null;

        return Result.success("更新成功", convertToModelResponse(model, activeModelId));
    }

    /**
     * 获取模型版本列表
     */
    public Result<List<ModelVersionResponse>> getModelVersions(String modelName) {
        List<Model> models = modelRepository.findByModelNameOrderByCreateTimeDesc(modelName);

        if (models.isEmpty()) {
            return Result.error(404, "未找到相关模型版本");
        }

        Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE).orElse(null);
        Long activeModelId = activeModel != null ? activeModel.getModelId() : null;

        List<ModelVersionResponse> versions = new ArrayList<>();
        BigDecimal previousAccuracy = null;

        for (Model model : models) {
            ModelVersionResponse version = ModelVersionResponse.builder()
                    .modelId(model.getModelId())
                    .modelName(model.getModelName())
                    .modelVersion(model.getModelVersion())
                    .modelType(model.getModelType())
                    .accuracy(model.getAccuracy())
                    .loss(model.getLoss())
                    .trainingSamples(model.getTrainingSamples())
                    .status(model.getStatus().name())
                    .createTime(model.getCreateTime())
                    .isActive(model.getModelId().equals(activeModelId))
                    .build();

            // 计算与上一版本的准确率提升
            if (previousAccuracy != null && model.getAccuracy() != null) {
                version.setAccuracyImprovement(
                        model.getAccuracy().subtract(previousAccuracy)
                                .multiply(new BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP)
                );
            }

            previousAccuracy = model.getAccuracy();
            versions.add(version);
        }

        return Result.success(versions);
    }

    /**
     * 版本对比
     */
    public Result<Object> compareModels(Long modelId1, Long modelId2) {
        Model model1 = modelRepository.findById(modelId1).orElse(null);
        Model model2 = modelRepository.findById(modelId2).orElse(null);

        if (model1 == null || model2 == null) {
            return Result.error(404, "模型不存在");
        }

        ModelComparisonResponse.ModelBasicInfo info1 = ModelComparisonResponse.ModelBasicInfo.builder()
                .modelId(model1.getModelId())
                .modelName(model1.getModelName())
                .modelVersion(model1.getModelVersion())
                .modelType(model1.getModelType())
                .accuracy(model1.getAccuracy())
                .loss(model1.getLoss())
                .trainingSamples(model1.getTrainingSamples())
                .modelSize(model1.getModelSize())
                .build();

        ModelComparisonResponse.ModelBasicInfo info2 = ModelComparisonResponse.ModelBasicInfo.builder()
                .modelId(model2.getModelId())
                .modelName(model2.getModelName())
                .modelVersion(model2.getModelVersion())
                .modelType(model2.getModelType())
                .accuracy(model2.getAccuracy())
                .loss(model2.getLoss())
                .trainingSamples(model2.getTrainingSamples())
                .modelSize(model2.getModelSize())
                .build();

        // 计算差异
        BigDecimal accuracyDiff = model2.getAccuracy() != null && model1.getAccuracy() != null
                ? model2.getAccuracy().subtract(model1.getAccuracy()) : null;
        BigDecimal lossDiff = model2.getLoss() != null && model1.getLoss() != null
                ? model2.getLoss().subtract(model1.getLoss()) : null;
        Integer samplesDiff = model2.getTrainingSamples() != null && model1.getTrainingSamples() != null
                ? model2.getTrainingSamples() - model1.getTrainingSamples() : null;
        Long sizeDiff = model2.getModelSize() != null && model1.getModelSize() != null
                ? model2.getModelSize() - model1.getModelSize() : null;

        // 推荐
        String recommendation = "无法判断";
        if (accuracyDiff != null) {
            if (accuracyDiff.compareTo(BigDecimal.ZERO) > 0) {
                recommendation = "推荐使用模型2：准确率更高";
            } else if (accuracyDiff.compareTo(BigDecimal.ZERO) < 0) {
                recommendation = "推荐使用模型1：准确率更高";
            } else {
                if (sizeDiff != null && sizeDiff < 0) {
                    recommendation = "推荐使用模型2：准确率相同但体积更小";
                } else if (sizeDiff != null && sizeDiff > 0) {
                    recommendation = "推荐使用模型1：准确率相同但体积更小";
                }
            }
        }

        ModelComparisonResponse.ComparisonMetrics metrics =
                ModelComparisonResponse.ComparisonMetrics.builder()
                        .accuracyDiff(accuracyDiff)
                        .lossDiff(lossDiff)
                        .samplesDiff(samplesDiff)
                        .sizeDiff(sizeDiff)
                        .recommendation(recommendation)
                        .build();

        ModelComparisonResponse response = ModelComparisonResponse.builder()
                .model1(info1)
                .model2(info2)
                .comparison(metrics)
                .build();

        return Result.success(response);
    }

    /**
     * 获取模型统计信息
     */
    public Result<Object> getModelStatistics() {
        Long totalModels = modelRepository.count();
        Long activeModels = modelRepository.countByStatus(Model.ModelStatus.ACTIVE);
        Long completedModels = modelRepository.countByStatus(Model.ModelStatus.COMPLETED);
        Long trainingModels = modelRepository.countByStatus(Model.ModelStatus.TRAINING);

        // 平均准确率
        List<Model> allModels = modelRepository.findAll();
        BigDecimal avgAccuracy = allModels.stream()
                .filter(m -> m.getAccuracy() != null)
                .map(Model::getAccuracy)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(allModels.size()), 4, RoundingMode.HALF_UP);

        // 最高准确率
        BigDecimal bestAccuracy = allModels.stream()
                .filter(m -> m.getAccuracy() != null)
                .map(Model::getAccuracy)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 总识别次数
        Long totalRecognitions = recognitionRecordRepository.count();

        // 模型类型分布
        Map<String, Long> modelTypeDistribution = allModels.stream()
                .collect(Collectors.groupingBy(Model::getModelType, Collectors.counting()));

        // Top模型（按使用次数）
        List<ModelStatisticsResponse.TopModel> topModels = allModels.stream()
                .sorted((m1, m2) -> {
                    Long count1 = recognitionRecordRepository.countByModelId(m1.getModelId());
                    Long count2 = recognitionRecordRepository.countByModelId(m2.getModelId());
                    return count2.compareTo(count1);
                })
                .limit(5)
                .map(model -> ModelStatisticsResponse.TopModel.builder()
                        .modelId(model.getModelId())
                        .modelName(model.getModelName())
                        .modelVersion(model.getModelVersion())
                        .accuracy(model.getAccuracy())
                        .usageCount(recognitionRecordRepository.countByModelId(model.getModelId()))
                        .build())
                .collect(Collectors.toList());

        ModelStatisticsResponse statistics = ModelStatisticsResponse.builder()
                .totalModels(totalModels)
                .activeModels(activeModels)
                .completedModels(completedModels)
                .trainingModels(trainingModels)
                .avgAccuracy(avgAccuracy)
                .bestAccuracy(bestAccuracy)
                .totalRecognitions(totalRecognitions)
                .modelTypeDistribution(modelTypeDistribution)
                .topModels(topModels)
                .build();

        return Result.success(statistics);
    }

    /**
     * 批量删除模型
     */
    @Transactional
    public Result<Void> batchDeleteModels(List<Long> modelIds, Long userId) {
        for (Long modelId : modelIds) {
            deleteModel(modelId, userId);
        }
        return Result.success("批量删除成功", null);
    }

    /**
     * 转换为模型响应对象
     */
    private ModelResponse convertToModelResponse(Model model, Long activeModelId) {
        // 获取创建者用户名
        String creatorName = null;
        if (model.getCreatorId() != null) {
            User creator = userRepository.findById(model.getCreatorId()).orElse(null);
            if (creator != null) {
                creatorName = creator.getUsername();
            }
        }

        return ModelResponse.builder()
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .modelVersion(model.getModelVersion())
                .modelType(model.getModelType())
                .accuracy(model.getAccuracy())
                .loss(model.getLoss())
                .trainingSamples(model.getTrainingSamples())
                .testSamples(model.getTestSamples())
                .modelSize(model.getModelSize())
                .status(model.getStatus().name())
                .description(model.getDescription())
                .creatorName(creatorName)
                .createTime(model.getCreateTime())
                .updateTime(model.getUpdateTime())
                .isActive(model.getModelId().equals(activeModelId))
                .build();
    }
}