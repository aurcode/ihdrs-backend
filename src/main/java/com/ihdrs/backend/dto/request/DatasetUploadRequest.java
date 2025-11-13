// DatasetUploadRequest.java

package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetUploadRequest {

    @NotBlank(message = "数据集名称不能为空")
    private String datasetName;

    private String description;

    @NotNull(message = "数据集类型不能为空")
    private String datasetType = "IMAGE_CLASSIFICATION";

    private Boolean isPublic = false;
}