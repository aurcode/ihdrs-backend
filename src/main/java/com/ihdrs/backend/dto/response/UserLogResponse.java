package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLogResponse {

    private Long logId;
    private Long userId;
    private String action;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createTime;
}
