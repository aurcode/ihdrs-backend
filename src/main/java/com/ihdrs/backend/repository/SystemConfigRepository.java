// SystemConfigRepository.java - 系统配置数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置键查找配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 查询所有公开配置
     */
    List<SystemConfig> findByIsPublicTrue();

    /**
     * 根据配置键模糊查询
     */
    List<SystemConfig> findByConfigKeyContaining(String keyword);
}