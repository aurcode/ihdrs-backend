// TestController.java - 测试控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "测试接口", description = "用于测试的临时接口")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;

    @Operation(summary = "测试接口", description = "测试系统是否正常工作")
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("Hello from IHDRS Backend!");
    }

    @Operation(summary = "测试数据库", description = "测试数据库连接是否正常")
    @GetMapping("/db")
    public Result<Map<String, Object>> testDatabase() {
        Map<String, Object> data = new HashMap<>();

        try {
            long userCount = userRepository.count();
            data.put("database", "connected");
            data.put("userCount", userCount);
            data.put("status", "success");
        } catch (Exception e) {
            data.put("database", "error");
            data.put("error", e.getMessage());
            data.put("status", "failed");
        }

        return Result.success(data);
    }

    @Operation(summary = "测试创建用户", description = "测试用户创建功能")
    @PostMapping("/create-user")
    public Result<User> createTestUser(@RequestParam String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("test_password");
        user.setSalt("test_salt");
        user.setRole(User.UserRole.USER);
        user.setStatus(true);

        User savedUser = userRepository.save(user);
        return Result.success("用户创建成功", savedUser);
    }
}