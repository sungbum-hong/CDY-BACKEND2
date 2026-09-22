package com.cdy.cdy.domain.benefit.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.benefit.dto.*;
import com.cdy.cdy.domain.benefit.entity.Benefit;
import com.cdy.cdy.domain.benefit.entity.BenefitClaim;
import com.cdy.cdy.domain.benefit.entity.BenefitStatus;
import com.cdy.cdy.domain.benefit.repository.BenefitClaimRepository;
import com.cdy.cdy.domain.benefit.repository.BenefitRepository;
import com.cdy.cdy.domain.partner.entity.Partner;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;
import com.cdy.cdy.domain.partner.repository.PartnerRepository;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BenefitService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BenefitRepository benefitRepository;
    private final BenefitClaimRepository benefitClaimRepository;
    private final PartnerRepository partnerRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;

    // ================= 크루용 =================

    /**
     * 노출 중이고 유효기간 내인 혜택 목록. category 가 null 이면 전체.
     */
    @Transactional(readOnly = true)
    public List<ResponseBenefitList> findActive(PartnerCategory category) {

        LocalDate today = LocalDate.now(KST);

        List<Benefit> benefits = (category == null)
                ? benefitRepository.findActive(BenefitStatus.ACTIVE, PartnerStatus.ACTIVE, today)
                : benefitRepository.findActiveByCategory(BenefitStatus.ACTIVE, PartnerStatus.ACTIVE, today, category);

        return benefits.stream()
                .map(b -> {
                    Partner p = b.getPartner();
                    return new ResponseBenefitList(
                            b.getId(),
                            b.getTitle(),
                            b.getDiscountText(),
                            b.getBenefitType(),
                            p.getName(),
                            p.getCategory(),
                            imageUrlResolver.toPresignedUrl(b.getImageKey()),
                            logoUrlOf(p));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseBenefitDetail findDetail(Long id) {

        Benefit b = benefitRepository.findWithPartnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 혜택"));

        // 숨김 처리(소프트 삭제)된 혜택은 크루에게 노출하지 않는다.
        if (b.getStatus() != BenefitStatus.ACTIVE) {
            throw new EntityNotFoundException("존재하지 않는 혜택");
        }

        Partner p = b.getPartner();
        return new ResponseBenefitDetail(
                b.getId(),
                b.getTitle(),
                b.getDiscountText(),
                b.getBenefitType(),
                p.getName(),
                p.getCategory(),
                imageUrlResolver.toPresignedUrl(b.getImageKey()),
                logoUrlOf(p),
                b.getDescription(),
                b.getUsageGuide(),
                b.getValidFrom(),
                b.getValidTo(),
                p.getWebsiteUrl());
    }

    @Transactional
    public ResponseBenefitClaim claim(String username, Long benefitId) {

        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        Benefit benefit = benefitRepository.findWithPartnerById(benefitId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 혜택"));

        if (benefit.getStatus() != BenefitStatus.ACTIVE) {
            throw new IllegalStateException("현재 이용할 수 없는 혜택입니다.");
        }

        LocalDate today = LocalDate.now(KST);
        if (!benefit.isActiveOn(today)) {
            throw new IllegalStateException("혜택 이용 기간이 아닙니다.");
        }

        LocalDateTime claimedAt = LocalDateTime.now(KST);
        benefitClaimRepository.save(BenefitClaim.builder()
                .user(user)
                .benefit(benefit)
                .claimedAt(claimedAt)
                .build());

        return switch (benefit.getBenefitType()) {
            case CODE -> {
                if (benefit.getCodeValue() == null || benefit.getCodeValue().isBlank()) {
                    throw new IllegalStateException("발급 가능한 코드가 등록되어 있지 않습니다.");
                }
                yield ResponseBenefitClaim.code(benefit.getCodeValue());
            }
            case LINK -> {
                if (benefit.getLinkUrl() == null || benefit.getLinkUrl().isBlank()) {
                    throw new IllegalStateException("연결할 링크가 등록되어 있지 않습니다.");
                }
                yield ResponseBenefitClaim.link(benefit.getLinkUrl());
            }
            case SHOW -> ResponseBenefitClaim.show(user.getNickname(), claimedAt);
        };
    }

    @Transactional(readOnly = true)
    public List<ResponseMyBenefitClaim> findMyClaims(String username) {

        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        return benefitClaimRepository.findMyClaims(user.getId());
    }

    // ================= 어드민용 =================

    @Transactional
    public void create(RequestBenefit dto) {

        Partner partner = partnerRepository.findById(dto.partnerId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 파트너"));

        benefitRepository.save(Benefit.builder()
                .partner(partner)
                .title(dto.title())
                .description(dto.description())
                .discountText(dto.discountText())
                .benefitType(dto.benefitType())
                .codeValue(dto.codeValue())
                .linkUrl(dto.linkUrl())
                .usageGuide(dto.usageGuide())
                .imageKey(dto.imageKey())
                .validFrom(dto.validFrom())
                .validTo(dto.validTo())
                .status(dto.status() != null ? dto.status() : BenefitStatus.ACTIVE)
                .sortOrder(dto.sortOrder() != null ? dto.sortOrder() : 0)
                .build());
    }

    @Transactional
    public void update(Long id, RequestBenefit dto) {

        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 혜택"));

        Partner partner = partnerRepository.findById(dto.partnerId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 파트너"));

        benefit.update(partner,
                dto.title(),
                dto.description(),
                dto.discountText(),
                dto.benefitType(),
                dto.codeValue(),
                dto.linkUrl(),
                dto.usageGuide(),
                dto.imageKey(),
                dto.validFrom(),
                dto.validTo(),
                dto.status(),
                dto.sortOrder());
    }

    /** 소프트 삭제 (status = HIDDEN) */
    @Transactional
    public void delete(Long id) {

        Benefit benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 혜택"));

        benefit.hide();
    }

    @Transactional(readOnly = true)
    public List<ResponseBenefitStats> getStats() {
        return benefitRepository.findClaimStats();
    }

    /** 어드민 목록. HIDDEN·기간만료 포함 전체 */
    @Transactional(readOnly = true)
    public List<ResponseAdminBenefit> findAllForAdmin() {
        return benefitRepository.findAllWithPartner()
                .stream()
                .map(b -> new ResponseAdminBenefit(
                        b.getId(),
                        b.getPartner().getId(),
                        b.getPartner().getName(),
                        b.getTitle(),
                        b.getDescription(),
                        b.getDiscountText(),
                        b.getBenefitType(),
                        b.getCodeValue(),
                        b.getLinkUrl(),
                        b.getUsageGuide(),
                        b.getImageKey(),
                        imageUrlResolver.toPresignedUrl(b.getImageKey()),
                        b.getValidFrom(),
                        b.getValidTo(),
                        b.getStatus(),
                        b.getSortOrder(),
                        b.getCreatedAt()))
                .toList();
    }

    // ================= 내부 =================

    /** 혜택몰용 로고(R2 키)가 없으면 기존 파트너 배너 이미지 URL로 대체 */
    private String logoUrlOf(Partner partner) {
        String resolved = imageUrlResolver.toPresignedUrl(partner.getLogoImageKey());
        return resolved != null ? resolved : partner.getImageUrl();
    }
}
