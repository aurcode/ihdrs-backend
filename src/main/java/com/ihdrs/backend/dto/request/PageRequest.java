// PageRequest.java - 分页请求
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageRequest {

    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    @Min(value = 1, message = "页面大小必须大于0")
    private Long size = 10L;

    private String sortField; // 排序字段
    private String sortOrder = "ASC"; // 排序方向：ASC, DESC

    // 计算偏移量
    public Long getOffset() {
        return (current - 1) * size;
    }
}