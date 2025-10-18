// LoginResponse.java - 登录响应
package com.ihdrs.backend.dto.response;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponse userInfo;
}