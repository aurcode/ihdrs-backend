// ModelService.java - 模型服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.ModelResponse;
import com.ihdrs.backend.entity.Model;
import com.ihdrs.backend.repository.ModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {

    private final ModelRepository modelRepository;

    /**
     * 分页查询模型列表
     */
    public Result<PageResult<ModelResponse>> getModelList(PageRequest pageRequest) {
        org.springframework.data.domain.PageRequest springPageRequest = org.springframework.data.domain.PageRequest.of(
                pageRequest.getCurrent().intValue() - 1,
                pageRequest.getSize().intValue(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<Model> modelPage = modelRepository.findAll(springPageRequest);

        // 获取当前活跃模型ID
        Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE)
                .orElse(null);
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
        Model model = modelRepository.findByStatus(Model.ModelStatus.ACTIVE)
                .orElse(null);

        if (model == null) {
            return Result.error(404, "没有活跃的模型");
        }

        return Result.success(convertToModelResponse(model, model.getModelId()));
    }

    /**
     * 切换活跃模型
     */
    @Transactional
    public Result<Void> switchActiveModel(Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        if (model.getStatus() != Model.ModelStatus.COMPLETED) {
            return Result.error(400, "只有已完成的模型才能设置为活跃状态");
        }

        // 将所有模型设置为COMPLETED状态
        modelRepository.deactivateAllModels();

        // 设置当前模型为ACTIVE
        model.setStatus(Model.ModelStatus.ACTIVE);
        modelRepository.save(model);

        log.info("切换活跃模型: modelId={}, modelName={}", modelId, model.getModelName());
        return Result.success("模型切换成功", null);
    }

    /**
     * 根据ID获取模型信息
     */
    public Result<ModelResponse> getModelById(Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElse(null);

        if (model == null) {
            return Result.error(404, "模型不存在");
        }

        Model activeModel = modelRepository.findByStatus(Model.ModelStatus.ACTIVE)
                .orElse(null);
        Long activeModelId = activeModel != null ? activeModel.getModelId() : null;

        return Result.success(convertToModelResponse(model, activeModelId));
    }

    /**
     * 转换为模型响应对象
     */
    private ModelResponse convertToModelResponse(Model model, Long activeModelId) {
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
                .createTime(model.getCreateTime())
                .updateTime(model.getUpdateTime())
                .isActive(model.getModelId().equals(activeModelId))
                .build();
    }
}