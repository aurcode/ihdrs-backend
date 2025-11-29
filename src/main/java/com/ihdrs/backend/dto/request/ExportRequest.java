// ExportRequest.java

package com.ihdrs.backend.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 导出请求参数
 */
@Data
public class ExportRequest {

    /**
     * 导出格式: excel, csv, pdf
     */
    private String format = "excel";

    /**
     * 导出范围: current(当前页), filtered(筛选后全部), all(全部)
     */
    private String scope = "filtered";

    /**
     * 导出字段列表
     */
    private List<String> fields;

    /**
     * 当前页码（scope=current时使用）
     */
    private Integer page;

    /**
     * 每页大小（scope=current时使用）
     */
    private Integer size;
}