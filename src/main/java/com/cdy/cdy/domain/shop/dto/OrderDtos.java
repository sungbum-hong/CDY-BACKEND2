package com.cdy.cdy.domain.shop.dto;

import com.cdy.cdy.domain.shop.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public record OrderLine(Long productId, Integer quantity) {}

    /** 주문 생성 요청 */
    public record CreateRequest(
            List<OrderLine> items,
            String receiverName,
            String receiverPhone,
            String postcode,
            String address,
            String addressDetail,
            String deliveryMemo
    ) {}

    /** 주문 생성 응답 — 이 값으로 토스 결제위젯을 호출한다 */
    public record CreateResponse(
            Long orderId,
            String orderNumber,
            Integer totalAmount,
            String orderName
    ) {}

    /** 결제 승인 요청 (토스 리다이렉트 쿼리에서 받은 값) */
    public record ConfirmRequest(String paymentKey, Integer amount) {}

    public record Item(
            Long productId,
            String productName,
            String thumbnailUrl,
            Integer unitPrice,
            Integer quantity,
            Integer lineTotal
    ) {}

    /** 주문 목록 */
    public record ListItem(
            Long id,
            String orderNumber,
            OrderStatus status,
            Integer totalAmount,
            String summary,
            LocalDateTime createdAt
    ) {}

    /** 주문 상세 */
    public record Detail(
            Long id,
            String orderNumber,
            OrderStatus status,
            Integer totalAmount,
            String receiverName,
            String receiverPhone,
            String postcode,
            String address,
            String addressDetail,
            String deliveryMemo,
            List<Item> items,
            Boolean cancelable,
            LocalDateTime createdAt,
            LocalDateTime paidAt,
            LocalDateTime cancelledAt
    ) {}

    /** 어드민 주문 목록 */
    public record AdminItem(
            Long id,
            String orderNumber,
            OrderStatus status,
            Integer totalAmount,
            String buyerNickname,
            String receiverName,
            String receiverPhone,
            String address,
            String addressDetail,
            String summary,
            LocalDateTime createdAt,
            LocalDateTime paidAt
    ) {}

    public record StatusRequest(OrderStatus status) {}
}
