// AuthService.java - 认证服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.common.utils.JwtUtil;
import com.ihdrs.backend.common.utils.PasswordUtil;
import com.ihdrs.backend.dto.request.UserLoginRequest;
import com.ihdrs.backend.dto.request.UserRegisterRequest;
import com.ihdrs.backend.dto.response.LoginResponse;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @Transactional
    public Result<UserResponse> register(UserRegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return Result.error(400, "用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return Result.error(400, "邮箱已被使用");
        }

        // 验证密码强度
        if (!passwordUtil.isValidPassword(request.getPassword())) {
            return Result.error(400, "密码强度不足，至少需要6个字符");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordUtil.encodePassword(request.getPassword()));
        user.setSalt(passwordUtil.generateSalt());
        user.setRole(User.UserRole.ADMIN); // TODO
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(true);
        user.setLoginCount(0);

        user = userRepository.save(user);

        log.info("用户注册成功: {}", user.getUsername());

        // 转换为响应对象
        UserResponse response = convertToUserResponse(user);
        return Result.success("注册成功", response);
    }

    /**
     * 用户登录
     */
    @Transactional
    public Result<LoginResponse> login(UserLoginRequest request) {
        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        // 检查用户状态
        if (!user.getStatus()) {
            return Result.error(403, "账户已被禁用");
        }

        // 验证密码
        if (!passwordUtil.matches(request.getPassword(), user.getPasswordHash())) {
            return Result.error(401, "用户名或密码错误");
        }

        // 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        // 生成Token
        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );

        log.info("用户登录成功: {}", user.getUsername());

        // 构建登录响应
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24小时
                .userInfo(convertToUserResponse(user))
                .build();

        return Result.success("登录成功", response);
    }

    /**
     * 验证Token
     */
    public Result<UserResponse> validateToken(String token) {
        try {
            if (jwtUtil.isTokenExpired(token)) {
                return Result.error(401, "Token已过期");
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            User user = userRepository.findById(userId)
                    .orElse(null);

            if (user == null) {
                return Result.error(401, "用户不存在");
            }

            if (!user.getStatus()) {
                return Result.error(403, "账户已被禁用");
            }

            return Result.success(convertToUserResponse(user));
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return Result.error(401, "Token无效");
        }
    }

    /**
     * 转换为用户响应对象
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .email(user.getEmail())
                .phone(user.getPhone())
                .lastLoginTime(user.getLastLoginTime())
                .loginCount(user.getLoginCount())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }
}