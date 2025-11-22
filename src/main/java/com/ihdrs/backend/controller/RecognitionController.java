// RecognitionController.java
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.RecognitionRequest;
import com.ihdrs.backend.dto.response.RecognitionMultiResponse;
import com.ihdrs.backend.dto.response.RecognitionResponse;
import com.ihdrs.backend.service.RecognitionService;
import com.ihdrs.backend.common.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "识别服务", description = "手写数字识别相关接口")
@RestController
@RequestMapping("/recognition")
@RequiredArgsConstructor
@Validated
public class RecognitionController {

    private final RecognitionService recognitionService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "数字识别", description = "识别手写数字图片（可匿名识别）")
    @PostMapping("/recognize")
    public Result<RecognitionResponse> recognize(
            @Valid @RequestBody RecognitionRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        return recognitionService.recognize(request, userId);
    }

    @PostMapping("/recognize_multi")
    public Result<RecognitionMultiResponse> recognizeMulti(
            @Valid @RequestBody RecognitionRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromRequest(httpRequest);
        return recognitionService.recognizeMulti(request, userId);
    }


    @Operation(summary = "获取识别历史记录", description = "获取当前用户的识别记录历史（支持 result / 时间区间 筛选）")
    @GetMapping("/history")
    public Result<?> getHistory(HttpServletRequest httpRequest,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) Integer result,
                                @RequestParam(required = false) Long userId,
                                @RequestParam(required = false) String startTime,
                                @RequestParam(required = false) String endTime) {

        // 支持两种时间格式：
        // 1) yyyy-MM-dd （表示整天） 2) ISO 日期时间 (例如 2025-11-03T19:12:28 或 2025-11-03 19:12:28)
        LocalDateTime start = null, end = null;
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;

        try {
            if (startTime != null && !startTime.isBlank()) {
                if (startTime.length() == 10) { // yyyy-MM-dd
                    start = LocalDate.parse(startTime).atStartOfDay();
                } else {
                    start = LocalDateTime.parse(startTime, isoFormatter);
                }
            }
            if (endTime != null && !endTime.isBlank()) {
                if (endTime.length() == 10) { // yyyy-MM-dd
                    end = LocalDate.parse(endTime).atTime(23, 59, 59);
                } else {
                    end = LocalDateTime.parse(endTime, isoFormatter);
                }
            }
        } catch (Exception ex) {
            return Result.error(400, "时间格式错误，支持 yyyy-MM-dd 或 ISO 日期时间 (例如 2025-11-03T19:12:28)");
        }

        return recognitionService.getAllHistory(page, size, result, userId, start, end);
    }

    @GetMapping("/history_user")
    @Operation(summary = "获取识别历史记录", description = "仅获取当前用户自己的识别记录")
    public Result<?> getUserHistory(HttpServletRequest httpRequest,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) Integer result,
                                @RequestParam(required = false) String startTime,
                                @RequestParam(required = false) String endTime) {

        Long userId = getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录用户无法查看记录");
        }

        LocalDateTime start = null, end = null;
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;

        try {
            if (startTime != null && !startTime.isBlank()) {
                if (startTime.length() == 10) {
                    start = LocalDate.parse(startTime).atStartOfDay();
                } else {
                    start = LocalDateTime.parse(startTime, isoFormatter);
                }
            }
            if (endTime != null && !endTime.isBlank()) {
                if (endTime.length() == 10) {
                    end = LocalDate.parse(endTime).atTime(23, 59, 59);
                } else {
                    end = LocalDateTime.parse(endTime, isoFormatter);
                }
            }
        } catch (Exception ex) {
            return Result.error(400, "时间格式错误");
        }

        return recognitionService.getAllHistory(page, size, result, userId, start, end);
    }


    @GetMapping("/history_admin")
    public Result<?> getAllHistory(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) Integer result,
                                   @RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) String startTime,
                                   @RequestParam(required = false) String endTime) {

        LocalDateTime start = (startTime != null && !startTime.isBlank()) ? LocalDateTime.parse(startTime) : null;
        LocalDateTime end   = (endTime   != null && !endTime.isBlank())   ? LocalDateTime.parse(endTime)   : null;

        return recognitionService.getAllHistory(page, size, result, userId, start, end);
    }

    @Operation(summary = "删除识别记录", description = "删除指定识别记录（用户只能删除自己的记录；管理员可删除任意记录）")
    @DeleteMapping("/history/{recordId}")
    public Result<Void> deleteHistoryRecord(@PathVariable Long recordId,
                                            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录用户无法删除记录");
        }
        return recognitionService.deleteRecord(recordId, userId);
    }

    public Long getUserIdFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    /**
     * 批量删除识别记录
     */
    @DeleteMapping("/history/batch")
    @Operation(summary = "批量删除识别记录")
    public Result<Void> batchDeleteRecords(@RequestBody List<Long> recordIds, HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return recognitionService.batchDeleteRecords(recordIds, userId);
    }
}
