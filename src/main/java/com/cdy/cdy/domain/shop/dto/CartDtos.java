package com.cdy.cdy.domain.shop.dto;

import com.cdy.cdy.domain.shop.entity.ProductStatus;

import java.util.List;

public final class CartDtos {

    private CartDtos() {}

    public record AddRequest(Long productId, Integer quantity) {}

    public record QuantityRequest(Integer quantity) {}

    public record Item(
            Long cartItemId,
            Long productId,
            String name,
            String thumbnailUrl,
            Integer originalPrice,
            Integer crewPrice,
            Integer quantity,
            Integer lineTotal,
            Integer stock,
            ProductStatus status,
            /** 품절·숨김·재고부족이면 false — 주문 시 제외해야 한다 */
            Boolean purchasable
    ) {}

    public record Response(List<Item> items, Integer totalCount, Integer totalAmount) {}
}
