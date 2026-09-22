package com.cdy.cdy.domain.benefit.dto;

import com.cdy.cdy.domain.benefit.entity.BenefitType;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;

import java.time.LocalDate;

/**
 * 상세 응답. codeValue / linkUrl 은 claim 시에만 반환하므로 여기에 포함하지 않는다.
 */
public record ResponseBenefitDetail(
        Long id,
        String title,
        String discountText,
        BenefitType benefitType,
        String partnerName,
        PartnerCategory partnerCategory,
        String imageUrl,
        String logoImageUrl,
        String description,
        String usageGuide,
        LocalDate validFrom,
        LocalDate validTo,
        String websiteUrl
) {
}
