package com.cdy.cdy.domain.partner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResponsePartner {
    private Long id;
    private String name;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
