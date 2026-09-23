package com.cdy.cdy.domain.shop.repository;

import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;
import com.cdy.cdy.domain.shop.entity.Product;
import com.cdy.cdy.domain.shop.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 크루용 목록. 상품 HIDDEN 과 HIDDEN 업체의 상품은 제외하고 SOLD_OUT 은 포함한다(품절 표시용).
     * category(업체 카테고리) / partnerId / keyword 는 null 이면 조건에서 빠진다.
     * keyword 는 상품명과 업체명을 함께 본다.
     *
     * partners.status 가 NULL 인 레거시 행(혜택몰 이전에 등록된 홈 배너 파트너)은
     * 숨김이 아니므로 통과시킨다.
     */
    @Query("""
            SELECT p
            FROM Product p
            JOIN FETCH p.partner pt
            WHERE p.status <> :hidden
              AND (pt.status IS NULL OR pt.status <> :partnerHidden)
              AND (:category IS NULL OR pt.category = :category)
              AND (:partnerId IS NULL OR pt.id = :partnerId)
              AND (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(pt.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.sortOrder ASC, p.id DESC
            """)
    List<Product> findForCrew(@Param("hidden") ProductStatus hidden,
                              @Param("partnerHidden") PartnerStatus partnerHidden,
                              @Param("category") PartnerCategory category,
                              @Param("partnerId") Long partnerId,
                              @Param("keyword") String keyword);

    /**
     * 판매중(ACTIVE) + 업체가 숨김이 아닌 상품 전체.
     * maxDiscountRate / 오늘의 특가 / 제휴 업체 목록이 전부 이 결과 하나에서 파생된다
     * — 할인율을 DB에서 다시 계산하지 않기 위해서다.
     */
    @Query("""
            SELECT p
            FROM Product p
            JOIN FETCH p.partner pt
            WHERE p.status = :active
              AND (pt.status IS NULL OR pt.status <> :partnerHidden)
            ORDER BY p.sortOrder ASC, p.id DESC
            """)
    List<Product> findOnSale(@Param("active") ProductStatus active,
                             @Param("partnerHidden") PartnerStatus partnerHidden);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.images
            WHERE p.id = :id
            """)
    Optional<Product> findWithImagesById(@Param("id") Long id);

    /** 어드민 목록. HIDDEN 포함 전체 */
    @Query("SELECT p FROM Product p JOIN FETCH p.partner ORDER BY p.sortOrder ASC, p.id DESC")
    List<Product> findAllForAdmin();

    /** 결제 승인/취소 시 재고 갱신용 — 동시 주문으로 재고가 음수가 되지 않도록 잠금 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /** 파트너 삭제 차단용 */
    boolean existsByPartnerId(Long partnerId);
}
