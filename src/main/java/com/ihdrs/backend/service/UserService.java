// UserService.java
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.request.UpdateProfileRequest;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.dto.response.UserLogResponse;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.entity.UserLog;
import com.ihdrs.backend.repository.UserRepository;
import com.ihdrs.backend.repository.UserLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLogRepository userLogRepository;

    /**
     * 分页查询用户列表（支持搜索、角色筛选、状态筛选）
     */
    public Result<PageResult<UserResponse>> getUserList(PageRequest pageRequest) {

        // 构建分页和排序
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.getCurrent().intValue() - 1,
                pageRequest.getSize().intValue(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        // 动态构建筛选条件
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 搜索：用户名模糊匹配 （对应前端 search）
            if (pageRequest.getUsername() != null && !pageRequest.getUsername().isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + pageRequest.getUsername() + "%"));
            }

            // 角色筛选（ADMIN / USER）
            if (pageRequest.getRole() != null && !pageRequest.getRole().isEmpty()) {
                predicates.add(cb.equal(root.get("role"), pageRequest.getRole()));
            }

            // 状态筛选（1=启用，0=禁用）
            Boolean statusValue = null;
            String raw = pageRequest.getStatus();
            if ("1".equals(raw) || "true".equalsIgnoreCase(raw)) statusValue = true;
            if ("0".equals(raw) || "false".equalsIgnoreCase(raw)) statusValue = false;

            if (statusValue != null) {
                predicates.add(cb.equal(root.get("status"), statusValue));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 执行分页查询
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // 转换 DTO
        List<UserResponse> userList = userPage.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        // 封装结果
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

        return Result.success("密码修改成功", null);
    }

    /**
     * 修改用户角色
     */
    @Transactional
    public Result<Void> updateUserRole(Long userId, String role) {
        try {
            // 验证角色是否合法
            if (!role.equals("USER") && !role.equals("ADMIN")) {
                return Result.error(400, "角色参数错误");
            }

            // 查询用户是否存在
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            // 将字符串转换为枚举并设置角色
            User.UserRole userRole = User.UserRole.valueOf(role);
            user.setRole(userRole);

            userRepository.save(user);

            return Result.success("角色修改成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "角色参数错误");
        } catch (Exception e) {
            return Result.error(500, "修改角色失败");
        }
    }

    /**
     * 获取用户行为日志
     */
    public Result<PageResult<UserLogResponse>> getUserLogs(Long userId, PageRequest pageRequest) {
        try {
            org.springframework.data.domain.PageRequest springPageRequest =
                    org.springframework.data.domain.PageRequest.of(
                            pageRequest.getCurrent().intValue() - 1,
                            pageRequest.getSize().intValue()
                    );

            Page<UserLog> logPage = userLogRepository.findByUserIdOrderByCreateTimeDesc(
                    userId,
                    springPageRequest
            );

            List<UserLogResponse> logResponses = logPage.getContent().stream()
                    .map(log -> UserLogResponse.builder()
                            .logId(log.getLogId())
                            .userId(log.getUserId())
                            .action(log.getAction())
                            .ipAddress(log.getIpAddress())
                            .userAgent(log.getUserAgent())
                            .createTime(log.getCreateTime())
                            .build())
                    .collect(Collectors.toList());

            PageResult<UserLogResponse> result = PageResult.of(
                    logResponses,
                    logPage.getTotalElements(),
                    pageRequest.getSize(),
                    pageRequest.getCurrent()
            );

            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取用户日志失败");
        }
    }

}