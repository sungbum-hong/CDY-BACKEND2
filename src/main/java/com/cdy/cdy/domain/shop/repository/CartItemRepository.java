package com.cdy.cdy.domain.shop.repository;

import com.cdy.cdy.domain.shop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("""
            SELECT c
            FROM CartItem c
            JOIN FETCH c.product p
            WHERE c.user.id = :userId
            ORDER BY c.id DESC
            """)
    List<CartItem> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.product.id = :productId")
    Optional<CartItem> findByUserIdAndProductId(@Param("userId") Long userId,
                                                @Param("productId") Long productId);

    long countByUserId(Long userId);

    void deleteByUserIdAndProductIdIn(Long userId, List<Long> productIds);

    /** 상품 하드 삭제 전 FK 정리용 — 누군가 장바구니에 담아둔 상태여도 지울 수 있어야 한다 */
    void deleteByProductId(Long productId);
}
