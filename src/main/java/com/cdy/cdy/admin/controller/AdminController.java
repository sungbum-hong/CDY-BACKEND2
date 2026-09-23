package com.cdy.cdy.admin.controller;

import com.cdy.cdy.admin.dto.RequestChangePassword;
import com.cdy.cdy.admin.dto.RequestPromoteAdmin;
import com.cdy.cdy.admin.dto.ResponseAdminStudy;
import com.cdy.cdy.admin.dto.ResponseUserList;
import com.cdy.cdy.admin.service.AdminService;
import com.cdy.cdy.domain.apply.dto.RequestApprove;
import com.cdy.cdy.domain.apply.dto.ResponseApplication;
import com.cdy.cdy.domain.apply.service.ApplicationService;
import com.cdy.cdy.domain.benefit.dto.RequestBenefit;
import com.cdy.cdy.domain.benefit.dto.ResponseAdminBenefit;
import com.cdy.cdy.domain.benefit.dto.ResponseBenefitStats;
import com.cdy.cdy.domain.benefit.service.BenefitService;
import com.cdy.cdy.domain.contest.dto.RequestContest;
import com.cdy.cdy.domain.contest.service.ContestService;
import com.cdy.cdy.domain.partner.dto.RequestPartner;
import com.cdy.cdy.domain.partner.dto.ResponseAdminPartner;
import com.cdy.cdy.domain.partner.service.PartnerService;
import com.cdy.cdy.domain.shop.dto.OrderDtos;
import com.cdy.cdy.domain.shop.dto.ProductDtos;
import com.cdy.cdy.domain.shop.entity.OrderStatus;
import com.cdy.cdy.domain.shop.service.OrderService;
import com.cdy.cdy.domain.shop.service.ProductService;
import com.cdy.cdy.domain.users.dto.UserRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ApplicationService applicationService;
    private final ContestService contestService;
    private final PartnerService partnerService;
    private final BenefitService benefitService;
    private final ProductService productService;
    private final OrderService orderService;

    @Operation(summary = "어드민이 신규 유저 등록")
    @PostMapping("/createUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(Authentication authentication, @RequestBody UserRequestDto dto) {
        log.info("[Admin] 유저 생성 요청 - admin: {}, targetUsername: {}", authentication.getName(), dto.getUsername());
        adminService.createUser(authentication.getName(), dto);
        return ResponseEntity.ok("회원가입 완료");
    }

    @Operation(summary = "전체 유저 목록 조회")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResponseUserList>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @Operation(summary = "유저 비밀번호 변경")
    @PutMapping("/users/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@PathVariable Long userId,
                                            @RequestBody RequestChangePassword dto) {
        adminService.changePassword(userId, dto);
        return ResponseEntity.ok("비밀번호가 변경됐습니다.");
    }

    @Operation(summary = "유저 권한 변경 (ADMIN/USER)")
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeRole(@PathVariable Long userId,
                                        @RequestBody java.util.Map<String, String> body) {
        adminService.changeRole(userId, body.get("role"));
        return ResponseEntity.ok("권한이 변경됐습니다.");
    }

    @Operation(summary = "유저 삭제 (soft delete)")
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok("유저가 삭제됐습니다.");
    }

    @Operation(summary = "최초 어드민 계정 승격 (부트스트랩)")
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestBody RequestPromoteAdmin dto) {
        adminService.promoteToAdmin(dto);
        return ResponseEntity.ok("ADMIN 권한이 부여됐습니다.");
    }

    @Operation(summary = "전체 스터디 목록 조회 (어드민)")
    @GetMapping("/studies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResponseAdminStudy>> getStudies() {
        return ResponseEntity.ok(adminService.getStudies());
    }

    @Operation(summary = "스터디 삭제 (어드민, soft delete)")
    @DeleteMapping("/studies/{studyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStudy(@PathVariable Long studyId) {
        adminService.deleteStudy(studyId);
        return ResponseEntity.ok("스터디가 삭제됐습니다.");
    }

    @Operation(summary = "크루원 신청 목록 조회")
    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<ResponseApplication>> getApplications() {
        return ResponseEntity.ok(applicationService.getApplications());
    }

    @Operation(summary = "크루원 신청 승인", description = "body: name, email, nickname, phone, password → users 테이블에 계정 생성")
    @PostMapping("/applications/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveApplication(@PathVariable Long id,
                                                @RequestBody RequestApprove dto) {
        applicationService.approve(id, dto);
        return ResponseEntity.ok("승인됐습니다.");
    }

    @Operation(summary = "크루원 신청 거절")
    @PostMapping("/applications/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectApplication(@PathVariable Long id) {
        applicationService.reject(id);
        return ResponseEntity.ok("거절됐습니다.");
    }

    @Operation(summary = "크루원 신청 삭제")
    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.ok("삭제됐습니다.");
    }

    @Operation(summary = "공모전 등록 (어드민)")
    @PostMapping("/contests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createContest(@RequestBody RequestContest dto) {
        contestService.create(dto);
        return ResponseEntity.ok("공모전이 등록됐습니다.");
    }

    @Operation(summary = "공모전 수정 (어드민)")
    @PutMapping("/contests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateContest(@PathVariable Long id, @RequestBody RequestContest dto) {
        contestService.update(id, dto);
        return ResponseEntity.ok("공모전이 수정됐습니다.");
    }

    @Operation(summary = "공모전 삭제 (어드민, soft delete)")
    @DeleteMapping("/contests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteContest(@PathVariable Long id) {
        contestService.delete(id);
        return ResponseEntity.ok("공모전이 삭제됐습니다.");
    }

    @Operation(summary = "파트너 등록 (어드민)")
    @PostMapping("/partners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPartner(@RequestBody RequestPartner dto) {
        partnerService.create(dto);
        return ResponseEntity.ok("파트너가 등록됐습니다.");
    }

    @Operation(summary = "파트너 수정 (어드민)")
    @PutMapping("/partners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePartner(@PathVariable Long id, @RequestBody RequestPartner dto) {
        partnerService.update(id, dto);
        return ResponseEntity.ok("파트너가 수정됐습니다.");
    }

    @Operation(summary = "파트너 전체 목록 조회 (어드민)", description = "HIDDEN 포함")
    @GetMapping("/partners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResponseAdminPartner>> getPartners() {
        return ResponseEntity.ok(partnerService.findAllForAdmin());
    }

    @Operation(summary = "파트너 삭제 (어드민)")
    @DeleteMapping("/partners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePartner(@PathVariable Long id) {
        partnerService.delete(id);
        return ResponseEntity.ok("파트너가 삭제됐습니다.");
    }

    @Operation(summary = "혜택 등록 (어드민)")
    @PostMapping("/benefits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBenefit(@RequestBody RequestBenefit dto) {
        benefitService.create(dto);
        return ResponseEntity.ok("혜택이 등록됐습니다.");
    }

    @Operation(summary = "혜택 전체 목록 조회 (어드민)", description = "HIDDEN·기간만료 포함")
    @GetMapping("/benefits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResponseAdminBenefit>> getBenefits() {
        return ResponseEntity.ok(benefitService.findAllForAdmin());
    }

    @Operation(summary = "혜택 수정 (어드민)")
    @PutMapping("/benefits/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBenefit(@PathVariable Long id, @RequestBody RequestBenefit dto) {
        benefitService.update(id, dto);
        return ResponseEntity.ok("혜택이 수정됐습니다.");
    }

    @Operation(summary = "혜택 삭제 (어드민, soft delete)", description = "status를 HIDDEN으로 변경")
    @DeleteMapping("/benefits/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBenefit(@PathVariable Long id) {
        benefitService.delete(id);
        return ResponseEntity.ok("혜택이 삭제됐습니다.");
    }

    @Operation(summary = "혜택별 발급 통계 (어드민)")
    @GetMapping("/benefits/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResponseBenefitStats>> getBenefitStats() {
        return ResponseEntity.ok(benefitService.getStats());
    }

    // ================= 쇼핑몰 — 상품 =================

    @Operation(summary = "상품 전체 목록 (어드민)", description = "HIDDEN 포함")
    @GetMapping("/shop/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductDtos.AdminItem>> getProducts() {
        return ResponseEntity.ok(productService.findAllForAdmin());
    }

    @Operation(summary = "상품 등록 (어드민)",
            description = "partnerId 필수. 없는 업체이거나 HIDDEN 업체면 400")
    @PostMapping("/shop/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody ProductDtos.Request dto) {
        productService.create(dto);
        return ResponseEntity.ok("상품이 등록됐습니다.");
    }

    @Operation(summary = "상품 수정 (어드민)")
    @PutMapping("/shop/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductDtos.Request dto) {
        productService.update(id, dto);
        return ResponseEntity.ok("상품이 수정됐습니다.");
    }

    @Operation(summary = "상품 삭제 (어드민, soft delete)", description = "status를 HIDDEN으로 변경")
    @DeleteMapping("/shop/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok("상품이 삭제됐습니다.");
    }

    @Operation(summary = "상품 일괄 등록 (어드민)",
            description = "상품 객체 JSON 배열을 그대로 보낸다. 각 건에 partnerId 필수. 실패 건은 errors 배열로 반환")
    @PostMapping("/shop/products/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDtos.BulkResult> createProductsBulk(@RequestBody List<ProductDtos.Request> dtos) {
        return ResponseEntity.ok(productService.createBulk(dtos));
    }

    // ================= 쇼핑몰 — 주문 =================

    @Operation(summary = "주문 목록 (어드민)", description = "status 미지정 시 전체")
    @GetMapping("/shop/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDtos.AdminItem>> getOrders(
            @RequestParam(name = "status", required = false) String status) {

        OrderStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "status는 PENDING, PAID, PREPARING, SHIPPING, DELIVERED, CANCELLED 중 하나여야 합니다.");
            }
        }
        return ResponseEntity.ok(orderService.findAllForAdmin(parsed));
    }

    @Operation(summary = "주문 상태 변경 (어드민)", description = "PREPARING / SHIPPING / DELIVERED")
    @PutMapping("/shop/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeOrderStatus(@PathVariable Long id,
                                               @RequestBody OrderDtos.StatusRequest dto) {
        orderService.changeStatus(id, dto);
        return ResponseEntity.ok("주문 상태가 변경됐습니다.");
    }
}
