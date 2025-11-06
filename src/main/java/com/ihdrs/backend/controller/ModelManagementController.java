// ModelManagementController.java - 模型管理控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.UpdateModelRequest;
import com.ihdrs.backend.dto.response.ModelResponse;
import com.ihdrs.backend.dto.response.ModelVersionResponse;
import com.ihdrs.backend.service.ModelManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "模型管理增强", description = "深度学习模型管理增强接口")
@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
@Validated
public class ModelManagementController {

    private final ModelManagementService modelManagementService;

    @Operation(summary = "获取模型列表", description = "分页查询所有模型")
    @GetMapping("/list")
    public Result<PageResult<ModelResponse>> getModelList(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String keyword) {
        return modelManagementService.getModelList(pageRequest, status, modelType, keyword);
    }

    @Operation(summary = "获取活跃模型", description = "获取当前正在使用的模型")
    @GetMapping("/active")
    public Result<ModelResponse> getActiveModel() {
        return modelManagementService.getActiveModel();
    }

    @Operation(summary = "获取模型详情", description = "根据ID获取模型详细信息")
    @GetMapping("/{modelId}")
    public Result<ModelResponse> getModelById(@PathVariable Long modelId) {
        return modelManagementService.getModelById(modelId);
    }

    @Operation(summary = "切换活跃模型", description = "将指定模型设置为当前使用的模型")
    @PutMapping("/{modelId}/activate")
    public Result<Void> switchActiveModel(
            @PathVariable Long modelId,
            @RequestAttribute("userId") Long userId) {
        return modelManagementService.switchActiveModel(modelId, userId);
    }

    @Operation(summary = "停用模型", description = "将指定模型设置为停用状态")
    @PutMapping("/{modelId}/disable")
    public Result<Void> disableModel(
            @PathVariable Long modelId,
            @RequestAttribute("userId") Long userId) {
        return modelManagementService.disableModel(modelId, userId);
    }

    @Operation(summary = "启用模型", description = "将停用的模型重新启用")
    @PutMapping("/{modelId}/enable")
    public Result<Void> enableModel(
            @PathVariable Long modelId,
            @RequestAttribute("userId") Long userId) {
        return modelManagementService.enableModel(modelId, userId);
    }

    @Operation(summary = "删除模型", description = "删除指定模型（逻辑删除）")
    @DeleteMapping("/{modelId}")
    public Result<Void> deleteModel(
            @PathVariable Long modelId,
            @RequestAttribute("userId") Long userId) {
        return modelManagementService.deleteModel(modelId, userId);
    }

    @Operation(summary = "更新模型信息", description = "更新模型的描述等信息")
    @PutMapping("/{modelId}")
    public Result<ModelResponse> updateModel(
            @PathVariable Long modelId,
            @Valid @RequestBody UpdateModelRequest request,
            @RequestAttribute("userId") Long userId) {
        return modelManagementService.updateModel(modelId, request, userId);
    }

    @Operation(summary = "获取模型版本列表", description = "获取同一模型名称的所有版本")
    @GetMapping("/{modelName}/versions")
    public Result<List<ModelVersionResponse>> getModelVersions(@PathVariable String modelName) {
        return modelManagementService.getModelVersions(modelName);
    }

    @Operation(summary = "版本对比", description = "对比两个模型版本的差异")
    @GetMapping("/compare")
    public Result<Object> compareModels(
            @RequestParam Long modelId1,
            @RequestParam Long modelId2) {
        return modelManagementService.compareModels(modelId1, modelId2);
    }

    @Operation(summary = "获取模型统计信息", description = "获取模型相关的统计信息")
    @GetMapping("/statistics")
    public Result<Object> getModelStatistics() {
        return modelManagementService.getModelStatistics();
    }

    @Operation(summary = "批量删除模型", description = "批量删除指定的模型")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteModels(
            @RequestBody List<Long> modelIds,
            @RequestAttribute("userId") Long userId) {
        return modelManagementService.batchDeleteModels(modelIds, userId);
    }
}