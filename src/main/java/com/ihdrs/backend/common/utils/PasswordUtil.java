// PasswordUtil.java - 密码工具类
package com.ihdrs.backend.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordUtil {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成随机盐值
     */
    public String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 加密密码
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 验证密码强度
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // 可以添加更复杂的密码强度验证逻辑
        return true;
    }
}