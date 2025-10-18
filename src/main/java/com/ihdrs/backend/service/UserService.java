// UserService.java - 用户服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
}