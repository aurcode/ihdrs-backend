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
    @NotNull(message = "数据集ID不能为空")
    private Long datasetId;
    private Boolean useAugmentation = false;
    private String augmentationStrength = "medium"; // light, medium, strong
    private BigDecimal validationSplit = new BigDecimal("0.2");

    // 模型配置
    @NotBlank(message = "模型类型不能为空")
    private String modelType = "CNN"; // CNN, ADVANCED_CNN, RESNET, VGG, MOBILENET

    private Integer hiddenSize = 128;
    private String activation = "relu"; // relu, leaky_relu, elu, sigmoid, tanh
    private BigDecimal dropout = new BigDecimal("0.2");
    private Boolean useBatchNorm = true; // 是否使用批归一化

    // 训练配置
    private BigDecimal learningRate = new BigDecimal("0.001");
    private Integer batchSize = 32;
    private String optimizer = "adam"; // adam, adamw, sgd, rmsprop, nadam
    private String lossFunction = "categorical_crossentropy";

    // 高级配置
    private BigDecimal l2Regularization = new BigDecimal("0.0"); // L2正则化系数
    private Integer earlyStoppingPatience = 5; // 早停轮数，0表示不使用
    private String lrScheduler = "none"; // 学习率衰减策略: none, exponential, cosine, step, reduce_on_plateau

    private String description; // 任务描述
}