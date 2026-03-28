package com.cdy.cdy.domain.project.controller;

import com.cdy.cdy.domain.project.dto.RequestProject;
import com.cdy.cdy.domain.project.dto.ResponseProject;
import com.cdy.cdy.domain.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 목록 조회 (공개)", description = "전체 프로젝트 목록 조회 (비로그인 접근 가능)")
    @GetMapping
    public ResponseEntity<List<ResponseProject>> findAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @Operation(summary = "프로젝트 단건 조회", description = "프로젝트 ID로 단건 조회")
    @GetMapping("/{projectId}")
    public ResponseEntity<ResponseProject> findById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.findById(projectId));
    }

    @Operation(summary = "프로젝트 생성", description = "로그인한 유저가 프로젝트를 생성합니다.")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @RequestBody RequestProject dto) {
        projectService.create(authentication.getName(), dto);
        return ResponseEntity.ok("프로젝트가 생성되었습니다.");
    }

    @Operation(summary = "프로젝트 수정", description = "작성자만 수정 가능합니다.")
    @PutMapping("/{projectId}")
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long projectId,
                                    @RequestBody RequestProject dto) {
        projectService.update(authentication.getName(), projectId, dto);
        return ResponseEntity.ok("프로젝트가 수정되었습니다.");
    }
}
