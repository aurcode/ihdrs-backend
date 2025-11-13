// DatasetController.java

package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.response.DatasetDTO;
import com.ihdrs.backend.dto.response.DatasetDetailVO;
import com.ihdrs.backend.dto.request.DatasetUploadRequest;
import com.ihdrs.backend.service.DatasetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    /**
     * 上传数据集
     */
    @PostMapping("/upload")
    public Result<DatasetDTO> uploadDataset(
            @RequestParam("file") MultipartFile file,
            @Valid DatasetUploadRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        DatasetDTO dataset = datasetService.uploadDataset(file, request, userId);
        return Result.success("数据集上传成功", dataset);
    }

    /**
     * 获取数据集详情
     */
    @GetMapping("/{datasetId}")
    public Result<DatasetDetailVO> getDatasetDetail(
            @PathVariable Long datasetId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        DatasetDetailVO detail = datasetService.getDatasetDetail(datasetId, userId);
        return Result.success(detail);
    }

    /**
     * 获取用户的数据集列表
     */
    @GetMapping("/my")
    public Result<PageResult<DatasetDTO>> getUserDatasets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        PageResult<DatasetDTO> result = datasetService.getUserDatasets(userId, page, size);
        return Result.success(result);
    }

    /**
     * 获取所有可用的数据集（用于训练时选择）
     */
    @GetMapping("/available")
    public Result<List<DatasetDTO>> getAvailableDatasets(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        List<DatasetDTO> datasets = datasetService.getAvailableDatasets(userId);
        return Result.success(datasets);
    }

    /**
     * 获取公开数据集列表
     */
    @GetMapping("/public")
    public Result<PageResult<DatasetDTO>> getPublicDatasets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResult<DatasetDTO> result = datasetService.getPublicDatasets(page, size);
        return Result.success(result);
    }

    /**
     * 删除数据集
     */
    @DeleteMapping("/{datasetId}")
    public Result<Void> deleteDataset(
            @PathVariable Long datasetId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        datasetService.deleteDataset(datasetId, userId);
        return Result.success();
    }

    /**
     * 更新数据集信息
     */
    @PutMapping("/{datasetId}")
    public Result<DatasetDTO> updateDataset(
            @PathVariable Long datasetId,
            @Valid @RequestBody DatasetUploadRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        DatasetDTO dataset = datasetService.updateDataset(datasetId, request, userId);
        return Result.success("数据集更新成功", dataset);
    }

    /**
     * 设置数据集公开状态
     */
    @PutMapping("/{datasetId}/public")
    public Result<Void> setDatasetPublic(
            @PathVariable Long datasetId,
            @RequestParam Boolean isPublic,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录或登录已过期");
        }

        datasetService.setDatasetPublic(datasetId, isPublic, userId);
        return Result.success();
    }
}