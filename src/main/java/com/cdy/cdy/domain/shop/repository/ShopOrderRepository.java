package com.cdy.cdy.domain.shop.repository;

import com.cdy.cdy.domain.shop.entity.OrderStatus;
import com.cdy.cdy.domain.shop.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @Query("""
            SELECT o
            FROM ShopOrder o
            WHERE o.user.id = :userId
            ORDER BY o.id DESC
            """)
    List<ShopOrder> findByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT o
            FROM ShopOrder o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.id = :id
            """)
    Optional<ShopOrder> findWithItemsById(@Param("id") Long id);

    /** 어드민 목록. status 가 null 이면 전체 */
    @Query("""
            SELECT o
            FROM ShopOrder o
            WHERE (:status IS NULL OR o.status = :status)
            ORDER BY o.id DESC
            """)
    List<ShopOrder> findAllForAdmin(@Param("status") OrderStatus status);
}
