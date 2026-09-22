package com.cdy.cdy.domain.benefit.dto;

import com.cdy.cdy.domain.benefit.entity.BenefitType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 혜택 발급 응답. benefitType 에 따라 채워지는 필드가 달라지며,
 * null 인 필드는 응답 JSON에서 제외된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseBenefitClaim(
        BenefitType benefitType,
        String codeValue,
        String linkUrl,
        String userNickname,
        LocalDateTime claimedAt
) {

    public static ResponseBenefitClaim code(String codeValue) {
        return new ResponseBenefitClaim(BenefitType.CODE, codeValue, null, null, null);
    }

    public static ResponseBenefitClaim link(String linkUrl) {
        return new ResponseBenefitClaim(BenefitType.LINK, null, linkUrl, null, null);
    }

    public static ResponseBenefitClaim show(String userNickname, LocalDateTime claimedAt) {
        return new ResponseBenefitClaim(BenefitType.SHOW, null, null, userNickname, claimedAt);
    }
}
