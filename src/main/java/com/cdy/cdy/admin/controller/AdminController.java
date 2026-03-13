package com.cdy.cdy.admin.controller;

import com.cdy.cdy.admin.dto.ResponseUserList;
import com.cdy.cdy.admin.service.AdminService;
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

    @Operation(summary = "어드민이 신규 유저 등록")
    @PostMapping("/createUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(
            Authentication authentication,
            @RequestBody UserRequestDto dto) {
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
}
