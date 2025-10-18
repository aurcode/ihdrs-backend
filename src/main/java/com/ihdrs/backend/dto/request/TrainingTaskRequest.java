// TrainingTaskRequest.java - 训练任务创建请求
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TrainingTaskRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "训练轮数不能为空")
    @Min(value = 1, message = "训练轮数至少为1")
    private Integer totalEpochs;

    // 数据集配置
    private String datasetName = "MNIST";
    private Boolean useAugmentation = false;
    private BigDecimal validationSplit = new BigDecimal("0.2");

    // 模型配置
    private String modelType = "CNN";
    private Integer hiddenSize = 128;
    private String activation = "relu";
    private BigDecimal dropout = new BigDecimal("0.2");

    // 训练配置
    private BigDecimal learningRate = new BigDecimal("0.001");
    private Integer batchSize = 32;
    private String optimizer = "adam";
    private String lossFunction = "categorical_crossentropy";

    private String description; // 任务描述
}