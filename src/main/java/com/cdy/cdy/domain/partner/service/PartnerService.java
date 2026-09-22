package com.cdy.cdy.domain.partner.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.benefit.repository.BenefitRepository;
import com.cdy.cdy.domain.partner.dto.RequestPartner;
import com.cdy.cdy.domain.partner.dto.ResponseAdminPartner;
import com.cdy.cdy.domain.partner.dto.ResponsePartner;
import com.cdy.cdy.domain.partner.entity.Partner;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;
import com.cdy.cdy.domain.partner.repository.PartnerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final BenefitRepository benefitRepository;
    private final ImageUrlResolver imageUrlResolver;

    public List<ResponsePartner> findAll() {
        return partnerRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(p -> ResponsePartner.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .imageUrl(p.getImageUrl())
                        .linkUrl(p.getLinkUrl())
                        .sortOrder(p.getSortOrder())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    /** 어드민 전용. HIDDEN 포함 전체 조회 */
    @Transactional(readOnly = true)
    public List<ResponseAdminPartner> findAllForAdmin() {
        return partnerRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(p -> new ResponseAdminPartner(
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getDescription(),
                        p.getLogoImageKey(),
                        imageUrlResolver.toPresignedUrl(p.getLogoImageKey()),
                        p.getWebsiteUrl(),
                        p.getStatus(),
                        p.getImageUrl(),
                        p.getLinkUrl(),
                        p.getSortOrder(),
                        p.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void create(RequestPartner dto) {
        partnerRepository.save(Partner.builder()
                .name(dto.getName())
                .imageUrl(dto.getImageUrl())
                .linkUrl(dto.getLinkUrl())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .category(dto.getCategory())
                .description(dto.getDescription())
                .logoImageKey(dto.getLogoImageKey())
                .websiteUrl(dto.getWebsiteUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : PartnerStatus.ACTIVE)
                .build());
    }

    @Transactional
    public void update(Long id, RequestPartner dto) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 파트너"));

        partner.update(dto.getName(),
                dto.getCategory(),
                dto.getDescription(),
                dto.getLogoImageKey(),
                dto.getWebsiteUrl(),
                dto.getStatus(),
                dto.getImageUrl(),
                dto.getLinkUrl(),
                dto.getSortOrder());
    }

    @Transactional
    public void delete(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 파트너"));

        // 혜택이 달려 있으면 FK 제약으로 실패하므로 미리 막는다.
        if (benefitRepository.existsByPartnerId(id)) {
            throw new IllegalStateException("등록된 혜택이 있는 파트너는 삭제할 수 없습니다. 혜택을 먼저 삭제하거나 파트너를 HIDDEN 처리하세요.");
        }

        partnerRepository.delete(partner);
    }
}
