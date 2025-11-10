// UpdateProfileRequest.java
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 邮箱（可选）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 电话（可选）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String telephone;
}