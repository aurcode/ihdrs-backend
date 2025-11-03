// RecognitionController.java - 识别控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.common.utils.JwtUtil;
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
    private final JwtUtil jwtUtil;

    @Operation(summary = "数字识别", description = "识别手写数字图片（无需登录）")
    @PostMapping("/recognize")
    public Result<RecognitionResponse> recognize(
            @Valid @RequestBody RecognitionRequest request,
            HttpServletRequest httpRequest) {

        // 从请求头中获取用户ID（如果已登录）
        Long userId = getUserIdFromRequest(httpRequest);

        return recognitionService.recognize(request, userId);
    }

    @Operation(summary = "获取识别历史记录", description = "获取当前用户的识别记录历史")
    @GetMapping("/history")
    public Result<?> getHistory(HttpServletRequest httpRequest,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {

        Long userId = getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录用户无法查看历史记录");
        }

        return recognitionService.getHistory(userId, page, size);
    }


    /**
     * 从请求中获取用户ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null; // 未登录或Token无效
    }
}