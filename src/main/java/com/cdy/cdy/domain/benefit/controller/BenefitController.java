package com.cdy.cdy.domain.benefit.controller;

import com.cdy.cdy.domain.benefit.dto.ResponseBenefitClaim;
import com.cdy.cdy.domain.benefit.dto.ResponseBenefitDetail;
import com.cdy.cdy.domain.benefit.dto.ResponseBenefitList;
import com.cdy.cdy.domain.benefit.dto.ResponseMyBenefitClaim;
import com.cdy.cdy.domain.benefit.service.BenefitService;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 크루 전용 혜택몰(폐쇄몰). 모든 엔드포인트가 로그인 필수.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    @Operation(summary = "혜택 목록 조회 (크루 전용)",
            description = "노출 중이고 유효기간 내인 혜택만 반환. category 파라미터는 CAFE/FOOD/EDU/TOOL/ETC")
    @GetMapping
    public ResponseEntity<List<ResponseBenefitList>> findAll(
            @RequestParam(name = "category", required = false) String category) {

        return ResponseEntity.ok(benefitService.findActive(parseCategory(category)));
    }

    @Operation(summary = "내가 받은 혜택 기록 (크루 전용)")
    @GetMapping("/my")
    public ResponseEntity<List<ResponseMyBenefitClaim>> findMyClaims(Authentication authentication) {

        return ResponseEntity.ok(benefitService.findMyClaims(authentication.getName()));
    }

    @Operation(summary = "혜택 상세 조회 (크루 전용)",
            description = "codeValue / linkUrl 은 claim 시에만 반환하므로 상세에는 포함되지 않는다.")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseBenefitDetail> findOne(@PathVariable Long id) {

        return ResponseEntity.ok(benefitService.findDetail(id));
    }

    @Operation(summary = "혜택 받기 (크루 전용)",
            description = "CODE → codeValue, LINK → linkUrl, SHOW → userNickname + claimedAt 반환")
    @PostMapping("/{id}/claim")
    public ResponseEntity<ResponseBenefitClaim> claim(Authentication authentication,
                                                      @PathVariable Long id) {

        log.info("[BenefitController] 혜택 발급 요청 - username: {}, benefitId: {}",
                authentication.getName(), id);

        return ResponseEntity.ok(benefitService.claim(authentication.getName(), id));
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
