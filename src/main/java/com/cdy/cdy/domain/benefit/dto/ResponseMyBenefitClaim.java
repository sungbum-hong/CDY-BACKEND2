package com.cdy.cdy.domain.benefit.dto;

import java.time.LocalDateTime;

public record ResponseMyBenefitClaim(
        Long claimId,
        Long benefitId,
        String title,
        String partnerName,
        LocalDateTime claimedAt
) {
}
