// TrainingTaskController.java
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.TrainingTaskRequest;
import com.ihdrs.backend.dto.response.TrainingTaskResponse;
import com.ihdrs.backend.dto.response.TrainingLogResponse;
import com.ihdrs.backend.service.TrainingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "训练管理", description = "训练任务管理接口")
@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@Validated
public class TrainingTaskController {

    private final TrainingTaskService trainingTaskService;

    @Operation(summary = "更新批次进度", description = "由Flask服务调用，更新batch级别实时进度")
    @PostMapping("/tasks/{taskId}/batch-progress")
    public Result<Void> updateBatchProgress(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> batchData
    ) {
        return trainingTaskService.updateBatchProgress(taskId, batchData);
    }

    @Operation(summary = "获取批次进度", description = "获取任务的实时batch进度")
    @GetMapping("/tasks/{taskId}/batch-progress")
    public Result<Map<String, Object>> getBatchProgress(@PathVariable Long taskId) {
        return trainingTaskService.getBatchProgress(taskId);
    }

    @Operation(summary = "创建训练任务", description = "创建新的模型训练任务")
    @PostMapping("/tasks")
    public Result<TrainingTaskResponse> createTask(
            @Valid @RequestBody TrainingTaskRequest request,
            @RequestAttribute("userId") Long userId
    ) {
        return trainingTaskService.createTrainingTask(request, userId);
    }

    @Operation(summary = "获取训练任务列表", description = "分页获取训练任务列表")
    @GetMapping("/tasks")
    public Result<PageResult<TrainingTaskResponse>> getTaskList(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword  // 添加关键词参数
    ) {
        return trainingTaskService.getTaskList(pageRequest, creatorId, status, keyword);
    }

    @Operation(summary = "获取任务详情", description = "根据ID获取训练任务详情")
    @GetMapping("/tasks/{taskId}")
    public Result<TrainingTaskResponse> getTaskById(@PathVariable Long taskId) {
        return trainingTaskService.getTaskById(taskId);
    }

    @Operation(summary = "获取训练日志", description = "获取训练过程的详细日志")
    @GetMapping("/tasks/{taskId}/logs")
    public Result<List<TrainingLogResponse>> getTaskLogs(@PathVariable Long taskId) {
        return trainingTaskService.getTaskLogs(taskId);
    }

    @Operation(summary = "取消训练任务", description = "取消正在进行的训练任务")
    @PutMapping("/tasks/{taskId}/cancel")
    public Result<Void> cancelTask(@PathVariable Long taskId) {
        return trainingTaskService.cancelTask(taskId);
    }

    @Operation(summary = "更新任务进度", description = "由Flask服务调用，更新训练进度")
    @PostMapping("/tasks/{taskId}/progress")
    public Result<Void> updateProgress(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> progressData
    ) {
        return trainingTaskService.updateTaskProgress(taskId, progressData);
    }

    @Operation(summary = "完成训练任务", description = "由Flask服务调用，标记任务完成")
    @PostMapping("/tasks/{taskId}/complete")
    public Result<Void> completeTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> resultData
    ) {
        return trainingTaskService.completeTask(taskId, resultData);
    }

    @Operation(summary = "标记任务失败", description = "由Flask调用，标记训练任务失败")
    @PostMapping("/tasks/{taskId}/fail")
    public Result<Void> failTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> data
    ) {
        String errorMessage = (String) data.get("errorMessage");
        return trainingTaskService.failTask(taskId, errorMessage);
    }

    @Operation(summary = "获取训练统计信息", description = "获取训练任务的统计数据")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return trainingTaskService.getStatistics();
    }

}