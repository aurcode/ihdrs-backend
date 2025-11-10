// UserService.java
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.UpdateProfileRequest;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public Result<PageResult<UserResponse>> getUserList(PageRequest pageRequest) {
        org.springframework.data.domain.PageRequest springPageRequest = org.springframework.data.domain.PageRequest.of(
                pageRequest.getCurrent().intValue() - 1,
                pageRequest.getSize().intValue(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<User> userPage = userRepository.findAll(springPageRequest);

        List<UserResponse> userList = userPage.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        PageResult<UserResponse> result = PageResult.of(
                userList,
                userPage.getTotalElements(),
                pageRequest.getSize(),
                pageRequest.getCurrent()
        );

        return Result.success(result);
    }

    /**
     * 根据ID获取用户信息
     */
    public Result<UserResponse> getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        return Result.success(convertToUserResponse(user));
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public Result<Void> updateUserStatus(Long userId, Boolean status) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setStatus(status);
        userRepository.save(user);

        log.info("更新用户状态: userId={}, status={}", userId, status);
        return Result.success("更新成功", null);
    }

    /**
     * 获取活跃用户数量
     */
    public Result<Long> getActiveUserCount() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Long count = userRepository.countActiveUsers(since);
        return Result.success(count);
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

    /**
     * 检查用户名是否已被其他用户占用
     * @param username 要检查的用户名
     * @param excludeUserId 排除的用户ID（当前用户）
     * @return true=已存在（不可用）, false=不存在（可用）
     */
    public boolean usernameExistsExcludingUser(String username, Long excludeUserId) {
        return userRepository.existsByUsernameAndUserIdNot(username, excludeUserId);
    }

    /**
     * 更新当前用户资料（用户名、邮箱、电话）
     */
    @Transactional
    public Result<Void> updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 1. 处理用户名更新
        if (StringUtils.hasText(req.getUsername()) &&
                !req.getUsername().equals(user.getUsername())) {

            // 检查用户名是否被占用
            if (usernameExistsExcludingUser(req.getUsername(), userId)) {
                return Result.error(400, "用户名已存在");
            }

            user.setUsername(req.getUsername());
            log.info("用户 {} 的用户名已更新为: {}", userId, req.getUsername());
        }

        // 2. 处理邮箱更新（可以为空）
        if (req.getEmail() != null) {
            user.setEmail(StringUtils.hasText(req.getEmail()) ? req.getEmail() : null);
        }

        // 3. 处理电话更新（可以为空）
        if (req.getTelephone() != null) {
            user.setPhone(StringUtils.hasText(req.getTelephone()) ? req.getTelephone() : null);
        }

        userRepository.save(user);
        log.info("用户资料更新成功: userId={}", userId);

        return Result.success("更新成功", null);
    }

    /**
     * 修改当前用户密码
     */
    @Transactional
    public Result<Void> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return Result.error(400, "原密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("用户 {} 密码修改成功", userId);
        return Result.success("密码修改成功", null);
    }
}