// ControllerIntegrationTest.java - 控制器集成测试
package com.ihdrs.backend.controller;

import com.ihdrs.backend.TestApplication;
import com.ihdrs.backend.dto.request.UserLoginRequest;
import com.ihdrs.backend.dto.request.UserRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void testPingEndpoint() throws Exception {
        mockMvc.perform(get("/health/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    void testUserRegistration() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void testUserLogin() throws Exception {
        // 先注册用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // 再测试登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.userInfo.username").value("testuser"));
    }

    @Test
    void testDatabaseConnection() throws Exception {
        mockMvc.perform(get("/test/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.database").value("connected"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/models/list"))
                .andExpect(status().isForbidden());
    }
}