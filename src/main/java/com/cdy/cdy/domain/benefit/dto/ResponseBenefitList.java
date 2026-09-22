package com.cdy.cdy.domain.benefit.dto;

import com.cdy.cdy.domain.benefit.entity.BenefitType;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;

public record ResponseBenefitList(
        Long id,
        String title,
        String discountText,
        BenefitType benefitType,
        String partnerName,
        PartnerCategory partnerCategory,
        String imageUrl,
        String logoImageUrl
) {
}
