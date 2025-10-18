// HealthController.java - 健康检查控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统健康", description = "系统健康检查接口")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Operation(summary = "健康检查", description = "检查系统是否正常运行")
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now());
        data.put("service", "ihdrs-backend");
        data.put("version", "1.0.0");

        return Result.success(data);
    }

    @Operation(summary = "Ping测试", description = "简单的连接测试")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }
}