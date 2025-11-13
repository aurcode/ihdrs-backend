// DatasetService.java

package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.dto.response.DatasetDTO;
import com.ihdrs.backend.dto.response.DatasetDetailVO;
import com.ihdrs.backend.dto.request.DatasetUploadRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatasetService {

    /**
     * 上传数据集
     */
    DatasetDTO uploadDataset(MultipartFile file, DatasetUploadRequest request, Long userId);

    /**
     * 获取数据集详情
     */
    DatasetDetailVO getDatasetDetail(Long datasetId, Long userId);

    /**
     * 获取用户的数据集列表
     */
    PageResult<DatasetDTO> getUserDatasets(Long userId, int page, int size);

    /**
     * 获取所有可用的数据集（包括公开的和用户自己的）
     */
    List<DatasetDTO> getAvailableDatasets(Long userId);

    /**
     * 获取公开数据集列表
     */
    PageResult<DatasetDTO> getPublicDatasets(int page, int size);

    /**
     * 删除数据集
     */
    void deleteDataset(Long datasetId, Long userId);

    /**
     * 更新数据集信息
     */
    DatasetDTO updateDataset(Long datasetId, DatasetUploadRequest request, Long userId);

    /**
     * 处理数据集（解压、验证、提取信息）
     */
    void processDataset(Long datasetId);

    /**
     * 设置数据集为公开/私有
     */
    void setDatasetPublic(Long datasetId, Boolean isPublic, Long userId);
}