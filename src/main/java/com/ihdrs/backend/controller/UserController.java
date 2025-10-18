// UserController.java - 用户管理控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取用户列表", description = "分页查询所有用户")
    @GetMapping("/list")
    public Result<PageResult<UserResponse>> getUserList(@Valid PageRequest pageRequest) {
        return userService.getUserList(pageRequest);
    }

    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @GetMapping("/{userId}")
    public Result<UserResponse> getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @Operation(summary = "更新用户状态", description = "启用或禁用用户账号")
    @PutMapping("/{userId}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Boolean status) {
        return userService.updateUserStatus(userId, status);
    }

    @Operation(summary = "获取活跃用户数", description = "获取最近30天活跃的用户数量")
    @GetMapping("/active-count")
    public Result<Long> getActiveUserCount() {
        return userService.getActiveUserCount();
    }
}