// ModelController.java - 模型管理控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.ModelResponse;
import com.ihdrs.backend.service.ModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "模型管理", description = "深度学习模型管理相关接口")
@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
@Validated
public class ModelController {

    private final ModelService modelService;

    @Operation(summary = "获取模型列表", description = "分页查询所有模型")
    @GetMapping("/list")
    public Result<PageResult<ModelResponse>> getModelList(@Valid PageRequest pageRequest) {
        return modelService.getModelList(pageRequest);
    }

    @Operation(summary = "获取活跃模型", description = "获取当前正在使用的模型")
    @GetMapping("/active")
    public Result<ModelResponse> getActiveModel() {
        return modelService.getActiveModel();
    }

    @Operation(summary = "获取模型详情", description = "根据ID获取模型详细信息")
    @GetMapping("/{modelId}")
    public Result<ModelResponse> getModelById(@PathVariable Long modelId) {
        return modelService.getModelById(modelId);
    }

    @Operation(summary = "切换活跃模型", description = "将指定模型设置为当前使用的模型")
    @PutMapping("/{modelId}/activate")
    public Result<Void> switchActiveModel(@PathVariable Long modelId) {
        return modelService.switchActiveModel(modelId);
    }
}