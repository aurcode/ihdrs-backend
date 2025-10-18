// PageResult.java - 分页结果类
package com.ihdrs.backend.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private Long total;
    private Long size;
    private Long current;
    private Long pages;

    public static <T> PageResult<T> of(List<T> records, Long total, Long size, Long current) {
        Long pages = (total + size - 1) / size; // 计算总页数
        return new PageResult<>(records, total, size, current, pages);
    }
}