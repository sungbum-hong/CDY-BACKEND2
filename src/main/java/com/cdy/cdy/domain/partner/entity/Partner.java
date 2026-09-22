package com.cdy.cdy.domain.partner.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partners")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Partner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // ---- 혜택몰(폐쇄몰) 제휴사 정보 ----

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private PartnerCategory category;

    @Column(name = "description", length = 500)
    private String description;

    /** R2 오브젝트 키. ImageUrlResolver 로 URL 변환해서 내려준다. */
    @Column(name = "logo_image_key", length = 500)
    private String logoImageKey;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private PartnerStatus status = PartnerStatus.ACTIVE;

    public void update(String name,
                       PartnerCategory category,
                       String description,
                       String logoImageKey,
                       String websiteUrl,
                       PartnerStatus status,
                       String imageUrl,
                       String linkUrl,
                       Integer sortOrder) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.websiteUrl = websiteUrl;
        this.linkUrl = linkUrl;
        // 이미지/상태/정렬은 값이 넘어온 경우에만 덮어쓴다 (부분 수정 허용)
        if (logoImageKey != null) this.logoImageKey = logoImageKey;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (status != null) this.status = status;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }
}
