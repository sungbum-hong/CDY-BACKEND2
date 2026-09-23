package com.cdy.cdy.domain.shop.entity;

public enum OrderStatus {

    /** 주문 생성, 결제 전 */
    PENDING,
    /** 결제 완료 */
    PAID,
    /** 상품 준비중 */
    PREPARING,
    /** 배송중 */
    SHIPPING,
    /** 배송완료 */
    DELIVERED,
    /** 취소 */
    CANCELLED;

    /** 크루가 직접 취소할 수 있는 단계인지 (PAID 까지만) */
    public boolean isCancelable() {
        return this == PENDING || this == PAID;
    }
}
