// UserController.java - 用户管理控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.ChangePasswordRequest;
import com.ihdrs.backend.dto.request.UpdateProfileRequest;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.dto.response.UserLogResponse;
import com.ihdrs.backend.service.AuthService;
import com.ihdrs.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final AuthService authService;

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

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public Result<UserResponse> getMe(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        UserResponse user = authService.validateToken(token).getData();
        return Result.success(user);
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateProfileRequest req) {

        String token = authorization.replace("Bearer ", "");
        Long userId = authService.validateToken(token).getData().getUserId();

        Result<Void> result = userService.updateProfile(userId, req);

        if (result.getCode() == 200) {
            return ResponseEntity.ok(Result.success("信息修改成功"));
        } else if (result.getCode() == 400) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ChangePasswordRequest req) {

        String token = authorization.replace("Bearer ", "");
        Long userId = authService.validateToken(token).getData().getUserId();

        Result<Void> result = userService.changePassword(userId, req.getOldPassword(), req.getNewPassword());

        // 根据 Result 的 code 返回不同的状态码
        if (result.getCode() == 200) {
            return ResponseEntity.ok(Result.success("密码修改成功"));
        } else if (result.getCode() == 400) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result); // 400
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result); // 500
        }
    }

    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(
            @RequestParam String username,
            @RequestHeader("Authorization") String authorization) {

        String token = authorization.replace("Bearer ", "");
        Long currentUserId = authService.validateToken(token).getData().getUserId();

        // 检查用户名是否被其他用户占用
        boolean exists = userService.usernameExistsExcludingUser(username, currentUserId);
        return Result.success(exists);
    }

    @Operation(summary="修改用户角色", description = "管理员修改用户角色")
    @PutMapping("/{userId}/role")
    public Result<Void> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        return userService.updateUserRole(userId, role);
    }

    @Operation(summary = "获取用户日志", description = "分页查询用户的操作日志")
    @GetMapping("/{userId}/logs")
    public Result<PageResult<UserLogResponse>> getUserLogs(
            @PathVariable Long userId,
            @Valid PageRequest pageRequest) {
        return userService.getUserLogs(userId, pageRequest);
    }

}