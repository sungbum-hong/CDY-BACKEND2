package com.cdy.cdy.domain.shop.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 주문 상품. 상품명·단가는 주문 시점 값을 스냅샷으로 보관한다
 * (나중에 상품이 수정·삭제돼도 주문 내역이 바뀌면 안 되므로).
 */
@Entity
@Table(name = "shop_order_items")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** 주문 시점 크루가 */
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public int lineTotal() {
        return unitPrice * quantity;
    }
}
