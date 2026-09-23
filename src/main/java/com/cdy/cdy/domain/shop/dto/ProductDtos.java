package com.cdy.cdy.domain.shop.dto;

import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.shop.entity.ProductStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 관련 요청/응답 DTO 모음.
 *
 * 상품은 제휴 업체(Partner)에 속하고, 화면에 노출되는 업체명/카테고리는
 * 전부 partner 에서 파생된다. supplierName/productCode 는 발주용 내부 정보라
 * 어드민 응답에만 담는다.
 */
public final class ProductDtos {

    private ProductDtos() {}

    /** 어드민 등록/수정 요청. bulk 등록에도 그대로 쓴다 */
    public record Request(
            String name,
            Long partnerId,
            Integer originalPrice,
            Integer crewPrice,
            Integer stock,
            String supplierName,
            String productCode,
            String thumbnailKey,
            List<String> imageKeys,
            String description,
            ProductStatus status,
            Integer sortOrder
    ) {}

    /** 크루 목록 항목 */
    public record ListItem(
            Long id,
            String name,
            Long partnerId,
            String partnerName,
            PartnerCategory partnerCategory,
            Integer originalPrice,
            Integer crewPrice,
            Integer discountRate,
            Integer stock,
            ProductStatus status,
            String thumbnailUrl
    ) {}

    /**
     * 크루 목록 응답.
     * maxDiscountRate 는 필터와 무관하게 "현재 판매중 상품 전체의 최고 할인율" —
     * 배너의 "최대 N%" 문구용이라 검색어를 바꿔도 흔들리면 안 된다. 상품이 없으면 0.
     */
    public record ListResponse(
            List<ListItem> items,
            int maxDiscountRate
    ) {}

    /** 크루 상세 */
    public record Detail(
            Long id,
            String name,
            Long partnerId,
            String partnerName,
            PartnerCategory partnerCategory,
            String partnerLogoUrl,
            Integer originalPrice,
            Integer crewPrice,
            Integer discountRate,
            Integer stock,
            ProductStatus status,
            String thumbnailUrl,
            List<String> imageUrls,
            String description
    ) {}

    /** 제휴 업체 칩/목록용. 판매중 상품이 1개 이상인 업체만 내려간다 */
    public record PartnerItem(
            Long id,
            String name,
            PartnerCategory category,
            String logoImageUrl,
            long productCount
    ) {}

    /** 어드민 목록/수정 폼용. HIDDEN 포함, imageKey 원본까지 내려준다 */
    public record AdminItem(
            Long id,
            String name,
            Long partnerId,
            String partnerName,
            PartnerCategory partnerCategory,
            Integer originalPrice,
            Integer crewPrice,
            Integer discountRate,
            Integer stock,
            String supplierName,
            String productCode,
            String thumbnailKey,
            String thumbnailUrl,
            List<String> imageKeys,
            List<String> imageUrls,
            String description,
            ProductStatus status,
            Integer sortOrder,
            LocalDateTime createdAt
    ) {}

    /** bulk 등록 결과 */
    public record BulkResult(int created, List<String> errors) {}
}
