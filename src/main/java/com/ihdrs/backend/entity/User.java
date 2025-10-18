// User.java - 用户实体类
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "salt", nullable = false, length = 32)
    private String salt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "status")
    private Boolean status = true;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public enum UserRole {
        USER, ADMIN
    }
}