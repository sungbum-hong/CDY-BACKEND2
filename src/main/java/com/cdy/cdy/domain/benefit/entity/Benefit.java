package com.cdy.cdy.domain.benefit.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import com.cdy.cdy.domain.partner.entity.Partner;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "benefits")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Benefit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    /** 예: "아메리카노 20% 할인" */
    @Column(name = "discount_text", length = 100)
    private String discountText;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 20)
    private BenefitType benefitType;

    /** CODE 타입일 때만 사용. 목록/상세에서는 내려주지 않고 claim 시에만 반환한다. */
    @Column(name = "code_value", length = 100)
    private String codeValue;

    /** LINK 타입일 때만 사용. 목록/상세에서는 내려주지 않고 claim 시에만 반환한다. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "usage_guide", length = 1000)
    private String usageGuide;

    /** R2 오브젝트 키. ImageUrlResolver 로 URL 변환해서 내려준다. */
    @Column(name = "image_key", length = 500)
    private String imageKey;

    /** null 이면 상시 */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /** null 이면 상시 */
    @Column(name = "valid_to")
    private LocalDate validTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BenefitStatus status = BenefitStatus.ACTIVE;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public void update(Partner partner,
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
                       Integer sortOrder) {
        this.partner = partner;
        this.title = title;
        this.description = description;
        this.discountText = discountText;
        this.benefitType = benefitType;
        this.codeValue = codeValue;
        this.linkUrl = linkUrl;
        this.usageGuide = usageGuide;
        this.validFrom = validFrom;
        this.validTo = validTo;
        // 이미지/상태/정렬은 값이 넘어온 경우에만 덮어쓴다 (부분 수정 허용)
        if (imageKey != null) this.imageKey = imageKey;
        if (status != null) this.status = status;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }

    /** 소프트 삭제 */
    public void hide() {
        this.status = BenefitStatus.HIDDEN;
    }

    public boolean isActiveOn(LocalDate date) {
        if (status != BenefitStatus.ACTIVE) return false;
        if (validFrom != null && date.isBefore(validFrom)) return false;
        if (validTo != null && date.isAfter(validTo)) return false;
        return true;
    }
}
