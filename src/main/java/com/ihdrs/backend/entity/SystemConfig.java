// SystemConfig.java - 系统配置实体类
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @NotBlank(message = "配置键不能为空")
    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_type")
    private ConfigType configType = ConfigType.STRING;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public enum ConfigType {
        STRING, NUMBER, BOOLEAN, JSON
    }
}