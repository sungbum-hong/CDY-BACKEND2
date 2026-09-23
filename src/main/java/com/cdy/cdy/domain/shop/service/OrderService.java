package com.cdy.cdy.domain.shop.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.shop.dto.OrderDtos;
import com.cdy.cdy.domain.shop.entity.*;
import com.cdy.cdy.domain.shop.repository.CartItemRepository;
import com.cdy.cdy.domain.shop.repository.ProductRepository;
import com.cdy.cdy.domain.shop.repository.ShopOrderRepository;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ShopOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;
    private final TossPaymentClient tossPaymentClient;

    // ================= 주문 생성 =================

    /**
     * 결제 전 주문(PENDING) 생성. 금액은 서버가 계산한다 —
     * 프론트가 보낸 금액을 믿으면 위변조가 가능하다.
     */
    @Transactional
    public OrderDtos.CreateResponse create(String username, OrderDtos.CreateRequest dto) {

        Users user = findUser(username);
        validateShipping(dto);

        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalArgumentException("주문할 상품이 없습니다.");
        }

        ShopOrder order = ShopOrder.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(0)
                .receiverName(dto.receiverName().trim())
                .receiverPhone(dto.receiverPhone().trim())
                .postcode(dto.postcode())
                .address(dto.address().trim())
                .addressDetail(dto.addressDetail())
                .deliveryMemo(dto.deliveryMemo())
                .build();

        int total = 0;
        String firstName = null;

        for (OrderDtos.OrderLine line : dto.items()) {

            if (line.productId() == null) throw new IllegalArgumentException("상품 정보가 올바르지 않습니다.");
            int quantity = line.quantity() == null ? 0 : line.quantity();
            if (quantity < 1) throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");

            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품"));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new IllegalStateException("판매 중이 아닌 상품이 포함돼 있습니다: " + product.getName());
            }
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new IllegalStateException("재고가 부족합니다: " + product.getName()
                        + " (남은 수량 " + product.getStock() + "개)");
            }

            order.addItem(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getCrewPrice())
                    .quantity(quantity)
                    .build());

            total += product.getCrewPrice() * quantity;
            if (firstName == null) firstName = product.getName();
        }

        order.applyTotalAmount(total);
        ShopOrder saved = orderRepository.save(order);   // items 는 cascade 로 함께 저장

        log.info("[OrderService] 주문 생성 - orderNumber: {}, amount: {}", saved.getOrderNumber(), total);

        return new OrderDtos.CreateResponse(
                saved.getId(),
                saved.getOrderNumber(),
                total,
                orderName(firstName, saved.getItems().size()));
    }

    // ================= 결제 승인 =================

    /**
     * 토스 승인 → 재고 차감 → 장바구니 정리.
     * 금액은 DB에 저장된 값과 대조해서 위변조를 막는다.
     */
    @Transactional
    public OrderDtos.Detail confirmPayment(String username, Long orderId, OrderDtos.ConfirmRequest dto) {

        Users user = findUser(username);
        ShopOrder order = findOwnedOrder(user, orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("취소된 주문입니다.");
        }
        // 이미 승인된 주문이면 재승인하지 않고 그대로 돌려준다 (새로고침 대비)
        if (order.getStatus() != OrderStatus.PENDING) {
            return toDetail(order);
        }
        if (dto.paymentKey() == null || dto.paymentKey().isBlank()) {
            throw new IllegalArgumentException("결제 정보가 올바르지 않습니다.");
        }
        if (dto.amount() == null || !dto.amount().equals(order.getTotalAmount())) {
            throw new IllegalStateException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        Map<String, Object> result =
                tossPaymentClient.confirm(dto.paymentKey(), order.getOrderNumber(), order.getTotalAmount());

        // 재고 차감 (행 잠금으로 동시 주문 시 음수 방지)
        for (OrderItem item : order.getItems()) {
            Product locked = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품"));
            locked.decreaseStock(item.getQuantity());
        }

        order.markPaid(dto.paymentKey(), LocalDateTime.now(KST));

        // 주문한 상품은 장바구니에서 제거
        List<Long> productIds = order.getItems().stream().map(i -> i.getProduct().getId()).toList();
        cartItemRepository.deleteByUserIdAndProductIdIn(user.getId(), productIds);

        log.info("[OrderService] 결제 승인 완료 - orderNumber: {}, tossStatus: {}",
                order.getOrderNumber(), result == null ? "-" : result.get("status"));

        return toDetail(order);
    }

    // ================= 조회 / 취소 =================

    @Transactional(readOnly = true)
    public List<OrderDtos.ListItem> findMyOrders(String username) {

        Users user = findUser(username);

        return orderRepository.findByUserId(user.getId()).stream()
                .map(o -> new OrderDtos.ListItem(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getStatus(),
                        o.getTotalAmount(),
                        summaryOf(o),
                        o.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.Detail findMyOrderDetail(String username, Long orderId) {
        return toDetail(findOwnedOrder(findUser(username), orderId));
    }

    @Transactional
    public void cancel(String username, Long orderId) {

        ShopOrder order = findOwnedOrder(findUser(username), orderId);

        if (!order.getStatus().isCancelable()) {
            throw new IllegalStateException("이미 상품 준비가 시작되어 취소할 수 없습니다. 고객센터로 문의해주세요.");
        }

        // 결제까지 끝난 주문이면 차감했던 재고를 되돌린다
        if (order.getStatus() == OrderStatus.PAID) {
            for (OrderItem item : order.getItems()) {
                productRepository.findByIdForUpdate(item.getProduct().getId())
                        .ifPresent(p -> p.restoreStock(item.getQuantity()));
            }
        }

        order.cancel(LocalDateTime.now(KST));
        log.info("[OrderService] 주문 취소 - orderNumber: {}", order.getOrderNumber());
    }

    // ================= 어드민 =================

    @Transactional(readOnly = true)
    public List<OrderDtos.AdminItem> findAllForAdmin(OrderStatus status) {

        return orderRepository.findAllForAdmin(status).stream()
                .map(o -> new OrderDtos.AdminItem(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getStatus(),
                        o.getTotalAmount(),
                        o.getUser().getNickname(),
                        o.getReceiverName(),
                        o.getReceiverPhone(),
                        o.getAddress(),
                        o.getAddressDetail(),
                        summaryOf(o),
                        o.getCreatedAt(),
                        o.getPaidAt()))
                .toList();
    }

    @Transactional
    public void changeStatus(Long orderId, OrderDtos.StatusRequest dto) {

        if (dto.status() == null) throw new IllegalArgumentException("변경할 상태가 필요합니다.");

        ShopOrder order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 주문"));

        if (order.getStatus() == OrderStatus.PENDING) {
            throw new IllegalStateException("결제가 완료되지 않은 주문입니다.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("취소된 주문은 상태를 변경할 수 없습니다.");
        }

        order.changeStatus(dto.status());
    }

    // ================= 내부 =================

    private Users findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));
    }

    /** 남의 주문을 조회·취소하지 못하게 소유자 확인 */
    private ShopOrder findOwnedOrder(Users user, Long orderId) {
        ShopOrder order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 주문"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new EntityNotFoundException("존재하지 않는 주문");
        }
        return order;
    }

    private void validateShipping(OrderDtos.CreateRequest dto) {
        if (dto.receiverName() == null || dto.receiverName().isBlank())
            throw new IllegalArgumentException("받는 분 이름은 필수입니다.");
        if (dto.receiverPhone() == null || dto.receiverPhone().isBlank())
            throw new IllegalArgumentException("연락처는 필수입니다.");
        if (dto.address() == null || dto.address().isBlank())
            throw new IllegalArgumentException("주소는 필수입니다.");
    }

    /** CDY + 시각 + 랜덤 6자리. 토스 orderId 규격(6~64자)을 만족한다 */
    private String generateOrderNumber() {
        for (int i = 0; i < 5; i++) {
            String candidate = "CDY" + LocalDateTime.now(KST).format(ORDER_NO_FMT)
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            if (!orderRepository.existsByOrderNumber(candidate)) return candidate;
        }
        throw new IllegalStateException("주문번호 생성에 실패했습니다. 다시 시도해주세요.");
    }

    private String orderName(String firstName, int itemCount) {
        if (firstName == null) return "코디영 크루 쇼핑";
        return itemCount > 1 ? firstName + " 외 " + (itemCount - 1) + "건" : firstName;
    }

    private String summaryOf(ShopOrder o) {
        List<OrderItem> items = o.getItems();
        if (items.isEmpty()) return "";
        String first = items.get(0).getProductName();
        return items.size() > 1 ? first + " 외 " + (items.size() - 1) + "건" : first;
    }

    private OrderDtos.Detail toDetail(ShopOrder o) {
        return new OrderDtos.Detail(
                o.getId(),
                o.getOrderNumber(),
                o.getStatus(),
                o.getTotalAmount(),
                o.getReceiverName(),
                o.getReceiverPhone(),
                o.getPostcode(),
                o.getAddress(),
                o.getAddressDetail(),
                o.getDeliveryMemo(),
                o.getItems().stream()
                        .map(i -> new OrderDtos.Item(
                                i.getProduct().getId(),
                                i.getProductName(),
                                imageUrlResolver.toPresignedUrl(i.getProduct().getThumbnailKey()),
                                i.getUnitPrice(),
                                i.getQuantity(),
                                i.lineTotal()))
                        .toList(),
                o.getStatus().isCancelable(),
                o.getCreatedAt(),
                o.getPaidAt(),
                o.getCancelledAt());
    }
}
