// SecurityTest.java - 安全测试
package com.ihdrs.backend.security;

import com.ihdrs.backend.TestApplication;
import com.ihdrs.backend.common.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testJWTTokenGeneration() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testJWTTokenValidation() {
        String token = jwtUtil.generateToken(1L, "testuser", "USER");

        assertTrue(jwtUtil.validateToken(token));
        assertFalse(jwtUtil.isTokenExpired(token));

        assertEquals("testuser", jwtUtil.getUsernameFromToken(token));
        assertEquals(Long.valueOf(1L), jwtUtil.getUserIdFromToken(token));
        assertEquals("USER", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void testJWTTokenInvalidation() {
        String invalidToken = "invalid.jwt.token";

        assertFalse(jwtUtil.validateToken(invalidToken));
        assertNull(jwtUtil.getUsernameFromToken(invalidToken));
        assertNull(jwtUtil.getUserIdFromToken(invalidToken));
        assertNull(jwtUtil.getRoleFromToken(invalidToken));
    }
}