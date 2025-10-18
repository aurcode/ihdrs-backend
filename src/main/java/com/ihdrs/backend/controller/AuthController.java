// AuthController.java - 认证控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.UserLoginRequest;
import com.ihdrs.backend.dto.request.UserRegisterRequest;
import com.ihdrs.backend.dto.response.LoginResponse;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户注册、登录相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "用户登录", description = "用户登录获取Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "验证Token", description = "验证Token是否有效")
    @GetMapping("/validate")
    public Result<UserResponse> validate(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        return authService.validateToken(token);
    }
}