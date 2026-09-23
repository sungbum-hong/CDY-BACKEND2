package com.cdy.cdy.domain.shop.controller;

import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.shop.dto.CartDtos;
import com.cdy.cdy.domain.shop.dto.OrderDtos;
import com.cdy.cdy.domain.shop.dto.ProductDtos;
import com.cdy.cdy.domain.shop.service.CartService;
import com.cdy.cdy.domain.shop.service.OrderService;
import com.cdy.cdy.domain.shop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 크루 전용 쇼핑몰. 모든 엔드포인트가 로그인 필수.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;

    // ---------- 상품 ----------

    @Operation(summary = "상품 목록 (크루 전용)",
            description = "category: 제휴 업체 카테고리(CAFE/FOOD/EDU/TOOL/ETC), partnerId: 업체 필터, "
                    + "keyword: 상품명+업체명 부분 검색. 응답의 maxDiscountRate 는 필터와 무관한 전체 최고 할인율")
    @GetMapping("/products")
    public ResponseEntity<ProductDtos.ListResponse> products(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "partnerId", required = false) Long partnerId,
            @RequestParam(name = "keyword", required = false) String keyword) {

        return ResponseEntity.ok(productService.findForCrew(parseCategory(category), partnerId, keyword));
    }

    @Operation(summary = "제휴 업체 목록 (크루 전용)",
            description = "판매중 상품이 1개 이상인 업체만. 숨김 업체는 제외")
    @GetMapping("/partners")
    public ResponseEntity<List<ProductDtos.PartnerItem>> partners() {
        return ResponseEntity.ok(productService.findPartnersWithProducts());
    }

    @Operation(summary = "오늘의 크루 특가 (크루 전용)",
            description = "판매중 상품 중 할인율 상위 6개. 동률이면 최신 등록 순")
    @GetMapping("/deals/today")
    public ResponseEntity<List<ProductDtos.ListItem>> todayDeals() {
        return ResponseEntity.ok(productService.findTodayDeals());
    }

    @Operation(summary = "상품 상세 (크루 전용)")
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDtos.Detail> product(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findDetail(id));
    }

    // ---------- 장바구니 ----------

    @Operation(summary = "장바구니 조회")
    @GetMapping("/cart")
    public ResponseEntity<CartDtos.Response> cart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }

    @Operation(summary = "장바구니 담긴 개수 (헤더 뱃지용)")
    @GetMapping("/cart/count")
    public ResponseEntity<Map<String, Long>> cartCount(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", cartService.countItems(authentication.getName())));
    }

    @Operation(summary = "장바구니 담기")
    @PostMapping("/cart")
    public ResponseEntity<?> addToCart(Authentication authentication,
                                       @RequestBody CartDtos.AddRequest dto) {
        cartService.addItem(authentication.getName(), dto);
        return ResponseEntity.ok("장바구니에 담았습니다.");
    }

    @Operation(summary = "장바구니 수량 변경")
    @PutMapping("/cart/{cartItemId}")
    public ResponseEntity<?> changeQuantity(Authentication authentication,
                                            @PathVariable Long cartItemId,
                                            @RequestBody CartDtos.QuantityRequest dto) {
        cartService.changeQuantity(authentication.getName(), cartItemId, dto);
        return ResponseEntity.ok("수량이 변경됐습니다.");
    }

    @Operation(summary = "장바구니 항목 삭제")
    @DeleteMapping("/cart/{cartItemId}")
    public ResponseEntity<?> removeFromCart(Authentication authentication,
                                            @PathVariable Long cartItemId) {
        cartService.removeItem(authentication.getName(), cartItemId);
        return ResponseEntity.ok("삭제됐습니다.");
    }

    // ---------- 주문 / 결제 ----------

    @Operation(summary = "주문 생성 (결제 전)",
            description = "응답의 orderNumber/totalAmount 로 토스 결제위젯을 호출한다. 금액은 서버가 계산한다.")
    @PostMapping("/orders")
    public ResponseEntity<OrderDtos.CreateResponse> createOrder(Authentication authentication,
                                                                @RequestBody OrderDtos.CreateRequest dto) {
        log.info("[ShopController] 주문 생성 요청 - username: {}", authentication.getName());
        return ResponseEntity.ok(orderService.create(authentication.getName(), dto));
    }

    @Operation(summary = "결제 승인",
            description = "토스 리다이렉트로 받은 paymentKey/amount 를 넘기면 서버가 승인하고 재고를 차감한다.")
    @PostMapping("/orders/{id}/confirm-payment")
    public ResponseEntity<OrderDtos.Detail> confirmPayment(Authentication authentication,
                                                           @PathVariable Long id,
                                                           @RequestBody OrderDtos.ConfirmRequest dto) {
        return ResponseEntity.ok(orderService.confirmPayment(authentication.getName(), id, dto));
    }

    @Operation(summary = "내 주문 목록")
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDtos.ListItem>> orders(Authentication authentication) {
        return ResponseEntity.ok(orderService.findMyOrders(authentication.getName()));
    }

    @Operation(summary = "내 주문 상세")
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDtos.Detail> order(Authentication authentication,
                                                  @PathVariable Long id) {
        return ResponseEntity.ok(orderService.findMyOrderDetail(authentication.getName(), id));
    }

    @Operation(summary = "주문 취소", description = "PAID 단계까지만 가능")
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(Authentication authentication, @PathVariable Long id) {
        orderService.cancel(authentication.getName(), id);
        return ResponseEntity.ok("주문이 취소됐습니다.");
    }

    private PartnerCategory parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try {
            return PartnerCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("category는 CAFE, FOOD, EDU, TOOL, ETC 중 하나여야 합니다.");
        }
    }
}
