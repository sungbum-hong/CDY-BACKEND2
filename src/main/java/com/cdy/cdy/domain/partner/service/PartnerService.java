package com.cdy.cdy.domain.partner.service;

import com.cdy.cdy.domain.partner.dto.RequestPartner;
import com.cdy.cdy.domain.partner.dto.ResponsePartner;
import com.cdy.cdy.domain.partner.entity.Partner;
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

    @Transactional
    public void create(RequestPartner dto) {
        partnerRepository.save(Partner.builder()
                .name(dto.getName())
                .imageUrl(dto.getImageUrl())
                .linkUrl(dto.getLinkUrl())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build());
    }

    @Transactional
    public void delete(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 파트너"));
        partnerRepository.delete(partner);
    }
}
