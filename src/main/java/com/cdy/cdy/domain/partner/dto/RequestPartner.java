package com.cdy.cdy.domain.partner.dto;

import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RequestPartner {
    // 홈 화면 파트너 배너용 (기존)
    private String name;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;

    // 혜택몰 제휴사용 (추가)
    private PartnerCategory category;
    private String description;
    private String logoImageKey;
    private String websiteUrl;
    private PartnerStatus status;
}
