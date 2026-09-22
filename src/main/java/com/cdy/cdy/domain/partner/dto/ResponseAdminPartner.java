package com.cdy.cdy.domain.partner.dto;

import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;

import java.time.LocalDateTime;

/**
 * 어드민 파트너 목록 응답. HIDDEN 포함 전체를 내려준다.
 */
public record ResponseAdminPartner(
        Long id,
        String name,
        PartnerCategory category,
        String description,
        String logoImageKey,
        String logoImageUrl,
        String websiteUrl,
        PartnerStatus status,
        String imageUrl,
        String linkUrl,
        Integer sortOrder,
        LocalDateTime createdAt
) {
}
