package com.cdy.cdy.domain.shop.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import com.cdy.cdy.domain.partner.entity.Partner;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 제휴 업체. 상품은 반드시 한 업체에 속한다.
     * 화면에 노출되는 업체명·카테고리는 전부 여기서 파생된다(상품 자체 카테고리 컬럼 없음).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    /** 정가 */
    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    /** 크루 전용가 */
    @Column(name = "crew_price", nullable = false)
    private Integer crewPrice;

    @Builder.Default
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    /** 발주용 내부 정보. 크루 화면에는 노출하지 않는다 (노출용 업체명은 partner.name) */
    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    /** 발주용 내부 정보(업체 측 상품코드). 어드민에게만 보인다 */
    @Column(name = "product_code", length = 100)
    private String productCode;

    /** R2 오브젝트 키 */
    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    /**
     * 할인율(%) = round((1 - crewPrice / originalPrice) * 100).
     * 정가가 0이거나 크루가가 더 비싸면 0.
     *
     * 목록·상세·오늘의 특가·maxDiscountRate 가 전부 이 메서드 하나만 쓴다.
     * 다른 곳에서 다시 계산하지 말 것 (DB 쿼리로 계산하면 반올림이 갈린다).
     */
    public int discountRate() {
        if (originalPrice == null || originalPrice <= 0 || crewPrice == null) return 0;
        if (crewPrice >= originalPrice) return 0;
        return (int) Math.round((originalPrice - crewPrice) * 100.0 / originalPrice);
    }

    /** 상품 카테고리는 소속 업체에서 파생된다 */
    public PartnerCategory categoryOfPartner() {
        return partner == null ? null : partner.getCategory();
    }

    public boolean isPurchasable() {
        return status == ProductStatus.ACTIVE && stock != null && stock > 0;
    }

    public void update(String name,
                       Partner partner,
                       Integer originalPrice,
                       Integer crewPrice,
                       Integer stock,
                       String supplierName,
                       String productCode,
                       String thumbnailKey,
                       String description,
                       ProductStatus status,
                       Integer sortOrder) {
        this.name = name;
        this.partner = partner;
        this.originalPrice = originalPrice;
        this.crewPrice = crewPrice;
        this.supplierName = supplierName;
        this.productCode = productCode;
        this.description = description;
        // 값이 넘어온 경우에만 덮어쓴다 (부분 수정 허용)
        if (stock != null) this.stock = stock;
        if (thumbnailKey != null) this.thumbnailKey = thumbnailKey;
        if (status != null) this.status = status;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }

    /** 상세 이미지 전체 교체 */
    public void replaceImages(List<String> imageKeys) {
        this.images.clear();
        if (imageKeys == null) return;
        for (int i = 0; i < imageKeys.size(); i++) {
            String key = imageKeys.get(i);
            if (key == null || key.isBlank()) continue;
            this.images.add(ProductImage.builder()
                    .product(this)
                    .imageKey(key)
                    .sortOrder(i)
                    .build());
        }
    }

    /** 소프트 삭제 */
    public void hide() {
        this.status = ProductStatus.HIDDEN;
    }

    /** 결제 승인 시 재고 차감. 부족하면 예외 */
    public void decreaseStock(int quantity) {
        if (stock == null || stock < quantity) {
            throw new IllegalStateException("재고가 부족합니다: " + name);
        }
        this.stock -= quantity;
        if (this.stock == 0 && this.status == ProductStatus.ACTIVE) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    /** 주문 취소 시 재고 복구 */
    public void restoreStock(int quantity) {
        this.stock = (stock == null ? 0 : stock) + quantity;
        if (this.status == ProductStatus.SOLD_OUT && this.stock > 0) {
            this.status = ProductStatus.ACTIVE;
        }
    }
}
