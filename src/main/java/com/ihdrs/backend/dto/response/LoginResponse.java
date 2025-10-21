// LoginResponse.java - 登录响应
package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponse userInfo;
}