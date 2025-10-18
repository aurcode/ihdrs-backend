// RecognitionController.java - 识别控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.RecognitionRequest;
import com.ihdrs.backend.dto.response.RecognitionResponse;
import com.ihdrs.backend.service.RecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;


@Tag(name = "识别服务", description = "手写数字识别相关接口")
@RestController
@RequestMapping("/recognition")
@RequiredArgsConstructor
@Validated
public class RecognitionController {

    private final RecognitionService recognitionService;

    @Operation(summary = "数字识别", description = "识别手写数字图片（无需登录）")
    @PostMapping("/recognize")
    public Result<RecognitionResponse> recognize(
            @Valid @RequestBody RecognitionRequest request,
            HttpServletRequest httpRequest) {

        // 从请求头中获取用户ID（如果已登录）
        Long userId = getUserIdFromRequest(httpRequest);

        return recognitionService.recognize(request, userId);
    }

    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        // 这里可以从JWT Token中解析用户ID
        // 暂时返回null表示匿名用户
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            // TODO: 从Token中解析用户ID
            return null;
        }
        return null;
    }
}