package com.cdy.cdy.admin.controller;

import com.cdy.cdy.admin.dto.RequestChangePassword;
import com.cdy.cdy.admin.dto.RequestPromoteAdmin;
import com.cdy.cdy.admin.dto.ResponseAdminStudy;
import com.cdy.cdy.admin.dto.ResponseUserList;
import com.cdy.cdy.admin.service.AdminService;
import com.cdy.cdy.domain.apply.dto.RequestApprove;
import com.cdy.cdy.domain.apply.dto.ResponseApplication;
import com.cdy.cdy.domain.apply.service.ApplicationService;
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
}
