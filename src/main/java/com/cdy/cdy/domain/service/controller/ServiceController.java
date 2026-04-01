package com.cdy.cdy.domain.service.controller;

import com.cdy.cdy.domain.service.dto.RequestService;
import com.cdy.cdy.domain.service.dto.ResponseService;
import com.cdy.cdy.domain.service.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @Operation(summary = "서비스 등록", description = "로그인 필요. 서비스를 등록합니다.")
    @PostMapping
    public ResponseEntity<ResponseService> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RequestService dto) {
        return ResponseEntity.ok(serviceService.create(userDetails.getUsername(), dto));
    }

    @Operation(summary = "서비스 목록 조회", description = "인증 불필요. ACTIVE 상태 서비스 전체 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<List<ResponseService>> findAll() {
        return ResponseEntity.ok(serviceService.findAll());
    }

    @Operation(summary = "서비스 상세 조회", description = "인증 불필요. 서비스 단건 조회.")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseService> findById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.findById(id));
    }
}
