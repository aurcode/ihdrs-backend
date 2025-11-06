// UpdateModelRequest.java - 更新模型请求
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateModelRequest {

    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    @Size(max = 20, message = "版本号长度不能超过20个字符")
    private String modelVersion;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    private String modelType;
}