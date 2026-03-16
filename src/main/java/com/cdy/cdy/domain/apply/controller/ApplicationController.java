package com.cdy.cdy.domain.apply.controller;

import com.cdy.cdy.domain.apply.dto.RequestApply;
import com.cdy.cdy.domain.apply.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/apply")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "크루원 신청", description = "인증 불필요. 이름/이메일/닉네임/분야를 저장합니다.")
    @PostMapping
    public ResponseEntity<?> apply(@RequestBody RequestApply dto) {
        applicationService.apply(dto);
        return ResponseEntity.ok("신청이 완료됐습니다.");
    }
}
