package com.cdy.cdy.domain.shop.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.shop.dto.CartDtos;
import com.cdy.cdy.domain.shop.entity.CartItem;
import com.cdy.cdy.domain.shop.entity.Product;
import com.cdy.cdy.domain.shop.entity.ProductStatus;
import com.cdy.cdy.domain.shop.repository.CartItemRepository;
import com.cdy.cdy.domain.shop.repository.ProductRepository;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public CartDtos.Response getCart(String username) {

        Long userId = findUser(username).getId();
        List<CartItem> items = cartItemRepository.findByUserId(userId);

        List<CartDtos.Item> mapped = items.stream().map(c -> {
            Product p = c.getProduct();
            boolean purchasable = p.getStatus() == ProductStatus.ACTIVE
                    && p.getStock() != null && p.getStock() >= c.getQuantity();
            return new CartDtos.Item(
                    c.getId(),
                    p.getId(),
                    p.getName(),
                    imageUrlResolver.toPresignedUrl(p.getThumbnailKey()),
                    p.getOriginalPrice(),
                    p.getCrewPrice(),
                    c.getQuantity(),
                    p.getCrewPrice() * c.getQuantity(),
                    p.getStock(),
                    p.getStatus(),
                    purchasable);
        }).toList();

        int totalAmount = mapped.stream()
                .filter(CartDtos.Item::purchasable)
                .mapToInt(CartDtos.Item::lineTotal)
                .sum();

        return new CartDtos.Response(mapped, mapped.size(), totalAmount);
    }

    /** 헤더 뱃지용 담긴 종류 수 */
    @Transactional(readOnly = true)
    public long countItems(String username) {
        return cartItemRepository.countByUserId(findUser(username).getId());
    }

    @Transactional
    public void addItem(String username, CartDtos.AddRequest dto) {

        if (dto.productId() == null) throw new IllegalArgumentException("상품을 선택해주세요.");
        int quantity = dto.quantity() == null ? 1 : dto.quantity();
        if (quantity < 1) throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");

        Users user = findUser(username);
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("현재 구매할 수 없는 상품입니다.");
        }

        cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .ifPresentOrElse(
                        existing -> {
                            int merged = existing.getQuantity() + quantity;
                            if (product.getStock() != null && merged > product.getStock()) {
                                throw new IllegalStateException("재고보다 많이 담을 수 없습니다. (재고 " + product.getStock() + "개)");
                            }
                            existing.changeQuantity(merged);
                        },
                        () -> {
                            if (product.getStock() != null && quantity > product.getStock()) {
                                throw new IllegalStateException("재고보다 많이 담을 수 없습니다. (재고 " + product.getStock() + "개)");
                            }
                            cartItemRepository.save(CartItem.builder()
                                    .user(user)
                                    .product(product)
                                    .quantity(quantity)
                                    .build());
                        });
    }

    @Transactional
    public void changeQuantity(String username, Long cartItemId, CartDtos.QuantityRequest dto) {

        if (dto.quantity() == null) throw new IllegalArgumentException("수량이 필요합니다.");

        CartItem item = findOwnedItem(username, cartItemId);
        Product product = item.getProduct();

        if (product.getStock() != null && dto.quantity() > product.getStock()) {
            throw new IllegalStateException("재고보다 많이 담을 수 없습니다. (재고 " + product.getStock() + "개)");
        }

        item.changeQuantity(dto.quantity());
    }

    @Transactional
    public void removeItem(String username, Long cartItemId) {
        cartItemRepository.delete(findOwnedItem(username, cartItemId));
    }

    // ================= 내부 =================

    private Users findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));
    }

    /** 남의 장바구니 항목을 건드리지 못하게 소유자 확인 */
    private CartItem findOwnedItem(String username, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 장바구니 항목"));
        if (!item.getUser().getId().equals(findUser(username).getId())) {
            throw new EntityNotFoundException("존재하지 않는 장바구니 항목");
        }
        return item;
    }
}
