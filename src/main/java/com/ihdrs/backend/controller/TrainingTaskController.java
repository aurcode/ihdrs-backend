// TrainingTaskController.java - 训练任务控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.TrainingTaskRequest;
import com.ihdrs.backend.dto.response.TrainingTaskResponse;
import com.ihdrs.backend.service.TrainingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "训练任务管理", description = "深度学习模型训练任务管理接口")
@RestController
@RequestMapping("/training")
@RequiredArgsConstructor
@Validated
public class TrainingTaskController {

    private final TrainingTaskService trainingTaskService;

    @Operation(summary = "创建训练任务", description = "创建新的模型训练任务")
    @PostMapping("/tasks")
    public Result<TrainingTaskResponse> createTask(
            @Valid @RequestBody TrainingTaskRequest request,
            @RequestAttribute("userId") Long userId) {
        return trainingTaskService.createTrainingTask(request, userId);
    }

    @Operation(summary = "获取训练任务列表", description = "分页查询训练任务")
    @GetMapping("/tasks")
    public Result<PageResult<TrainingTaskResponse>> getTaskList(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) Long creatorId) {
        return trainingTaskService.getTaskList(pageRequest, creatorId);
    }

    @Operation(summary = "获取训练任务详情", description = "根据ID获取训练任务详细信息")
    @GetMapping("/tasks/{taskId}")
    public Result<TrainingTaskResponse> getTaskById(@PathVariable Long taskId) {
        return trainingTaskService.getTaskById(taskId);
    }

    @Operation(summary = "取消训练任务", description = "取消正在进行的训练任务")
    @PutMapping("/tasks/{taskId}/cancel")
    public Result<Void> cancelTask(@PathVariable Long taskId) {
        return trainingTaskService.cancelTask(taskId);
    }
}