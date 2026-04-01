package com.cdy.cdy.domain.service.dto;

import com.cdy.cdy.domain.service.entity.ServiceCategory;
import com.cdy.cdy.domain.service.entity.ServiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResponseService {
    private Long id;
    private Long userId;
    private String userNickname;
    private String title;
    private String description;
    private Integer price;
    private ServiceCategory category;
    private String thumbnail;
    private ServiceStatus status;
    private LocalDateTime createdAt;
}
