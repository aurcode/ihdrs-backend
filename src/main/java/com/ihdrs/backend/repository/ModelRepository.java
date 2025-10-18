// ModelRepository.java - 模型数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    /**
     * 根据模型名称和版本查找模型
     */
    Optional<Model> findByModelNameAndModelVersion(String modelName, String modelVersion);

    /**
     * 检查模型名称和版本是否存在
     */
    boolean existsByModelNameAndModelVersion(String modelName, String modelVersion);

    /**
     * 查找当前活跃的模型
     */
    Optional<Model> findByStatus(Model.ModelStatus status);

    /**
     * 根据状态分页查询模型
     */
    Page<Model> findByStatusOrderByCreateTimeDesc(Model.ModelStatus status, Pageable pageable);

    /**
     * 根据创建者查询模型
     */
    Page<Model> findByCreatorIdOrderByCreateTimeDesc(Long creatorId, Pageable pageable);

    /**
     * 查询所有非DISABLED状态的模型，按准确率降序排列
     */
    @Query("SELECT m FROM Model m WHERE m.status != 'DISABLED' ORDER BY m.accuracy DESC, m.createTime DESC")
    List<Model> findAvailableModelsOrderByAccuracy();

    /**
     * 将所有模型状态从ACTIVE改为COMPLETED
     */
    @Modifying
    @Query("UPDATE Model m SET m.status = 'COMPLETED' WHERE m.status = 'ACTIVE'")
    int deactivateAllModels();

    /**
     * 统计各状态模型数量
     */
    @Query("SELECT m.status, COUNT(m) FROM Model m GROUP BY m.status")
    List<Object[]> countByStatus();

    /**
     * 根据模型名称模糊查询
     */
    @Query("SELECT m FROM Model m WHERE m.modelName LIKE %:keyword% OR m.description LIKE %:keyword%")
    Page<Model> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}