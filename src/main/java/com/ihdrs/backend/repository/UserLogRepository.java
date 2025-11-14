package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.UserLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    /**
     * 根据用户ID分页查询日志
     */
    Page<UserLog> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
}