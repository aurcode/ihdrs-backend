// TrainingTaskService.java - 训练任务服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.TrainingTaskRequest;
import com.ihdrs.backend.dto.response.TrainingTaskResponse;
import com.ihdrs.backend.entity.TrainingTask;
import com.ihdrs.backend.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingTaskService {

    private final TrainingTaskRepository taskRepository;

    /**
     * 创建训练任务
     */
    @Transactional
    public Result<TrainingTaskResponse> createTrainingTask(TrainingTaskRequest request, Long creatorId) {
        try {
            // 构建训练配置JSON
            String trainingConfig = buildTrainingConfig(request);
            String datasetConfig = buildDatasetConfig(request);

            TrainingTask task = new TrainingTask();
            task.setTaskName(request.getTaskName());
            task.setCreatorId(creatorId);
            task.setTotalEpochs(request.getTotalEpochs());
            task.setTrainingConfig(trainingConfig);
            task.setDatasetConfig(datasetConfig);
            task.setStatus(TrainingTask.TaskStatus.PENDING);
            task.setProgress(BigDecimal.ZERO);

            task = taskRepository.save(task);

            // 异步调用Python训练服务
            // TODO: 实现调用Flask训练接口

            log.info("创建训练任务成功: taskId={}, taskName={}", task.getTaskId(), task.getTaskName());
            return Result.success("训练任务创建成功", convertToTaskResponse(task));

        } catch (Exception e) {
            log.error("创建训练任务失败", e);
            return Result.error(500, "创建训练任务失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询训练任务
     */
    public Result<PageResult<TrainingTaskResponse>> getTaskList(PageRequest pageRequest, Long creatorId) {
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        Page<TrainingTask> taskPage = creatorId != null ?
                taskRepository.findByCreatorIdOrderByCreateTimeDesc(creatorId, springPageRequest) :
                taskRepository.findAll(springPageRequest);

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

    /**
     * 获取训练任务详情
     */
    public Result<TrainingTaskResponse> getTaskById(Long taskId) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }
        return Result.success(convertToTaskResponse(task));
    }

    /**
     * 取消训练任务
     */
    @Transactional
    public Result<Void> cancelTask(Long taskId) {
        TrainingTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return Result.error(404, "训练任务不存在");
        }

        if (task.getStatus() == TrainingTask.TaskStatus.COMPLETED ||
                task.getStatus() == TrainingTask.TaskStatus.FAILED) {
            return Result.error(400, "任务已完成，无法取消");
        }

        task.setStatus(TrainingTask.TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        taskRepository.save(task);

        log.info("取消训练任务: taskId={}", taskId);
        return Result.success("任务已取消", null);
    }

    private String buildTrainingConfig(TrainingTaskRequest request) {
        return String.format("""
            {
                "learning_rate": %s,
                "batch_size": %d,
                "epochs": %d,
                "optimizer": "%s",
                "loss_function": "%s",
                "model_type": "%s",
                "hidden_size": %d,
                "activation": "%s",
                "dropout": %s
            }
            """,
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
        return String.format("""
            {
                "dataset_name": "%s",
                "use_augmentation": %s,
                "validation_split": %s
            }
            """,
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