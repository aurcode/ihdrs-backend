// UserResponse.java - 用户响应
package com.ihdrs.backend.dto.response;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long userId;
    private String username;
    private String role;
    private String email;
    private String phone;
    private LocalDateTime lastLoginTime;
    private Integer loginCount;
    private Boolean status;
    private LocalDateTime createTime;
}