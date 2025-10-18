// AuthServiceTest.java - 认证服务测试
package com.ihdrs.backend.service;

import com.ihdrs.backend.TestApplication;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.UserLoginRequest;
import com.ihdrs.backend.dto.request.UserRegisterRequest;
import com.ihdrs.backend.dto.response.LoginResponse;
import com.ihdrs.backend.dto.response.UserResponse;
import com.ihdrs.backend.entity.User;
import com.ihdrs.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testUserRegistration() {
        // 准备测试数据
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        // 执行注册
        Result<UserResponse> result = authService.register(request);

        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("testuser", result.getData().getUsername());

        // 验证数据库中的数据
        assertTrue(userRepository.existsByUsername("testuser"));
    }

    @Test
    void testUserRegistrationWithDuplicateUsername() {
        // 先创建一个用户
        User existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setPasswordHash("hashedpassword");
        existingUser.setSalt("salt");
        existingUser.setRole(User.UserRole.USER);
        existingUser.setStatus(true);
        userRepository.save(existingUser);

        // 尝试注册相同用户名
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Result<UserResponse> result = authService.register(request);

        // 验证结果
        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("用户名已存在"));
    }

    @Test
    void testUserLogin() {
        // 先注册用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        // 执行登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        Result<LoginResponse> result = authService.login(loginRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getToken());
        assertEquals("testuser", result.getData().getUserInfo().getUsername());
    }

    @Test
    void testUserLoginWithWrongPassword() {
        // 先注册用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        // 使用错误密码登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        Result<LoginResponse> result = authService.login(loginRequest);

        // 验证结果
        assertEquals(401, result.getCode());
        assertTrue(result.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testTokenValidation() {
        // 先注册并登录用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        Result<LoginResponse> loginResult = authService.login(loginRequest);

        String token = loginResult.getData().getToken();

        // 验证Token
        Result<UserResponse> validateResult = authService.validateToken(token);

        // 验证结果
        assertNotNull(validateResult);
        assertEquals(200, validateResult.getCode());
        assertEquals("testuser", validateResult.getData().getUsername());
    }
}