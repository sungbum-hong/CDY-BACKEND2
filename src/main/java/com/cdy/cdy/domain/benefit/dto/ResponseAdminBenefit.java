package com.cdy.cdy.domain.benefit.dto;

import com.cdy.cdy.domain.benefit.entity.BenefitStatus;
import com.cdy.cdy.domain.benefit.entity.BenefitType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 어드민 혜택 목록/수정용 응답. HIDDEN·기간만료 건도 포함하고
 * 수정 폼을 채울 수 있도록 codeValue / linkUrl / imageKey 까지 내려준다.
 */
public record ResponseAdminBenefit(
        Long id,
        Long partnerId,
        String partnerName,
        String title,
        String description,
        String discountText,
        BenefitType benefitType,
        String codeValue,
        String linkUrl,
        String usageGuide,
        String imageKey,
        String imageUrl,
        LocalDate validFrom,
        LocalDate validTo,
        BenefitStatus status,
        Integer sortOrder,
        LocalDateTime createdAt
) {
}
