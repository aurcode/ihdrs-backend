// UserLoginRequest.java - 用户登录请求
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserLoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    private String password;
}