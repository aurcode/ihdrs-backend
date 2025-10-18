// RecognitionRequest.java - 识别请求
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecognitionRequest {

    @NotNull(message = "图像数据不能为空")
    private String imageData; // Base64编码的图像数据

    private String inputType = "CANVAS"; // CANVAS, UPLOAD, CAMERA

    private String sessionId; // 会话ID

    private String clientInfo; // 客户端信息JSON
}