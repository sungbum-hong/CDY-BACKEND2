package com.cdy.cdy.domain.shop.entity;

public enum ProductStatus {

    /** 판매 중 */
    ACTIVE,
    /** 숨김 (소프트 삭제) */
    HIDDEN,
    /** 품절 — 목록에는 보이되 구매 불가 */
    SOLD_OUT
}
