// DatasetRepository.java

package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    // 根据创建者ID查询数据集
    List<Dataset> findByCreatorId(Long creatorId);

    // 根据状态查询数据集
    List<Dataset> findByStatus(String status);

    // 根据创建者ID分页查询数据集
    Page<Dataset> findByCreatorId(Long creatorId, Pageable pageable);

    // 查询所有公开数据集
    Page<Dataset> findByIsPublicTrue(Pageable pageable);

    // 查询可用的数据集（用户自己的或公开的）
    @Query("SELECT d FROM Dataset d WHERE (d.creatorId = :userId OR d.isPublic = true) AND d.status = 'AVAILABLE'")
    List<Dataset> findAvailableDatasets(@Param("userId") Long userId);

    // 根据状态查询数据集
    List<Dataset> findByStatus(Dataset.DatasetStatus status);

    // 根据数据集名称和创建者ID查询（检查重复）
    Optional<Dataset> findByDatasetNameAndCreatorId(String datasetName, Long creatorId);

    // 根据类型和状态查询
    List<Dataset> findByDatasetTypeAndStatus(
            Dataset.DatasetType datasetType,
            Dataset.DatasetStatus status
    );

    // 统计用户的数据集数量
    long countByCreatorId(Long creatorId);

    // 统计公开数据集数量
    long countByIsPublicTrue();
}