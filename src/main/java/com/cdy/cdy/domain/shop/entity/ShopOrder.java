package com.cdy.cdy.domain.shop.entity;

import com.cdy.cdy.common.entity.BaseEntity;
import com.cdy.cdy.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문. 테이블명은 MySQL 예약어(ORDER) 회피를 위해 shop_orders 를 쓴다.
 */
@Entity
@Table(name = "shop_orders")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
public class ShopOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토스에 넘기는 주문번호 (orderId). 사용자에게도 노출 */
    @Column(name = "order_number", nullable = false, unique = true, length = 64)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    // ---- 배송지 ----
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "postcode", length = 10)
    private String postcode;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "delivery_memo", length = 200)
    private String deliveryMemo;

    // ---- 결제 ----
    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    /** 서버가 계산한 합계를 반영 (프론트가 보낸 금액은 신뢰하지 않는다) */
    public void applyTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void markPaid(String paymentKey, LocalDateTime paidAt) {
        this.paymentKey = paymentKey;
        this.paidAt = paidAt;
        this.status = OrderStatus.PAID;
    }

    public void cancel(LocalDateTime cancelledAt) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }
}
