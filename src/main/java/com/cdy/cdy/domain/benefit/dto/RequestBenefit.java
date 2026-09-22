package com.cdy.cdy.domain.benefit.dto;

import com.cdy.cdy.domain.benefit.entity.BenefitStatus;
import com.cdy.cdy.domain.benefit.entity.BenefitType;

import java.time.LocalDate;

public record RequestBenefit(
        Long partnerId,
        String title,
        String description,
        String discountText,
        BenefitType benefitType,
        String codeValue,
        String linkUrl,
        String usageGuide,
        String imageKey,
        LocalDate validFrom,
        LocalDate validTo,
        BenefitStatus status,
        Integer sortOrder
) {
}
