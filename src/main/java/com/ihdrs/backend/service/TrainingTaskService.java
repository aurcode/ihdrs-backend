package com.ihdrs.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.config.ModelServiceConfig;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.TrainingTaskRequest;
import com.ihdrs.backend.dto.response.TrainingTaskResponse;
import com.ihdrs.backend.dto.response.TrainingLogResponse;
import com.ihdrs.backend.entity.Dataset;
import com.ihdrs.backend.entity.TrainingTask;
import com.ihdrs.backend.entity.TrainingLog;
import com.ihdrs.backend.entity.Model;
import com.ihdrs.backend.repository.DatasetRepository;
import com.ihdrs.backend.repository.TrainingTaskRepository;
import com.ihdrs.backend.repository.TrainingLogRepository;
import com.ihdrs.backend.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingTaskService {

    private final TrainingTaskRepository taskRepository;
    private final TrainingLogRepository logRepository;
    private final ModelRepository modelRepository;
    private final RestTemplate restTemplate;
    private final ModelServiceConfig modelServiceConfig;
    private final DatasetRepository datasetRepository;
    private final ConcurrentHashMap<Long, Map<String, Object>> batchProgressCache = new ConcurrentHashMap<>();

    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 总任务数
        long totalTasks = taskRepository.count();
        statistics.put("totalTasks", totalTasks);

        // 各状态任务数量
        List<Object[]> statusCounts = taskRepository.countTasksByStatus();
        long completedTasks = 0;
        long runningTasks = 0;

        for (Object[] row : statusCounts) {
            TrainingTask.TaskStatus status = (TrainingTask.TaskStatus) row[0];
            Long count = (Long) row[1];

            if (status == TrainingTask.TaskStatus.COMPLETED) {
                completedTasks = count;
            } else if (status == TrainingTask.TaskStatus.RUNNING) {
                runningTasks = count;
            }
        }

        statistics.put("completedTasks", completedTasks);
        statistics.put("runningTasks", runningTasks);

        // 平均准确率
        Double avgAccuracy = taskRepository.getAverageAccuracy();
        statistics.put("avgAccuracy", avgAccuracy != null ? avgAccuracy : 0.0);

        return Result.success(statistics);
    }

    /**
     * 更新 batch 级别进度（存到内存缓存）
     */
    public Result<Void> updateBatchProgress(Long taskId, Map<String, Object> batchData) {
        try {
            // 验证任务是否存在
            if (!taskRepository.existsById(taskId)) {
                return Result.error(404, "训练任务不存在");
            }

            // 存到内存缓存
            batchProgressCache.put(taskId, batchData);

            log.debug("更新任务 {} 的 batch 进度: epoch={}, batch={}/{}",
                    taskId,
                    batchData.get("epoch"),
                    batchData.get("currentBatch"),
                    batchData.get("totalBatches")
            );

            return Result.success(null);
        } catch (Exception e) {
            log.error("更新 batch 进度失败", e);
            return Result.error(500, "更新进度失败");
        }
    }

    /**
     * 获取任务的最新 batch 进度
     */
    public Result<Map<String, Object>> getBatchProgress(Long taskId) {
        Map<String, Object> progress = batchProgressCache.get(taskId);

        if (progress == null) {
            // 返回空数据
            progress = new HashMap<>();
            progress.put("epoch", 0);
            progress.put("currentBatch", 0);
            progress.put("totalBatches", 0);
            progress.put("status", "waiting");
        }

        return Result.success(progress);
    }

    @Transactional
    public Result<TrainingTaskResponse> createTrainingTask(TrainingTaskRequest request, Long creatorId) {
        try {
            // 构建训练配置
            String trainingConfig = buildTrainingConfig(request);
            String datasetConfig = buildDatasetConfig(request);

            if (taskRepository.existsByTaskName(request.getTaskName())) {
                return Result.error("任务名称已存在，请使用其他名称");
            }

            // 创建任务
            TrainingTask task = new TrainingTask();
            task.setTaskName(request.getTaskName());
            task.setCreatorId(creatorId);
            task.setTotalEpochs(request.getTotalEpochs());
            task.setTrainingConfig(trainingConfig);
            task.setDatasetConfig(datasetConfig);
            task.setStatus(TrainingTask.TaskStatus.PENDING);
            task.setProgress(BigDecimal.ZERO);
            task.setCurrentEpoch(0);

            task = taskRepository.save(task);

            task.setStatus(TrainingTask.TaskStatus.RUNNING);
            task.setStartTime(LocalDateTime.now());
            task = taskRepository.saveAndFlush(task);

            // 异步调用Flask服务开始训练
            submitTrainingToFlask(task);

            return Result.success("训练任务创建成功", convertToTaskResponse(task));

        } catch (Exception e) {
            log.error("创建训练任务失败", e);
            return Result.error(500, "创建训练任务失败: " + e.getMessage());
        }
    }

    private void submitTrainingToFlask(TrainingTask task) {
        new Thread(() -> {
            try {
                String url = modelServiceConfig.getBaseUrl() + "/api/train";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = new HashMap<>();
                body.put("taskId", task.getTaskId());
                body.put("taskName", task.getTaskName());
                body.put("trainingConfig", task.getTrainingConfig());
                body.put("datasetConfig", task.getDatasetConfig());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

                if (response.getStatusCode() != HttpStatus.OK) {
                    updateTaskStatusToFailed(task.getTaskId(), "无法启动训练服务");
                }
            } catch (Exception e) {
                log.error("提交训练任务异常", e);
                updateTaskStatusToFailed(task.getTaskId(), "训练启动失败:" + e.getMessage());
            }
        }).start();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTaskStatusToFailed(Long taskId, String errorMessage) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.setStatus(TrainingTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            taskRepository.save(task);
        }
    }

    public Result<PageResult<TrainingTaskResponse>> getTaskList(PageRequest pageRequest, Long creatorId, String status, String keyword) {
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        Page<TrainingTask> taskPage;

        // 将 status 转换为枚举
        TrainingTask.TaskStatus taskStatus = null;
        if (status != null && !status.isEmpty()) {
            taskStatus = TrainingTask.TaskStatus.valueOf(status);
        }

        // 使用动态查询支持所有条件组合
        if (keyword != null && !keyword.isEmpty()) {
            if (creatorId != null && taskStatus != null) {
                taskPage = taskRepository.findByCreatorIdAndStatusAndTaskNameContaining(creatorId, taskStatus, keyword, springPageRequest);
            } else if (creatorId != null) {
                taskPage = taskRepository.findByCreatorIdAndTaskNameContaining(creatorId, keyword, springPageRequest);
            } else if (taskStatus != null) {
                taskPage = taskRepository.findByStatusAndTaskNameContaining(taskStatus, keyword, springPageRequest);
            } else {
                taskPage = taskRepository.findByTaskNameContaining(keyword, springPageRequest);
            }
        } else {
            if (creatorId != null && taskStatus != null) {
                taskPage = taskRepository.findByCreatorIdAndStatus(creatorId, taskStatus, springPageRequest);
            } else if (creatorId != null) {
                taskPage = taskRepository.findByCreatorIdOrderByCreateTimeDesc(creatorId, springPageRequest);
            } else if (taskStatus != null) {
                taskPage = taskRepository.findByStatusOrderByCreateTimeDesc(taskStatus, springPageRequest);
            } else {
                taskPage = taskRepository.findAll(springPageRequest);
            }
        }

        List<TrainingTaskResponse> taskList = taskPage.getContent().stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());

        PageResult<TrainingTaskResponse> result = PageResult.of(
                taskList,
                taskPage.getTotalElements(),
                pageRequest.getSize(),
                pageRequest.getCurrent()
        );

        return Result.success(result);
    }

    public Result<TrainingTaskResponse> getTaskById(Long taskId) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }
        return Result.success(convertToTaskResponse(task));
    }

    public Result<List<TrainingLogResponse>> getTaskLogs(Long taskId) {
        List<TrainingLog> logs = logRepository.findByTaskIdOrderByEpochAsc(taskId);

        List<TrainingLogResponse> responses = logs.stream()
                .map(log -> TrainingLogResponse.builder()
                        .logId(log.getLogId())
                        .taskId(log.getTaskId())
                        .epoch(log.getEpoch())
                        .step(log.getStep())
                        .loss(log.getLoss())
                        .accuracy(log.getAccuracy())
                        .valLoss(log.getValLoss())
                        .valAccuracy(log.getValAccuracy())
                        .learningRate(log.getLearningRate())
                        .batchSize(log.getBatchSize())
                        .timestamp(log.getTimestamp())
                        .message(log.getMessage())
                        .build())
                .collect(Collectors.toList());

        return Result.success(responses);
    }

    @Transactional
    public Result<Void> cancelTask(Long taskId) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }

        if (task.getStatus() == TrainingTask.TaskStatus.COMPLETED ||
                task.getStatus() == TrainingTask.TaskStatus.FAILED) {
            return Result.error(400, "任务已完成或已失败，无法取消");
        }

        // 通知Flask服务取消训练
        try {
            String url = modelServiceConfig.getBaseUrl() + "/api/train/cancel";
            Map<String, Object> body = new HashMap<>();
            body.put("taskId", taskId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (Exception e) {
            log.error("通知Flask取消训练失败", e);
        }

        task.setStatus(TrainingTask.TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        taskRepository.save(task);

        return Result.success("取消训练任务成功", null);
    }

    @Transactional
    public Result<Void> updateTaskProgress(Long taskId, Map<String, Object> progressData) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }

        Integer currentEpoch = (Integer) progressData.get("currentEpoch");
        Double progress = (Double) progressData.get("progress");
        Double accuracy = (Double) progressData.get("accuracy");
        Double loss = (Double) progressData.get("loss");
        Double valAccuracy = (Double) progressData.get("valAccuracy");
        Double valLoss = (Double) progressData.get("valLoss");
        Integer step = (Integer) progressData.get("step");
        Double learningRate = (Double) progressData.get("learningRate");
        Integer batchSize = (Integer) progressData.get("batchSize");

        task.setCurrentEpoch(currentEpoch);
        task.setProgress(BigDecimal.valueOf(progress));

        if (accuracy != null) {
            BigDecimal currentAccuracy = BigDecimal.valueOf(accuracy);
            if (task.getBestAccuracy() == null || currentAccuracy.compareTo(task.getBestAccuracy()) > 0) {
                task.setBestAccuracy(currentAccuracy);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Epoch ").append(currentEpoch).append("/")
                .append(task.getTotalEpochs() != null ? task.getTotalEpochs() : "?")
                .append("\n");

        // 这里没有 step/秒 的详细进度条，只做一个简化版
        List<String> metrics = new ArrayList<>();
        if (accuracy != null) {
            metrics.add(String.format("accuracy: %.4f", accuracy));
        }
        if (loss != null) {
            metrics.add(String.format("loss: %.4f", loss));
        }
        if (valAccuracy != null) {
            metrics.add(String.format("val_accuracy: %.4f", valAccuracy));
        }
        if (valLoss != null) {
            metrics.add(String.format("val_loss: %.4f", valLoss));
        }
        if (learningRate != null) {
            metrics.add(String.format("lr: %.6f", learningRate));
        }
        if (batchSize != null) {
            metrics.add("batch_size: " + batchSize);
        }

        // 形如：Epoch 1/3\nmetrics...
        if (!metrics.isEmpty()) {
            sb.append(String.join(" - ", metrics));
        }

        String message = sb.toString();

        // 保存训练日志
        TrainingLog log = new TrainingLog();
        log.setTaskId(taskId);
        log.setEpoch(currentEpoch);
        log.setLoss(loss != null ? BigDecimal.valueOf(loss) : null);
        log.setAccuracy(accuracy != null ? BigDecimal.valueOf(accuracy) : null);
        log.setValLoss(valLoss != null ? BigDecimal.valueOf(valLoss) : null);
        log.setValAccuracy(valAccuracy != null ? BigDecimal.valueOf(valAccuracy) : null);
        log.setStep(step);
        log.setLearningRate(learningRate != null ? BigDecimal.valueOf(learningRate) : null);
        log.setBatchSize(batchSize);
        log.setMessage(message);

        logRepository.save(log);
        taskRepository.save(task);
        return Result.success(null);
    }

    public Result<Void> completeTask(Long taskId, Map<String, Object> resultData) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }

        Double finalAccuracy = (Double) resultData.get("finalAccuracy");
        Double finalLoss = (Double) resultData.get("finalLoss");
        String modelPath = (String) resultData.get("modelPath");
        Integer trainingSamples = resultData.get("trainingSamples") != null
                ? ((Number) resultData.get("trainingSamples")).intValue() : null;
        Integer testSamples = resultData.get("testSamples") != null
                ? ((Number) resultData.get("testSamples")).intValue() : null;
        Long modelSize = resultData.get("modelSize") != null
                ? ((Number) resultData.get("modelSize")).longValue() : null;

        // 接收混淆矩阵和类名
        Object confusionMatrixObj = resultData.get("confusionMatrix");
        Object classNamesObj = resultData.get("classNames");

        try {
            ObjectMapper mapper = new ObjectMapper();
            if (confusionMatrixObj != null) {
                String cmJson = mapper.writeValueAsString(confusionMatrixObj);
                task.setConfusionMatrixJson(cmJson);
            }
            if (classNamesObj != null) {
                String classNamesJson = mapper.writeValueAsString(classNamesObj);
                task.setClassNamesJson(classNamesJson);
            }
        } catch (JsonProcessingException e) {
            log.error("序列化混淆矩阵或类别名称失败", e);
        }

        Model model = new Model();
        model.setModelName(task.getTaskName());
        model.setModelVersion("v1.0.0");
        model.setModelPath(modelPath);
        model.setAccuracy(finalAccuracy != null ? BigDecimal.valueOf(finalAccuracy) : null);
        model.setLoss(finalLoss != null ? BigDecimal.valueOf(finalLoss) : null);
        model.setTrainingSamples(trainingSamples);
        model.setTestSamples(testSamples);
        model.setModelSize(modelSize);
        model.setCreatorId(task.getCreatorId());
        model.setStatus(Model.ModelStatus.COMPLETED);
        modelRepository.save(model);

        task.setStatus(TrainingTask.TaskStatus.COMPLETED);
        task.setProgress(new BigDecimal("100.00"));
        task.setFinalAccuracy(model.getAccuracy());
        task.setFinalLoss(model.getLoss());
        task.setEndTime(LocalDateTime.now());
        task.setModelId(model.getModelId());
        taskRepository.save(task);

        return Result.success(null);
    }

    @Transactional
    public Result<Void> failTask(Long taskId, String errorMessage) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }

        task.setStatus(TrainingTask.TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());
        taskRepository.save(task);

        return Result.success(null);
    }

    /**
     * 构建训练配置JSON
     */
    private String buildTrainingConfig(TrainingTaskRequest request) {
        Map<String, Object> config = new HashMap<>();

        // 基础训练参数
        config.put("learningrate", request.getLearningRate().toString());
        config.put("batchsize", request.getBatchSize());
        config.put("epochs", request.getTotalEpochs());
        config.put("optimizer", request.getOptimizer());
        config.put("lossfunction", request.getLossFunction());

        // 模型配置
        config.put("modeltype", request.getModelType());
        config.put("hiddensize", request.getHiddenSize());
        config.put("activation", request.getActivation());
        config.put("dropout", request.getDropout().toString());
        config.put("useBatchNorm", request.getUseBatchNorm());

        // 高级配置
        config.put("l2Regularization", request.getL2Regularization().toString());
        config.put("earlyStoppingPatience", request.getEarlyStoppingPatience());
        config.put("lrScheduler", request.getLrScheduler());

        // 数据增强
        config.put("useAugmentation", request.getUseAugmentation());
        if (request.getUseAugmentation()) {
            config.put("augmentationStrength", request.getAugmentationStrength());
        }

        config.put("validationSplit", request.getValidationSplit().toString());

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("构建训练配置失败", e);
            throw new RuntimeException("构建训练配置失败", e);
        }
    }

    private String buildDatasetConfig(TrainingTaskRequest request) throws JsonProcessingException {
        Dataset dataset = datasetRepository.findById(request.getDatasetId())
                .orElseThrow(() -> new RuntimeException("数据集不存在"));

        Map<String, Object> config = new HashMap<>();
        config.put("dataset_id", dataset.getDatasetId());
        config.put("dataset_name", dataset.getDatasetName());
        config.put("file_path", dataset.getFilePath());
        config.put("dataset_type", dataset.getDatasetType());
        config.put("num_classes", dataset.getNumClasses());
        config.put("num_samples", dataset.getNumSamples());
        config.put("train_samples", dataset.getTrainSamples());
        config.put("test_samples", dataset.getTestSamples());
        config.put("image_width", dataset.getImageWidth());
        config.put("image_height", dataset.getImageHeight());
        config.put("class_names", dataset.getClassNames());

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(config);
    }

    private TrainingTaskResponse convertToTaskResponse(TrainingTask task) {
        return TrainingTaskResponse.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .status(task.getStatus().name())
                .progress(task.getProgress())
                .currentEpoch(task.getCurrentEpoch())
                .totalEpochs(task.getTotalEpochs())
                .bestAccuracy(task.getBestAccuracy())
                .finalAccuracy(task.getFinalAccuracy())
                .finalLoss(task.getFinalLoss())
                .errorMessage(task.getErrorMessage())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .estimatedTime(task.getEstimatedTime())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .modelId(task.getModelId())
                .confusionMatrixJson(task.getConfusionMatrixJson())
                .classNamesJson(task.getClassNamesJson())
                .trainingConfig(task.getTrainingConfig())
                .datasetConfig(task.getDatasetConfig())
                .build();
    }
}