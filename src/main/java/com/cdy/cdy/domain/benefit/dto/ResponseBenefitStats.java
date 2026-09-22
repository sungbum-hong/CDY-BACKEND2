package com.cdy.cdy.domain.benefit.dto;

import java.time.LocalDateTime;

public record ResponseBenefitStats(
        Long benefitId,
        String title,
        String partnerName,
        Long claimCount,
        LocalDateTime lastClaimedAt
) {
}
