package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public Result<?> getDashboardStats() {
        return Result.success(statsService.getDashboardStats());
    }

    @GetMapping("/recognitions")
    public Result<?> getRecentRecognitions(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(statsService.getRecentRecognitions(limit));
    }

    @GetMapping("/performance")
    public Result<?> getPerformanceMetrics() {
        return Result.success(statsService.getPerformanceMetrics());
    }

    @GetMapping("/analysis")
    public Result<?> getErrorAnalysis() {
        return Result.success(statsService.getErrorAnalysis());
    }
}
