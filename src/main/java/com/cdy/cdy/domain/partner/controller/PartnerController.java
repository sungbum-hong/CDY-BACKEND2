package com.cdy.cdy.domain.partner.controller;

import com.cdy.cdy.domain.partner.dto.ResponsePartner;
import com.cdy.cdy.domain.partner.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "파트너 목록 조회 (공개)", description = "인증 없이 조회 가능")
    @GetMapping
    public ResponseEntity<List<ResponsePartner>> findAll() {
        return ResponseEntity.ok(partnerService.findAll());
    }
}
