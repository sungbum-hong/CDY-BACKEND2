package com.cdy.cdy.domain.shop.repository;

import com.cdy.cdy.domain.shop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 상품 하드 삭제 가능 여부 판단용.
     * 주문 상품은 상품명·단가를 스냅샷으로 들고 있지만 product_id FK 도 함께 갖고 있어서,
     * 주문된 적 있는 상품을 지우면 주문 내역이 깨진다.
     */
    boolean existsByProductId(Long productId);
}
