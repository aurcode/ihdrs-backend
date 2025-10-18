// UserRepository.java - 用户数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据状态分页查询用户
     */
    Page<User> findByStatus(Boolean status, Pageable pageable);

    /**
     * 根据角色和状态查询用户
     */
    Page<User> findByRoleAndStatus(User.UserRole role, Boolean status, Pageable pageable);

    /**
     * 查询指定时间段内注册的用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createTime BETWEEN :startTime AND :endTime")
    Long countByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询活跃用户数量（最近30天有登录记录）
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginTime >= :since")
    Long countActiveUsers(@Param("since") LocalDateTime since);

    /**
     * 根据用户名模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}