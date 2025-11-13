// TrainingTaskService.java

package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.config.ModelServiceConfig;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.TrainingTaskRequest;
import com.ihdrs.backend.dto.response.TrainingTaskResponse;
import com.ihdrs.backend.dto.response.TrainingLogResponse;
import com.ihdrs.backend.entity.TrainingTask;
import com.ihdrs.backend.entity.TrainingLog;
import com.ihdrs.backend.entity.Model;
import com.ihdrs.backend.repository.TrainingTaskRepository;
import com.ihdrs.backend.repository.TrainingLogRepository;
import com.ihdrs.backend.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
                    // 只有失败时才更新状态
                    updateTaskStatusToFailed(task.getTaskId(), "无法启动训练服务");
                }
            } catch (Exception e) {
                log.error("提交训练任务异常", e);
                updateTaskStatusToFailed(task.getTaskId(), "训练启动失败:" + e.getMessage());
            }
        }).start();
    }

    // 使用新事务更新失败状态
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTaskStatusToFailed(Long taskId, String errorMessage) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.setStatus(TrainingTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            taskRepository.save(task);
        }
    }


    public Result<PageResult<TrainingTaskResponse>> getTaskList(PageRequest pageRequest, Long creatorId, String status) {
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        Page<TrainingTask> taskPage;
        if (creatorId != null && status != null) {
            TrainingTask.TaskStatus taskStatus = TrainingTask.TaskStatus.valueOf(status);
            taskPage = taskRepository.findByCreatorIdAndStatus(creatorId, taskStatus, springPageRequest);
        } else if (creatorId != null) {
            taskPage = taskRepository.findByCreatorIdOrderByCreateTimeDesc(creatorId, springPageRequest);
        } else if (status != null) {
            TrainingTask.TaskStatus taskStatus = TrainingTask.TaskStatus.valueOf(status);
            taskPage = taskRepository.findByStatusOrderByCreateTimeDesc(taskStatus, springPageRequest);
        } else {
            taskPage = taskRepository.findAll(springPageRequest);
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

        log.info("取消训练任务成功，taskId: {}", taskId);
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

        task.setCurrentEpoch(currentEpoch);
        task.setProgress(BigDecimal.valueOf(progress));

        if (accuracy != null) {
            BigDecimal currentAccuracy = BigDecimal.valueOf(accuracy);
            if (task.getBestAccuracy() == null || currentAccuracy.compareTo(task.getBestAccuracy()) > 0) {
                task.setBestAccuracy(currentAccuracy);
            }
        }

        // 保存训练日志
        TrainingLog log = new TrainingLog();
        log.setTaskId(taskId);
        log.setEpoch(currentEpoch);
        log.setLoss(loss != null ? BigDecimal.valueOf(loss) : null);
        log.setAccuracy(accuracy != null ? BigDecimal.valueOf(accuracy) : null);
        log.setValLoss(valLoss != null ? BigDecimal.valueOf(valLoss) : null);
        log.setValAccuracy(valAccuracy != null ? BigDecimal.valueOf(valAccuracy) : null);
        logRepository.save(log);

        taskRepository.save(task);
        return Result.success(null);
    }

    @Transactional
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

        Model model = new Model();
        model.setModelName(task.getTaskName());
        model.setModelVersion("v" + "1.0.0");
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


    private String buildTrainingConfig(TrainingTaskRequest request) {
        return String.format(
                "{\"learningrate\": \"%s\", \"batchsize\": %d, \"epochs\": %d, \"optimizer\": \"%s\", " +
                        "\"lossfunction\": \"%s\", \"modeltype\": \"%s\", \"hiddensize\": %d, \"activation\": \"%s\", \"dropout\": \"%s\"}",
                request.getLearningRate(),
                request.getBatchSize(),
                request.getTotalEpochs(),
                request.getOptimizer(),
                request.getLossFunction(),
                request.getModelType(),
                request.getHiddenSize(),
                request.getActivation(),
                request.getDropout()
        );
    }

    private String buildDatasetConfig(TrainingTaskRequest request) {
        return String.format(
                "{\"datasetname\": \"%s\", \"useaugmentation\": \"%s\", \"validationsplit\": \"%s\"}",
                request.getDatasetName(),
                request.getUseAugmentation(),
                request.getValidationSplit()
        );
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
                .build();
    }
}