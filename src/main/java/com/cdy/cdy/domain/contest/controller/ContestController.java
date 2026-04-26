package com.cdy.cdy.domain.contest.controller;

import com.cdy.cdy.domain.contest.dto.ResponseContest;
import com.cdy.cdy.domain.contest.service.ContestService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    @Operation(summary = "공모전 목록 조회 (공개)", description = "인증 없이 조회 가능")
    @GetMapping
    public ResponseEntity<List<ResponseContest>> findAll() {
        return ResponseEntity.ok(contestService.findAll());
    }
}
